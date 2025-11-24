package com.boot.ict05_final_user.domain.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 가맹점 정보 찾는 DTO
 *
 * <p>가맹점 찾는 검색을 위한 데이터 전송 객체(DTO)이다.</p>
 *
 * <p>주요 필드:</p>
 * <ul>
 *
 * </ul>
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindStoreDTO {

    /** 매장 고유 Id */
    private Long storeId;

    /** 가맹점명 */
    private String storeName;
}
