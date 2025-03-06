package org.example.petcarebe.repository;

import org.example.petcarebe.model.Point;
import org.example.petcarebe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByUser(User user);
    Optional<Point> findByPhone(String phone);
}