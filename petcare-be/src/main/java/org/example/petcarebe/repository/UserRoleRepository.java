package org.example.petcarebe.repository;


import org.example.petcarebe.model.User;
import org.example.petcarebe.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    UserRole findByUser(User user);

    @Query("SELECT ur.role.roleName FROM UserRole ur WHERE ur.user = :user")
    List<String> findRoleNamesByUser(@Param("user") User user);
}
