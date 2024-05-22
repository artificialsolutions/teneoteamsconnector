package com.artificialsolutions.teneoengine;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URLEncoder;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;

import com.artificialsolutions.teamsconnector.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artificialsolutions.common.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to send messages to Teneo engine.
 */
public class TeneoEngineClient implements Serializable {

    /**
     * Serial version ID for object (de)serialization.
     */
    private static final long serialVersionUID = -2047203770909462478L;

    /**
     * The charset used for encoding.
     */
    private static final Charset charset = StandardCharsets.UTF_8;

    /**
     * The content type header to be used in requests to Teneo engine.
     */
    private static final String requestContentTypeHeader = "application/x-www-form-urlencoded;charset=" + charset;

    private static final String lineSeparator;
    static {
        String s;
        try {
            s = System.getProperty("line.separator");
        } catch (final Exception ex) {
            s = null;
        }
        lineSeparator = s == null || s.isEmpty() ? "\n" : s;
    }

    /**
     * The cookie store for the Teneo engine client.
     */
    private final TeneoEngineCookieStore cookieStore = new TeneoEngineCookieStore();

    /**
     * The values of the latest response header "X-Gateway-Session".
     */
    private volatile List<String> gatewaySessionValues;

    /**
     * The Teneo engine session ID.
     */
    private volatile String sessionId;

    /**
     * The session-tagged Teneo endpoint URL.
     */
    private volatile URI uri;

    /**
     * The parameters and values to be used in the Teneo engine "endsession" request.
     */
    private final Map<String, Object> endsessionParamNameToValue = new HashMap<>(2);

    /**
     * The configuration object.
     */
    private final Config config;

    /**
     * The JSON object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * Response timeout in milliseconds. It is defined as transient since its serialization is forbidden
     * by its implementation despite implementing Serializable.
     */
    private transient Duration responseTimeoutMillis;

    /**
     * The HTTP client to communicate with Teneo engine.
     */
    private transient HttpClient httpClient;

    /**
     * The logger.
     */
    private transient Logger logger;

    /**
     * Indicates if potentially GDPR-sensitive data may ({@code true}) or may not ({@code false}) be
     * logged. The value of this property should be {@code false} in production.
     */
    private transient boolean logSensitive;

    /**
     * Constructs an instance of the client to send messages to Teneo engine.
     * 
     * @param config The configuration object.
     */
    public TeneoEngineClient(final Config config, final ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.uri = config.getTeneoEngineEndpointUri();
        initTransient();
        logger.trace("An instance of {} is constructed", TeneoEngineClient.class);
    }

    /**
     * Initializes the transient class members.
     */
    private void initTransient() {
        logger = LoggerFactory.getLogger(TeneoEngineClient.class);
        logSensitive = logger.isDebugEnabled() || logger.isTraceEnabled();
        responseTimeoutMillis = Duration.ofMillis(config.getTeneoEngineResponseTimeoutMillis());
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(config.getTeneoEngineConnectTimeoutMillis()))
                .cookieHandler(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
                .build();
    }

    /**
     * Reads and assigns the values to use in the subsequent request for the
     * <a href="https://developers.teneo.ai/article/deploy/engine-api#session-management">session
     * management in Teneo engine</a>
     * 
     * @param headers the response headers.
     * @param json the response JSON object.
     */
    private void assignResponseValues(final HttpHeaders headers, final JsonNode json) {
        final String sessionId = json.path("sessionId").asText();
        List<String> w = headers.allValues("X-Gateway-Session");
        if (w.isEmpty()) w = null;
        // Re-using the sync lock - the only goal of re-using this object
        // is to avoid unnecessarily creating multiple sync objects:
        cookieStore.getLock().lock();
        try {
            if (sessionId == null) {
                this.sessionId = null;
                this.uri = config.getTeneoEngineEndpointUri();
            } else if (!sessionId.equals(this.sessionId)) {
                this.sessionId = sessionId;
                this.uri = getSessionTaggedEndpointUri(sessionId);
            }
            this.gatewaySessionValues = w;
        } finally {
            cookieStore.getLock().unlock();
        }
    }

    /**
     * Resets the values set by {@link #assignResponseValues(HttpHeaders, JsonNode)}.
     */
    private void resetResponseValues() {
        // Re-using the sync lock - the only goal of re-using this object
        // is to avoid unnecessarily creating multiple sync objects:
        cookieStore.getLock().lock();
        try {
            this.sessionId = null;
            this.uri = config.getTeneoEngineEndpointUri();
            this.gatewaySessionValues = null;
        } finally {
            cookieStore.getLock().unlock();
        }
    }

    /**
     * Asynchronously sends an input to Teneo engine.
     * 
     * @param paramNameToValue the parameter-value mapping to be sent to Teneo engine.
     * 
     * @return a {@code java.util.concurrent.CompletableFuture<JsonNode>} object containing a Teneo
     * engine response or an error.
     * 
     * @see java.util.concurrent.CompletableFuture
     * @see com.fasterxml.jackson.databind.JsonNode
     */
    public CompletableFuture<JsonNode> sendAsync(final Map<String, ?> paramNameToValue) {
        final long millisStart = System.currentTimeMillis();
        final HttpRequest request = createRequest(createPayload(paramNameToValue));
        if (logger.isDebugEnabled()) {
            String t = "Async request [" + millisStart + "] start, URI [" + request.uri() + "], cookieStore [" + cookieStore + ']';
            if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + ']';
            logger.debug(t);
        }
        final CompletableFuture<JsonNode> r = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply((final HttpResponse<String> response) -> {
            final String s = response.body();
            final HttpHeaders headers = response.headers();
            if (logger.isDebugEnabled()) {
                String t = "Async request [" + millisStart + "] end, URI [" + request.uri() + "], duration " + (System.currentTimeMillis() - millisStart) + " millisecs, status code " + response.statusCode() + ", headers [" + headers + "], cookieStore [" + cookieStore + ']';
                if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + "], response body [" + getAbbreviated(s) + ']';
                logger.debug(t);
            }
            if (response.statusCode() != 200) {
                String t = "Async request [" + millisStart + "] error, URI [" + request.uri() + "], status code " + response.statusCode() + ", headers [" + headers + "], cookieStore [" + cookieStore + ']';
                if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + "], response body [" + getAbbreviated(s) + ']';
                logger.error(t);
                throw new RuntimeException("Async request error, status code " + response.statusCode());
            }
            if (s == null || s.isEmpty()) {
                String t = "Async request [" + millisStart + "] no body error, URI [" + request.uri() + "], status code " + response.statusCode() + ", headers [" + headers + "], cookieStore [" + cookieStore + ']';
                if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + ']';
                logger.error(t);
                throw new RuntimeException("Async request error no body error");
            }
            final JsonNode json;
            try {
                json = mapper.readTree(new StringReader(s));
            } catch (final Exception ex) {
                String t = "Async request [" + millisStart + "] parsing error, URI [" + request.uri() + "], headers [" + headers + "], cookieStore [" + cookieStore + ']';
                if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + "], response body [" + getAbbreviated(s) + ']';
                logger.error(t, ex);
                throw new RuntimeException("Async request parsing error", ex);
            }
            assignResponseValues(headers, json);
            return json;
        });
        if (logger.isDebugEnabled()) {
            return r.whenComplete((final JsonNode json, final Throwable thr) -> {
                if (thr != null) {
                    String t = "Async request [" + millisStart + "] error, URI [" + request.uri() + "], duration " + (System.currentTimeMillis() - millisStart) + " millisecs, cookieStore [" + cookieStore + ']';
                    if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + ']';
                    logger.warn(t, thr);
                } else {
                    String t = "Async request [" + millisStart + "] success, URI [" + request.uri() + "], duration " + (System.currentTimeMillis() - millisStart) + " millisecs, cookieStore [" + cookieStore + ']';
                    if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + "], response object " + json;
                    logger.debug(t);
                }
            });
        }
        return r;
    }

    /**
     * Sends an input to Teneo engine.
     * 
     * @param paramNameToValue the parameter-value mapping to be sent to Teneo engine.
     * 
     * @return the Teneo engine response as a {@link com.fasterxml.jackson.databind.JsonNode} object.
     * 
     * @throws RuntimeException if status code of the response is not 200.
     * @throws IOException the same as in
     * {@link HttpClient#send(HttpRequest, java.net.http.HttpResponse.BodyHandler)}
     * @throws InterruptedException the same as in
     * {@link HttpClient#send(HttpRequest, java.net.http.HttpResponse.BodyHandler)}
     * @throws SecurityException the same as in
     * {@link HttpClient#send(HttpRequest, java.net.http.HttpResponse.BodyHandler)}
     */
    public JsonNode send(final Map<String, ?> paramNameToValue) throws IOException, InterruptedException, SecurityException, RuntimeException {
        final long millisStart = System.currentTimeMillis();
        final HttpRequest request = createRequest(createPayload(paramNameToValue));
        if (logger.isDebugEnabled()) {
            String t = "Request [" + millisStart + "] start, URI [" + request.uri() + "], cookieStore [" + cookieStore + ']';
            if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + ']';
            logger.debug(t);
        }
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        final String s = response.body();
        final HttpHeaders headers = response.headers();
        if (logger.isDebugEnabled()) {
            String t = "Request [" + millisStart + "] end, URI [" + request.uri() + "], duration " + (System.currentTimeMillis() - millisStart) + " millisecs, status code " + response.statusCode() + ", headers [" + headers + "], cookieStore [" + cookieStore + ']';
            if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + "], response body [" + getAbbreviated(s) + ']';
            logger.debug(t);
        }
        if (response.statusCode() != 200) {
            String t = "Request [" + millisStart + "] error, URI [" + request.uri() + "], status code " + response.statusCode() + ", headers [" + headers + "], cookieStore [" + cookieStore + ']';
            if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + "], response body [" + getAbbreviated(s) + ']';
            logger.error(t);
            throw new RuntimeException("Request error, status code " + response.statusCode());
        }
        if (s == null || s.isEmpty()) {
            String t = "Request [" + millisStart + "] no body error, URI [" + request.uri() + "], status code " + response.statusCode() + ", headers [" + headers + "], cookieStore [" + cookieStore + ']';
            if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + ']';
            logger.error(t);
            throw new RuntimeException("Async request error no body error");
        }
        final JsonNode json;
        try {
            json = mapper.readTree(new StringReader(s));
        } catch (final Exception ex) {
            String t = "Request [" + millisStart + "] parsing error, URI [" + request.uri() + "], headers [" + headers + "], cookieStore [" + cookieStore + ']';
            if (logSensitive) t += ", query [" + createPayloadDebugShow(paramNameToValue) + "], response body [" + getAbbreviated(s) + ']';
            logger.error(t, ex);
            throw new RuntimeException("Request parsing error", ex);
        }        
        assignResponseValues(headers, json);
        return json;
    }

    /**
     * Asynchronously sends an endsession request to Teneo engine.
     * 
     * @return a {@code java.util.concurrent.CompletableFuture<String>} object containing an unparsed
     * Teneo engine response or an error.
     * 
     * @see java.util.concurrent.CompletableFuture
     */
    public CompletableFuture<String> endSessionAsync() {
        final long millisStart = System.currentTimeMillis();
        final HttpRequest request = createEndsessionRequest();
        if (logger.isDebugEnabled()) {
            logger.debug("Async endsession request [" + millisStart + "] start, URI [" + request.uri() + "], cookieStore [" + cookieStore + ']');
        }
        final CompletableFuture<String> r = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply((final HttpResponse<String> response) -> {
            final String s = response.body();
            if (logger.isDebugEnabled()) {
                String t = "Async endsession request [" + millisStart + "] end, URI [" + request.uri() + "], duration " + (System.currentTimeMillis() - millisStart) + " millisecs, status code " + response.statusCode() + ", headers [" + response.headers() + "], cookieStore [" + cookieStore + ']';
                if (logSensitive) t += ", response body [" + getAbbreviated(s) + ']';
                logger.debug(t);
            }
            if (response.statusCode() != 200) {
                String t = "Async endsession request [" + millisStart + "] error, URI [" + request.uri() + "], status code " + response.statusCode() + ", headers [" + response.headers() + "], cookieStore [" + cookieStore + ']';
                if (logSensitive) t += ", response body [" + getAbbreviated(s) + ']';
                logger.error(t);
                throw new RuntimeException("Async request error, status code " + response.statusCode());
            }
            resetResponseValues();
            return s;
        });
        if (logger.isDebugEnabled()) {
            return r.whenComplete((final String s, final Throwable thr) -> {
                if (thr != null) {
                    String t = "Async endsession request [" + millisStart + "] error, URI [" + request.uri() + "], duration " + (System.currentTimeMillis() - millisStart) + " millisecs, cookieStore [" + cookieStore + ']';
                    logger.warn(t, thr);
                } else {
                    String t = "Async endsession request [" + millisStart + "] success, URI [" + request.uri() + "], duration " + (System.currentTimeMillis() - millisStart) + " millisecs, cookieStore [" + cookieStore + ']';
                    if (logSensitive) t += ", response body [" + getAbbreviated(s) + ']';
                    logger.debug(t);
                }
            });
        }
        return r;
    }

    /**
     * Adds some common values to the request builder. These values include the session identification
     * support as per https://developers.teneo.ai/article/deploy/engine-api#session-management
     * 
     * @param builder the request builder
     * @param gatewaySessionValues the gateway session values to add
     * @param encodedJsessionid URL-encoded session ID
     */
    private void addValues(final HttpRequest.Builder builder, final List<String> gatewaySessionValues, final String encodedJsessionid) {
        builder.timeout(responseTimeoutMillis).header("Content-Type", requestContentTypeHeader);
        if (gatewaySessionValues != null) {
            // Add the "X-Teneo-Session" header, as per
            // https://developers.artificial-solutions.com/engine/api#session-management
            for (final String s : gatewaySessionValues) {
                // gatewaySessionValues will most likely either be empty or contain one single
                // element so there is no need to optimize string concatenations here:
                builder.header("X-Teneo-Session", "JSESSIONID=" + encodedJsessionid + "; " + s);
            }
        }
    }

    /**
     * Creates a session-tagged endpoint URI from an endpoint URI without session tag.
     * 
     * @return the session-tagged URI
     */
    private URI getSessionTaggedEndpointUri(final String sessionId) {
        final URI uri = config.getTeneoEngineEndpointUri();
        try {
            String path = uri.getPath();
            if (path == null || path.isEmpty()) path = "/;jsessionid=" + sessionId;
            else path += ";jsessionid=" + sessionId;
            return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
        } catch (final Exception ex) {
            logger.error("Failure to construct session tagged URI from URI [{}]; the Teneo session might not be sustained correctly", uri, ex);
            return uri;
        }
    }

    /**
     * Creates a session-tagged endsession URI from an endpoint URI without session tag.
     * 
     * @return the session-tagged URI
     */
    private URI getSessionTaggedEndsessionUri(final String sessionId) {
        final URI uri = config.getTeneoEngineEndpointUri();
        try {
            String path = uri.getPath();
            if (path == null || path.isEmpty()) path = "/endsession;jsessionid=" + sessionId;
            else path += (path.endsWith("/") ? "endsession;jsessionid=" : "/endsession;jsessionid=") + sessionId;
            return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
        } catch (final Exception ex) {
            logger.error("Failure to construct session tagged URI from URI [{}]; the Teneo session might not be sustained correctly", uri, ex);
            return uri;
        }
    }

    /**
     * Creates a request object with the given payload, to submit user input.
     * 
     * @param payload the payload for the request.
     * 
     * @return the created request.
     */
    private HttpRequest createRequest(final String payload) {
        // Re-using the sync lock - the only goal of re-using this object
        // is to avoid unnecessarily creating multiple sync objects:
        cookieStore.getLock().lock();
        final String sessionId;
        final URI uri;
        final List<String> gatewaySessionValues;
        try {
            uri = this.uri;
            sessionId = this.sessionId;
            gatewaySessionValues = this.gatewaySessionValues;
        } finally {
            cookieStore.getLock().unlock();
        }
        final HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
        logger.info("Session id: {}", sessionId);
        addValues(builder, gatewaySessionValues, sessionId);
        return builder.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
    }

    /**
     * Creates an request object to end the session with teneo engine.
     * 
     * @return the created request.
     */
    private HttpRequest createEndsessionRequest() {
        final String payload;
        synchronized (endsessionParamNameToValue) {
            payload = createPayload(endsessionParamNameToValue);
        }
        // Re-using the sync lock - the only goal of re-using this object
        // is to avoid unnecessarily creating multiple sync objects:
        cookieStore.getLock().lock();
        final String sessionId;
        final List<String> gatewaySessionValues;
        try {
            sessionId = this.sessionId;
            gatewaySessionValues = this.gatewaySessionValues;
        } finally {
            cookieStore.getLock().unlock();
        }
        final HttpRequest.Builder builder = HttpRequest.newBuilder().uri(getSessionTaggedEndsessionUri(sessionId));
        addValues(builder, gatewaySessionValues, sessionId);
        return builder.POST(HttpRequest.BodyPublishers.ofString(payload)).build();
    }

    /**
     * Creates a request payload.
     * 
     * @param paramNameToValue the parameter-value mapping to be sent to Teneo engine.
     * 
     * @return the payload as a String.
     * 
     * @throws IllegalArgumentException if a parameter name is {@code null} or an empty String.
     */
    private String createPayload(final Map<String, ?> paramNameToValue) throws IllegalArgumentException {
        final Object viewname = paramNameToValue.get("viewname");
        final Object viewtype = paramNameToValue.get("viewtype");
        synchronized (endsessionParamNameToValue) {
            endsessionParamNameToValue.put("viewname", viewname);
            endsessionParamNameToValue.put("viewtype", viewtype);
        }
        StringBuilder sb = null;
        final StringBuilder sbResult = new StringBuilder();
        for (final Map.Entry<String, ?> e : paramNameToValue.entrySet()) {
            final Object v = e.getValue();
            if (v == null) continue;
            final String paramName = e.getKey();
            if (paramName == null || paramName.isEmpty()) throw new IllegalArgumentException("null or empty param name");
            if (sbResult.length() > 0) sbResult.append('&');
            sbResult.append(URLEncoder.encode(paramName, charset)).append('=');
            if ((v instanceof Map) || (v instanceof Collection) || (v instanceof Object[])) {
                if (sb == null) sb = new StringBuilder();
                else sb.delete(0, sb.length());
                sbResult.append(URLEncoder.encode(JsonUtils.appendAsJson(sb, v).toString(), charset));
            } else if (v instanceof JsonNode) {
                sbResult.append(URLEncoder.encode(((JsonNode)v).asText(), charset));
            } else {
                sbResult.append(URLEncoder.encode(v.toString(), charset));
            }
        }
        sb = null;
        return sbResult.toString();
    }

    /**
     * Creates a visually beautified request payload to be displayed in logs etc.
     * 
     * @param paramNameToValue the parameter-value mapping to be sent to Teneo engine.
     * 
     * @return the demo payload as a String.
     * 
     * @throws IllegalArgumentException if a parameter name is null or an empty String.
     */
    private static String createPayloadDebugShow(final Map<String, ?> paramNameToValue) throws IllegalArgumentException {
        final int maxLenth = 1024;
        final StringBuilder sbResult = new StringBuilder(maxLenth + 64);
        for (final Map.Entry<String, ?> e : paramNameToValue.entrySet()) {
            final Object v = e.getValue();
            if (v == null) continue;
            if (sbResult.length() > 0) {
                if (sbResult.length() > maxLenth) break;
                sbResult.append(lineSeparator).append('&').append(lineSeparator);
            }
            final String paramName = e.getKey();
            sbResult.append(URLEncoder.encode(paramName, charset)).append('=');
            if (sbResult.length() > maxLenth) break;
            if ((v instanceof Map) || (v instanceof Collection) || (v instanceof Object[])) JsonUtils.appendAsJson(sbResult, v);
            else if (v instanceof JsonNode) sbResult.append(URLEncoder.encode(((JsonNode)v).asText(), charset));
            else sbResult.append(URLEncoder.encode(v.toString(), charset));
        }
        if (sbResult.length() > maxLenth) {
            sbResult.delete(maxLenth - 3, sbResult.length()).append("...");
        }
        return sbResult.toString();
    }

    /**
     * Abbreviates a long text, to be displayed in logs etc.
     * 
     * @param s the text to abbreviate
     * 
     * @return the abbreviated text.
     */
    private static String getAbbreviated(final String s) {
        return s.length() > 256 ? s.substring(0, 253) + "..." : s;
    }

    /**
     * Reconstitutes this object from a stream (i.e., deserializes it).
     * 
     * @param ois the stream.
     * 
     * @throws IOException if the class of a serialized object could not be found.
     * @throws ClassNotFoundException if an I/O error occurs.
     */
    private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        initTransient();
        logger.trace("An instance of {} is created from deserialization", TeneoEngineClient.class);
    }
}
