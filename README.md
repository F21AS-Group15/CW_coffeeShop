# Coffee Shop Order Management System

This is a simple coffee shop order management system implemented in Java. It includes a graphical user interface (GUI) built using Swing, and it supports product management, order placement, discount application, and sales reporting.

## Features

1. **Product Management**:
   - Load products from a 'menu.txt' file.
   - Add or remove products dynamically.

2. **Order Management**:
   - Place orders with multiple products.
   - Calculate the total price of an order.
   - Apply discounts based on predefined rules (e.g.,Spend 20, get 2 off; spend 30, get 5 off, spend 50, get 20% off).

3. **Discount Management**:
   - Support for default and custom discount rules.
   - Apply discounts to orders dynamically.

4. **Sales Reporting**:
   - Generate a sales report showing the number of times each product was ordered and the total revenue.

5. **Exception Handling**:
   - Custom exception 'OutOfStockException' for handling insufficient stock scenarios.

## File Structure

- 'menu.txt': Contains product details in the format 'id,name,description,category,price'.
- 'orders.txt': Contains order details in the format 'orderId,timestamp,customerId,productId1,productId2,...'.
- 'CoffeeShop.java': Main class for the application, including GUI setup and business logic.
- 'CoffeeShopTest.java': JUnit test cases for discount application, order calculation, and exception handling.

## How to Run

1. Ensure you have Java installed on your system.
2. Compile the Java files:
   ```bash
   javac com/itjava/*.java
