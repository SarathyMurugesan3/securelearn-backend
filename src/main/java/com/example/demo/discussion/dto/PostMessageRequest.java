package com.example.demo.discussion.dto;

public class PostMessageRequest {

    private String threadId;
    private String parentMessageId; // null = top-level message; non-null = nested reply
    private String message;

    public PostMessageRequest() {
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(String parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
