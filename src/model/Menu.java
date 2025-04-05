package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Menu class
public class Menu {
    private Map<String, Product> products;

    public Menu() {
        this.products = new HashMap<>();
    }

    // Get all products
    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    // Get product by ID
    public Product getProductById(String productId) {
        return products.get(productId);
    }

    // Add product
    public void addProduct(Product product) {
        if (products.containsKey(product.getId())) {
            throw new IllegalArgumentException("Product ID already exists: " + product.getId());
        }
        products.put(product.getId(), product);
    }

    // Load menu from file
    public void loadFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length != 6) {
                        System.err.println("Line " + lineNum + " format error: requires 6 fields, but got " + parts.length);
                        continue;
                    }

                    // Validate each field
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String description = parts[2].trim();
                    String category = parts[3].trim();
                    double price = Double.parseDouble(parts[4].trim());
                    int stock = Integer.parseInt(parts[5].trim());

                    addProduct(new Product(id, name, description, category, price, stock));
                } catch (Exception e) {
                    System.err.println("Error processing line " + lineNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading menu file: " + e.getMessage());
        }
    }
}
