package com.example.blog.common.idempotency;

import java.io.Serializable;

public class IdempotencyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    private Status status;
    private int statusCode;
    private Object responseBody;

    public IdempotencyRecord() {}

    public static IdempotencyRecord inProgress() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.status = Status.IN_PROGRESS;
        return record;
    }

    public static IdempotencyRecord completed(int statusCode, Object responseBody) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.status = Status.COMPLETED;
        record.statusCode = statusCode;
        record.responseBody = responseBody;
        return record;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public Object getResponseBody() { return responseBody; }
    public void setResponseBody(Object responseBody) { this.responseBody = responseBody; }
}
