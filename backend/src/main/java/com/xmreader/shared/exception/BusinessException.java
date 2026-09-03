package com.xmreader.shared.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final int code;
    private final String type;

    public BusinessException(HttpStatus status, int code, String type, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.type = type;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
