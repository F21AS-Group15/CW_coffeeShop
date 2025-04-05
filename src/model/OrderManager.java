package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Order management class
public class OrderManager {
    private List<Order> orders;
    private double totalRevenue;

    // New category statistics fields
    private double preOrderRevenue;
    private double walkInRevenue;
    private Map<Product, Integer> preOrderProductCounts;
    private Map<Product, Integer> walkInProductCounts;

    public OrderManager() {
        this.orders = new ArrayList<>();
        this.totalRevenue = 0;
        this.preOrderRevenue = 0;
        this.walkInRevenue = 0;
        this.preOrderProductCounts = new HashMap<>();
        this.walkInProductCounts = new HashMap<>();
    }

    public void addOrder(Order order) {
        orders.add(order);

        // Update revenue statistics
        double orderTotal = order.getTotalPrice();
        totalRevenue += orderTotal;

        if ("PRE_ORDER".equals(order.getOrderType())) {
            preOrderRevenue += orderTotal;
            updateProductCounts(order, preOrderProductCounts);
        } else {
            walkInRevenue += orderTotal;
            updateProductCounts(order, walkInProductCounts);
        }
    }

    // Generate sales report and return as a string
    public String generateReport() {
        StringBuilder report = new StringBuilder();

        report.append("=== Daily Sales Report ===\n\n");
        report.append("=== Pre-order Sales ===\n");
        report.append(printCategoryReport(getPreOrderProductCounts(), getPreOrderRevenue()));

        report.append("\n=== Walk-in Sales ===\n");
        report.append(printCategoryReport(getWalkInProductCounts(), getWalkInRevenue()));

        report.append("\n=== Total Orders Summary ===\n");
        report.append(String.format("Total Sales: $%.2f\n", totalRevenue));
        report.append("Total Orders: ").append(orders.size()).append("\n");
        report.append("Among them:\n");
        report.append("- Pre-order Sales: ").append(preOrderProductCounts.values().stream().mapToInt(i -> i).sum()).append(" items\n");
        report.append("- Walk-in Sales: ").append(walkInProductCounts.values().stream().mapToInt(i -> i).sum()).append(" items\n");

        // Print to terminal
        System.out.println(report);

        return report.toString();
    }

    private String printCategoryReport(Map<Product, Integer> productCounts, double revenue) {
        StringBuilder report = new StringBuilder();

        report.append("Product Sales Details:\n");
        productCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry -> {
                    Product p = entry.getKey();
                    report.append(String.format("%-20s ×%-4d $%-8.2f (Subtotal: $%.2f)\n",
                            p.getName(),
                            entry.getValue(),
                            p.getPrice(),
                            p.getPrice() * entry.getValue()));
                });
        report.append("----------------------------\n");
        report.append(String.format("Total sales for this order type: $%.2f\n", revenue));
        report.append("Number of product types: ").append(productCounts.size()).append("\n");

        return report.toString();
    }

    private void updateProductCounts(Order order, Map<Product, Integer> countsMap) {
        for (Product product : order.getItems()) {
            countsMap.put(product, countsMap.getOrDefault(product, 0) + 1);
            product.incrementOrderCount(1);
        }
    }

    // New category statistics methods
    public Map<Product, Integer> getPreOrderProductCounts() {
        return new HashMap<>(preOrderProductCounts);
    }

    public Map<Product, Integer> getWalkInProductCounts() {
        return new HashMap<>(walkInProductCounts);
    }

    public double getPreOrderRevenue() {
        return preOrderRevenue;
    }

    public double getWalkInRevenue() {
        return walkInRevenue;
    }

    // Get all orders
    public List<Order> getOrders() {
        return orders;
    }

    // Load orders from a file
    public void loadFromFile(String filePath, Menu menu) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            Map<String, Order> orderMap = new HashMap<>(); // Used to merge same orders

            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length != 6) {
                        System.err.println("Line " + lineNum + " format error: expected 6 fields, got " + parts.length);
                        continue;
                    }

                    String orderId = parts[0].trim();
                    String timestamp = parts[1].trim();
                    String customerId = parts[2].trim();
                    String productId = parts[3].trim();
                    int quantity = Integer.parseInt(parts[4].trim());
                    String orderType = parts[5].trim();

                    Product product = menu.getProductById(productId);
                    if (product == null) {
                        System.err.println("Line " + lineNum + " error: Product ID does not exist - " + productId);
                        continue;
                    }

                    // Merge same orders
                    Order order = orderMap.get(orderId);
                    if (order == null) {
                        order = new Order(orderId, timestamp, customerId, orderType);
                        orderMap.put(orderId, order);
                    }

                    // Add product to order
                    if (quantity > 0) {
                        if (quantity > product.getStock()) {
                            System.err.println("Line " + lineNum + " warning: Order quantity exceeds current stock - Product: " + product.getName() +
                                    ", Order Quantity: " + quantity + ", Stock: " + product.getStock());
                            quantity = Math.min(quantity, product.getStock());
                            if (quantity == 0) continue;
                        }
                        order.addItem(product, quantity);
                    }

                } catch (Exception e) {
                    System.err.println("Line " + lineNum + " processing error: " + e.getMessage());
                }
            }

            // Add all merged orders to the order manager
            orderMap.values().forEach(this::addOrder);

        } catch (IOException e) {
            System.err.println("Error loading order file: " + e.getMessage());
        }
    }
}
