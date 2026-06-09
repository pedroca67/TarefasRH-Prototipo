package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.model.Time;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findByResponsaveisContaining(Usuario usuario);
    List<Tarefa> findByTime(Time time);
    long countByStatus(Status status);

    @Query("SELECT COUNT(t) FROM Tarefa t WHERE t.status = 'PENDENTE' AND t.dataPrazo >= CURRENT_DATE " +
           "AND t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countPendentesNaoAtrasadas(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Tarefa t WHERE (t.status = 'ATRASADA' OR (t.status = 'PENDENTE' AND t.dataPrazo < CURRENT_DATE)) " +
           "AND t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countAtrasadas(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Tarefa t WHERE (t.status = 'ATRASADA' OR (t.status = 'PENDENTE' AND t.dataPrazo < CURRENT_DATE)) " +
           "AND t.dataCriacao >= :start AND t.dataCriacao <= :end ORDER BY t.dataPrazo ASC")
    List<Tarefa> findTopAtrasadas(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN FETCH t.time LEFT JOIN FETCH t.criadoPor LEFT JOIN FETCH t.concluidoPor")
    List<Tarefa> findTarefasForExport();

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.time")
    List<Usuario> findUsuariosForExport();

    @Query("SELECT YEAR(t.dataConclusao), MONTH(t.dataConclusao), " +
           "SUM(CASE WHEN t.complexidade = 'ALTA' THEN 5 WHEN t.complexidade = 'MEDIA' THEN 3 ELSE 1 END) * 3.0 " +
           "FROM Tarefa t WHERE t.status = 'CONCLUIDA' AND t.dataConclusao IS NOT NULL AND t.concluidoPor IS NOT NULL " +
           "GROUP BY YEAR(t.dataConclusao), MONTH(t.dataConclusao)")
    List<Object[]> findMonthlyEffortData();

    @Query("SELECT t.concluidoPor.nome, " +
           "SUM(CASE WHEN t.complexidade = 'ALTA' THEN 5 WHEN t.complexidade = 'MEDIA' THEN 3 ELSE 1 END) as pontos " +
           "FROM Tarefa t WHERE t.status = 'CONCLUIDA' AND t.concluidoPor IS NOT NULL " +
           "AND t.dataConclusao >= :start AND t.dataConclusao <= :end " +
           "GROUP BY t.concluidoPor.nome " +
           "ORDER BY pontos DESC")
    List<Object[]> findRankingData(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.complexidade = 'ALTA' THEN 5 WHEN t.complexidade = 'MEDIA' THEN 3 ELSE 1 END), 0) " +
           "FROM Tarefa t WHERE t.status = 'CONCLUIDA' AND t.concluidoPor IS NOT NULL AND t.dataConclusao >= :start AND t.dataConclusao <= :end")
    long sumEsforcoConcluido(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT t FROM Tarefa t LEFT JOIN t.time time WHERE " +
           "(:responsavelId IS NULL OR EXISTS (SELECT 1 FROM t.responsaveis r WHERE r.id = :responsavelId)) AND " +
           "(:timeId IS NULL OR time.id = :timeId) AND " +
           "(t.dataPrazo >= :start AND t.dataPrazo <= :end)")
    List<Tarefa> findForCalendario(@Param("responsavelId") Long responsavelId, @Param("timeId") Long timeId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
