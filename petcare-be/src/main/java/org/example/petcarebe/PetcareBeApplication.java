package org.example.petcarebe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // Kích hoạt scheduling
@SpringBootApplication
public class PetcareBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetcareBeApplication.class, args);
    }

}
