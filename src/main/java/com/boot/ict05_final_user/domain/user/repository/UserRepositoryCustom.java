package com.boot.ict05_final_user.domain.user.repository;

import java.util.Map;
import java.util.Objects;

public interface UserRepositoryCustom {
    Long findStoreIdByUsername(String username);
    Long findMamberId(String username);
    String findMemberName(String username);
}
