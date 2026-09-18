package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an operation is rejected due to the user account status
 * (e.g. SUSPENDED, LOCKED, PENDING_ACTIVATION, or DELETED).
 */
@Getter
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountStatusException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "ACCOUNT_INACTIVE";

    private final UUID userId;
    private final String accountStatus;

    public AccountStatusException(UUID userId, String accountStatus) {
        super(String.format("Access denied: User account is currently in state '%s'.", accountStatus),
                DEFAULT_ERROR_CODE, HttpStatus.FORBIDDEN);
        this.userId = userId;
        this.accountStatus = accountStatus;
    }

    public AccountStatusException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.FORBIDDEN);
        this.userId = null;
        this.accountStatus = null;
    }
}
