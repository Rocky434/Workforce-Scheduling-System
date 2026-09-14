package com.example.scheduling.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    List<AppUser> findAllByRoleAndActiveTrueOrderByDisplayName(AppUser.Role role);
}

