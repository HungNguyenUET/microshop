package com.microshop.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.UUID;

/**
 * Moi loi tra ve dang RFC 9457 Problem Details:
 * <pre>{"type":"about:blank","title":"Not Found","status":404,"detail":"Product ... not found","instance":"/api/products/..."}</pre>
 */
@RestControllerAdvice
public class ProductExceptions extends ResponseEntityExceptionHandler {

    public static class NotFound extends RuntimeException {
        public NotFound(UUID id) {
            super("Product " + id + " not found");
        }
    }

    public static class DuplicateSku extends RuntimeException {
        public DuplicateSku(String sku) {
            super("SKU " + sku + " already exists");
        }
    }

    @ExceptionHandler(NotFound.class)
    ProblemDetail notFound(NotFound ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateSku.class)
    ProblemDetail duplicate(DuplicateSku ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
