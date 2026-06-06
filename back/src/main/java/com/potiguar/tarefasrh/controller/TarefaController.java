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
import java.time.LocalDateTime;
import java.util.Collections;
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
    private final com.potiguar.tarefasrh.repository.NotificacaoRepository notificacaoRepository;
    private final com.potiguar.tarefasrh.service.GoogleSheetsService googleSheetsService;

    private static final Map<String, Integer> PESO_ESFORCO = Map.of(
            "BAIXA", 1,
            "MEDIA", 3,
            "ALTA", 5
    );

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
                String previstoStr = body.get("previstoNoCargoColaborador");
                String concluidoPorId = body.get("concluidoPorId");
                
                if (evidencia == null || evidencia.trim().isEmpty()) {
                    return ResponseEntity.badRequest().<Tarefa>build();
                }
                
                t.setEvidencia(evidencia);
                t.setDataConclusao(LocalDateTime.now());
                
                // Registra quem concluiu de fato
                if (concluidoPorId != null) {
                    Usuario executor = usuarioRepository.findById(Long.parseLong(concluidoPorId)).orElse(null);
                    t.setConcluidoPor(executor);
                }
                
                // Salva a percepção do colaborador na conclusão
                if (previstoStr != null) {
                    t.setPrevistoNoCargoColaborador(Boolean.parseBoolean(previstoStr));
                }
            } else {
                t.setDataConclusao(null);
                t.setConcluidoPor(null);
            }
            t.setStatus(novoStatus);
            Tarefa salva = tarefaRepository.save(t);
            googleSheetsService.syncAllTasks();
            return ResponseEntity.ok(salva);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/feedback")
    @Transactional
    public ResponseEntity<Tarefa> salvarFeedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return tarefaRepository.findById(id).map(t -> {
            t.setFeedbackGestor(body.get("feedback"));
            t.setDataFeedback(LocalDateTime.now());
            Tarefa salva = tarefaRepository.save(t);

            // Gerar Notificações
            String msg = "O gestor deixou um feedback na tarefa: " + t.getTitulo();
            
            // Notifica todos os responsáveis
            t.getResponsaveis().forEach(u -> {
                notificacaoRepository.save(com.potiguar.tarefasrh.model.Notificacao.builder()
                        .tipo("FEEDBACK")
                        .mensagem(msg)
                        .usuario(u)
                        .referenciaId(t.getId())
                        .build());
            });

            // Se for time, notifica todos do time
            if (t.getTime() != null) {
                usuarioRepository.findAll().stream()
                        .filter(u -> u.getTime() != null && u.getTime().getId().equals(t.getTime().getId()))
                        .forEach(u -> {
                            notificacaoRepository.save(com.potiguar.tarefasrh.model.Notificacao.builder()
                                    .tipo("FEEDBACK")
                                    .mensagem(msg)
                                    .usuario(u)
                                    .referenciaId(t.getId())
                                    .build());
                        });
            }

            googleSheetsService.syncAllTasks();
            return ResponseEntity.ok(salva);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<Tarefa> todas = tarefaRepository.findAll();
        
        // Atualiza todas para contar corretamente (simplificado para protótipo)
        todas.forEach(t -> {
            if (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(LocalDate.now())) {
                if (t.getStatus() != Status.ATRASADA) {
                    t.setStatus(Status.ATRASADA);
                    tarefaRepository.save(t);
                }
            }
        });

        long esforcoTotal = todas.stream()
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        long esforcoConcluido = todas.stream()
                .filter(t -> t.getStatus() == Status.CONCLUIDA)
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        // Métrica macro baseada no Gestor (Expectativa)
        long aderenciaGestorSim = todas.stream()
                .filter(Tarefa::isPrevistoNoCargoGestor)
                .count();
        
        long aderenciaGestorNao = todas.stream()
                .filter(t -> !t.isPrevistoNoCargoGestor())
                .count();
        
        // Métrica macro baseada no Colaborador (Realidade)
        long aderenciaColabSim = todas.stream()
                .filter(t -> t.getPrevistoNoCargoColaborador() != null && t.getPrevistoNoCargoColaborador())
                .count();

        long totalHorasEst = usuarioRepository.countByAtivoTrue() * 40;
        long concluidasHorasEst = esforcoConcluido * 2;

        // Lógica do Ranking do Mês Atual
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        Map<Usuario, Long> pontosPorUsuario = new HashMap<>();
        
        todas.stream()
            .filter(t -> t.getStatus() == Status.CONCLUIDA)
            .filter(t -> t.getDataConclusao() != null && t.getDataConclusao().toLocalDate().isAfter(inicioMes.minusDays(1)))
            .forEach(t -> {
                Usuario executor = t.getConcluidoPor();
                if (executor == null && !t.getResponsaveis().isEmpty()) {
                    executor = t.getResponsaveis().get(0);
                }
                if (executor != null) {
                    long pontos = PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0);
                    pontosPorUsuario.put(executor, pontosPorUsuario.getOrDefault(executor, 0L) + pontos);
                }
            });

        List<Map<String, Object>> ranking = pontosPorUsuario.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("nome", e.getKey().getNome());
                    item.put("fotoUrl", e.getKey().getFotoUrl());
                    item.put("pontos", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", (long) todas.size());
        stats.put("pendente", todas.stream().filter(t -> t.getStatus() == Status.PENDENTE).count());
        stats.put("em_andamento", todas.stream().filter(t -> t.getStatus() == Status.EM_ANDAMENTO).count());
        stats.put("concluida", todas.stream().filter(t -> t.getStatus() == Status.CONCLUIDA).count());
        stats.put("atrasada", todas.stream().filter(t -> t.getStatus() == Status.ATRASADA).count());
        stats.put("total_times", timeRepository.count());
        stats.put("esforco_total", esforcoTotal);
        stats.put("esforco_concluido", esforcoConcluido);
        stats.put("aderencia_gestor_sim", aderenciaGestorSim);
        stats.put("aderencia_gestor_nao", aderenciaGestorNao);
        stats.put("aderencia_colab_sim", aderenciaColabSim);
        stats.put("total_horas_est", totalHorasEst);
        stats.put("concluidas_horas_est", concluidasHorasEst);
        stats.put("ranking", ranking);
        
        return stats;
    }
}
