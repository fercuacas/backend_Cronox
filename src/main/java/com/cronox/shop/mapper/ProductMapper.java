package com.cronox.shop.mapper;

import com.cronox.shop.dto.ProductRequest;
import com.cronox.shop.dto.ProductResponse;
import com.cronox.shop.entity.Product;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPriceCents(request.getPriceCents());
        product.setQuantity(request.getQuantity());
        return product;
    }

    public void updateEntity(Product product, ProductRequest request) {
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPriceCents(request.getPriceCents());
        product.setQuantity(request.getQuantity());
    }

    public ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPriceCents(product.getPriceCents());
        response.setQuantity(product.getQuantity());
        response.setUpdatedAt(product.getUpdatedAt());
        return response;
    }

    public List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
