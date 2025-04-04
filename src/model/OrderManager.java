package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 订单管理类
public class OrderManager {
    private List<Order> orders;
    private double totalRevenue;

    // 新增分类统计字段
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

        // 更新营收统计
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

    // 生成销售报告
    public void generateReport(Menu menu) {
        System.out.println("=== 每日销售报告 ===");
        System.out.println("\n=== 预定订单 ===");
        printCategoryReport(menu, getPreOrderProductCounts(), getPreOrderRevenue());

        System.out.println("\n=== 现场订单 ===");
        printCategoryReport(menu, getWalkInProductCounts(), getWalkInRevenue());

        System.out.println("\n=== 全体订单汇总 ===");
        System.out.println("总销售额: ?" + String.format("%.2f", totalRevenue));
        System.out.println("总订单数: " + orders.size());
        System.out.println("其中:");
        System.out.println("- 预定订单: " + preOrderProductCounts.values().stream().mapToInt(i -> i).sum() + " 件商品");
        System.out.println("- 现场订单: " + walkInProductCounts.values().stream().mapToInt(i -> i).sum() + " 件商品");
    }

    private void printCategoryReport(Menu menu, Map<Product, Integer> productCounts, double revenue) {
        System.out.println("商品销售明细:");
        productCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry -> {
                    Product p = entry.getKey();
                    System.out.printf("%-20s ×%-4d ?%-8.2f (小计: ?%.2f)\n",
                            p.getName(),
                            entry.getValue(),
                            p.getPrice(),
                            p.getPrice() * entry.getValue());
                });
        System.out.println("----------------------------");
        System.out.println("该类订单总销售额: ?" + String.format("%.2f", revenue));
        System.out.println("商品种类数: " + productCounts.size());
    }

    private void updateProductCounts(Order order, Map<Product, Integer> countsMap) {
        for (Product product : order.getItems()) {
            countsMap.put(product, countsMap.getOrDefault(product, 0) + 1);
            product.incrementOrderCount(1); // 保留原有的总计数
        }
    }

    // 新增分类统计方法
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

    // 获取所有订单
    public List<Order> getOrders() {
        return orders;
    }

    // 移除订单
    public boolean removeOrder(String orderId) {
        return orders.removeIf(o -> o.getOrderId().equals(orderId));
    }

    // 增加总营收
    public void addToTotalRevenue(double amount) {
        this.totalRevenue += amount;
    }

    // 从文件加载订单
    public void loadFromFile(String filePath, Menu menu) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            Map<String, Order> orderMap = new HashMap<>(); // 用于合并相同订单

            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length != 6) {
                        System.err.println("第 " + lineNum + " 行格式错误: 需要6个字段，实际得到 " + parts.length);
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
                        System.err.println("第 " + lineNum + " 行错误: 产品ID不存在 - " + productId);
                        continue;
                    }

                    // 合并相同订单
                    Order order = orderMap.get(orderId);
                    if (order == null) {
                        order = new Order(orderId, timestamp, customerId, orderType);
                        orderMap.put(orderId, order);
                    }

                    // 添加商品到订单
                    if (quantity > 0) {
                        if (quantity > product.getStock()) {
                            System.err.println("第 " + lineNum + " 行警告: 订单数量超过当前库存 - 产品: " + product.getName() +
                                    ", 订单量: " + quantity + ", 库存: " + product.getStock());
                            quantity = Math.min(quantity, product.getStock());
                            if (quantity == 0) continue;
                        }
                        order.addItem(product, quantity);
                    }

                } catch (Exception e) {
                    System.err.println("第 " + lineNum + " 行处理错误: " + e.getMessage());
                }
            }

            // 将所有合并后的订单添加到订单管理器
            orderMap.values().forEach(this::addOrder);

        } catch (IOException e) {
            System.err.println("加载订单文件错误: " + e.getMessage());
        }
    }
}
