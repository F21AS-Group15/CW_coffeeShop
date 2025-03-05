import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
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
    private double totalPrice; // Added totalPrice field

    public Order(String orderId, String timestamp, String customerId) {
        this.orderId = orderId;
        this.timeStamp = timestamp;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.isCompleted = false;
        this.totalPrice = 0; // Initialize to 0
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

    public String getOrderDetails() { return "Order ID: " + orderId + " Total: $" + getTotalPrice(); }
    public boolean isCompleted() { return isCompleted; }
    public void completeOrder() { this.isCompleted = true; }
    public void resetOrder() { this.isCompleted = false; }
    public List<Product> getItems() { return items; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() {return customerId;}
    public String getTimeStamp(){return timeStamp;}
}


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


class Menu {
    private List<Product> products;

    public Menu() { this.products = new ArrayList<>(); }
    public List<Product> getAllProducts() {
        return new ArrayList<>(products); // Returns a copy of the product
    }
    public void loadFromFile(String filePath) {}
    public void displayMenu() { products.forEach(p -> System.out.println(p.getDetails())); }
    public Product getProductById(String productId) { return products.stream().filter(p -> p.getId().equals(productId)).findFirst().orElse(null); }
    public void addProduct(Product product) { products.add(product); }
    public boolean removeProduct(String productId) { return products.removeIf(p -> p.getId().equals(productId)); }
}


class OrderManager {
    private List<Order> orders;
    private double totalRevenue; // Added totalRevenue field

    public OrderManager() {
        this.orders = new ArrayList<>();
        this.totalRevenue = 0;
    }

    public void generateReport(Menu menu) {
        System.out.println("=== Sales Report ===");
        for (Product product : menu.getAllProducts()) {
            System.out.println(product.getDetails() + " - Ordered " + product.getOrderCount() + " times");
        }
        System.out.println("Total Revenue: $" + totalRevenue); // Use totalRevenue directly
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

    // New method: Add the total price of the order to totalRevenue
    public void addToTotalRevenue(double amount) {
        this.totalRevenue += amount;
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

    // Abstract methods, subclasses implement specific discount rules
    public abstract double applyDiscount(Order order, double originalPrice);
}


class DiscountManager {
    private Map<String, Discount> discountRules;

    public DiscountManager() {
        this.discountRules = new HashMap<>();
        // Add default discount rule
        addDiscount(new DefaultDiscount());
        // Add "food_and_beverage" discount rule
        addDiscount(new FoodAndBeverageDiscount());
        // Add "free_cake" discount rule
        addDiscount(new FreeCakeDiscount());
    }

    // Add discount rule
    public void addDiscount(Discount discount) {
        discountRules.put(discount.getDiscountName(), discount);
    }

    // Remove discount rule
    public boolean removeDiscount(Discount discount) {
        return discountRules.remove(discount.getDiscountName()) != null;
    }

    // Apply discount rule, default to "default" discount rule
    public double applyDiscount(String discountName, Order order, double originalPrice) {
        // If discount name is empty or null, use default discount rule
        if (discountName == null || discountName.isEmpty()) {
            discountName = "default";
        }

        // Get the specified discount rule and apply it, if the rule does not exist, return the original price
        Discount discount = discountRules.getOrDefault(discountName, new DefaultDiscount());
        return discount.applyDiscount(order,originalPrice);
    }
}

// Default discount
class DefaultDiscount extends Discount {
    public DefaultDiscount() {
        super("default"); // Set discount name to "default"
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        // 20% off for orders over $50
        if (originalPrice >= 50) {
            return originalPrice*0.8;
        }
        // $5 off for orders over $30
        else if (originalPrice >= 30) {
            return originalPrice - 5;
        }
        // $2 off for orders over $20
        else if (originalPrice >= 20) {
            return originalPrice - 2;
        }

        return originalPrice; // Return original price if no conditions are met
    }
}

// Single set meal discount (2 foods + 1 beverage)
class FoodAndBeverageDiscount extends Discount {
    public FoodAndBeverageDiscount() {
        super("food_and_beverage"); // Set discount name to "food_and_beverage"
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        int foodCount = 0;
        int beverageCount = 0;

        // Traverse the items in the order, count the number of Food and Beverage items
        for (Product product : order.getItems()) {
            if ("Food".equals(product.getCategory())) {
                foodCount++;
            } else if ("Beverage".equals(product.getCategory())) {
                beverageCount++;
            }
        }

        // If conditions are met (2 or more Food items and 1 or more Beverage items), apply 20% off
        if (foodCount >= 2 && beverageCount >= 1) {
            return originalPrice * 0.8;
        }

        // Return original price if conditions are not met
        return originalPrice;
    }
}

// Free cake discount rule
class FreeCakeDiscount extends Discount {
    public FreeCakeDiscount() {
        super("free_cake"); // Set discount name to "free_cake"
    }

    @Override
    public double applyDiscount(Order order, double originalPrice) {
        int cakeCount = 0;

        // Traverse the items in the order, count the number of Cake items
        for (Product product : order.getItems()) {
            if ("Cake".equals(product.getName())) {
                cakeCount++;
            }
        }

        // If conditions are met (3 or more Cake items), deduct the price of one Cake
        if (cakeCount >= 3) {
            return originalPrice - 4.0;
        }

        // Return original price if conditions are not met
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
    private Map<Product, Integer> selectedProducts; // Product and corresponding purchase quantity

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
        // id, name, description, category, price, stock
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    menu.addProduct(new Product(parts[0], parts[1], parts[2], parts[3], Double.parseDouble(parts[4]), Integer.parseInt(parts[5])));
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
                if (parts.length == 5) {  // Ensure the format is: order ID, timestamp, customer ID, product ID, quantity
                    String orderId = parts[0];
                    String timestamp = parts[1];
                    String customerId = parts[2];
                    String productId = parts[3];
                    int quantity = Integer.parseInt(parts[4]);

                    // Create a new Order object
                    Order order = new Order(orderId, timestamp, customerId);

                    // Get the corresponding product based on the product ID
                    Product product = menu.getProductById(productId);
                    if (product != null) {
                        order.addItem(product, quantity);  // Add the product to the order
                    }

                    // Add the order to the order manager
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
        frame.setSize(800, 600); // Reduce window size
        frame.setLayout(new BorderLayout());

        // Gradient background
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // Ensure parent class drawing method is called
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(255, 223, 186); // Light orange
                Color color2 = new Color(255, 182, 193); // Light pink
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        frame.setContentPane(backgroundPanel);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false); // Transparent tabs

        // Product panel
        productPanel = new JPanel();
        productPanel.setLayout(new GridLayout(0, 2, 10, 10)); // Change to 2-column grid layout
        productPanel.setOpaque(false); // Make panel transparent

        for (Product product : menu.getAllProducts()) {
            // Create card panel
            JPanel cardPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g); // Ensure parent class drawing method is called
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(255, 255, 255, 200)); // Semi-transparent white background
                    g2d.fillRect(0, 0, getWidth(), getHeight()); // Fill background
                }
            };
            cardPanel.setOpaque(false); // Ensure panel is transparent
            cardPanel.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 2)); // Border
            cardPanel.setPreferredSize(new Dimension(350, 150)); // Adjust card size

            // Product info panel
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false); // Make panel transparent

            // Product name
            JLabel nameLabel = new JLabel("<html><b>" + product.getName() + "</b></html>");
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            infoPanel.add(nameLabel);

            // Product description
            JTextArea descriptionArea = new JTextArea(product.getDescription());
            descriptionArea.setFont(new Font("Arial", Font.PLAIN, 12));
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setEditable(false);
            descriptionArea.setOpaque(false); // Transparent background
            infoPanel.add(descriptionArea);

            // Product price
            JLabel priceLabel = new JLabel("Price: $" + product.getPrice());
            priceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            infoPanel.add(priceLabel);

            // Quantity selection panel
            JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            quantityPanel.setOpaque(false); // Make panel transparent

            JButton minusButton = new JButton("-");
            JLabel quantityLabel = new JLabel("0");
            quantityLabels.add(quantityLabel); // Add label to list
            JButton plusButton = new JButton("+");

            // Custom button style
            minusButton.setBackground(new Color(255, 183, 197)); // Cherry blossom pink
            minusButton.setForeground(Color.WHITE);
            minusButton.setFocusPainted(false);
            plusButton.setBackground(new Color(152, 251, 152)); // Matcha green
            plusButton.setForeground(Color.WHITE);
            plusButton.setFocusPainted(false);

            // Button click events
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
        productScroll.setOpaque(false); // Make scroll pane transparent
        productScroll.getViewport().setOpaque(false); // Make viewport transparent
        productScroll.setPreferredSize(new Dimension(700, 450)); // Adjust scroll pane size
        tabbedPane.addTab("Products", productScroll);

        // Order panel
        JPanel orderPanel = new JPanel();
        orderPanel.setOpaque(false); // Make panel transparent
        orderPanel.setPreferredSize(new Dimension(700, 450)); // Adjust order panel size
        tabbedPane.addTab("Orders", orderPanel);


        // Discount panel
        JPanel discountPanel = new JPanel();
        discountPanel.setOpaque(false); // Make panel transparent
        discountPanel.setLayout(new BorderLayout()); // Use BorderLayout

        // Create JTextArea to display discount information
        JTextArea discountTextArea = new JTextArea();
        discountTextArea.setEditable(false); // Set to non-editable
        discountTextArea.setOpaque(false); // Transparent background
        discountTextArea.setFont(new Font("Arial", Font.PLAIN, 14)); // Set font
        discountTextArea.setLineWrap(true); // Auto wrap
        discountTextArea.setWrapStyleWord(true); // Wrap by word

        // Set discount information text
        String discountInfo = "1. Free Cake\n" +
                "   - Buy three or more cakes and get one for free.\n\n" +
                "2. Single-Person Meal Deal\n" +
                "   - Purchase at least two food items and one beverage to enjoy a 20% discount.\n" +
                "   (This discount cannot be combined with the default discount.)\n\n" +
                "3. Default Discount\n" +
                "   - Spend $20 and get $2 off.\n" +
                "   - Spend $30 and get $5 off.\n" +
                "   - Spend $50 and enjoy a 20% discount.";

        discountTextArea.setText(discountInfo); // Set text content

        // Add JTextArea to JScrollPane to support scrolling
        JScrollPane discountScrollPane = new JScrollPane(discountTextArea);
        discountScrollPane.setOpaque(false); // Make scroll pane transparent
        discountScrollPane.getViewport().setOpaque(false); // Make viewport transparent

        // Add JScrollPane to discountPanel
        discountPanel.add(discountScrollPane, BorderLayout.CENTER);

        // Add discountPanel to tabbedPane
        tabbedPane.addTab("Discounts", discountPanel);


        backgroundPanel.add(tabbedPane, BorderLayout.CENTER);

        // Place order button
        placeOrderButton = new JButton("Place Order");
        placeOrderButton.setBackground(new Color(70, 130, 180)); // Starry sky blue
        placeOrderButton.setForeground(Color.WHITE);
        placeOrderButton.setFont(new Font("Arial", Font.BOLD, 14)); // Reduce font size
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

        // Count the quantity of each product in the order
        Map<Product, Integer> productQuantityMap = new HashMap<>();
        for (Product product : order.getItems()) {
            productQuantityMap.put(product, productQuantityMap.getOrDefault(product, 0) + 1);
        }

        // Display order information
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
                        .append(quantity)
                        .append("\n");
            }
        }

        orderTextArea.setText(orderDetails.toString());

        // Add order information to the "Order" interface
        JPanel orderPanel = (JPanel) ((JTabbedPane) frame.getContentPane().getComponent(0)).getComponent(1);
        orderPanel.add(new JScrollPane(orderTextArea));
        orderPanel.revalidate();
        orderPanel.repaint();
    }

    private void placeOrder() {
        Order order = new Order(UUID.randomUUID().toString(), new Date().toString(), "customer1");
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            if (entry.getValue() > 0) {
                order.addItem(entry.getKey(), entry.getValue());
                entry.getKey().incrementOrderCount(entry.getValue()); // Increase the order count of the product
                entry.getKey().setStock(entry.getKey().getStock() - entry.getValue()); // Reduce stock
            }
        }

        if (order.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No products selected!");
            return;
        }

        // Calculate the original total price
        double totalPrice = order.getTotalPrice();

        // Check if discount conditions are met
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

        // Apply discount rules
        if(cakeCount >= 3){
            totalPrice = discountManager.applyDiscount("free_cake", order, totalPrice);
            foodCount--; // Free cake does not count towards discount calculation
        }

        if (foodCount >= 2 && beverageCount >= 1) {
            totalPrice = discountManager.applyDiscount("food_and_beverage", order, totalPrice);
        }else {
            totalPrice = discountManager.applyDiscount("default", order, totalPrice);
        }

        // Update the total price of the order
        order.setTotalPrice(totalPrice);

        // Complete the order
        order.completeOrder();
        orderManager.addOrder(order);

        // Add the total price of the order to totalRevenue
        orderManager.addToTotalRevenue(totalPrice);

        // Display purchase success popup
        JOptionPane.showMessageDialog(frame, "Order Placed Successfully!\n" + order.getOrderDetails());

        // Clear selected products
        selectedProducts.clear();

        // Reset product quantity display in the interface
        for (JLabel quantityLabel : quantityLabels) {
            quantityLabel.setText("0");
        }

        // Display the order in the "Order" interface
        displayOrderInGUI(order);
    }


    public static void main(String[] args) {
        CoffeeShop coffeeShop = new CoffeeShop();
        coffeeShop.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Generate report
                coffeeShop.orderManager.generateReport(coffeeShop.menu);

                // Append orders to order.txt file
                try (BufferedWriter bw = new BufferedWriter(new FileWriter("orders.txt", true))) {
                    for (Order order : coffeeShop.orderManager.getOrders()) {
                        // Count the quantity of each product in the order
                        Map<Product, Integer> productQuantityMap = new HashMap<>();
                        for (Product product : order.getItems()) {
                            productQuantityMap.put(product, productQuantityMap.getOrDefault(product, 0) + 1);
                        }

                        // Write order information
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
