package com.styliste.controller;

import com.styliste.dto.*;
import com.styliste.entity.Attribute;
import com.styliste.repository.AttributeRepository;
import com.styliste.service.FileStorageService;
import com.styliste.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AttributeRepository attributeRepository;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> createProduct(
            @RequestPart("product") @Valid CreateProductRequest request,
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "videoFiles", required = false) List<MultipartFile> videoFiles // 👈 Added video part
    ) {
        log.info("Creating product with {} images and {} videos",
                imageFiles != null ? imageFiles.size() : 0,
                videoFiles != null ? videoFiles.size() : 0);

        // 1. Process Images
        List<String> savedImagePaths = new ArrayList<>();
        if (imageFiles != null) {
            for (MultipartFile file : imageFiles) {
                savedImagePaths.add(fileStorageService.saveFile(file, "image"));
            }
        }
        request.setImages(savedImagePaths);

        // 2. Process Videos
        List<String> savedVideoPaths = new ArrayList<>();
        if (videoFiles != null) {
            for (MultipartFile file : videoFiles) {
                savedVideoPaths.add(fileStorageService.saveFile(file, "video")); // 👈 Save to video folder
            }
        }
        request.setVideos(savedVideoPaths);

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }


    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> getFilters() {
        return ResponseEntity.ok(productService.getProductFilters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("Fetching product with ID: {}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") @Valid UpdateProductRequest request, // Use @RequestPart
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "videoFiles", required = false) List<MultipartFile> videoFiles
    ) {
        // 1. Process new images
        List<String> newImagePaths = new ArrayList<>();
        if (imageFiles != null) {
            for (MultipartFile file : imageFiles) {
                newImagePaths.add(fileStorageService.saveFile(file, "image"));
            }
        }
        // Add new images to the existing images list sent from frontend
        if (request.getImages() == null) request.setImages(new ArrayList<>());
        request.getImages().addAll(newImagePaths);

        // 2. Process new videos
        List<String> newVideoPaths = new ArrayList<>();
        if (videoFiles != null) {
            for (MultipartFile file : videoFiles) {
                newVideoPaths.add(fileStorageService.saveFile(file, "video"));
            }
        }
        if (request.getVideos() == null) request.setVideos(new ArrayList<>());
        request.getVideos().addAll(newVideoPaths);

        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product with ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivateProduct(@PathVariable Long id) {
        log.info("Deactivating product with ID: {}", id);
        productService.softDeleteProduct(id);
        return ResponseEntity.ok("Product deactivated successfully");
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateProduct(@PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        log.info("Fetching all products");
        return ResponseEntity.ok(productService.getAllProducts(page, pageSize));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchProducts(@RequestBody ProductFilterRequest filterRequest) {
        log.info("Searching products with filters");
        return ResponseEntity.ok(productService.searchProducts(filterRequest));
    }



    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        log.info("Fetching products by category: {}", category);
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @GetMapping("/subcategory/{subcategory}")
    public ResponseEntity<List<ProductDTO>> getProductsBySubcategory(@PathVariable String subcategory) {
        log.info("Fetching products by subcategory: {}", subcategory);
        return ResponseEntity.ok(productService.getProductsBySubcategory(subcategory));
    }



}
