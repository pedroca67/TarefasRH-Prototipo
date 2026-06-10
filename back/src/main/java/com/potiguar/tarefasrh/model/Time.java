package com.potiguar.tarefasrh.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "time")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @OneToMany(mappedBy = "time")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Builder.Default
    private java.util.Set<Usuario> membros = new java.util.HashSet<>();
}
