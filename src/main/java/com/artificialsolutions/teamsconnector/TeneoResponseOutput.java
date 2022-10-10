package com.artificialsolutions.teamsconnector;

public class TeneoResponseOutput {

    private String text;

    private String adaptiveCardContents;
    
    private String textSegmentIndexes;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAdaptiveCardContents() {
        return adaptiveCardContents;
    }

    public void setAdaptiveCardContents(String adaptiveCardContents) {
        this.adaptiveCardContents = adaptiveCardContents;
    }

    public String getTextSegmentIndexes() {
        return textSegmentIndexes;
    }

    public void setTextSegmentIndexes(String textSegmentIndexes) {
        this.textSegmentIndexes = textSegmentIndexes;
    }

}
