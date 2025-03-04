package com.itjava;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CoffeeShopTest {
    // ---------------------------
    // DefaultDiscount test
    // ---------------------------

    // When the original price is less than 20, no discount
    @Test
    public void testDefaultDiscount_NoDiscount() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order1", "timestamp", "customer1");
        double originalPrice = 19.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(19.0, discountedPrice, 0.001);
    }

    // When the original price is between 20 and 29, subtract 2 yuan
    @Test
    public void testDefaultDiscount_Above20() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order2", "timestamp", "customer1");
        double originalPrice = 22.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(20.0, discountedPrice, 0.001);
    }

    // When the original price is between 30 and 49, deduct 5 yuan
    @Test
    public void testDefaultDiscount_Above30() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order3", "timestamp", "customer1");
        double originalPrice = 35.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(30.0, discountedPrice, 0.001);
    }

    // When the original price is greater than or equal to 50, it's 20% off
    @Test
    public void testDefaultDiscount_Above50() {
        DefaultDiscount discount = new DefaultDiscount();
        Order order = new Order("order4", "timestamp", "customer1");
        double originalPrice = 100.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(80.0, discountedPrice, 0.001);
    }

    // ---------------------------
    // FoodAndBeverageDiscount test
    // ---------------------------
    @Test
    public void testFoodAndBeverageDiscount_NoDiscount() {
        FoodAndBeverageDiscount discount = new FoodAndBeverageDiscount();
        Order order = new Order("order5", "timestamp", "customer1");
        Product food = new Product("p1", "Burger", "Delicious", "Food", 8.0, 10);
        order.addItem(food, 2);
        double originalPrice = 16;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(16, discountedPrice, 0.001);

        Product beverage = new Product("p2", "Coke", "Refreshing", "Beverage", 3.0, 10);
        order.addItem(beverage, 1);
        originalPrice = 19.0;
        discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(19*0.8, discountedPrice, 0.001);
    }
    // ---------------------------
    // FreeCakeDiscount test
    // ---------------------------
    @Test
    public void testFreeCakeDiscount() {
        FreeCakeDiscount discount = new FreeCakeDiscount();
        Order order = new Order("order6", "timestamp", "customer1");
        // Add 2 Cake items
        Product cake = new Product("p3", "Cake", "Sweet", "Food", 4.0, 10);
        order.addItem(cake, 2);

        double originalPrice = 8.0;
        double discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(originalPrice, discountedPrice, 0.001);

        order.addItem(cake, 1);
        originalPrice = 12.0;
        discountedPrice = discount.applyDiscount(order, originalPrice);
        assertEquals(8.0, discountedPrice, 0.001);
    }



    // ---------------------------
    // DiscountManager test
    // ---------------------------

    // If the discount name passed in is null or an empty string or an invalid discount name, the default" default" rule should be used
    @Test
    public void testDiscountManager_DefaultIfNullOrEmpty() {
        DiscountManager manager = new DiscountManager();
        Order order = new Order("order7", "timestamp", "customer1");
        double originalPrice = 50.0;

        // The discount name is null
        double discountedPrice = manager.applyDiscount(null, order, originalPrice);
        assertEquals(40.0, discountedPrice, 0.001);

        // Discount name is empty string
        discountedPrice = manager.applyDiscount("", order, originalPrice);
        assertEquals(40.0, discountedPrice, 0.001);

        // Discount name is invalid discount name
        discountedPrice = manager.applyDiscount("nonexistent", order, originalPrice);
        assertEquals(40.0, discountedPrice, 0.001);
    }

    // ---------------------------
    // Order total price calculation test
    // ---------------------------

    @Test
    public void testOrderTotalPriceCalculation() {
        Order order = new Order("order8", "timestamp", "customer1");
        Product product1 = new Product("p4", "Sandwich", "Tasty", "Food", 5.0, 10);
        Product product2 = new Product("p5", "Juice", "Fresh", "Beverage", 3.5, 10);
        // Add 2 sandwiches for a total price of 10.0
        order.addItem(product1, 2);
        // Add 3 juice, the total price is 10.5
        order.addItem(product2, 3);
        // When setTotalPrice is not called, getTotalPrice is calculated based on the price of the item
        assertEquals(20.5, order.getTotalPrice(), 0.001);

        // After calling setTotalPrice, the value of set is returned
        order.setTotalPrice(18.0);
        assertEquals(18.0, order.getTotalPrice(), 0.001);
    }

    @Test
    void testCakeAndFoodBeverageCombination() {
        DiscountManager discountManager = new DiscountManager();
        Order order = new Order("order9", "timestamp", "customer1");
        Product product1 = new Product("p6", "Cake", "Sweet", "Food", 4.0, 10);
        Product product2 = new Product("p7", "Coffee", "Arabica", "Beverage", 8.0, 10);
        // Added items: 3 cakes + 1 drinks
        order.addItem(product1, 3);   // 4*3 = 12
        order.addItem(product2, 1); // 8*1 = 8
        double originalPrice = 20.0;

        // Applied discount strategy
        double finalPrice = discountManager.applyDiscount("free_cake", order, originalPrice);
        finalPrice = discountManager.applyDiscount("food_and_beverage", order, finalPrice);
        assertEquals(16*0.8, finalPrice, 0.001);
    }


    // Test discount application priority
    @Test
    void testDiscountPriority() {
        DiscountManager discountManager = new DiscountManager();
        Order order = new Order("order10", "timestamp", "customer1");
        Product product1 = new Product("c1", "Cake", "Sweet", "Food", 4.0, 10);
        Product product2 = new Product("f1", "Burger", "Juicy", "Food", 15.0, 10);
        Product product3 = new Product("b1", "Coffee", "Arabica", "Beverage", 8.0, 10);
        // Add items: 3 cakes + 2 food + 1 drinks + other items
        order.addItem(product1, 3);      // 4*3 = 12
        order.addItem(product2, 2);      // 15*2 = 30
        order.addItem(product3, 1); // 8*1 = 8
        Product other = new Product("o1", "Notebook", "Moleskine", "Stationery", 30.0, 10);
        order.addItem(other, 1);     // 30*1 = 30
        double originalPrice = 80.0;

        // Simulates placeOrder method logic
        double tempPrice = originalPrice;

        // 1. Check for cake discounts
        if (countCakes(order) >= 3) {
            tempPrice = discountManager.applyDiscount("free_cake", order, tempPrice);
        }

        // 2. Check food and drink combinations
        if (hasFoodBeverageCombo(order)) {
            tempPrice = discountManager.applyDiscount("food_and_beverage", order, tempPrice);
        } else {
            tempPrice = discountManager.applyDiscount("default", order, tempPrice);
        }
        assertEquals(60.8, tempPrice, 0.001);
    }

    private int countCakes(Order order) {
        return (int) order.getItems().stream().filter(p -> "Cake".equals(p.getName())).count();
    }

    private boolean hasFoodBeverageCombo(Order order) {
        int foodCount = 0;
        int beverageCount = 0;
        for (Product p : order.getItems()) {
            if ("Food".equals(p.getCategory())) {
                foodCount++;
            }
            if ("Beverage".equals(p.getCategory())) {
                beverageCount++;
            }
        }
        return foodCount >= 2 && beverageCount >= 1;
    }

    @Test
    void testOutOfStockExceptionThrown() {
        Order order = new Order("order11", "timestamp", "customer1");
        Product product1 = new Product("p1", "Cake", "Sweet", "Food", 4.0, 2);
        Map<Product, Integer> selectedProducts = new HashMap<>();
        // First increase the quantity of goods to the stock limit
        selectedProducts.put(product1, 2);

        // Simulate the logic of clicking the button
        Exception exception = assertThrows(OutOfStockException.class, () -> {
            int quantity = selectedProducts.getOrDefault(product1, 0);
            if (quantity >= product1.getStock()) {
                throw new OutOfStockException("Out of stock!");
            }
            quantity++;
            selectedProducts.put(product1, quantity);
        });

        // Verify that the exception information is correct
        assertEquals("Out of stock!", exception.getMessage());

        // Make sure the quantity is not increased
        assertEquals(2, selectedProducts.get(product1));
    }

}
