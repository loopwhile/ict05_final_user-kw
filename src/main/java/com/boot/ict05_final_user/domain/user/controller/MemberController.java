// src/main/java/com/boot/ict05_final_user/api/MemberController.java
package com.boot.ict05_final_user.domain.user.controller;

import com.boot.ict05_final_user.api.dto.MemberSignupRequest;
import com.boot.ict05_final_user.domain.user.repository.MemberRepository;
import com.boot.ict05_final_user.domain.user.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    // 이메일/전화 중복 체크 (선택)
    @PostMapping("/member/exist-email")
    public ResponseEntity<Boolean> existEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return ResponseEntity.ok(memberRepository.existsByEmail(email));
    }

    @PostMapping("/member/exist-phone")
    public ResponseEntity<Boolean> existPhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        return ResponseEntity.ok(memberRepository.existsByPhone(phone));
    }

    // 회원가입
    @PostMapping("/member")
    public ResponseEntity<Map<String, Long>> signup(@Valid @RequestBody MemberSignupRequest req) {
        Long id = memberService.signup(req);
        return ResponseEntity.status(201).body(Map.of("memberId", id));
    }
}
