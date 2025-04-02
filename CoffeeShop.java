import org.junit.jupiter.params.shadow.com.univocity.parsers.common.DataValidationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

// Custom exception: Inventory shortage exception
class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}

class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private int stock;
    private int orderCount; // Keep track of how many times items are ordered

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

    public String getDetails() {
        return name + ": " + description + " (" + category + ") - $" + price + " (Stock: " + stock + ")";
    }

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

    public void addItem(Product product) { items.add(product); }
    public void addItem(Product product, int amount) {
        for (int i = 0; i < amount; i++){
            items.add(product);
        }
    }
    public void deleteItem(Product product) { items.remove(product); }

    public double getTotalPrice() {
        return totalPrice > 0 ? totalPrice : Math.round(items.stream().mapToDouble(Product::getPrice).sum() * 100.0) / 100.0;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = Math.round(totalPrice * 100.0) / 100.0;
    }

    public String getOrderDetails() {
        return "Order ID: " + orderId + "\nTime: " + timeStamp + "\nTotal: $" + getTotalPrice();
    }

    public boolean isCompleted() { return isCompleted; }
    public void completeOrder() { this.isCompleted = true; }
    public void resetOrder() { this.isCompleted = false; }
    public List<Product> getItems() { return items; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getTimeStamp() { return timeStamp; }
}

class Menu {
    private Map<String, Product> products;

    public Menu() {
        this.products = new HashMap<>();
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public void displayMenu() {
        products.values().forEach(p -> System.out.println(p.getDetails()));
    }

    public Product getProductById(String productId) {
        return products.get(productId);
    }

    public void addProduct(Product product) {
        if (products.containsKey(product.getId())) {
            throw new IllegalArgumentException("产品ID已存在: " + product.getId());
        }
        products.put(product.getId(), product);
    }

    public boolean removeProduct(String productId) {
        return products.remove(productId) != null;
    }

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
                    String id = validateId(parts[0].trim(), lineNum);
                    String name = validateName(parts[1].trim(), lineNum);
                    String description = parts[2].trim();
                    String category = validateCategory(parts[3].trim(), lineNum);
                    double price = validatePrice(parts[4].trim(), lineNum);
                    int stock = validateStock(parts[5].trim(), lineNum);

                    addProduct(new Product(id, name, description, category, price, stock));
                } catch (DataValidationException e) {
                    System.err.println("第 " + lineNum + " 行数据验证失败: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("第 " + lineNum + " 行处理错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("加载菜单文件错误: " + e.getMessage());
        }
    }

    // 新增数据验证方法
    private String validateId(String id, int lineNum) throws DataValidationException {
        if (id == null || id.isEmpty()) {
            throw new DataValidationException("产品ID不能为空");
        }
        if (id.length() > 10) {
            throw new DataValidationException("产品ID过长(最大10字符)");
        }
        return id;
    }

    private String validateName(String name, int lineNum) throws DataValidationException {
        if (name == null || name.isEmpty()) {
            throw new DataValidationException("产品名称不能为空");
        }
        if (name.length() > 50) {
            throw new DataValidationException("产品名称过长(最大50字符)");
        }
        return name;
    }

    private String validateCategory(String category, int lineNum) throws DataValidationException {
        Set<String> validCategories = Set.of("Beverage", "Food", "Dessert");
        if (!validCategories.contains(category)) {
            throw new DataValidationException("无效分类: " + category + " (有效值: Beverage, Food, Dessert)");
        }
        return category;
    }

    private double validatePrice(String priceStr, int lineNum) throws DataValidationException {
        try {
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                throw new DataValidationException("价格必须大于0");
            }
            if (price > 1000) {
                throw new DataValidationException("价格不能超过1000");
            }
            return Math.round(price * 100) / 100.0; // 保留两位小数
        } catch (NumberFormatException e) {
            throw new DataValidationException("价格格式无效: " + priceStr);
        }
    }

    private int validateStock(String stockStr, int lineNum) throws DataValidationException {
        try {
            int stock = Integer.parseInt(stockStr);
            if (stock < 0) {
                throw new DataValidationException("库存不能为负数");
            }
            if (stock > 1000) {
                throw new DataValidationException("库存不能超过1000");
            }
            return stock;
        } catch (NumberFormatException e) {
            throw new DataValidationException("库存格式无效: " + stockStr);
        }
    }
}

class OrderManager {
    private List<Order> orders;
    private double totalRevenue;

    public OrderManager() {
        this.orders = new ArrayList<>();
        this.totalRevenue = 0;
    }

    public void generateReport(Menu menu) {
        System.out.println("=== Sales Report ===");
        for (Product product : menu.getAllProducts()) {
            System.out.println(product.getDetails() + " - Ordered " + product.getOrderCount() + " times");
        }
        System.out.println("Total Revenue: $" + totalRevenue);
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public List<Order> getOrders() {
        return orders;
    }

    public boolean removeOrder(String orderId) {
        return orders.removeIf(o -> o.getOrderId().equals(orderId));
    }

    public void addToTotalRevenue(double amount) {
        this.totalRevenue += amount;
    }

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

                    // 验证订单数据
                    String orderId = validateOrderId(parts[0].trim(), lineNum);
                    String timestamp = validateTimestamp(parts[1].trim(), lineNum);
                    String customerId = validateCustomerId(parts[2].trim(), lineNum);
                    String productId = parts[3].trim();
                    int quantity = validateQuantity(parts[4].trim(), lineNum);

                    // 验证产品是否存在
                    Product product = menu.getProductById(productId);
                    if (product == null) {
                        System.err.println("第 " + lineNum + " 行错误: 产品ID不存在 - " + productId);
                        continue;
                    }

                    // 验证库存是否足够
                    if (quantity > product.getStock()) {
                        System.err.println("第 " + lineNum + " 行警告: 订单数量超过当前库存 - 产品: " + product.getName() +
                                ", 订单量: " + quantity + ", 库存: " + product.getStock());
                        // 可以选择调整数量或跳过该订单
                        quantity = Math.min(quantity, product.getStock());
                        if (quantity == 0) continue;
                    }

                    Order order = new Order(orderId, timestamp, customerId);
                    order.addItem(product, quantity);
                    addOrder(order);

                } catch (DataValidationException e) {
                    System.err.println("第 " + lineNum + " 行数据验证失败: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("第 " + lineNum + " 行处理错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("加载订单文件错误: " + e.getMessage());
        }
    }

    // 新增订单数据验证方法
    private String validateOrderId(String orderId, int lineNum) throws DataValidationException {
        if (orderId == null || orderId.isEmpty()) {
            throw new DataValidationException("订单ID不能为空");
        }
        if (!orderId.matches("[A-Za-z0-9-]+")) {
            throw new DataValidationException("订单ID包含无效字符");
        }
        return orderId;
    }

    private String validateTimestamp(String timestamp, int lineNum) throws DataValidationException {
        try {
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(timestamp);
            return timestamp;
        } catch (Exception e) {
            throw new DataValidationException("时间格式无效，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String validateCustomerId(String customerId, int lineNum) throws DataValidationException {
        if (customerId == null || customerId.isEmpty()) {
            throw new DataValidationException("客户ID不能为空");
        }
        return customerId;
    }

    private int validateQuantity(String quantityStr, int lineNum) throws DataValidationException {
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                throw new DataValidationException("数量必须大于0");
            }
            if (quantity > 100) {
                throw new DataValidationException("单次订单数量不能超过100");
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new DataValidationException("数量格式无效: " + quantityStr);
        }
    }
}

abstract class Discount {
    private String discountName;
    public Discount(String discountName) {
        this.discountName = discountName;
    }
    public String getDiscountName() {
        return discountName;
    }

    public abstract double applyDiscount(Order order, double originalPrice);
}

class DiscountManager {
    private Map<String, Discount> discountRules;

    public DiscountManager() {
        this.discountRules = new HashMap<>();
        addDiscount(new DefaultDiscount());
        addDiscount(new FoodAndBeverageDiscount());
        addDiscount(new FreeCakeDiscount());
    }

    public void addDiscount(Discount discount) {
        discountRules.put(discount.getDiscountName(), discount);
    }

    public boolean removeDiscount(Discount discount) {
        return discountRules.remove(discount.getDiscountName()) != null;
    }

    public double applyDiscount(String discountName, Order order, double originalPrice) {
        if (discountName == null || discountName.isEmpty()) {
            discountName = "default";
        }

        Discount discount = discountRules.getOrDefault(discountName, new DefaultDiscount());
        return discount.applyDiscount(order, originalPrice);
    }
}

class DefaultDiscount extends Discount {
    public DefaultDiscount() {
        super("default");
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        if (originalPrice >= 50) {
            return originalPrice * 0.8;
        }
        else if (originalPrice >= 30) {
            return originalPrice - 5;
        }
        else if (originalPrice >= 20) {
            return originalPrice - 2;
        }
        return originalPrice;
    }
}

class FoodAndBeverageDiscount extends Discount {
    public FoodAndBeverageDiscount() {
        super("food_and_beverage");
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        int foodCount = 0;
        int beverageCount = 0;

        for (Product product : order.getItems()) {
            if ("Food".equals(product.getCategory())) {
                foodCount++;
            } else if ("Beverage".equals(product.getCategory())) {
                beverageCount++;
            }
        }

        if (foodCount >= 2 && beverageCount >= 1) {
            return originalPrice * 0.8;
        }

        return originalPrice;
    }
}

class FreeCakeDiscount extends Discount {
    public FreeCakeDiscount() {
        super("free_cake");
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        int cakeCount = 0;

        for (Product product : order.getItems()) {
            if ("Cake".equals(product.getName())) {
                cakeCount++;
            }
        }

        if (cakeCount >= 3) {
            return originalPrice - 4.0;
        }

        return originalPrice;
    }
}

public class CoffeeShop {
    private List<JLabel> quantityLabels = new ArrayList<>();
    private Menu menu;
    private OrderManager orderManager;
    private DiscountManager discountManager;
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
        discountManager = new DiscountManager();
        selectedProducts = new HashMap<>();
        allOrderSummaries = new StringBuilder(); // 初始化

        menu.loadFromFile("menu.txt");
        orderManager.loadFromFile("orders.txt", menu);
        setupGUI();
//        loadAllOrdersToDisplay(); // 加载历史订单
    }

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

    /**
     * 加载所有历史订单到显示区域
     */
    //TODO
    private void loadAllOrdersToDisplay() {
        allOrderSummaries = new StringBuilder();
        allOrderSummaries.append("════════════ 历史订单记录 ════════════\n\n");

        if (!orderManager.getOrders().isEmpty()) {
            List<Order> sortedOrders = new ArrayList<>(orderManager.getOrders());
            sortedOrders.sort((o1, o2) -> o2.getTimeStamp().compareTo(o1.getTimeStamp()));

            for (Order order : sortedOrders) {
                // 构建每个订单的详细内容
                StringBuilder orderContent = new StringBuilder();
                orderContent.append("┌──────────────────────────────┐\n");
                orderContent.append("│ 订单编号: ").append(String.format("%-20s", order.getOrderId())).append("│\n");
                // [保持原有的订单格式构建逻辑...]

                allOrderSummaries.append(orderContent.toString());
            }
        } else {
            allOrderSummaries.append("您的订单详情将在此显示...\n\n请从左侧菜单选择商品");
        }

        orderSummaryArea.setText(allOrderSummaries.toString());
    }

    // 新增方法：更新价格显示
    private void updatePriceDisplay() {
        // 计算总价
        double totalPrice = selectedProducts.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getPrice() * entry.getValue())
                .sum();

        // 创建临时订单计算折扣
        Order tempOrder = new Order("temp", "temp", "temp");
        selectedProducts.forEach((product, quantity) -> {
            if (quantity > 0) {
                tempOrder.addItem(product, quantity);
            }
        });

        // 应用折扣
        double discountedPrice = discountManager.applyDiscount("default", tempOrder, totalPrice);

        // 更新显示
        totalPriceLabel.setText(String.format("$%.2f", totalPrice));
        discountedPriceLabel.setText(String.format("$%.2f", discountedPrice));
    }

    private void displayOrderInGUI(Order newOrder) {
        SwingUtilities.invokeLater(() -> {
            // 获取现有订单文本
            String currentText = orderSummaryArea.getText();

            // 如果是初始提示文本，则清空
            if (currentText.startsWith("您的订单详情将在此显示...")) {
                currentText = "";
            }

            // 准备新订单内容
            StringBuilder orderContent = new StringBuilder();

            // 添加分隔线
            orderContent.append("\n════════════ 订单 ════════════\n");

            // 订单基本信息
            orderContent.append("订单编号: ").append(newOrder.getOrderId()).append("\n");
            orderContent.append("下单时间: ").append(newOrder.getTimeStamp()).append("\n\n");
            orderContent.append("购买商品:\n");

            // 商品列表
            Map<Product, Integer> productQuantities = new HashMap<>();
            for (Product product : newOrder.getItems()) {
                productQuantities.put(product, productQuantities.getOrDefault(product, 0) + 1);
            }

            for (Map.Entry<Product, Integer> entry : productQuantities.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();
                orderContent.append(String.format(
                        "▸ %-15s ×%-2d @ ¥%-6.2f = ¥%-7.2f\n",
                        product.getName(),
                        quantity,
                        product.getPrice(),
                        product.getPrice() * quantity
                ));
            }

            // 订单金额
            orderContent.append("\n订单总价: ¥").append(String.format("%.2f", newOrder.getTotalPrice())).append("\n");

            // 将新订单内容添加到现有内容前面(最新订单显示在最上面)
            orderSummaryArea.setText(orderContent.toString() + "\n" + currentText);
            orderSummaryArea.setCaretPosition(0); // 滚动到顶部
        });
    }

    /**
     * 更新单个产品的库存显示
     * @param product 要更新显示的产品对象
     */
    private void updateProductStockDisplay(Product product) {
        SwingUtilities.invokeLater(() -> {
            JLabel stockLabel = stockLabels.get(product.getId());
            if (stockLabel != null) {
                stockLabel.setText("库存: " + product.getStock());

                // 根据库存量改变颜色提示
                if (product.getStock() < 3) {
                    stockLabel.setForeground(Color.RED);  // 库存紧张显示红色
                } else {
                    stockLabel.setForeground(new Color(100, 100, 100));  // 正常库存显示灰色
                }
            }
        });
    }


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
        Map<Product, Integer> orderedItems = new LinkedHashMap<>(); // 保持插入顺序

        // 1. 添加商品到订单并计算总价
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            if (entry.getValue() > 0) {
                Product product = entry.getKey();
                int quantity = entry.getValue();

                // 更新实际库存
                product.setStock(product.getStock() - quantity);
                updateProductStockDisplay(product); // 更新UI显示

                // 添加到订单
                order.addItem(product, quantity);
                product.incrementOrderCount(quantity);
                orderedItems.put(product, quantity);

                // 计算小计并累加总价
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
        double discountedPrice = totalPrice;
        String appliedDiscount = "无可用折扣";

        // 检查各种折扣条件
        if (totalPrice >= 50) {
            // 满50打8折
            double discount50 = totalPrice * 0.8;
            if (discount50 < discountedPrice) {
                discountedPrice = discount50;
                appliedDiscount = "满50元8折优惠";
            }
        } else if (totalPrice >= 30) {
            // 满30减5
            double discount30 = totalPrice - 5;
            if (discount30 < discountedPrice) {
                discountedPrice = discount30;
                appliedDiscount = "满30元减5元";
            }
        } else if (totalPrice >= 20) {
            // 满20减2
            double discount20 = totalPrice - 2;
            if (discount20 < discountedPrice) {
                discountedPrice = discount20;
                appliedDiscount = "满20元减2元";
            }
        }

        // 检查套餐折扣(2食品+1饮料)
        int foodCount = 0;
        int beverageCount = 0;
        for (Product p : order.getItems()) {
            if ("Food".equals(p.getCategory())) foodCount++;
            if ("Beverage".equals(p.getCategory())) beverageCount++;
        }
        if (foodCount >= 2 && beverageCount >= 1) {
            double mealDeal = totalPrice * 0.8;
            if (mealDeal < discountedPrice) {
                discountedPrice = mealDeal;
                appliedDiscount = "套餐优惠(8折)";
            }
        }

        // 检查蛋糕优惠(买3送1)
        int cakeCount = (int) order.getItems().stream()
                .filter(p -> "Cake".equals(p.getName()))
                .count();
        if (cakeCount >= 3) {
            double cakeDiscount = totalPrice - 4.0; // 减去一个蛋糕的价格
            if (cakeDiscount < discountedPrice) {
                discountedPrice = cakeDiscount;
                appliedDiscount = "蛋糕买三送一";
            }
        }

        // 设置订单最终价格
        order.setTotalPrice(discountedPrice);
        orderManager.addOrder(order);
        orderManager.addToTotalRevenue(discountedPrice);

        // 完成订单后
        order.setTotalPrice(discountedPrice);
        orderManager.addOrder(order);
        orderManager.addToTotalRevenue(discountedPrice);

        // 显示订单信息
        displayOrderInGUI(order);

        // 3. 完善订单摘要
        summary.append("\n────────────────────────────────\n");
        summary.append(String.format("%-20s: ¥%7.2f\n", "商品总价", totalPrice));
        summary.append(String.format("%-20s: %-10s\n", "应用优惠", appliedDiscount));
        summary.append(String.format("%-20s: ¥%7.2f\n", "折后价格", discountedPrice));
        summary.append(String.format("%-20s: ¥%7.2f\n", "节省金额", totalPrice - discountedPrice));
        summary.append("\n════════════════════════════════\n");
        summary.append("感谢您的惠顾，欢迎再次光临！\n");

        // 4. 更新UI显示
        allOrderSummaries.insert(0, summary.toString() + "\n\n"); // 新订单添加到前面
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

    public static void main(String[] args) {
        CoffeeShop coffeeShop = new CoffeeShop();
        coffeeShop.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                coffeeShop.orderManager.generateReport(coffeeShop.menu);

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
                    System.out.println("Error writing to order.txt: " + e.getMessage());
                }
            }
        });
    }
}
