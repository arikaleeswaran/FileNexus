package com.capstone.filenexus.repository;

import com.capstone.filenexus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleAndActiveTrue(com.capstone.filenexus.entity.enums.UserRole role);
}
