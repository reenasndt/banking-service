package com.dws.challenge.exception;

public class InvalidTransferRequestException extends RuntimeException {
    public InvalidTransferRequestException(String message) {
        super(message);
    }
}
