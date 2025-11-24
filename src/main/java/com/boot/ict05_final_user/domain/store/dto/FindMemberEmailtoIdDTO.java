package com.boot.ict05_final_user.domain.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindMemberEmailtoIdDTO {

    private Long id;

    private String email;
}
