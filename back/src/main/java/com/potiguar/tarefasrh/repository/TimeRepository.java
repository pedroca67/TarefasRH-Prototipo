package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Time;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeRepository extends JpaRepository<Time, Long> {
}
