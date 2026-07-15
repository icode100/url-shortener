package com.service.url_shortener.exception;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String code) {
        super("No link exists for code '" + code + "'");
    }
}
