package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 菜单类
public class Menu {
    private Map<String, Product> products;

    public Menu() {
        this.products = new HashMap<>();
    }

    // 获取所有商品
    public List<Product> getAllProducts() {
        //TODO
        return new ArrayList<>(products.values());
    }

    // 显示菜单
    public void displayMenu() {
        products.values().forEach(p -> System.out.println(p.getDetails()));
    }

    // 根据ID获取商品
    public Product getProductById(String productId) {
        return products.get(productId);
    }

    // 添加商品
    public void addProduct(Product product) {
        if (products.containsKey(product.getId())) {
            throw new IllegalArgumentException("产品ID已存在: " + product.getId());
        }
        products.put(product.getId(), product);
    }

    // 移除商品
    public boolean removeProduct(String productId) {
        return products.remove(productId) != null;
    }

    // 从文件加载菜单
    public void loadFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length != 6) {
                        System.err.println("第 " + lineNum + " 行格式错误: 需要6个字段，实际得到 " + parts.length);
                        continue;
                    }

                    // 验证每个字段
                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String description = parts[2].trim();
                    String category = parts[3].trim();
                    double price = Double.parseDouble(parts[4].trim());
                    int stock = Integer.parseInt(parts[5].trim());

                    addProduct(new Product(id, name, description, category, price, stock));
                } catch (Exception e) {
                    System.err.println("第 " + lineNum + " 行处理错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("加载菜单文件错误: " + e.getMessage());
        }
    }
}
