package view;

import model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainView extends JFrame implements QueueObserver {
    // Core components
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JPanel productPanel = new JPanel();
    private final JTextArea orderSummaryArea = new JTextArea();
    private final JTextArea queueTextArea = new JTextArea();
    private final JTextArea serversTextArea = new JTextArea();
    private final JButton placeOrderButton = new JButton("Place Order");
    private final JButton startSimulationButton = new JButton("Start Simulation");
    private final JButton stopSimulationButton = new JButton("Stop Simulation");
    private final JSlider speedSlider = new JSlider(500, 5000, 2000);
    private final JLabel totalPriceLabel = new JLabel("$0.00");
    private final JLabel discountedPriceLabel = new JLabel("$0.00");
    private final JLabel discountInfoLabel = new JLabel("No Discount");

    // Status tracking
    private final Map<String, JLabel> stockLabels = new HashMap<>();
    private final Map<Product, JLabel> quantityLabels = new HashMap<>();
    private final StringBuilder allOrderSummaries = new StringBuilder();


    public MainView() {
        setupUI();
    }

    private void setupUI() {
        // 1. Main window configuration
        setTitle("Coffee Shop Order Management System");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Gradient background panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                Color color1 = new Color(255, 248, 240);  // Light beige
                Color color2 = new Color(220, 240, 255);  // Light blue
                GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);

        // 2. Top price display
        add(createPricePanel(), BorderLayout.NORTH);

        // 3. Main tabs
        initProductTab();
        initOrderTab();
        initDiscountTab();
        initSimulationTab();
        backgroundPanel.add(tabbedPane, BorderLayout.CENTER);

        // 4. Place order button
        configureOrderButton();
        backgroundPanel.add(placeOrderButton, BorderLayout.SOUTH);
    }

    private JPanel createPricePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel totalLabel = new JLabel("Total Price:");
        totalLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        panel.add(totalLabel);

        totalPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        totalPriceLabel.setForeground(new Color(0, 100, 0));  // Dark green
        panel.add(totalPriceLabel);

        JLabel discountLabel = new JLabel("Discounted Price:");
        discountLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        panel.add(discountLabel);

        discountedPriceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        discountedPriceLabel.setForeground(new Color(200, 0, 0));  // Dark red
        panel.add(discountedPriceLabel);

        discountInfoLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        discountInfoLabel.setForeground(new Color(150, 0, 150));
        panel.add(discountInfoLabel);

        return panel;
    }


    private void initProductTab() {
        productPanel.setLayout(new GridLayout(0, 2, 15, 15));
        productPanel.setOpaque(false);
        productPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JScrollPane productScroll = new JScrollPane(productPanel);
        productScroll.setOpaque(false);
        productScroll.getViewport().setOpaque(false);
        tabbedPane.addTab("Product Menu", productScroll);
    }


    private void initOrderTab() {
        orderSummaryArea.setEditable(false);
        orderSummaryArea.setOpaque(false);
        orderSummaryArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        orderSummaryArea.setLineWrap(true);
        orderSummaryArea.setWrapStyleWord(true);

        JScrollPane orderScroll = new JScrollPane(orderSummaryArea);
        orderScroll.setOpaque(false);
        orderScroll.getViewport().setOpaque(false);
        tabbedPane.addTab("Live Orders", orderScroll);
    }


    private void initDiscountTab() {
        JPanel discountPanel = new JPanel(new BorderLayout());
        discountPanel.setOpaque(false);
        discountPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JTextArea discountTextArea = new JTextArea();
        discountTextArea.setEditable(false);
        discountTextArea.setOpaque(false);
        discountTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        discountTextArea.setLineWrap(true);
        discountTextArea.setWrapStyleWord(true);

        String discountInfo = "Current Discount Promotions:\n\n" +
                "1. Cake Special\n" +
                "   - Buy 3 or more cakes, get 1 free\n\n" +
                "2. Meal Set Discount\n" +
                "   - 2 food items (excluding cakes) + 1 drink, enjoy 20% off\n\n" +
                "3. Regular Discounts\n" +
                "   - $2 off for orders over $20\n" +
                "   - $5 off for orders over $30\n" +
                "   - 20% off for orders over $50\n\n" +
                "Note: Discounts cannot be combined. The system will automatically apply the best discount.";

        discountTextArea.setText(discountInfo);

        JScrollPane discountScroll = new JScrollPane(discountTextArea);
        discountScroll.setOpaque(false);
        discountScroll.getViewport().setOpaque(false);
        discountPanel.add(discountScroll, BorderLayout.CENTER);

        tabbedPane.addTab("Discount Information", discountPanel);
    }


    private void initSimulationTab() {
        JPanel simulationPanel = new JPanel(new BorderLayout());
        simulationPanel.setOpaque(false);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setOpaque(false);

        // Initialize buttons
        startSimulationButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        stopSimulationButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        stopSimulationButton.setEnabled(false);

        // Speed adjustment slider
        speedSlider.setMajorTickSpacing(1000);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        controlPanel.add(startSimulationButton);
        controlPanel.add(stopSimulationButton);
        controlPanel.add(new JLabel("Wait Time for Servers (Please adjust before starting simulation):"));
        controlPanel.add(speedSlider);

        // Status display area
        queueTextArea.setEditable(false);
        queueTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        serversTextArea.setEditable(false);
        serversTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        JPanel displayPanel = new JPanel(new GridLayout(1, 2));
        displayPanel.add(new JScrollPane(queueTextArea));
        displayPanel.add(new JScrollPane(serversTextArea));

        simulationPanel.add(controlPanel, BorderLayout.NORTH);
        simulationPanel.add(displayPanel, BorderLayout.CENTER);

        tabbedPane.addTab("Simulation Control", simulationPanel);
    }


    private void configureOrderButton() {
        placeOrderButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        placeOrderButton.setBackground(new Color(70, 130, 180));
        placeOrderButton.setForeground(Color.WHITE);
        placeOrderButton.setFocusPainted(false);
        placeOrderButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    }

    public void addProductCard(Product product, ActionListener addAction, ActionListener removeAction) {
        // Create product card panel
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

        // Product info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel nameLabel = new JLabel("<html><b><font size=+1>" + product.getName() + "</font></b></html>");
        nameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(5));

        // Product description
        JTextArea descriptionArea = new JTextArea(product.getDescription());
        descriptionArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setMaximumSize(new Dimension(350, 40));
        infoPanel.add(descriptionArea);
        infoPanel.add(Box.createVerticalStrut(5));

        // Product category and price
        JPanel detailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        detailPanel.setOpaque(false);

        JLabel categoryLabel = new JLabel("Category: " + product.getCategory());
        categoryLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        detailPanel.add(categoryLabel);

        JLabel priceLabel = new JLabel("Price: $" + product.getPrice());
        priceLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        priceLabel.setForeground(new Color(0, 100, 0));
        detailPanel.add(priceLabel);

        infoPanel.add(detailPanel);

        // Stock info
        JLabel stockLabel = new JLabel("Stock: " + product.getStock());
        stockLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        stockLabel.setForeground(new Color(100, 100, 100));
        infoPanel.add(stockLabel);
        stockLabels.put(product.getId(), stockLabel);

        // Quantity selection panel
        JPanel quantityPanel = new JPanel();
        quantityPanel.setOpaque(false);
        quantityPanel.setLayout(new BoxLayout(quantityPanel, BoxLayout.X_AXIS));
        quantityPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Minus button
        JButton minusButton = new JButton("-");
        minusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        minusButton.setPreferredSize(new Dimension(30, 30));
        minusButton.setBackground(new Color(255, 150, 150));
        minusButton.setForeground(Color.WHITE);
        minusButton.setFocusPainted(false);
        minusButton.addActionListener(e -> {
            int currentQty = Integer.parseInt(quantityLabels.get(product).getText());
            if (currentQty > 0) {
                quantityLabels.get(product).setText(String.valueOf(currentQty - 1));
                removeAction.actionPerformed(e); // Notify controller about quantity decrease
            } else {
                showMessage("Invalid Operation", "Purchase quantity cannot be less than 0", JOptionPane.WARNING_MESSAGE);
            }
        });


        // Quantity display label
        JLabel quantityLabel = new JLabel("0");
        quantityLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        quantityLabels.put(product, quantityLabel);

        // Plus button
        JButton plusButton = new JButton("+");
        plusButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        plusButton.setPreferredSize(new Dimension(30, 30));
        plusButton.setBackground(new Color(150, 200, 150));
        plusButton.setForeground(Color.WHITE);
        plusButton.setFocusPainted(false);
        plusButton.addActionListener(e -> {
            int currentQty = Integer.parseInt(quantityLabels.get(product).getText());
            if (currentQty < product.getStock()) {
                quantityLabels.get(product).setText(String.valueOf(currentQty + 1));
                addAction.actionPerformed(e); // Notify controller about quantity increase
            } else {
                showMessage("Insufficient Stock",
                        "Not enough stock!\n" + product.getName() +
                                " Remaining: " + product.getStock() +
                                "\nMaximum addable quantity: " + product.getStock(),
                        JOptionPane.WARNING_MESSAGE);
            }
        });


        quantityPanel.add(minusButton);
        quantityPanel.add(quantityLabel);
        quantityPanel.add(plusButton);
        infoPanel.add(quantityPanel);

        cardPanel.add(infoPanel, BorderLayout.CENTER);
        productPanel.add(cardPanel);
    }

    public void updatePriceDisplay(double totalPrice, double discountedPrice, String discountInfo) {
        SwingUtilities.invokeLater(() -> {
            totalPriceLabel.setText(String.format("$%.2f", totalPrice));
            discountedPriceLabel.setText(String.format("$%.2f", discountedPrice));
            discountInfoLabel.setText(discountInfo);
        });
    }

    public void updateProductStock(Product product) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = stockLabels.get(product.getId());
            if (label != null) {
                label.setText("Stock: " + product.getStock());
                if (product.getStock() < 3) {
                    label.setForeground(Color.RED);
                } else {
                    label.setForeground(new Color(100, 100, 100));
                }

                // Update quantity label
                JLabel qtyLabel = quantityLabels.get(product);
                if (qtyLabel != null) {
                    int currentQty = Integer.parseInt(qtyLabel.getText());
                    if (currentQty > product.getStock()) {
                        qtyLabel.setText(String.valueOf(product.getStock()));
                    }
                }
            }
        });
    }

    public void appendOrderNote(String note) {
        SwingUtilities.invokeLater(() -> {
            allOrderSummaries.insert(0, note + "\n");
            orderSummaryArea.setText(allOrderSummaries.toString());
            orderSummaryArea.setCaretPosition(0);
        });
    }

    public void resetProductSelections() {
        SwingUtilities.invokeLater(() -> {
            quantityLabels.values().forEach(label -> label.setText("0"));
        });
    }

    public void resetOrderSummariesSelections() {
        totalPriceLabel.setText("$0.00");
        discountedPriceLabel.setText("$0.00");
        discountInfoLabel.setText("No discount");
    }

    public void showMessage(String title, String message, int messageType) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, title, messageType)
        );
    }

    public void setOrderButtonListener(ActionListener listener) {
        placeOrderButton.addActionListener(listener);
    }

    public void setStartSimulationListener(ActionListener listener) {
        startSimulationButton.addActionListener(listener);
    }

    public void setStopSimulationListener(ActionListener listener) {
        stopSimulationButton.addActionListener(listener);
    }

    public int getSimulationSpeed() {
        return speedSlider.getValue();
    }

    public void setSimulationControlsEnabled(boolean running) {
        startSimulationButton.setEnabled(!running);
        stopSimulationButton.setEnabled(running);
        placeOrderButton.setEnabled(running);
    }

    @Override
    public void updateQueue(List<Order> orders) {
        SwingUtilities.invokeLater(() -> {
            // Group orders by order ID, merging the display of the same orders
            Map<String, List<Order>> groupedOrders = orders.stream()
                    .collect(Collectors.groupingBy(Order::getOrderId));

            StringBuilder queueText = new StringBuilder();
            queueText.append("=== Current Queue Status ===\n");
            queueText.append("There are ");
            queueText.append(orders.size());
            queueText.append(" orders waiting to be processed\n");

            // Reserved orders
            queueText.append("=== Reserved Orders ===\n");
            groupedOrders.values().stream()
                    .filter(list -> "PRE_ORDER".equals(list.get(0).getOrderType()))
                    .forEach(orderList -> appendMergedOrder(queueText, orderList));

            // Walk-in orders
            queueText.append("\n=== Walk-in Orders ===\n");
            groupedOrders.values().stream()
                    .filter(list -> "WALK_IN".equals(list.get(0).getOrderType()))
                    .forEach(orderList -> appendMergedOrder(queueText, orderList));

            queueTextArea.setText(queueText.toString());
        });
    }

    private void appendMergedOrder(StringBuilder sb, List<Order> orders) {
        if (orders.isEmpty()) return;

        Order firstOrder = orders.get(0);
        sb.append("Order ID: ").append(firstOrder.getOrderId()).append("\n");
        sb.append("Customer: ").append(firstOrder.getCustomerName()).append("\n");
        sb.append("Type: ").append(firstOrder.getOrderType()).append("\n");

        // Merge all products
        Map<Product, Long> allProducts = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        allProducts.forEach((product, count) -> {
            sb.append(String.format("  %-15s ×%-2d @ $%-5.2f\n",
                    product.getName(), count, product.getPrice()));
        });

        double totalPrice = orders.stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();

        sb.append(String.format("Total Price: $%.2f\n\n", totalPrice));
    }

    @Override
    public void updateServers(List<ServerThread> servers) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Server Status:\n\n");

            for (ServerThread server : servers) {
                sb.append(server.getName()).append(": ");
                Order currentOrder = server.getCurrentOrder();
                if (currentOrder != null) {
                    sb.append(server.getCurrentOrderDetails());
                } else {
                    sb.append("Idle\n");
                }
            }

            serversTextArea.setText(sb.toString());
        });
    }

}
