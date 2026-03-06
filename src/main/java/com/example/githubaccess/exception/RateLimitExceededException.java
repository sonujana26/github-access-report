package com.example.githubaccess.exception;

public class RateLimitExceededException extends RuntimeException {

    private final long resetEpochSeconds;

    public RateLimitExceededException(long resetEpochSeconds) {
        super("GitHub API rate limit exceeded. Please try again later.");
        this.resetEpochSeconds = resetEpochSeconds;
    }

    public long getResetEpochSeconds() {
        return resetEpochSeconds;
    }
}
