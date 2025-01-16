package org.example.petcarebe.config;

import org.example.petcarebe.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthFilter jwtAuthFilter;

    // Apply @Lazy here to break the circular dependency
    public SecurityConfig(@Lazy UserService userService, JwtAuthFilter jwtAuthFilter) {
        this.userService = userService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthFilter.setUserService(userService); // Set UserService in JwtAuthFilter
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/css/**",
                                "/js/**",
                                "/").permitAll()
                        .requestMatchers("/api/users/create").hasRole("ADMIN")
                        .requestMatchers("/api/users/update").hasAnyRole("STAFF","ADMIN")
                        .requestMatchers("/api/roles/list","/api/roles/find-by-id").hasAnyRole("VIEW_ROLE","ADMINISTRATOR")
                        .requestMatchers("/api/roles/create").hasAnyRole("CREATE_ROLE","ADMINISTRATOR")
                        .requestMatchers("/api/roles/update").hasAnyRole("UPDATE_ROLE","ADMINISTRATOR")
                        .requestMatchers("/api/departments/list","/api/departments/get-department").hasAnyRole("VIEW_DEPARTMENT","ADMINISTRATOR")
                        .requestMatchers("/api/departments/create").hasAnyRole("CREATE_DEPARTMENT","ADMINISTRATOR")
                        .requestMatchers("/api/departments/update").hasAnyRole("UPDATE_DEPARTMENT","ADMINISTRATOR")
                        .requestMatchers("/api/specialties/list","/api/specialties/find-by-id").hasAnyRole("VIEW_SPECIALTY","ADMINISTRATOR")
                        .requestMatchers("/api/specialties/create").hasAnyRole("CREATE_SPECIALTY","ADMINISTRATOR")
                        .requestMatchers("/api/specialties/update").hasAnyRole("UPDATE_SPECIALTY","ADMINISTRATOR")
                )
                // TODO: properly configure the security
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .logout((logout) -> logout
                        .logoutSuccessUrl("/home")
                        .permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authenticationProvider=new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
