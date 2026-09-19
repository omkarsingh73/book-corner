# Book Corner REST API - Postman Collection & Test Suite

Production-grade Postman collection and environment specification for the **Book Corner** online bookstore platform, fully covering all **44 REST endpoints** (plus Spring Boot Actuator health and telemetry probes) with realistic, real-world domain test data, automated Chai assertions, and dynamic end-to-end chaining.

---

## 📁 Files Included

| File | Description |
| :--- | :--- |
| [`book_corner.postman_collection.json`](./book_corner.postman_collection.json) | Complete Postman v2.1.0 collection containing 12 organized folders, 50 executable test requests, and Chai assertions. |
| [`book_corner_local.postman_environment.json`](./book_corner_local.postman_environment.json) | Local development environment presets for `http://localhost:8080`, store codes, and initial catalog UUIDs. |
| [`build_collection.py`](./build_collection.py) | Standalone Python generator script used to maintain, audit, and re-generate the collection files. |

---

## 🚀 Quick Start Guide

### 1. Import into Postman App

1. Launch Postman (Desktop or Web).
2. Click **Import** (top left).
3. Drag & drop or select both:
   - `postman/book_corner.postman_collection.json`
   - `postman/book_corner_local.postman_environment.json`
4. In the top right environment dropdown, select **"Book Corner - Local Environment"**.

### 2. Start the Backend Application

Ensure the Spring Boot backend and PostgreSQL database are running:

```bash
# Using Docker Compose (PostgreSQL, Redis, and Application)
docker-compose up -d

# Or running locally via Maven
mvn spring-boot:run
```

Verify the application is healthy by running the request in `12. System Health & Observability -> 01. System Health Check` (`http://localhost:8080/actuator/health`).

### 3. Run Entire Suite via Postman Collection Runner

1. Click on the collection root **"Book Corner REST API - Full Test Suite"**.
2. Click **Run** button.
3. Keep the default execution order and click **Run Book Corner REST API**.
4. All 50 requests will execute sequentially from top to bottom. Dynamic authentication tokens (`accessToken`, `refreshToken`), session IDs, address UUIDs, order numbers, and RMA codes are automatically propagated from one request to the next!

### 4. Run via Newman CLI (Automated CI/CD)

You can run the entire suite headless from the command line using [Newman](https://github.com/postmanlabs/newman) (via `npx` or global install):

```bash
# Using npx (no global installation needed)
npx newman run postman/book_corner.postman_collection.json \
  -e postman/book_corner_local.postman_environment.json \
  --reporters cli

# Generate HTML report (optional)
npx newman run postman/book_corner.postman_collection.json \
  -e postman/book_corner_local.postman_environment.json \
  -r cli,htmlextra
```

---

## 📋 Comprehensive Endpoint Coverage Matrix

The collection is organized into 12 domain folders covering 100% of the API surface:

### 1. Authentication & Sessions (`/auth`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 1 | Start Anonymous Guest Session | `POST` | `/auth/guest-session` | Issues signed guest session token with store code tenant binding. |
| 2 | Register Customer Account (Primary) | `POST` | `/auth/register` | Registers customer profile with dynamic timestamped email; saves JWT access and refresh tokens. |
| 3 | Authenticate User Credentials (Login) | `POST` | `/auth/login` | Validates credentials and yields fresh token pair. |
| 4 | Rotate JWT Refresh Token | `POST` | `/auth/refresh` | Performs cryptographically safe refresh token rotation. |
| 5 | Register Auxiliary Reviewer Account | `POST` | `/auth/register` | Registers secondary customer (`Samantha Reed`) to verify review helpfulness voting without self-voting rules. |
| 6 | Invalidate User Session (Logout) | `POST` | `/auth/logout` | Revokes auxiliary customer tokens. |

### 2. Customer Profile & Addresses (`/users`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 7 | Retrieve Current Customer Profile | `GET` | `/users/me` | Fetches authenticated customer profile and role (`ROLE_CUSTOMER`). |
| 8 | Update Customer Profile | `PATCH` | `/users/me` | Partially updates user name and E.164 contact phone number. |
| 9 | List Saved Addresses | `GET` | `/users/me/addresses` | Returns customer address book collection. |
| 10 | Add Primary Shipping Address | `POST` | `/users/me/addresses` | Creates default delivery address (`742 Evergreen Terrace, Springfield, OR 97477`); stores `addressId`. |
| 11 | Add Ephemeral Address | `POST` | `/users/me/addresses` | Creates secondary billing address to test deletion without altering checkout state. |
| 12 | Delete Address from Address Book | `DELETE` | `/users/me/addresses/:addressId` | Deletes the ephemeral address. |

### 3. Catalog & Discovery (`/categories`, `/authors`, `/publishers`, `/books`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 13 | List Hierarchical Categories Tree | `GET` | `/categories` | Fetches nested category tree (Computer Science, Architecture, Sci-Fi). |
| 14 | Browse Authors | `GET` | `/authors` | Paginated search filtering authors (`Robert C. Martin`, `Erich Gamma`). |
| 15 | Get Author Details by ID | `GET` | `/authors/:authorId` | Retrieves biography and author slug. |
| 16 | List Publishers | `GET` | `/publishers` | Lists publishers (Pearson, Addison-Wesley, O'Reilly Media). |
| 17 | Browse Books with Faceted Filters | `GET` | `/books` | Faceted queries by binding (`PAPERBACK`), price band (`1000-6000` cents), and language. |
| 18 | Get Book Details by ID | `GET` | `/books/:bookId` | Fetches full specification for *Clean Code* and extracts purchasable format SKUs (`formatId`). |

### 4. Search & Autocomplete (`/search`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 19 | Multi-Faceted Full-Text Search | `GET` | `/search` | Full-text keyword query (`Clean`) with facet summaries. |
| 20 | Autocomplete Typeahead Suggestions | `GET` | `/search/suggestions` | Fast autocomplete matching titles and authors for search input boxes. |

### 5. Customer Wishlist (`/wishlists`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 21 | Retrieve Customer Wishlist | `GET` | `/wishlists` | Lists saved items and active price monitoring alerts. |
| 22 | Add Book to Wishlist with Price Alert | `POST` | `/wishlists/items` | Adds *Design Patterns* with a target alert price ($40.00). |
| 23 | Remove Book from Wishlist | `DELETE` | `/wishlists/items/:bookId` | Deletes item from customer wishlist. |

### 6. Shopping Cart & Basket (`/cart`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 24 | Retrieve Active Shopping Cart | `GET` | `/cart` | Retrieves current basket, line items, and financial subtotals. |
| 25 | Add Primary Format Item to Cart | `POST` | `/cart/items` | Adds 2 units of *Clean Code Paperback* (`formatId`). |
| 26 | Add Secondary Format Item to Cart | `POST` | `/cart/items` | Adds 1 unit of *Design Patterns Hardcover* (`formatId2`). |
| 27 | Mutate Cart Item Quantity | `PATCH` | `/cart/items/:formatId` | Updates quantity to 3 units, checking inventory and recalculating subtotal. |
| 28 | Remove Secondary Item from Cart | `DELETE` | `/cart/items/:formatId` | Removes secondary line item from basket. |
| 29 | Apply Promotional Coupon Code | `POST` | `/cart/coupon` | Applies coupon code `SUMMER20` to calculate discounts. |
| 30 | Remove Promotional Coupon | `DELETE` | `/cart/coupon` | Clears applied coupon and reverts to standard pricing. |
| 31 | Merge Anonymous Guest Basket | `POST` | `/cart/merge` | Synchronizes anonymous guest basket into customer account. |

### 7. Checkout & Order Lifecycle (`/orders`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 32 | Submit Checkout Saga and Confirm Order | `POST` | `/orders/checkout` | Idempotent checkout saga decrementing inventory, snapshotting addresses, charging payment, creating consignment, and returning `ORD-` number. |
| 33 | List Customer Order History | `GET` | `/orders` | Paginated listing of customer confirmed orders. |
| 34 | Retrieve Detailed Order & Invoice | `GET` | `/orders/:orderNumber` | Detailed invoice snapshot, line items, and stores `orderLineItemId`. |
| 35 | Initiate RMA Return | `POST` | `/orders/:orderNumber/returns` | Creates RMA authorization (`RMA-`) and issues prepaid UPS return tracking label. |
| 36 | Place Secondary Order (For Cancellation) | `POST` | `/orders/checkout` | Seeds fresh basket and places order specifically to test policy grace period cancellation. |
| 37 | Cancel Order within Policy Grace Period | `POST` | `/orders/:orderNumber/cancel` | Executes self-service cancellation within 60-minute window, auto-restocking inventory. |

### 8. Payments & Digital Wallet (`/payments`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 38 | Coordinate Multi-Tender Payment Intent | `POST` | `/payments/intent` | Prepares split-tender authorization (Credit Card + Store Wallet balance). |
| 39 | Ingest Payment Gateway Webhook | `POST` | `/payments/webhooks/:provider` | Simulates asynchronous Stripe charge success webhook event with signature header. |
| 40 | Retrieve Customer Digital Wallet Balance | `GET` | `/payments/wallet` | Queries store credit ledger, currency code, and lock status. |

### 9. Shipping & Logistics (`/shipping`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 41 | Calculate Dynamic Carrier Shipping Rates & ETA | `POST` | `/shipping/rates` | Calculates delivery estimates and freight costs by weight and postal code. |
| 42 | Track Carrier Consignment Milestones | `GET` | `/shipping/consignments/:trackingNumber` | Queries package milestones (`MANIFEST_CREATED`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`). |

### 10. Reviews & Helpfulness Ratings (`/books/:bookId/reviews`, `/reviews/:reviewId/votes`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 43 | List Customer Book Reviews | `GET` | `/books/:bookId/reviews` | Paginated reviews sorted by helpfulness score. |
| 44 | Submit Verified Customer Review & Rating | `POST` | `/books/:bookId/reviews` | Submits 5-star rating and detailed written review; extracts `reviewId`. |
| 45 | Vote on Review Helpfulness | `POST` | `/reviews/:reviewId/votes` | Secondary user casts helpful upvote; validates counter increment. |

### 11. Merchandising & Recommendations (`/recommendations`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 46 | Get Personalized Home Page Recommendations | `GET` | `/recommendations/home` | Multi-category landing carousels and trending picks. |
| 47 | Get Book Up-Sell Collector Editions | `GET` | `/recommendations/books/:bookId/upsell` | Suggests collector hardcover editions and upgrade variants. |
| 48 | Get Book Cross-Sell Companion Titles | `GET` | `/recommendations/books/:bookId/cross-sell` | Recommends companion bundles (e.g. *Clean Architecture* alongside *Clean Code*). |

### 12. System Health & Observability (`/actuator`)
| # | Request Name | Method | Endpoint | Description |
|---|---|---|---|---|
| 49 | System Health Check | `GET` | `/actuator/health` | Probes application liveness, readiness, and PostgreSQL connectivity. |
| 50 | System Application Info | `GET` | `/actuator/info` | Spring Boot build metadata and environment info. |

---

## 🔑 Realistic Test Data Reference

| Entity | Real-World Test Value | Context / Source |
| :--- | :--- | :--- |
| **Store Code** | `BK_MAIN_ONLINE` | Seed flagship digital storefront |
| **Primary Book** | `30000000-0000-0000-0000-000000000001` | *Clean Code: A Handbook of Agile Software Craftsmanship* |
| **Secondary Book** | `30000000-0000-0000-0000-000000000002` | *Design Patterns: Elements of Reusable Object-Oriented Software* |
| **Format SKU 1** | `31000000-0000-0000-0000-000000000001` | `SKU-CC-PB` (Paperback, $39.99 / 3999 cents) |
| **Format SKU 2** | `31000000-0000-0000-0000-000000000004` | `SKU-DP-HC` (Hardcover, $54.99 / 5499 cents) |
| **Author** | `40000000-0000-0000-0000-000000000001` | Robert C. Martin (`robert-c-martin`) |
| **Category** | `10000000-0000-0000-0000-000000000031` | Computer Science (`computer-science`) |
| **Shipping Address** | `742 Evergreen Terrace, Springfield, OR 97477, US` | Standard US domestic residential delivery destination |
| **Coupon Code** | `SUMMER20` | Promotional percentage discount coupon |
| **Payment Token** | `pm_card_visa_tok_4242` | Simulated Stripe test token |
| **Consignment Carrier** | `FEDEX` / `UPS` | Standard ground freight carrier |
| **Monetary Units** | Integers in cents (e.g., `3999` = $39.99 USD) | Invariant representation across all schemas |

---

## 🛠️ Automated Dynamic Test Assertions

Each request includes Chai test assertions verifying:
1. **HTTP Status Codes**: `200 OK`, `201 Created`, or `204 No Content`.
2. **Latency SLAs**: Response time `< 1500ms` - `2000ms`.
3. **Response Schema & Headers**: Valid `application/json` payload structure.
4. **State Transitions**: Quantity mutates properly, totals reflect subtotal adjustments, and status transitions (`PENDING` -> `CONFIRMED` -> `CANCELLED` / `RMA REQUESTED`).
5. **Dynamic Variable Stashing**: Automatically extracts tokens and IDs so no manual copy-pasting is ever required:
   ```javascript
   pm.collectionVariables.set("accessToken", jsonData.accessToken);
   pm.collectionVariables.set("orderNumber", jsonData.orderNumber);
   ```
