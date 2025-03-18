package org.example.petcarebe.repository;


import org.example.petcarebe.model.CartDetails;
import org.example.petcarebe.model.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriesRepository extends JpaRepository<Categories, Long> {

}
