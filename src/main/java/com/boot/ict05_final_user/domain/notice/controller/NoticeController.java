package com.boot.ict05_final_user.domain.notice.controller;

import com.boot.ict05_final_user.domain.notice.dto.NoticeListDTO;
import com.boot.ict05_final_user.domain.notice.dto.NoticeListResponseDTO;
import com.boot.ict05_final_user.domain.notice.dto.NoticeSearchDTO;
import com.boot.ict05_final_user.domain.notice.entity.Notice;
import com.boot.ict05_final_user.domain.notice.entity.NoticeAttachment;
import com.boot.ict05_final_user.domain.notice.service.NoticeAttachmentService;
import com.boot.ict05_final_user.domain.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 가맹점용 공지사항 조회 컨트롤러.
 *
 * <p>본 클래스는 가맹점 사용자(Store App) 측에서 접근 가능한 공지사항 API를 제공합니다.
 * HQ(본사)에서 등록한 공지 데이터를 페이징 목록 또는 단건 상세 형태로 조회할 수 있습니다.</p>
 *
 * <ul>
 *   <li>공지 목록 조회: 검색 조건 및 페이지네이션 지원</li>
 *   <li>공지 상세 조회: 첨부파일 포함 세부 내용 반환</li>
 * </ul>
 *
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
@Tag(name = "공지사항", description = "가맹점용 공지사항 API")
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeAttachmentService noticeAttachmentService;

    /**
     * 공지사항 목록 조회 API.
     *
     * <p>검색 조건({@link NoticeSearchDTO})과 페이지네이션({@link Pageable}) 정보를 기반으로
     * HQ 공지사항을 최신순으로 조회합니다.</p>
     *
     * @param noticeSearchDTO 검색 조건 DTO
     * @param pageable        페이지네이션 정보 (기본 1페이지, 10건)
     * @return 공지사항 목록 응답 DTO
     */
    @GetMapping("/list")
    @Operation(
            summary = "공지사항 목록 조회",
            description = "가맹점에서 HQ 공지사항 목록을 조회합니다. "
                    + "검색 조건 및 페이징 정보를 전달할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<NoticeListResponseDTO> getNoticeList(
            NoticeSearchDTO noticeSearchDTO,
            @PageableDefault(page = 1, size = 10, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable) {

        PageRequest pageRequest = PageRequest.of(
                pageable.getPageNumber() - 1,
                pageable.getPageSize(),
                Sort.by("id").descending()
        );

        NoticeListResponseDTO response = noticeService.selectAllOfficeNotice(noticeSearchDTO, pageRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 공지사항 상세 조회 API.
     *
     * <p>공지 ID를 통해 본문 및 첨부파일 정보를 함께 반환합니다.</p>
     *
     * @param id 공지사항 ID
     * @return 공지 엔티티와 첨부파일 목록을 포함하는 Map 응답
     */
    @GetMapping("/detail/{id}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "가맹점에서 특정 공지사항의 상세 내용을 조회합니다. "
                    + "공지 본문과 첨부파일 목록이 함께 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 공지사항이 존재하지 않음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> getNoticeDetail(@PathVariable Long id) {
        Notice notice = noticeService.detailNotice(id);
        if (notice == null) {
            return ResponseEntity.notFound().build();
        }

        List<NoticeAttachment> attachments = noticeAttachmentService.findByNoticeId(notice.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("notice", notice);
        response.put("attachments", attachments);

        return ResponseEntity.ok(response);
    }
}
