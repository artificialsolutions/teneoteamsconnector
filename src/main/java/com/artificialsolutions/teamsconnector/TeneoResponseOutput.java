package com.artificialsolutions.teamsconnector;

/**
 * Specifies the content of a Teneo engine response.
 */
public class TeneoResponseOutput {

    /**
     * The response text to be displayed to the user.
     */
    private String text;

    /**
     * The value of the {@code msbotframework} output parameter of the Teneo engine response.
     */
    private String adaptiveCardContents;

    /**
     * The value of the {@code outputTextSegmentIndexes} output parameter of the Teneo engine response.
     */
    private String textSegmentIndexes;

    /**
     * Gets the response text, to be displayed to the user.
     * 
     * @return The response text to be displayed to the user.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the response text, to be displayed to the user.
     * 
     * @param text the response text, to be displayed to the user.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the value of the {@code msbotframework} output parameter of the Teneo engine response.
     * 
     * @return the value of the {@code msbotframework} output parameter of the Teneo engine response.
     */
    public String getAdaptiveCardContents() {
        return adaptiveCardContents;
    }

    /**
     * Sets the value of the {@code msbotframework} output parameter of the Teneo engine response.
     * 
     * @param adaptiveCardContents the value of the {@code msbotframework} output parameter of the Teneo engine
     * response.
     */
    public void setAdaptiveCardContents(String adaptiveCardContents) {
        this.adaptiveCardContents = adaptiveCardContents;
    }

    /**
     * Gets the value of the {@code outputTextSegmentIndexes} output parameter of the Teneo engine response.
     * 
     * @return the value of the {@code outputTextSegmentIndexes} output parameter of the Teneo engine response.
     */
    public String getTextSegmentIndexes() {
        return textSegmentIndexes;
    }

    /**
     * Sets the value of the {@code outputTextSegmentIndexes} output parameter of the Teneo engine response.
     * 
     * @param adaptiveCardContents the value of the {@code outputTextSegmentIndexes} output parameter of the Teneo
     * engine response.
     */
    public void setTextSegmentIndexes(String textSegmentIndexes) {
        this.textSegmentIndexes = textSegmentIndexes;
    }
}
