# Velnora Task 10: Architecture Decisions and API Compatibility Plan

**Prepared:** 19 November 2026  
**Scope:** Planning and documentation only. This report was verified against the current frontend and Spring Boot source. No application code, configuration, SQL, or database data was changed.

## 1. Executive summary

The agreed **one shop per cart and checkout** decision matches the backend model: backend cart service rejects a different shop unless the request explicitly confirms clearing the existing cart, and each order has one required shop foreign key. The current frontend cart and order summary still allow and present multiple shops in one cart/order, so the frontend must be aligned before API checkout integration.

Backend delivery and pickup support already exists under enum values `DELIVERY` and `PICKUP`. The delivery address is required by service validation for delivery and is stored as null for pickup. The frontend currently only supports delivery addresses. The requested wire values `SHOP_DELIVERY` and `CUSTOMER_PICKUP` do not match the backend enum; an adapter can map them to the existing values without changing backend enums. Shop address is already exposed on `ShopResponse`, but the order response does not include a pickup location snapshot.

The backend has JWT authentication, BCrypt password hashing, role checks, shop ownership checks, stock locking/validation, and role-scoped order reads. The frontend is still mock-only: `apiClient` has no token attachment or API calls, local mock authentication persists password values in browser storage, and its identifiers/statuses/data fields differ from backend contracts.

**Integration status: Blocked until frontend cart behavior and checkout/fulfillment contracts are aligned.** No live API, database schema, or browser runtime testing was performed.

## 2. Confirmed architecture decisions

These are business decisions supplied for this task and treated as authoritative:

1. A cart contains products from exactly one shop. A customer wishing to shop elsewhere must place the current shop order separately or explicitly replace the cart.
2. Every checkout and order belongs to exactly one shop. There is no parent multi-shop order.
3. Fulfillment modes are shop delivery and customer pickup. Proposed UI/domain labels are `SHOP_DELIVERY` and `CUSTOMER_PICKUP`.
4. Shop delivery requires a customer delivery address. Customer pickup does not require one and uses the selected shop's existing address.
5. No delivery fee, pickup schedule, payment behavior, or extra business policy is inferred here.
6. Product belongs to shop; product has no category relation. Shop belongs to one category.

### Recommended experience for adding from a different shop

Do not silently erase the current cart. When a customer adds an item from a different shop, show a clear confirmation naming both shops and the consequence: **“Your bag has items from [current shop]. Clear it and start a bag for [new shop]?”** Offer **Keep current bag** and **Clear bag and add item**. Preserve the current cart if they cancel. This is a recommendation only; no UI behavior was changed. It matches the backend's `clearExistingCart` confirmation flag and `CART_SHOP_MISMATCH` response.

## 3. Verified Task 09 findings

| Finding | Result | Evidence and implications |
|---|---|---|
| Frontend cart vs single-shop restriction | **Verified mismatch** | `frontend/src/context/CartContext.jsx` adds by product/variant key without checking `shopId`. `OrderSummaryPage.jsx` explicitly summarizes items “across” shops. Backend `CartService.add` detects different shop IDs and requires `clearExistingCart`; checkout also rechecks that all lines share a shop. |
| Order structure | **Verified mismatch** | Frontend writes one local order with lines that may have several `shopId`s. Backend `Order.shop` is a required many-to-one, and `OrderService.createOrder` rejects any product outside the selected shop. |
| Order statuses | **Verified mismatch** | Frontend order state uses `Processing`, `Shipped`, `Delivered`, `Cancelled`. Backend enum is `PLACED`, `ACCEPTED`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED`; service enforces a sequential progression. No `SHIPPED` backend status exists. |
| Delivery address | **Partially compatible** | Frontend keeps structured address records in localStorage and requires one. Backend stores one nullable text `deliveryAddress` string plus customer phone/name snapshots. Backend requires nonblank address for `DELIVERY` and forces null for `PICKUP`; there is no address-book endpoint/entity. |
| Delivery methods | **Verified backend support; frontend gap** | Backend `FulfillmentType` is `PICKUP`, `DELIVERY`; frontend checkout has no fulfillment selection and always asks for an address. Proposed UI enum names need mapping. |
| Pickup location | **Partially verified** | Shop's required `address` is exposed by `ShopResponse`; order response includes `shopId`/`shopName`, fulfillment type and delivery address but not shop address. Frontend can resolve current location by shop, but historic location is not snapshotted in order response. |
| Payment | **Not found** | No payment controller/entity/provider/status contract was found. UI explicitly says no payment is collected; backend order has totals but no payment fields. |
| IDs and DTO fields | **Verified mismatch** | Frontend IDs are mock slugs/strings and order references use `VLN-...`; backend entity IDs are `Long`. Product, shop, category, order and response DTO shapes also differ as listed below. |
| Product/shop/category relationships | **Verified match in domain** | Backend `Product` belongs to `Shop`; `Shop.category` is required; no product-category relation found. Frontend fixture uses product `shopId` and shop `categoryId`, also with no product category field. |
| Inventory | **Verified backend protections** | Seller stock endpoints validate inputs; `SetStockRequest.quantity` has `@Min(0)`. `ProductService` checks owner/shop/product association. Checkout locks and refreshes product rows and rejects unavailable/insufficient stock before decrement. Manual SQL includes nonnegative stock check. |
| Authentication and roles | **Verified backend protections; frontend not integrated** | Backend issues JWT, hashes passwords with BCrypt, rejects admin public registration, checks roles, and resolves role/token validity per authenticated request. Frontend mock routes are client-only and `apiClient` has no Authorization interceptor. |
| Admin/shopkeeper/customer permissions | **Partially verified** | Admin controller is class-protected by `hasRole('ADMIN')`. Customer cart/checkout and customer order endpoints are role-scoped. Shopkeeper product/stock/shop endpoints are role-scoped and services check ownership. `PATCH /api/orders/{id}/status` has a broad role annotation, but service checks actor ownership and legal transitions. Runtime 401/403 behavior remains untested. |

## 4. Frontend-to-backend API compatibility map

Endpoint paths below are the actual controller mappings. All examples omit deployment host/context path. Request/response descriptions are the source DTO or controller parameters, not proposed API fields. “Wire” means the UI/API layer can call the endpoint; it does not imply the current mock screen is compatible.

| Module | Frontend files/components | Backend controller/service | Exact endpoint and method | Actual request | Actual response | Auth/role | Compatibility and work needed |
|---|---|---|---|---|---|---|---|
| Authentication/login | `src/pages/auth/AuthPage.jsx`, `src/context/AuthContext.jsx`, `src/data/mockAccounts.js`, `src/services/apiClient.js` | `AuthController`, `AuthService`, `JwtService` | `POST /api/auth/login` | `LoginRequest`: `email`, `password` | `AuthResponse`: `accessToken`, `tokenType`, `expiresIn`, `userId`, `fullName`, `email`, `role` | Public | **Incompatible today.** Replace mock login/session with request/response adapter; normalize role casing and map identity fields. |
| Registration | Auth page/context/mock account data | `AuthController`, `AuthService` | `POST /api/auth/register` | `RegisterRequest`: `fullName`, `email`, `phone`, `password`, `role` | Same `AuthResponse` | Public; backend permits CUSTOMER or SHOPKEEPER only; ADMIN request rejected | **Partially compatible.** Frontend registration fields/role flow must map to DTO. Never send or persist admin role from public registration. |
| Customer profile | No live profile API use found; mock `AuthContext` | `ProfileController`, `ProfileService` | `GET /api/profile`; `PUT /api/profile` | Update `UpdateProfileRequest`: `fullName`, `phone` | Map with `id`, `fullName`, `email`, `phone`, `role` | JWT; admin profile explicitly unavailable by controller | **Not wired.** Add authenticated service and map `name` to `fullName`. |
| Categories | `src/data/marketplace.js`, `MarketplaceDataContext.jsx`, `App.jsx`, admin dashboard | `CategoryController`, `CategoryService` | `GET /api/categories?page=&size=` | Query `page` (default 0), `size` (default 20) | Spring `Page<CategoryResponse>`; each category has `id`, `name`, `imagePath` | Public GET | **Partial.** Frontend category IDs are slugs and fixture adds `note`; map numeric ID and image path. |
| Shops/public discovery | `src/data/marketplace.js`, `MarketplaceDataContext.jsx`, `App.jsx`, `ShopDetailView.jsx`, `Navbar.jsx` | `ShopController`, `ShopService`, `HomepageController`, `HomepageService` | `GET /api/shops?page=&size=`, `GET /api/shops/{shopId}`, `GET /api/shops/search?keyword=&page=&size=`, `GET /api/shops/search/page?...`, `GET /api/shops/category/{categoryId}?page=&size=`, `GET /api/homepage`, `/popular-products`, `/featured-shops` | Query/path values only | `Page<ShopResponse>` or homepage maps/lists; `ShopResponse`: `id`, `name`, `description`, `address`, `contactPhone`, `contactEmail`, `imagePath`, `categoryId`, `categoryName` | Public GET paths | **Partial.** Frontend shop fields include `area`, `rating`, `delivery`, `category`; API exposes address/categoryName and no rating/delivery estimate. Verify public shop visibility for each endpoint with runtime/API tests. |
| Shop application/management | `src/pages/shopkeeper/ShopkeeperDashboard.jsx`, `MarketplaceDataContext.jsx` | `ShopController`, `ShopService` | `POST /api/shops` multipart; `GET /api/shops/my-shop`; `PUT /api/shops/{shopId}` multipart | Multipart part `shop` (`CreateShopRequest`/`UpdateShopRequest`) plus optional `image`; creation includes `name`, `description`, `address`, `categoryId`, `contactPhone`, `contactEmail` | `ShopApplicationResponse` | SHOPKEEPER; services bind owner from JWT subject | **Incompatible fields** (front uses `area`/`delivery`). Use numeric category IDs and backend address/contact model. |
| Shop approval | Admin dashboard mock actions | `AdminController`, shop repository/service logic | `GET /api/admin/shops` or `/page`; `PATCH /api/admin/shops/{shopId}/approval` | List query includes status/page/size; approval action query parameter `status` uses `ShopApprovalStatus`; rejection reason is supported by controller params | `Page<AdminShopResponse>` / `AdminShopResponse` | ADMIN | **Not wired.** Map Pending/Approved/Rejected and rejection reason to actual enum/request parameters. |
| Products/catalog | `src/data/marketplace.js`, `MarketplaceDataContext.jsx`, product cards/detail/shop views, seller dashboard | `ProductController`, `ProductService` | `GET /api/shops/{shopId}/products?page=&size=`, `GET /api/products/{productId}` | Path/query only | `Page<ProductResponse>` or `ProductResponse`: `id`, `name`, `description`, `brand`, `variant`, `packSizeValue`, `packSizeUnit`, `price`, `currentPrice`, `stockQuantity`, `available`, `imagePath`, `shopId`, `shopName`, `dealType`, `discountAmount`, `dealExpiresAt` | Public GET; product detail requires public approved/active product | **Incompatible IDs/shape.** Front `wasPrice`, `unit`, boolean `clearance`, image URL differ. Add mapping, preserve shop owner relation. |
| Product create/update | Seller dashboard product form | `ProductController`, `ProductService` | `POST /api/shops/{shopId}/products` multipart; `PUT /api/shops/{shopId}/products/{productId}` multipart; `DELETE` same product path | Multipart `products` part (`CreateProductRequest`) or `product` (`UpdateProductRequest`) plus optional `image`. Fields include `name`, `description`, `brand`, `variant`, `packSizeValue`, `packSizeUnit`, `price`; creation also `stockQuantity` | `ProductResponse`; delete `204` | SHOPKEEPER; service verifies active approved owned shop and product belongs to shop | **Mapping needed.** Front form fields/stock separation require adapters and keep stock update distinct from product details. |
| Product search/comparison | Homepage search, shop detail, product detail comparison | `ProductController`, `ProductService` | `GET /api/products/search?keyword=&page=&size=`, `/search/page`, `GET /api/shops/{shopId}/products/search?...`, `GET /api/products/compare?brand=&name=&variant=&packSizeValue=&packSizeUnit=&page=&size=` | Query parameters shown in paths | `Page<ProductResponse>` / `Page<ProductComparisonResponse>` | Public GET | **Partial.** Front uses client filtering and `comparisonKey`; backend comparison uses exact brand/name/variant/pack-size query. Map semantic identity and pagination. |
| Inventory | Seller dashboard inventory tab | `ProductController`, `ProductService`, `StockAdjustmentService` | `GET /api/shops/{shopId}/products/{productId}/stock`; `POST .../stock`; `PUT .../stock`; `GET .../stock/history?page=&size=`; `PATCH .../availability?available=` | `AddStockRequest.quantity` (min 1); `SetStockRequest.quantity` (min 0), optional `reason`; availability query boolean | `ProductResponse`, `Integer`, or `Page<StockAdjustmentResponse>` | SHOPKEEPER; owner and shop/product association checked | **Backend capability exists; no API wiring.** Stock mutation is separate from product edits. |
| Cart | `src/context/CartContext.jsx`, `src/pages/CartPage.jsx`, product cards/detail | `CartController`, `CartService` | `GET /api/cart`; `POST /api/cart/items`; `PATCH /api/cart/items/{itemId}`; `DELETE /api/cart/items/{itemId}`; `DELETE /api/cart` | Add: `AddCartItemRequest(productId, quantity, clearExistingCart)`; update: `UpdateCartQuantityRequest(quantity)` | Get: map with `items` (`CartItemResponse[]`) and `total`; mutations return `CartItemResponse[]`; fields include `id`, `productId`, product display fields, `quantity`, `stockQuantity`, `unitPrice`, `subtotal`, `available` | CUSTOMER | **Major behavior/shape mismatch.** Front allows multiple shops and stores product IDs/variants in localStorage; backend supports single shop and cart unique key is customer/product. Handle mismatch confirmation and map server cart IDs. |
| Checkout/order creation | `src/pages/CheckoutPage.jsx`, `OrderSummaryPage.jsx`, `CartContext.jsx`, `OrderContext.jsx` | `CartController`/`CartService` and `OrderController`/`OrderService` | `POST /api/cart/checkout` or `POST /api/orders` | Cart checkout: `CartCheckoutRequest(fulfillmentType, deliveryAddress)`. Direct order: `CreateOrderRequest(shopId, deliveryAddress, fulfillmentType, items[{productId, quantity}])` | `OrderController.OrderResponse`: `id`, `shopId`, `shopName`, shop contacts, `customerName`, `customerPhone`, `fulfillmentType`, `deliveryAddress`, `totalAmount`, `status`, `createdAt`, `items[{productId, productName, quantity, priceAtPurchase, subtotal}]` | CUSTOMER | **Major mismatch.** Frontend currently requires local structured address, no fulfillment choice, creates local `VLN-...` record and supports cross-shop lines. Decide adapter and move totals/stock authority to backend response. |
| Orders/customer history/detail | `src/pages/OrdersPage.jsx`, `OrderConfirmationPage.jsx`, `OrderContext.jsx` | `OrderController`, `OrderService` | `GET /api/orders/my-orders?page=&size=`; `GET /api/orders/{orderId}` | Query/path only | `Page<OrderResponse>` / `OrderResponse` above | CUSTOMER; order detail service scopes by authenticated customer | **Not wired.** Front statuses, order reference and snapshots differ; use current user scoped server APIs. |
| Seller orders | `src/pages/shopkeeper/ShopkeeperDashboard.jsx`, `OrderContext.jsx` | `OrderController`, `OrderService` | `GET /api/orders/my-shop-orders?page=&size=`; `GET /api/orders/my-shop-orders/{orderId}`; `GET /api/orders/shop/{shopId}?page=&size=` | Query/path only | `Page<OrderResponse>` / `OrderResponse` | SHOPKEEPER; `my-shop` service resolves authenticated shop; shop-specific method verifies owner | **Not wired.** Backend orders are one-shop so seller sees its complete order; map status and delivery fields. |
| Order status | Seller/customer/admin dashboard mock updates | `OrderController`, `OrderService` | `PATCH /api/orders/{orderId}/status?status=...` | Enum query parameter `status` (`OrderStatus`) | `OrderResponse` | Controller permits CUSTOMER/SHOPKEEPER/ADMIN; service restricts actor and transitions | **Status mismatch.** UI must only offer actions allowed to actor and current state; admin has broader service override. |
| Admin dashboard/operations | `src/pages/admin/AdminDashboard.jsx`, `AdminLayout.jsx`, local data contexts | `AdminController` | `GET /api/admin/dashboard`; `GET /api/admin/users[?role=&keyword=&page=&size=]`; `GET /users/{userId}`; `PATCH /users/{userId}/suspension?suspended=`; `GET /shops[?status=&page=&size=]`; `PATCH /shops/{shopId}/approval?...`; `PATCH /shops/{shopId}/suspension?suspended=`; `GET /products[?keyword=&page=&size=]`; `PATCH /products/{productId}/active?active=`; `GET /orders[?status=&keyword=&page=&size=]`; `GET /orders/{orderId}`; category POST/PUT multipart, DELETE and active PATCH | Query/path and multipart category fields/image | Count map; paged `AdminUserResponse`, `AdminShopResponse`, `AdminProductResponse`, `AdminCategoryResponse`, and `OrderResponse` | ADMIN at controller class | **Capabilities exist; frontend uses local mock state.** Map query conventions, enum values and DTO fields. |
| Delivery method/address | Checkout and order pages above | `OrderController`/`CartController`, `OrderService`, `ShopController` | Checkout uses `POST /api/cart/checkout` or `POST /api/orders`; shop address via `GET /api/shops/{shopId}` | `fulfillmentType`, optional `deliveryAddress`; backend enum accepts `DELIVERY`/`PICKUP` | Order response has fulfillment type, delivery address and shop ID/name; ShopResponse has shop address | CUSTOMER for checkout; public shop detail | **Partial.** Adapter maps agreed labels to backend values. Pickup location must be rendered from shop address; address is not an order-response snapshot. |

## 5. Order lifecycle and status mapping

### Verified backend lifecycle

`OrderStatus` values are `PLACED`, `ACCEPTED`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED`.

`OrderService.updateOrderStatus` enforces the normal progression:

`PLACED → ACCEPTED → PREPARING → READY → COMPLETED`

Completed and cancelled are terminal. Customer can cancel only while `PLACED`. The owning shopkeeper can cancel at `PLACED` or `ACCEPTED`. Shopkeeper progress updates must be exactly the next state. Admin can override the sequence subject to terminal-state and no-return-to-PLACED checks.

### Recommended customer-facing labels

| Backend value | General/customer label | Shop delivery presentation | Customer pickup presentation |
|---|---|---|---|
| `PLACED` | Order placed | Awaiting shop acceptance | Awaiting shop acceptance |
| `ACCEPTED` | Accepted by shop | Shop accepted the delivery order | Shop accepted the pickup order |
| `PREPARING` | Being prepared | Shop is preparing for dispatch | Shop is preparing for collection |
| `READY` | Ready | Ready for dispatch | Ready for pickup |
| `COMPLETED` | Completed | Delivered | Collected |
| `CANCELLED` | Cancelled | Cancelled | Cancelled |

These are display labels only. Keep backend enum strings unchanged and centralize label mapping in frontend presentation code. Both fulfillment types use the same backend sequence; presentation differs at READY/COMPLETED. There is no backend `SHIPPED` state, so do not show a Shipped action/state unless a future explicit backend decision adds it. Whether `COMPLETED` means delivery completion or pickup collection is inferred from fulfillment type; runtime/product-owner confirmation is recommended before customer copy is finalized.

## 6. Delivery method and address contract

### Existing backend contract (verified)

- `CreateOrderRequest` contains `shopId`, nullable `deliveryAddress` (max 255), required `fulfillmentType`, and a nonempty `items` list of positive `productId` and `quantity` (max 50 items).
- Nested enum is `PICKUP`, `DELIVERY`; entity enum `FulfillmentType` has the same values.
- `CartCheckoutRequest` contains required `fulfillmentType` and nullable `deliveryAddress` (max 255).
- `OrderService.createOrder` rejects missing/blank delivery address for `DELIVERY`. For `PICKUP`, it stores null even if a string was supplied.
- `Shop.address` is required, and `ShopResponse` returns that address.
- `OrderResponse` returns `fulfillmentType`, `deliveryAddress`, `shopId`, and `shopName`, but not `shopAddress`.

### Proposed Velnora-to-wire mapping

| Velnora domain/UI value | Existing backend wire enum | Address handling |
|---|---|---|
| `SHOP_DELIVERY` | `DELIVERY` | Send a nonblank delivery address. Backend rejects missing/blank address and stores trimmed text. |
| `CUSTOMER_PICKUP` | `PICKUP` | Do not require/send a delivery address. Backend stores null. Resolve pickup location from the selected shop's existing `ShopResponse.address`. |

No backend enum change is required if the frontend adapter maps these values. Do not send `SHOP_DELIVERY` or `CUSTOMER_PICKUP` directly to the current enum parser.

### Contract gaps and recommendations

1. Frontend checkout must offer both methods and make address fields/selection conditional on shop delivery.
2. Checkout must be restricted to one cart shop; derive `shopId` from that shop's product IDs and send the matching product IDs/quantities.
3. For pickup, fetch/cache selected shop details and display its current `address`; never substitute a customer address.
4. Order response lacks a pickup-address snapshot. Current shop address is sufficient to meet the stated “shop's existing address” decision, but a shop address edit can change what historic order pages display. Decide whether to accept current-address display or later snapshot an address for historical accuracy. That future decision may require response/model work; it is not implemented here.
5. Delivery address is a single string in the backend, while the mock frontend stores name, phone, street, city, state, PIN, and optional landmark separately. Serialize the approved fields into the existing string; customer identity/phone are separately sourced by backend from authenticated user. Confirm formatting and any length strategy before integration; do not assume all structured address parts fit 255 characters.

## 7. Authentication and security review

### Existing implementation

- JWT login/register at `POST /api/auth/login` and `/register`; response includes bearer token and expiry.
- BCrypt password encoder is configured. Registration hashes the password.
- Public registration accepts `CUSTOMER` or `SHOPKEEPER`; `ADMIN` is rejected by `AuthService` even though a role field is present in the request DTO.
- Security is stateless, validates issuer/token version and checks current user enabled state. `POST /api/account/logout` increments token version; `/api/account/password` changes password and increments token version.
- Role annotations protect admin endpoints, customer cart/checkout and order endpoints, and shopkeeper management endpoints.
- Product/shop services compare authenticated email/owner to requested shop and verify product-shop association for protected seller changes.
- Customer order detail lookup is scoped to authenticated customer; seller order lookup resolves the authenticated seller's own shop.
- Checkout recalculates prices on backend, obtains product row locks/refreshes stock, rejects wrong shop, unavailable product or insufficient quantity, and decrements stock transactionally.

### Missing frontend integration and security work

- Current `AuthContext` uses mock accounts/registered users in local storage; registered mock password values are stored in plaintext. Remove that mock credential path from production authentication; never migrate saved mock passwords.
- Current `apiClient.js` only configures an Axios base URL and Accept header. It does not attach bearer tokens, refresh/auth state, or normalize 401/403/API errors.
- Do not treat React route guards as authorization. Backend role/ownership checks remain mandatory; verify they cover every endpoint and resource.
- Choose token persistence deliberately. Prefer an HttpOnly Secure SameSite cookie if backend deployment supports it; otherwise use a documented short-lived token strategy and assess XSS risk. Do not copy the mock localStorage credential pattern for JWTs without an explicit threat-model decision.
- Login success should map backend role strings (`CUSTOMER`, `SHOPKEEPER`, `ADMIN`) to the frontend role names and fetch profile/shop identity as needed. Role values must not be trusted from browser-supplied session fields.
- Logout should call `/api/account/logout` with valid JWT before clearing local auth; handle expired token gracefully.
- Add request-level tests for customer, shopkeeper, admin, wrong-owner, suspended user, invalid token, and forbidden resource combinations.

## 8. Database compatibility review

| Data | Verified mapping/relationship | Compatibility notes |
|---|---|---|
| Users/roles | User primary key; roles are enum-backed; unique email and nullable unique phone were verified in entity/repository audit | Front mock users use email/name/string role; backend uses numeric IDs/fullName/enum. Public registration cannot create admin. |
| Shop ownership | Shop has one required category relation and an owner relation; service resolves shop by authenticated owner | Mock `shopId` is a slug; backend IDs are numeric. Use server-returned IDs. |
| Shop/category | Shop category foreign key is non-null; each shop has one category | Matches agreed rule. Category records have backend numeric IDs and name/image path. |
| Product/shop | Product belongs to one shop; no product-category relation found | Matches business rule. Map IDs and `packSizeValue`/`packSizeUnit`; retain owning shop id. |
| Inventory | Quantity on Product; stock adjustment records point to product and actor; service protects mutations; manual SQL adds nonnegative check | No separate warehouse/inventory entity. Frontend local update state should be replaced by stock endpoints and refreshed product data. |
| Cart | CartItem references customer and product, unique `(customer_id, product_id)` | Single-shop enforced in service, not as a cart-level shop foreign key. Cart schema does not include variant discriminator, while UI key includes variant/unit; backend product IDs represent individual products. |
| Orders/order items | Order has required shop/customer foreign keys, fulfillment enum, status enum, total, nullable text delivery address, customer name/phone snapshots; items cascade from order and reference product with quantity/price/subtotal snapshots | Exactly one shop/order matches final decision. `OrderItem` does not snapshot variant/brand/image; customer-facing history may resolve product fields from current product. Evaluate historic display if products can change/delete. |
| Delivery | Fulfillment type is persisted in `customer_orders`; delivery address nullable | Service enforces address only for DELIVERY; entity/DB mapping alone does not express conditional nullability. Keep service validation and test it. |
| Pickup location | Shop's required `address` is available; no separate order pickup address snapshot found | Read shop address by shopId for pickup display; historical stability is not guaranteed if shop address changes. |
| SQL/migrations | `src/main/resources/db/manual/` contains dated manual scripts (stock/security/comparison and shop rejection/index changes); no Flyway/Liquibase dependency or migration framework identified in `pom.xml` | Scripts say apply once and use non-idempotent DDL. Verify deployed schema version and execution history before changes. No DB was accessed. |

## 9. Missing features and blockers

### Critical blockers before checkout integration

1. Frontend multi-shop cart/order behavior must be converted to the approved single-shop flow.
2. Frontend must offer delivery and pickup, map to backend enum values, and apply conditional address validation.
3. Frontend cart fixture IDs and product/variant identity must be replaced/mapped to backend IDs. Backend cart uniqueness is product-based, not the UI's composite product/variant key.
4. Frontend order ref `VLN-...` does not map to backend numeric `id`; backend has no separate public order reference field in `OrderResponse`. Decide whether to display a formatted ID or introduce a future backend reference contract before relying on search by reference.
5. Frontend status labels and actions must be adapted to the backend transition graph; there is no `SHIPPED` state.

### Other missing/unsupported capabilities

- No backend address book/list/create/update/delete API or Address entity was found. Checkout accepts one address string only.
- No payment processing or payment status API/entity was found.
- No backend password reset/forgot-password endpoint was found.
- Backend does not include pickup location in the order response; it can be looked up from shop details, without a historical snapshot.
- Frontend mock `rating`, delivery estimate, product `comparisonKey`, local stock/product status and some card display fields have no one-to-one backend response field. Confirm sources or omit/map as derived data.
- No runtime API contract test or deployment base URL/CORS check was performed.

## 10. Prioritized Task 10.1 implementation plan

Task 10.1 should prepare the frontend contract layer and single-shop/fulfillment UI without rewriting pages. Recommended order:

1. **Contract types and mapping functions:** define backend response types for category/shop/product/cart/order/auth; map numeric IDs, `fullName`, `currentPrice`, pack size, deal type, enum roles and order statuses into existing UI view models. Keep API enum values distinct from display labels.
2. **Single-shop cart behavior:** add the other-shop confirmation described above; confirm-before-clear; preserve original cart on cancel; after confirmation call cart add with `clearExistingCart=true`; handle backend `CART_SHOP_MISMATCH`, stock conflict, unavailable item and server totals.
3. **Checkout fulfillment UI:** add Shop Delivery and Customer Pickup. Require address only for Shop Delivery; show shop address for pickup. Map `SHOP_DELIVERY` to `DELIVERY` and `CUSTOMER_PICKUP` to `PICKUP` in a single API adapter.
4. **Address serializer and validation:** define exactly which saved address fields become the backend 255-character delivery string; test long/empty values. Do not send address for pickup. Keep the frontend address book local-only until a backend address API is separately approved.
5. **Server cart and checkout service methods:** route requests through existing `apiClient`; attach bearer token via the chosen session approach; use backend cart as source of truth and order response totals/status/IDs as authoritative.
6. **Order lifecycle adapter:** map backend states and allowed next actions to customer/shopkeeper UI per role and fulfillment type. Remove assumptions of `Shipped`; keep admin controls constrained to backend behavior.
7. **Error and loading states:** map validation, 401, 403, stock conflicts, empty cart, shop mismatch and network failures to inline messages with retry/continue actions.
8. **Mock mode boundary:** retain existing fixtures as an explicit development mode, separate from real login. Prevent fixtures or plaintext demo credentials from entering real API auth or production builds.
9. **Acceptance/API checks:** use controller-backed tests for DTO serialization, role access, owner isolation, cart mismatch confirmation, delivery address required, pickup address optional, stock concurrency, and order visibility before enabling the integrated flow.

Do not begin broad dashboard integration until contract adapters and auth handling are stable.

## 11. Suggested Task 11 integration sequence

1. Confirm backend base URL, CORS allowed origins and runtime schema/manual SQL version (no secrets in frontend config).
2. Integrate public read-only catalog: categories, approved shops, products, search and clearance; validate numeric IDs and image paths.
3. Integrate JWT login/register/profile/logout and role routing; verify backend denial for unauthorized roles.
4. Integrate shopkeeper shop profile and product CRUD; then stock adjustments/availability; confirm ownership enforcement.
5. Integrate one-shop cart and checkout only after Task 10.1 mapping; verify address/pickup and inventory transaction behavior.
6. Integrate customer orders and order details, preserving user-scoped access and status presentation.
7. Integrate shopkeeper orders/status updates and customer-visible synchronization.
8. Integrate admin dashboard operations using admin-only endpoints and the backend enum/DTO contracts.
9. Run end-to-end checks, viewport/accessibility review, API authorization tests, and production build after each module; deploy only after schema and rollback plans are confirmed.

## 12. Testing requirements and acceptance criteria

### Cart and checkout

- Cart contains one `shopId` only after each successful action.
- Adding from another shop prompts before clearing; cancel preserves every item/quantity; confirm replaces cart with the requested item and backend confirms it.
- Duplicate same product increments quantity; invalid/nonpositive/over-stock quantities are rejected; unavailable or unapproved shop items cannot check out.
- Checkout sends correct shop, product IDs, quantities and fulfillment type; backend prices/totals are used rather than browser totals.
- Delivery with blank address fails; valid delivery address is persisted and returned.
- Pickup succeeds with no customer address, stores null delivery address, and displays the selected shop's address.
- Each order has exactly one shop, and separate shop purchases create separate orders.
- Stock is not negative under concurrent checkout; cancellation/restock follows backend service rules.

### Order lifecycle and permissions

- Verify all transitions `PLACED → ACCEPTED → PREPARING → READY → COMPLETED`, terminal states, and cancellation rules for customer/shopkeeper/admin.
- Delivery and pickup labels vary appropriately without altering backend status values.
- A customer can only read their own orders; a shopkeeper can only read/update their own shop orders; admin endpoints reject non-admin roles.
- Verify wrong-shop product IDs in order payload are rejected.

### Authentication and data mapping

- Register only supported roles; admin registration is rejected server-side.
- Login response role/token/expiry maps correctly; missing/expired/revoked token yields a clear signed-out state.
- Do not persist or log plaintext passwords; test token/logout behavior.
- Test DTO field and identifier mapping for categories, shops, products, cart, checkout and orders, including discounts, image paths, variants and empty/null optional fields.

### Operational/UI

- Verify backend CORS and configured API base URL in the target environment without exposing secrets.
- Test empty/loading/error/401/403/validation states and retries.
- Check customer, seller, and admin flows at 390px and desktop widths after implementation; verify keyboard focus and no horizontal overflow.
- Run API/contract/integration tests and frontend production build after implementation. None were run during this planning task.

## Verification scope and limitations

The complete shared `SKILL.md` was read. The exact requested copy at `frontend/skills/landing-page-design/SKILL.md` is absent in the project; this report uses the shared file you supplied. Its complete visual system was considered, while the explicit Velnora brand requirements in this task take precedence. Actual project paths are `frontend/` and `rural-marketplace/`.

Source inspection only: no server was started, no endpoints called, no tests/builds executed, and no live database/schema queried. Controller contracts and security findings therefore require runtime verification against the target deployment.
