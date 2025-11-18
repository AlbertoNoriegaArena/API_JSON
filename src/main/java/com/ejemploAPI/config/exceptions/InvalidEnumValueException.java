package com.ejemploAPI.config.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEnumValueException extends RuntimeException {

    private final String attributeName;
    private final String invalidValue;
    private final Iterable<String> allowedValues;

    public InvalidEnumValueException(String attributeName, String invalidValue, Iterable<String> allowedValues) {
        super("Valor '" + invalidValue + "' no permitido para enum del atributo '" + attributeName + "'");
        this.attributeName = attributeName;
        this.invalidValue = invalidValue;
        this.allowedValues = allowedValues;
    }

    public String getAttributeName() { return attributeName; }
    public String getInvalidValue() { return invalidValue; }
    public Iterable<String> getAllowedValues() { return allowedValues; }
}

