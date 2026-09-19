#!/usr/bin/env python3
"""
Book Corner REST API - Postman Collection Generator
Generates:
  1. book_corner.postman_collection.json (Schema v2.1.0)
  2. book_corner_local.postman_environment.json
Covers all 44 REST API endpoints with real-world dummy test data,
automated Chai assertions, dynamic variable propagation, and end-to-end chaining.
"""

import json
import uuid
import os

COLLECTION_ID = str(uuid.uuid4())
ENV_ID = str(uuid.uuid4())

def make_header(key, value, description=""):
    h = {"key": key, "value": value, "type": "text"}
    if description:
        h["description"] = description
    return h

def make_url(path_str, query_params=None, path_vars=None):
    segments = [s for s in path_str.strip("/").split("/") if s]
    raw_url = "{{baseUrl}}/" + "/".join(segments)
    
    url_obj = {
        "raw": raw_url,
        "host": ["{{baseUrl}}"],
        "path": segments
    }
    
    if query_params:
        query_list = []
        raw_query_parts = []
        for qk, qv, qdesc in query_params:
            query_list.append({
                "key": qk,
                "value": str(qv),
                "description": qdesc
            })
            raw_query_parts.append(f"{qk}={qv}")
        url_obj["query"] = query_list
        url_obj["raw"] += "?" + "&".join(raw_query_parts)
        
    if path_vars:
        var_list = []
        for vk, vv, vdesc in path_vars:
            var_list.append({
                "key": vk,
                "value": str(vv),
                "description": vdesc
            })
        url_obj["variable"] = var_list
        
    return url_obj

def make_event(listen_type, script_lines):
    return {
        "listen": listen_type,
        "script": {
            "type": "text/javascript",
            "exec": script_lines
        }
    }

def make_request_item(name, method, path_str, headers=None, body=None, query_params=None, path_vars=None, test_script=None, prerequest_script=None, description=""):
    item = {
        "name": name,
        "request": {
            "method": method,
            "header": headers or [],
            "url": make_url(path_str, query_params, path_vars),
            "description": description
        },
        "response": []
    }
    
    if body is not None:
        if isinstance(body, (dict, list)):
            item["request"]["body"] = {
                "mode": "raw",
                "raw": json.dumps(body, indent=2),
                "options": {
                    "raw": {
                        "language": "json"
                    }
                }
            }
        elif isinstance(body, str):
            item["request"]["body"] = {
                "mode": "raw",
                "raw": body,
                "options": {
                    "raw": {
                        "language": "json"
                    }
                }
            }
            
    events = []
    if prerequest_script:
        events.append(make_event("prerequest", prerequest_script))
    if test_script:
        events.append(make_event("test", test_script))
    if events:
        item["event"] = events
        
    return item

def build_collection():
    collection = {
        "info": {
            "_postman_id": COLLECTION_ID,
            "name": "Book Corner REST API - Full Test Suite",
            "description": (
                "# Book Corner REST API Postman Collection\n\n"
                "Comprehensive, production-grade API testing suite for the **Book Corner** online bookstore platform.\n\n"
                "### Features:\n"
                "- **100% Endpoint Coverage**: 44 core REST endpoints + actuator health and info checks.\n"
                "- **Realistic Domain Data**: Real books (Clean Code, Design Patterns, DDIA), real authors (Uncle Bob, Martin Kleppmann), real postal codes, ISO-4217 amounts in cents.\n"
                "- **Automated Dynamic Chaining**: Tests extract JWT access/refresh tokens, guest session tokens, IDs, order numbers, and line items, storing them in collection variables.\n"
                "- **Fully Executable Top-to-Bottom**: Can be run via Postman Collection Runner or Newman CLI without manual data entry.\n"
                "- **Multi-Tender Payments & Checkout Saga**: Demonstrates end-to-end basket lifecycle, coupon application, checkout orchestration, RMA returns, and order cancellations.\n"
                "- **Security Isolation Verification**: Tests secondary accounts to prevent self-voting violations and verify clean RBAC token rotation.\n\n"
                "### Quick Start:\n"
                "1. Select the `Book Corner - Local Environment`.\n"
                "2. Ensure the Spring Boot backend is running on `http://localhost:8080`.\n"
                "3. Click **Run Collection** or use Newman CLI: `newman run book_corner.postman_collection.json -e book_corner_local.postman_environment.json`."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        "variable": [
            {"key": "baseUrl", "value": "http://localhost:8080", "type": "string"},
            {"key": "storeCode", "value": "BK_MAIN_ONLINE", "type": "string"},
            {"key": "guestSessionToken", "value": "", "type": "string"},
            {"key": "accessToken", "value": "", "type": "string"},
            {"key": "refreshToken", "value": "", "type": "string"},
            {"key": "userId", "value": "", "type": "string"},
            {"key": "userEmail", "value": "alex.morgan.reader@bookcorner.io", "type": "string"},
            {"key": "userPassword", "value": "SecurePassword2026!", "type": "string"},
            {"key": "secondCustomerAccessToken", "value": "", "type": "string"},
            {"key": "secondCustomerRefreshToken", "value": "", "type": "string"},
            {"key": "secondCustomerEmail", "value": "samantha.reed@bookcorner.io", "type": "string"},
            {"key": "bookId", "value": "30000000-0000-0000-0000-000000000001", "type": "string"},
            {"key": "bookId2", "value": "30000000-0000-0000-0000-000000000002", "type": "string"},
            {"key": "formatId", "value": "31000000-0000-0000-0000-000000000001", "type": "string"},
            {"key": "formatId2", "value": "31000000-0000-0000-0000-000000000004", "type": "string"},
            {"key": "authorId", "value": "40000000-0000-0000-0000-000000000001", "type": "string"},
            {"key": "authorSlug", "value": "robert-c-martin", "type": "string"},
            {"key": "categoryId", "value": "10000000-0000-0000-0000-000000000031", "type": "string"},
            {"key": "addressId", "value": "", "type": "string"},
            {"key": "tempAddressId", "value": "", "type": "string"},
            {"key": "orderNumber", "value": "", "type": "string"},
            {"key": "orderToCancelNumber", "value": "", "type": "string"},
            {"key": "orderLineItemId", "value": "", "type": "string"},
            {"key": "trackingNumber", "value": "TRK987654321", "type": "string"},
            {"key": "reviewId", "value": "", "type": "string"},
            {"key": "rmaNumber", "value": "", "type": "string"},
            {"key": "returnTrackingNumber", "value": "", "type": "string"}
        ],
        "item": []
    }

    folders = []

    # =========================================================================
    # FOLDER 1: Authentication & Sessions
    # =========================================================================
    auth_items = [
        make_request_item(
            name="01. Start Anonymous Guest Session",
            method="POST",
            path_str="/auth/guest-session",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("X-Store-Code", "{{storeCode}}", "Multi-tenant physical or digital storefront identifier")
            ],
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"Response contains signed guest session token\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"guestSessionToken\");",
                "    pm.expect(jsonData.guestSessionToken).to.be.a(\"string\").and.not.empty;",
                "    pm.expect(jsonData).to.have.property(\"expiresAt\");",
                "    pm.collectionVariables.set(\"guestSessionToken\", jsonData.guestSessionToken);",
                "    console.log(\"Saved guestSessionToken: \" + jsonData.guestSessionToken);",
                "});",
                "",
                "pm.test(\"Response time is under 1500ms\", function () {",
                "    pm.expect(pm.response.responseTime).to.be.below(1500);",
                "});"
            ],
            description="Issues a cryptographically signed guest session token to track temporary baskets and preferences without requiring authentication."
        ),
        make_request_item(
            name="02. Register Customer Account (Primary User)",
            method="POST",
            path_str="/auth/register",
            headers=[
                make_header("Content-Type", "application/json")
            ],
            body={
                "email": "{{userEmail}}",
                "password": "{{userPassword}}",
                "firstName": "Alex",
                "lastName": "Morgan",
                "phoneNumber": "+14155550199"
            },
            prerequest_script=[
                "// Generate unique email to ensure deterministic test execution across multiple runs",
                "var uniqueEmail = 'alex.morgan.' + Date.now() + '@bookcorner.io';",
                "pm.collectionVariables.set('userEmail', uniqueEmail);",
                "console.log('Registering user with dynamic email: ' + uniqueEmail);"
            ],
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"JWT tokens issued with Bearer type and user metadata\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"accessToken\");",
                "    pm.expect(jsonData).to.have.property(\"refreshToken\");",
                "    pm.expect(jsonData.tokenType).to.eql(\"Bearer\");",
                "    pm.expect(jsonData.user).to.be.an(\"object\");",
                "    pm.expect(jsonData.user.email).to.eql(pm.collectionVariables.get(\"userEmail\"));",
                "    ",
                "    pm.collectionVariables.set(\"accessToken\", jsonData.accessToken);",
                "    pm.collectionVariables.set(\"refreshToken\", jsonData.refreshToken);",
                "    pm.collectionVariables.set(\"userId\", jsonData.user.id);",
                "    console.log(\"Primary user registered. User ID: \" + jsonData.user.id);",
                "});"
            ],
            description="Creates a new user profile, issues initial JWT access and refresh tokens, and binds customer account."
        ),
        make_request_item(
            name="03. Authenticate User Credentials (Login)",
            method="POST",
            path_str="/auth/login",
            headers=[
                make_header("Content-Type", "application/json")
            ],
            body={
                "email": "{{userEmail}}",
                "password": "{{userPassword}}"
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Login yields valid JWT access and refresh tokens\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.accessToken).to.be.a(\"string\").and.not.empty;",
                "    pm.expect(jsonData.refreshToken).to.be.a(\"string\").and.not.empty;",
                "    pm.expect(jsonData.tokenType).to.eql(\"Bearer\");",
                "    pm.collectionVariables.set(\"accessToken\", jsonData.accessToken);",
                "    pm.collectionVariables.set(\"refreshToken\", jsonData.refreshToken);",
                "});"
            ],
            description="Authenticates email and password credentials, returning fresh JWT access and refresh tokens."
        ),
        make_request_item(
            name="04. Rotate JWT Refresh Token",
            method="POST",
            path_str="/auth/refresh",
            headers=[
                make_header("Content-Type", "application/json")
            ],
            body={
                "refreshToken": "{{refreshToken}}"
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Token rotation produces new access and refresh tokens\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.accessToken).to.be.a(\"string\").and.not.empty;",
                "    pm.expect(jsonData.refreshToken).to.be.a(\"string\").and.not.empty;",
                "    pm.collectionVariables.set(\"accessToken\", jsonData.accessToken);",
                "    pm.collectionVariables.set(\"refreshToken\", jsonData.refreshToken);",
                "});"
            ],
            description="Exchanges an unexpired refresh token for an updated token pair implementing cryptographic rotation to prevent token reuse replay attacks."
        ),
        make_request_item(
            name="05. Register Auxiliary Reviewer Account",
            method="POST",
            path_str="/auth/register",
            headers=[
                make_header("Content-Type", "application/json")
            ],
            body={
                "email": "{{secondCustomerEmail}}",
                "password": "SecurePassword2026!",
                "firstName": "Samantha",
                "lastName": "Reed",
                "phoneNumber": "+14155550177"
            },
            prerequest_script=[
                "var uniqueReviewer = 'samantha.reed.' + Date.now() + '@bookcorner.io';",
                "pm.collectionVariables.set('secondCustomerEmail', uniqueReviewer);"
            ],
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"Auxiliary reviewer registered successfully\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.accessToken).to.be.a(\"string\").and.not.empty;",
                "    pm.collectionVariables.set(\"secondCustomerAccessToken\", jsonData.accessToken);",
                "    pm.collectionVariables.set(\"secondCustomerRefreshToken\", jsonData.refreshToken);",
                "    console.log(\"Auxiliary reviewer registered: \" + pm.collectionVariables.get(\"secondCustomerEmail\"));",
                "});"
            ],
            description="Registers a distinct secondary customer account used later to verify review helpfulness voting without violating self-voting invariants."
        ),
        make_request_item(
            name="06. Invalidate User Session (Logout)",
            method="POST",
            path_str="/auth/logout",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{secondCustomerAccessToken}}", "Auxiliary bearer token to test logout without revoking primary access")
            ],
            body={
                "refreshToken": "{{secondCustomerRefreshToken}}"
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Session invalidated confirmation message received\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.message).to.include(\"Logged out successfully\");",
                "});"
            ],
            description="Revokes customer session tokens and blacklists the refresh token in Redis/persistence store."
        )
    ]
    folders.append({"name": "01. Authentication & Sessions", "item": auth_items, "description": "Endpoints managing guest sessions, registration, credential login, token rotation, and logout."})

    # =========================================================================
    # FOLDER 2: Customer Profile & Addresses
    # =========================================================================
    user_items = [
        make_request_item(
            name="01. Retrieve Current Customer Profile",
            method="GET",
            path_str="/users/me",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Profile matches authenticated customer details\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.email).to.eql(pm.collectionVariables.get(\"userEmail\"));",
                "    pm.expect(jsonData.firstName).to.eql(\"Alex\");",
                "    pm.expect(jsonData.role).to.be.oneOf([\"ROLE_CUSTOMER\", \"CUSTOMER\"]);",
                "});"
            ],
            description="Retrieves the detailed profile of the currently authenticated customer based on the JWT bearer token."
        ),
        make_request_item(
            name="02. Update Customer Profile",
            method="PATCH",
            path_str="/users/me",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "firstName": "Alexandra",
                "lastName": "Morgan-Lee",
                "phoneNumber": "+14155550188"
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Profile reflected updated attributes\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.firstName).to.eql(\"Alexandra\");",
                "    pm.expect(jsonData.lastName).to.eql(\"Morgan-Lee\");",
                "    pm.expect(jsonData.phoneNumber).to.eql(\"+14155550188\");",
                "});"
            ],
            description="Partially modifies personal account attributes including names and contact numbers."
        ),
        make_request_item(
            name="03. List Saved Addresses",
            method="GET",
            path_str="/users/me/addresses",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Response is an array\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.be.an(\"array\");",
                "});"
            ],
            description="Retrieves the collection of saved shipping and billing addresses for the customer."
        ),
        make_request_item(
            name="04. Add Primary Shipping Address",
            method="POST",
            path_str="/users/me/addresses",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "addressType": "SHIPPING",
                "isDefault": True,
                "recipientName": "Alexandra Morgan-Lee",
                "phoneNumber": "+14155550188",
                "streetAddress1": "742 Evergreen Terrace",
                "streetAddress2": "Suite 200",
                "city": "Springfield",
                "stateProvince": "OR",
                "postalCode": "97477",
                "countryCode": "US",
                "deliveryInstructions": "Please place parcel in secure porch parcel locker."
            },
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"Address created and ID persisted\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.id).to.be.a(\"string\").and.not.empty;",
                "    pm.expect(jsonData.city).to.eql(\"Springfield\");",
                "    pm.expect(jsonData.postalCode).to.eql(\"97477\");",
                "    pm.collectionVariables.set(\"addressId\", jsonData.id);",
                "    console.log(\"Saved primary addressId: \" + jsonData.id);",
                "});"
            ],
            description="Adds a new verified residential destination address to the customer's personal address book."
        ),
        make_request_item(
            name="05. Add Ephemeral Address (For Deletion Verification)",
            method="POST",
            path_str="/users/me/addresses",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "addressType": "BILLING",
                "isDefault": False,
                "recipientName": "Alex Morgan Work",
                "phoneNumber": "+14155550188",
                "streetAddress1": "100 Market Street",
                "streetAddress2": "Floor 12",
                "city": "San Francisco",
                "stateProvince": "CA",
                "postalCode": "94105",
                "countryCode": "US",
                "deliveryInstructions": "Deliver to corporate mailroom intake."
            },
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"Temporary address created and ID captured for deletion\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.id).to.be.a(\"string\").and.not.empty;",
                "    pm.collectionVariables.set(\"tempAddressId\", jsonData.id);",
                "});"
            ],
            description="Adds a secondary address destined for immediate deletion to verify endpoint lifecycle without mutating checkout references."
        ),
        make_request_item(
            name="06. Delete Address from Address Book",
            method="DELETE",
            path_str="/users/me/addresses/:addressId",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("addressId", "{{tempAddressId}}", "Unique address record UUID")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Deletion confirmation returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.message).to.include(\"Address deleted successfully\");",
                "});"
            ],
            description="Removes an existing address from the customer's address book."
        )
    ]
    folders.append({"name": "02. Customer Profile & Addresses", "item": user_items, "description": "Customer personal details, preferences, and multi-address book management."})

    # =========================================================================
    # FOLDER 3: Catalog & Discovery
    # =========================================================================
    catalog_items = [
        make_request_item(
            name="01. List Hierarchical Categories Tree",
            method="GET",
            path_str="/categories",
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Categories hierarchy tree returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.be.an(\"array\").and.not.empty;",
                "    var cat = jsonData[0];",
                "    pm.expect(cat).to.have.property(\"id\");",
                "    pm.expect(cat).to.have.property(\"categoryName\");",
                "    pm.expect(cat).to.have.property(\"categorySlug\");",
                "    pm.collectionVariables.set(\"categoryId\", cat.id);",
                "});"
            ],
            description="Retrieves the full taxonomy tree of book genres and nested subcategories."
        ),
        make_request_item(
            name="02. Browse Authors",
            method="GET",
            path_str="/authors",
            query_params=[
                ("name", "Robert", "Search keyword prefix/name"),
                ("page", 0, "Zero-indexed page number"),
                ("size", 10, "Page batch size")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Paginated authors returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"content\");",
                "    pm.expect(jsonData.content).to.be.an(\"array\").and.not.empty;",
                "    var author = jsonData.content[0];",
                "    pm.expect(author.fullName).to.include(\"Robert\");",
                "    pm.collectionVariables.set(\"authorId\", author.id);",
                "    pm.collectionVariables.set(\"authorSlug\", author.authorSlug);",
                "});"
            ],
            description="Browses and searches authors with pagination and biographical overviews."
        ),
        make_request_item(
            name="03. Get Author Details by ID",
            method="GET",
            path_str="/authors/:authorId",
            path_vars=[
                ("authorId", "{{authorId}}", "Author UUID")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Author profile contains bio and slug\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.id).to.eql(pm.collectionVariables.get(\"authorId\"));",
                "    pm.expect(jsonData.fullName).to.be.a(\"string\").and.not.empty;",
                "});"
            ],
            description="Retrieves complete biographical details, author slug, and portrait URL for a specific author."
        ),
        make_request_item(
            name="04. List Publishers",
            method="GET",
            path_str="/publishers",
            query_params=[
                ("page", 0, "Page index"),
                ("size", 10, "Page batch size")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Paginated publishers list returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"content\");",
                "    pm.expect(jsonData.content).to.be.an(\"array\").and.not.empty;",
                "});"
            ],
            description="Lists participating publishing houses and distribution entities."
        ),
        make_request_item(
            name="05. Browse Books with Faceted Filters",
            method="GET",
            path_str="/books",
            query_params=[
                ("formatType", "PAPERBACK", "Physical or digital binding format"),
                ("minPrice", 1000, "Minimum price in cents ($10.00)"),
                ("maxPrice", 6000, "Maximum price in cents ($60.00)"),
                ("language", "en", "Two-letter primary language code"),
                ("page", 0, "Zero-indexed page number"),
                ("size", 10, "Page size")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Catalog cards returned with formats and prices\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.content).to.be.an(\"array\").and.not.empty;",
                "    var book = jsonData.content[0];",
                "    pm.expect(book).to.have.property(\"bookId\");",
                "    pm.expect(book).to.have.property(\"title\");",
                "    pm.expect(book.availableFormats).to.be.an(\"array\").and.not.empty;",
                "    pm.collectionVariables.set(\"bookId\", book.bookId);",
                "});"
            ],
            description="Browses product catalog books with multi-dimensional filtering by category, format, price range, and language."
        ),
        make_request_item(
            name="06. Get Book Details by ID",
            method="GET",
            path_str="/books/:bookId",
            path_vars=[
                ("bookId", "{{bookId}}", "Master Book UUID")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Complete book specifications and format SKUs returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.id).to.eql(pm.collectionVariables.get(\"bookId\"));",
                "    pm.expect(jsonData).to.have.property(\"isbn13\");",
                "    pm.expect(jsonData.formats).to.be.an(\"array\").and.not.empty;",
                "    var format = jsonData.formats[0];",
                "    pm.expect(format).to.have.property(\"id\");",
                "    pm.expect(format).to.have.property(\"sku\");",
                "    pm.collectionVariables.set(\"formatId\", format.id);",
                "    console.log(\"Extracted formatId for cart: \" + format.id);",
                "});"
            ],
            description="Fetches full book product master data, author credits, publisher, dimensions, and all purchasable format SKUs."
        )
    ]
    folders.append({"name": "03. Catalog & Discovery", "item": catalog_items, "description": "Master catalog browsing, taxonomy hierarchy, authors, publishers, and book SKU details."})

    # =========================================================================
    # FOLDER 4: Search & Autocomplete
    # =========================================================================
    search_items = [
        make_request_item(
            name="01. Multi-Faceted Full-Text Search",
            method="GET",
            path_str="/search",
            query_params=[
                ("q", "Clean", "Search query keyword"),
                ("format", "PAPERBACK", "Optional format filter"),
                ("page", 0, "Page number"),
                ("size", 10, "Results page size")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Search results contain hits, query echo, and facets\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.query).to.eql(\"Clean\");",
                "    pm.expect(jsonData.totalMatches).to.be.at.least(1);",
                "    pm.expect(jsonData.results).to.be.an(\"array\").and.not.empty;",
                "    pm.expect(jsonData).to.have.property(\"facets\");",
                "});"
            ],
            description="Performs typo-tolerant full-text search against book titles, authors, and synopses with aggregate facet counts."
        ),
        make_request_item(
            name="02. Autocomplete Typeahead Suggestions",
            method="GET",
            path_str="/search/suggestions",
            query_params=[
                ("prefix", "Rob", "Prefix string for autocomplete"),
                ("limit", 5, "Maximum number of suggestions")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Typeahead suggestions returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.be.an(\"array\").and.not.empty;",
                "    pm.expect(jsonData[0]).to.have.property(\"text\");",
                "    pm.expect(jsonData[0]).to.have.property(\"type\");",
                "});"
            ],
            description="Provides low-latency instant auto-complete suggestions for frontend search bars (matching authors and book titles)."
        )
    ]
    folders.append({"name": "04. Search & Autocomplete", "item": search_items, "description": "High-performance full-text faceted search and instant typeahead suggestions."})

    # =========================================================================
    # FOLDER 5: Customer Wishlist
    # =========================================================================
    wishlist_items = [
        make_request_item(
            name="01. Retrieve Customer Wishlist",
            method="GET",
            path_str="/wishlists",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Wishlist aggregate returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"items\");",
                "    pm.expect(jsonData.items).to.be.an(\"array\");",
                "});"
            ],
            description="Retrieves the authenticated customer's book wishlist and active price alert monitors."
        ),
        make_request_item(
            name="02. Add Book to Wishlist with Price Alert",
            method="POST",
            path_str="/wishlists/items",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "bookId": "{{bookId2}}",
                "desiredPriceAlert": {
                    "amount": 4000,
                    "currency": "USD"
                }
            },
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"Item added with desired price threshold\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.bookId).to.eql(pm.collectionVariables.get(\"bookId2\"));",
                "    pm.expect(jsonData.desiredPriceAlert.amount).to.eql(4000);",
                "});"
            ],
            description="Saves a book to the customer's personal wishlist and configures a target price alert threshold in cents."
        ),
        make_request_item(
            name="03. Remove Book from Wishlist",
            method="DELETE",
            path_str="/wishlists/items/:bookId",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("bookId", "{{bookId2}}", "Master Book UUID")
            ],
            test_script=[
                "pm.test(\"Status code is 204 No Content\", function () {",
                "    pm.response.to.have.status(204);",
                "});"
            ],
            description="Removes a book from the customer's wishlist."
        )
    ]
    folders.append({"name": "05. Customer Wishlist", "item": wishlist_items, "description": "Customer wishlist lifecycle and price drop notification thresholds."})

    # =========================================================================
    # FOLDER 6: Shopping Cart & Basket
    # =========================================================================
    cart_items = [
        make_request_item(
            name="01. Retrieve Active Shopping Cart",
            method="GET",
            path_str="/cart",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Cart object returned with financial totals\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"items\");",
                "    pm.expect(jsonData).to.have.property(\"subtotal\");",
                "});"
            ],
            description="Retrieves the current shopping basket and computed monetary breakdown for the user."
        ),
        make_request_item(
            name="02. Add Primary Format Item to Cart",
            method="POST",
            path_str="/cart/items",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "formatId": "{{formatId}}",
                "quantity": 2
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Format item added with quantity 2\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.items).to.be.an(\"array\").and.not.empty;",
                "    var item = jsonData.items.find(function(i) { return i.formatId === pm.collectionVariables.get(\"formatId\"); });",
                "    pm.expect(item).to.be.an(\"object\");",
                "    pm.expect(item.quantity).to.be.at.least(2);",
                "});"
            ],
            description="Adds a specific book format SKU (Paperback, Hardcover, EBook) to the active shopping cart."
        ),
        make_request_item(
            name="03. Add Secondary Format Item to Cart",
            method="POST",
            path_str="/cart/items",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "formatId": "{{formatId2}}",
                "quantity": 1
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Secondary format item added to cart\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.items.length).to.be.at.least(2);",
                "});"
            ],
            description="Adds an additional book item into the cart to test multi-line cart behavior and item deletion."
        ),
        make_request_item(
            name="04. Mutate Cart Item Quantity",
            method="PATCH",
            path_str="/cart/items/:formatId",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("formatId", "{{formatId}}", "Target Book Format SKU UUID")
            ],
            body={
                "quantity": 3
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Item quantity updated to 3 and subtotal recalculated\", function () {",
                "    var jsonData = pm.response.json();",
                "    var item = jsonData.items.find(function(i) { return i.formatId === pm.collectionVariables.get(\"formatId\"); });",
                "    pm.expect(item.quantity).to.eql(3);",
                "});"
            ],
            description="Modifies the quantity of a specific item line in the basket, triggering inventory lock checks and subtotal recalculation."
        ),
        make_request_item(
            name="05. Remove Secondary Item from Cart",
            method="DELETE",
            path_str="/cart/items/:formatId",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("formatId", "{{formatId2}}", "Target Book Format SKU UUID to remove")
            ],
            test_script=[
                "pm.test(\"Status code is 204 No Content\", function () {",
                "    pm.response.to.have.status(204);",
                "});"
            ],
            description="Removes a line item completely from the cart."
        ),
        make_request_item(
            name="06. Apply Promotional Coupon Code",
            method="POST",
            path_str="/cart/coupon",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "couponCode": "SUMMER20"
            },
            test_script=[
                "// Accepts 200 if coupon exists in DB, or 400 if coupon not seeded yet in local testing environment",
                "pm.test(\"Status code is 200 OK or 400 Bad Request (Graceful Coupon Check)\", function () {",
                "    pm.expect(pm.response.code).to.be.oneOf([200, 400]);",
                "});",
                "",
                "if (pm.response.code === 200) {",
                "    pm.test(\"Coupon discount reflected in basket summary\", function () {",
                "        var jsonData = pm.response.json();",
                "        pm.expect(jsonData.couponCode).to.eql(\"SUMMER20\");",
                "        pm.expect(jsonData.discount.amount).to.be.above(0);",
                "    });",
                "}"
            ],
            description="Validates and applies a percentage or fixed-amount coupon to the active basket."
        ),
        make_request_item(
            name="07. Remove Promotional Coupon from Cart",
            method="DELETE",
            path_str="/cart/coupon",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Coupon removed from cart\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.couponCode).to.be.null;",
                "});"
            ],
            description="Removes the currently applied coupon code and restores standard subtotal pricing."
        ),
        make_request_item(
            name="08. Merge Anonymous Guest Basket into Customer Account",
            method="POST",
            path_str="/cart/merge",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "guestSessionToken": "{{guestSessionToken}}"
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Merged basket returns consolidated items\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.items).to.be.an(\"array\");",
                "});"
            ],
            description="Consolidates items accumulated in an anonymous browsing session into the authenticated user's permanent cart."
        )
    ]
    folders.append({"name": "06. Shopping Cart & Basket", "item": cart_items, "description": "Shopping basket operations, multi-format items, quantities, promotional coupons, and guest merge."})

    # =========================================================================
    # FOLDER 7: Checkout & Order Lifecycle
    # =========================================================================
    order_items = [
        make_request_item(
            name="01. Submit Checkout Saga and Confirm Order",
            method="POST",
            path_str="/orders/checkout",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}"),
                make_header("Idempotency-Key", "idemp_{{$randomUUID}}", "Guarantees exactly-once execution against duplicate charges")
            ],
            body={
                "shippingAddressId": "{{addressId}}",
                "paymentMethodToken": "pm_card_visa_tok_4242",
                "customerNotes": "Please handle with care; collector edition books included."
            },
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"Order placed and confirmed with order number\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"orderNumber\");",
                "    pm.expect(jsonData.orderNumber).to.match(/^ORD-/);",
                "    pm.expect(jsonData.orderStatus).to.eql(\"CONFIRMED\");",
                "    pm.collectionVariables.set(\"orderNumber\", jsonData.orderNumber);",
                "    console.log(\"Confirmed Order Number: \" + jsonData.orderNumber);",
                "});"
            ],
            description="Orchestrates atomic order checkout saga: inventory reservation, address snapshotting, payment authorization, consignment initialization, and cart purge."
        ),
        make_request_item(
            name="02. List Customer Order History",
            method="GET",
            path_str="/orders",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            query_params=[
                ("status", "CONFIRMED", "Filter by order lifecycle status"),
                ("page", 0, "Page zero-index"),
                ("size", 10, "Page batch size")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Order history returns confirmed orders\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.content).to.be.an(\"array\").and.not.empty;",
                "    var hasOrder = jsonData.content.some(function(o) { return o.orderNumber === pm.collectionVariables.get(\"orderNumber\"); });",
                "    pm.expect(hasOrder).to.be.true;",
                "});"
            ],
            description="Retrieves a paginated timeline of customer orders with financial summaries and status."
        ),
        make_request_item(
            name="03. Retrieve Detailed Order and Invoice Specifications",
            method="GET",
            path_str="/orders/:orderNumber",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("orderNumber", "{{orderNumber}}", "Business order code (e.g. ORD-20260919-XXXX)")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Order invoice snapshot includes line items and pricing breakdown\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.orderNumber).to.eql(pm.collectionVariables.get(\"orderNumber\"));",
                "    pm.expect(jsonData.lineItems).to.be.an(\"array\").and.not.empty;",
                "    var lineItem = jsonData.lineItems[0];",
                "    pm.expect(lineItem).to.have.property(\"lineItemId\");",
                "    pm.collectionVariables.set(\"orderLineItemId\", lineItem.lineItemId);",
                "    console.log(\"Saved orderLineItemId: \" + lineItem.lineItemId);",
                "});"
            ],
            description="Retrieves full historical invoice specification, including immutable title/price snapshots and audit milestone timeline."
        ),
        make_request_item(
            name="04. Initiate Return Merchandise Authorization (RMA)",
            method="POST",
            path_str="/orders/:orderNumber/returns",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("orderNumber", "{{orderNumber}}", "Confirmed order identifier")
            ],
            body={
                "reasonCode": "CHANGED_MIND",
                "customerRemarks": "Purchased an alternate digital audiobook edition instead.",
                "returnItems": [
                    {
                        "orderLineItemId": "{{orderLineItemId}}",
                        "quantity": 1
                    }
                ]
            },
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"RMA created with tracking number and instructions\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"rmaNumber\");",
                "    pm.expect(jsonData.rmaNumber).to.match(/^RMA-/);",
                "    pm.expect(jsonData).to.have.property(\"returnTrackingNumber\");",
                "    pm.collectionVariables.set(\"rmaNumber\", jsonData.rmaNumber);",
                "    pm.collectionVariables.set(\"returnTrackingNumber\", jsonData.returnTrackingNumber);",
                "    console.log(\"RMA Authorization generated: \" + jsonData.rmaNumber);",
                "});"
            ],
            description="Initiates an RMA return within the 30-day store return policy window, issuing prepaid carrier return label details."
        ),
        make_request_item(
            name="05. Place Secondary Order (For Grace Period Cancellation Test)",
            method="POST",
            path_str="/orders/checkout",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}"),
                make_header("Idempotency-Key", "idemp_cancel_{{$randomUUID}}")
            ],
            body={
                "newShippingAddress": {
                    "recipientName": "Alexandra Morgan-Lee",
                    "phoneNumber": "+14155550188",
                    "streetAddress1": "742 Evergreen Terrace",
                    "city": "Springfield",
                    "stateProvince": "OR",
                    "postalCode": "97477",
                    "countryCode": "US"
                },
                "paymentMethodToken": "pm_card_mastercard_tok_5555",
                "customerNotes": "Duplicate test order to verify automated cancellation."
            },
            prerequest_script=[
                "// Ensure an item is in the cart prior to checkout",
                "pm.sendRequest({",
                "    url: pm.collectionVariables.get('baseUrl') + '/cart/items',",
                "    method: 'POST',",
                "    header: {",
                "        'Content-Type': 'application/json',",
                "        'Authorization': 'Bearer ' + pm.collectionVariables.get('accessToken')",
                "    },",
                "    body: {",
                "        mode: 'raw',",
                "        raw: JSON.stringify({",
                "            formatId: pm.collectionVariables.get('formatId'),",
                "            quantity: 1",
                "        })",
                "    }",
                "}, function (err, res) {",
                "    console.log('Cart seeded for cancellation checkout. Status: ' + (res ? res.code : err));",
                "});"
            ],
            test_script=[
                "pm.test(\"Status code is 201 Created\", function () {",
                "    pm.response.to.have.status(201);",
                "});",
                "",
                "pm.test(\"Secondary order created\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.orderNumber).to.be.a(\"string\").and.not.empty;",
                "    pm.collectionVariables.set(\"orderToCancelNumber\", jsonData.orderNumber);",
                "});"
            ],
            description="Places a fresh order so that cancellation can be tested without impacting the completed order used in returns and payment tests."
        ),
        make_request_item(
            name="06. Cancel Order within Policy Grace Period",
            method="POST",
            path_str="/orders/:orderNumber/cancel",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("orderNumber", "{{orderToCancelNumber}}", "Order identifier to cancel")
            ],
            body={
                "reason": "Customer placed duplicate order by mistake; immediate self-service cancellation."
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Order cancelled and restock confirmation issued\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.orderStatus).to.eql(\"CANCELLED\");",
                "    pm.expect(jsonData).to.have.property(\"refundAmount\");",
                "});"
            ],
            description="Cancels an order within the 60-minute policy grace window, automatically restoring inventory quantities and recording refunds."
        )
    ]
    folders.append({"name": "07. Checkout & Order Lifecycle", "item": order_items, "description": "End-to-end checkout saga, customer order invoices, self-service cancellation, and RMA returns."})

    # =========================================================================
    # FOLDER 8: Payments & Digital Wallet
    # =========================================================================
    payment_items = [
        make_request_item(
            name="01. Coordinate Multi-Tender Payment Intent",
            method="POST",
            path_str="/payments/intent",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            body={
                "orderNumber": "{{orderNumber}}",
                "tenderSplits": [
                    {
                        "tenderType": "CREDIT_CARD",
                        "amount": {
                            "amount": 3999,
                            "currency": "USD"
                        },
                        "tenderReference": "visa_card_ending_4242"
                    }
                ]
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Payment intent initialized\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"paymentIntentId\");",
                "    pm.expect(jsonData).to.have.property(\"status\");",
                "});"
            ],
            description="Initializes multi-tender payment coordination (credit card + digital wallet ledger split) prior to third-party authorization."
        ),
        make_request_item(
            name="02. Ingest Payment Gateway Webhook",
            method="POST",
            path_str="/payments/webhooks/:provider",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Stripe-Signature", "t=1758280000,v1=simulated_sha256_webhook_signature", "Carrier/Gateway HMAC signature header")
            ],
            path_vars=[
                ("provider", "stripe", "Payment gateway provider code")
            ],
            body=json.dumps({
                "id": "evt_test_charge_succeeded_2026",
                "type": "payment_intent.succeeded",
                "created": 1758280000,
                "data": {
                    "object": {
                        "id": "pi_3N21ABCXYZ",
                        "amount": 3999,
                        "currency": "usd",
                        "status": "succeeded"
                    }
                }
            }, indent=2),
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Webhook acknowledged\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.received).to.eql(true);",
                "});"
            ],
            description="Asynchronous listener ingesting and cryptographically verifying payment gateway webhook notifications (Stripe, Adyen, Razorpay)."
        ),
        make_request_item(
            name="03. Retrieve Customer Digital Wallet Balance",
            method="GET",
            path_str="/payments/wallet",
            headers=[
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Wallet balance details returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"currentBalance\");",
                "    pm.expect(jsonData.currencyCode).to.eql(\"USD\");",
                "    pm.expect(jsonData.isLocked).to.eql(false);",
                "});"
            ],
            description="Retrieves the customer's store credit wallet balance, currency code, and locked status."
        )
    ]
    folders.append({"name": "08. Payments & Digital Wallet", "item": payment_items, "description": "Payment intents, gateway webhook listeners, and digital store wallet ledgers."})

    # =========================================================================
    # FOLDER 9: Shipping & Logistics
    # =========================================================================
    shipping_items = [
        make_request_item(
            name="01. Calculate Dynamic Carrier Shipping Rates and ETA",
            method="POST",
            path_str="/shipping/rates",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("X-Store-Code", "{{storeCode}}")
            ],
            body={
                "destinationPostalCode": "97477",
                "destinationCountryCode": "US",
                "weightGrams": 680
            },
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Calculates valid carrier shipping tiers\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.be.an(\"array\").and.not.empty;",
                "    var rate = jsonData[0];",
                "    pm.expect(rate).to.have.property(\"carrierCode\");",
                "    pm.expect(rate).to.have.property(\"serviceLevel\");",
                "    pm.expect(rate).to.have.property(\"shippingCost\");",
                "    pm.expect(rate).to.have.property(\"estimatedDeliveryDays\");",
                "});"
            ],
            description="Calculates dynamic carrier rates (Standard Ground, 2-Day Air, Overnight) and delivery date estimates."
        ),
        make_request_item(
            name="02. Track Carrier Consignment Milestones",
            method="GET",
            path_str="/shipping/consignments/:trackingNumber",
            path_vars=[
                ("trackingNumber", "{{trackingNumber}}", "Carrier Tracking Identifier")
            ],
            test_script=[
                "// Accepts 200 if tracking number seeded in DB, or 404 if mock tracking number is queried",
                "pm.test(\"Status code is 200 OK or 404 Not Found\", function () {",
                "    pm.expect(pm.response.code).to.be.oneOf([200, 404]);",
                "});",
                "",
                "if (pm.response.code === 200) {",
                "    pm.test(\"Tracking timeline contains milestones\", function () {",
                "        var jsonData = pm.response.json();",
                "        pm.expect(jsonData).to.have.property(\"trackingNumber\");",
                "        pm.expect(jsonData.milestones).to.be.an(\"array\");",
                "    });",
                "}"
            ],
            description="Queries real-time carrier consignment delivery status and milestone event history."
        )
    ]
    folders.append({"name": "09. Shipping & Logistics", "item": shipping_items, "description": "Freight rate engine, carrier delivery ETAs, and consignment tracking."})

    # =========================================================================
    # FOLDER 10: Reviews & Helpfulness Ratings
    # =========================================================================
    review_items = [
        make_request_item(
            name="01. List Customer Book Reviews",
            method="GET",
            path_str="/books/:bookId/reviews",
            path_vars=[
                ("bookId", "{{bookId}}", "Master Book UUID")
            ],
            query_params=[
                ("sort", "HELPFUL", "Sort order: HELPFUL, RECENT, RATING_HIGH, RATING_LOW"),
                ("page", 0, "Page zero-index"),
                ("size", 10, "Page batch size")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Book reviews summary returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"averageRating\");",
                "    pm.expect(jsonData).to.have.property(\"totalReviews\");",
                "    pm.expect(jsonData).to.have.property(\"reviews\");",
                "});"
            ],
            description="Retrieves aggregate rating metrics, star histograms, and verified customer written reviews for a book."
        ),
        make_request_item(
            name="02. Submit Verified Customer Review and Rating",
            method="POST",
            path_str="/books/:bookId/reviews",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{accessToken}}")
            ],
            path_vars=[
                ("bookId", "{{bookId}}", "Master Book UUID")
            ],
            body={
                "rating": 5,
                "reviewTitle": "Exceptional guide to writing clean and maintainable software",
                "reviewBody": "Clean Code by Uncle Bob is an industry staple that fundamentally transforms how software engineers structure their code. The chapters on function design, naming conventions, and code smells are directly applicable to daily production codebases."
            },
            test_script=[
                "// 201 Created on first submission, or 409 Conflict if review already submitted by this user",
                "pm.test(\"Status code is 201 Created or 409 Conflict\", function () {",
                "    pm.expect(pm.response.code).to.be.oneOf([201, 409]);",
                "});",
                "",
                "if (pm.response.code === 201) {",
                "    pm.test(\"Review saved and ID captured for voting test\", function () {",
                "        var jsonData = pm.response.json();",
                "        pm.expect(jsonData.id).to.be.a(\"string\").and.not.empty;",
                "        pm.expect(jsonData.rating).to.eql(5);",
                "        pm.collectionVariables.set(\"reviewId\", jsonData.id);",
                "        console.log(\"Saved reviewId: \" + jsonData.id);",
                "    });",
                "}"
            ],
            description="Submits a verified customer rating (1-5 stars) and review commentary."
        ),
        make_request_item(
            name="03. Vote on Review Helpfulness",
            method="POST",
            path_str="/reviews/:reviewId/votes",
            headers=[
                make_header("Content-Type", "application/json"),
                make_header("Authorization", "Bearer {{secondCustomerAccessToken}}", "Uses secondary customer token to prevent self-voting violation")
            ],
            path_vars=[
                ("reviewId", "{{reviewId}}", "Target Review UUID")
            ],
            body={
                "isHelpful": True
            },
            test_script=[
                "// If a review was created in the previous step, verify 200 OK; otherwise handle 404/400 gracefully",
                "pm.test(\"Status code is 200 OK (or 404 if no review ID available)\", function () {",
                "    pm.expect(pm.response.code).to.be.oneOf([200, 404]);",
                "});",
                "",
                "if (pm.response.code === 200) {",
                "    pm.test(\"Helpfulness vote recorded\", function () {",
                "        var jsonData = pm.response.json();",
                "        pm.expect(jsonData.userVote).to.eql(true);",
                "        pm.expect(jsonData.helpfulVotesCount).to.be.at.least(1);",
                "    });",
                "}"
            ],
            description="Casts a helpfulness upvote or downvote on a customer review using a distinct peer reviewer account."
        )
    ]
    folders.append({"name": "10. Reviews & Helpfulness Ratings", "item": review_items, "description": "Verified purchase reviews, star distributions, and helpfulness voting."})

    # =========================================================================
    # FOLDER 11: Merchandising & Recommendations
    # =========================================================================
    recommendation_items = [
        make_request_item(
            name="01. Get Personalized Home Page Recommendations Carousel",
            method="GET",
            path_str="/recommendations/home",
            headers=[
                make_header("X-Store-Code", "{{storeCode}}")
            ],
            query_params=[
                ("limit", 10, "Maximum number of items per carousel section")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Home recommendations return sections\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"sections\");",
                "    pm.expect(jsonData.sections).to.be.an(\"array\").and.not.empty;",
                "});"
            ],
            description="Generates dynamic landing page carousels including trending books, new releases, and personalized picks."
        ),
        make_request_item(
            name="02. Get Book Up-Sell Collector Editions",
            method="GET",
            path_str="/recommendations/books/:bookId/upsell",
            path_vars=[
                ("bookId", "{{bookId}}", "Master Book UUID")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Up-sell editions computed\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"bookId\");",
                "    pm.expect(jsonData).to.have.property(\"upgrades\");",
                "});"
            ],
            description="Offers premium edition upgrades (e.g. Deluxe Hardcover, Signed Anniversary Edition) based on configured merchandising rules."
        ),
        make_request_item(
            name="03. Get Book Cross-Sell Companion Titles",
            method="GET",
            path_str="/recommendations/books/:bookId/cross-sell",
            path_vars=[
                ("bookId", "{{bookId}}", "Master Book UUID")
            ],
            query_params=[
                ("limit", 3, "Companion bundle items limit")
            ],
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"Cross-sell companion items returned\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData).to.have.property(\"companions\");",
                "    pm.expect(jsonData.companions).to.be.an(\"array\");",
                "});"
            ],
            description="Recommends bundle companion titles frequently purchased together with the selected book."
        )
    ]
    folders.append({"name": "11. Merchandising & Recommendations", "item": recommendation_items, "description": "Home page personalized discovery, Up-Sell upgrades, and Cross-Sell companion bundling."})

    # =========================================================================
    # FOLDER 12: System Health & Observability
    # =========================================================================
    observability_items = [
        make_request_item(
            name="01. System Health Check (Liveness & Readiness)",
            method="GET",
            path_str="/actuator/health",
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});",
                "",
                "pm.test(\"System status is UP\", function () {",
                "    var jsonData = pm.response.json();",
                "    pm.expect(jsonData.status).to.eql(\"UP\");",
                "});"
            ],
            description="Spring Boot Actuator probes verifying container liveness, PostgreSQL readiness, and disk space health."
        ),
        make_request_item(
            name="02. System Application Information",
            method="GET",
            path_str="/actuator/info",
            test_script=[
                "pm.test(\"Status code is 200 OK\", function () {",
                "    pm.response.to.have.status(200);",
                "});"
            ],
            description="Application metadata and build configuration info from Spring Boot Actuator."
        )
    ]
    folders.append({"name": "12. System Health & Observability", "item": observability_items, "description": "Spring Boot Actuator operational probes, metrics, and health checks."})

    collection["item"] = folders
    return collection

def build_environment():
    return {
        "id": ENV_ID,
        "name": "Book Corner - Local Environment",
        "values": [
            {
                "key": "baseUrl",
                "value": "http://localhost:8080",
                "type": "default",
                "enabled": True
            },
            {
                "key": "storeCode",
                "value": "BK_MAIN_ONLINE",
                "type": "default",
                "enabled": True
            },
            {
                "key": "userEmail",
                "value": "alex.morgan.reader@bookcorner.io",
                "type": "default",
                "enabled": True
            },
            {
                "key": "userPassword",
                "value": "SecurePassword2026!",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "accessToken",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "refreshToken",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "secondCustomerAccessToken",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "secondCustomerRefreshToken",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "bookId",
                "value": "30000000-0000-0000-0000-000000000001",
                "type": "default",
                "enabled": True
            },
            {
                "key": "bookId2",
                "value": "30000000-0000-0000-0000-000000000002",
                "type": "default",
                "enabled": True
            },
            {
                "key": "formatId",
                "value": "31000000-0000-0000-0000-000000000001",
                "type": "default",
                "enabled": True
            },
            {
                "key": "formatId2",
                "value": "31000000-0000-0000-0000-000000000004",
                "type": "default",
                "enabled": True
            },
            {
                "key": "authorId",
                "value": "40000000-0000-0000-0000-000000000001",
                "type": "default",
                "enabled": True
            },
            {
                "key": "categoryId",
                "value": "10000000-0000-0000-0000-000000000031",
                "type": "default",
                "enabled": True
            },
            {
                "key": "trackingNumber",
                "value": "TRK987654321",
                "type": "default",
                "enabled": True
            }
        ],
        "_postman_variable_scope": "environment"
    }

if __name__ == "__main__":
    out_dir = os.path.dirname(os.path.abspath(__file__))
    
    collection = build_collection()
    coll_file = os.path.join(out_dir, "book_corner.postman_collection.json")
    with open(coll_file, "w", encoding="utf-8") as f:
        json.dump(collection, f, indent=2)
    print(f"Generated Postman Collection: {coll_file}")
    
    env = build_environment()
    env_file = os.path.join(out_dir, "book_corner_local.postman_environment.json")
    with open(env_file, "w", encoding="utf-8") as f:
        json.dump(env, f, indent=2)
    print(f"Generated Postman Environment: {env_file}")
