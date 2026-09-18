package com.bookcorner.controller;

import com.bookcorner.dto.shipping.ConsignmentTrackingResponse;
import com.bookcorner.dto.shipping.ShippingRateCalculationRequest;
import com.bookcorner.dto.shipping.ShippingRateOptionDto;
import com.bookcorner.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Logistics and Freight Shipping REST Controller.
 */
@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipping", description = "Real-time dynamic freight calculation, delivery ETA estimation, and consignment tracking.")
public class ShippingController {

    private final ShippingService shippingService;

    @Operation(summary = "Calculate dynamic carrier shipping rates and ETA", operationId = "calculateShippingRates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrier rate options calculated."),
            @ApiResponse(responseCode = "400", description = "Bad Request / Invalid Postal Code.")
    })
    @PostMapping("/rates")
    public ResponseEntity<List<ShippingRateOptionDto>> calculateShippingRates(
            @Parameter(description = "Store code header")
            @RequestHeader(value = "X-Store-Code", required = false) String storeCode,
            @Valid @RequestBody ShippingRateCalculationRequest request
    ) {
        log.info("POST /shipping/rates for country: {}, postal: {}, weight: {}g",
                request.getDestinationCountryCode(), request.getDestinationPostalCode(), request.getWeightGrams());
        List<ShippingRateOptionDto> rates = shippingService.calculateShippingRates(request);
        return ResponseEntity.ok(rates);
    }

    @Operation(summary = "Track carrier consignment milestones", operationId = "trackConsignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consignment tracking timeline retrieved."),
            @ApiResponse(responseCode = "404", description = "Consignment not found.")
    })
    @GetMapping("/consignments/{trackingNumber}")
    public ResponseEntity<ConsignmentTrackingResponse> trackConsignment(
            @Parameter(description = "Carrier tracking number", required = true)
            @PathVariable("trackingNumber") String trackingNumber
    ) {
        log.info("GET /shipping/consignments/{}", trackingNumber);
        ConsignmentTrackingResponse response = shippingService.trackConsignment(trackingNumber);
        return ResponseEntity.ok(response);
    }
}
