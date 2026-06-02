package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.model.Time;
import com.potiguar.tarefasrh.repository.TarefaRepository;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import com.potiguar.tarefasrh.repository.TimeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TimeRepository timeRepository;
    private final com.potiguar.tarefasrh.service.GoogleSheetsService googleSheetsService;

    private List<Tarefa> atualizarStatusAtrasadas(List<Tarefa> tarefas) {
        LocalDate hoje = LocalDate.now();
        return tarefas.stream().peek(t -> {
            if (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(hoje)) {
                t.setStatus(Status.ATRASADA);
            }
        }).collect(Collectors.toList());
    }

    @GetMapping
    public List<Tarefa> listar(@RequestParam(required = false) Long responsavelId, @RequestParam(required = false) Long timeId) {
        List<Tarefa> tarefas;
        if (responsavelId != null) {
            Usuario resp = usuarioRepository.findById(responsavelId).orElseThrow();
            tarefas = tarefaRepository.findByResponsaveisContaining(resp);
        } else if (timeId != null) {
            Time time = timeRepository.findById(timeId).orElseThrow();
            tarefas = tarefaRepository.findByTime(time);
        } else {
            tarefas = tarefaRepository.findAll();
        }
        return atualizarStatusAtrasadas(tarefas);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> criar(@RequestBody Tarefa tarefa) {
        if (!tarefa.getResponsaveis().isEmpty() && tarefa.getTime() != null) {
            return ResponseEntity.badRequest().body("Selecione apenas um tipo de atribuição.");
        }

        List<Usuario> responsaveisCompletos = tarefa.getResponsaveis().stream()
                .map(u -> usuarioRepository.findById(u.getId()).orElseThrow())
                .collect(Collectors.toList());
        tarefa.setResponsaveis(responsaveisCompletos);


        if (tarefa.getTime() != null && tarefa.getTime().getId() != null) {
            Time timeCompleto = timeRepository.findById(tarefa.getTime().getId()).orElseThrow();
            tarefa.setTime(timeCompleto);
        }

        Tarefa salva = tarefaRepository.saveAndFlush(tarefa);
        googleSheetsService.syncAllTasks();
        return ResponseEntity.ok(salva);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Tarefa> buscar(@PathVariable Long id) {
        return tarefaRepository.findById(id)
                .map(t -> {
                    if (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(LocalDate.now())) {
                        t.setStatus(Status.ATRASADA);
                    }
                    return ResponseEntity.ok(t);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Tarefa> atualizarStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return tarefaRepository.findById(id).map(t -> {
            Status novoStatus = Status.valueOf(body.get("status"));
            if (novoStatus == Status.CONCLUIDA) {
                String evidencia = body.get("evidencia");
                if (evidencia == null || evidencia.trim().isEmpty()) {
                    return ResponseEntity.badRequest().<Tarefa>build();
                }
                t.setEvidencia(evidencia);
            }
            t.setStatus(novoStatus);
            Tarefa salva = tarefaRepository.save(t);
            googleSheetsService.syncAllTasks();
            return ResponseEntity.ok(salva);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        // Atualiza todas para contar corretamente (simplificado para protótipo)
        tarefaRepository.findAll().forEach(t -> {
            if (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(LocalDate.now())) {
                if (t.getStatus() != Status.ATRASADA) {
                    t.setStatus(Status.ATRASADA);
                    tarefaRepository.save(t);
                }
            }
        });

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", tarefaRepository.count());
        stats.put("pendente", tarefaRepository.countByStatus(Status.PENDENTE));
        stats.put("em_andamento", tarefaRepository.countByStatus(Status.EM_ANDAMENTO));
        stats.put("concluida", tarefaRepository.countByStatus(Status.CONCLUIDA));
        stats.put("atrasada", tarefaRepository.countByStatus(Status.ATRASADA));
        stats.put("total_times", timeRepository.count());
        return stats;
    }
}
