package com.community.healthcare.pharmacy.infrastructure;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 处方、库存、收费或医保状态与当前操作发生业务冲突。 */
@ResponseStatus(HttpStatus.CONFLICT)
public class R3ConflictException extends RuntimeException {
    public R3ConflictException(String message) { super(message); }
}
