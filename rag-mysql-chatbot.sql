CREATE SCHEMA rag-data-schema;

CREATE TABLE products (
        product_id INT PRIMARY KEY AUTO_INCREMENT,
        product_name VARCHAR(255) NOT NULL,
        description TEXT,
        price DECIMAL(10, 2) NOT NULL,
        stock_quantity INT NOT NULL DEFAULT 0
    );

 CREATE TABLE orders (
        order_id INT PRIMARY KEY AUTO_INCREMENT,customer_name VARCHAR(255) NOT NULL,order_date DATETIME DEFAULT CURRENT_TIMESTAMP,total_amount DECIMAL(10, 2) NOT NULL
    );


CREATE TABLE order_items (order_item_id INT PRIMARY KEY AUTO_INCREMENT,order_id INT NOT NULL,product_id INT NOT NULL,quantity INT NOT NULL,price_at_purchase DECIMAL(10, 2) NOT NULL, FOREIGN KEY (order_id) REFERENCES orders(order_id),FOREIGN KEY (product_id) REFERENCES products(product_id)
    );

 INSERT INTO products (product_name, description, price, stock_quantity) VALUES
    ('Laptop Pro', 'High-performance laptop', 1200.00, 50),
    ('Wireless Mouse', 'Ergonomic wireless mouse', 25.99, 150),
    ('Mechanical Keyboard', 'RGB mechanical keyboard', 89.99, 75),
    ('Monitor 27-inch', 'Full HD 27-inch monitor', 199.99, 30);

INSERT INTO orders (customer_name, total_amount) VALUES
    ('Alice Smith', 1225.99),
    ('Bob Johnson', 179.98);

 INSERT INTO order_items (order_id, product_id, quantity, price_at_purchase) VALUES
    (1, 1, 1, 1200.00),
    (1, 2, 1, 25.99),
    (2, 2, 2, 25.99),
    (2, 3, 1, 89.99);