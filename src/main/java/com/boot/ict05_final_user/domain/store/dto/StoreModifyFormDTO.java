package com.boot.ict05_final_user.domain.store.dto;

import com.boot.ict05_final_user.domain.store.entity.StoreStatus;
import com.boot.ict05_final_user.domain.store.entity.StoreType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 가맹점 정보 수정 폼 DTO
 *
 * 가맹점 정보 수정 시 클라이언트에서 전달하는 값들을 담는다.
 * 수정 대상 식별자, 가맹점명, 사업자 등록번호, 매장 연락처, 운영 상태, 매장 구분, 사업장 주소,
 * 계약일, 가맹일, 매장계약기간, 가맹비, 월 매출, 월 로열티, 특이사항 등을 포함한다.
 *
 *
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreModifyFormDTO {

    /** 수정할 매장의 고유 Id */
    private Long storeId;

    /** 가맹점명 */
    @NotBlank(message = "가맹점명을 입력해주세요")
    @Size(max = 10, message = "가맹점명은 10자 이내로 입력해주세요")
    private String storeName;

    /** 매장 사업자 등록번호 */
    @NotBlank(message = "사업자등록번호를 입력해주세요")
    @Pattern(regexp = "^[0-9\\-]{9,13}$", message = "사업자등록번호는 숫자와 하이픈만 입력해주세요")
    private String businessRegistrationNumber;

    /** 매장 연락처 */
    @NotBlank(message = "매장 연락처를 입력해주세요")
    @Pattern(regexp = "^[0-9\\-]{9,13}$", message = "전화번호는 숫자와 하이픈만 입력해주세요")
    private String storePhone;

    /** 운영 상태 */
    @NotNull(message = "운영 상태를 선택해주세요")
    private StoreStatus storeStatus;

    /** 매장 구분 (직영점/가맹점)*/
    @NotNull(message = "매장을 선택해주세요")
    private StoreType storeType;

    @Size(max = 255, message = "주소는 255자 이내로 입력해주세요")
    private String userAddress1;

    @Size(max = 255, message = "주소는 255자 이내로 입력해주세요")
    private String userAddress2;

    /** 사업장 주소 */
    @Size(max = 255, message = "주소는 255자 이내로 입력해주세요")
    private String storeLocation;

    /** 총 직원 수 */
    @NotNull(message="총 직원수를 입력해주세요")
    @Min(value=1, message="총 직원수는 1 이상이어야 합니다")
    private Integer storeTotalEmployees;

    /** 계약일 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate storeContractStartDate;

    /** 가맹일 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate storeContractAffiliateDate;

    /** 매장계약기간 */
    @NotNull(message = "계약기간을 입력해주세요")
    @Min(value = 1, message = "계약기간은 1 이상이어야 합니다")
    private Integer storeContractTerm;

    /** 가맹비 */
    @NotNull(message = "가맹비를 입력해주세요")
    @Digits(integer = 14, fraction = 2, message = "가맹비는 소수점 둘째 자리까지 입력 가능합니다")
    @PositiveOrZero(message = "가맹비는 0 이상이어야 합니다")
    private BigDecimal storeAffiliatePrice;

    /** 월 매출 */
    @Digits(integer = 14, fraction = 2, message = "월 매출은 소수점 둘째 자리까지 입력 가능합니다")
    @PositiveOrZero(message = "월 매출은 0 이상이어야 합니다")
    private BigDecimal storeMonthlySales;

    /** 월 로열티 */
    @NotNull(message = "월 로열티를 입력해주세요")
    @Digits(integer = 14, fraction = 2, message = "월 로열티는 소수점 둘째 자리까지 입력 가능합니다")
    @PositiveOrZero(message = "월 로열티는 0 이상이어야 합니다")
    private BigDecimal royalty;

    /** 특이사항 */
    @Size(max = 1000, message = "특이사항은 1000자 이내로 입력해주세요")
    private String comment;

}
