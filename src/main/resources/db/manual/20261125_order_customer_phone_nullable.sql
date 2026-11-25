-- Align an existing MySQL database with the current Order mapping.
-- A customer may use an address phone for delivery or choose pickup without
-- having a phone number saved on their account, so this field is optional.
ALTER TABLE customer_orders
    MODIFY COLUMN customer_phone VARCHAR(20) NULL;

-- Pickup orders do not have a delivery address.
ALTER TABLE customer_orders
    MODIFY COLUMN delivery_address VARCHAR(255) NULL;
