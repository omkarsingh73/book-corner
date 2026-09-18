package com.bookcorner.service;

import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.dto.shipping.ConsignmentTrackingResponse;
import com.bookcorner.dto.shipping.ShippingRateCalculationRequest;
import com.bookcorner.dto.shipping.ShippingRateOptionDto;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.shipping.CarrierRateCardEntity;
import com.bookcorner.entity.shipping.ShippingConsignmentEntity;
import com.bookcorner.mapper.ShippingMapper;
import com.bookcorner.repository.shipping.CarrierRateCardRepository;
import com.bookcorner.repository.shipping.ShippingConsignmentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Logistics and Freight Shipping Service.
 * Coordinates rate calculation based on postal destinations and parcel weights,
 * carrier consignment generation, and tracking milestone lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final CarrierRateCardRepository carrierRateCardRepository;
    private final ShippingConsignmentRepository shippingConsignmentRepository;
    private final ShippingMapper shippingMapper;

    /**
     * Calculates available shipping rates, carrier options, and estimated delivery dates.
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "shippingCarrier")
    @Retry(name = "shippingCarrier")
    public List<ShippingRateOptionDto> calculateShippingRates(ShippingRateCalculationRequest request) {
        String destCountry = (request.getDestinationCountryCode() != null)
                ? request.getDestinationCountryCode().trim().toUpperCase()
                : "US";

        log.info("Calculating shipping rates for destination: {} (postal: {}), weight: {}g",
                destCountry, request.getDestinationPostalCode(), request.getWeightGrams());

        List<CarrierRateCardEntity> rateCards = carrierRateCardRepository.findActiveRatesByRoute("US", destCountry);

        if (rateCards.isEmpty()) {
            log.info("No direct route rate cards for US -> {}. Falling back to default domestic rates.", destCountry);
            rateCards = carrierRateCardRepository.findActiveRatesByRoute("US", "US");
        }

        if (rateCards.isEmpty()) {
            // Provide fallback default options if rate cards are not populated
            return List.of(
                    ShippingRateOptionDto.builder()
                            .carrierCode("STANDARD")
                            .serviceLevel("Standard Ground")
                            .shippingCost(MoneyDto.of(499L, "USD"))
                            .estimatedMinDays(3)
                            .estimatedMaxDays(5)
                            .estimatedDeliveryDate(LocalDate.now().plusDays(5).toString())
                            .build(),
                    ShippingRateOptionDto.builder()
                            .carrierCode("EXPRESS")
                            .serviceLevel("Priority Express")
                            .shippingCost(MoneyDto.of(1299L, "USD"))
                            .estimatedMinDays(1)
                            .estimatedMaxDays(2)
                            .estimatedDeliveryDate(LocalDate.now().plusDays(2).toString())
                            .build()
            );
        }

        double weightKg = Math.ceil(request.getWeightGrams() / 1000.0);
        List<ShippingRateOptionDto> options = new ArrayList<>();

        for (CarrierRateCardEntity card : rateCards) {
            long totalCost = card.getBaseRateAmount() + (card.getPerKgRateAmount() * (long) weightKg);
            LocalDate deliveryEta = LocalDate.now().plusDays(card.getEstimatedMaxDays());

            options.add(ShippingRateOptionDto.builder()
                    .carrierCode(card.getCarrierCode())
                    .serviceLevel(card.getServiceLevel())
                    .shippingCost(MoneyDto.of(totalCost, card.getCurrencyCode()))
                    .estimatedMinDays(card.getEstimatedMinDays())
                    .estimatedMaxDays(card.getEstimatedMaxDays())
                    .estimatedDeliveryDate(deliveryEta.toString())
                    .build());
        }

        return options;
    }

    /**
     * Retrieves consignment tracking details and milestone history.
     */
    @Transactional(readOnly = true)
    public ConsignmentTrackingResponse trackConsignment(String trackingNumber) {
        log.info("Tracking consignment for trackingNumber: {}", trackingNumber);

        ShippingConsignmentEntity consignment = shippingConsignmentRepository.findByTrackingNumberWithMilestones(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Consignment not found for tracking number: " + trackingNumber));

        return shippingMapper.toConsignmentTrackingResponse(consignment);
    }

    /**
     * Initializes and persists a shipping consignment for a confirmed customer order.
     */
    @Transactional
    @CircuitBreaker(name = "shippingCarrier")
    @Retry(name = "shippingCarrier")
    public ShippingConsignmentEntity createConsignment(OrderEntity order, String carrierCode, String serviceLevel, long costAmount) {
        String effectiveCarrier = (carrierCode != null && !carrierCode.isBlank()) ? carrierCode : "FEDEX";
        String trackingNumber = "TRK" + System.currentTimeMillis() + (int) (Math.random() * 900 + 100);

        log.info("Creating shipping consignment for order {}: carrier={}, tracking={}",
                order.getOrderNumber(), effectiveCarrier, trackingNumber);

        ShippingConsignmentEntity consignment = ShippingConsignmentEntity.builder()
                .order(order)
                .trackingNumber(trackingNumber)
                .carrierCode(effectiveCarrier)
                .consignmentStatus("MANIFEST_CREATED")
                .shippingCostAmount(costAmount)
                .currencyCode(order.getCurrencyCode())
                .estimatedDeliveryAt(Instant.now().plus(4, ChronoUnit.DAYS))
                .build();

        consignment.addMilestone(
                "MANIFEST_CREATED",
                "Main Central Fulfillment Warehouse",
                Instant.now(),
                "Shipping label generated. Order packed and awaiting carrier pickup."
        );

        return shippingConsignmentRepository.save(consignment);
    }
}
