package vn.edu.fpt.io;

import vn.edu.fpt.model.Product;
import vn.edu.fpt.service.ReportService;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


public class ReportExporter {

    private static final Path OUTPUT_DIR = Path.of("output");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public Path export(ReportService reportService, int topN, int lowStockThreshold) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        Path file = OUTPUT_DIR.resolve("report-" + LocalDateTime.now().format(FILE_STAMP) + ".txt");

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
            writer.println("========================================");
            writer.println("        STOCKPILOT SALES REPORT           ");
            writer.printf("        Generated: %s%n", LocalDateTime.now());
            writer.println("========================================");

            writer.printf("Total orders : %d%n", reportService.totalOrderCount());
            writer.printf("Total revenue: %s%n", reportService.totalRevenue());

            writer.println("\n--- Top " + topN + " best-selling products ---");
            List<Map.Entry<Product, Long>> top = reportService.topSellingProducts(topN);
            if (top.isEmpty()) {
                writer.println("No sales yet.");
            } else {
                for (Map.Entry<Product, Long> entry : top) {
                    writer.printf("%-10s %-25s qty sold: %d%n",
                            entry.getKey().getSku(), entry.getKey().getName(), entry.getValue());
                }
            }

            writer.println("\n--- Revenue by category ---");
            Map<String, BigDecimal> byCategory = reportService.revenueByCategory();
            if (byCategory.isEmpty()) {
                writer.println("No sales yet.");
            } else {
                byCategory.forEach((category, revenue) ->
                        writer.printf("%-20s %s%n", category, revenue));
            }

            writer.println("\n--- Low stock (under " + lowStockThreshold + ") ---");
            List<Product> lowStock = reportService.lowStockProducts(lowStockThreshold);
            if (lowStock.isEmpty()) {
                writer.println("All products are well-stocked.");
            } else {
                for (Product p : lowStock) {
                    writer.printf("%-10s %-25s stock: %d%n", p.getSku(), p.getName(), p.getStockQuantity());
                }
            }
            writer.println("========================================");
        }

        return file;
    }
}