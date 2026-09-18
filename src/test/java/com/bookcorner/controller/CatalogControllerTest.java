package com.bookcorner.controller;

import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.catalog.AuthorDto;
import com.bookcorner.dto.catalog.BookCatalogCardDto;
import com.bookcorner.dto.catalog.BookDetailResponse;
import com.bookcorner.dto.catalog.CategoryNodeDto;
import com.bookcorner.mapper.CatalogMapper;
import com.bookcorner.repository.catalog.PublisherRepository;
import com.bookcorner.security.CustomAccessDeniedHandler;
import com.bookcorner.security.CustomAuthenticationEntryPoint;
import com.bookcorner.security.CustomUserDetailsService;
import com.bookcorner.security.JwtFilter;
import com.bookcorner.security.JwtProvider;
import com.bookcorner.service.CatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CatalogController WebMvc Tests (MockMvc & AssertJ)")
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogService catalogService;

    @MockBean
    private PublisherRepository publisherRepository;

    @MockBean
    private CatalogMapper catalogMapper;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    @DisplayName("GET /categories should return 200 OK with list of hierarchical category trees")
    void shouldListCategoriesSuccessfully() throws Exception {
        CategoryNodeDto root = CategoryNodeDto.builder()
                .categoryId(UUID.randomUUID())
                .name("Computer Science")
                .slug("computer-science")
                .displayOrder(1)
                .build();

        when(catalogService.listCategories()).thenReturn(List.of(root));

        mockMvc.perform(get("/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Computer Science"))
                .andExpect(jsonPath("$[0].slug").value("computer-science"));
    }

    @Test
    @DisplayName("GET /authors should return 200 OK with paginated authors")
    void shouldListAuthorsSuccessfully() throws Exception {
        AuthorDto author = AuthorDto.builder()
                .authorId(UUID.randomUUID())
                .fullName("Martin Fowler")
                .authorSlug("martin-fowler")
                .build();
        Page<AuthorDto> page = new PageImpl<>(List.of(author));

        when(catalogService.listAuthors(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/authors")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Martin Fowler"))
                .andExpect(jsonPath("$.content[0].authorSlug").value("martin-fowler"));
    }

    @Test
    @DisplayName("GET /authors/{authorId} should return 200 OK when author exists")
    void shouldGetAuthorByIdSuccessfully() throws Exception {
        UUID authorId = UUID.randomUUID();
        AuthorDto author = AuthorDto.builder()
                .authorId(authorId)
                .fullName("Robert C. Martin")
                .authorSlug("robert-c-martin")
                .build();

        when(catalogService.getAuthorDetails(authorId)).thenReturn(author);

        mockMvc.perform(get("/authors/{authorId}", authorId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Robert C. Martin"))
                .andExpect(jsonPath("$.authorSlug").value("robert-c-martin"));
    }

    @Test
    @DisplayName("GET /authors/{authorId} should return 404 Not Found when author does not exist")
    void shouldReturn404WhenAuthorNotFound() throws Exception {
        UUID authorId = UUID.randomUUID();
        when(catalogService.getAuthorDetails(authorId))
                .thenThrow(new ResourceNotFoundException("Author not found with ID: " + authorId));

        mockMvc.perform(get("/authors/{authorId}", authorId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /books should return 200 OK with faceted search results")
    void shouldBrowseBooksSuccessfully() throws Exception {
        BookCatalogCardDto card = BookCatalogCardDto.builder()
                .bookId(UUID.randomUUID())
                .title("Clean Code")
                .isbn13("9780132350884")
                .primaryAuthorName("Robert C. Martin")
                .build();
        Page<BookCatalogCardDto> page = new PageImpl<>(List.of(card));

        when(catalogService.browseBooks(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/books")
                        .param("formatType", "PAPERBACK")
                        .param("minPrice", "1000")
                        .param("maxPrice", "5000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[0].isbn13").value("9780132350884"));
    }

    @Test
    @DisplayName("GET /books/{bookId} should return 200 OK with complete book details")
    void shouldGetBookByIdSuccessfully() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookDetailResponse bookDetail = BookDetailResponse.builder()
                .bookId(bookId)
                .title("Clean Code")
                .isbn13("9780132350884")
                .primaryLanguage("en")
                .pageCount(464)
                .build();

        when(catalogService.getBookDetails(bookId)).thenReturn(bookDetail);

        mockMvc.perform(get("/books/{bookId}", bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(bookId.toString()))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.isbn13").value("9780132350884"));
    }

    @Test
    @DisplayName("GET /books/{bookId} should return 404 Not Found when book ID does not exist")
    void shouldReturn404WhenBookNotFound() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(catalogService.getBookDetails(bookId))
                .thenThrow(new ResourceNotFoundException("Book not found with ID: " + bookId));

        mockMvc.perform(get("/books/{bookId}", bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
