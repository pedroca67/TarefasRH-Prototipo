package com.potiguar.tarefasrh.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tarefa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Complexidade complexidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Categoria categoria = Categoria.OUTROS;

    @Column(name = "previsto_no_cargo_gestor")
    @Builder.Default
    private Boolean previstoNoCargoGestor = true;

    @Column(name = "previsto_no_cargo_colaborador")
    private Boolean previstoNoCargoColaborador;

    // Getters customizados para garantir tratamento de nulos
    public boolean isPrevistoNoCargoGestor() {
        return previstoNoCargoGestor == null || previstoNoCargoGestor;
    }

    public Boolean getPrevistoNoCargoColaborador() {
        return previstoNoCargoColaborador;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String evidencia;

    @Column(name = "data_prazo", nullable = false)
    private LocalDate dataPrazo;

    @Column(name = "data_criacao")
    @Builder.Default
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    @ManyToOne
    @JoinColumn(name = "concluido_por")
    private Usuario concluidoPor;

    @ManyToOne
    @JoinColumn(name = "criado_por")
    private Usuario criadoPor;

    @ManyToMany
    @JoinTable(
        name = "tarefa_responsavel",
        joinColumns = @JoinColumn(name = "tarefa_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    @Builder.Default
    private List<Usuario> responsaveis = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "time_id")
    private Time time;
}
