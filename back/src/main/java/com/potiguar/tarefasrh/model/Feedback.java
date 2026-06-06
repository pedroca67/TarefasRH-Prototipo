package com.potiguar.tarefasrh.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tarefa_id", nullable = false)
    @JsonIgnore
    private Tarefa tarefa;

    @ManyToOne
    @JoinColumn(name = "gestor_id", nullable = false)
    private Usuario gestor;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    @Builder.Default
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao = LocalDateTime.now();
}
