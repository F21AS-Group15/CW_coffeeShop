import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

// 自定义异常：库存不足异常
class OutOfStockException extends Exception {
    public OutOfStockException(String message) {
        super(message);
    }
}

// Product 类
class Product {
    private String id;
    private String name;
    private String description;
    private String category;
    private double price;
    private int orderCount; // 记录商品被点单的次数

    public Product(String id, String name, String description, String category, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.orderCount = 0;
    }

    public String getDetails() {
        return name + ": " + description + " (" + category + ") - $" + price;
    }

    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getId() { return id; }
    public int getOrderCount() { return orderCount; }
    public void incrementOrderCount(Integer value) { this.orderCount++; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }

    // 添加 getName 和 getDescription 方法
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}

// Order 类
class Order {
    private String orderId;
    private String timeStamp;
    private String customerId;
    private List<Product> items;
    private boolean isCompleted;

    public Order(String orderId, String timestamp, String customerId) {
        this.orderId = orderId;
        this.timeStamp = timestamp;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.isCompleted = false;
    }

    public void addItem(Product product) { items.add(product); }
    public void addItem(Product product, int amount) {
        for (int i = 0; i < amount; i++){
            items.add(product);
        }
    }
    public void deleteItem(Product product) { items.remove(product); }
    public double getTotalPrice() { return items.stream().mapToDouble(Product::getPrice).sum(); }
    public String getOrderDetails() { return "Order ID: " + orderId + " Total: $" + getTotalPrice(); }
    public boolean isCompleted() { return isCompleted; }
    public void completeOrder() { this.isCompleted = true; }
    public void resetOrder() { this.isCompleted = false; }
    public List<Product> getItems() { return items; }
}

// Customer 类
class Customer {
    private String customerId;
    private String name;
    private List<Order> orders;

    public Customer(String customerId, String name) {
        this.customerId = customerId;
        this.name = name;
        this.orders = new ArrayList<>();
    }

    public void placeOrder(Order order) { orders.add(order); }
    public List<Order> getOrderHistory() { return orders; }
}

// Menu 类
class Menu {
    private List<Product> products;

    public Menu() { this.products = new ArrayList<>(); }
    public List<Product> getAllProducts() {
        return new ArrayList<>(products); // 返回产品的副本
    }
    public void loadFromFile(String filePath) {}
    public void displayMenu() { products.forEach(p -> System.out.println(p.getDetails())); }
    public Product getProductById(String productId) { return products.stream().filter(p -> p.getId().equals(productId)).findFirst().orElse(null); }
    public void addProduct(Product product) { products.add(product); }
    public boolean removeProduct(String productId) { return products.removeIf(p -> p.getId().equals(productId)); }
}

// OrderManager 类
class OrderManager {
    private List<Order> orders;

    public OrderManager() { this.orders = new ArrayList<>(); }
    public void loadOrdersFromFile(String filePath) {}
    public void generateReport(Menu menu) {
        double totalRevenue = 0;
        System.out.println("=== Sales Report ===");
        for (Product product : menu.getAllProducts()) {
            System.out.println(product.getDetails() + " - Ordered " + product.getOrderCount() + " times");
        }
        for (Order order : orders) {
            totalRevenue += order.getTotalPrice();
        }
        System.out.println("Total Revenue: $" + totalRevenue);
    }
    public void addOrder(Order order) { orders.add(order); }
    public boolean removeOrder(String orderId) { return orders.removeIf(o -> o.getOrderDetails().contains(orderId)); }
}

// Discount 类
class Discount {
    private String discountName;

    public Discount(String discountName) { this.discountName = discountName; }
    public String getDiscountName() { return discountName; }
    public void setDiscountName(String discountName) { this.discountName = discountName; }
    public double applyDiscount(Order order) { return order.getTotalPrice(); }
}

// DiscountManager 类
class DiscountManager {
    private Map<String, Discount> discountRules;

    public DiscountManager() { this.discountRules = new HashMap<>(); }
    public void addDiscount(Discount discount) { discountRules.put(discount.getDiscountName(), discount); }
    public boolean removeDiscount(Discount discount) { return discountRules.remove(discount.getDiscountName()) != null; }
    public double applyDiscount(String discountName, Order order) { return discountRules.getOrDefault(discountName, new Discount("None")).applyDiscount(order); }
    public void displayDiscount() { discountRules.keySet().forEach(System.out::println); }
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
    private Map<Product, Integer> selectedProducts; // 商品和对应的购买数量

    public CoffeeShop() {
        menu = new Menu();
        orderManager = new OrderManager();
        discountManager = new DiscountManager();
        selectedProducts = new HashMap<>();
        loadMenuFromFile("D:\\PPSTAR\\github\\untitled\\src\\menu.txt");
        loadOrdersFromFile("D:\\PPSTAR\\github\\untitled\\src\\orders.txt");
        setupGUI();
    }

    private void loadMenuFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    menu.addProduct(new Product(parts[0], parts[1], parts[2], parts[3], Double.parseDouble(parts[4])));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading menu: " + e.getMessage());
        }
    }

    private void loadOrdersFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 3) {  // 确保至少有 orderId, timestamp 和 customerId
                    String orderId = parts[0];
                    String timestamp = parts[1];
                    String customerId = parts[2];

                    // 创建一个新的 Order 对象
                    Order order = new Order(orderId, timestamp, customerId);

                    // 将所有产品添加到订单中
                    for (int i = 3; i < parts.length; i++) {
                        String productId = parts[i];

                        // 根据产品ID获取对应的产品（假设你有一个 menu 对象）
                        Product product = menu.getProductById(productId);
                        if (product != null) {
                            order.addItem(product, 1);  // 将产品添加到订单
                        }
                    }

                    // 假设你有一个 orders 列表来存储所有的订单
                    orderManager.addOrder(order);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading orders: " + e.getMessage());
        }
    }

    private void setupGUI() {
        frame = new JFrame("Order Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600); // 缩小窗口尺寸
        frame.setLayout(new BorderLayout());

        // 渐变背景
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // 确保调用父类绘制方法
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(255, 223, 186); // 浅橙色
                Color color2 = new Color(255, 182, 193); // 浅粉色
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        frame.setContentPane(backgroundPanel);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false); // 标签透明

        // 商品面板
        productPanel = new JPanel();
        productPanel.setLayout(new GridLayout(0, 2, 10, 10)); // 改为 2 列网格布局
        productPanel.setOpaque(false); // 使面板透明

        for (Product product : menu.getAllProducts()) {
            // 创建卡片面板
            JPanel cardPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g); // 确保调用父类绘制方法
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(255, 255, 255, 200)); // 半透明白色背景
                    g2d.fillRect(0, 0, getWidth(), getHeight()); // 填充背景
                }
            };
            cardPanel.setOpaque(false); // 确保面板透明
            cardPanel.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 2)); // 边框
            cardPanel.setPreferredSize(new Dimension(350, 150)); // 调整卡片尺寸

            // 商品信息面板
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false); // 使面板透明

            // 商品名称
            JLabel nameLabel = new JLabel("<html><b>" + product.getName() + "</b></html>");
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            infoPanel.add(nameLabel);

            // 商品描述
            JTextArea descriptionArea = new JTextArea(product.getDescription());
            descriptionArea.setFont(new Font("Arial", Font.PLAIN, 12));
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setEditable(false);
            descriptionArea.setOpaque(false); // 透明背景
            infoPanel.add(descriptionArea);

            // 商品价格
            JLabel priceLabel = new JLabel("Price: $" + product.getPrice());
            priceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            infoPanel.add(priceLabel);

            // 数量选择面板
            JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            quantityPanel.setOpaque(false); // 使面板透明

            JButton minusButton = new JButton("-");
            JLabel quantityLabel = new JLabel("0");
            quantityLabels.add(quantityLabel); // 将标签添加到列表
            JButton plusButton = new JButton("+");

            // 自定义按钮样式
            minusButton.setBackground(new Color(255, 183, 197)); // 樱花粉
            minusButton.setForeground(Color.WHITE);
            minusButton.setFocusPainted(false);
            plusButton.setBackground(new Color(152, 251, 152)); // 抹茶绿
            plusButton.setForeground(Color.WHITE);
            plusButton.setFocusPainted(false);

            // 按钮点击事件
            minusButton.addActionListener(e -> {
                int quantity = selectedProducts.getOrDefault(product, 0);
                if (quantity > 0) {
                    quantity--;
                    selectedProducts.put(product, quantity);
                    quantityLabel.setText(String.valueOf(quantity));
                }
            });

            plusButton.addActionListener(e -> {
                int quantity = selectedProducts.getOrDefault(product, 0);
                quantity++;
                selectedProducts.put(product, quantity);
                quantityLabel.setText(String.valueOf(quantity));
            });

            quantityPanel.add(minusButton);
            quantityPanel.add(quantityLabel);
            quantityPanel.add(plusButton);
            infoPanel.add(quantityPanel);

            cardPanel.add(infoPanel, BorderLayout.CENTER);
            productPanel.add(cardPanel);
        }

        JScrollPane productScroll = new JScrollPane(productPanel);
        productScroll.setOpaque(false); // 使滚动面板透明
        productScroll.getViewport().setOpaque(false); // 使视口透明
        productScroll.setPreferredSize(new Dimension(700, 450)); // 调整滚动面板尺寸
        tabbedPane.addTab("Products", productScroll);

        // 订单面板
        JPanel orderPanel = new JPanel();
        orderPanel.setOpaque(false); // 使面板透明
        orderPanel.setPreferredSize(new Dimension(700, 450)); // 调整订单面板尺寸
        tabbedPane.addTab("Orders", orderPanel);

        // 折扣面板
        JPanel discountPanel = new JPanel();
        discountPanel.setOpaque(false); // 使面板透明
        discountPanel.setPreferredSize(new Dimension(700, 450)); // 调整折扣面板尺寸
        tabbedPane.addTab("Discounts", discountPanel);

        backgroundPanel.add(tabbedPane, BorderLayout.CENTER);

        // 下单按钮
        placeOrderButton = new JButton("Place Order");
        placeOrderButton.setBackground(new Color(70, 130, 180)); // 星空蓝
        placeOrderButton.setForeground(Color.WHITE);
        placeOrderButton.setFont(new Font("Arial", Font.BOLD, 14)); // 缩小字体
        placeOrderButton.setFocusPainted(false);
        placeOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placeOrder();
            }
        });
        backgroundPanel.add(placeOrderButton, BorderLayout.SOUTH);

        frame.setVisible(true);
    }
    private void placeOrder() {
        // 创建订单
        Order order = new Order(UUID.randomUUID().toString(), new Date().toString(), "customer1");
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            if (entry.getValue() > 0) {
                order.addItem(entry.getKey(), entry.getValue());
                entry.getKey().incrementOrderCount(entry.getValue());
            }
        }

        if (order.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No products selected!");
            return;
        }

        // 应用折扣
        double totalPrice = order.getTotalPrice();
        totalPrice = discountManager.applyDiscount("default", order);
        orderManager.addOrder(order);

        // 显示购买成功弹窗
        JOptionPane.showMessageDialog(frame, "Order Placed Successfully!\n" + order.getOrderDetails());

        // 清空已选商品并重置数量显示
        selectedProducts.clear();
        for (JLabel label : quantityLabels) {
            label.setText("0");
        }
        productPanel.revalidate(); // 刷新布局
        productPanel.repaint();    // 重绘界面
    }

    public static void main(String[] args) {
        CoffeeShop coffeeShop = new CoffeeShop();
        coffeeShop.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                coffeeShop.orderManager.generateReport(coffeeShop.menu);
            }
        });
    }
}
