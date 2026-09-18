package com.bookcorner.security;

import com.bookcorner.entity.member.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetails principal representing an authenticated customer,
 * administrator, or anonymous guest session.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;

    @JsonIgnore
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;
    private final boolean isGuest;

    public static UserPrincipal create(UserEntity user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> {
                    String roleName = role.getRoleCode();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());

        if (authorities.isEmpty()) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        }

        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .active("ACTIVE".equalsIgnoreCase(user.getAccountStatus()))
                .isGuest(false)
                .build();
    }

    public static UserPrincipal createGuest(String guestSessionToken) {
        return UserPrincipal.builder()
                .id(UUID.nameUUIDFromBytes(guestSessionToken.getBytes()))
                .email("guest_" + guestSessionToken + "@guest.bookcorner.com")
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST")))
                .active(true)
                .isGuest(true)
                .build();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
