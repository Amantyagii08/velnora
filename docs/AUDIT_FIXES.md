# Audit Fixes and API Notes

## Updated behavior

- Product checkout locks products in ascending ID order and refreshes each locked row before checking or reducing stock.
- Shopkeepers can manage products, stock, availability, and discounts only while their shop is approved and active. Shopkeepers can view orders only for their approved active shop.
- Material shop profile changes return an approved shop to pending review. Contact or image-only changes keep its approval. Rejected shops must be edited or resubmitted before review.
- Admin shop rejection requires a reason (up to 500 characters). Approval and rejection are available only for pending applications. `GET /api/admin/categories` includes inactive categories.
- `POST /api/cart/items` sets the selected quantity (it is an upsert). `PATCH /api/cart/items/{itemId}` also sets the quantity. Cart entries now report `available` so clients can identify products that became unavailable after being added.
- Shopkeepers can read one of their orders at `GET /api/orders/my-shop-orders/{orderId}`. Their clearance endpoints now support both POST and PUT, and `GET /api/shops/{shopId}/clearance-deals` lists their active deals.
- The public homepage now uses bounded product/shop lists and database aggregates for popularity rankings. Public product detail and clearance results hide inactive products and shops; clearance deals require stock.
- Invalid or expired JWT responses have a JSON message, the JWT issuer is checked, and public registration has a per-address attempt limit.
- Uploaded images created in a transaction are removed if that transaction rolls back. Open EntityManager in View is disabled.
- Product and shop search, filters, comparisons, clearance, category lists, admin lists/search, and order history now use bounded database pagination. Admin lists and homepage/catalog responses use response DTOs. Search strings are length-limited and SQL LIKE wildcards are escaped.
- Product comparison now matches brand, name, variant, and an explicit pack size. Stock corrections use a product lock and are recorded in stock history. Product optimistic versioning, nonnegative stock checks, token versioning, logout, and password change are included.
- The admin dashboard uses database count queries. Upload fallback storage uses a relative data directory, and the health endpoint identifies Velnora.

## Existing production database

Default local development uses Hibernate schema update. Production uses schema validation, so apply the one-time scripts in `src/main/resources/db/manual` to an existing MySQL database before deploying this version. Apply `20261115_add_shop_rejection_reason.sql` and `20261115_stock_security_and_comparison.sql` only if their changes are not already present. Back up first; the scripts are not idempotent. The project does not yet include Flyway/Liquibase migration automation, so schema changes still require a manual, recorded deployment step.

## Verification

Run the Maven test suite before deployment. This workspace's Maven cache was unavailable during this audit session, so the new source changes could not be compiled or exercised here. Checkout concurrency and the full application context also need verification against a running test database; do not use production data for those checks.
