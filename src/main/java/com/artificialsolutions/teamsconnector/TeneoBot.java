// Licensed under the MIT License, based on a Microsoft template code.

package com.artificialsolutions.teamsconnector;

import com.artificialsolutions.common.OneTimeTask;
import com.artificialsolutions.graph.GraphClient;
import com.artificialsolutions.teneoengine.TeneoEngineClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.bot.builder.ActivityHandler;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.Attachment;
import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.ResourceResponse;
import com.microsoft.bot.schema.Serialization;
import com.microsoft.graph.models.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the functionality of the Bot. This is where application
 * specific logic for
 * interacting with the users is added. For this sample, the
 * {@link #onMessageActivity(TurnContext)}
 * echos the text back to the user. The
 * {@link #onMembersAdded(List, TurnContext)} will send a
 * greeting to new conversation participants.
 */
public class TeneoBot extends ActivityHandler {

    private final Logger logger = LoggerFactory.getLogger(TeneoBot.class);

    private final Config config;

    private final ObjectMapper mapper;

    private final Map<BridgeSessionId, BridgeSession> bsidToBridgeSession = new HashMap<>();

    private final GraphClient graphClient;

    private final class BridgeSession {

        final BridgeSessionId bsid;
        final TeneoEngineClient teneoEngineClient;
        transient volatile boolean expired;
        transient volatile OneTimeTask timeoutTask;

        BridgeSession(final BridgeSessionId bsid) {
            this.bsid = bsid;
            teneoEngineClient = new TeneoEngineClient(config, mapper);
        }

        BridgeSessionId getBsid() {
            return bsid;
        }

        TeneoEngineClient getTeneoEngineClient() {
            return teneoEngineClient;
        }

        synchronized boolean isExpired() {
            return expired;
        }

        synchronized void clearTimeoutTask() {
            if (timeoutTask == null)
                return;
            timeoutTask.cancel();
            timeoutTask = null;
        }

        void expire() {
            final int sessionCount;
            synchronized (bsidToBridgeSession) {
                synchronized (BridgeSession.this) {
                    clearTimeoutTask();
                    expired = true;
                    bsidToBridgeSession.remove(bsid);
                    // This here guarantees there are no STATE_EXPIRED sessions in the repo
                    // accessed as synchronized
                }
                sessionCount = bsidToBridgeSession.size();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Expiring and removing bridge session with bsid {}; simultaneous sessions: {}", bsid,
                        sessionCount);
            }
        }

        void handleTiming() {
            boolean bExpired = false;
            synchronized (this) {
                clearTimeoutTask();
                if (expired)
                    bExpired = true;
                else {
                    // The session has not expired yet. Schedule a (new) session timeout task:
                    timeoutTask = new OneTimeTask(config.getBridgeSessionTimeoutMillis()) {
                        @Override
                        public void run() {
                            expire();
                        }
                    };
                    timeoutTask.start();
                }
            }
            if (bExpired) {
                if (logger.isDebugEnabled())
                    logger.info("Hitting upon an expired bridge session with bsid {}", bsid);
                else
                    logger.info("Hitting upon an expired session");
            } else {
                if (logger.isDebugEnabled())
                    logger.debug("Scheduling a new timeout task for bridge session with bsid {}", bsid);
            }
        }
    }

    private BridgeSession provideBridgeSession(final BridgeSessionId bsid) {
        BridgeSession session;
        final int sessionCount;
        boolean newSession = false;
        synchronized (bsidToBridgeSession) {
            session = bsidToBridgeSession.get(bsid);
            if (session == null && bsidToBridgeSession.size() < config.getMaxParallelSessions()) {
                newSession = true;
                bsidToBridgeSession.put(bsid, session = new BridgeSession(bsid));
            }
            sessionCount = bsidToBridgeSession.size();
            if (session != null)
                session.handleTiming();
        }
        if (session == null)
            logger.warn("Failing to add new session because the session limit {} is exceeded",
                    config.getMaxParallelSessions());
        else if (logger.isDebugEnabled()) {
            if (newSession)
                logger.debug("Adding new session with bsid {}; simultaneous sessions: {}", bsid, sessionCount);
            else
                logger.debug("Getting existing session with bsid {}; simultaneous sessions: {}", bsid, sessionCount);
        }
        return session;
    }

    public TeneoBot(final Config config, final ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.graphClient = new GraphClient(config, logger, mapper);
        logger.debug("An instance of [{}] is created", TeneoBot.class);
    }

    @Override
    protected CompletableFuture<Void> onMessageActivity(final TurnContext turnContext) {
        final Activity activity = turnContext.getActivity();
        final ChannelAccount sender = activity.getFrom();
        final BridgeSession session = provideBridgeSession(
                new BridgeSessionId(sender.getAadObjectId(), sender.getId()));
        if (session == null)
            return sendTextToTeams(turnContext, "Session limit for Teneo Teams bridge has been reached")
                    .thenApply(sendResult -> null);

        var userId = turnContext.getActivity().getFrom().getAadObjectId();
        var user = graphClient.getUserById(userId);

        if (logger.isDebugEnabled()) {
            logActivity(activity, user);
        }
        final Map<String, ?> paramNameToValue = prepareParamNameToValue(user, activity);

        return session.getTeneoEngineClient().sendAsync(paramNameToValue)
                .handle((json, thr) -> doAfterTeneoEngineQuery(turnContext, session, json, thr))
                .handle((sendResult, ex) -> null);
    }

    @Override
    protected CompletableFuture<Void> onMembersAdded(final List<ChannelAccount> membersAdded,
            final TurnContext turnContext) {
        logger.debug("membersAdded: {}", membersAdded);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> onMembersRemoved(final List<ChannelAccount> membersRemoved,
            final TurnContext turnContext) {
        logger.debug("membersRemoved: {}", membersRemoved);
        return CompletableFuture.completedFuture(null);
    }

    private void logActivity(final Activity activity, final User user) {
        if (config.isExplicitData()) {
            logger.info("New activity: user[{}] sent new request", user.userPrincipalName);
        } else {
            logger.info("New activity");
        }
        logger.info("userinput: [{}]", activity.getText());
        logger.info("value: [{}]", activity.getValue());
        if (activity.getValue() instanceof Map<?,?>) {
            var incomingVariables = (Map<?, ?>) activity.getValue();
            for (var variable : incomingVariables.entrySet()) {
                logger.info("inputparam key=[{}], value=[{}]", variable.getKey(), variable.getValue());
            }
        }
    }

    private Map<String, ?> prepareParamNameToValue(User user, Activity activity) {
        Map<String, Object> result = new HashMap<>(Map.of(
                "viewtype", "tieapi",
                "channel", "Teams"));

        if (activity.getText() != null) {
            result.put("userinput", activity.getText());
        }

        if (activity.getValue() instanceof Map<?, ?>) {
            var incomingVariables = (Map<?, ?>) activity.getValue();
            for (var variable : incomingVariables.entrySet()) {
                result.put((String) variable.getKey(), variable.getValue());
            }
        }

        graphClient.getGraphRequestParams().stream()
                .filter(param -> param.hasValue(user))
                .forEach(param -> result.put(param.getTeneoName(), param.getValue(user)));
        return result;
    }

    private CompletableFuture<ResourceResponse> doAfterTeneoEngineQuery(final TurnContext turnContext,
            final BridgeSession session, final JsonNode json, final Throwable thr) {
        if (thr != null) {
            logger.error("Teneo response failure", thr);
            if (config.isExplicitData()) {
                return sendTextToTeams(turnContext, "Teneo response failure: " + thr);
            }
            return sendTextToTeams(turnContext, "Teneo failed to respond");
        }

        var r = processTeneoResponse(turnContext, TeneoResponse.Parser.parse(json));

        if (session.isExpired()) {
            if (logger.isDebugEnabled())
                logger.debug("Session was cancelled for bsid [{}], forcing Teneo engine endsession", session.getBsid());
            else
                logger.debug("Session was cancelled, forcing endsession");
            try {
                session.getTeneoEngineClient().endSessionAsync();
            } catch (final Exception ex) {
                logger.warn("Failure ending Teneo session", ex);
            }
        }
        return r != null ? r : CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<ResourceResponse> processTeneoResponse(final TurnContext turnContext,
            final TeneoResponse response) {
        if (response.getStatus().isFailed()) {
            return createErrorResponse(turnContext, response);
        }
        return createResponse(turnContext, response);
    }

    private CompletableFuture<ResourceResponse> createErrorResponse(final TurnContext turnContext,
            final TeneoResponse response) {
        var msg = response.getStatus().getErrorMessage();
        if (response.getStatus().isFromTeneo()) {
            if (logger.isDebugEnabled())
                logger.debug("Submitting Teneo error message to Teams: [{}]", msg);
            else
                logger.debug("Submitting Teneo error message to Teams");
            return sendTextToTeams(turnContext, msg);
        }
        if (logger.isDebugEnabled()) {
            msg += "; response: " + response.getJson();
        }
        logger.error(msg);
        if (config.isExplicitData()) {
            return sendTextToTeams(turnContext, msg);
        }
        return null;
    }

    private CompletableFuture<ResourceResponse> createResponse(final TurnContext turnContext,
            final TeneoResponse response) {
        var activities = new ArrayList<Activity>();

        var output = response.getOutput();
        if (output.getTextSegmentIndexes() != null) {
            activities.addAll(processTextSegmentIndexes(output.getText(), output.getTextSegmentIndexes()));
        } else {
            activities.add(MessageFactory.text(output.getText()));
        }
        if (output.getAdaptiveCardContents() != null) {
            var cardAttachment = createAdaptiveCardAttachment(output.getAdaptiveCardContents());
            activities.add(MessageFactory.attachment(cardAttachment));
        }
        turnContext.sendActivities(activities).thenApply(resourceResponses -> null);

        return null;
    }

    private CompletableFuture<ResourceResponse> sendTextToTeams(final TurnContext turnContext, final String text) {
        return turnContext.sendActivity(MessageFactory.text(text));
    }

    private Attachment createAdaptiveCardAttachment(String adaptiveCardJson) {
        try {
            Attachment attachment = new Attachment();
            attachment.setContentType("application/vnd.microsoft.card.adaptive");
            attachment.setContent(Serialization.jsonToTree(adaptiveCardJson));

            return attachment;

        } catch (IOException e) {
            e.printStackTrace();
            return new Attachment();
        }
    }

    private List<Activity> processTextSegmentIndexes(String text, String textSegmentIndexesJson) {
        try {
            var indexes = mapper.readTree(textSegmentIndexesJson);
            if (indexes.isArray()) {
                Iterable<JsonNode> iterable = indexes::iterator;
                return StreamSupport.stream(iterable.spliterator(), false)
                    .map(n -> parseIndexNode(n, text))
                    .map(MessageFactory::text)
                    .collect(Collectors.toList());
            }
        } catch (final Exception ex) {
            logger.error("Failure parsing outputTextSegmentIndexes {}, {}", textSegmentIndexesJson, ex);
        }
        return Collections.emptyList();
    }

    private String parseIndexNode(JsonNode n, String s) {
        Iterator<JsonNode> iterator = n.elements();
        var idx1 = iterator.next().asInt();
        var idx2 = iterator.next().asInt();
        return s.substring(idx1, idx2);
    }
}
