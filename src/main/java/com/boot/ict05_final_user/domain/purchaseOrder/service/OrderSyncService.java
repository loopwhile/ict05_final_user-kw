package com.boot.ict05_final_user.domain.purchaseOrder.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 가맹점 ↔ 본사 발주/수주 상태 동기화 서비스
 *
 * <p>가맹점에서 상태 변경 시 본사로 동기화 전송,
 * 본사에서 상태 변경 시 가맹점 DB 업데이트를 처리한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderSyncService {

    @Value("${hq.sync.url:http://localhost:8081/admin/API/receive/sync/status}")
    private String HQ_SYNC_URL;

    @Value("${hq.sync.secret:local-dev-secret}")
    private String HQ_SECRET;

    private final RestTemplate restTemplate;

    /**
     * 가맹점 → 본사 상태 동기화
     *
     * @param orderCode 발주 코드 (본사 수주 코드 동일)
     * @param status    변경된 상태 (예: RECEIVED, DELIVERED)
     */
    public void syncToHQ(String orderCode, String status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(HQ_SYNC_URL)
                .queryParam("orderCode", orderCode)
                .queryParam("status", status);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Sync-Auth", HQ_SECRET);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(builder.toUriString(), HttpMethod.PUT, entity, Void.class);
    }
}
