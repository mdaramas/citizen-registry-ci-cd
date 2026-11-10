package com.citizenregistry.service.exception;

public class CitizenAlreadyExistsException extends RuntimeException {
    public CitizenAlreadyExistsException(String message) {
        super(message);
    }
}
