package com.bookcorner.integration;

import com.bookcorner.dto.auth.AuthTokenResponse;
import com.bookcorner.dto.auth.GuestSessionResponse;
import com.bookcorner.dto.auth.LoginRequest;
import com.bookcorner.dto.auth.RefreshTokenRequest;
import com.bookcorner.dto.auth.RegisterRequest;
import com.bookcorner.dto.auth.TokenRotationResponse;
import com.bookcorner.dto.cart.AddCartItemRequest;
import com.bookcorner.dto.cart.CartResponse;
import com.bookcorner.dto.cart.MergeCartRequest;
import com.bookcorner.dto.cart.UpdateCartItemQuantityRequest;
import com.bookcorner.entity.catalog.AuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.catalog.CategoryEntity;
import com.bookcorner.entity.catalog.PublisherEntity;
import com.bookcorner.entity.store.StoreEntity;
import com.bookcorner.repository.catalog.AuthorRepository;
import com.bookcorner.repository.catalog.BookFormatRepository;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.catalog.CategoryRepository;
import com.bookcorner.repository.catalog.PublisherRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Full API End-to-End Integration Tests (PostgreSQL Testcontainer)")
class FullApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookFormatRepository bookFormatRepository;

    @Autowired
    private UserRepository userRepository;

    private StoreEntity store;
    private PublisherEntity publisher;
    private AuthorEntity author;
    private CategoryEntity category;
    private BookEntity book;
    private BookFormatEntity format;

    @BeforeEach
    void setUpTestData() {
        if (storeRepository.findByStoreCode("BK_ONLINE_IT").isEmpty()) {
            store = storeRepository.save(StoreEntity.builder()
                    .storeCode("BK_ONLINE_IT")
                    .storeName("Book Corner Online Integration")
                    .defaultCurrency("USD")
                    .isActive(true)
                    .build());
        } else {
            store = storeRepository.findByStoreCode("BK_ONLINE_IT").get();
        }

        if (publisherRepository.findAll().isEmpty()) {
            publisher = publisherRepository.save(PublisherEntity.builder()
                    .publisherCode("PUB-IT-01")
                    .publisherName("Addison-Wesley Professional")
                    .contactEmail("contact@aw.com")
                    .build());
        } else {
            publisher = publisherRepository.findAll().get(0);
        }

        if (categoryRepository.findBySlug("software-engineering").isEmpty()) {
            category = categoryRepository.save(CategoryEntity.builder()
                    .categoryName("Software Engineering")
                    .categorySlug("software-engineering")
                    .displayOrder(1)
                    .build());
        } else {
            category = categoryRepository.findBySlug("software-engineering").get();
        }

        if (authorRepository.findAll().isEmpty()) {
            author = authorRepository.save(AuthorEntity.builder()
                    .fullName("Martin Fowler")
                    .authorSlug("martin-fowler-it")
                    .biography("Author and speaker on software architecture")
                    .build());
        } else {
            author = authorRepository.findAll().get(0);
        }

        if (bookRepository.findByIsbn13("9780134757599").isEmpty()) {
            book = BookEntity.builder()
                    .isbn13("9780134757599")
                    .title("Refactoring: Improving the Design of Existing Code")
                    .subtitle("Second Edition")
                    .synopsis("A guide to refactoring and patterns.")
                    .publisher(publisher)
                    .primaryCategory(category)
                    .publicationDate(LocalDate.of(2018, 11, 20))
                    .primaryLanguage("English")
                    .pageCount(448)
                    .build();

            book.addAuthor(author, 1, "AUTHOR");

            format = BookFormatEntity.builder()
                    .book(book)
                    .sku("SKU-REFACTOR-PB")
                    .formatType("PAPERBACK")
                    .basePriceAmount(4999L)
                    .currencyCode("USD")
                    .stockQuantity(25)
                    .build();
            book.addFormat(format);

            book = bookRepository.save(book);
            format = book.getFormats().get(0);
        } else {
            book = bookRepository.findByIsbn13("9780134757599").get();
            format = book.getFormats().get(0);
        }
    }

    @Test
    @DisplayName("API Flow 1: Complete Authentication Lifecycle (Register -> Login -> Rotate -> Logout)")
    void shouldExecuteCompleteAuthLifecycle() throws Exception {
        String uniqueEmail = "it_user_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        // 1. Register
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(uniqueEmail)
                .password("SecurePassword2026!")
                .firstName("Alice")
                .lastName("Walker")
                .phoneNumber("+12025550143")
                .build();

        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        AuthTokenResponse regTokens = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthTokenResponse.class);

        // 2. Login
        LoginRequest loginReq = LoginRequest.builder()
                .email(uniqueEmail)
                .password("SecurePassword2026!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        AuthTokenResponse loginTokens = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthTokenResponse.class);

        // 3. Refresh Token Rotation
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(loginTokens.getRefreshToken())
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        TokenRotationResponse rotatedTokens = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), TokenRotationResponse.class);

        // 4. Logout
        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(rotatedTokens.getRefreshToken())
                .build();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + rotatedTokens.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully. Tokens invalidated."));
    }

    @Test
    @DisplayName("API Flow 2: Catalog Browsing and Product Specification Queries")
    void shouldBrowseCatalogAndRetrieveSpecifications() throws Exception {
        // 1. List Categories
        mockMvc.perform(get("/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("software-engineering"));

        // 2. Browse Books with filters
        mockMvc.perform(get("/books")
                        .param("formatType", "PAPERBACK")
                        .param("minPrice", "1000")
                        .param("maxPrice", "6000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isbn13").value("9780134757599"))
                .andExpect(jsonPath("$.content[0].title").value(containsString("Refactoring")));

        // 3. Get Book Details by ID
        mockMvc.perform(get("/books/{bookId}", book.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn13").value("9780134757599"))
                .andExpect(jsonPath("$.formats[0].sku").value("SKU-REFACTOR-PB"))
                .andExpect(jsonPath("$.formats[0].basePrice.amount").value(4999));

        // 4. Get Author by ID
        mockMvc.perform(get("/authors/{authorId}", author.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Martin Fowler"))
                .andExpect(jsonPath("$.authorSlug").value("martin-fowler-it"));
    }

    @Test
    @DisplayName("API Flow 3: Guest Cart carryover and Account Merge upon Registration")
    void shouldHandleGuestCartLifecycleAndMergeIntoCustomerAccount() throws Exception {
        // 1. Initialize anonymous guest session
        MvcResult sessionResult = mockMvc.perform(post("/auth/guest-session")
                        .header("X-Store-Code", "BK_ONLINE_IT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestSessionToken").isNotEmpty())
                .andReturn();

        GuestSessionResponse session = objectMapper.readValue(
                sessionResult.getResponse().getContentAsString(), GuestSessionResponse.class);
        String guestToken = session.getGuestSessionToken();

        // 2. Add format item to anonymous guest cart
        AddCartItemRequest addReq = AddCartItemRequest.builder()
                .formatId(format.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/cart/items")
                        .header("X-Guest-Session-Token", guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal.amount").value(4999 * 2));

        // 3. Update quantity in guest cart
        UpdateCartItemQuantityRequest updateReq = UpdateCartItemQuantityRequest.builder()
                .quantity(3)
                .build();

        mockMvc.perform(patch("/cart/items/{formatId}", format.getId())
                        .header("X-Guest-Session-Token", guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal.amount").value(4999 * 3));

        // 4. Register new customer
        String customerEmail = "guest_merge_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(customerEmail)
                .password("Password123!")
                .firstName("Bob")
                .lastName("Dylan")
                .build();

        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthTokenResponse customerTokens = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthTokenResponse.class);

        // 5. Merge guest cart into authenticated customer cart
        MergeCartRequest mergeReq = MergeCartRequest.builder()
                .guestSessionToken(guestToken)
                .build();

        mockMvc.perform(post("/cart/merge")
                        .header("Authorization", "Bearer " + customerTokens.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mergeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.subtotal.amount").value(4999 * 3));

        // 6. Verify customer cart retains merged contents
        MvcResult cartResult = mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + customerTokens.getAccessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].format.sku").value("SKU-REFACTOR-PB"))
                .andReturn();

        CartResponse cart = objectMapper.readValue(cartResult.getResponse().getContentAsString(), CartResponse.class);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("API Flow 4: Digital Wallet Balance Retrieval")
    void shouldRetrieveCustomerWalletBalance() throws Exception {
        String customerEmail = "wallet_user_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(customerEmail)
                .password("Password123!")
                .firstName("Charlie")
                .lastName("Brown")
                .build();

        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthTokenResponse customerTokens = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthTokenResponse.class);

        mockMvc.perform(get("/payments/wallet")
                        .header("Authorization", "Bearer " + customerTokens.getAccessToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance.amount").value(0))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.isLocked").value(false));
    }
}
