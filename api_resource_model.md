# REST API Resource Model Specification
## Online Bookstore Platform ("Book Corner")

---

## 1. Architectural Standards & Global API Conventions

### 1.1 Base URL & API Versioning
All RESTful API resources are versioned through the URI path:
- **Production Base URL**: `https://api.bookcorner.com/api/v1`
- **Sandbox / Staging**: `https://api.staging.bookcorner.com/api/v1`

### 1.2 Authentication & Context Headers
- **User Authentication**: `Authorization: Bearer <JWT_ACCESS_TOKEN>` (RFC 6750). Contains customer `sub` (UUID), `email`, `roles[]`, and `entitlements[]`.
- **Guest Session Context**: `X-Guest-Session-Token: <UUID_OR_OPAQUE_TOKEN>`. Issued to anonymous visitors for persistent cart and browsing state.
- **Store Tenant Context**: `X-Store-Code: <STORE_CODE>` (default: `BK_MAIN_ONLINE`). Selects localized catalog, currency, and store policies.
- **Idempotency**: `Idempotency-Key: <UUIDv4>`. Mandatory on mutating endpoints (`POST /orders/checkout`, `POST /payments/*`, `POST /orders/*/cancel`).
- **Distributed Tracing**: `traceparent: 00-<trace_id>-<span_id>-01` (W3C Trace Context).

### 1.3 Uniform Response Envelope
All API endpoints return JSON conforming to standard response envelopes.

#### Standard Success Envelope
```json
{
  "success": true,
  "data": {},
  "metadata": {
    "timestamp": "2026-09-18T10:15:30Z",
    "requestId": "req_8f12a3bc-59d1-4cb2-9388-129087c9d012",
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 142,
      "totalPages": 8,
      "hasNext": true,
      "hasPrevious": false
    }
  }
}
```

#### Standard Error Envelope
```json
{
  "success": false,
  "error": {
    "code": "CART_ITEM_LIMIT_EXCEEDED",
    "message": "Cannot add more than 10 units of a single book SKU to the cart.",
    "details": [
      {
        "field": "quantity",
        "rejectedValue": 12,
        "issue": "Quantity must be an integer between 1 and 10."
      }
    ],
    "timestamp": "2026-09-18T10:15:30Z",
    "requestId": "req_8f12a3bc-59d1-4cb2-9388-129087c9d012"
  }
}
```

---

## 2. API Resource Model by Domain

```
+---------------------------------------------------------------------------------------------------------+
|                                        15 REST API DOMAINS                                              |
+---------------------+---------------------+---------------------+------------------+--------------------+
| 1. Authentication   | 2. Users            | 3. Authors          | 4. Publishers    | 5. Categories      |
| 6. Books            | 7. Search           | 8. Wishlist         | 9. Cart          | 10. Orders         |
| 11. Payments        | 12. Shipping        | 13. Reviews         | 14. Coupons      | 15. Recommendations|
+---------------------+---------------------+---------------------+------------------+--------------------+
```

---

### Domain 1: Authentication

#### API 1.1: Start Anonymous Guest Session
- **Resource Name**: Guest Session Initializer
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/guest-session`
- **Headers**: `X-Store-Code: BK_MAIN_ONLINE`
- **Request DTO**:
  ```json
  {}
  ```
- **Response DTO (`201 Created`)**:
  ```json
  {
    "guestSessionToken": "gst_e7b92f8012d449fa894c25f77890a1bc",
    "expiresAt": "2026-10-18T10:00:00Z"
  }
  ```
- **Validation Rules**: Client IP and User-Agent logged; rate limited to 30 requests/min per IP.
- **Error Codes**: `429 RATE_LIMIT_EXCEEDED`, `500 INTERNAL_SERVER_ERROR`.

---

#### API 1.2: Register Customer Account
- **Resource Name**: Customer Registration
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/register`
- **Request DTO**:
  ```json
  {
    "email": "customer@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+14155552671",
    "guestSessionToken": "gst_e7b92f8012d449fa894c25f77890a1bc"
  }
  ```
- **Response DTO (`201 Created`)**:
  ```json
  {
    "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "email": "customer@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "accountStatus": "ACTIVE",
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "rft_7c8d9e0f...",
    "expiresInSeconds": 900
  }
  ```
- **Validation Rules**:
  - `email`: Required, valid RFC 5322 email string, max 255 chars, unique.
  - `password`: Required, 8–64 chars, $\ge 1$ uppercase, $\ge 1$ lowercase, $\ge 1$ digit, $\ge 1$ special symbol.
  - `firstName`, `lastName`: Required, 1–100 chars.
  - `phoneNumber`: Optional, valid E.164 format.
  - `guestSessionToken`: Optional; if provided, triggers asynchronous cart and history carryover.
- **Error Codes**: `400 VALIDATION_FAILED`, `409 AUTH_EMAIL_ALREADY_EXISTS`, `500 INTERNAL_SERVER_ERROR`.

---

#### API 1.3: User Login
- **Resource Name**: Authentication Token Issuer
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/login`
- **Request DTO**:
  ```json
  {
    "email": "customer@example.com",
    "password": "SecurePassword123!",
    "guestSessionToken": "gst_e7b92f8012d449fa894c25f77890a1bc"
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "email": "customer@example.com",
    "roles": ["ROLE_CUSTOMER"],
    "entitlements": ["VIP_CLUB"],
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "rft_7c8d9e0f...",
    "expiresInSeconds": 900
  }
  ```
- **Validation Rules**: `email` and `password` non-empty. Account must not be `SUSPENDED` or `LOCKED`.
- **Error Codes**: `401 AUTH_INVALID_CREDENTIALS`, `403 AUTH_ACCOUNT_LOCKED`, `429 AUTH_TOO_MANY_ATTEMPTS`.

---

#### API 1.4: Refresh Token Rotation
- **Resource Name**: Token Refresher
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/refresh`
- **Request DTO**:
  ```json
  {
    "refreshToken": "rft_7c8d9e0f..."
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJSUzI1NiIs...",
    "refreshToken": "rft_8d9e0f1a...",
    "expiresInSeconds": 900
  }
  ```
- **Validation Rules**: `refreshToken` must be non-empty, cryptographically valid, and not blacklisted/expired.
- **Error Codes**: `401 AUTH_TOKEN_EXPIRED`, `401 AUTH_TOKEN_REVOKED`, `400 VALIDATION_FAILED`.

---

#### API 1.5: User Logout
- **Resource Name**: Session Revoker
- **HTTP Method**: `POST`
- **URI**: `/api/v1/auth/logout`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**:
  ```json
  {
    "refreshToken": "rft_8d9e0f1a..."
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "message": "Successfully logged out. Active tokens have been invalidated."
  }
  ```
- **Validation Rules**: Requires valid authenticated Bearer token.
- **Error Codes**: `401 UNAUTHORIZED`.

---

### Domain 2: Users (Profile & Address Book)

#### API 2.1: Get Current User Profile
- **Resource Name**: Current User Profile
- **HTTP Method**: `GET`
- **URI**: `/api/v1/users/me`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "email": "customer@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+14155552671",
    "roles": ["ROLE_CUSTOMER"],
    "entitlements": ["VIP_CLUB"],
    "walletBalance": {
      "amount": 2500,
      "currency": "USD"
    },
    "createdAt": "2026-01-15T08:30:00Z"
  }
  ```
- **Error Codes**: `401 UNAUTHORIZED`, `404 USER_NOT_FOUND`.

---

#### API 2.2: Update Current User Profile
- **Resource Name**: Profile Updater
- **HTTP Method**: `PATCH`
- **URI**: `/api/v1/users/me`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**:
  ```json
  {
    "firstName": "Jonathan",
    "lastName": "Doe",
    "phoneNumber": "+14155559999",
    "version": 0
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "firstName": "Jonathan",
    "lastName": "Doe",
    "phoneNumber": "+14155559999",
    "version": 1,
    "updatedAt": "2026-09-18T10:20:00Z"
  }
  ```
- **Validation Rules**: `version` required for OCC. Phone must be valid E.164.
- **Error Codes**: `400 VALIDATION_FAILED`, `409 CONCURRENCY_CONFLICT`.

---

#### API 2.3: List Saved Addresses
- **Resource Name**: User Address Book
- **HTTP Method**: `GET`
- **URI**: `/api/v1/users/me/addresses`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  [
    {
      "addressId": "c4b1e5a8-44d2-43f1-b9a3-8349281a98c1",
      "addressType": "SHIPPING",
      "recipientName": "John Doe",
      "phoneNumber": "+14155552671",
      "streetLine1": "123 Market St",
      "streetLine2": "Suite 400",
      "city": "San Francisco",
      "stateProvince": "CA",
      "postalCode": "94103",
      "countryCode": "US",
      "isDefault": true
    }
  ]
  ```
- **Error Codes**: `401 UNAUTHORIZED`.

---

#### API 2.4: Create Address
- **Resource Name**: Address Creator
- **HTTP Method**: `POST`
- **URI**: `/api/v1/users/me/addresses`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**:
  ```json
  {
    "addressType": "SHIPPING",
    "recipientName": "John Doe",
    "phoneNumber": "+14155552671",
    "streetLine1": "123 Market St",
    "streetLine2": "Suite 400",
    "city": "San Francisco",
    "stateProvince": "CA",
    "postalCode": "94103",
    "countryCode": "US",
    "isDefault": true
  }
  ```
- **Response DTO (`201 Created`)**:
  ```json
  {
    "addressId": "c4b1e5a8-44d2-43f1-b9a3-8349281a98c1",
    "recipientName": "John Doe",
    "postalCode": "94103",
    "isDefault": true,
    "createdAt": "2026-09-18T10:25:00Z"
  }
  ```
- **Validation Rules**: `countryCode` must be 2 uppercase characters. `postalCode` validated against country regex. If `isDefault=true`, previous default is cleared.
- **Error Codes**: `400 VALIDATION_FAILED`, `401 UNAUTHORIZED`.

---

#### API 2.5: Delete Address
- **Resource Name**: Address Remover
- **HTTP Method**: `DELETE`
- **URI**: `/api/v1/users/me/addresses/{addressId}`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**: None
- **Response DTO (`204 No Content`)**: None
- **Validation Rules**: `addressId` must belong to the authenticated user.
- **Error Codes**: `401 UNAUTHORIZED`, `404 ADDRESS_NOT_FOUND`.

---

### Domain 3: Authors

#### API 3.1: List Authors
- **Resource Name**: Authors Directory
- **HTTP Method**: `GET`
- **URI**: `/api/v1/authors`
- **Query Parameters**: `page` (default 1), `size` (default 20), `query` (search by name)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  [
    {
      "authorId": "a1b2c3d4-e5f6-4a1b-8c2d-3e4f5a6b7c8d",
      "fullName": "Robert C. Martin",
      "authorSlug": "robert-c-martin",
      "avatarUrl": "https://cdn.bookcorner.com/authors/uncle-bob.jpg",
      "bookCount": 8
    }
  ]
  ```
- **Error Codes**: `400 INVALID_PAGINATION_PARAMS`.

---

#### API 3.2: Get Author Details & Bibliography
- **Resource Name**: Author Profile & Books
- **HTTP Method**: `GET`
- **URI**: `/api/v1/authors/{authorId}`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "authorId": "a1b2c3d4-e5f6-4a1b-8c2d-3e4f5a6b7c8d",
    "fullName": "Robert C. Martin",
    "authorSlug": "robert-c-martin",
    "biography": "Robert Cecil Martin, colloquially known as Uncle Bob, is an American software engineer and author.",
    "avatarUrl": "https://cdn.bookcorner.com/authors/uncle-bob.jpg",
    "bibliography": [
      {
        "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
        "title": "Clean Architecture",
        "isbn13": "9780134494166",
        "publicationYear": 2017,
        "primaryFormat": "PAPERBACK",
        "basePrice": { "amount": 3499, "currency": "USD" }
      }
    ]
  }
  ```
- **Error Codes**: `404 AUTHOR_NOT_FOUND`.

---

### Domain 4: Publishers

#### API 4.1: List Publishers
- **Resource Name**: Publisher Registry
- **HTTP Method**: `GET`
- **URI**: `/api/v1/publishers`
- **Query Parameters**: `page`, `size`, `search`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  [
    {
      "publisherId": "p1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
      "publisherName": "Prentice Hall",
      "publisherCode": "PUB_PRENTICE_HALL",
      "websiteUrl": "https://www.pearson.com"
    }
  ]
  ```
- **Error Codes**: `400 INVALID_QUERY_PARAMS`.

---

#### API 4.2: Get Publisher Details
- **Resource Name**: Publisher Profile
- **HTTP Method**: `GET`
- **URI**: `/api/v1/publishers/{publisherId}`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "publisherId": "p1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "publisherName": "Prentice Hall",
    "publisherCode": "PUB_PRENTICE_HALL",
    "contactEmail": "support@pearson.com",
    "websiteUrl": "https://www.pearson.com",
    "totalPublishedTitles": 142
  }
  ```
- **Error Codes**: `404 PUBLISHER_NOT_FOUND`.

---

### Domain 5: Categories

#### API 5.1: Get Hierarchical Category Tree
- **Resource Name**: Category Taxonomy Tree
- **HTTP Method**: `GET`
- **URI**: `/api/v1/categories`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  [
    {
      "categoryId": "cat_root_tech_01",
      "categoryName": "Computers & Technology",
      "categorySlug": "computers-technology",
      "treeLevel": 0,
      "subcategories": [
        {
          "categoryId": "cat_sub_software_02",
          "categoryName": "Software Architecture",
          "categorySlug": "software-architecture",
          "treeLevel": 1,
          "subcategories": []
        }
      ]
    }
  ]
  ```
- **Caching**: Cached on CDN/Edge with 1-hour TTL.
- **Error Codes**: `500 INTERNAL_SERVER_ERROR`.

---

#### API 5.2: Get Category Details
- **Resource Name**: Category Detail
- **HTTP Method**: `GET`
- **URI**: `/api/v1/categories/{categoryId}`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "categoryId": "cat_sub_software_02",
    "categoryName": "Software Architecture",
    "categorySlug": "software-architecture",
    "parentCategoryId": "cat_root_tech_01",
    "treeLevel": 1,
    "totalBookCount": 85
  }
  ```
- **Error Codes**: `404 CATEGORY_NOT_FOUND`.

---

### Domain 6: Books (Product Master & Formats)

#### API 6.1: Browse Books (Filter & Sort)
- **Resource Name**: Book Catalog Listing
- **HTTP Method**: `GET`
- **URI**: `/api/v1/books`
- **Query Parameters**:
  - `categorySlug`: Filter by category (e.g. `software-architecture`)
  - `authorSlug`: Filter by author (e.g. `robert-c-martin`)
  - `formatType`: Filter by format (`PAPERBACK`, `HARDCOVER`, `EBOOK`)
  - `minPrice`, `maxPrice`: In cents
  - `minRating`: 1 to 5
  - `sort`: `POPULARITY`, `PRICE_ASC`, `PRICE_DESC`, `NEWEST`, `RATING`
  - `page`: default 1, `size`: default 20
- **Headers**: Optional `Authorization: Bearer <TOKEN>` (evaluates user entitlements)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  [
    {
      "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
      "isbn13": "9780134494166",
      "title": "Clean Architecture",
      "subtitle": "A Craftsman's Guide to Software Structure and Design",
      "authors": [
        { "authorId": "a1b2c3d4-...", "name": "Robert C. Martin", "role": "AUTHOR" }
      ],
      "primaryCategory": { "categoryId": "cat_02", "name": "Software Architecture" },
      "coverImageUrl": "https://cdn.bookcorner.com/covers/clean-arch.jpg",
      "averageRating": 4.7,
      "reviewCount": 384,
      "availableFormats": [
        {
          "formatId": "fmt_pb_01",
          "formatType": "PAPERBACK",
          "sku": "SKU-CA-PB",
          "price": { "amount": 3499, "currency": "USD" },
          "inStock": true
        },
        {
          "formatId": "fmt_hc_02",
          "formatType": "HARDCOVER",
          "sku": "SKU-CA-HC",
          "price": { "amount": 4999, "currency": "USD" },
          "inStock": true
        }
      ]
    }
  ]
  ```
- **Validation Rules**: `minRating` between 1 and 5. Page size $\le 100$.
- **Error Codes**: `400 INVALID_FILTER_PARAMS`.

---

#### API 6.2: Get Book Detail Page
- **Resource Name**: Comprehensive Book Details
- **HTTP Method**: `GET`
- **URI**: `/api/v1/books/{bookId}`
- **Headers**: Optional `Authorization: Bearer <TOKEN>` (unlocks member pricing/entitlements)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
    "isbn13": "9780134494166",
    "isbn10": "0134494164",
    "title": "Clean Architecture",
    "subtitle": "A Craftsman's Guide to Software Structure and Design",
    "publisher": { "publisherId": "p1a2...", "name": "Prentice Hall" },
    "authors": [
      { "authorId": "a1b2...", "name": "Robert C. Martin", "role": "AUTHOR" }
    ],
    "synopsis": "By applying universal rules of software architecture, you can dramatically improve developer productivity...",
    "publicationDate": "2017-09-20",
    "edition": "1st Edition",
    "pageCount": 432,
    "language": "en",
    "coverImageUrl": "https://cdn.bookcorner.com/covers/clean-arch.jpg",
    "ratingSummary": {
      "averageRating": 4.7,
      "totalReviews": 384,
      "ratingBreakdown": { "5": 290, "4": 70, "3": 15, "2": 5, "1": 4 }
    },
    "formats": [
      {
        "formatId": "fmt_pb_01",
        "formatType": "PAPERBACK",
        "sku": "SKU-CA-PB",
        "price": { "amount": 3499, "currency": "USD" },
        "stockQuantity": 84,
        "weightGrams": 680,
        "dimensionsMm": { "length": 230, "width": 178, "thickness": 25 }
      }
    ],
    "merchandising": {
      "upSellItem": {
        "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
        "formatType": "HARDCOVER",
        "title": "Deluxe Collector's Hardcover",
        "price": { "amount": 4999, "currency": "USD" },
        "priceDifference": 1500
      },
      "crossSellItems": [
        {
          "bookId": "b9c8d7e6-...",
          "title": "Clean Code",
          "coverImageUrl": "https://cdn.bookcorner.com/covers/clean-code.jpg",
          "bundleDiscountPercentage": 10.00
        }
      ]
    }
  }
  ```
- **Error Codes**: `404 BOOK_NOT_FOUND`, `403 CATALOG_ENTITLEMENT_RESTRICTED`.

---

### Domain 7: Search

#### API 7.1: Multi-Faceted Full-Text Search
- **Resource Name**: Search Engine Query
- **HTTP Method**: `GET`
- **URI**: `/api/v1/search`
- **Query Parameters**:
  - `q`: Search keyword (title, author, ISBN, or synopsis text)
  - `categoryId`: Facet filter
  - `format`: `PAPERBACK`, `HARDCOVER`, `EBOOK`
  - `priceRange`: e.g. `1000-5000` (in cents)
  - `page`: 1, `size`: 20
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "query": "architecture",
    "totalMatches": 48,
    "facets": {
      "categories": [
        { "categoryId": "cat_02", "name": "Software Architecture", "count": 32 },
        { "categoryId": "cat_05", "name": "System Design", "count": 16 }
      ],
      "formats": [
        { "formatType": "PAPERBACK", "count": 48 },
        { "formatType": "HARDCOVER", "count": 22 },
        { "formatType": "EBOOK", "count": 41 }
      ],
      "priceRanges": [
        { "label": "Under $20", "count": 12 },
        { "label": "$20 - $50", "count": 30 },
        { "label": "Over $50", "count": 6 }
      ]
    },
    "results": [
      {
        "bookId": "b1c2d3e4-f5a6-...",
        "title": "Clean Architecture",
        "authorNames": ["Robert C. Martin"],
        "matchHighlight": "A Craftsman's Guide to Software <em>Architecture</em>",
        "startingPrice": { "amount": 3499, "currency": "USD" },
        "averageRating": 4.7
      }
    ]
  }
  ```
- **Validation Rules**: `q` must contain $\ge 2$ characters. Max length 200 chars.
- **Error Codes**: `400 SEARCH_QUERY_TOO_SHORT`.

---

#### API 7.2: Search Auto-Complete & Typeahead Suggestions
- **Resource Name**: Search Typeahead
- **HTTP Method**: `GET`
- **URI**: `/api/v1/search/suggestions`
- **Query Parameters**: `prefix` (minimum 2 characters), `limit` (default 5)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  [
    { "type": "BOOK", "id": "b1c2...", "text": "Clean Architecture" },
    { "type": "BOOK", "id": "b2c3...", "text": "Clean Code" },
    { "type": "AUTHOR", "id": "a1b2...", "text": "Robert C. Martin" },
    { "type": "CATEGORY", "id": "cat_02", "text": "Software Architecture" }
  ]
  ```
- **Error Codes**: `400 INVALID_PREFIX_LENGTH`.

---

### Domain 8: Wishlist

#### API 8.1: Get User Wishlist
- **Resource Name**: Customer Wishlist
- **HTTP Method**: `GET`
- **URI**: `/api/v1/wishlists`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "wishlistId": "w1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "name": "My Wishlist",
    "totalItems": 2,
    "items": [
      {
        "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
        "title": "Clean Architecture",
        "authorName": "Robert C. Martin",
        "coverImageUrl": "https://cdn.bookcorner.com/covers/clean-arch.jpg",
        "currentPrice": { "amount": 3499, "currency": "USD" },
        "desiredPriceAlert": 3000,
        "addedAt": "2026-09-10T14:30:00Z"
      }
    ]
  }
  ```
- **Error Codes**: `401 UNAUTHORIZED`.

---

#### API 8.2: Add Book to Wishlist
- **Resource Name**: Wishlist Item Ingestion
- **HTTP Method**: `POST`
- **URI**: `/api/v1/wishlists/items`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**:
  ```json
  {
    "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
    "desiredPriceAlert": 3000
  }
  ```
- **Response DTO (`201 Created`)**:
  ```json
  {
    "wishlistItemId": "wi_8a7b6c5d-4e3f-...",
    "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
    "desiredPriceAlert": 3000,
    "addedAt": "2026-09-18T10:30:00Z"
  }
  ```
- **Validation Rules**: `bookId` must exist in catalog. Wishlist capacity $\le 200$ items. Duplicates rejected.
- **Error Codes**: `404 BOOK_NOT_FOUND`, `409 WISHLIST_ITEM_ALREADY_EXISTS`, `422 WISHLIST_CAPACITY_EXCEEDED`.

---

#### API 8.3: Remove Item from Wishlist
- **Resource Name**: Wishlist Item Remover
- **HTTP Method**: `DELETE`
- **URI**: `/api/v1/wishlists/items/{bookId}`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**: None
- **Response DTO (`204 No Content`)**: None
- **Error Codes**: `404 WISHLIST_ITEM_NOT_FOUND`, `401 UNAUTHORIZED`.

---

### Domain 9: Cart (Basket Lifecycle)

#### API 9.1: Get Active Cart
- **Resource Name**: Shopping Cart
- **HTTP Method**: `GET`
- **URI**: `/api/v1/cart`
- **Headers**:
  - `Authorization: Bearer <TOKEN>` (if logged in) OR
  - `X-Guest-Session-Token: <GST_TOKEN>` (if guest)
  - `X-Store-Code: BK_MAIN_ONLINE`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "storeCode": "BK_MAIN_ONLINE",
    "currency": "USD",
    "appliedCouponCode": "FALLREADS10",
    "subtotalAmount": 6998,
    "discountAmount": 700,
    "estimatedTaxAmount": 520,
    "totalPayableAmount": 6818,
    "itemCount": 2,
    "items": [
      {
        "formatId": "fmt_pb_01",
        "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
        "title": "Clean Architecture",
        "formatType": "PAPERBACK",
        "sku": "SKU-CA-PB",
        "unitPrice": 3499,
        "quantity": 2,
        "lineTotal": 6998,
        "inStock": true,
        "maxStockAllowed": 10
      }
    ]
  }
  ```
- **Error Codes**: `400 CART_CONTEXT_MISSING`, `404 CART_NOT_FOUND`.

---

#### API 9.2: Add Item to Cart
- **Resource Name**: Cart Item Ingestion
- **HTTP Method**: `POST`
- **URI**: `/api/v1/cart/items`
- **Headers**: `Authorization: Bearer <TOKEN>` OR `X-Guest-Session-Token: <GST_TOKEN>`
- **Request DTO**:
  ```json
  {
    "formatId": "fmt_pb_01",
    "quantity": 2
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "itemCount": 2,
    "subtotalAmount": 6998,
    "message": "Item added to cart."
  }
  ```
- **Validation Rules**: `quantity` must be between 1 and 10. `formatId` must be in stock.
- **Error Codes**: `400 VALIDATION_FAILED`, `404 SKU_NOT_FOUND`, `409 INSUFFICIENT_STOCK`, `422 CART_ITEM_LIMIT_EXCEEDED`.

---

#### API 9.3: Update Cart Item Quantity
- **Resource Name**: Cart Item Quantity Mutator
- **HTTP Method**: `PATCH`
- **URI**: `/api/v1/cart/items/{formatId}`
- **Headers**: `Authorization: Bearer <TOKEN>` OR `X-Guest-Session-Token: <GST_TOKEN>`
- **Request DTO**:
  ```json
  {
    "quantity": 3
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "formatId": "fmt_pb_01",
    "quantity": 3,
    "lineTotal": 10497,
    "subtotalAmount": 10497
  }
  ```
- **Validation Rules**: `quantity` must be between 1 and 10. (Setting to 0 removes the item).
- **Error Codes**: `404 ITEM_NOT_IN_CART`, `409 INSUFFICIENT_STOCK`.

---

#### API 9.4: Remove Item from Cart
- **Resource Name**: Cart Item Deleter
- **HTTP Method**: `DELETE`
- **URI**: `/api/v1/cart/items/{formatId}`
- **Headers**: `Authorization: Bearer <TOKEN>` OR `X-Guest-Session-Token: <GST_TOKEN>`
- **Request DTO**: None
- **Response DTO (`204 No Content`)**: None
- **Error Codes**: `404 ITEM_NOT_IN_CART`.

---

#### API 9.5: Merge Guest Cart into User Cart
- **Resource Name**: Cart Synchronizer
- **HTTP Method**: `POST`
- **URI**: `/api/v1/cart/merge`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**:
  ```json
  {
    "guestSessionToken": "gst_e7b92f8012d449fa894c25f77890a1bc"
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "totalMergedItems": 3,
    "subtotalAmount": 10497
  }
  ```
- **Validation Rules**: Deduplicates matching SKUs; caps merged quantities at 10 units per SKU.
- **Error Codes**: `404 GUEST_CART_NOT_FOUND`, `401 UNAUTHORIZED`.

---

### Domain 10: Orders (Checkout, Lifecycle & RMA)

#### API 10.1: Submit Checkout & Place Order
- **Resource Name**: Checkout Saga Initiator
- **HTTP Method**: `POST`
- **URI**: `/api/v1/orders/checkout`
- **Headers**:
  - `Authorization: Bearer <TOKEN>`
  - `Idempotency-Key: <UUIDv4>`
- **Request DTO**:
  ```json
  {
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "shippingAddressId": "c4b1e5a8-44d2-43f1-b9a3-8349281a98c1",
    "billingAddressId": "c4b1e5a8-44d2-43f1-b9a3-8349281a98c1",
    "shippingCarrierCode": "FEDEX",
    "shippingServiceLevel": "STANDARD",
    "paymentIntent": {
      "tenderSplits": [
        { "tenderType": "DIGITAL_WALLET", "amount": 2500 },
        { "tenderType": "CREDIT_CARD", "amount": 4318, "gatewayPaymentMethodToken": "pm_tok_1O8q9r..." }
      ]
    }
  }
  ```
- **Response DTO (`201 Created`)**:
  ```json
  {
    "orderNumber": "BK-2026-98124",
    "orderStatus": "CONFIRMED",
    "totalAmount": 6818,
    "currency": "USD",
    "placedAt": "2026-09-18T10:35:00Z",
    "estimatedDeliveryWindow": {
      "minDate": "2026-09-22T18:00:00Z",
      "maxDate": "2026-09-24T18:00:00Z"
    },
    "paymentStatus": "CAPTURED",
    "lineItems": [
      {
        "sku": "SKU-CA-PB",
        "title": "Clean Architecture",
        "quantity": 2,
        "lineTotal": 6818
      }
    ]
  }
  ```
- **Validation Rules**:
  - `Idempotency-Key` mandatory.
  - Sum of tender amounts must equal net order total.
  - Cart cannot be empty.
  - Inventory must be reservable.
- **Error Codes**: `400 VALIDATION_FAILED`, `409 INVENTORY_OUT_OF_STOCK`, `402 PAYMENT_DECLINED`, `409 IDEMPOTENCY_KEY_DUPLICATE`.

---

#### API 10.2: List Customer Order History
- **Resource Name**: Customer Order History
- **HTTP Method**: `GET`
- **URI**: `/api/v1/orders`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Query Parameters**: `page` (default 1), `size` (default 10), `status` (optional filter)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  [
    {
      "orderNumber": "BK-2026-98124",
      "orderStatus": "CONFIRMED",
      "totalAmount": 6818,
      "currency": "USD",
      "totalItems": 2,
      "placedAt": "2026-09-18T10:35:00Z",
      "trackingNumber": "TRK_FEDEX_928192847",
      "carrier": "FEDEX",
      "isReturnEligible": false
    }
  ]
  ```
- **Error Codes**: `401 UNAUTHORIZED`.

---

#### API 10.3: Get Order Details & Tracking
- **Resource Name**: Order Detail View
- **HTTP Method**: `GET`
- **URI**: `/api/v1/orders/{orderNumber}`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "orderNumber": "BK-2026-98124",
    "orderStatus": "CONFIRMED",
    "pricing": {
      "subtotalAmount": 6998,
      "discountAmount": 700,
      "shippingAmount": 0,
      "taxAmount": 520,
      "totalAmount": 6818,
      "currency": "USD"
    },
    "shippingDetails": {
      "carrier": "FEDEX",
      "serviceLevel": "STANDARD",
      "trackingNumber": "TRK_FEDEX_928192847",
      "status": "MANIFESTED",
      "recipientAddress": {
        "recipientName": "John Doe",
        "streetLine1": "123 Market St",
        "city": "San Francisco",
        "state": "CA",
        "postalCode": "94103",
        "country": "US"
      }
    },
    "paymentDetails": {
      "transactionId": "tx_99281...",
      "status": "CAPTURED",
      "tenders": [
        { "tenderType": "DIGITAL_WALLET", "amount": 2500 },
        { "tenderType": "CREDIT_CARD", "amount": 4318, "reference": "*4242" }
      ]
    },
    "lineItems": [
      {
        "lineItemId": "li_001...",
        "sku": "SKU-CA-PB",
        "title": "Clean Architecture",
        "quantity": 2,
        "unitPrice": 3499,
        "lineTotal": 6818
      }
    ]
  }
  ```
- **Error Codes**: `404 ORDER_NOT_FOUND`, `401 UNAUTHORIZED`.

---

#### API 10.4: Customer Order Cancellation
- **Resource Name**: Order Cancellation
- **HTTP Method**: `POST`
- **URI**: `/api/v1/orders/{orderNumber}/cancel`
- **Headers**:
  - `Authorization: Bearer <TOKEN>`
  - `Idempotency-Key: <UUIDv4>`
- **Request DTO**:
  ```json
  {
    "reason": "Accidental duplicate order"
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "orderNumber": "BK-2026-98124",
    "orderStatus": "CANCELLED",
    "refundStatus": "INITIATED",
    "refundAmount": 6818,
    "cancelledAt": "2026-09-18T10:45:00Z"
  }
  ```
- **Validation Rules**:
  - Order must belong to authenticated user.
  - Order status must be `CONFIRMED` or `PROCESSING` (prior to dispatch).
  - Store policy cancellation grace period must not have elapsed.
- **Error Codes**: `400 CANCELLATION_POLICY_WINDOW_EXPIRED`, `409 ORDER_ALREADY_DISPATCHED`.

---

#### API 10.5: Request Order Return (RMA)
- **Resource Name**: Return Merchandise Authorization (RMA)
- **HTTP Method**: `POST`
- **URI**: `/api/v1/orders/{orderNumber}/returns`
- **Headers**:
  - `Authorization: Bearer <TOKEN>`
  - `Idempotency-Key: <UUIDv4>`
- **Request DTO**:
  ```json
  {
    "reasonCode": "DAMAGED_ITEM",
    "customerRemarks": "Book spine cracked upon arrival.",
    "returnItems": [
      {
        "orderLineItemId": "li_001...",
        "quantity": 1
      }
    ]
  }
  ```
- **Response DTO (`201 Created`)**:
  ```json
  {
    "rmaNumber": "RMA-2026-443",
    "rmaStatus": "APPROVED",
    "returnCarrier": "FEDEX",
    "returnTrackingNumber": "RET_FEDEX_19284729",
    "prepaidReturnLabelUrl": "https://cdn.bookcorner.com/labels/rma-2026-443.pdf",
    "estimatedRefundAmount": 3409,
    "returnInstructions": "Print the prepaid label and attach to original packaging. Drop off at any FedEx location."
  }
  ```
- **Validation Rules**:
  - Order must be in `DELIVERED` status.
  - Delivery date must be within store policy return window (e.g., 30 days).
  - Return quantity must not exceed purchased quantity.
- **Error Codes**: `400 RETURN_POLICY_WINDOW_EXPIRED`, `400 INVALID_RETURN_QUANTITY`, `404 ORDER_NOT_FOUND`.

---

### Domain 11: Payments (Gateways, Wallets & Refunds)

#### API 11.1: Create Payment Intent
- **Resource Name**: Payment Intent Coordinator
- **HTTP Method**: `POST`
- **URI**: `/api/v1/payments/intent`
- **Headers**:
  - `Authorization: Bearer <TOKEN>`
  - `Idempotency-Key: <UUIDv4>`
- **Request DTO**:
  ```json
  {
    "orderNumber": "BK-2026-98124",
    "useWalletBalance": true,
    "giftCardCode": "GIFT-2026-READ"
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "paymentTransactionId": "tx_99281...",
    "totalOrderAmount": 6818,
    "walletDeduction": 2500,
    "giftCardDeduction": 1000,
    "remainingPayableViaGateway": 3318,
    "gatewayClientSecret": "pi_3N9x..._secret_...",
    "gatewayProvider": "STRIPE",
    "currency": "USD"
  }
  ```
- **Validation Rules**: Validates wallet balance; places temporary hold on wallet funds.
- **Error Codes**: `404 ORDER_NOT_FOUND`, `400 INSUFFICIENT_FUNDS_FOR_INTENT`.

---

#### API 11.2: Ingest Payment Gateway Webhook
- **Resource Name**: Payment Gateway Webhook Reconciler
- **HTTP Method**: `POST`
- **URI**: `/api/v1/payments/webhooks/{provider}`
- **Headers**: `Stripe-Signature: t=16145...,v1=...`
- **Request DTO**: Raw Gateway Payload (JSON/Binary)
- **Response DTO (`200 OK`)**:
  ```json
  {
    "received": true,
    "eventId": "evt_1N9x..."
  }
  ```
- **Validation Rules**: Cryptographic HMAC signature check against webhook secret. Idempotent deduplication on `eventId`.
- **Error Codes**: `400 INVALID_WEBHOOK_SIGNATURE`, `404 UNKNOWN_GATEWAY_PROVIDER`.

---

#### API 11.3: Get Customer Wallet & Ledger
- **Resource Name**: Customer Digital Wallet
- **HTTP Method**: `GET`
- **URI**: `/api/v1/payments/wallet`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "walletId": "wal_1a2b3c...",
    "currentBalance": 2500,
    "heldBalance": 0,
    "currency": "USD",
    "recentLedger": [
      {
        "entryId": "led_01...",
        "entryType": "CREDIT_TOPUP",
        "amount": 5000,
        "balanceAfter": 5000,
        "createdAt": "2026-09-01T12:00:00Z"
      },
      {
        "entryId": "led_02...",
        "entryType": "DEBIT_PURCHASE",
        "amount": -2500,
        "balanceAfter": 2500,
        "referenceOrderId": "BK-2026-98124",
        "createdAt": "2026-09-18T10:35:00Z"
      }
    ]
  }
  ```
- **Error Codes**: `401 UNAUTHORIZED`.

---

### Domain 12: Shipping (Rating & Consignments)

#### API 12.1: Calculate Shipping Rates & ETA
- **Resource Name**: Real-Time Shipping Rate Calculator
- **HTTP Method**: `POST`
- **URI**: `/api/v1/shipping/rates`
- **Headers**: `X-Store-Code: BK_MAIN_ONLINE`
- **Request DTO**:
  ```json
  {
    "destinationPostalCode": "94103",
    "countryCode": "US",
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c"
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  [
    {
      "carrierCode": "FEDEX",
      "serviceLevel": "STANDARD",
      "rateAmount": 0,
      "currency": "USD",
      "isFreeShippingApplied": true,
      "estimatedDeliveryMin": "2026-09-22T18:00:00Z",
      "estimatedDeliveryMax": "2026-09-24T18:00:00Z",
      "transitDays": "3-5 business days"
    },
    {
      "carrierCode": "FEDEX",
      "serviceLevel": "EXPRESS",
      "rateAmount": 999,
      "currency": "USD",
      "isFreeShippingApplied": false,
      "estimatedDeliveryMin": "2026-09-20T18:00:00Z",
      "estimatedDeliveryMax": "2026-09-21T18:00:00Z",
      "transitDays": "1-2 business days"
    }
  ]
  ```
- **Validation Rules**: `postalCode` must be non-empty and valid for `countryCode`. Cart must exist.
- **Error Codes**: `400 INVALID_POSTAL_CODE`, `404 CART_NOT_FOUND`.

---

#### API 12.2: Track Shipment Consignment
- **Resource Name**: Consignment Tracking Timeline
- **HTTP Method**: `GET`
- **URI**: `/api/v1/shipping/consignments/{trackingNumber}`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "trackingNumber": "TRK_FEDEX_928192847",
    "carrierCode": "FEDEX",
    "serviceLevel": "STANDARD",
    "consignmentStatus": "IN_TRANSIT",
    "estimatedDelivery": "2026-09-23T18:00:00Z",
    "milestones": [
      {
        "status": "PICKED_UP",
        "location": "Memphis, TN",
        "timestamp": "2026-09-19T08:00:00Z",
        "remarks": "Package received from merchant facility"
      },
      {
        "status": "DEPARTED_FACILITY",
        "location": "Oakland, CA",
        "timestamp": "2026-09-20T14:30:00Z",
        "remarks": "In transit to local delivery hub"
      }
    ]
  }
  ```
- **Error Codes**: `404 CONSIGNMENT_NOT_FOUND`.

---

### Domain 13: Reviews (Ratings & Social Proof)

#### API 13.1: List Book Reviews
- **Resource Name**: Book Reviews & Aggregate Score
- **HTTP Method**: `GET`
- **URI**: `/api/v1/books/{bookId}/reviews`
- **Query Parameters**: `page` (default 1), `size` (default 10), `sort` (`HELPFUL`, `RECENT`, `RATING_HIGH`, `RATING_LOW`)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
    "averageRating": 4.7,
    "totalReviews": 384,
    "reviews": [
      {
        "reviewId": "rev_99182...",
        "authorName": "Alice W.",
        "rating": 5,
        "reviewTitle": "Essential reading for engineers",
        "reviewBody": "A masterclass in separating concerns and building resilient systems.",
        "isVerifiedPurchase": true,
        "helpfulVotes": 42,
        "createdAt": "2026-08-12T11:00:00Z"
      }
    ]
  }
  ```
- **Error Codes**: `404 BOOK_NOT_FOUND`.

---

#### API 13.2: Submit Book Review
- **Resource Name**: Review Submission
- **HTTP Method**: `POST`
- **URI**: `/api/v1/books/{bookId}/reviews`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**:
  ```json
  {
    "rating": 5,
    "reviewTitle": "Must read for architecture practitioners",
    "reviewBody": "Practical guidelines that remain relevant across any stack."
  }
  ```
- **Response DTO (`201 Created`)**:
  ```json
  {
    "reviewId": "rev_99182...",
    "bookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
    "rating": 5,
    "isVerifiedPurchase": true,
    "moderationStatus": "APPROVED",
    "createdAt": "2026-09-18T10:50:00Z"
  }
  ```
- **Validation Rules**:
  - `rating`: Integer strictly between 1 and 5.
  - `reviewTitle`: Optional, max 200 chars.
  - User can submit only 1 review per book.
  - `isVerifiedPurchase` automatically resolved from user order history.
- **Error Codes**: `400 VALIDATION_FAILED`, `409 REVIEW_ALREADY_SUBMITTED`, `404 BOOK_NOT_FOUND`.

---

#### API 13.3: Vote Review Helpful
- **Resource Name**: Review Helpful Voter
- **HTTP Method**: `POST`
- **URI**: `/api/v1/reviews/{reviewId}/votes`
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request DTO**:
  ```json
  {
    "isHelpful": true
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "reviewId": "rev_99182...",
    "helpfulVotesCount": 43,
    "userVote": true
  }
  ```
- **Validation Rules**: User cannot vote on their own review; toggles vote if clicked again.
- **Error Codes**: `404 REVIEW_NOT_FOUND`, `400 CANNOT_VOTE_OWN_REVIEW`.

---

### Domain 14: Coupons (Promotions)

#### API 14.1: Validate Coupon Code
- **Resource Name**: Coupon Validator
- **HTTP Method**: `POST`
- **URI**: `/api/v1/coupons/validate`
- **Headers**: `X-Store-Code: BK_MAIN_ONLINE`
- **Request DTO**:
  ```json
  {
    "couponCode": "FALLREADS10",
    "orderSubtotalAmount": 6998
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "couponCode": "FALLREADS10",
    "isValid": true,
    "discountType": "PERCENTAGE",
    "discountValue": 1000,
    "calculatedDiscountAmount": 700,
    "minOrderAmount": 3000,
    "message": "10% discount applied."
  }
  ```
- **Validation Rules**: `couponCode` uppercase trimmed. Checks expiry date, usage limit, and minimum order threshold.
- **Error Codes**: `400 COUPON_INVALID_OR_EXPIRED`, `400 COUPON_MIN_ORDER_NOT_MET`, `400 COUPON_LIMIT_EXHAUSTED`.

---

#### API 14.2: Apply Coupon to Cart
- **Resource Name**: Cart Coupon Application
- **HTTP Method**: `POST`
- **URI**: `/api/v1/cart/coupon`
- **Headers**: `Authorization: Bearer <TOKEN>` OR `X-Guest-Session-Token: <GST_TOKEN>`
- **Request DTO**:
  ```json
  {
    "couponCode": "FALLREADS10"
  }
  ```
- **Response DTO (`200 OK`)**:
  ```json
  {
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "appliedCouponCode": "FALLREADS10",
    "discountAmount": 700,
    "newTotalPayable": 6818
  }
  ```
- **Error Codes**: `400 COUPON_NOT_APPLICABLE`, `404 CART_NOT_FOUND`.

---

#### API 14.3: Remove Coupon from Cart
- **Resource Name**: Cart Coupon Remover
- **HTTP Method**: `DELETE`
- **URI**: `/api/v1/cart/coupon`
- **Headers**: `Authorization: Bearer <TOKEN>` OR `X-Guest-Session-Token: <GST_TOKEN>`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "cartId": "c1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c",
    "appliedCouponCode": null,
    "discountAmount": 0,
    "newTotalPayable": 7518
  }
  ```
- **Error Codes**: `404 CART_NOT_FOUND`.

---

### Domain 15: Recommendations (Personalization, Up-Sell & Cross-Sell)

#### API 15.1: Get Personalized Home Recommendations
- **Resource Name**: Home Personalization Carousel
- **HTTP Method**: `GET`
- **URI**: `/api/v1/recommendations/home`
- **Headers**:
  - `Authorization: Bearer <TOKEN>` (personalized via order history) OR
  - Unauthenticated (falls back to trending bestsellers)
  - `X-Store-Code: BK_MAIN_ONLINE`
- **Query Parameters**: `limit` (default 10)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "strategy": "COLLABORATIVE_FILTERING_ORDER_HISTORY",
    "recommendations": [
      {
        "bookId": "b7c8d9e0-...",
        "title": "Designing Data-Intensive Applications",
        "authorName": "Martin Kleppmann",
        "coverImageUrl": "https://cdn.bookcorner.com/covers/ddia.jpg",
        "startingPrice": { "amount": 3999, "currency": "USD" },
        "averageRating": 4.9,
        "recommendationReason": "Because you purchased Clean Architecture"
      }
    ]
  }
  ```
- **Performance**: Precomputed in Redis; P95 $< 80\text{ ms}$.
- **Error Codes**: `500 RECOMMENDATION_FALLBACK_FAILED`.

---

#### API 15.2: Get Up-Sell Recommendations
- **Resource Name**: Product Up-Sell Prompt
- **HTTP Method**: `GET`
- **URI**: `/api/v1/recommendations/books/{bookId}/upsell`
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "sourceBookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
    "hasUpSell": true,
    "upSellOption": {
      "targetBookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
      "formatType": "HARDCOVER",
      "editionTitle": "Deluxe Illustrated Hardcover Edition",
      "basePrice": { "amount": 4999, "currency": "USD" },
      "priceDelta": { "amount": 1500, "currency": "USD" },
      "benefits": ["Acid-free archival paper", "Author signed frontispiece", "Slipcase included"]
    }
  }
  ```
- **Error Codes**: `404 BOOK_NOT_FOUND`.

---

#### API 15.3: Get Cross-Sell Companion Recommendations
- **Resource Name**: Cross-Sell & Co-Purchase Bundles
- **HTTP Method**: `GET`
- **URI**: `/api/v1/recommendations/books/{bookId}/cross-sell`
- **Query Parameters**: `limit` (default 3)
- **Request DTO**: None
- **Response DTO (`200 OK`)**:
  ```json
  {
    "sourceBookId": "b1c2d3e4-f5a6-4b2c-9d3e-4f5a6b7c8d9e",
    "bundleDiscountPercentage": 10.00,
    "companionTitles": [
      {
        "bookId": "b2c3d4e5-...",
        "title": "Clean Code",
        "authorName": "Robert C. Martin",
        "formatType": "PAPERBACK",
        "individualPrice": 3299,
        "bundlePrice": 2969,
        "coverImageUrl": "https://cdn.bookcorner.com/covers/clean-code.jpg"
      }
    ]
  }
  ```
- **Error Codes**: `404 BOOK_NOT_FOUND`.

---

## 3. Master HTTP Error Code Catalog

| HTTP Status | Domain Error Code | Description / Triggering Scenario |
| :--- | :--- | :--- |
| **`400 Bad Request`** | `VALIDATION_FAILED` | Request payload fails structural validation (e.g., regex, range, missing mandatory field). |
| **`400 Bad Request`** | `COUPON_MIN_ORDER_NOT_MET` | Cart subtotal does not satisfy the coupon's minimum spend constraint. |
| **`400 Bad Request`** | `RETURN_POLICY_WINDOW_EXPIRED` | RMA requested after the store's allowed return window (e.g., $>30$ days post-delivery). |
| **`401 Unauthorized`** | `AUTH_INVALID_CREDENTIALS` | Invalid email/password combination during login. |
| **`401 Unauthorized`** | `AUTH_TOKEN_EXPIRED` | Access token JWT has expired. |
| **`402 Payment Required`** | `PAYMENT_DECLINED` | External payment gateway declined charge (insufficient funds, fraud flag). |
| **`403 Forbidden`** | `CATALOG_ENTITLEMENT_RESTRICTED` | Book title requires specialized membership tier (e.g., academic license, VIP club). |
| **`404 Not Found`** | `BOOK_NOT_FOUND` | Specified book identifier or ISBN does not exist in the active catalog. |
| **`404 Not Found`** | `ORDER_NOT_FOUND` | Order number not found for the requesting customer. |
| **`409 Conflict`** | `AUTH_EMAIL_ALREADY_EXISTS` | Registration attempted with an email already bound to an active account. |
| **`409 Conflict`** | `CONCURRENCY_CONFLICT` | OCC update failed due to mismatched record version token (`version`). |
| **`409 Conflict`** | `INVENTORY_OUT_OF_STOCK` | Stock insufficient during cart addition or checkout order placement. |
| **`409 Conflict`** | `IDEMPOTENCY_KEY_DUPLICATE` | Mutating request re-submitted with an in-flight or completed idempotency token. |
| **`422 Unprocessable`** | `CART_ITEM_LIMIT_EXCEEDED` | Attempt to add more than 10 units of a single SKU to a shopping basket. |
| **`429 Too Many Requests`** | `RATE_LIMIT_EXCEEDED` | Request threshold exceeded on rate-limited endpoints (auth, search). |
| **`503 Service Unavailable`** | `GATEWAY_CIRCUIT_OPEN` | External dependency (Stripe, carrier API) temporarily unavailable; circuit breaker open. |

---
*End of REST API Resource Model Specification. Ready for transition to Phase 6: OpenAPI 3.1 Contract Generation & Automated Mock Server Setup.*
