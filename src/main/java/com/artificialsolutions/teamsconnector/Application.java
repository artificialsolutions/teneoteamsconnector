// Licensed under the MIT License, based on a Microsoft template code.

package com.artificialsolutions.teamsconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.bot.builder.Bot;
import com.microsoft.bot.integration.AdapterWithErrorHandler;
import com.microsoft.bot.integration.BotFrameworkHttpAdapter;
import com.microsoft.bot.integration.Configuration;
import com.microsoft.bot.integration.spring.BotController;
import com.microsoft.bot.integration.spring.BotDependencyConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
// This is the starting point of the Sprint Boot Bot application.
//
@SpringBootApplication

// Use the default BotController to receive incoming Channel messages. A custom
// controller could be used by eliminating this import and creating a new
// org.springframework.web.bind.annotation.RestController.
// The default controller is created by the Spring Boot container using
// dependency injection. The default route is /api/messages.
@Import({ BotController.class })

/**
 * This class extends the BotDependencyConfiguration which provides the default implementations for
 * a Bot application. The Application class should override methods in order to provide custom
 * implementations.
 */
public class Application extends BotDependencyConfiguration {

    final Logger logger = LoggerFactory.getLogger(Application.class);

    @Autowired
    private Config config;
    
    @Autowired
    ObjectMapper mapper;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @Override
    public Configuration getConfiguration() {
        return new EnvironmentPropertiesConfiguration(config);
    }

    /**
     * Returns the Bot for this application. The @Component annotation could be used on the Bot class
     * instead of this method with the @Bean annotation.
     *
     * @return The Bot implementation for this application.
     */
    @Bean
    public Bot getBot() {
        if (logger.isDebugEnabled()) {
            logger.info("Reading config:");
            logger.info("microsoftAppId [{}]", config.getMicrosoftAppId());
            logger.info("microsoftTenantId [{}]", config.getMicrosoftTenantId());
            logger.info("teneoEngineEndpointUri [{}]", config.getTeneoEngineEndpointUri());
            logger.info("teneoEngineConnectTimeoutMillis [{}]", config.getTeneoEngineConnectTimeoutMillis());
            logger.info("teneoEngineResponseTimeoutMillis [{}]", config.getTeneoEngineResponseTimeoutMillis());
            logger.info("bridgeSessionTimeoutMillis [{}]", config.getBridgeSessionTimeoutMillis());
            logger.info("maxParallelSessions [{}]", config.getMaxParallelSessions());
            logger.info("isExplicitData [{}]", config.isExplicitData());
        }

        return new TeneoBot(config, mapper);
    }

    /**
     * Returns a custom Adapter that provides error handling.
     *
     * @param configuration The Configuration object to use.
     * 
     * @return An error handling BotFrameworkHttpAdapter.
     */
    @Override
    public BotFrameworkHttpAdapter getBotFrameworkHttpAdaptor(final Configuration configuration) {
        return new AdapterWithErrorHandler(configuration);
    }
}
