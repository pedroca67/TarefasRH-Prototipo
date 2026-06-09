package com.potiguar.tarefasrh.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "codigo_funcionario", unique = true, length = 50)
    private String codigoFuncionario;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nivel nivel;

    @ManyToOne
    @JoinColumn(name = "time_id")
    private Time time;

    @Column(length = 100)
    private String loja;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Builder.Default
    private boolean ativo = true;

    @Column(name = "data_criacao", insertable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private java.time.LocalDateTime dataCriacao;

    @Column(name = "data_desativacao")
    private java.time.LocalDateTime dataDesativacao;
}
