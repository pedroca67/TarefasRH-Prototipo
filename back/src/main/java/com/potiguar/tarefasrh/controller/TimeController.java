package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.model.Time;
import com.potiguar.tarefasrh.repository.TimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/times")
@RequiredArgsConstructor
public class TimeController {

    private final TimeRepository timeRepository;

    @GetMapping
    public List<Time> listar() {
        return timeRepository.findAll();
    }
}
