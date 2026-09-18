package com.bookcorner.mapper;

import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.dto.shipping.ConsignmentTrackingResponse;
import com.bookcorner.dto.shipping.ShippingRateOptionDto;
import com.bookcorner.entity.shipping.CarrierRateCardEntity;
import com.bookcorner.entity.shipping.ShippingConsignmentEntity;
import com.bookcorner.entity.shipping.TrackingMilestoneEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Shipping consignments, milestones, and freight rate calculations.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface ShippingMapper {

    @Mapping(target = "milestones", source = "milestones")
    ConsignmentTrackingResponse toConsignmentTrackingResponse(ShippingConsignmentEntity entity);

    @Mapping(target = "status", source = "milestoneStatus")
    @Mapping(target = "location", source = "locationName")
    @Mapping(target = "timestamp", source = "milestoneTimestamp")
    @Mapping(target = "description", source = "milestoneDescription")
    ConsignmentTrackingResponse.MilestoneDto toMilestoneDto(TrackingMilestoneEntity entity);

    @Mapping(target = "shippingCost", source = "entity", qualifiedByName = "mapRateCost")
    @Mapping(target = "estimatedDeliveryDate", constant = "Calculated at dispatch")
    ShippingRateOptionDto toShippingRateOptionDto(CarrierRateCardEntity entity);

    List<ShippingRateOptionDto> toShippingRateOptionDtoList(List<CarrierRateCardEntity> entities);

    @Named("mapRateCost")
    default MoneyDto mapRateCost(CarrierRateCardEntity rateCard) {
        if (rateCard == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(rateCard.getBaseRateAmount(), rateCard.getCurrencyCode());
    }
}
