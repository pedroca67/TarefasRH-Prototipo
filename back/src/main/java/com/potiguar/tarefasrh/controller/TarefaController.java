package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.model.Time;
import com.potiguar.tarefasrh.model.Feedback;
import com.potiguar.tarefasrh.repository.TarefaRepository;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import com.potiguar.tarefasrh.repository.TimeRepository;
import com.potiguar.tarefasrh.repository.FeedbackRepository;
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
    private final FeedbackRepository feedbackRepository;
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

    private List<Tarefa> filtrarPorPeriodo(List<Tarefa> tarefas, LocalDate startDate, LocalDate endDate, boolean useCompletionDate) {
        if (startDate == null && endDate == null) return tarefas;
        return tarefas.stream().filter(t -> {
            boolean matches = true;
            if (useCompletionDate) {
                if (t.getDataConclusao() == null) return false;
                LocalDate dataConc = t.getDataConclusao().toLocalDate();
                if (startDate != null) matches = matches && dataConc.isAfter(startDate.minusDays(1));
                if (endDate != null) matches = matches && dataConc.isBefore(endDate.plusDays(1));
            } else {
                LocalDate dataRef = t.getDataCriacao().toLocalDate();
                LocalDate dataPrazo = t.getDataPrazo();
                if (startDate != null) matches = matches && (dataRef.isAfter(startDate.minusDays(1)) || dataPrazo.isAfter(startDate.minusDays(1)));
                if (endDate != null) matches = matches && (dataRef.isBefore(endDate.plusDays(1)) || dataPrazo.isBefore(endDate.plusDays(1)));
            }
            return matches;
        }).collect(Collectors.toList());
    }

    @GetMapping
    public List<Tarefa> listar(
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) Long timeId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
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
        
        tarefas = filtrarPorPeriodo(tarefas, startDate, endDate, false);
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
                
                if (concluidoPorId != null) {
                    Usuario executor = usuarioRepository.findById(Long.parseLong(concluidoPorId)).orElse(null);
                    t.setConcluidoPor(executor);
                }
                
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

    @PostMapping("/{id}/feedback")
    @Transactional
    public ResponseEntity<?> salvarFeedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            return tarefaRepository.findById(id).map(t -> {
                Object gestorIdObj = body.get("gestorId");
                if (gestorIdObj == null) {
                    return ResponseEntity.badRequest().body("ID do gestor não fornecido.");
                }
                
                Long gestorId = Long.valueOf(gestorIdObj.toString());
                Usuario gestor = usuarioRepository.findById(gestorId).orElseThrow();
                
                Feedback novoFeedback = Feedback.builder()
                        .tarefa(t)
                        .gestor(gestor)
                        .mensagem(body.get("feedback").toString())
                        .build();
                
                Feedback salvo = feedbackRepository.save(novoFeedback);

                String msg = "O gestor " + gestor.getNome() + " deixou um feedback na tarefa: " + t.getTitulo();
                
                t.getResponsaveis().forEach(u -> {
                    notificacaoRepository.save(com.potiguar.tarefasrh.model.Notificacao.builder()
                            .tipo("FEEDBACK")
                            .mensagem(msg)
                            .usuario(u)
                            .referenciaId(t.getId())
                            .build());
                });

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
                return ResponseEntity.ok(salvo);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao salvar feedback: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/feedbacks")
    public List<Feedback> listarFeedbacks(@PathVariable Long id) {
        Tarefa t = tarefaRepository.findById(id).orElseThrow();
        return feedbackRepository.findByTarefaOrderByDataCriacaoDesc(t);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean analyticalMode) {
        
        List<Tarefa> todasBase = tarefaRepository.findAll();
        todasBase = atualizarStatusAtrasadas(todasBase);

        // Para os cards de status (Pendente, Atrasada, etc), sempre usamos a lógica operacional
        List<Tarefa> tarefasStatus = filtrarPorPeriodo(todasBase, startDate, endDate, false);

        // Para as métricas de performance (Produtividade, Ranking, etc), usamos o modo analítico se solicitado
        List<Tarefa> tarefasPerformance = filtrarPorPeriodo(todasBase, startDate, endDate, analyticalMode);

        long esforcoTotal = tarefasStatus.stream()
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        long esforcoConcluido = tarefasPerformance.stream()
                .filter(t -> t.getStatus() == Status.CONCLUIDA)
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        long aderenciaGestorSim = tarefasPerformance.stream()
                .filter(Tarefa::isPrevistoNoCargoGestor)
                .count();
        
        long aderenciaGestorNao = tarefasPerformance.stream()
                .filter(t -> !t.isPrevistoNoCargoGestor())
                .count();
        
        long aderenciaColabSim = tarefasPerformance.stream()
                .filter(t -> t.getPrevistoNoCargoColaborador() != null && t.getPrevistoNoCargoColaborador())
                .count();

        long activeUsers = usuarioRepository.countByAtivoTrue();
        long totalHorasEst;
        if (startDate != null && endDate != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            // Base: 40h por semana (7 dias)
            totalHorasEst = activeUsers * (days * 40 / 7);
        } else {
            totalHorasEst = activeUsers * 40; // Padrão semanal se não houver filtro
        }
        
        long concluidasHorasEst = esforcoConcluido * 2;

        Map<Usuario, Long> pontosPorUsuario = new HashMap<>();
        tarefasPerformance.stream()
            .filter(t -> t.getStatus() == Status.CONCLUIDA)
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

        List<Map<String, Object>> turnoverData = new java.util.ArrayList<>();
        LocalDate hoje = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate mesReferencia = hoje.minusMonths(i);
            int mes = mesReferencia.getMonthValue();
            int ano = mesReferencia.getYear();

            long admissoes = usuarioRepository.findAll().stream()
                    .filter(u -> u.getDataCriacao() != null && u.getDataCriacao().getMonthValue() == mes && u.getDataCriacao().getYear() == ano)
                    .count();

            long desligamentos = usuarioRepository.findAll().stream()
                    .filter(u -> u.getDataDesativacao() != null && u.getDataDesativacao().getMonthValue() == mes && u.getDataDesativacao().getYear() == ano)
                    .count();

            Map<String, Object> turnoverMes = new HashMap<>();
            turnoverMes.put("mes", mesReferencia.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, new java.util.Locale("pt", "BR")));
            turnoverMes.put("admissoes", admissoes);
            turnoverMes.put("desligamentos", desligamentos);
            turnoverData.add(turnoverMes);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", (long) tarefasStatus.size());
        stats.put("pendente", tarefasStatus.stream().filter(t -> t.getStatus() == Status.PENDENTE).count());
        stats.put("em_andamento", tarefasStatus.stream().filter(t -> t.getStatus() == Status.EM_ANDAMENTO).count());
        stats.put("concluida", tarefasStatus.stream().filter(t -> t.getStatus() == Status.CONCLUIDA).count());
        stats.put("atrasada", tarefasStatus.stream().filter(t -> t.getStatus() == Status.ATRASADA).count());
        stats.put("total_times", timeRepository.count());
        stats.put("esforco_total", esforcoTotal);
        stats.put("esforco_concluido", esforcoConcluido);
        stats.put("aderencia_gestor_sim", aderenciaGestorSim);
        stats.put("aderencia_gestor_nao", aderenciaGestorNao);
        stats.put("aderencia_colab_sim", aderenciaColabSim);
        stats.put("total_horas_est", totalHorasEst);
        stats.put("concluidas_horas_est", concluidasHorasEst);
        stats.put("ranking", ranking);
        stats.put("turnover", turnoverData);
        
        return stats;
    }
}
