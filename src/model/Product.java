package model;

// Product class
public class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private int stock;
    private int orderCount; // Tracks the number of times the product has been ordered

    public Product(String id, String name, String description, String category, double price, int stock) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Product ID cannot be empty");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Product name cannot be empty");
        if (price <= 0) throw new IllegalArgumentException("Price must be greater than 0");
        if (stock < 0) throw new IllegalArgumentException("Stock cannot be negative");

        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.orderCount = 0;
    }

    // Method to reduce stock
    public void reduceStock(int quantity) throws OutOfStockException {
        if (quantity > stock) {
            throw new OutOfStockException("Insufficient stock");
        }
        stock -= quantity;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public void incrementOrderCount(int quantity) {
        this.orderCount += quantity;
    }

    public int getStock() {
        return stock;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
