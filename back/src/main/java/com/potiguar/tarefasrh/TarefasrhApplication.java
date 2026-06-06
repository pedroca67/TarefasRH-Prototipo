package com.potiguar.tarefasrh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class TarefasrhApplication {
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Fortaleza"));
    }

    public static void main(String[] args) {
        SpringApplication.run(TarefasrhApplication.class, args);

    }
}
