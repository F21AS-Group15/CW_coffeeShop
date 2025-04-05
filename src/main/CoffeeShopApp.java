package main;

import util.CoffeeShopLogger;
import controller.MainController;
import javax.swing.*;

// Main
public class CoffeeShopApp {
    public static void main(String[] args) {
        // Create GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize controller (automatically creates view and model)
                new MainController();

                // Initialize logging
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        CoffeeShopLogger.getInstance().saveToFile("src//coffee_shop_log.txt");
                    } catch (Exception e) {
                        System.err.println("Failed to save log: " + e.getMessage());
                    }
                }));

            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "System initialization failed: " + e.getMessage(),
                        "Fatal Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1); // Exit on exception
            }
        });
    }
}
