package com.cronox.shop.repository;

import com.cronox.shop.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) -> name == null || name.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> skuEquals(String sku) {
        return (root, query, cb) -> sku == null || sku.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("sku"), sku);
    }
}
