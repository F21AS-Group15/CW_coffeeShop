import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

// 自定义异常：库存不足异常
class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}

// 商品类
class Product {
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

    // 获取商品详情
    public String getDetails() {
        return name + ": " + description + " (" + category + ") - $" + price + " (Stock: " + stock + ")";
    }

    // Getter方法
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getId() { return id; }
    public int getOrderCount() { return orderCount; }
    public void incrementOrderCount(int quantity) { this.orderCount += quantity; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}

// 订单类
class Order {
    private String orderId;
    private String timeStamp;
    private String customerId;
    private List<Product> items;
    private boolean isCompleted;
    private double totalPrice;

    public Order(String orderId, String timestamp, String customerId) {
        this.orderId = orderId;
        this.timeStamp = timestamp;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.isCompleted = false;
        this.totalPrice = 0;
    }

    // 添加商品到订单
    public void addItem(Product product) { items.add(product); }
    public void addItem(Product product, int amount) {
        for (int i = 0; i < amount; i++) {
            items.add(product);
        }
    }

    // 从订单移除商品
    public void deleteItem(Product product) { items.remove(product); }

    // 计算订单总价
    public double getTotalPrice() {
        return totalPrice > 0 ? totalPrice : Math.round(items.stream().mapToDouble(Product::getPrice).sum() * 100.0) / 100.0;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = Math.round(totalPrice * 100.0) / 100.0;
    }

    // 获取订单详情
    public String getOrderDetails() {
        return "Order ID: " + orderId + "\nTime: " + timeStamp + "\nTotal: $" + getTotalPrice();
    }

    // Getter和Setter方法
    public boolean isCompleted() { return isCompleted; }
    public void completeOrder() { this.isCompleted = true; }
    public void resetOrder() { this.isCompleted = false; }
    public List<Product> getItems() { return items; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getTimeStamp() { return timeStamp; }
}

// 菜单类
class Menu {
    private Map<String, Product> products;

    public Menu() {
        this.products = new HashMap<>();
    }

    // 获取所有商品
    public List<Product> getAllProducts() {
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

// 订单管理类
class OrderManager {
    private List<Order> orders;
    private double totalRevenue;

    public OrderManager() {
        this.orders = new ArrayList<>();
        this.totalRevenue = 0;
    }

    // 生成销售报告
    public void generateReport(Menu menu) {
        System.out.println("=== 销售报告 ===");
        for (Product product : menu.getAllProducts()) {
            System.out.println(product.getDetails() + " - 已订购 " + product.getOrderCount() + " 次");
        }
        System.out.println("总营收: $" + totalRevenue);
    }

    // 添加订单
    public void addOrder(Order order) {
        orders.add(order);
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
            while ((line = br.readLine()) != null) {
                lineNum++;
                try {
                    String[] parts = line.split(",");
                    if (parts.length != 5) {
                        System.err.println("第 " + lineNum + " 行格式错误: 需要5个字段，实际得到 " + parts.length);
                        continue;
                    }

                    String orderId = parts[0].trim();
                    String timestamp = parts[1].trim();
                    String customerId = parts[2].trim();
                    String productId = parts[3].trim();
                    int quantity = Integer.parseInt(parts[4].trim());

                    Product product = menu.getProductById(productId);
                    if (product == null) {
                        System.err.println("第 " + lineNum + " 行错误: 产品ID不存在 - " + productId);
                        continue;
                    }

                    if (quantity > product.getStock()) {
                        System.err.println("第 " + lineNum + " 行警告: 订单数量超过当前库存 - 产品: " + product.getName() +
                                ", 订单量: " + quantity + ", 库存: " + product.getStock());
                        quantity = Math.min(quantity, product.getStock());
                        if (quantity == 0) continue;
                    }

                    Order order = new Order(orderId, timestamp, customerId);
                    order.addItem(product, quantity);
                    addOrder(order);

                } catch (Exception e) {
                    System.err.println("第 " + lineNum + " 行处理错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("加载订单文件错误: " + e.getMessage());
        }
    }
}

// 折扣计算器类（重构后的核心折扣逻辑）
class DiscountCalculator {
    private static final double CAKE_PRICE = 4.0; // 蛋糕单价

    // 折扣计算结果
    static class DiscountResult {
        static final DiscountResult NO_DISCOUNT = new DiscountResult(false, "无可用折扣", 0.0);

        final boolean applied;      // 是否应用了折扣
        final String description;   // 折扣描述
        final double discountAmount; // 折扣金额

        DiscountResult(boolean applied, String description, double discountAmount) {
            this.applied = applied;
            this.description = description;
            this.discountAmount = discountAmount;
        }
    }

    /**
     * 计算最佳折扣（按照优先级顺序）
     * 优先级规则：蛋糕折扣 → 套餐折扣 → 默认折扣
     */
    public DiscountResult calculateBestDiscount(Order originalOrder) {
        // 创建不包含蛋糕的订单副本用于后续折扣计算
        Order orderWithoutCakes = createOrderWithoutCakes(originalOrder);

        // 按优先级顺序计算各种折扣
        DiscountResult cakeDiscount = applyCakeDiscount(originalOrder);
        DiscountResult mealDealDiscount = applyMealDealDiscount(orderWithoutCakes);
        DiscountResult defaultDiscount = applyDefaultDiscount(orderWithoutCakes);

        // 根据优先级选择最优折扣
        if (cakeDiscount.applied) {
            return cakeDiscount;
        }
        if (mealDealDiscount.applied) {
            return mealDealDiscount;
        }
        return defaultDiscount;
    }

    // 创建不包含蛋糕的订单副本
    private Order createOrderWithoutCakes(Order originalOrder) {
        Order orderWithoutCakes = new Order(
                originalOrder.getOrderId() + "-nocakes",
                originalOrder.getTimeStamp(),
                originalOrder.getCustomerId()
        );

        for (Product product : originalOrder.getItems()) {
            if (!"Cake".equals(product.getName())) {
                orderWithoutCakes.addItem(product);
            }
        }
        return orderWithoutCakes;
    }

    // 应用蛋糕折扣规则：买3送1
    private DiscountResult applyCakeDiscount(Order order) {
        int cakeCount = countCakes(order);
        if (cakeCount >= 3) {
            int freeCakes = cakeCount / 3;
            double discountAmount = freeCakes * CAKE_PRICE;
            return new DiscountResult(
                    true,
                    "蛋糕买三送一 (减¥" + discountAmount + ")",
                    discountAmount
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // 应用套餐折扣规则：2份食品+1份饮料打8折
    private DiscountResult applyMealDealDiscount(Order order) {
        int foodCount = countItemsByCategory(order, "Food");
        int beverageCount = countItemsByCategory(order, "Beverage");

        if (foodCount >= 2 && beverageCount >= 1) {
            double originalPrice = order.getTotalPrice();
            double discountedPrice = originalPrice * 0.8;
            return new DiscountResult(
                    true,
                    "套餐优惠 (8折)",
                    originalPrice - discountedPrice
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // 应用默认折扣规则：
    // - 满50元打8折
    // - 满30元减5元
    // - 满20元减2元
    private DiscountResult applyDefaultDiscount(Order order) {
        double originalPrice = order.getTotalPrice();

        if (originalPrice >= 50) {
            double discountedPrice = originalPrice * 0.8;
            return new DiscountResult(
                    true,
                    "满50元8折优惠",
                    originalPrice - discountedPrice
            );
        } else if (originalPrice >= 30) {
            return new DiscountResult(
                    true,
                    "满30元减5元",
                    5.0
            );
        } else if (originalPrice >= 20) {
            return new DiscountResult(
                    true,
                    "满20元减2元",
                    2.0
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // 统计蛋糕数量
    private int countCakes(Order order) {
        return (int) order.getItems().stream()
                .filter(p -> "Cake".equals(p.getName()))
                .count();
    }

    // 按分类统计商品数量
    private int countItemsByCategory(Order order, String category) {
        return (int) order.getItems().stream()
                .filter(p -> category.equals(p.getCategory()))
                .count();
    }
}

// 咖啡店主类
public class CoffeeShop {
    private List<JLabel> quantityLabels = new ArrayList<>();
    private Menu menu;
    private OrderManager orderManager;
    private DiscountCalculator discountCalculator;  // 使用新的折扣计算器
    private JFrame frame;
    private JPanel productPanel;
    private JTextArea orderDetails;
    private JButton placeOrderButton;
    private Map<Product, Integer> selectedProducts;
    private Map<String, JLabel> stockLabels = new HashMap<>();
    private JLabel totalPriceLabel;
    private JLabel discountedPriceLabel;
    private JTextArea orderSummaryArea;
    private StringBuilder allOrderSummaries = new StringBuilder();

    public CoffeeShop() {
        menu = new Menu();
        orderManager = new OrderManager();
        discountCalculator = new DiscountCalculator();  // 初始化折扣计算器
        selectedProducts = new HashMap<>();
        allOrderSummaries = new StringBuilder();

        menu.loadFromFile("menu.txt");
        orderManager.loadFromFile("orders.txt", menu);
        setupGUI();
    }

    // GUI初始化方法（保持不变）
    private void setupGUI() {
        frame = new JFrame("咖啡店订单管理系统");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        // 渐变背景面板
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(255, 248, 240);  // 浅米色
                Color color2 = new Color(220, 240, 255);  // 浅蓝色
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        frame.setContentPane(backgroundPanel);

        // 顶部价格信息面板
        JPanel priceInfoPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        priceInfoPanel.setOpaque(false);
        priceInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel totalLabel = new JLabel("商品总价:");
        totalLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        priceInfoPanel.add(totalLabel);

        totalPriceLabel = new JLabel("¥0.00");
        totalPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        totalPriceLabel.setForeground(new Color(0, 100, 0));  // 深绿色
        priceInfoPanel.add(totalPriceLabel);

        JLabel discountLabel = new JLabel("折后价格:");
        discountLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        priceInfoPanel.add(discountLabel);

        discountedPriceLabel = new JLabel("¥0.00");
        discountedPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        discountedPriceLabel.setForeground(new Color(200, 0, 0));  // 深红色
        priceInfoPanel.add(discountedPriceLabel);

        backgroundPanel.add(priceInfoPanel, BorderLayout.NORTH);

        // 主选项卡面板
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);

        // ========== 商品面板 ==========
        productPanel = new JPanel();
        productPanel.setLayout(new GridLayout(0, 2, 15, 15));
        productPanel.setOpaque(false);
        productPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 清空库存标签映射
        stockLabels.clear();

        for (Product product : menu.getAllProducts()) {
            // 创建商品卡片面板
            JPanel cardPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(255, 255, 255, 220));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2d.setColor(new Color(200, 200, 200, 100));
                    g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
                }
            };
            cardPanel.setOpaque(false);
            cardPanel.setPreferredSize(new Dimension(380, 180));

            // 商品信息面板
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            // 商品名称
            JLabel nameLabel = new JLabel("<html><b><font size=+1>" + product.getName() + "</font></b></html>");
            nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(5));

            // 商品描述
            JTextArea descriptionArea = new JTextArea(product.getDescription());
            descriptionArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setEditable(false);
            descriptionArea.setOpaque(false);
            descriptionArea.setMaximumSize(new Dimension(350, 40));
            infoPanel.add(descriptionArea);
            infoPanel.add(Box.createVerticalStrut(5));

            // 商品分类和价格
            JPanel detailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            detailPanel.setOpaque(false);

            JLabel categoryLabel = new JLabel("分类: " + product.getCategory());
            categoryLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            detailPanel.add(categoryLabel);

            JLabel priceLabel = new JLabel("价格: ¥" + product.getPrice());
            priceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
            priceLabel.setForeground(new Color(0, 100, 0));
            detailPanel.add(priceLabel);

            infoPanel.add(detailPanel);

            // 库存信息
            JLabel stockLabel = new JLabel("库存: " + product.getStock());
            stockLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            stockLabel.setForeground(new Color(100, 100, 100));
            infoPanel.add(stockLabel);

            // 将库存标签存入映射
            stockLabels.put(product.getId(), stockLabel);

            // 数量选择面板
            JPanel quantityPanel = new JPanel();
            quantityPanel.setOpaque(false);
            quantityPanel.setLayout(new BoxLayout(quantityPanel, BoxLayout.X_AXIS));
            quantityPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

            JButton minusButton = new JButton("-");
            minusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            minusButton.setPreferredSize(new Dimension(30, 30));
            minusButton.setBackground(new Color(255, 150, 150));
            minusButton.setForeground(Color.WHITE);
            minusButton.setFocusPainted(false);

            JLabel quantityLabel = new JLabel("0");
            quantityLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            quantityLabels.add(quantityLabel);

            JButton plusButton = new JButton("+");
            plusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            plusButton.setPreferredSize(new Dimension(30, 30));
            plusButton.setBackground(new Color(150, 200, 150));
            plusButton.setForeground(Color.WHITE);
            plusButton.setFocusPainted(false);

            // 按钮事件处理
            minusButton.addActionListener(e -> {
                int quantity = selectedProducts.getOrDefault(product, 0);
                if (quantity > 0) {
                    quantity--;
                    selectedProducts.put(product, quantity);
                    quantityLabel.setText(String.valueOf(quantity));
                    updatePriceDisplay();
                }
            });

            plusButton.addActionListener(e -> {
                try {
                    int quantity = selectedProducts.getOrDefault(product, 0);
                    if (quantity >= product.getStock()) {
                        throw new OutOfStockException(
                                "库存不足！\n" + product.getName() +
                                        " 剩余: " + product.getStock() +
                                        "\n最大可添加数量: " + product.getStock()
                        );
                    }
                    quantity++;
                    selectedProducts.put(product, quantity);
                    quantityLabel.setText(String.valueOf(quantity));
                    updatePriceDisplay();
                } catch (OutOfStockException ex) {
                    JOptionPane.showMessageDialog(
                            frame,
                            ex.getMessage(),
                            "库存警告",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            });

            quantityPanel.add(minusButton);
            quantityPanel.add(quantityLabel);
            quantityPanel.add(plusButton);
            infoPanel.add(quantityPanel);

            cardPanel.add(infoPanel, BorderLayout.CENTER);
            productPanel.add(cardPanel);
        }

        JScrollPane productScroll = new JScrollPane(productPanel);
        productScroll.setOpaque(false);
        productScroll.getViewport().setOpaque(false);
        tabbedPane.addTab("商品菜单", productScroll);

        // ========== 订单详情面板 ==========
        JPanel orderPanel = new JPanel(new BorderLayout());
        orderPanel.setOpaque(false);
        orderPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        orderSummaryArea = new JTextArea();
        orderSummaryArea.setEditable(false);
        orderSummaryArea.setOpaque(false);
        orderSummaryArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        orderSummaryArea.setLineWrap(true);
        orderSummaryArea.setWrapStyleWord(true);

        // 加载所有历史订单
//        loadAllOrdersToDisplay();

        JScrollPane orderScroll = new JScrollPane(orderSummaryArea);
        orderScroll.setOpaque(false);
        orderScroll.getViewport().setOpaque(false);
        orderPanel.add(orderScroll, BorderLayout.CENTER);

        tabbedPane.addTab("订单详情", orderPanel);

        // ========== 折扣信息面板 ==========
        JPanel discountPanel = new JPanel(new BorderLayout());
        discountPanel.setOpaque(false);
        discountPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JTextArea discountTextArea = new JTextArea();
        discountTextArea.setEditable(false);
        discountTextArea.setOpaque(false);
        discountTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        discountTextArea.setLineWrap(true);
        discountTextArea.setWrapStyleWord(true);

        String discountInfo = "当前优惠活动:\n\n" +
                "1. 蛋糕特惠\n" +
                "   - 购买3个或以上蛋糕，可免费获得1个\n\n" +
                "2. 套餐优惠\n" +
                "   - 2份食品 + 1份饮料可享8折优惠\n\n" +
                "3. 常规优惠\n" +
                "   - 满¥20减¥2\n" +
                "   - 满¥30减¥5\n" +
                "   - 满¥50享8折优惠\n\n" +
                "注：优惠不可叠加使用，系统将自动应用最优折扣";

        discountTextArea.setText(discountInfo);

        JScrollPane discountScroll = new JScrollPane(discountTextArea);
        discountScroll.setOpaque(false);
        discountScroll.getViewport().setOpaque(false);
        discountPanel.add(discountScroll, BorderLayout.CENTER);

        tabbedPane.addTab("优惠信息", discountPanel);

        backgroundPanel.add(tabbedPane, BorderLayout.CENTER);

        // ========== 下单按钮 ==========
        placeOrderButton = new JButton("确认下单");
        placeOrderButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        placeOrderButton.setBackground(new Color(70, 130, 180));
        placeOrderButton.setForeground(Color.WHITE);
        placeOrderButton.setFocusPainted(false);
        placeOrderButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        placeOrderButton.addActionListener(e -> placeOrder());

        backgroundPanel.add(placeOrderButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }


    // 更新价格显示
    private void updatePriceDisplay() {
        // 计算商品总价
        double totalPrice = selectedProducts.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue())
                .sum();

        // 创建临时订单用于折扣计算
        Order tempOrder = new Order("temp", "temp", "temp");
        selectedProducts.forEach((product, quantity) -> {
            if (quantity > 0) {
                tempOrder.addItem(product, quantity);
            }
        });

        // 计算最佳折扣
        DiscountCalculator.DiscountResult discount = discountCalculator.calculateBestDiscount(tempOrder);
        double discountedPrice = totalPrice - discount.discountAmount;

        // 更新UI显示
        totalPriceLabel.setText(String.format("¥%.2f", totalPrice));
        discountedPriceLabel.setText(String.format("¥%.2f", discountedPrice));
    }

    // 下单方法
    private void placeOrder() {
        // 检查是否选择了商品
        if (selectedProducts.values().stream().allMatch(q -> q == 0)) {
            JOptionPane.showMessageDialog(frame,
                    "您还没有选择任何商品！\n请先添加商品到订单",
                    "订单错误",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 创建新订单
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Order order = new Order(orderId, timestamp, "CUST-001");

        // 准备订单摘要
        StringBuilder summary = new StringBuilder();
        summary.append("════════════ 订单详情 ════════════\n\n");
        summary.append("订单编号: ").append(orderId).append("\n");
        summary.append("下单时间: ").append(timestamp).append("\n");
        summary.append("────────────────────────────────\n\n");
        summary.append("已购商品:\n");

        double totalPrice = 0;
        Map<Product, Integer> orderedItems = new LinkedHashMap<>();

        // 1. 添加商品到订单并计算总价
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            if (entry.getValue() > 0) {
                Product product = entry.getKey();
                int quantity = entry.getValue();

                // 更新库存
                product.setStock(product.getStock() - quantity);
                updateProductStockDisplay(product);

                // 添加到订单
                order.addItem(product, quantity);
                product.incrementOrderCount(quantity);
                orderedItems.put(product, quantity);

                // 计算小计
                double subtotal = product.getPrice() * quantity;
                totalPrice += subtotal;

                // 添加到订单摘要
                summary.append(String.format(
                        "▶ %-15s x%-2d @ ¥%-6.2f = ¥%-7.2f\n",
                        product.getName(),
                        quantity,
                        product.getPrice(),
                        subtotal
                ));
            }
        }

        // 2. 应用最佳折扣
        DiscountCalculator.DiscountResult discount = discountCalculator.calculateBestDiscount(order);
        double discountedPrice = totalPrice - discount.discountAmount;

        // 设置订单最终价格
        order.setTotalPrice(discountedPrice);
        orderManager.addOrder(order);
        orderManager.addToTotalRevenue(discountedPrice);

        // 3. 完善订单摘要
        summary.append("\n────────────────────────────────\n");
        summary.append(String.format("%-20s: ¥%7.2f\n", "商品总价", totalPrice));
        summary.append(String.format("%-20s: %-10s\n", "应用优惠", discount.description));
        summary.append(String.format("%-20s: ¥%7.2f\n", "折后价格", discountedPrice));
        summary.append(String.format("%-20s: ¥%7.2f\n", "节省金额", discount.discountAmount));
        summary.append("\n════════════════════════════════\n");
        summary.append("感谢您的惠顾，欢迎再次光临！\n");

        // 4. 更新UI显示
        allOrderSummaries.insert(0, summary.toString() + "\n\n");
        orderSummaryArea.setText(allOrderSummaries.toString());
        orderSummaryArea.setCaretPosition(0);

        // 5. 显示订单确认对话框
        JOptionPane.showMessageDialog(
                frame,
                "订单提交成功！\n\n" +
                        "订单编号: " + orderId + "\n" +
                        "折后总价: ¥" + String.format("%.2f", discountedPrice),
                "订单确认",
                JOptionPane.INFORMATION_MESSAGE
        );

        // 6. 重置选择状态
        selectedProducts.clear();
        for (JLabel quantityLabel : quantityLabels) {
            quantityLabel.setText("0");
        }
        totalPriceLabel.setText("¥0.00");
        discountedPriceLabel.setText("¥0.00");
    }

    // 更新商品库存显示
    private void updateProductStockDisplay(Product product) {
        SwingUtilities.invokeLater(() -> {
            JLabel stockLabel = stockLabels.get(product.getId());
            if (stockLabel != null) {
                stockLabel.setText("库存: " + product.getStock());

                // 库存紧张时显示红色
                if (product.getStock() < 3) {
                    stockLabel.setForeground(Color.RED);
                } else {
                    stockLabel.setForeground(new Color(100, 100, 100));
                }
            }
        });
    }

    // 主方法
    public static void main(String[] args) {
        CoffeeShop coffeeShop = new CoffeeShop();
        coffeeShop.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                coffeeShop.orderManager.generateReport(coffeeShop.menu);

                // 保存订单到文件
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("orders.txt", false))) {
                    for (Order order : coffeeShop.orderManager.getOrders()) {
                        Map<Product, Integer> productQuantityMap = new HashMap<>();
                        for (Product product : order.getItems()) {
                            productQuantityMap.put(product, productQuantityMap.getOrDefault(product, 0) + 1);
                        }

                        for (Map.Entry<Product, Integer> entry : productQuantityMap.entrySet()) {
                            Product product = entry.getKey();
                            int quantity = entry.getValue();

                            if (quantity > 0) {
                                bw.write(order.getOrderId() + "," +
                                        order.getTimeStamp() + "," +
                                        order.getCustomerId() + "," +
                                        product.getId() + "," +
                                        quantity);
                                bw.newLine();
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("写入订单文件错误: " + e.getMessage());
                }
            }
        });
    }
}
