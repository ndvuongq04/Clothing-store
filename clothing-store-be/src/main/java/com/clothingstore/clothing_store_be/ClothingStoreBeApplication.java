package com.clothingstore.clothing_store_be;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ClothingStoreBeApplication {

    public static void main(String[] args) {
        String profile = System.getenv("SPRING_PROFILE");
        if (profile == null) {
            profile = "dev";
        }

        String envFile = profile.equals("prod") ? ".env.production" : ".env";

        Dotenv dotenv = Dotenv.configure()
                .filename(envFile)
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(ClothingStoreBeApplication.class, args);
    }

}
