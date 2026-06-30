package com.empik.coupon.adapter.rest;

import com.empik.coupon.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CouponNotFoundException.class)
    ProblemDetail handleNotFound(CouponNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Coupon Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(CouponExhaustedException.class)
    ProblemDetail handleExhausted(CouponExhaustedException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Coupon Exhausted", ex.getMessage(), request);
    }

    @ExceptionHandler(CountryNotAllowedException.class)
    ProblemDetail handleCountryNotAllowed(CountryNotAllowedException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Country Not Allowed", ex.getMessage(), request);
    }

    @ExceptionHandler(CouponAlreadyUsedException.class)
    ProblemDetail handleAlreadyUsed(CouponAlreadyUsedException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Coupon Already Used", ex.getMessage(), request);
    }

    @ExceptionHandler(CouponCodeAlreadyExistsException.class)
    ProblemDetail handleCodeExists(CouponCodeAlreadyExistsException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Coupon Code Already Exists", ex.getMessage(), request);
    }

    @ExceptionHandler(GeoLocationUnavailableException.class)
    ProblemDetail handleGeoUnavailable(GeoLocationUnavailableException ex, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Geo-location Unavailable", ex.getMessage(), request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Concurrent Update",
                "The resource was modified concurrently. Please retry your request.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation Failed", detail, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }
}
