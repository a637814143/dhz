package com.example.silkmall.dto;

public class CaptchaChallengeDTO {
    private String challengeId;
    private String question;
    private long expiresIn;

    public CaptchaChallengeDTO() {
    }

    public CaptchaChallengeDTO(String challengeId, String question, long expiresIn) {
        this.challengeId = challengeId;
        this.question = question;
        this.expiresIn = expiresIn;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
