package com.artificialsolutions.teamsconnector;

import java.io.Serializable;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Global configuration object.
 */
@Component
public class Config implements Serializable {

    /**
     * Serial version ID for object (de)serialization.
     */
    private static final long serialVersionUID = 4902449067499652244L;

    /**
     * <code>{@value}</code> - the property name for the Microsoft app ID - don't change!
     */
    private static final String PROP_MICROSOFT_APP_ID = "${MicrosoftAppId}";

    /**
     * <code>{@value}</code> - the property name for the Microsoft app ID - don't change!
     */
    private static final String PROP_MICROSOFT_APP_PASSWORD = "${MicrosoftAppPassword}";

    /**
     * <code>{@value}</code> - the property name for the Microsoft tenant ID - don't change!
     */
    private static final String PROP_MICROSOFT_TENANT_ID = "${MicrosoftTenantId}";

    /**
     * <code>{@value}</code> - the property name for the URL of the Teneo engine.
     */
    private static final String PROP_TENEO_ENGINE_URL = "${teneo.engine.endpointUrl}";

    /**
     * <code>{@value}</code> - the property name for the connection timeout of the requests sent to the
     * Teneo engine.
     */
    private static final String PROP_TENEO_ENGINE_CONNECT_TIMEOUT = "${teneo.engine.connectTimeoutMillis}";

    /**
     * <code>{@value}</code> - the property name for the response timeout of the requests sent to the
     * Teneo engine.
     */
    private static final String PROP_TENEO_ENGINE_RESPONSE_TIMEOUT = "${teneo.engine.responseTimeoutMillis}";

    /**
     * <code>{@value}</code> - the property name for the session timeout in milliseconds of the
     * Teams-Teneo bridge. For the best efficiency, this value should be some 10-20 seconds longer than
     * the timeout of the Teneo engine. Setting it longer will result in more parallel session
     * coexisting in the bridge. Setting it shorted than the Teneo engine timeout will have the same
     * effect for the user as shortening the Teneo engine session timeout itself without reducing the
     * number of parallel session in the Teneo engine though, so it should be avoided.
     */
    private static final String PROP_BRIDGE_SESSION_TIMEOUT = "${bridge.sessionTimeoutMillis}";

    /**
     * <code>{@value}</code> - the property name for the maximal number of parallel session in the the
     * Teams-Teneo bridge.
     */
    private static final String PROP_BRIDGE_MAX_PARALLEL_SESSIONS = "${bridge.maxParallelSessions}";

    /**
     * <code>{@value}</code> - the property name for the Boolean value indicating if some error and
     * debug information should be added to requests sent both to Teneo engine and to Teams. This
     * property is not obligatory and defaults to {@code false}. It should only be set to {@code true}
     * for testing and troubleshooting.
     */
    private static final String PROP_APPLICATION_EXPLICIT_DATA = "${application.explicitData}";

    /**
     * <code>{@value}</code> - a comma-separated list of additional user-related Microsoft Graph parameters (CSV) to be sent to Teneo engine.
     */
    private static final String PROP_MICROSOFT_GRAPH_REQUEST_PARAMS = "${microsoft.graph.request.params}";

    /**
     * Microsoft app ID
     */
    @Value(PROP_MICROSOFT_APP_ID)
    private String microsoftAppId;

    /**
     * Microsoft app password
     */
    @Value(PROP_MICROSOFT_APP_PASSWORD)
    private String microsoftAppPassword;

    /**
     * Microsoft tenant ID
     */
    @Value(PROP_MICROSOFT_TENANT_ID)
    private String microsoftTenantId;

    /**
     * The URL (URI) of the Teneo engine fordialog requests.
     */
    @Value(PROP_TENEO_ENGINE_URL)
    private URI teneoEngineEndpointUri;

    /**
     * The connection timeout of the requests sent to the Teneo engine.
     */
    @Value(PROP_TENEO_ENGINE_CONNECT_TIMEOUT)
    private Integer teneoEngineConnectTimeoutMillis;

    /**
     * The response timeout of the requests sent to the Teneo engine.
     */
    @Value(PROP_TENEO_ENGINE_RESPONSE_TIMEOUT)
    private Integer teneoEngineResponseTimeoutMillis;

    /**
     * The session timeout in milliseconds of the Teams-Teneo bridge.
     */
    @Value(PROP_BRIDGE_SESSION_TIMEOUT)
    private Integer bridgeSessionTimeoutMillis;

    /**
     * The maximal number of parallel session in the the Teams-Teneo bridge.
     */
    @Value(PROP_BRIDGE_MAX_PARALLEL_SESSIONS)
    private Integer maxParallelSessions;

    /**
     * A flag indicating if some error and debug information should be added to requests sent both to
     * Teneo engine and to Teams Messenger.
     */
    @Value(PROP_APPLICATION_EXPLICIT_DATA)
    private Boolean explicitData;

    /**
     * The CSV value indicating the user-related Microsoft Graph parameters to be sent to Teneo engine.
     */
    @Value(PROP_MICROSOFT_GRAPH_REQUEST_PARAMS)
    private String microsoftGraphRequestParams;

    /**
     * Returns the Microsoft app ID.
     * 
     * @return the Microsoft app ID.
     * 
     * @see #PROP_MICROSOFTT_APP_ID
     */
    public String getMicrosoftAppId() {
        return microsoftAppId;
    }

    /**
     * Returns the Microsoft app password.
     * 
     * @return the Microsoft app password.
     * 
     * @see #PROP_MICROSOFTT_APP_PASSWORD
     */
    public String getMicrosoftAppPassword() {
        return microsoftAppPassword;
    }

    /**
     * Returns the Microsoft tenant ID.
     * 
     * @return the Microsoft tenant ID.
     * 
     * @see #PROP_MICROSOFT_TENANT_ID
     */
    public String getMicrosoftTenantId(){
        return microsoftTenantId;
    }
    
    /**
     * Gets the URL (URI) of the Teneo engine.
     * 
     * @return the URL (URI) of the Teneo engine.
     * 
     * @see #PROP_TENEO_ENGINE_URL
     */
    public URI getTeneoEngineEndpointUri() {
        return teneoEngineEndpointUri;
    }

    /**
     * Gets the connection timeout of the requests sent to the Teneo engine.
     * 
     * @return the connection timeout of the requests sent to the Teneo engine.
     * 
     * @see #PROP_TENEO_ENGINE_CONNECT_TIMEOUT
     */
    public int getTeneoEngineConnectTimeoutMillis() {
        return teneoEngineConnectTimeoutMillis;
    }

    /**
     * Gets the response timeout of the requests sent to the Teneo engine.
     * 
     * @return the response timeout of the requests sent to the Teneo engine.
     * 
     * @see #PROP_TENEO_ENGINE_RESPONSE_TIMEOUT
     */
    public int getTeneoEngineResponseTimeoutMillis() {
        return teneoEngineResponseTimeoutMillis;
    }

    /**
     * Gets the session timeout in milliseconds of the Teams-Teneo bridge.
     * 
     * @return the session timeout in milliseconds of the Teams-Teneo bridge.
     * 
     * @see #PROP_BRIDGE_SESSION_TIMEOUT
     */
    public int getBridgeSessionTimeoutMillis() {
        return bridgeSessionTimeoutMillis;
    }

    /**
     * Gets the maximal number of parallel session in the the Teams-Teneo bridge.
     * 
     * @return the maximal number of parallel session in the the Teams-Teneo bridge.
     * 
     * @see #PROP_BRIDGE_MAX_PARALLEL_SESSIONS
     */
    public Integer getMaxParallelSessions() {
        return maxParallelSessions;
    }

    /**
     * Checks if some error and debug information should be added to requests sent both to Teneo engine
     * and to Teams Messenger.
     * 
     * @return {@code true} if this information should be added, {@code false} otherwise.
     * 
     * @see #PROP_APPLICATION_EXPLICIT_DATA
     */
    public boolean isExplicitData() {
        return explicitData;
    }

    /**
     * Gets a comma-separated list of additional user-related Microsoft Graph parameters (CSV) to be sent to Teneo engine.
     * 
     * @return the CSV value indicating the user-related Microsoft Graph parameters to be sent to Teneo engine.
     * 
     * @see #PROP_MICROSOFT_GRAPH_REQUEST_PARAMS
     */
    public String getMicrosoftGraphRequestParams() {
        return microsoftGraphRequestParams;
    }
}
