package com.boot.ict05_final_user.domain.fcm.repository;

import com.boot.ict05_final_user.domain.fcm.entity.FcmStoreSendLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * 가맹점용 FCM 발송 로그 리포지토리.
 *
 * <p>공지, 재고부족, 유통임박 등 각종 알림 발송 이력을 저장하고,
 * 중복 발송 방지를 위한 Dedupe 쿼리를 제공합니다.</p>
 *
 * <ul>
 *   <li>카테고리 + refType + refId 기준 중복 방지</li>
 *   <li>storeId + refDate 조합으로 하루 1회 제한</li>
 * </ul>
 *
 * @author 이경욱
 * @since 2025-11-20
 */
public interface FcmStoreSendLogRepository extends JpaRepository<FcmStoreSendLog, Long> {

    /**
     * 공지 중복 발송 여부 확인 (카테고리 + refType + refId)
     *
     * @param category 알림 카테고리
     * @param refType  참조 타입 (예: NOTICE)
     * @param refId    참조 ID (공지 PK 등)
     * @return 이미 발송된 경우 true
     */
    boolean existsByCategoryAndRefTypeAndRefId(String category, String refType, Long refId);

    /**
     * 재고/유통 알림의 일일 중복 방지 (카테고리 + 매장 + 기준일)
     *
     * @param category  알림 카테고리
     * @param storeIdFk 매장 ID
     * @param refDate   기준일
     * @return 이미 발송된 경우 true
     */
    boolean existsByCategoryAndStoreIdFkAndRefDate(String category, Long storeIdFk, LocalDate refDate);
}
