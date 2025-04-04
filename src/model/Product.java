package model;

// 商品类
public class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private int stock;
    private int orderCount; // 记录商品被订购的次数

    public Product(String id, String name, String description, String category, double price, int stock) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("产品ID不能为空");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("产品名称不能为空");
        if (price <= 0) throw new IllegalArgumentException("价格必须大于0");
        if (stock < 0) throw new IllegalArgumentException("库存不能为负数");

        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.orderCount = 0;
    }
    // 添加库存减少方法
    public void reduceStock(int quantity) throws OutOfStockException {
        if (quantity > stock) {
            throw new OutOfStockException("库存不足");
        }
        stock -= quantity;
    }
    // 获取商品详情
    public String getDetails() {
        return name + ": " + description + " (" + category + ") - $" + price + " (Stock: " + stock + ")";
    }

    // Getter方法
    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void incrementOrderCount(int quantity) {
        this.orderCount += quantity;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
