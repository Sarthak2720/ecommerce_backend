package com.styliste.controller;

import com.styliste.dto.*;
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

@Slf4j
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileStorageService fileStorageService;

//    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }) // 👈 Tell Spring this is a File Upload
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ProductDTO> createProduct(
//            @RequestPart("product") @Valid CreateProductRequest request, // 👈 Look for the "product" part
//            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles // 👈 Look for the files
//    ) {
//        log.info("Creating new product with {} images", imageFiles != null ? imageFiles.size() : 0);
//
//        // 1. Save files to disk and get their paths
//        List<String> savedImagePaths = new ArrayList<>();
//        if (imageFiles != null) {
//            for (MultipartFile file : imageFiles) {
//                String path = fileStorageService.saveFile(file); // From the FileStorageService we created
//                savedImagePaths.add(path);
//            }
//        }
//
//        // 2. Set the generated paths into the request object
//        request.setImages(savedImagePaths);
//
//        // 3. Pass to service as usual
//        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
//    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("Fetching product with ID: {}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);
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
