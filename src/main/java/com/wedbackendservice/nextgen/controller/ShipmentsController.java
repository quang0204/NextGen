package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.shipment.ShipmentTrackResponse;
import com.wedbackendservice.nextgen.service.ShipmentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentsController {

    private final ShipmentsService shipmentsService;

    /**
     * API 9.1: Khách hàng tra cứu hành trình vận chuyển (Public)
     */
    @GetMapping("/track/{trackingCode}")
    public ApiResponse<ShipmentTrackResponse> trackShipment(@PathVariable String trackingCode) {
        return ApiResponse.<ShipmentTrackResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Tra cứu thông tin vận đơn thành công")
                .data(shipmentsService.trackShipment(trackingCode))
                .build();
    }
}
