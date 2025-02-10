package org.example.petcarebe.repository;

import org.example.petcarebe.model.Favorites;
import org.example.petcarebe.model.Products;
import org.example.petcarebe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoritesRepository extends JpaRepository<Favorites, Long> {
    List<Favorites> findByUser(User user);
    List<Favorites> findByProducts(Products products);
    Favorites findByUserAndProducts(User user, Products products);
}
