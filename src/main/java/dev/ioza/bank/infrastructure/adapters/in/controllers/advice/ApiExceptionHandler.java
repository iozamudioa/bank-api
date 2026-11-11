package dev.ioza.bank.infrastructure.adapters.in.controllers.advice;

import com.mongodb.MongoException;
import dev.ioza.bank.domain.exceptions.ConflictException;
import dev.ioza.bank.domain.exceptions.ForbiddenException;
import dev.ioza.bank.domain.exceptions.NotFoundException;
import dev.ioza.bank.domain.exceptions.ValidationException;
import dev.ioza.bank.infrastructure.adapters.in.controllers.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> internal(Exception e) {
    // Spring's ErrorResponse and our REST ErrorResponse share a simple name.
    if (e instanceof org.springframework.web.ErrorResponse frameworkError) {
      HttpStatus status = HttpStatus.resolve(frameworkError.getStatusCode().value());
      return response(
          status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status,
          "Solicitud HTTP no disponible para este recurso");
    }
    LOG.error("Error interno al procesar la solicitud", e);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Error interno; si ocurrió durante una operación financiera reintenta con la misma"
            + " Idempotency-Key");
  }

  private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new ErrorResponse(status.name(), message));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> missing(NotFoundException e) {
    return response(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> forbidden(ForbiddenException e) {
    return response(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> invalid(ValidationException e) {
    return response(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler({
    ConflictException.class,
    DuplicateKeyException.class,
    OptimisticLockingFailureException.class
  })
  public ResponseEntity<ErrorResponse> conflict(Exception e) {
    return response(
        HttpStatus.CONFLICT,
        e instanceof ConflictException
            ? e.getMessage()
            : "Conflicto de operación; reintenta con la misma Idempotency-Key. Si cambiaste el"
                + " contenido usa otra clave.");
  }

  @ExceptionHandler({DataAccessException.class, TransactionException.class})
  public ResponseEntity<ErrorResponse> unavailable(Exception e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof MongoException mongo
          && (mongo.hasErrorLabel("TransientTransactionError") || mongo.getCode() == 112)) {
        return response(
            HttpStatus.CONFLICT, "Conflicto concurrente; reintenta con la misma Idempotency-Key");
      }
    }
    return response(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Persistencia no disponible o confirmación incierta; reintenta con la misma"
            + " Idempotency-Key");
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MissingRequestHeaderException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorResponse> malformed(Exception e) {
    return response(HttpStatus.BAD_REQUEST, "Solicitud inválida o incompleta");
  }
}
