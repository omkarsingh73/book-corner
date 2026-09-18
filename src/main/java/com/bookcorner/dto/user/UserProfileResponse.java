package com.bookcorner.dto.user;

import com.bookcorner.dto.common.AddressDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detailed user profile response representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse implements Serializable {

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String accountStatus;
    private Instant lastLoginAt;
    private Instant createdAt;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Builder.Default
    private List<AddressDto> addresses = new ArrayList<>();
}
