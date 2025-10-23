package com.cronox.shop.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long id) {
        super("Product %d would have negative stock".formatted(id));
    }
}
