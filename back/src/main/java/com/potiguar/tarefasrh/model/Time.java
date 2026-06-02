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
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;
}
