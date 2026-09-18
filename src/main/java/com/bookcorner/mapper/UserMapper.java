package com.bookcorner.mapper;

import com.bookcorner.dto.auth.AuthTokenResponse;
import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.user.CreateAddressRequest;
import com.bookcorner.dto.user.UpdateAddressRequest;
import com.bookcorner.dto.user.UserProfileResponse;
import com.bookcorner.entity.member.RoleEntity;
import com.bookcorner.entity.member.UserAddressEntity;
import com.bookcorner.entity.member.UserEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * MapStruct mapper for User and Address entities and DTOs.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface UserMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    @Mapping(target = "addresses", source = "addresses")
    UserProfileResponse toUserProfileResponse(UserEntity entity);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    AuthTokenResponse.UserSummary toUserSummary(UserEntity entity);

    AddressDto toAddressDto(UserAddressEntity entity);

    List<AddressDto> toAddressDtoList(List<UserAddressEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    UserAddressEntity toUserAddressEntity(CreateAddressRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "addressType", ignore = true)
    @Mapping(target = "countryCode", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateAddressFromRequest(UpdateAddressRequest request, @MappingTarget UserAddressEntity entity);

    @Named("mapRolesToStrings")
    default List<String> mapRolesToStrings(Set<RoleEntity> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(RoleEntity::getRoleCode)
                .toList();
    }
}
