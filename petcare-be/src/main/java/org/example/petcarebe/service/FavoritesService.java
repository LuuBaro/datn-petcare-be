package org.example.petcarebe.service;

import org.example.petcarebe.model.Favorites;


import org.example.petcarebe.model.Products;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.FavoritesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class FavoritesService {
    private final FavoritesRepository favoritesRepository;

    @Autowired
    public FavoritesService(FavoritesRepository favoritesRepository) {this.favoritesRepository = favoritesRepository;}

    // Lấy tất cả các mục yêu thích của người dùng
    public List<Favorites> getFavoritesByUser(User user) {return  favoritesRepository.findByUser(user);}

    // Lấy tất cả các mục yêu thích cho một sản phẩm
    public List<Favorites> getFavouritesByProduct(Products products) {
        return favoritesRepository.findByProducts(products);
    }

    // Lấy được yêu thích cụ thể của người dùng và sản phẩm
    public Optional<Favorites> getFavouriteByUserAndProduct(User user, Products products) {
        return Optional.ofNullable(favoritesRepository.findByUserAndProducts(user,products));
    }

    public Favorites addOrUpdateFavourite(Favorites favourites) {
        System.out.println("Received favorite: " + favourites);

        Favorites existingFavourite = favoritesRepository.findByUserAndProducts(
                favourites.getUser(), favourites.getProducts()
        );

        if (existingFavourite != null) {
            existingFavourite.setLiked(favourites.isLiked());
            existingFavourite.setLike_date(favourites.getLike_date());
            existingFavourite.setUser(favourites.getUser());
            existingFavourite.setProducts(favourites.getProducts());

            System.out.println("Updating favorite: " + existingFavourite);
            return favoritesRepository.save(existingFavourite);
        }

        System.out.println("Saving new favorite: " + favourites);
        return favoritesRepository.save(favourites);
    }


    // Xóa mục yêu thích theo ID
    public void removeFavourite(Long id) {
        favoritesRepository.deleteById(id);
    }

    // Xóa một mục yêu thích cụ thể của người dùng và sản phẩm
    public void removeFavouriteByUserAndProduct(User user, Products products) {
        Favorites favourites = favoritesRepository.findByUserAndProducts(user, products);
        if (favourites != null) {
            favoritesRepository.delete(favourites);
        }
    }
}
