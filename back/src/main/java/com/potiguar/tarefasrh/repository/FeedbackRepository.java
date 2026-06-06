package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Feedback;
import com.potiguar.tarefasrh.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByTarefaOrderByDataCriacaoDesc(Tarefa tarefa);
}
