package com.artificialsolutions.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.artificialsolutions.teamsconnector.Config;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class specifying a Microsoft Graph Client.
 */
public class GraphClient {
    
    /**
     * Scopes of the Auth Provider.
     */
    private static final List<String> SCOPES = Collections.singletonList("https://graph.microsoft.com/.default");

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(GraphClient.class);

    /**
     * The client to get the user object.
     */
    private final GraphServiceClient<?> client;

    /**
     * The JSON object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * A comma-separated list of additional user-related Microsoft Graph parameters (CSV) to be sent to Teneo engine.
     */
    private final String requestParamsValue;
    
    /**
     * Available fields for Graph API calls, which are some of the fields described
     * <a href="https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser">here</a>. 
     */
    private final List<GraphParameter> graphRequestParams;

    /**
     * Constructor for this class.
     * 
     * @param config the global config object.
     * @param mapper the JSON object mapper.
     */
    public GraphClient(final Config config, final ObjectMapper mapper) {
        this.client = getAuthenticatedGraphClient(config);
        this.mapper = mapper;
        this.requestParamsValue = config.getMicrosoftGraphRequestParams();
        this.graphRequestParams = parseGraphRequestParams(config.getMicrosoftGraphRequestParams());
    }

    /**
     * Gets the user by his/her ID.
     *  
     * @param userId the user ID.
     * 
     * @return the {@code User} object for the corresponding user
     */
    public User getUserById(final String userId) {
        if (userId == null) {
            logger.error("User id is null");
            return new User();
        }

        try {
            if (logger.isDebugEnabled())
                logger.info("Fetching user info from Graph, userId: {}", userId);

            var user = client
                    .users(userId)
                    .buildRequest()
                    .select(requestParamsValue)
                    .get();

            if (logger.isDebugEnabled())
                logger.info("Got user info from Graph {}", mapper.writeValueAsString(user));

            return user;
        } catch (Exception ex) {
            logger.error("Failed to get user from Graph", ex);
            return new User();
        }
    }

    /**
     * Gets available fields for Graph API calls, which are some of the fields described
     * <a href="https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser">here</a>.
     *
     * @return Available fields for Graph API calls.
     */
    public List<GraphParameter> getGraphRequestParams() {
        return graphRequestParams;
    }

    /**
     * Gets an authenticated graph client.
     * 
     * @param config the global config object.
     * 
     * @return an authenticated graph client.
     */
    private GraphServiceClient<?> getAuthenticatedGraphClient(final Config config) {
        logger.info("Try to connect to graph appId MicrosoftAppId=[{}]", config.getMicrosoftAppId());
        final var clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(config.getMicrosoftAppId())
                .clientSecret(config.getMicrosoftAppPassword())
                .tenantId(config.getMicrosoftTenantId())
                .build();

        final var tokenCredentialAuthProvider = new TokenCredentialAuthProvider(SCOPES, clientSecretCredential);

        return GraphServiceClient
                .builder()
                .authenticationProvider(tokenCredentialAuthProvider)
                .buildClient();
    }

    /**
     * Returns available fields for Graph API calls, which are some of the fields described
     * <a href="https://learn.microsoft.com/en-us/powershell/module/microsoft.graph.users/update-mguser">here</a>. 
     *
     * @param requestParams a comma-separated list of additional user-related Microsoft Graph parameters (CSV) to be sent to Teneo engine.
     *
     * @return Available fields for Graph API calls.
     */
    private List<GraphParameter> parseGraphRequestParams(String requestParams) {
        var parts = requestParams.split("\\s*,\\s*");
        var result = new ArrayList<GraphParameter>(parts.length);
        for (String part : parts) {
            var p = GraphParameter.findByName(part);
            if (p != null) {
                result.add(p);
            } else {
                logger.warn("Parameter with name {} not found", part);
            }
        }
        return result;
    }
}
