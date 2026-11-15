-- Apply once to an existing production database before deploying this version.
ALTER TABLE shop
    ADD COLUMN rejection_reason VARCHAR(500) NULL;

CREATE INDEX idx_shop_approval_active ON shop (approval_status, active);
CREATE INDEX idx_product_shop_name ON product (shop_id, name);
CREATE INDEX idx_orders_customer_created ON customer_orders (customer_id, created_at);
CREATE INDEX idx_orders_shop_created ON customer_orders (shop_id, created_at);
CREATE INDEX idx_orders_status_created ON customer_orders (status, created_at);
