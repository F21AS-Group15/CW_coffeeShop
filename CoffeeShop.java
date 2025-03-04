import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

// 自定义异常：库存不足异常
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
    private int stock;      // 商品库存
    private int orderCount; // 记录商品被点单的次数

    public Product(String id, String name, String description, String category, double price, int stock) {
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

// Order 类
class Order {
    private String orderId;
    private String timeStamp;
    private String customerId;
    private List<Product> items;
    private boolean isCompleted;
    private double totalPrice; // 新增的 totalPrice 字段

    public Order(String orderId, String timestamp, String customerId) {
        this.orderId = orderId;
        this.timeStamp = timestamp;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.isCompleted = false;
        this.totalPrice = 0; // 初始化为0
    }

    public void addItem(Product product) { items.add(product); }
    public void addItem(Product product, int amount) {
        for (int i = 0; i < amount; i++){
            items.add(product);
        }
    }
    public void deleteItem(Product product) { items.remove(product); }

    // 获取订单总价
    public double getTotalPrice() {
        return totalPrice > 0 ? totalPrice : Math.round(items.stream().mapToDouble(Product::getPrice).sum() * 100.0) / 100.0;
    }

    // 设置总价
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = Math.round(totalPrice * 100.0) / 100.0;
    }

    public String getOrderDetails() { return "Order ID: " + orderId + " Total: $" + getTotalPrice(); }
    public boolean isCompleted() { return isCompleted; }
    public void completeOrder() { this.isCompleted = true; }
    public void resetOrder() { this.isCompleted = false; }
    public List<Product> getItems() { return items; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() {return customerId;}
    public String getTimeStamp(){return timeStamp;}
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
    private double totalRevenue; // 新增的 totalRevenue 字段

    public OrderManager() {
        this.orders = new ArrayList<>();
        this.totalRevenue = 0; // 初始化 totalRevenue 为 0
    }

    public void loadOrdersFromFile(String filePath) {} // TODO

    public void generateReport(Menu menu) {
        System.out.println("=== Sales Report ===");
        for (Product product : menu.getAllProducts()) {
            System.out.println(product.getDetails() + " - Ordered " + product.getOrderCount() + " times");
        }
        System.out.println("Total Revenue: $" + totalRevenue); // 直接使用 totalRevenue
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public List<Order> getOrders() {
        return orders;
    }

    public boolean removeOrder(String orderId) {
        return orders.removeIf(o -> o.getOrderDetails().contains(orderId));
    }

    // 新增方法：将订单的总价添加到 totalRevenue 中
    public void addToTotalRevenue(double amount) {
        this.totalRevenue += amount;
    }
}

//折扣抽象类
abstract class Discount {
    private String discountName;
    public Discount(String discountName) {
        this.discountName = discountName;
    }
    public String getDiscountName() {
        return discountName;
    }

    // 抽象方法，子类实现具体折扣规则
    public abstract double applyDiscount(Order order, double originalPrice);
}

//DiscountManager类
class DiscountManager {
    private Map<String, Discount> discountRules;

    public DiscountManager() {
        this.discountRules = new HashMap<>();
        // 默认添加 "default" 折扣规则
        addDiscount(new DefaultDiscount());
        // 添加 "food_and_beverage" 折扣规则
        addDiscount(new FoodAndBeverageDiscount());
        // 添加 "free_cake" 折扣规则
        addDiscount(new FreeCakeDiscount());
    }

    // 添加折扣规则
    public void addDiscount(Discount discount) {
        discountRules.put(discount.getDiscountName(), discount);
    }

    // 删除折扣规则
    public boolean removeDiscount(Discount discount) {
        return discountRules.remove(discount.getDiscountName()) != null;
    }

    // 应用折扣规则，默认使用"default"折扣规则
    public double applyDiscount(String discountName, Order order, double originalPrice) {
        // 如果折扣名称为空或null，使用默认折扣规则
        if (discountName == null || discountName.isEmpty()) {
            discountName = "default";
        }


        // 获取指定折扣规则并应用，如果没有该规则，则返回原价
        Discount discount = discountRules.getOrDefault(discountName, new DefaultDiscount());
        return discount.applyDiscount(order,originalPrice);
    }
}

//默认折扣
class DefaultDiscount extends Discount {
    public DefaultDiscount() {
        super("default"); // 设置折扣名称为 "default"
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
//        double total = order.getTotalPrice();

        // 满50打8折
        if (originalPrice >= 50) {
            return originalPrice*0.8;
        }
        // 满30减5
        else if (originalPrice >= 30) {
            return originalPrice - 5;
        }
        // 满20减2
        else if (originalPrice >= 20) {
            return originalPrice - 2;
        }

        return originalPrice; // 不满足时，返回原价
    }
}

// Food 和 Beverage 折扣规则
class FoodAndBeverageDiscount extends Discount {
    public FoodAndBeverageDiscount() {
        super("food_and_beverage"); // 设置折扣名称为 "food_and_beverage"
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        int foodCount = 0;
        int beverageCount = 0;

        // 遍历订单中的商品，统计 Food 和 Beverage 的数量
        for (Product product : order.getItems()) {
            if ("Food".equals(product.getCategory())) {
                foodCount++;
            } else if ("Beverage".equals(product.getCategory())) {
                beverageCount++;
            }
        }

        // 如果满足条件（2个或以上 Food 且 1个或以上 Beverage），则打8折
        if (foodCount >= 2 && beverageCount >= 1) {
            return originalPrice * 0.8;
        }

        // 不满足条件时，返回原价
        return originalPrice;
    }
}

// Free cake 折扣规则
class FreeCakeDiscount extends Discount {
    public FreeCakeDiscount() {
        super("free_cake"); // 设置折扣名称为 "free_cake"
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        int cakeCount = 0;

        // 遍历订单中的商品，统计 Cake 的数量
        for (Product product : order.getItems()) {
            if ("Cake".equals(product.getName())) {
                cakeCount++;
            }
        }

        // 如果满足条件（购买的Cake数量大于或等于3），则免去一个Cake的价格
        if (cakeCount >= 3) {
            return originalPrice - 4.0;
        }

        // 不满足条件时，返回原价
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
    private Map<Product, Integer> selectedProducts; // 商品和对应的购买数量

    public CoffeeShop() {
        menu = new Menu();
        orderManager = new OrderManager();
        discountManager = new DiscountManager();
        selectedProducts = new HashMap<>();
        loadMenuFromFile("menu.txt");
        loadOrdersFromFile("orders.txt");
        setupGUI();
    }

    private void loadMenuFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    menu.addProduct(new Product(parts[0], parts[1], parts[2], parts[3], Double.parseDouble(parts[4]), Integer.parseInt(parts[5])));
                }
            }
        } catch (IOException e) {
            //TODO:
            System.out.println("Error loading menu: " + e.getMessage());
        }
    }

    private void loadOrdersFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {  // 确保格式为：订单编号,时间戳,用户编号,商品编号,点单数量
                    String orderId = parts[0];
                    String timestamp = parts[1];
                    String customerId = parts[2];
                    String productId = parts[3];
                    int quantity = Integer.parseInt(parts[4]);

                    // 创建一个新的 Order 对象
                    Order order = new Order(orderId, timestamp, customerId);

                    // 根据产品ID获取对应的产品
                    Product product = menu.getProductById(productId);
                    if (product != null) {
                        order.addItem(product, quantity);  // 将产品添加到订单
                    }

                    // 将订单添加到订单管理器
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
                try {
                    if (quantity >= product.getStock()) {
                        throw new OutOfStockException("Out of stock!");
                    }
                    quantity++;
                    selectedProducts.put(product, quantity);
                    quantityLabel.setText(String.valueOf(quantity));
                } catch (OutOfStockException ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage());
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
        discountPanel.setLayout(new BorderLayout()); // 使用 BorderLayout 布局

        // 创建 JTextArea 来显示折扣信息
        JTextArea discountTextArea = new JTextArea();
        discountTextArea.setEditable(false); // 设置为不可编辑
        discountTextArea.setOpaque(false); // 透明背景
        discountTextArea.setFont(new Font("Arial", Font.PLAIN, 14)); // 设置字体
        discountTextArea.setLineWrap(true); // 自动换行
        discountTextArea.setWrapStyleWord(true); // 按单词换行

        // 设置折扣信息文本
        String discountInfo = "1. Free Cake\n" +
                "   - Buy three or more cakes and get one for free.\n\n" +
                "2. Single-Person Meal Deal\n" +
                "   - Purchase at least two food items and one beverage to enjoy a 20% discount.\n" +
                "   (This discount cannot be combined with the default discount.)\n\n" +
                "3. Default Discount\n" +
                "   - Spend $20 and get $2 off.\n" +
                "   - Spend $30 and get $5 off.\n" +
                "   - Spend $50 and enjoy a 20% discount.";

        discountTextArea.setText(discountInfo); // 设置文本内容

        // 将 JTextArea 添加到 JScrollPane 中，以便支持滚动
        JScrollPane discountScrollPane = new JScrollPane(discountTextArea);
        discountScrollPane.setOpaque(false); // 使滚动面板透明
        discountScrollPane.getViewport().setOpaque(false); // 使视口透明

        // 将 JScrollPane 添加到 discountPanel 中
        discountPanel.add(discountScrollPane, BorderLayout.CENTER);

        // 将 discountPanel 添加到 tabbedPane 中
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

    private void displayOrderInGUI(Order order) {
        JTextArea orderTextArea = new JTextArea();
        orderTextArea.setEditable(false);
        orderTextArea.setOpaque(false);

        // 统计每个商品的点单数量
        Map<Product, Integer> productQuantityMap = new HashMap<>();
        for (Product product : order.getItems()) {
            productQuantityMap.put(product, productQuantityMap.getOrDefault(product, 0) + 1);
        }

        // 显示订单信息
        StringBuilder orderDetails = new StringBuilder();
        for (Map.Entry<Product, Integer> entry : productQuantityMap.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            if (quantity > 0) {
                orderDetails.append(order.getOrderId())
                        .append(", ")
                        .append(order.getTimeStamp())
                        .append(", ")
                        .append(order.getCustomerId())
                        .append(", ")
                        .append(product.getId())
                        .append(", ")
                        .append(quantity) // 使用实际点单数量
                        .append("\n");
            }
        }

        orderTextArea.setText(orderDetails.toString());

        // 将订单信息添加到“Order”界面
        JPanel orderPanel = (JPanel) ((JTabbedPane) frame.getContentPane().getComponent(0)).getComponent(1);
//        orderPanel.removeAll(); // 清空之前的订单信息
        orderPanel.add(new JScrollPane(orderTextArea));
        orderPanel.revalidate();
        orderPanel.repaint();
    }

    private void placeOrder() {
        Order order = new Order(UUID.randomUUID().toString(), new Date().toString(), "customer1");
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            if (entry.getValue() > 0) {
                order.addItem(entry.getKey(), entry.getValue());
                entry.getKey().incrementOrderCount(entry.getValue()); // 增加商品的点单次数
                entry.getKey().setStock(entry.getKey().getStock() - entry.getValue()); // 减少库存
            }
        }

        if (order.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No products selected!");
            return;
        }

        // 计算原始总价
        double totalPrice = order.getTotalPrice();

        // 检查是否满足 Food 和 Beverage 的条件
        int foodCount = 0;
        int beverageCount = 0;
        int cakeCount = 0;
        for (Product product : order.getItems()) {
            if ("Food".equals(product.getCategory())) {
                foodCount++;
            } else if ("Beverage".equals(product.getCategory())) {
                beverageCount++;
            }
            if ("Cake".equals(product.getName())){
                cakeCount++;
            }

        }


        // 应用折扣规则
        if(cakeCount >= 3){
            System.out.println("1111111111111");
            totalPrice = discountManager.applyDiscount("free_cake", order, totalPrice);
            foodCount--; //免费的蛋糕不计入折扣计算
        }

        if (foodCount >= 2 && beverageCount >= 1) {
            totalPrice = discountManager.applyDiscount("food_and_beverage", order, totalPrice);
        }else {
            totalPrice = discountManager.applyDiscount("default", order, totalPrice);
        }

        // 更新订单的总价
        order.setTotalPrice(totalPrice);

        // 完成订单
        order.completeOrder();
        orderManager.addOrder(order);

        // 将订单的总价添加到 totalRevenue 中
        orderManager.addToTotalRevenue(totalPrice);

        // 显示购买成功弹窗
        JOptionPane.showMessageDialog(frame, "Order Placed Successfully!\n" + order.getOrderDetails());

        // 清空已选商品
        selectedProducts.clear();

        // 重置界面中的商品数量显示
        for (JLabel quantityLabel : quantityLabels) {
            quantityLabel.setText("0");
        }

        // 将订单显示在“Order”界面中
        displayOrderInGUI(order);
    }


    public static void main(String[] args) {
        CoffeeShop coffeeShop = new CoffeeShop();
        coffeeShop.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // 生成报告
                coffeeShop.orderManager.generateReport(coffeeShop.menu);

                // 将订单追加到 order.txt 文件中
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("orders.txt", true))) {
                    for (Order order : coffeeShop.orderManager.getOrders()) {
                        // 统计每个商品的点单数量
                        Map<Product, Integer> productQuantityMap = new HashMap<>();
                        for (Product product : order.getItems()) {
                            productQuantityMap.put(product, productQuantityMap.getOrDefault(product, 0) + 1);
                        }

                        // 写入订单信息
                        for (Map.Entry<Product, Integer> entry : productQuantityMap.entrySet()) {
                            Product product = entry.getKey();
                            int quantity = entry.getValue();

                            if (quantity > 0) {
                                bw.write(order.getOrderId() + "," +
                                        order.getTimeStamp() + "," +
                                        order.getCustomerId() + "," +
                                        product.getId() + "," +
                                        quantity); // 使用实际点单数量
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
