package com.artificialsolutions.teamsconnector;


/**
 * Specifies the status of a Teneo engine response.
 */
public class TeneoResponseStatus {

    /**
     * A flag indication if the response is "failed".
     */
    private boolean failed;

    /**
     * A flag indicating if the failure was due to a Teneo error.
     */
    private boolean teneoError;

    /**
     * The error message.
     */
    private String errorMessage;

    /**
     * Returns {@code true} if the response is "failed" and {@code false} otherwise.
     * 
     * @return {@code true} if the response is "failed" and {@code false} otherwise.
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Returns {@code true} if the response is "failed" and the failure is of Teneo engine; {@code false} otherwise.
     * 
     * @return {@code true} if the response is "failed" and the failure is of Teneo engine; {@code false} otherwise.
     */
    public boolean isTeneoError() {
        return teneoError;
    }

    /**
     * Sets an error message.
     * 
     * @param errorMessage the error message.
     */
    public void setErrorMessage(String errorMessage) {
        this.failed = true;
        this.errorMessage = errorMessage;
    }

    /**
     * Sets an error message as originating in Teneo engine.
     * 
     * @param errorMessage the error message.
     */
    public void setErrorMessageFromTeneo(String errorMessage) {
        this.failed = true;
        this.teneoError = true;
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the error message.
     * 
     * @return The error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
