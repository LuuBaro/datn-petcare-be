package org.example.petcarebe.repository;

import jakarta.transaction.Transactional;
import org.example.petcarebe.model.User;
import org.springframework.context.annotation.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {


    User findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.userRoles r WHERE r.roleName = :role")
    List<User> findByRole(@Param("role") String role);

    @Query("SELECT u FROM User u")
    List<User> findAllWithoutPassword();

    // Cập nhật status của user
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isStatus = :status WHERE u.userId = :userId")
    void updateUserStatus(Long userId, boolean status);

    User findByPhone(String phone);
}
