package com.boot.ict05_final_user.domain.user.controller;

import com.boot.ict05_final_user.domain.user.dto.UserRequestDTO;
import com.boot.ict05_final_user.domain.user.dto.UserResponseDTO;
import com.boot.ict05_final_user.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
//@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
// 클래스에 @RequestMapping("/user") 쓰지 않음 (context-path=/user가 이미 있음)
public class UserController {

    private final UserService userService;

    // POST /user/exist
    @PostMapping(value = "/exist", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> existUserApi(@Validated(UserRequestDTO.existGroup.class) @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.existUser(dto));
    }

    // POST /user  (회원가입)
    @PostMapping(value = "/join", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(@Validated(UserRequestDTO.addGroup.class) @RequestBody UserRequestDTO dto) {
        Long id = userService.addUser(dto);
        return ResponseEntity.status(201).body(Collections.singletonMap("userEntityId", id));
    }

    //  GET /user/me  (세션 확인용)
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDTO userMeApi() {
        return userService.readUser();  // 인증 실패 시 Security가 401
    }

    // PUT /user/me
    @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUserApi(@Validated(UserRequestDTO.updateGroup.class) @RequestBody UserRequestDTO dto) throws AccessDeniedException {
        return ResponseEntity.ok(userService.updateUser(dto));
    }

    // DELETE /user/me
    @DeleteMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> deleteUserApi(@Validated(UserRequestDTO.deleteGroup.class) @RequestBody UserRequestDTO dto) throws AccessDeniedException {
        userService.deleteUser(dto);
        return ResponseEntity.ok(true);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutApi() throws AccessDeniedException {
        userService.logout();
        return ResponseEntity.ok().build();
    }

}

