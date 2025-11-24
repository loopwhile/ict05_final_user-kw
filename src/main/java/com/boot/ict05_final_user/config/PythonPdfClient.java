package com.boot.ict05_final_user.config;

import com.boot.ict05_final_user.domain.analytics.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Python FastAPI 기반 PDF 서버와 동기(Blocking)로 통신하는 클라이언트.
 *
 * <p><b>역할</b>:
 * <ul>
 *   <li>분석 리포트(KPI/주문/메뉴/재료/시간·요일) PDF 생성 HTTP 호출 전담</li>
 *   <li>요청/응답 헤더 설정(JSON → PDF) 및 오류 래핑(IllegalStateException) 처리</li>
 * </ul>
 * </p>
 *
 * <p><b>경계/입출력</b>:
 * <ul>
 *   <li>입력: 각 리포트별 <code>*Payload</code> DTO(JSON 직렬화 가능)</li>
 *   <li>출력: PDF 바이트 배열(<code>byte[]</code>)</li>
 *   <li>전송 헤더: <code>Content-Type: application/json</code>, <code>Accept: application/pdf</code></li>
 * </ul>
 * </p>
 *
 * <p><b>설정</b>:
 * <ul>
 *   <li><code>pdf.python.base-url</code> (예: <code>http://localhost:8001</code>)</li>
 *   <li>엔드포인트:
 *     <ul>
 *       <li>KPI:         <code>POST /pdf/kpi-report</code></li>
 *       <li>주문:         <code>POST /pdf/orders</code></li>
 *       <li>메뉴:         <code>POST /pdf/menus</code></li>
 *       <li>시간·요일:     <code>POST /pdf/time-day</code></li>
 *       <li>재료:         <code>POST /pdf/material</code></li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 *
 * <p><b>에러/타임아웃</b>:
 * <ul>
 *   <li>HTTP 오류는 <code>WebClientResponseException</code> 로 수신하여 상태/바디를 로그 적재 후
 *       <code>IllegalStateException</code>으로 래핑해 전달</li>
 *   <li>네트워크/직렬화 등 기타 예외도 동일하게 래핑</li>
 *   <li>타임아웃은 WebClient 설정(커넥션/응답)으로 외부에서 관리 권장</li>
 * </ul>
 * </p>
 *
 * <p><b>보안</b>:
 * <ul>
 *   <li>내부망/인증된 PDF 서버 전제. 외부 노출 시 인증/서명/HTTPS 적용 권장</li>
 *   <li>대용량 응답(PDF) 처리 시 메모리 압력 주의</li>
 * </ul>
 * </p>
 *
 * <p>작성자: 이경욱 / 작성일: 2025-11-20</p>
 */
@Component
@Slf4j
public class PythonPdfClient {

    /** Base URL이 주입된 WebClient. 엔드포인트 경로는 각 메서드에서 지정. */
    private final WebClient webClient;

    /**
     * 생성자.
     *
     * @param baseUrl   PDF 서버 베이스 URL (예: http://localhost:8001)
     * @param builder   Spring WebClient 빌더
     */
    public PythonPdfClient(
            @Value("${pdf.python.base-url}") String baseUrl,
            WebClient.Builder builder
    ) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .build();
        log.info("[PythonPdfClient] baseUrl={}", baseUrl);
    }


    /**
     * 시간/요일 분석 리포트 PDF 생성 요청.
     *
     * @param payload 시간/요일 분석 페이로드 (요약/차트/테이블 포함)
     * @return PDF 바이트 배열
     * @throws IllegalStateException FastAPI 오류 또는 통신 예외 시
     */
    public byte[] requestTimeDayReport(TimeDayReportPayload payload) {
        try {
            return webClient.post()
                    .uri("/pdf/time-day") // FastAPI 라우트
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(); // 동기 처리
        } catch (WebClientResponseException e) {
            // 상태코드/응답바디를 함께 남겨 원인 분석 용이
            log.error("[PythonPdfClient] time-day report 실패 status={} body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("시간/요일 분석 PDF 생성 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[PythonPdfClient] time-day report 호출 중 예외", e);
            throw new IllegalStateException("시간/요일 분석 PDF 호출 중 예외 발생", e);
        }
    }


    /**
     * KPI 분석 리포트 PDF 생성 요청.
     *
     * @param payload KPI 페이로드
     * @return PDF 바이트 배열
     * @throws IllegalStateException FastAPI 오류 또는 통신 예외 시
     */
    public byte[] requestKpiReport(KpiPdfPayload payload) {
        try {
            return webClient.post()
                    .uri("/pdf/kpi-report")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[PythonPdfClient] kpi report 실패 status={} body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("KPI PDF 생성 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[PythonPdfClient] kpi report 호출 중 예외", e);
            throw new IllegalStateException("KPI PDF 호출 중 예외 발생", e);
        }
    }


    /**
     * 주문 분석 리포트 PDF 생성 요청.
     *
     * @param payload 주문 페이로드
     * @return PDF 바이트 배열
     * @throws IllegalStateException FastAPI 오류 또는 통신 예외 시
     */
    public byte[] requestOrdersReport(OrdersPdfPayload payload) {
        try {
            return webClient.post()
                    .uri("/pdf/orders")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[PythonPdfClient] orders report 실패 status={} body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("주문 분석 PDF 생성 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[PythonPdfClient] orders report 호출 중 예외", e);
            throw new IllegalStateException("주문 분석 PDF 호출 중 예외 발생", e);
        }
    }


    /**
     * 메뉴 분석 리포트 PDF 생성 요청.
     *
     * @param payload 메뉴 페이로드
     * @return PDF 바이트 배열
     * @throws IllegalStateException FastAPI 오류 또는 통신 예외 시
     */
    public byte[] requestMenusReport(MenuPdfPayload payload) {
        try {
            return webClient.post()
                    .uri("/pdf/menus")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[PythonPdfClient] menus report 실패 status={} body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("메뉴 분석 PDF 생성 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[PythonPdfClient] menus report 호출 중 예외", e);
            throw new IllegalStateException("메뉴 분석 PDF 호출 중 예외 발생", e);
        }
    }


    /**
     * 재료 분석 리포트 PDF 생성 요청.
     *
     * <p><b>주의</b>: FastAPI 라우트는 <code>/api</code> prefix가 없으며, 정확한 경로는 <code>/pdf/material</code>이다.</p>
     *
     * @param payload 재료 분석 페이로드
     * @return PDF 바이트 배열
     * @throws IllegalStateException FastAPI 오류 또는 통신 예외 시
     */
    public byte[] requestMaterialReport(MaterialReportPayload payload) {
        try {
            return webClient.post()
                    .uri("/pdf/material")  // ⚠️ "/api/pdf/material" 아님!
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_PDF)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("[PythonPdfClient] material report 실패 status={} body={}",
                    e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("재료 분석 PDF 생성 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[PythonPdfClient] material report 호출 중 예외", e);
            throw new IllegalStateException("재료 분석 PDF 호출 중 예외 발생", e);
        }
    }
}
