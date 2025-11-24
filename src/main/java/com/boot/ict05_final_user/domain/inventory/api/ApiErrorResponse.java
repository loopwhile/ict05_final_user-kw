package com.boot.ict05_final_user.domain.inventory.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    /** 애플리케이션 에러 코드(선택). 예: INV-400, INV-409 등 */
    private final String code;
    /** 사용자/프런트 표시 메시지 */
    private final String message;
    /** 요청 경로(선택) */
    private final String path;
    /** 발생 시각 */
    private final OffsetDateTime timestamp;
    /** 바인딩/검증 오류 상세(선택) */
    private final Object errors;

    private ApiErrorResponse(String code, String message, String path, Object errors) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.errors = errors;
        this.timestamp = OffsetDateTime.now();
    }

    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, path, null);
    }
    public static ApiErrorResponse of(String code, String message, String path, Object errors) {
        return new ApiErrorResponse(code, message, path, errors);
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public Object getErrors() { return errors; }
}
