package org.example.petcarebe.repository;

import org.example.petcarebe.model.User;
import org.springframework.context.annotation.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.userRoles r WHERE r.roleName = :role")
    List<User> findByRole(@Param("role") String role);


}
