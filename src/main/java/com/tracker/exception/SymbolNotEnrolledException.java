package com.tracker.exception;

import lombok.Getter;

@Getter
public class SymbolNotEnrolledException extends RuntimeException {

    private final String symbol;

    public SymbolNotEnrolledException(String symbol) {
        super(String.format("Symbol %s is not enrolled. Please enroll first using POST /api/v1/enroll/%s", symbol, symbol));
        this.symbol = symbol;
    }
}
