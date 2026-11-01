package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.cart.CartItemAddRequest;
import com.wedbackendservice.nextgen.dto.response.cart.CartItemAddResponse;
import com.wedbackendservice.nextgen.dto.response.cart.CartResponse;
import com.wedbackendservice.nextgen.exception.GlobalExceptionHandler;
import com.wedbackendservice.nextgen.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetCart_Success() throws Exception {
        CartResponse resp = CartResponse.builder()
                .cartId(1L)
                .userId(101L)
                .items(List.of())
                .totalQuantity(0)
                .subtotal(BigDecimal.ZERO)
                .build();

        when(cartService.getCart(any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.cartId").value(1));
    }

    @Test
    void testAddItemToCart_Success() throws Exception {
        CartItemAddRequest req = CartItemAddRequest.builder()
                .variantId(100L)
                .quantity(1)
                .build();

        CartItemAddResponse resp = CartItemAddResponse.builder()
                .cartItemId(1L)
                .variantId(100L)
                .quantity(1)
                .totalCartItems(1)
                .build();

        when(cartService.addItemToCart(any(), any(CartItemAddRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer valid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.cartItemId").value(1));
    }
}
