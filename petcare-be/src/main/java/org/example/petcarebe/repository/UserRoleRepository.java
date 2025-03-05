package org.example.petcarebe.repository;

import org.example.petcarebe.model.User;
import org.example.petcarebe.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    UserRole findByUser(User user);

    @Query("SELECT ur.role.roleName FROM UserRole ur WHERE ur.user = :user")
    List<String> findRoleNamesByUser(@Param("user") User user);

    // ✅ Kiểm tra số lượng role của một user (nếu >1 thì bị trùng)
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.user.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    // ✅ Xóa role bị trùng (giữ lại 1 dòng duy nhất)
    @Transactional
    @Modifying
    @Query(value = """
        DELETE FROM user_role 
        WHERE user_id = :userId 
        AND id NOT IN (
            SELECT id FROM user_role WHERE user_id = :userId LIMIT 1
        )
    """, nativeQuery = true)
    void deleteDuplicateRoles(@Param("userId") Long userId);
}