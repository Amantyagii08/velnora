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

## Existing production database

Default local development uses Hibernate schema update. Production uses schema validation, so apply `src/main/resources/db/manual/20261115_add_shop_rejection_reason.sql` once to an existing MySQL database before deploying this version. The script adds the rejection reason and the recommended lookup indexes. It is a one-time manual migration and should not be rerun unchanged.

## Verification

The clearance date-time binding test runs without a database. Checkout concurrency and the full application context still need verification against a running test database; do not use production data for those checks.
