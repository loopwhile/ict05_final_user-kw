// src/main/java/.../domain/user/entity/Member.java
package com.boot.ict05_final_user.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")     // PK 컬럼명
    private Long id;

    @Column(name = "member_email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "member_password", nullable = false, length = 255)
    private String password;

    @Column(name = "member_name", nullable = false, length = 100)
    private String name;

    @Column(name = "member_phone", length = 20)
    private String phone;

    // 필요 시 created_at/updated_at 등 컬럼도 매핑

    @Column(name = "member_image_path")
    private String memberImagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;
}
