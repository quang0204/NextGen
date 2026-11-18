package com.wedbackendservice.nextgen.module.shipment.controller.client;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.shipment.dto.client.response.ShipmentTrackResponse;
import com.wedbackendservice.nextgen.module.shipment.service.ShipmentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentsController {

  private final ShipmentsService shipmentsService;

  /** API 9.1: Khách hàng tra cứu hành trình vận chuyển (Public) */
  @GetMapping("/track/{trackingCode}")
  public ApiResponse<ShipmentTrackResponse> trackShipment(@PathVariable String trackingCode) {
    return ApiResponse.ok(
        "Tra cứu thông tin vận đơn thành công", shipmentsService.trackShipment(trackingCode));
  }
}
