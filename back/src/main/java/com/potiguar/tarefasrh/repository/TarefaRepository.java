package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.model.Time;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT t FROM Tarefa t " +
            "LEFT JOIN FETCH t.responsaveis " +
            "LEFT JOIN FETCH t.time " +
            "LEFT JOIN FETCH t.criadoPor " +
            "WHERE " +
            "(:responsavelId IS NULL OR EXISTS (SELECT r FROM t.responsaveis r WHERE r.id = :responsavelId)) AND " +
            "(:timeId IS NULL OR t.time.id = :timeId) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:complexidade IS NULL OR t.complexidade = :complexidade) AND " +
            "(:categoria IS NULL OR t.categoria = :categoria) AND " +
            "(:search IS NULL OR LOWER(t.titulo) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:startDate IS NULL OR :endDate IS NULL OR " +
            " (t.dataCriacao >= :startDate AND t.dataCriacao <= :endDate) OR " +
            " (t.dataPrazo >= :startDate AND t.dataPrazo <= :endDate))",
            countQuery = "SELECT COUNT(DISTINCT t) FROM Tarefa t " +
            "LEFT JOIN t.responsaveis r " +
            "WHERE " +
            "(:responsavelId IS NULL OR r.id = :responsavelId) AND " +
            "(:timeId IS NULL OR t.time.id = :timeId) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:complexidade IS NULL OR t.complexidade = :complexidade) AND " +
            "(:categoria IS NULL OR t.categoria = :categoria) AND " +
            "(:search IS NULL OR LOWER(t.titulo) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:startDate IS NULL OR :endDate IS NULL OR " +
            " (t.dataCriacao >= :startDate AND t.dataCriacao <= :endDate) OR " +
            " (t.dataPrazo >= :startDate AND t.dataPrazo <= :endDate))")
    org.springframework.data.domain.Page<Tarefa> findComFiltros(
            @org.springframework.data.repository.query.Param("responsavelId") Long responsavelId,
            @org.springframework.data.repository.query.Param("timeId") Long timeId,
            @org.springframework.data.repository.query.Param("status") Status status,
            @org.springframework.data.repository.query.Param("complexidade") Complexidade complexidade,
            @org.springframework.data.repository.query.Param("categoria") Categoria categoria,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable);
    long countByStatus(Status status);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countByDataCriacaoBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.status = 'PENDENTE' AND t.dataPrazo >= CURRENT_DATE AND t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countPendentesNaoAtrasadas(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE (t.status = 'ATRASADA' OR (t.status = 'PENDENTE' AND t.dataPrazo < CURRENT_DATE)) AND t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countAtrasadas(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.status = :status AND t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countByStatusAndDataCriacaoBetween(Status status, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
