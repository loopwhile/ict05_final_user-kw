package com.boot.ict05_final_user.domain.myPage.controller;

import com.boot.ict05_final_user.config.security.principal.AppUser;
import com.boot.ict05_final_user.domain.myPage.dto.MyPageDTO;
import com.boot.ict05_final_user.domain.myPage.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * 마이페이지 관련 REST 컨트롤러.
 *
 * <p>
 * - 회원 기본 정보 조회 및 수정<br>
 * - 프로필 이미지 업로드/조회/초기화<br>
 * - 비밀번호 확인 및 변경<br>
 * - 회원 탈퇴 처리<br>
 * 기능을 제공한다.
 * </p>
 *
 * <p>
 * 모든 엔드포인트는 인증된 사용자 {@link AppUser} 컨텍스트를 기반으로
 * 자신의 정보만 조회/수정할 수 있도록 설계되어 있다.
 * </p>
 */
@RestController
@RequestMapping("/api")   // ⇒ 최종 경로: /user/api/...
@RequiredArgsConstructor
public class MyPageRestController {

    private final MyPageService myPageService;

    @Value("${file.upload-dir.profile}")
    private String profileImageDir;

    /**
     * 마이페이지 상세 정보를 조회한다.
     *
     * @param user 현재 로그인한 사용자 정보(스프링 시큐리티 Principal)
     * @return 회원 프로필 정보 DTO
     */
    @GetMapping("/myPage")
    @Operation(summary = "마이페이지 상세 조회", description = "회원의 프로필 정보를 조회한다.")
    public ResponseEntity<MyPageDTO> myPage(@AuthenticationPrincipal AppUser user) {
        MyPageDTO dto = myPageService.getMyPro(user.getMemberId());
        return ResponseEntity.ok(dto);
    }

    /**
     * 마이페이지 기본 정보를 수정한다.
     *
     * <p>
     * - 이름, 전화번호 등 기본 프로필 정보만 수정한다.<br>
     * - 비밀번호 변경은 별도의 엔드포인트에서 처리한다.
     * </p>
     *
     * @param user    현재 로그인한 사용자
     * @param request 수정할 프로필 정보
     * @return 수정 후 갱신된 프로필 정보
     */
    @PutMapping("/myPage")
    public ResponseEntity<MyPageDTO> updateMyPage(
            @AuthenticationPrincipal AppUser user,
            @RequestBody MyPageDTO request
    ) {
        MyPageDTO updated = myPageService.updateMyPage(user.getMemberId(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 프로필 이미지를 업로드한다.
     *
     * <p>
     * - 기존 이미지가 있을 경우 교체된다.<br>
     * - 저장 경로는 설정값 file.upload-dir.profile 을 따른다.
     * </p>
     *
     * @param user 현재 로그인한 사용자
     * @param file 업로드할 이미지 파일(Multipart)
     * @return 이미지 경로가 반영된 최신 프로필 정보
     */
    @PostMapping(
            value = "/myPage/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "프로필 이미지 업로드")
    public ResponseEntity<MyPageDTO> uploadProfileImage(
            @AuthenticationPrincipal AppUser user,
            @RequestPart("file") MultipartFile file
    ) {
        MyPageDTO updated = myPageService.updateProfileImage(user.getMemberId(), file);
        return ResponseEntity.ok(updated);
    }

    /**
     * 프로필 이미지를 반환한다.
     *
     * <p>
     * - 헤더 우측 아바타 영역에서 사용하는 이미지 조회용 엔드포인트이다.<br>
     * - 저장된 이미지가 없으면 404 를 반환하며, 프런트에서는 기본 이미지를 사용한다.
     * </p>
     *
     * @param user 현재 로그인한 사용자
     * @return 이미지 바이너리(Resource), 없으면 404
     * @throws IOException 파일 시스템에서 이미지 로딩 실패 시
     */
    @GetMapping("/myPage/profile-image")
    public ResponseEntity<Resource> getMyProfileImage(
            @AuthenticationPrincipal AppUser user
    ) throws IOException {

        Long memberId = user.getMemberId();
        Resource image = myPageService.loadProfileImage(memberId);

        if (image == null) {
            // 이미지 없으면 404  프론트에서는 기본 이미지로 처리
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(image.getFile().toPath());
        if (contentType == null) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image);
    }

    /**
     * 프로필 이미지를 기본 이미지 상태로 초기화한다.
     *
     * <p>
     * - 서버에 저장된 프로필 이미지 파일을 삭제하고 DB 상의 이미지 경로도 제거한다.<br>
     * - 이후 프런트에서는 기본 이미지를 사용하게 된다.
     * </p>
     *
     * @param user 현재 로그인한 사용자
     * @return 204 No Content
     */
    @DeleteMapping("/myPage/profile-image")
    @Operation(summary = "프로필 이미지 초기화", description = "저장된 프로필 이미지를 제거하고 기본 이미지로 되돌린다.")
    public ResponseEntity<Void> resetProfileImage(@AuthenticationPrincipal AppUser user) {
        myPageService.resetProfileImage(user.getMemberId());
        return ResponseEntity.noContent().build();   // 204
    }

    /**
     * 현재 비밀번호가 올바른지 확인한다.
     *
     * <p>
     * - 비밀번호 변경 전, 프런트에서 현재 비밀번호를 검증할 때 사용한다.<br>
     * - 비밀번호가 비어 있거나 일치하지 않으면 400 을 반환한다.
     * </p>
     *
     * @param user 현재 로그인한 사용자
     * @param body {"currentPassword": "..."} 형태의 JSON
     * @return 200 OK(성공) 또는 400 BAD_REQUEST(실패 메시지 포함)
     */
    @PostMapping("/myPage/check-password")
    public ResponseEntity<?> checkPassword(
            @AuthenticationPrincipal AppUser user,
            @RequestBody Map<String, String> body
    ) {
        String currentPassword = body.get("currentPassword");
        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "현재 비밀번호가 비어 있습니다."));
        }

        boolean ok = myPageService.checkCurrentPassword(user.getMemberId(), currentPassword);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
        }

        return ResponseEntity.ok().build();   // 200, body 없음
    }

    /**
     * 비밀번호를 변경한다.
     *
     * <p>
     * 처리 순서:
     * </p>
     * <ol>
     *     <li>현재 비밀번호와 새 비밀번호가 비어 있는지 검증</li>
     *     <li>서비스에서 현재 비밀번호 일치 여부 확인</li>
     *     <li>조건 만족 시 새 비밀번호로 변경</li>
     * </ol>
     *
     * @param user 현재 로그인한 사용자
     * @param body {"currentPassword": "...", "newPassword": "..."} 형태의 JSON
     * @return 200 OK 또는 400 BAD_REQUEST(유효성 실패 시)
     */
    @PostMapping("/myPage/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal AppUser user,
            @RequestBody Map<String, String> body
    ) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "비밀번호가 비어 있습니다."));
        }

        myPageService.updatePassword(user.getMemberId(), currentPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 탈퇴를 처리한다.
     *
     * <p>
     * - 회원 상태를 WITHDRAW 로 변경하고,<br>
     *   해당 회원의 Refresh 토큰을 삭제하여 재인증이 필요하도록 만든다.<br>
     * - 실제 물리 삭제는 수행하지 않고, 소프트 딜리트 형태로 관리한다.
     * </p>
     *
     * @param user 현재 로그인한 사용자
     * @return 204 No Content
     */
    @DeleteMapping("/myPage")
    @Operation(summary = "회원 탈퇴", description = "회원 상태를 WITHDRAW 로 변경하고 Refresh 토큰을 제거한다.")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal AppUser user) {

        myPageService.withdrawMember(user.getMemberId());

        return ResponseEntity.noContent().build();   // 204
    }

}
