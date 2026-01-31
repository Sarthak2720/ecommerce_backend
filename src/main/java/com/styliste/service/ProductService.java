package com.styliste.service;

import com.styliste.dto.*;
import com.styliste.entity.Attribute;
import com.styliste.entity.Product;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.AttributeRepository;
import com.styliste.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AttributeRepository attributeRepository;


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
                .attributes(request.getAttributes() != null ? syncAttributes(request.getAttributes()) : new ArrayList<>())                .isActive(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());
        return mapToDTO(savedProduct);
    }

    public Map<String, List<String>> getProductFilters() {
        log.info("Fetching all active product filters");
        List<Attribute> allAttributes = attributeRepository.findAll();

        // Grouping: { "COLOR": ["Red", "Blue"], "SIZE": ["M", "L"] }
        return allAttributes.stream()
                .collect(Collectors.groupingBy(
                        Attribute::getType,
                        Collectors.mapping(Attribute::getValue, Collectors.toList())
                ));
    }

    private List<Attribute> syncAttributes(List<ProductAttributeDTO> dtos) {
        if (dtos == null) return new ArrayList<>();
        return dtos.stream().map(dto -> {
            // Check if this Color/Size combination already exists in our master table
            return attributeRepository.findByTypeAndValue(dto.getType(), dto.getValue())
                    .orElseGet(() -> attributeRepository.save(
                            Attribute.builder()
                                    .type(dto.getType())
                                    .value(dto.getValue())
                                    .build()
                    ));
        }).collect(Collectors.toList());
    }
    private void cleanupOrphanAttributes(List<Attribute> attributesToCheck) {
        if (attributesToCheck == null) return;
        for (Attribute attr : attributesToCheck) {
            // Check if any product is still using this attribute
            long count = attributeRepository.countProductsUsingAttribute(attr.getId());
            if (count == 0) {
                log.info("Deleting unused attribute: {} - {}", attr.getType(), attr.getValue());
                attributeRepository.delete(attr);
            }
        }
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

        // Keep track of old attributes to check for orphans later
        List<Attribute> oldAttributes = new ArrayList<>(product.getAttributes());

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getSalePrice() != null) product.setSalePrice(request.getSalePrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getSubcategory() != null) product.setSubcategory(request.getSubcategory());
        if (request.getImages() != null) product.setImages(request.getImages());
        if (request.getVideos() != null) product.setVideos(request.getVideos());

        // Update attributes if provided
        if (request.getAttributes() != null) {
            product.setAttributes(syncAttributes(request.getAttributes()));
        }

        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        Product updatedProduct = productRepository.save(product);

        // After saving, check if any of the old attributes are now "orphans"
        cleanupOrphanAttributes(oldAttributes);

        log.info("Product updated successfully");
        return mapToDTO(updatedProduct);
    }
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Capture the attributes before deleting the product
        List<Attribute> attributesToClean = new ArrayList<>(product.getAttributes());

        productRepository.delete(product);

        // Check if any of those attributes are now "orphans"
        cleanupOrphanAttributes(attributesToClean);
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

    private List<ProductAttributeDTO> mapEntitiesToAttributeDTOs(List<Attribute> attributes) {
        if (attributes == null) return new ArrayList<>();
        return attributes.stream()
                .map(attr -> ProductAttributeDTO.builder()
                        .type(attr.getType())
                        .value(attr.getValue())
                        .build())
                .collect(Collectors.toList());
    }

}
