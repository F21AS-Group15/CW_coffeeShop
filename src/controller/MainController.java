package controller;

import model.*;
import util.CoffeeShopLogger;
import util.OrderQueue;
import view.MainView;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.awt.event.ActionEvent;

public class MainController {
    private volatile boolean isMonitoring = false; // Control the status of the monitoring thread
    public DiscountCalculator discountCalculator;
    public MainView view;
    public Menu menu;
    public OrderManager orderManager;
    public CoffeeShopSimulator simulator;
    public Map<Product, Integer> selectedProducts = new HashMap<>();
    public OrderQueue orderQueue;

    public MainController() {
        this.discountCalculator = new DiscountCalculator();
        // 1. Initialize models
        this.menu = new Menu();
        this.orderManager = new OrderManager();
        this.orderQueue = new OrderQueue(100); // Set queue capacity
        this.simulator = new CoffeeShopSimulator(orderQueue, 2, 2000, orderManager); // Default order processing time: 2000ms
        loadInitialData();

        // 2. Initialize the view
        this.view = new MainView();
        setupEventHandlers();
        initProductDisplay();

        // 3. Show the window
        view.setVisible(true);

        if (orderQueue == null) throw new IllegalStateException("orderQueue not initialized");
        if (orderManager == null) throw new IllegalStateException("orderManager not initialized");
        if (menu == null) throw new IllegalStateException("menu not initialized");

        // 4. Start monitoring for order completion
        startCompletionMonitor();

        System.out.println("[DEBUG] Initialization completed, all components are ready");
    }

    // Start the order completion monitoring thread
    private void startCompletionMonitor() {
        if (isMonitoring) {
            return; // Avoid starting repeatedly
        }

        isMonitoring = true;

        Thread completionMonitorThread = new Thread(() -> {
            while (isMonitoring) {
                try {
                    // Check order status every second
                    Thread.sleep(1000);

                    // Check if all orders are completed
                    if (simulator != null && simulator.areAllOrdersCompleted()) {
                        SwingUtilities.invokeLater(this::stopSimulation);
                        break; // Exit monitoring loop
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        completionMonitorThread.start(); // Start the monitoring thread
    }

    private void loadInitialData() {
        try {
            menu.loadFromFile("src\\menu.txt");
            orderManager.loadFromFile("src\\pre_orders.txt", menu);
        } catch (Exception e) {
            showErrorDialog("Initialization Error", "Failed to load data: " + e.getMessage());
        }
    }

    private void setupEventHandlers() {
        // Order button
        view.setOrderButtonListener(this::handlePlaceOrder);

        // Simulation control buttons
        view.setStartSimulationListener(e -> startSimulation());
        view.setStopSimulationListener(e -> stopSimulation());
    }

    private void initProductDisplay() {
        menu.getAllProducts().forEach(product -> {
            view.addProductCard(
                    product,
                    e -> handleAddProduct(product),
                    e -> handleRemoveProduct(product)
            );

            // Initialize selected quantity
            selectedProducts.put(product, 0);
        });
    }

    // Order processing logic
    private void handlePlaceOrder(ActionEvent e) {
        if (selectedProducts.values().stream().allMatch(q -> q == 0)) {
            showWarningDialog("Order Error", "Please select at least one product");
            return;
        }

        if (simulator == null || !simulator.isRunning()) {
            showWarningDialog("System Not Ready", "Please start the simulation first");
            return;
        }

        try {
            // 1. Create new order
            Order newOrder = createNewOrder();

            // 2. Add products to order
            addItemsToOrder(newOrder);

            // 3. Calculate and apply discounts
            applyDiscounts(newOrder);

            // 4. Submit order to system
            submitOrder(newOrder);

            // 5. Update UI state
            resetOrderUI();
            showSuccessMessage(newOrder);

        } catch (Exception ex) {
            handleOrderException(ex);
        }
    }

    private Order createNewOrder() {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        return new Order(orderId, timestamp, "User", "WALK_IN");
    }

    private void addItemsToOrder(Order order) throws OutOfStockException {
        for (Map.Entry<Product, Integer> entry : selectedProducts.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            if (quantity > 0) {
                if (quantity > product.getStock()) {
                    throw new OutOfStockException(
                            product.getName() + " is out of stock (Remaining: " + product.getStock() + ")");
                }
                order.addItem(product, quantity);
                product.reduceStock(quantity);
                view.updateProductStock(product);
            }
        }
    }

    private void applyDiscounts(Order order) {
        DiscountCalculator.DiscountResult discount =
                new DiscountCalculator().calculateBestDiscount(order);

        order.setDiscountAmount(discount.discountAmount);
        order.setTotalPrice(order.calculateOriginalPrice() - discount.discountAmount);

        // Display discounted order details
        view.appendOrderNote(order.getOrderDetails());
    }

    // Simulation control logic
    private void startSimulation() {
        new Thread(() -> {
            try {
                // 1. Validate components
                if (orderQueue == null || orderManager == null) {
                    throw new IllegalStateException("Core components are not initialized");
                }

                // 2. Create simulator
                this.simulator = new CoffeeShopSimulator(
                        orderQueue,
                        2,
                        view.getSimulationSpeed(),
                        orderManager
                );

                // 3. Set observer
                simulator.addObserver(view);

                // 4. Start simulation
                simulator.startSimulation();

                // 5. Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    view.setSimulationControlsEnabled(true);
                    view.showMessage("Success", "Simulation started", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                e.printStackTrace();

                SwingUtilities.invokeLater(() ->
                        showErrorDialog("Start Failed",
                                "Reason: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                        )
                );
            }
        }).start();
    }

    private void stopSimulation() {
        if (simulator != null) {
            simulator.stopSimulation();
            view.setSimulationControlsEnabled(false);
            CoffeeShopLogger.getInstance().logEvent("Simulation stopped");
            CoffeeShopLogger.getInstance().logEvent(orderManager.generateReport());

            try {
                CoffeeShopLogger.getInstance().saveToFile("src\\coffee_shop_log.txt");
            } catch (Exception e) {
                showErrorDialog("Log Error", "Failed to save log: " + e.getMessage());
            }

            System.exit(0);
        }
    }

    // Product selection handlers
    private void handleAddProduct(Product product) {
        try {
            int current = selectedProducts.get(product);
            if (current >= product.getStock()) {
                throw new OutOfStockException("Stock limit reached");
            }

            selectedProducts.put(product, current + 1);
            updatePriceDisplay();

        } catch (OutOfStockException e) {
            showWarningDialog("Stock Warning", e.getMessage());
        }
    }

    private void handleRemoveProduct(Product product) {
        int current = selectedProducts.get(product);
        if (current > 0) {
            selectedProducts.put(product, current - 1);
            updatePriceDisplay();
        }
    }

    private void updatePriceDisplay() {
        // Create a temporary order for calculation
        Order tempOrder = createTempOrder();

        // Calculate discount
        DiscountCalculator.DiscountResult discountResult = calculateDiscount(tempOrder);

        double total = tempOrder.calculateOriginalPrice();
        double discountedPrice = total - discountResult.discountAmount;

        view.updatePriceDisplay(
                total,
                discountedPrice,
                discountResult.description // Display discount description
        );
    }

    private Order createTempOrder() {
        Order tempOrder = new Order("TEMP", "TEMP", "TEMP", "TEMP");
        selectedProducts.forEach((product, quantity) -> {
            if (quantity > 0) {
                tempOrder.addItem(product, quantity);
            }
        });
        return tempOrder;
    }

    private DiscountCalculator.DiscountResult calculateDiscount(Order order) {
        return discountCalculator.calculateBestDiscount(order);
    }

    private void submitOrder(Order order) throws InterruptedException {
        orderManager.addOrder(order);
        simulator.getOrderQueue().addOrder(order);
        CoffeeShopLogger.getInstance().logEvent(
                "New order submitted: " + order.getOrderId() + " Total: " + order.getTotalPrice());
    }

    private void resetOrderUI() {
        selectedProducts.replaceAll((p, q) -> 0);
        view.resetProductSelections();
        view.resetOrderSummariesSelections();
    }

    private void showSuccessMessage(Order order) {
        view.showMessage(
                "Order Submitted Successfully",
                String.format("Order ID: %s\nTotal: ¥%.2f", order.getOrderId(), order.getTotalPrice()),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleOrderException(Exception ex) {
        CoffeeShopLogger.getInstance().logEvent("Order Error: " + ex.getMessage());
        if (ex instanceof OutOfStockException) {
            showWarningDialog("Stock Error", ex.getMessage());
        } else {
            showErrorDialog("System Error", ex.getMessage());
        }
    }

    // UI helper methods
    private void showWarningDialog(String title, String message) {
        view.showMessage(title, message, JOptionPane.WARNING_MESSAGE);
    }

    private void showErrorDialog(String title, String message) {
        view.showMessage(title, message, JOptionPane.ERROR_MESSAGE);
    }
}
