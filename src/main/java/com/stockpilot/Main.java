package com.stockpilot;

import com.stockpilot.concurrent.FlashSaleSimulator;
import com.stockpilot.exception.DataAccessException;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.InvalidInputException;
import com.stockpilot.exception.ProductNotFoundException;
import com.stockpilot.io.CsvProductImporter;
import com.stockpilot.io.InvoiceExporter;
import com.stockpilot.io.ReportExporter;
import com.stockpilot.model.Customer;
import com.stockpilot.model.Order;
import com.stockpilot.model.Product;
import com.stockpilot.repository.CustomerRepository;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.service.CustomerService;
import com.stockpilot.service.OrderService;
import com.stockpilot.service.ProductService;
import com.stockpilot.service.ReportService;
import com.stockpilot.service.pricing.BulkDiscount;
import com.stockpilot.service.pricing.NoDiscount;
import com.stockpilot.service.pricing.PercentageDiscount;
import com.stockpilot.service.pricing.PricingRule;
import com.stockpilot.util.DatabaseConnection;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== INITIALIZING STOCKPILOT SYSTEM ===");
        try {
            DatabaseConnection.initializeDatabase();
        } catch (DataAccessException e) {
            System.out.println("Fatal error: could not initialize the database - " + e.getMessage());
            System.out.println("Check that data/ is writable and schema.sql is present, then try again.");
            return;
        }

        ProductRepository productRepository = new ProductRepository();
        CustomerRepository customerRepository = new CustomerRepository();
        OrderRepository orderRepository = new OrderRepository();

        ProductService productService = new ProductService(productRepository);
        CustomerService customerService = new CustomerService(customerRepository);
        OrderService orderService = new OrderService(orderRepository, productService, customerService);
        ReportService reportService = new ReportService(orderRepository, productRepository);

        CsvProductImporter csvImporter = new CsvProductImporter(productService);
        InvoiceExporter invoiceExporter = new InvoiceExporter(productService);
        ReportExporter reportExporter = new ReportExporter();
        FlashSaleSimulator flashSaleSimulator = new FlashSaleSimulator(productRepository);

        mainMenuLoop(productService, customerService, orderService, reportService,
                csvImporter, invoiceExporter, reportExporter, flashSaleSimulator);
    }

    private static void mainMenuLoop(ProductService productService, CustomerService customerService,
                                     OrderService orderService, ReportService reportService,
                                     CsvProductImporter csvImporter, InvoiceExporter invoiceExporter,
                                     ReportExporter reportExporter, FlashSaleSimulator flashSaleSimulator) {
        while (true) {
            System.out.println("\n=========================================");
            System.out.println("       STOCKPILOT MANAGEMENT MENU        ");
            System.out.println("=========================================");
            System.out.println("1. Product Management");
            System.out.println("2. Customer Management");
            System.out.println("3. Place Order");
            System.out.println("4. Reports & Analytics");
            System.out.println("5. Flash Sale Simulation (F6 demo)");
            System.out.println("6. Exit System");

            int choice = readInt("Please enter your choice (1-6): ");

            switch (choice) {
                case 1 -> productMenu(productService, csvImporter);
                case 2 -> customerMenu(customerService);
                case 3 -> placeOrderFlow(orderService, customerService, productService, invoiceExporter);
                case 4 -> reportsMenu(reportService, reportExporter);
                case 5 -> flashSaleFlow(productService, flashSaleSimulator);
                case 6 -> {
                    System.out.println("\nClosing connections... Thank you for using StockPilot!");
                    scanner.close();
                    System.exit(0);
                }
                default -> System.out.println("Error: Invalid choice. Please choose a number between 1 and 6.");
            }
        }
    }


    private static void productMenu(ProductService productService, CsvProductImporter csvImporter) {
        while (true) {
            System.out.println("\n----------- PRODUCT MANAGEMENT -----------");
            System.out.println("1. Create product");
            System.out.println("2. List all products");
            System.out.println("3. Find product by id");
            System.out.println("4. Update product");
            System.out.println("5. Adjust stock quantity");
            System.out.println("6. Delete product");
            System.out.println("7. Import products from CSV");
            System.out.println("0. Back to main menu");

            int choice = readInt("Choice: ");
            try {
                switch (choice) {
                    case 1 -> createProductFlow(productService);
                    case 2 -> listProducts(productService);
                    case 3 -> findProductFlow(productService);
                    case 4 -> updateProductFlow(productService);
                    case 5 -> adjustStockFlow(productService);
                    case 6 -> deleteProductFlow(productService);
                    case 7 -> importProductsFlow(csvImporter);
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println("Error: Invalid choice.");
                }
            } catch (InvalidInputException | ProductNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static void createProductFlow(ProductService productService) {
        String sku = readLine("SKU (format AAA-1234): ");
        String name = readLine("Name: ");
        String category = readLine("Category: ");
        BigDecimal price = readBigDecimal("Price: ");
        int stock = readInt("Initial stock quantity: ");

        Product product = productService.createProduct(sku, name, category, price, stock);
        System.out.println("Created: " + product);
    }

    private static void listProducts(ProductService productService) {
        System.out.println("Sort by: 1) id (default)  2) name  3) price");
        int sortChoice = readInt("Choice: ");

        List<Product> products = switch (sortChoice) {
            case 2 -> productService.listSortedByName();
            case 3 -> productService.listSortedByPriceAsc();
            default -> productService.listAll();
        };

        if (products.isEmpty()) {
            System.out.println("No products yet.");
            return;
        }
        products.forEach(System.out::println);
    }

    private static void importProductsFlow(CsvProductImporter csvImporter) {
        String path = readLine("CSV file path (e.g. products.csv): ");
        CsvProductImporter.ImportResult result = csvImporter.importFromFile(path);

        if (result.getFatalError() != null) {
            System.out.println("Error: " + result.getFatalError());
            return;
        }

        System.out.println("Imported " + result.getImported().size() + " product(s).");
        if (!result.getErrors().isEmpty()) {
            System.out.println("Skipped " + result.getErrors().size() + " invalid line(s):");
            result.getErrors().forEach(err -> System.out.println("  - " + err));
        }
    }

    private static void findProductFlow(ProductService productService) {
        long id = readLong("Product id: ");
        Product product = productService.findById(id);
        System.out.println(product);
    }

    private static void updateProductFlow(ProductService productService) {
        long id = readLong("Product id to update: ");
        Product existing = productService.findById(id);
        System.out.println("Current: " + existing);
        System.out.println("Enter new values (leave blank to keep the current value):");

        String sku = readLineOrDefault("SKU [" + existing.getSku() + "]: ", existing.getSku());
        String name = readLineOrDefault("Name [" + existing.getName() + "]: ", existing.getName());
        String category = readLineOrDefault("Category [" + existing.getCategory() + "]: ", existing.getCategory());
        BigDecimal price = readBigDecimalOrDefault("Price [" + existing.getPrice() + "]: ", existing.getPrice());
        int stock = readIntOrDefault("Stock [" + existing.getStockQuantity() + "]: ", existing.getStockQuantity());

        Product updated = new Product(existing.getId(), sku, name, category, price, stock);
        productService.updateProduct(updated);
        System.out.println("Updated: " + updated);
    }

    private static void adjustStockFlow(ProductService productService) {
        long id = readLong("Product id: ");
        Product existing = productService.findById(id);
        System.out.println("Current stock for " + existing.getSku() + ": " + existing.getStockQuantity());
        int delta = readInt("Enter quantity change (+ to add stock, - to remove): ");

        int newStock = existing.getStockQuantity() + delta;
        if (newStock < 0) {
            System.out.println("Error: resulting stock cannot be negative (would be " + newStock + ").");
            return;
        }
        Product updated = new Product(existing.getId(), existing.getSku(), existing.getName(),
                existing.getCategory(), existing.getPrice(), newStock);
        productService.updateProduct(updated);
        System.out.println("New stock for " + updated.getSku() + ": " + updated.getStockQuantity());
    }

    private static void deleteProductFlow(ProductService productService) {
        long id = readLong("Product id to delete: ");
        Product existing = productService.findById(id);
        String confirm = readLine("Delete " + existing.getSku() + " - " + existing.getName() + "? (yes/no): ");
        if (confirm.equalsIgnoreCase("yes")) {
            productService.deleteProduct(id);
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static void customerMenu(CustomerService customerService) {
        while (true) {
            System.out.println("\n----------- CUSTOMER MANAGEMENT -----------");
            System.out.println("1. Add customer");
            System.out.println("2. List all customers");
            System.out.println("3. Find customer by id");
            System.out.println("0. Back to main menu");

            int choice = readInt("Choice: ");
            try {
                switch (choice) {
                    case 1 -> {
                        String name = readLine("Name: ");
                        String email = readLine("Email: ");
                        String phone = readLine("Phone (10-11 digits): ");
                        Customer customer = customerService.createCustomer(name, email, phone);
                        System.out.println("Created: " + customer);
                    }
                    case 2 -> {
                        List<Customer> customers = customerService.listAll();
                        if (customers.isEmpty()) {
                            System.out.println("No customers yet.");
                        } else {
                            customers.forEach(System.out::println);
                        }
                    }
                    case 3 -> {
                        long id = readLong("Customer id: ");
                        System.out.println(customerService.findById(id));
                    }
                    case 0 -> {
                        return;
                    }
                    default -> System.out.println("Error: Invalid choice.");
                }
            } catch (InvalidInputException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }


    private static void placeOrderFlow(OrderService orderService, CustomerService customerService,
                                       ProductService productService, InvoiceExporter invoiceExporter) {
        System.out.println("\n----------- PLACE ORDER -----------");
        List<Customer> customers = customerService.listAll();
        if (customers.isEmpty()) {
            System.out.println("No customers registered yet. Add one first (Customer Management > Add customer).");
            return;
        }
        customers.forEach(System.out::println);
        long customerId = readLong("Customer id: ");

        Map<String, Integer> cart = new LinkedHashMap<>();
        System.out.println("Add products to the cart. Enter an empty SKU to finish.");
        while (true) {
            String sku = readLine("SKU (blank to finish): ");
            if (sku.isBlank()) {
                break;
            }
            try {
                Product product = productService.findBySku(sku);
                int qty = readInt("Quantity for " + product.getName() + " (in stock: " + product.getStockQuantity() + "): ");
                cart.merge(product.getSku(), qty, Integer::sum);
                System.out.println("Added to cart.");
            } catch (ProductNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        if (cart.isEmpty()) {
            System.out.println("Cart is empty, order cancelled.");
            return;
        }

        PricingRule discountRule = chooseDiscountRule();

        try {
            Order order = orderService.placeOrder(customerId, cart, discountRule);
            printInvoice(order, productService);
            offerInvoiceExport(order, invoiceExporter);
        } catch (InsufficientStockException | InvalidInputException e) {
            System.out.println("Error: order could not be placed - " + e.getMessage());
        } catch (DataAccessException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static PricingRule chooseDiscountRule() {
        System.out.println("Discount: 1) None  2) Percentage off everything  3) Bulk discount (qty threshold)"
                + "  4) Custom: flat amount off orders above a total");
        int choice = readInt("Choice: ");
        try {
            return switch (choice) {
                case 2 -> {
                    BigDecimal percent = readBigDecimal("Discount percentage (e.g. 10 for 10%): ");
                    yield new PercentageDiscount(percent);
                }
                case 3 -> {
                    int minQty = readInt("Minimum total quantity to qualify: ");
                    BigDecimal percent = readBigDecimal("Discount percentage once qualified: ");
                    yield new BulkDiscount(minQty, percent);
                }
                case 4 -> {
                    BigDecimal minTotal = readBigDecimal("Order subtotal threshold: ");
                    BigDecimal flatOff = readBigDecimal("Flat amount to take off once threshold is met: ");
                    // Ad-hoc rule written as a lambda instead of a DiscountPolicy subclass.
                    yield (PricingRule) order -> order.getSubtotal().compareTo(minTotal) >= 0
                            ? flatOff
                            : BigDecimal.ZERO;
                }
                default -> new NoDiscount();
            };
        } catch (InvalidInputException e) {
            System.out.println("Error: " + e.getMessage() + " - no discount applied.");
            return new NoDiscount();
        }
    }

    private static void printInvoice(Order order, ProductService productService) {
        System.out.println("\n========== INVOICE ==========");
        System.out.println(order);
        order.getItems().forEach(item -> {
            String label = "Product #" + item.getProductId();
            try {
                label = productService.findById(item.getProductId()).getSku();
            } catch (Exception ignored) {
                // fall back to the raw id if the product was removed since
            }
            System.out.printf("  %-10s | Qty: %-4d | Unit price: %s%n", label, item.getQuantity(), item.getUnitPrice());
        });
        System.out.println("Subtotal: " + order.getSubtotal());
        System.out.println("Discount: -" + order.getDiscountAmount());
        System.out.println("Total: " + order.getTotalAmount());
        System.out.println("==============================");
    }

    private static void offerInvoiceExport(Order order, InvoiceExporter invoiceExporter) {
        String answer = readLine("Export this invoice to a file? (yes/no): ");
        if (!answer.equalsIgnoreCase("yes")) {
            return;
        }
        try {
            Path file = invoiceExporter.export(order);
            System.out.println("Invoice written to: " + file.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error: could not write invoice file - " + e.getMessage());
        }
    }


    private static void reportsMenu(ReportService reportService, ReportExporter reportExporter) {
        while (true) {
            System.out.println("\n----------- REPORTS & ANALYTICS -----------");
            System.out.println("1. Revenue & order count");
            System.out.println("2. Top-N best selling products");
            System.out.println("3. Revenue by category");
            System.out.println("4. Low stock alert");
            System.out.println("5. Export full report snapshot to file");
            System.out.println("0. Back to main menu");

            int choice = readInt("Choice: ");
            switch (choice) {
                case 1 -> printRevenueReport(reportService);
                case 2 -> {
                    int topN = readInt("Top how many products? ");
                    printTopSellingProducts(reportService, topN);
                }
                case 3 -> printRevenueByCategory(reportService);
                case 4 -> {
                    int threshold = readInt("Alert threshold (stock below this value): ");
                    printLowStockAlert(reportService, threshold);
                }
                case 5 -> exportReportFlow(reportService, reportExporter);
                case 0 -> {
                    return;
                }
                default -> System.out.println("Error: Invalid choice.");
            }
        }
    }

    private static void exportReportFlow(ReportService reportService, ReportExporter reportExporter) {
        int topN = readInt("Top how many products to include? ");
        int threshold = readInt("Low stock threshold to include? ");
        try {
            Path file = reportExporter.export(reportService, topN, threshold);
            System.out.println("Report written to: " + file.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error: could not write report file - " + e.getMessage());
        }
    }

    private static void printRevenueReport(ReportService reportService) {
        System.out.println("\n========================================");
        System.out.println("        REVENUE & SALES REPORT          ");
        System.out.println("========================================");
        System.out.printf("Total Orders Placed : %d%n", reportService.totalOrderCount());
        System.out.printf("Total Revenue Earned: %s%n", reportService.totalRevenue());
        System.out.println("========================================");
    }

    private static void printTopSellingProducts(ReportService reportService, int topN) {
        System.out.println("\n==================================================");
        System.out.printf("         TOP %d BEST SELLING PRODUCTS             %n", topN);
        System.out.println("==================================================");
        System.out.printf("%-12s | %-22s | %-10s%n", "SKU", "Product Name", "Qty Sold");
        System.out.println("--------------------------------------------------");
        List<Map.Entry<Product, Long>> top = reportService.topSellingProducts(topN);
        if (top.isEmpty()) {
            System.out.println("No sales yet.");
        } else {
            top.forEach(entry ->
                    System.out.printf("%-12s | %-22s | %-10d%n",
                            entry.getKey().getSku(), entry.getKey().getName(), entry.getValue()));
        }
        System.out.println("==================================================");
    }

    private static void printRevenueByCategory(ReportService reportService) {
        System.out.println("\n==================================================");
        System.out.println("            REVENUE BY CATEGORY                  ");
        System.out.println("==================================================");
        Map<String, BigDecimal> revenueByCategory = reportService.revenueByCategory();
        if (revenueByCategory.isEmpty()) {
            System.out.println("No sales yet.");
        } else {
            revenueByCategory.forEach((category, revenue) ->
                    System.out.printf("%-20s | %s%n", category, revenue));
        }
        System.out.println("==================================================");
    }

    private static void printLowStockAlert(ReportService reportService, int threshold) {
        System.out.println("\n===============================================");
        System.out.printf("        LOW STOCK ALERT (UNDER %d)             %n", threshold);
        System.out.println("===============================================");
        System.out.printf("%-12s | %-22s | %-14s%n", "SKU", "Product Name", "Current Stock");
        System.out.println("--------------------------------------------------");
        List<Product> lowStock = reportService.lowStockProducts(threshold);
        if (lowStock.isEmpty()) {
            System.out.println("All products are well-stocked! No alerts.");
        } else {
            lowStock.forEach(p -> System.out.printf("%-12s | %-22s | %-14d%n",
                    p.getSku(), p.getName(), p.getStockQuantity()));
        }
        System.out.println("==================================================");
    }


    private static void flashSaleFlow(ProductService productService, FlashSaleSimulator simulator) {
        System.out.println("\n----------- FLASH SALE SIMULATION (F6) -----------");
        System.out.println("Pick an existing product with limited stock to flash-sale.");

        long productId = readLong("Product id: ");
        Product product;
        try {
            product = productService.findById(productId);
        } catch (ProductNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        int originalStock = product.getStockQuantity();
        System.out.println("Product: " + product.getSku() + " - " + product.getName() + " (stock: " + originalStock + ")");

        int numberOfOrders = readInt("How many concurrent orders to simulate (e.g. 20): ");
        int qtyPerOrder = readInt("Quantity per order (e.g. 1): ");
        long simulatedDelayMillis = 15; // widens the race window so it reproduces reliably every run

        System.out.println("\n=== STEP 1: UNSAFE (no synchronization) ===");
        FlashSaleSimulator.Result unsafeResult = simulator.runUnsafe(productId, qtyPerOrder, numberOfOrders, simulatedDelayMillis);
        printFlashSaleResult(productService, productId, originalStock, qtyPerOrder, unsafeResult);

        System.out.println("\nResetting stock back to " + originalStock + " before the safe run...");
        Product resetProduct = new Product(product.getId(), product.getSku(), product.getName(),
                product.getCategory(), product.getPrice(), originalStock);
        productService.updateProduct(resetProduct);

        System.out.println("\n=== STEP 2: SAFE (ReentrantLock per product) ===");
        FlashSaleSimulator.Result safeResult = simulator.runSafe(productId, qtyPerOrder, numberOfOrders, simulatedDelayMillis);
        printFlashSaleResult(productService, productId, originalStock, qtyPerOrder, safeResult);

        printRaceConditionWriteUp();
    }

    private static void printFlashSaleResult(ProductService productService, long productId,
                                             int originalStock, int qtyPerOrder, FlashSaleSimulator.Result result) {
        Product current = productService.findById(productId);
        int expectedMaxSuccesses = originalStock / qtyPerOrder;

        System.out.printf("Orders attempted : %d%n", result.getAttempted());
        System.out.printf("Orders succeeded : %d (at most %d should ever succeed given %d in stock)%n",
                result.getSucceeded(), expectedMaxSuccesses, originalStock);
        System.out.printf("Stock remaining  : %d%n", current.getStockQuantity());

        if (current.getStockQuantity() < 0 || result.getSucceeded() > expectedMaxSuccesses) {
            System.out.println(">>> OVERSOLD: more stock was sold than actually existed. <<<");
        } else {
            System.out.println(">>> OK: stock was never oversold. <<<");
        }
        if (!result.getRejections().isEmpty()) {
            System.out.println("Sample rejections:");
            result.getRejections().stream().limit(3).forEach(r -> System.out.println("  - " + r));
        }
    }

    private static void printRaceConditionWriteUp() {
        System.out.println("""

                --- Race condition write-up (for README) ---
                The bug: decrementing stock is a "check-then-act" - read the
                current quantity, decide if there's enough, then write the new
                value back, as two separate database statements. With no lock,
                two threads can both read the same stock value before either
                one writes, so both believe there's enough left and both
                proceed - the product gets oversold.

                The fix: a java.util.concurrent.locks.ReentrantLock is kept
                per product id (StockLockManager). Before touching a product's
                stock, a thread must acquire that product's lock; it releases
                it only after the read-then-write is complete. That serializes
                every check-then-decrement for the same product, so no two
                threads can interleave - the race window is closed and the
                "SAFE" run above never oversells.
                """);
    }


    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a whole number.");
            }
        }
    }

    private static int readIntOrDefault(String prompt, int defaultValue) {
        System.out.print(prompt);
        String line = scanner.nextLine();
        if (line.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: not a number, keeping current value.");
            return defaultValue;
        }
    }

    private static long readLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Long.parseLong(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a whole number.");
            }
        }
    }

    private static BigDecimal readBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return new BigDecimal(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a valid amount (e.g. 199.99).");
            }
        }
    }

    private static BigDecimal readBigDecimalOrDefault(String prompt, BigDecimal defaultValue) {
        System.out.print(prompt);
        String line = scanner.nextLine();
        if (line.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(line.trim());
        } catch (NumberFormatException e) {
            System.out.println("Error: not a valid amount, keeping current value.");
            return defaultValue;
        }
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static String readLineOrDefault(String prompt, String defaultValue) {
        System.out.print(prompt);
        String line = scanner.nextLine();
        return line.isBlank() ? defaultValue : line.trim();
    }
}