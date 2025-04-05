package model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DiscountCalculator {
    private static final double CAKE_PRICE = 4.0; // Cake unit price

    public static class DiscountResult {
        public static final DiscountResult NO_DISCOUNT =
                new DiscountResult(false, "No available discount", 0.0);

        public final boolean applied;
        public final String description;
        public final double discountAmount;

        public DiscountResult(boolean applied, String description, double discountAmount) {
            this.applied = applied;
            this.description = description;
            this.discountAmount = discountAmount;
        }
    }

    // Calculate all possible discounts and return the one with the highest discount
    public DiscountResult calculateBestDiscount(Order originalOrder) {
        List<DiscountResult> allDiscounts = new ArrayList<>();

        // 1. Calculate cake discount
        allDiscounts.add(applyCakeDiscount(originalOrder));

        // 2. Calculate meal deal discount (excluding cake items)
        Order orderWithoutCakes = createOrderWithoutCakes(originalOrder);
        allDiscounts.add(applyMealDealDiscount(orderWithoutCakes));

        // 3. Calculate default discount (excluding cake items)
        allDiscounts.add(applyDefaultDiscount(orderWithoutCakes));

        // 4. Choose the discount with the highest discount amount
        return allDiscounts.stream()
                .max(Comparator.comparingDouble(d -> d.discountAmount))
                .orElse(DiscountResult.NO_DISCOUNT);
    }

    // Create a copy of the order without cakes
    private Order createOrderWithoutCakes(Order originalOrder) {
        Order orderWithoutCakes = new Order(
                originalOrder.getOrderId() + "-nocakes",
                originalOrder.getTimeStamp(),
                originalOrder.getCustomerName(),
                originalOrder.getOrderType()
        );

        for (Product product : originalOrder.getItems()) {
            if (!"Cake".equals(product.getName())) {
                orderWithoutCakes.addItem(product);
            }
        }
        return orderWithoutCakes;
    }

    // Apply cake discount rule: Buy 3, get 1 free
    private DiscountResult applyCakeDiscount(Order order) {
        int cakeCount = countCakes(order);
        if (cakeCount >= 3) {
            int freeCakes = cakeCount / 3;
            double discountAmount = freeCakes * CAKE_PRICE;
            return new DiscountResult(
                    true,
                    "Buy 3 Cakes, Get 1 Free (Save $" + discountAmount + ")",
                    discountAmount
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // Apply meal deal discount rule: 2 food items (excluding cakes) + 1 beverage at 20% off
    private DiscountResult applyMealDealDiscount(Order order) {
        int foodCount = countItemsByCategory(order, "Food");
        int beverageCount = countItemsByCategory(order, "Beverage");

        if (foodCount >= 2 && beverageCount >= 1) {
            double originalPrice = order.getTotalPrice();
            double discountedPrice = originalPrice * 0.8;
            return new DiscountResult(
                    true,
                    "Meal Deal Discount (20% off)",
                    originalPrice - discountedPrice
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // Apply default discount rules:
    // - 20% off for orders over 50
    // - $5 off for orders over 30
    // - $2 off for orders over 20
    private DiscountResult applyDefaultDiscount(Order order) {
        double originalPrice = order.getTotalPrice();

        if (originalPrice >= 50) {
            double discountedPrice = originalPrice * 0.8;
            return new DiscountResult(
                    true,
                    "20% off for orders over $50",
                    originalPrice - discountedPrice
            );
        } else if (originalPrice >= 30) {
            return new DiscountResult(
                    true,
                    "$5 off for orders over $30",
                    5.0
            );
        } else if (originalPrice >= 20) {
            return new DiscountResult(
                    true,
                    "$2 off for orders over $20",
                    2.0
            );
        }
        return DiscountResult.NO_DISCOUNT;
    }

    // Count the number of cakes in the order
    private int countCakes(Order order) {
        return (int) order.getItems().stream()
                .filter(p -> "Cake".equals(p.getName()))
                .count();
    }

    // Count the number of items by category
    private int countItemsByCategory(Order order, String category) {
        return (int) order.getItems().stream()
                .filter(p -> category.equals(p.getCategory()))
                .count();
    }
}
