package com.boot.ict05_final_user.domain.myPage.service;

import com.boot.ict05_final_user.config.security.jwt.service.JwtService;
import com.boot.ict05_final_user.domain.myPage.dto.MyPageDTO;
import com.boot.ict05_final_user.domain.myPage.repository.MyPageRepository;
import com.boot.ict05_final_user.domain.staff.entity.StaffProfile;
import com.boot.ict05_final_user.domain.staff.repository.StaffRepository;
import com.boot.ict05_final_user.domain.user.entity.Member;
import com.boot.ict05_final_user.domain.user.entity.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 마이페이지 도메인의 비즈니스 로직을 담당하는 서비스 클래스.
 *
 * <p>
 * 주요 역할:
 * </p>
 * <ul>
 *     <li>회원/직원 정보를 기반으로 마이페이지 프로필 데이터 구성</li>
 *     <li>프로필 이미지 업로드, 조회, 초기화</li>
 *     <li>기본 프로필 정보 수정 (이름, 연락처 등)</li>
 *     <li>비밀번호 검증 및 변경</li>
 *     <li>회원 탈퇴 처리 및 JWT Refresh 토큰 정리</li>
 * </ul>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class MyPageService {

    private final MyPageRepository myPageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StaffRepository staffRepository;

    @Value("${file.upload-dir.profile}")
    private String profileImageDir;

    /**
     * 로그인한 회원의 마이페이지 정보를 조회한다.
     *
     * <p>
     * - 기본값은 Member 엔티티를 사용하고,<br>
     *   StaffProfile 이 존재하는 경우 staffName, staffEmail, 소속 점포명으로 오버라이드한다.<br>
     * - 점포(지점) 이름은 StaffProfile.store 기준으로 채운다.
     * </p>
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 화면에서 사용할 마이페이지 DTO
     * @throws AccessDeniedException 회원이 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    public MyPageDTO getMyPro(Long memberId) {

        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new AccessDeniedException("회원이 존재하지 않습니다."));

        StaffProfile staff = staffRepository.findByMember_Id(memberId).orElse(null);

        String name = member.getName();
        String email = member.getEmail();
        String phone = member.getPhone();
        String storeName = null;

        String memberImagePath = member.getMemberImagePath();

        if (staff != null) {
            if (staff.getStaffName() != null && !staff.getStaffName().isBlank()) {
                name = staff.getStaffName();
            }
            if (staff.getStaffEmail() != null && !staff.getStaffEmail().isBlank()) {
                email = staff.getStaffEmail();
            }
            if (staff.getStore() != null) {
                storeName = staff.getStore().getName();
            }
        }

        return MyPageDTO.builder()
                .id(member.getId())
                .name(name)
                .email(email)
                .phone(phone)
                .memberImagePath(memberImagePath)
                .storeName(storeName)
                .build();
    }

    /**
     * 프로필 이미지를 업로드하거나 변경한다.
     *
     * <p>
     * - 실제 파일은 file.upload-dir.profile 설정 경로에 저장된다.<br>
     * - DB 에는 파일명만 저장하며, 경로나 URL 은 저장하지 않는다.<br>
     * - 기존 파일 삭제는 이 메서드에서 다루지 않고, 필요 시 별도 정리 로직에서 처리할 수 있다.
     * </p>
     *
     * @param memberId   현재 로그인한 회원 ID
     * @param file       업로드할 Multipart 파일
     * @return 변경된 프로필 이미지 정보가 반영된 MyPageDTO
     * @throws IllegalArgumentException 업로드할 파일이 없거나 회원이 존재하지 않는 경우
     * @throws RuntimeException         파일 저장 중 IO 오류 발생 시
     */
    @Transactional
    public MyPageDTO updateProfileImage(Long memberId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        try {
            // 실제 저장 폴더
            Path uploadDir = Paths.get(profileImageDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 확장자 추출
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.lastIndexOf(".") != -1) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            // 파일명 예시: member_3_랜덤값.jpg
            String filename = "member_" + memberId + "_" + UUID.randomUUID() + ext;
            Path targetPath = uploadDir.resolve(filename);

            // 파일 저장
            file.transferTo(targetPath.toFile());

            // DB 에는 "파일 이름만" 저장 (경로/URL X)
            member.setMemberImagePath(filename);

            return MyPageDTO.fromEntity(member);
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회원 프로필 이미지 파일을 Resource 로 반환한다.
     *
     * <p>
     * - DB 에 memberImagePath 가 없거나, 실제 파일이 존재하지 않으면 null 을 반환한다.<br>
     * - 컨트롤러에서는 null 인 경우 404 를 반환하고 프론트는 기본 이미지를 사용한다.
     * </p>
     *
     * @param memberId 현재 로그인한 회원 ID
     * @return 프로필 이미지 Resource, 없으면 null
     * @throws IllegalArgumentException 회원이 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    public Resource loadProfileImage(Long memberId) {
        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        String imagePath = member.getMemberImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            // imagePath 자체가 파일 이름이라고 가정
            String filename = imagePath;

            Path uploadDir = Paths.get(profileImageDir).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(filename).normalize();

            if (!Files.exists(filePath)) {
                return null;
            }

            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            log.error("프로필 이미지 로드 중 오류", e);
            return null;
        }
    }

    /**
     * 회원의 프로필 이미지를 기본 상태로 초기화한다.
     *
     * <p>
     * - DB 의 memberImagePath 를 null 로 설정하여,<br>
     *   이후 조회 시 기본 이미지를 사용하도록 만든다.<br>
     * - 파일 시스템상의 실제 이미지 삭제는 별도 처리에 맡긴다.
     * </p>
     *
     * @param memberId 현재 로그인한 회원 ID
     * @throws IllegalArgumentException 회원이 존재하지 않을 경우
     */
    @Transactional
    public void resetProfileImage(Long memberId) {
        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // DB 에서 이미지 경로 제거 → 프론트에서 기본 이미지로 보이게 됨
        member.setMemberImagePath(null);
    }

    /**
     * 마이페이지 기본 정보를 수정한다.
     *
     * <p>
     * - 클라이언트가 보낸 id, email 은 무시하고 이름/전화번호/이미지 경로만 반영한다.<br>
     * - 실제 비즈니스 정책상 수정 가능한 필드를 이 메서드에서 제한한다.
     * </p>
     *
     * @param memberId 현재 로그인한 회원 ID
     * @param dto      수정 요청 DTO (이름, 전화번호, 이미지 경로 등)
     * @return 수정 결과가 반영된 MyPageDTO
     * @throws IllegalArgumentException 회원이 존재하지 않을 경우
     */
    @Transactional
    public MyPageDTO updateMyPage(Long memberId, MyPageDTO dto) {
        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 여기서 클라이언트가 보낸 id, email 은 사용하지 않는다
        if (dto.getName() != null) {
            member.setName(dto.getName());
        }
        if (dto.getPhone() != null) {
            member.setPhone(dto.getPhone());
        }
        if (dto.getMemberImagePath() != null) {
            member.setMemberImagePath(dto.getMemberImagePath());
        }

        return MyPageDTO.fromEntity(member);
    }

    /**
     * 비밀번호 변경
     *
     * <p>현재 비밀번호를 검증한 후 새 비밀번호를 암호화하여 저장한다.<br>
     *
     * @param memberId 로그인된 회원 ID
     * @param currentPassword 입력한 현재 비밀번호
     * @param newPassword 변경할 새 비밀번호
     * @throws IllegalArgumentException 현재 비밀번호 불일치 또는 회원 미존재 시 발생
     */
    //Spring Security의 {@link PasswordEncoder}를 사용한다.</p>
    @Transactional
    public void updatePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * 비밀번호 검증
     *
     * <p>입력된 현재 비밀번호가 실제 회원 비밀번호와 일치하는지 확인한다.</p>
     *
     * @param memberId 로그인된 회원 ID
     * @param currentPassword 입력된 비밀번호
     * @return 일치 여부 (true = 일치, false = 불일치)
     * @throws IllegalArgumentException 회원이 존재하지 않을 경우 발생
     */
    @Transactional(readOnly = true)
    public boolean checkCurrentPassword(Long memberId, String currentPassword) {
        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        return passwordEncoder.matches(currentPassword, member.getPassword());
    }

    @Transactional
    public void withdrawMember(Long memberId) {
        Member member = myPageRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("이미 탈퇴했거나 존재하지 않는 회원입니다."));

        // 상태 변경 (ACTIVE -> WITHDRAW)
        member.setStatus(MemberStatus.WITHDRAW);

        // 이메일 기준으로 refresh 토큰 삭제
        jwtService.removeRefreshUser(member.getEmail());
    }

}
