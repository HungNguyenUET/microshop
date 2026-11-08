package com.microshop.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class OrderExceptions extends ResponseEntityExceptionHandler {

    public static class NotFound extends RuntimeException {
        public NotFound(String message) {
            super(message);
        }
    }

    public static class InvalidRequest extends RuntimeException {
        public InvalidRequest(String message) {
            super(message);
        }
    }

    /** Vd: thanh toan don da PAID. */
    public static class Conflict extends RuntimeException {
        public Conflict(String message) {
            super(message);
        }
    }

    @ExceptionHandler(NotFound.class)
    ProblemDetail notFound(NotFound ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequest.class)
    ProblemDetail invalid(InvalidRequest ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(Conflict.class)
    ProblemDetail conflict(Conflict ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** product-service chet / qua cham. */
    @ExceptionHandler(RestClientException.class)
    ProblemDetail downstream(RestClientException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Khong goi duoc product-service: " + ex.getMessage());
    }
}
