package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.model.Time;
import com.potiguar.tarefasrh.model.Complexidade;
import com.potiguar.tarefasrh.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT t FROM Tarefa t " +
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
            " (CAST(t.dataPrazo AS LocalDateTime) >= :startDate AND CAST(t.dataPrazo AS LocalDateTime) <= :endDate) OR " +
            " ((t.status = 'ATRASADA' OR (t.status = 'PENDENTE' AND t.dataPrazo < CURRENT_DATE)) AND CAST(t.dataPrazo AS LocalDateTime) <= :endDate))",
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
            " (CAST(t.dataPrazo AS LocalDateTime) >= :startDate AND CAST(t.dataPrazo AS LocalDateTime) <= :endDate) OR " +
            " ((t.status = 'ATRASADA' OR (t.status = 'PENDENTE' AND t.dataPrazo < CURRENT_DATE)) AND CAST(t.dataPrazo AS LocalDateTime) <= :endDate))")
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

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.status = :status AND t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countByStatusAndDataCriacaoBetween(@org.springframework.data.repository.query.Param("status") Status status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.status = 'PENDENTE' AND t.dataPrazo >= CURRENT_DATE " +
            "AND ((t.dataCriacao >= :start AND t.dataCriacao <= :end) OR (CAST(t.dataPrazo AS LocalDateTime) >= :start AND CAST(t.dataPrazo AS LocalDateTime) <= :end))")
    long countPendentesNaoAtrasadas(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE (t.status = 'ATRASADA' OR (t.status = 'PENDENTE' AND t.dataPrazo < CURRENT_DATE)) " +
            "AND ((t.dataCriacao >= :start AND t.dataCriacao <= :end) OR (CAST(t.dataPrazo AS LocalDateTime) >= :start AND CAST(t.dataPrazo AS LocalDateTime) <= :end))")
    long countAtrasadas(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.previstoNoCargoGestor = :previsto " +
            "AND ((t.dataCriacao >= :start AND t.dataCriacao <= :end) OR (CAST(t.dataPrazo AS LocalDateTime) >= :start AND CAST(t.dataPrazo AS LocalDateTime) <= :end))")
    long countByAderenciaGestor(@org.springframework.data.repository.query.Param("previsto") boolean previsto, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Tarefa t WHERE (t.status = 'ATRASADA' OR (t.status = 'PENDENTE' AND t.dataPrazo < CURRENT_DATE)) " +
            "AND ((t.dataCriacao >= :start AND t.dataCriacao <= :end) OR (CAST(t.dataPrazo AS LocalDateTime) >= :start AND CAST(t.dataPrazo AS LocalDateTime) <= :end)) ORDER BY t.dataPrazo ASC")
    List<Tarefa> findTopAtrasadas(java.time.LocalDateTime start, java.time.LocalDateTime end, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN FETCH t.time LEFT JOIN FETCH t.criadoPor LEFT JOIN FETCH t.concluidoPor")
    List<Tarefa> findTarefasForExport();

    @org.springframework.data.jpa.repository.Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.time")
    List<Usuario> findUsuariosForExport();

    @org.springframework.data.jpa.repository.Query("SELECT " +
            "YEAR(t.dataConclusao), MONTH(t.dataConclusao), " +
            "SUM(CASE WHEN t.complexidade = 'ALTA' THEN 5 WHEN t.complexidade = 'MEDIA' THEN 3 ELSE 1 END) * 3.0 " +
            "FROM Tarefa t WHERE t.status = 'CONCLUIDA' AND t.dataConclusao IS NOT NULL " +
            "GROUP BY YEAR(t.dataConclusao), MONTH(t.dataConclusao)")
    List<Object[]> findMonthlyEffortData();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(t.concluidoPor.nome, r.nome), SUM(CASE WHEN t.complexidade = 'ALTA' THEN 5 WHEN t.complexidade = 'MEDIA' THEN 3 ELSE 1 END) as pontos " +
            "FROM Tarefa t LEFT JOIN t.responsaveis r " +
            "WHERE t.status = 'CONCLUIDA' AND t.dataConclusao >= :start AND t.dataConclusao <= :end " +
            "GROUP BY COALESCE(t.concluidoPor.nome, r.nome) ORDER BY pontos DESC")
    List<Object[]> findRankingData(java.time.LocalDateTime start, java.time.LocalDateTime end, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(CASE WHEN t.complexidade = 'ALTA' THEN 5 WHEN t.complexidade = 'MEDIA' THEN 3 ELSE 1 END), 0) " +
            "FROM Tarefa t WHERE t.status = 'CONCLUIDA' AND t.dataConclusao >= :start AND t.dataConclusao <= :end")
    long sumEsforcoConcluido(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN t.time time WHERE " +
            "(:responsavelId IS NULL OR EXISTS (SELECT 1 FROM t.responsaveis r WHERE r.id = :responsavelId)) AND " +
            "(:timeId IS NULL OR time.id = :timeId) AND " +
            "(t.dataPrazo >= :start AND t.dataPrazo <= :end)")
    List<Tarefa> findForCalendario(@org.springframework.data.repository.query.Param("responsavelId") Long responsavelId, @org.springframework.data.repository.query.Param("timeId") Long timeId, @org.springframework.data.repository.query.Param("start") java.time.LocalDate start, @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);
}
