-- Apply once to an existing database before deploying the full audit batch.
-- Back up the database first. This script intentionally does not use IF NOT EXISTS;
-- MySQL versions differ in support for idempotent index/column DDL.

ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0;

ALTER TABLE product
    ADD COLUMN pack_size_value DECIMAL(12,3) NULL,
    ADD COLUMN pack_size_unit VARCHAR(20) NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE product_stock_adjustments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    reason VARCHAR(40) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_stock_adjustment_product_created (product_id, created_at),
    CONSTRAINT fk_stock_adjustment_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_stock_adjustment_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT chk_stock_adjustment_quantities CHECK (previous_quantity >= 0 AND new_quantity >= 0)
);

ALTER TABLE product
    ADD CONSTRAINT chk_product_stock_nonnegative CHECK (stock_quantity >= 0),
    ADD CONSTRAINT chk_product_price_positive CHECK (price > 0);
