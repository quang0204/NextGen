package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.category.CategoryResponse;
import com.wedbackendservice.nextgen.exception.GlobalExceptionHandler;
import com.wedbackendservice.nextgen.service.CategoriesAndBrandsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CategoriesControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoriesAndBrandsService categoriesAndBrandsService;

    @InjectMocks
    private CategoriesController categoriesController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoriesController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetCategoriesTree_Success() throws Exception {
        CategoryResponse cat = CategoryResponse.builder()
                .categoryId(1L)
                .name("Điện thoại")
                .isVisible(true)
                .productCount(10)
                .build();

        when(categoriesAndBrandsService.getCategoriesTree(false)).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].categoryId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Điện thoại"));
    }

    @Test
    void testGetCategoryById_Success() throws Exception {
        CategoryResponse cat = CategoryResponse.builder()
                .categoryId(1L)
                .name("Điện thoại")
                .isVisible(true)
                .productCount(10)
                .build();

        when(categoriesAndBrandsService.getCategoryById(1L)).thenReturn(cat);

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryId").value(1))
                .andExpect(jsonPath("$.data.name").value("Điện thoại"));
    }
}
