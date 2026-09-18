package com.bookcorner.service;

import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.user.CreateAddressRequest;
import com.bookcorner.dto.user.UpdateUserProfileRequest;
import com.bookcorner.dto.user.UserProfileResponse;
import com.bookcorner.entity.member.UserAddressEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.mapper.UserMapper;
import com.bookcorner.repository.member.UserAddressRepository;
import com.bookcorner.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer Account and Address Book Management Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserMapper userMapper;

    /**
     * Retrieves customer profile, assigned roles, and address book.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        log.info("Fetching profile for user ID: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return userMapper.toUserProfileResponse(user);
    }

    /**
     * Updates personal account profile details.
     */
    @Transactional
    public UserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        UserEntity saved = userRepository.save(user);
        return userMapper.toUserProfileResponse(saved);
    }

    /**
     * Retrieves saved customer addresses.
     */
    @Transactional(readOnly = true)
    public List<AddressDto> listUserAddresses(UUID userId) {
        log.info("Listing saved addresses for user ID: {}", userId);
        List<UserAddressEntity> addresses = userAddressRepository.findByUserId(userId);
        return userMapper.toAddressDtoList(addresses);
    }

    /**
     * Adds a new address to customer address book.
     */
    @Transactional
    public AddressDto createUserAddress(UUID userId, CreateAddressRequest request) {
        log.info("Adding new address for user ID: {}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (isDefault) {
            userAddressRepository.clearDefaultFlags(userId, request.getAddressType());
        }

        UserAddressEntity address = UserAddressEntity.builder()
                .user(user)
                .addressType(request.getAddressType() != null ? request.getAddressType().toUpperCase() : "SHIPPING")
                .isDefault(isDefault)
                .recipientName(request.getRecipientName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .streetAddress1(request.getStreetAddress1().trim())
                .streetAddress2(request.getStreetAddress2())
                .city(request.getCity().trim())
                .stateProvince(request.getStateProvince().trim())
                .postalCode(request.getPostalCode().trim())
                .countryCode(request.getCountryCode().trim().toUpperCase())
                .deliveryInstructions(request.getDeliveryInstructions())
                .build();

        UserAddressEntity saved = userAddressRepository.save(address);
        log.info("Saved address ID {} for user ID {}", saved.getId(), userId);
        return userMapper.toAddressDto(saved);
    }

    /**
     * Removes an address from customer address book.
     */
    @Transactional
    public void deleteUserAddress(UUID userId, UUID addressId) {
        log.info("Deleting address ID {} for user ID: {}", addressId, userId);
        UserAddressEntity address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));
        userAddressRepository.delete(address);
    }
}
