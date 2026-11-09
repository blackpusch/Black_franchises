package com.frank.franchises.infrastructure.adapter.in.web.error;

import com.frank.franchises.domain.exception.BadRequestException;
import com.frank.franchises.domain.exception.DuplicateResourceException;
import com.frank.franchises.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, ServerWebExchange exchange) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Ruta no encontrada: " + exchange.getRequest().getPath().value(),
                exchange
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex, ServerWebExchange exchange) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException ex, ServerWebExchange exchange) {
        log.warn("Conflicto de concurrencia agotados los reintentos en [{}]: {}", exchange.getRequest().getPath().value(), ex.getMessage());
        return buildResponse(
                HttpStatus.CONFLICT,
                "El recurso fue modificado concurrentemente por otra operación. Por favor, intente de nuevo",
                exchange
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String errors = ex.getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (errors.isBlank()) {
            errors = "Error en la validación de los datos enviados";
        }

        return buildResponse(HttpStatus.BAD_REQUEST, errors, exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleMalformedInput(ServerWebInputException ex, ServerWebExchange exchange) {
        log.warn("Entrada HTTP inválida o JSON malformado en [{}]: {}", exchange.getRequest().getPath().value(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Formato de petición JSON inválido o malformado", exchange);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleErrorResponseException(ErrorResponseException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String path = exchange.getRequest().getPath().value();
        if (status.is4xxClientError()) {
            log.warn("Error HTTP {} en [{}]: {}", status.value(), path, ex.getMessage());
        } else {
            log.error("Error HTTP {} en [{}]: {}", status.value(), path, ex.getMessage(), ex);
        }
        String message = switch (status) {
            case METHOD_NOT_ALLOWED -> "Método HTTP no permitido para esta ruta";
            case NOT_ACCEPTABLE -> "Formato de respuesta solicitado (Accept) no soportado";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido (Content-Type) no soportado";
            default -> status.is4xxClientError()
                    ? "Solicitud HTTP inválida"
                    : "Ocurrió un error interno en el servidor";
        };
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status)
                .headers(ex.getHeaders())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, ServerWebExchange exchange) {
        log.error("Excepción no controlada en [{}]: {}", exchange.getRequest().getPath().value(), ex.getMessage(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno en el servidor",
                exchange
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, ServerWebExchange exchange) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value()
        );
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
