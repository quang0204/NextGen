package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.shipment.ShipmentTrackResponse;
import com.wedbackendservice.nextgen.exception.GlobalExceptionHandler;
import com.wedbackendservice.nextgen.service.ShipmentsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ShipmentsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShipmentsService shipmentsService;

    @InjectMocks
    private ShipmentsController shipmentsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shipmentsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testTrackShipment_Success() throws Exception {
        ShipmentTrackResponse resp = ShipmentTrackResponse.builder()
                .shipmentId(89L)
                .trackingCode("GHN-20261015089")
                .orderCode("DH-20261015-089")
                .carrier("Giao Hàng Nhanh Express")
                .status("IN_TRANSIT")
                .statusLabel("Đang vận chuyển")
                .build();

        when(shipmentsService.trackShipment("GHN-20261015089")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/shipments/track/GHN-20261015089"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.shipmentId").value(89))
                .andExpect(jsonPath("$.data.trackingCode").value("GHN-20261015089"));
    }
}
