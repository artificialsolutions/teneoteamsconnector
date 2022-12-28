package com.artificialsolutions.teamsconnector;

import java.util.Properties;

import com.microsoft.bot.integration.Configuration;

public class EnvironmentPropertiesConfiguration implements Configuration {

    /**
     * Holds the properties
     */
    private final Properties properties = new Properties();

    /**
     * Loads properties from environment variables.
     */
    public EnvironmentPropertiesConfiguration(Config config) {
        properties.setProperty("MicrosoftAppId", config.getMicrosoftAppId());
        properties.setProperty("MicrosoftAppPassword", config.getMicrosoftAppPassword());
    }

    /**
     * Returns a value for the specified property name.
     *
     * @param key The property name.
     * 
     * @return The property value.
     */
    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * @return The Properties value.
     */
    @Override
    public Properties getProperties() {
        return this.properties;
    }

    /**
     * Returns an array of values from an entry that is comma delimited.
     * 
     * @param key The property name.
     * 
     * @return The property values as a String array.
     */
    @Override
    public String[] getProperties(String key) {
        String baseProperty = properties.getProperty(key);
        if (baseProperty != null) {
            String[] splitProperties = baseProperty.split(",");
            return splitProperties;
        } else {
            return null;
        }
    }
}
