package com.stockpilot.io;

import com.stockpilot.model.Product;
import com.stockpilot.service.ProductService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CsvProductImporter {

    private static final Pattern CSV_SPLIT = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

    private final ProductService productService;

    public CsvProductImporter(ProductService productService) {
        this.productService = productService;
    }

    public ImportResult importFromFile(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            return ImportResult.fileNotFound(filePath);
        }

        List<Product> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            int lineNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                try {
                    Product product = parseLine(line);
                    productService.createProduct(
                            product.getSku(), product.getName(), product.getCategory(),
                            product.getPrice(), product.getStockQuantity());
                    imported.add(product);
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("Could not read file: " + e.getMessage());
        }

        return new ImportResult(imported, errors);
    }

    private Product parseLine(String line) {
        String[] fields = CSV_SPLIT.split(line, -1);
        if (fields.length < 5) {
            throw new IllegalArgumentException(
                    "expected 5 columns (sku,name,category,price,stock) but got " + fields.length);
        }

        String sku = unquote(fields[0]);
        String name = unquote(fields[1]);
        String category = unquote(fields[2]);
        BigDecimal price;
        int stock;
        try {
            price = new BigDecimal(unquote(fields[3]).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("price '" + fields[3] + "' is not a valid number");
        }
        try {
            stock = Integer.parseInt(unquote(fields[4]).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("stock '" + fields[4] + "' is not a valid whole number");
        }

        return new Product(null, sku, name, category, price, stock);
    }

    private String unquote(String field) {
        String trimmed = field.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static class ImportResult {
        private final List<Product> imported;
        private final List<String> errors;
        private final String fatalError;

        ImportResult(List<Product> imported, List<String> errors) {
            this.imported = imported;
            this.errors = errors;
            this.fatalError = null;
        }

        private ImportResult(String fatalError) {
            this.imported = List.of();
            this.errors = List.of();
            this.fatalError = fatalError;
        }

        static ImportResult fileNotFound(String path) {
            return new ImportResult("File not found: " + path);
        }

        public List<Product> getImported() {
            return imported;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getFatalError() {
            return fatalError;
        }
    }
}