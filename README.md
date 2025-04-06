
## Coffee Shop Order Management System

### Overview

This system simulates a coffee shop's order management process, supporting both online reservation orders and on-site customer purchases. Prior to initiating the simulation, the handling time (in milliseconds) required by each waiter for different product types can be configured. By default, this time is set to 2000ms and must be adjusted before the simulation begins.

### Simulation Control

To start the simulation, navigate to the **Simulation Control** page and click the **"Start Simulation"** button. Only after the simulation has started can customers place orders. Upon initiation, two waiters will begin processing online reservation orders.

### Product Selection and Ordering

On-site customers can browse the **Product Menu** page and add desired items to their order by clicking the **"+"** button next to each item. The system will display real-time updates at the top of the page, including:

- Total price of selected items  
- Discounted price  
- Applicable discount details  

> Note: Only on-site orders are eligible for discounts, as online reservation orders are exempt from service fees and thus not discountable.

If a customer attempts to order a quantity exceeding the available stock, a **"Not enough stock!"** dialog will appear, indicating the actual stock and the maximum allowable quantity. Clicking the **"–"** button allows removal of items. If the quantity falls below zero, a **"Purchase quantity cannot be less than 0"** message will be shown.

Once the customer finalizes their selection, clicking the **"Confirm Order"** button will submit the order.

### Discount Information

All discount policies and usage rules are detailed on the **Discount Information** page, which customers may consult at any time.

### Live Order Monitoring

Detailed information of all on-site orders—including order ID, product names, quantities, and order type—is displayed in the **Live Orders** view. Meanwhile, the left panel of the **Simulation Control** page offers real-time monitoring of:

- The number of customers currently in the queue  
- All pending orders (including both on-site and online orders)  

The right panel displays:

- The status of each waiter  
- The specific order currently being handled by each waiter  

Waiters will always process online orders before handling new on-site orders. On-site orders are processed strictly in the order they are placed.

### Simulation Termination and Logging

The simulation may be terminated at any time by clicking the **"Stop Simulation"** button, or it will automatically exit once all orders are completed. All operational details and order-handling steps are logged in a designated log file. The log also includes a final summary with:

- Total sales amount for the day  
- Total number of orders processed  


