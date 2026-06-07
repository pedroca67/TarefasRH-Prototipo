package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.model.Time;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findByResponsaveisContaining(Usuario usuario);
    List<Tarefa> findByTime(Time time);
    long countByStatus(Status status);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countByDataCriacaoBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM Tarefa t WHERE t.status = :status AND t.dataCriacao >= :start AND t.dataCriacao <= :end")
    long countByStatusAndDataCriacaoBetween(Status status, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
