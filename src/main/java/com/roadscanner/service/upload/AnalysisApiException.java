package com.roadscanner.service.upload;

/**
 * Classifies failures returned by, or encountered while contacting, the image
 * analysis service without exposing its endpoint or response body.
 */
public final class AnalysisApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum FailureType {
        TIMEOUT,
        CONNECTION,
        SERVER_ERROR,
        RESPONSE_ERROR
    }

    private final FailureType failureType;
    private final int upstreamStatus;
    private final boolean retryable;

    private AnalysisApiException(FailureType failureType, String message, int upstreamStatus,
                                 boolean retryable, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.upstreamStatus = upstreamStatus;
        this.retryable = retryable;
    }

    public static AnalysisApiException timeout(Throwable cause) {
        return new AnalysisApiException(
                FailureType.TIMEOUT, "Analysis service timed out", 0, true, cause);
    }

    public static AnalysisApiException connection(Throwable cause) {
        return new AnalysisApiException(
                FailureType.CONNECTION, "Analysis service connection failed", 0, true, cause);
    }

    public static AnalysisApiException serverError(int upstreamStatus, Throwable cause) {
        return new AnalysisApiException(
                FailureType.SERVER_ERROR, "Analysis service returned a server error",
                upstreamStatus, true, cause);
    }

    public static AnalysisApiException responseError(int upstreamStatus, Throwable cause) {
        return new AnalysisApiException(
                FailureType.RESPONSE_ERROR, "Analysis service returned an unusable response",
                upstreamStatus, false, cause);
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
