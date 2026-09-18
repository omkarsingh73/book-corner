package com.bookcorner.dto.shipping;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Calculated shipping carrier option and ETA representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateOptionDto implements Serializable {

    private String carrierCode;
    private String serviceLevel;
    private MoneyDto shippingCost;
    private Integer estimatedMinDays;
    private Integer estimatedMaxDays;
    private String estimatedDeliveryDate;
}
