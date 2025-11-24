package com.boot.ict05_final_user.domain.inventory.dto;


import com.boot.ict05_final_user.domain.inventory.entity.MaterialStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;


/**
 * 가맹점 재료 상태(사용/중지) 업데이트 요청 DTO
 *
 * <p>본사 재료 사용 토글에 대응한다.</p>
 *
 * @author …
 * @since 2025-11-20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreMaterialUpdateStatusRequest {

    /** 재료 상태 값: USE | STOP */
    @NotNull(message = "상태는 필수입니다.")
    private MaterialStatus status;
}