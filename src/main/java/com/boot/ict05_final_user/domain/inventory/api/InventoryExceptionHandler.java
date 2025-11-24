package com.boot.ict05_final_user.domain.inventory.api;

import com.boot.ict05_final_user.domain.inventory.controller.StoreInventoryRestController;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 인벤토리 도메인 한정 글로벌 예외 핸들러.
 * - 스코프: StoreInventoryRestController 에만 적용
 */
@RestControllerAdvice(assignableTypes = {
        StoreInventoryRestController.class
})
public class InventoryExceptionHandler {

    /** 400: DTO 바인딩/검증 오류(@Valid) */
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<ApiErrorResponse> handleBind(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        ApiErrorResponse body = ApiErrorResponse.of(
                "INV-400",
                "요청 형식이 올바르지 않습니다.",
                currentPath(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** 400: 경로/쿼리 파라미터 검증 실패(@Validated + @Min 등) */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
                "INV-400",
                ex.getMessage(),  // 메시지 소스 없이 기본 메시지 사용
                currentPath()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** 404: 대상 없음 */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
                "INV-404",
                ex.getMessage(),
                currentPath()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** 409: 무결성 위반 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
                "INV-409",
                "데이터 무결성 위반으로 처리할 수 없습니다.",
                currentPath()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /** 400: 도메인 검증 실패(IllegalArgumentException) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
                "INV-400",
                ex.getMessage(),
                currentPath()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** 500: 기타 예외(최후 보루) — 필요 시 제거 가능 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleEtc(Exception ex) {
        ApiErrorResponse body = ApiErrorResponse.of(
                "INV-500",
                "서버에서 오류가 발생했습니다.",
                currentPath()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /** 요청 경로를 로그 MDC나 RequestContext에서 가져오고 싶다면 교체 */
    private String currentPath() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attrs != null && attrs.getRequest() != null)
                ? attrs.getRequest().getRequestURI()
                : null;
    }
}
