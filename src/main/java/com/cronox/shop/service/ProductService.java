package com.cronox.shop.service;

import com.cronox.shop.dto.PagedResponse;
import com.cronox.shop.dto.ProductRequest;
import com.cronox.shop.dto.ProductResponse;
import com.cronox.shop.entity.Product;
import com.cronox.shop.exception.DuplicateSkuException;
import com.cronox.shop.exception.InsufficientStockException;
import com.cronox.shop.exception.ProductNotFoundException;
import com.cronox.shop.mapper.ProductMapper;
import com.cronox.shop.repository.ProductRepository;
import com.cronox.shop.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> listProducts(int page, int size, String name, String sku) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<Product> spec = Specification.where(ProductSpecifications.nameContains(name))
                .and(ProductSpecifications.skuEquals(sku));
        Page<ProductResponse> productPage = productRepository.findAll(spec, pageable)
                .map(productMapper::toResponse);
        return new PagedResponse<>(productPage.getContent(), productPage.getNumber(), productPage.getSize(),
                productPage.getTotalElements(), productPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new DuplicateSkuException(request.getSku());
        }
        productMapper.updateEntity(product, request);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse upsertBySku(ProductRequest request) {
        return productRepository.findBySku(request.getSku())
                .map(existing -> {
                    productMapper.updateEntity(existing, request);
                    return productMapper.toResponse(productRepository.save(existing));
                })
                .orElseGet(() -> {
                    Product created = productRepository.save(productMapper.toEntity(request));
                    return productMapper.toResponse(created);
                });
    }

    @Transactional
    public ProductResponse adjustQuantity(Long id, int delta) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        int newQuantity = product.getQuantity() + delta;
        if (newQuantity < 0) {
            throw new InsufficientStockException(id);
        }
        product.setQuantity(newQuantity);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}
