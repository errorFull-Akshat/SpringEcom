package com.akshat.SpringEcom.service;

import com.akshat.SpringEcom.model.Product;
import com.akshat.SpringEcom.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        return productRepo.searchProducts(keyword);
    }

    public Product getProductById(int id) {
        return productRepo.findById(id).orElse(null);
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Product addOrUpdateProduct(Product product, MultipartFile imageFile) throws IOException {
        product.setImageName(imageFile.getOriginalFilename());
        product.setImageData(imageFile.getBytes());
        product.setImageType(imageFile.getContentType());

        // Restocking an out-of-stock product makes it available again
        if (product.getStockQuantity() > 0) {
            productRepo.findById(product.getId())
                    .filter(existing -> existing.getStockQuantity() == 0)
                    .ifPresent(existing -> product.setProductAvailable(true));
        }
        return productRepo.save(product);
    }

    public void deleteProduct(int id) {
        productRepo.deleteById(id);
    }
}
