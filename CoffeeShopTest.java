package com.itjava;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;


class CoffeeShopTest {
    private DiscountManager discountManager;
    private Order order;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        discountManager = new DiscountManager();
        order = new Order(UUID.randomUUID().toString(), new Date().toString(), "customer1");
        product1 = new Product("1", "Espresso", "Strong coffee", "Beverage", 5.0);
        product2 = new Product("2", "Latte", "Milk coffee", "Beverage", 6.0);
    }

    @Test
    void testApplyDiscount_NoDiscount() {
        order.addItem(product1, 1);
        order.addItem(product2, 1);
        double total = discountManager.applyDiscount(null, order);
        assertEquals(11.0, total, 0.01);
    }

    @Test
    void testApplyDiscount_Above20() {
        order.addItem(product1, 3);
        order.addItem(product2, 1);
        double total = discountManager.applyDiscount("default", order);
        assertEquals(19.0, total, 0.01); // 21减2
    }

    @Test
    void testApplyDiscount_Above30() {
        order.addItem(product1, 4);
        order.addItem(product2, 2);
        double total = discountManager.applyDiscount("default", order);
        assertEquals(27.0, total, 0.01); // 32减5
    }

    @Test
    void testApplyDiscount_Above50() {
        order.addItem(product1, 6);
        order.addItem(product2, 4);
        double total = discountManager.applyDiscount("default", order);
        assertEquals(43.2, total, 0.01); // 50*0.8
    }

    @Test
    void testBoundaryValues() {
        order.setTotalPrice(50.0);
        assertEquals(40.0, discountManager.applyDiscount(null, order), 0.001,
                "50 boundary values should be discounted by 20%");

        order.setTotalPrice(30.0);
        assertEquals(25.0, discountManager.applyDiscount(null, order), 0.001,
                "The boundary value of 30 should be reduced by 5");

        order.setTotalPrice(20.0);
        assertEquals(18.0, discountManager.applyDiscount(null, order), 0.001,
                "The boundary value of 20 should be reduced by 2");
    }

    @Test
    void testOrderTotalPriceCalculation() {
        order.addItem(product1, 2);
        order.addItem(product2, 3);
        assertEquals(28.0, order.getTotalPrice(), 0.01);
    }

    @Test
    void testCustomDiscount() {
        discountManager.addDiscount(new Discount("half-price") {
            public double applyDiscount(Order order) {
                return order.getTotalPrice() * 0.5;
            }
        });
        order.addItem(product1, 2); // 10
        double total = discountManager.applyDiscount("half-price", order);
        assertEquals(5.0, total, 0.01);
    }

    @Test
    void testInvalidDiscount() {
        order.addItem(product1, 4);
        double total = discountManager.applyDiscount("non-existent", order);
        assertEquals(18.0, total, 0.01); // 应回退到默认折扣
    }

    @Test
    void should_increment_order_count_correctly() {
        // 测试商品点单次数统计
        int initialCount1 = product1.getOrderCount();
        int initialCount2 = product1.getOrderCount();


        order.addItem(product1, 3);
        order.addItem(product2, 2);
        discountManager.applyDiscount(null, order);  // 触发订单完成

        assertEquals(initialCount1 + 3, product1.getOrderCount(),
                "Product 1 should have 3 additional orders placed");
        assertEquals(initialCount2 + 2, product2.getOrderCount(),
                "Product 2 should have 2 additional orders placed");
    }

    // 测试异常处理
    @Test
    public void should_throw_out_of_stock_exception() {
        // 商品库存不足，抛出异常
        assertThrows(OutOfStockException.class, () -> {
            order.addItem(product1, 101);  // 试图购买101件商品，库存只有100
        }, "Should throw OutOfStockException when attempting to buy more than available stock.");
    }

}

