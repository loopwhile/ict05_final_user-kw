package com.boot.ict05_final_user.domain.store.dto;

import com.boot.ict05_final_user.domain.store.entity.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 가맹점(매장) 목록 조회용 DTO.
 *
 * <p>가맹점 목록 조회 시 사용되는 데이터 전송 객체(DTO)이다.
 * 가맹점의 기본 정보를 포함한다.</p>
 *
 * <p>주요 필드:</p>
 * <ul>
 *     <li>storeId : 가맹점 고유 ID</li>
 *     <li>storeName : 매장명</li>
 *     <li>storeStatus : 매장 상태</li>
 *     <li>storePhone : 가맹점 연락처</li>
 *     <li>storeMonthlySales : 월 매출</li>
 *     <li>storeTotalEmployees : 총 직원 수</li>
 * </ul>
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoreListDTO  {

    /** 매장 고유 Id */
    private Long storeId;

    /** 가맹점명 */
    private String storeName;

    /** 운영 상태 */
    private StoreStatus storeStatus;

    /** 점주명 */
    private String staffName;

    /** 매장 연락처 */
    private String storePhone;

    /** 매장 월매출 */
    private BigDecimal storeMonthlySales;

    /** 매장 총 직원수 */
    private Integer storeTotalEmployees;


}
