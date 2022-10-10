package com.artificialsolutions.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.artificialsolutions.teamsconnector.Config;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import org.slf4j.Logger;

public class GraphClient {
    private static final List<String> SCOPES = Arrays.asList(new String[] { "https://graph.microsoft.com/.default" });

    private final Logger logger;
    private final GraphServiceClient client;
    private final ObjectMapper mapper;
    private final String requestParamsValue;
    private final List<GraphParameter> graphRequestParams;

    public GraphClient(final Config config, final Logger logger, final ObjectMapper mapper) {
        super();
        this.logger = logger;
        this.client = getAuthenticatedGraphClient(config);
        this.mapper = mapper;
        this.requestParamsValue = config.getMicrosoftGraphRequestParams();
        this.graphRequestParams = parseGraphRequestParams(config.getMicrosoftGraphRequestParams());
    }

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

    public List<GraphParameter> getGraphRequestParams() {
        return graphRequestParams;
    }

    private GraphServiceClient getAuthenticatedGraphClient(final Config config) {
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

    private List<GraphParameter> parseGraphRequestParams(String requestParams) {
        var result = new ArrayList<GraphParameter>();
        var parts = requestParams.split(",");
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
