package com.potiguar.tarefasrh;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("Hash para 123456:");
        System.out.println(encoder.encode("123456"));

        System.out.println("\nHash para admin123:");
        System.out.println(encoder.encode("admin123"));
    }
}