package com.styliste.service;

import com.styliste.dto.*;
import com.styliste.entity.Product;
import com.styliste.entity.ProductAttribute;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public ProductDTO createProduct(CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());

        if (request.getSalePrice() != null &&
                request.getSalePrice().compareTo(request.getPrice()) > 0) {
            throw new BadRequestException("Sale price cannot be greater than regular price");
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .subcategory(request.getSubcategory())
                .images(request.getImages())
                .videos(request.getVideos())
                .attributes(mapAttributeDTOsToEntities(request.getAttributes()))
                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());
        return mapToDTO(savedProduct);
    }

    public ProductDTO getProductById(Long id) {
        log.debug("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToDTO(product);
    }

    public ProductDTO updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getSalePrice() != null) product.setSalePrice(request.getSalePrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getSubcategory() != null) product.setSubcategory(request.getSubcategory());
        if (request.getImages() != null) product.setImages(request.getImages());
        if (request.getVideos() != null) product.setVideos(request.getVideos());
        if (request.getAttributes() != null)
            product.setAttributes(mapAttributeDTOsToEntities(request.getAttributes()));
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully");
        return mapToDTO(updatedProduct);
    }

    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        productRepository.delete(product);
        log.info("Product deleted successfully");
    }

    public void softDeleteProduct(Long id) {
        log.info("Soft deleting product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setIsActive(false);
        productRepository.save(product);
        log.info("Product soft deleted successfully");
    }

    public void activateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setIsActive(true);
        productRepository.save(product);
    }

    public Page<ProductDTO> searchProducts(ProductFilterRequest filterRequest) {
        log.debug("Searching products with filters: {}", filterRequest);

        int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
        int pageSize = filterRequest.getPageSize() != null ? filterRequest.getPageSize() : 12;

        Sort.Direction direction = Sort.Direction.DESC;
        if (filterRequest.getSortOrder() != null &&
                filterRequest.getSortOrder().equalsIgnoreCase("ASC")) {
            direction = Sort.Direction.ASC;
        }

        String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "createdAt";
        Sort sort = Sort.by(direction, sortBy);

        Pageable pageable = PageRequest.of(page, pageSize, sort);

        Page<Product> products = productRepository.searchProducts(
                filterRequest.getCategory(),
                filterRequest.getSubcategory(),
                filterRequest.getMinPrice(),
                filterRequest.getMaxPrice(),
                filterRequest.getSearchQuery(),
                pageable
        );

        return products.map(this::mapToDTO);
    }

    public Page<ProductDTO> getAllProducts(Integer page, Integer pageSize) {
        log.debug("Fetching all products");

        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 12;

        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("createdAt").descending());
        return productRepository.findAll(pageable).map(this::mapToDTO);
    }

    public List<ProductDTO> getProductsByCategory(String category) {
        log.debug("Fetching products by category: {}", category);
        return productRepository.findByCategory(category).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getProductsBySubcategory(String subcategory) {
        log.debug("Fetching products by subcategory: {}", subcategory);
        return productRepository.findBySubcategory(subcategory).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .subcategory(product.getSubcategory())
                .images(product.getImages())
                .videos(product.getVideos())
                .attributes(mapEntitiesToAttributeDTOs(product.getAttributes()))
                .isActive(product.getIsActive())
                .build();
    }

    private List<ProductAttributeDTO> mapEntitiesToAttributeDTOs(List<ProductAttribute> attributes) {
        if (attributes == null) return null;
        return attributes.stream()
                .map(attr -> ProductAttributeDTO.builder()
                        .type(attr.getType())
                        .value(attr.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductAttribute> mapAttributeDTOsToEntities(List<ProductAttributeDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                .map(dto -> new ProductAttribute(dto.getType(), dto.getValue()))
                .collect(Collectors.toList());
    }
}
