package vn.edu.fpt.service;

import vn.edu.fpt.exception.InsufficientStockException;
import vn.edu.fpt.exception.ProductNotFoundException;
import vn.edu.fpt.model.Product;
import vn.edu.fpt.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(String sku, String name, String category, BigDecimal price, int stockQuantity) {
        Product product = new Product(null, sku, name, category, price, stockQuantity);
        productRepository.save(product);
        return product;
    }

    public List<Product> listAll() {
        return productRepository.findAll();
    }

    public Product findBySku(String sku) {
        return productRepository.findAll().stream()
                .filter(p -> p.getSku().equalsIgnoreCase(sku))
                .findFirst()
                .orElseThrow(() -> new ProductNotFoundException("No product found with SKU: " + sku));
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("No product found with id: " + id));
    }

    public void updateProduct(Product product) {
        productRepository.update(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public void ensureStockAvailable(Product product, int requestedQty) {
        if (product.getStockQuantity() < requestedQty) {
            throw new InsufficientStockException(
                    "Not enough stock for " + product.getSku() +
                            " (requested " + requestedQty + ", available " + product.getStockQuantity() + ")");
        }
    }

    public List<Product> lowStockProducts(int threshold) {
        return productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() < threshold)
                .sorted(Comparator.comparingInt(Product::getStockQuantity))
                .collect(Collectors.toList());
    }
}