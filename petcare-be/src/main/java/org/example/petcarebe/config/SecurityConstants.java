package org.example.petcarebe.config;

public class SecurityConstants {
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/google-login",
            "/api/facebook-login",
            "/api/auth/register",
            "/api/orders/**",
            "/api/addresses/**",
            "/api/products/getAllProducts",
            "/api/products/getAllProductss",
            "/api/statistics/best-selling-products",
            "/api/productDetails/**",
            "/css/**",
            "/js/**",
            "/ws/**",
            "/"
    };
}
