package org.wow.backend.http.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = "org.wow")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

