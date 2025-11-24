package com.boot.ict05_final_user.domain.store.dto;

import com.boot.ict05_final_user.domain.store.entity.StoreStatus;
import com.boot.ict05_final_user.domain.store.entity.StoreType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 가맹점 상세 정보 DTO
 *
 * <p>가맹점 상세 조회 시 사용되는 데이터 전송 객체(DTO)이다.</p>
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
public class StoreDetailDTO {

    /** 매장 고유 Id */
    private Long storeId;

    /** 가맹점명 */
    private String storeName;

    /** 점주명 */
    private String staffName;

    /** 매장 사업자 등록번호 */
    private String businessRegistrationNumber;

    /** 매장 연락처 */
    private String storePhone;

    /** 매장 본사 담당자 */
    private String memberName;

    /** 이메일 */
    private String memberEmail;

    /** 사업장 주소 */
    private String storeLocation;

    /** 운영 상태 */
    private StoreStatus storeStatus;

    /** 매장 구분 (직영점/가맹점)*/
    private StoreType storeType;

    /** 매장 총 직원수 */
    private Integer storeTotalEmployees;

    /** 계약 시작일 */
    private LocalDate storeContractStartDate;

    /** 계약 가맹일 */
    private LocalDate storeContractAffiliateDate;

    /** 매장계약기간 */
    private int storeContractTerm;

    /** 가맹비 */
    private BigDecimal storeAffiliatePrice;

    /** 월매출 */
    private BigDecimal storeMonthlySales;

    /** 월 로열티 */
    private BigDecimal royalty;

    /** 특이사항 */
    private String comment;



}
