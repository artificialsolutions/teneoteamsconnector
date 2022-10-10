package com.artificialsolutions.teamsconnector;

public class TeneoResponseStatus {

    private boolean failed;

    private boolean fromTeneo;

    private String errorMessage;

    public boolean isFailed() {
        return failed;
    }

    public boolean isFromTeneo() {
        return fromTeneo;
    }

    public void setErrorMessage(String errorMessage) {
        this.failed = true;
        this.errorMessage = errorMessage;
    }

    public void setErrorMessageFromTeneo(String errorMessage) {
        this.failed = true;
        this.fromTeneo = true;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}


