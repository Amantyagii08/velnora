# Frontend project context

## Task 02 — Customer homepage
- Built a desktop-first homepage from reusable sections: navigation, hero, categories, products, local shops, good finds, value section, seller callout, and footer.
- Mock marketplace data lives in `src/data/marketplace.js` and includes 4 categories, 10 products, 4 shops, and 5 clearance items.
- Local interactions: product/brand search, shop-name search, category filtering, shop-to-products browsing, saved-product toggles, and a quantity-aware bag preview.
- Product records are shared across homepage sections; all remote images have a visual fallback.
- Backend integration, login, checkout, and dashboards remain out of scope.

## Task 03 — Mock authentication
- Added local login, customer registration, and forgot-password routes with client-side validation and a password visibility toggle.
- Mock customer, shopkeeper, and admin accounts live in `src/context/AuthContext.jsx`; registered demo accounts and the signed-in session persist in local storage only.
- Customer sign-in returns to the homepage with account and logout controls. Shopkeeper/admin sign-in routes to a role-specific placeholder; dashboards remain out of scope.
- No backend API, authentication service, or database is connected.

## Task 05 — Cart and checkout
- Added a shared local cart context with localStorage persistence. Cart lines resolve by product ID to the original mock product and shop records; matching product variants merge and quantities stay within current stock.
- Added cart, checkout, and order confirmation routes. Checkout supports saved and new delivery addresses with validation, calculates totals from product prices, places a mock order, and clears the cart on confirmation.
- Payment processing and backend order storage are not connected. Cart contents and saved addresses are stored only in this browser.

## Task 06 — Customer orders
- Added a shared `velnora-orders-v1` local order list. The legacy `velnora-last-order-v1` record is migrated once by order ID and retained for compatibility.
- Checkout appends orders to the shared list; confirmation, My Orders, and order details read those same order records.
- Added status filtering, order details, local cancellation, and useful empty/not-found states. Orders remain browser-local demo data.

## Task 07 — Shopkeeper workspace
- Linked the mock shopkeeper account to the existing Daily Needs Store shop record and added a seller workspace for that shop only.
- Added overview, shop information editing, product add/edit/search, separate stock updates, low-stock indicators, and shop-scoped orders with Processing → Shipped → Delivered actions.
- Shop/product edits persist in `velnora-marketplace-data-v1` and are shared with the marketplace and cart through `MarketplaceDataContext`.
- Order status updates use the shared Task 06 order list; no separate seller order records are created.

## Task 08 — Admin workspace
- Added a responsive admin dashboard with overview statistics, shop approvals, category management, a marketplace-wide product list, shared order history, and safe user/role listings.
- Shop status, category edits, and category additions/deletions persist through the existing `velnora-marketplace-data-v1` store. Public marketplace listings show approved shops and their products only.
- Admin order search, filters, and details read `velnora-orders-v1`; status stays read-only and customer contact/address details are not exposed.
- Existing admin role guard remains in place. No backend integration or backend files were changed.
