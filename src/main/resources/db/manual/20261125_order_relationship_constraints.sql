-- Apply once to an existing MySQL database if checkout returns
-- "A record conflicts with an existing value". Older schemas may still have
-- single-column unique indexes from earlier one-to-one order relationships.
-- Current mappings allow customers to place many orders and products to appear
-- in many order items. This removes only those obsolete single-column indexes;
-- it does not delete or change any rows.

SET @customer_order_unique_index = (
    SELECT s.INDEX_NAME
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'customer_orders'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
    GROUP BY s.INDEX_NAME
    HAVING COUNT(*) = 1 AND MAX(s.COLUMN_NAME) = 'customer_id'
    LIMIT 1
);
SET @drop_customer_order_unique_sql = IF(
    @customer_order_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `customer_orders` DROP INDEX `', @customer_order_unique_index, '`')
);
PREPARE drop_customer_order_unique_stmt FROM @drop_customer_order_unique_sql;
EXECUTE drop_customer_order_unique_stmt;
DEALLOCATE PREPARE drop_customer_order_unique_stmt;

SET @order_item_product_unique_index = (
    SELECT s.INDEX_NAME
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'order_items'
      AND s.NON_UNIQUE = 0
      AND s.INDEX_NAME <> 'PRIMARY'
    GROUP BY s.INDEX_NAME
    HAVING COUNT(*) = 1 AND MAX(s.COLUMN_NAME) = 'product_id'
    LIMIT 1
);
SET @drop_order_item_product_unique_sql = IF(
    @order_item_product_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `order_items` DROP INDEX `', @order_item_product_unique_index, '`')
);
PREPARE drop_order_item_product_unique_stmt FROM @drop_order_item_product_unique_sql;
EXECUTE drop_order_item_product_unique_stmt;
DEALLOCATE PREPARE drop_order_item_product_unique_stmt;
