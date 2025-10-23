package com.cronox.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cronox.shop.dto.ApiError;
import com.cronox.shop.dto.PagedResponse;
import com.cronox.shop.dto.ProductRequest;
import com.cronox.shop.dto.ProductResponse;
import com.cronox.shop.dto.QuantityAdjustmentRequest;
import com.cronox.shop.repository.ProductRepository;
import java.net.URI;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.2-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveProduct() {
        ProductRequest request = buildProductRequest("SKU-1", "Test Product", 1200, 10);
        ResponseEntity<ProductResponse> createResponse = restTemplate.postForEntity(baseUrl("/api/products"), request,
                ProductResponse.class);
        assertEquals(201, createResponse.getStatusCode().value());
        Long id = Objects.requireNonNull(createResponse.getBody()).getId();

        ResponseEntity<ProductResponse> getResponse = restTemplate.getForEntity(baseUrl("/api/products/" + id),
                ProductResponse.class);
        assertEquals(200, getResponse.getStatusCode().value());
        assertThat(Objects.requireNonNull(getResponse.getBody()).getSku()).isEqualTo("SKU-1");
    }

    @Test
    void shouldListWithPaginationAndFilters() {
        restTemplate.postForEntity(baseUrl("/api/products"), buildProductRequest("SKU-10", "Red Shirt", 2500, 5),
                ProductResponse.class);
        restTemplate.postForEntity(baseUrl("/api/products"), buildProductRequest("SKU-11", "Blue Pants", 3000, 8),
                ProductResponse.class);

        ResponseEntity<PagedResponse<ProductResponse>> response = restTemplate.exchange(
                baseUrl("/api/products?page=0&size=1&name=shirt"), HttpMethod.GET, null,
                new ParameterizedTypeReference<PagedResponse<ProductResponse>>() {
                });

        assertEquals(200, response.getStatusCode().value());
        PagedResponse<ProductResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getContent()).hasSize(1);
        assertThat(body.getTotalElements()).isEqualTo(1);

        ResponseEntity<PagedResponse<ProductResponse>> skuResponse = restTemplate.exchange(
                baseUrl("/api/products?sku=SKU-11"), HttpMethod.GET, null,
                new ParameterizedTypeReference<PagedResponse<ProductResponse>>() {
                });
        assertEquals(200, skuResponse.getStatusCode().value());
        assertThat(Objects.requireNonNull(skuResponse.getBody()).getContent()).extracting(ProductResponse::getSku)
                .containsExactly("SKU-11");
    }

    @Test
    void shouldRejectDuplicateSku() {
        ProductRequest request = buildProductRequest("SKU-20", "Hat", 1800, 2);
        restTemplate.postForEntity(baseUrl("/api/products"), request, ProductResponse.class);

        ResponseEntity<ApiError> duplicateResponse = restTemplate.postForEntity(baseUrl("/api/products"), request,
                ApiError.class);
        assertEquals(422, duplicateResponse.getStatusCode().value());
        assertThat(Objects.requireNonNull(duplicateResponse.getBody()).getMessage()).contains("already exists");
    }

    @Test
    void shouldAdjustQuantityAtomically() {
        ProductRequest request = buildProductRequest("SKU-30", "Laptop", 250000, 5);
        ResponseEntity<ProductResponse> createResponse = restTemplate.postForEntity(baseUrl("/api/products"), request,
                ProductResponse.class);
        Long id = Objects.requireNonNull(createResponse.getBody()).getId();

        QuantityAdjustmentRequest adjustmentRequest = new QuantityAdjustmentRequest();
        adjustmentRequest.setDelta(-2);
        RequestEntity<QuantityAdjustmentRequest> patchRequest = RequestEntity
                .patch(URI.create(baseUrl("/api/products/" + id + "/adjust-quantity")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(adjustmentRequest);
        ResponseEntity<ProductResponse> patchResponse = restTemplate.exchange(patchRequest, ProductResponse.class);
        assertEquals(200, patchResponse.getStatusCode().value());
        assertThat(Objects.requireNonNull(patchResponse.getBody()).getQuantity()).isEqualTo(3);
    }

    @Test
    void shouldDeleteProduct() {
        ResponseEntity<ProductResponse> createResponse = restTemplate.postForEntity(baseUrl("/api/products"),
                buildProductRequest("SKU-40", "Mouse", 5000, 15), ProductResponse.class);
        Long id = Objects.requireNonNull(createResponse.getBody()).getId();

        restTemplate.delete(baseUrl("/api/products/" + id));

        ResponseEntity<ApiError> getResponse = restTemplate.getForEntity(baseUrl("/api/products/" + id),
                ApiError.class);
        assertEquals(404, getResponse.getStatusCode().value());
    }

    private ProductRequest buildProductRequest(String sku, String name, int priceCents, int quantity) {
        ProductRequest request = new ProductRequest();
        request.setSku(sku);
        request.setName(name);
        request.setDescription(name + " description");
        request.setPriceCents(priceCents);
        request.setQuantity(quantity);
        return request;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
