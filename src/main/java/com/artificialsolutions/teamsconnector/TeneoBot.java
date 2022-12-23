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

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(TeneoBot.class);

    /**
     * Indicates if potentially GDPR-sensitive data may ({@code true}) or may not ({@code false}) be
     * logged. The value of this property should be {@code false} in production.
     */
    private final boolean logSensitive = logger.isDebugEnabled() || logger.isTraceEnabled();

    /**
     * The configuration object.
     */
    private final Config config;

    /**
     * A JSON object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * Session repository mapping a session ID to the corresponding session object.
     */
    private final Map<BridgeSessionId, BridgeSession> bsidToBridgeSession = new HashMap<>();

    /**
     * A Microsoft graph client.
     */
    private final GraphClient graphClient;

    /**
     * A session implementation. Each session contains a Teneo engine client and a session
     * timeout task to terminate and eliminate the session after a period of inactivity.
     */
    private final class BridgeSession {

        /**
         * Session ID for for a particular Teams conversion started by a particular user.
         */
        final BridgeSessionId bsid;

        /**
         * Teneo engine client.
         */
        final TeneoEngineClient teneoEngineClient;

        /**
         * Flag indicates if the session is expired ({@code true}) or still alive ({@code false}).
         */
        transient volatile boolean expired;

        /**
         * The current session timeout task. Its goal is to terminate the session, which includes closing
         * all the communication clients the session object contains and deleting the session object itself
         * from the session repository.
         */
        transient volatile OneTimeTask timeoutTask;

        /**
         * Constructs a session object.
         * 
         * @param bsid Session ID for for a particular Teams conversion started by a particular user.
         */
        BridgeSession(final BridgeSessionId bsid) {
            this.bsid = bsid;
            teneoEngineClient = new TeneoEngineClient(config, mapper);
        }

        /**
         * Gets the session ID object for this session.
         *
         * @return The session ID object for this session.
         */
        BridgeSessionId getBsid() {
            return bsid;
        }

        /**
         * Returns the Teneo engine client for this session.
         * 
         * @return this session's Teneo engine client.
         */
        TeneoEngineClient getTeneoEngineClient() {
            return teneoEngineClient;
        }

        /**
         * Indicates if the session has expired.
         * 
         * @return {@code true} if the session expired and {@code false} otherwise.
         */
        synchronized boolean isExpired() {
            return expired;
        }

        /**
         * Cancels the scheduled timeout task.
         */
        synchronized void clearTimeoutTask() {
            if (timeoutTask == null) return;
            timeoutTask.cancel();
            timeoutTask = null;
        }

        /**
         * "Expires" the session: the eventual pending timeout task is cancelled, the timing start value is
         * reset and the session object is removed from the session repository. If this method is called for
         * an already expired session, it does nothing.
         */
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
                if (logSensitive) logger.debug("Expiring and removing bridge session with bsid {}; simultaneous sessions: {}", bsid, sessionCount);
                else logger.debug("Expiring and removing bridge session; simultaneous sessions: {}", sessionCount);
            }
        }

        /**
         * (Re)starts the session timeout count-down by canceling the current scheduled timeout task (if
         * there is one) and scheduling a new one. If this method is called for an already expired session,
         * it does nothing.
         */
        void handleTiming() {
            boolean bExpired = false;
            synchronized (this) {
                clearTimeoutTask();
                if (expired) bExpired = true;
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
                if (logSensitive) logger.info("Hitting upon an expired bridge session with bsid {}", bsid);
                else logger.info("Hitting upon an expired session");
            } else {
                if (logSensitive) logger.debug("Scheduling a new timeout task for bridge session with bsid {}", bsid);
                else logger.debug("Scheduling a new timeout task for bridge session");
            }
        }
    }

    /**
     * Provides a {@link BridgeSession} object for the given ID. If there already exists a {@link BridgeSession}
     * object for this ID, its timeout task is restarted and it is returned. If not, a new one is
     * created and associated with this ID, its timeout task is started and it is returned.
     * 
     * @param bsid the ID of the requested session.
     * 
     * @return the {@link BridgeSession} object or {@code null} if <i>too many</i> parallel sessions exist
     * (see {@link Config#PROP_BRIDGE_MAX_PARALLEL_SESSIONS} end
     * {@link Config#getMaxParallelSessions()}).
     */
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
            if (session != null) session.handleTiming();
        }
        if (session == null) {
            logger.warn("Failing to add new session because the session limit {} is exceeded", config.getMaxParallelSessions());
        } else if (logger.isDebugEnabled()) {
            if (newSession) logger.debug("Adding new session with bsid {}; simultaneous sessions: {}", bsid, sessionCount);
            else logger.debug("Getting existing session with bsid {}; simultaneous sessions: {}", bsid, sessionCount);
        }
        return session;
    }

    /**
     * Constructs an instance of this class.
     * 
     * @param config the global config object.
     * @param mapper the JSON object mapper.
     */
    public TeneoBot(final Config config, final ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.graphClient = new GraphClient(config, mapper);
        logger.debug("An instance of [{}] is created", TeneoBot.class);
    }

    /**
     * The callback invoked by the controller when a user submits a message into a Teams conversation.
     * It sends the user's input further to Teneo engine.
     * Normally you should not call this method in your code explicitly because it is called by the controller.
     *
     * @param turnContext The context object for this turn.
     *
     * @return A task that represents the work queued to execute.
     * You shouldn't normally do anything with this object. 
     *
     * @see ActivityHandler#onMessageActivity(TurnContext)
     */
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

        if (logSensitive && logger.isDebugEnabled()) logActivityDebug(activity, user);

        final Map<String, ?> paramNameToValue = prepareParamNameToValue(user, activity);

        return session.getTeneoEngineClient().sendAsync(paramNameToValue)
                .handle((json, thr) -> doAfterTeneoEngineQuery(turnContext, session, json, thr))
                .handle((sendResult, ex) -> null);
    }

    /**
     * The callback invoked by the controller when new members other than the bot joins a Teams conversation.
     * Normally you should not call this method explicitly in your code because it is called by the controller.
     *
     * @param turnContext The context object for this turn.
     * @param membersAdded the new conversation participants.
     *
     * @return A task that represents the work queued to execute.
     * You shouldn't normally do anything with this object. 
     *
     * @see ActivityHandler#onMembersAdded(List, TurnContext)
     */
    @Override
    protected CompletableFuture<Void> onMembersAdded(final List<ChannelAccount> membersAdded,
            final TurnContext turnContext) {
        if (logSensitive) logger.debug("membersAdded: {}", membersAdded);
        else logger.debug("membersAdded");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The callback invoked by the controller when members other than the bot leave a Teams conversation.
     * Normally you should not call this method explicitly in your code because it is called by the controller.
     *
     * @param turnContext The context object for this turn.
     * @param membersRemoved the conversation participants leaving the conversation.
     *
     * @return A task that represents the work queued to execute.
     * You shouldn't normally do anything with this object. 
     *
     * @see ActivityHandler#onMembersRemoved(List, TurnContext)
     */
    @Override
    protected CompletableFuture<Void> onMembersRemoved(final List<ChannelAccount> membersRemoved,
            final TurnContext turnContext) {
        if (logSensitive) logger.debug("membersRemoved: {}", membersRemoved);
        else logger.debug("membersRemoved");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Logs activity for debug purposes.
     * 
     * @param activity the Microsoft bot activity to log.
     * @param user the user.
     */
    private void logActivityDebug(final Activity activity, final User user) {
        logger.debug("New activity: user[{}] sent new request", user.userPrincipalName);
        logger.debug("userinput: [{}]", activity.getText());
        logger.debug("value: [{}]", activity.getValue());
        if (logger.isDebugEnabled()) {
            if (activity.getValue() instanceof Map<?, ?>) {
                var incomingVariables = (Map<?, ?>) activity.getValue();
                for (var variable : incomingVariables.entrySet()) {
                    logger.debug("inputparam key=[{}], value=[{}]", variable.getKey(), variable.getValue());
                }
            }
        }
    }

    /**
     * Creates a {@code Map} mapping the names of request parameters to their values
     * for a request to be sent to Teneo engine.
     * 
     * @param user the user.
     * @param activity the Microsoft bot activity.
     *
     * @return the {@code Map} of the names of request parameters to their values.
     */
    private Map<String, ?> prepareParamNameToValue(User user, Activity activity) {
        final Map<String, Object> result = new HashMap<>();
        result.put("viewtype", "tieapi");
        result.put("channel", "Teams");

        if (activity.getText() != null) {
            result.put("userinput", activity.getText());
        }
        if (activity.getValue() instanceof Map<?, ?>) {
            var incomingVariables = (Map<?, ?>) activity.getValue();
            for (var variable : incomingVariables.entrySet()) {
                final Object key = variable.getKey();
                if (key != null) result.put(key.toString(), variable.getValue());
            }
        }
        // Adding the parameters defined for the given user:
        graphClient.getGraphRequestParams().stream()
                .filter(param -> param.hasValue(user))
                .forEach(param -> result.put(param.getTeneoName(), param.getValue(user)));
        return result;
    }

    /**
     * Processes an eventual Teneo error and/or submits data to Teams. If the bridge session expires
     * in the meantime, it also closes the Teneo engine session.
     * 
     * @param turnContext Microsoft turn context.
     * @param session the bridge session.
     * @param json the object containing data that is received from Teneo engine and whose content will be submitted to Teams.
     * @param thr the Teneo error to be processed.
     * 
     * @return A {@link CompletableFuture} object with a Microsoft {@code ResourceResponse} result.
     */
    private CompletableFuture<?> doAfterTeneoEngineQuery(final TurnContext turnContext,
            final BridgeSession session, final JsonNode json, final Throwable thr) {
        if (thr != null) {
            logger.error("Teneo response failure", thr);
            if (config.isExplicitData()) {
                return sendTextToTeams(turnContext, "Teneo failed to respond: " + thr);
            }
            return sendTextToTeams(turnContext, "Teneo failed to respond");
        }
        final TeneoResponse tr = TeneoResponse.Parser.parse(json);
        final CompletableFuture<?> r = tr.getStatus().isFailed() ? sendErrorToTeams(turnContext, tr) : sendNormalToTeams(turnContext, tr);

        if (session.isExpired()) {
            if (logSensitive) logger.debug("Session was cancelled for bsid {}, forcing Teneo engine endsession", session.getBsid());
            else logger.debug("Session was cancelled, forcing Teneo engine endsession");
            try {
                session.getTeneoEngineClient().endSessionAsync();
            } catch (final Exception ex) {
                logger.warn("Failure ending Teneo session", ex);
            }
        }
        return r;
    }

    /**
     * Sends an error message to Teams.
     * 
     * @param turnContext Microsoft turn context.
     * @param response a Teneo response.
     * 
     * @return a {@link CompletableFuture} object with a Microsoft {@code ResourceResponse} result.
     */
    private CompletableFuture<ResourceResponse> sendErrorToTeams(final TurnContext turnContext,
            final TeneoResponse response) {
        String msg = response.getStatus().getErrorMessage();
        if (config.isExplicitData()) msg += "; Teneo response: " + response.getJson();

        if (response.getStatus().isTeneoError()) {
            if (logSensitive) logger.info("Submitting Teneo error message to Teams: [{}]", msg);
            else logger.info("Submitting Teneo error message to Teams");
        } else {
            if (logSensitive) logger.error("Submitting error message to Teams: [{}]", msg);
            else logger.error("Submitting error message to Teams");
        }
        return sendTextToTeams(turnContext, msg);
    }

    /**
     * Sends an error message to Teams.
     * 
     * @param turnContext Microsoft turn context.
     * @param response a Teneo response.
     * 
     * @return a {@link CompletableFuture} object with a Microsoft {@code ResourceResponse[]} result.
     */
    private CompletableFuture<ResourceResponse[]> sendNormalToTeams(final TurnContext turnContext,
            final TeneoResponse response) {
        final List<Activity> activities = new ArrayList<>();

        var output = response.getOutput();
        if (output.getTextSegmentIndexes() != null) {
            activities.addAll(processTextSegmentIndexes(output.getText(), output.getTextSegmentIndexes()));
        } else {
            activities.add(MessageFactory.text(output.getText()));
        }
        if (output.getAdaptiveCardContents() != null) {
            final var cardAttachment = createAdaptiveCardAttachment(output.getAdaptiveCardContents());
            if (cardAttachment != null) activities.add(MessageFactory.attachment(cardAttachment));
        }
        if (logger.isDebugEnabled()) {
            if (logSensitive) logger.debug("Submitting {} Activity instance(s) to Teams: {}", activities.size(), response.getJson());
            else logger.debug("Submitting {} Activity instance(s) to Teams", activities.size());
        }
        return turnContext.sendActivities(activities);
    }

    /**
     * Sends a text to Teams so it is displayed to the user.
     *
     * @param turnContext a Microsoft turn context.
     * @param text the text to send.
     *
     * @return A {@link CompletableFuture} object with a {@code ResourceResponse} result.
     */
    private CompletableFuture<ResourceResponse> sendTextToTeams(final TurnContext turnContext, final String text) {
        return turnContext.sendActivity(MessageFactory.text(text));
    }

    /**
     * Creates an adaptive card attachment from a stringified JSON object.
     * 
     * @param adaptiveCardJson a stringified JSON object.
     * 
     * @return an adaptive card attachment or {@code null} if it could not be created.
     */
    private Attachment createAdaptiveCardAttachment(String adaptiveCardJson) {
        try {
            final Attachment attachment = new Attachment();
            attachment.setContentType("application/vnd.microsoft.card.adaptive");
            attachment.setContent(Serialization.jsonToTree(adaptiveCardJson));
            if (logSensitive) logger.debug("An adaptive card attachmend was created from [{}]", adaptiveCardJson);
            else logger.debug("An adaptive card attachmend was created");
            return attachment;
        } catch (final Exception ex) {
            if (logSensitive) logger.error("Failure creating attachment from [{}]", adaptiveCardJson, ex);
            else logger.error("Failure creating attachment", ex);
            return null;
        }
    }

    /**
     * Creates a list of {@code Activity} instances containing an Teneo text answer
     * splitted into "bubbles".
     * 
     * @param text a Teneo answer text.
     * @param textSegmentIndexesJson a stringified JSON object defining the splitting.
     * 
     * @return a list of {@code Activity} instances.
     */
    private List<Activity> processTextSegmentIndexes(String text, String textSegmentIndexesJson) {
        try {
            var indexes = mapper.readTree(textSegmentIndexesJson);
            if (indexes.isArray()) {
                Iterable<JsonNode> iterable = indexes::iterator;
                final List<Activity> aa = StreamSupport.stream(iterable.spliterator(), false)
                    .map(n -> cutIndexNode(n, text))
                    .map(MessageFactory::text)
                    .collect(Collectors.toList());

                if (logger.isDebugEnabled()) {
                    if (logSensitive) logger.debug("{} Activitiy instance(s) created from [{}]", aa.size(), textSegmentIndexesJson);
                    else logger.debug("{} Activitiy instance(s) created", aa.size());
                }
                return aa;
            }
            if (logSensitive) {
                logger.warn("indexes are not an array, but {}: {}", indexes.getNodeType(), indexes);
            } else {
                logger.warn("indexes are not an array, but {}", indexes.getNodeType());
            }
        } catch (final Exception ex) {
            if (logSensitive) {
                logger.error("Failure parsing outputTextSegmentIndexes [{}]", textSegmentIndexesJson, ex);
            } else {
                logger.error("Failure parsing outputTextSegmentIndexes", ex);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Cuts a text basing on the content of a splitting node.
     * 
     * @param n a splitting node.
     * @param s the text to cut.
     * 
     * @return the text cut.
     */
    private String cutIndexNode(JsonNode n, String s) {
        Iterator<JsonNode> iterator = n.elements();
        var idx1 = iterator.next().asInt();
        var idx2 = iterator.next().asInt();
        return s.substring(idx1, idx2);
    }
}
