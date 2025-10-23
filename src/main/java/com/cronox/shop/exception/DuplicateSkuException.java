package com.cronox.shop.exception;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("A product with sku '%s' already exists".formatted(sku));
    }
}
