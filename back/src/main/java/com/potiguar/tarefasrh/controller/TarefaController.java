package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.model.Time;
import com.potiguar.tarefasrh.model.Feedback;
import com.potiguar.tarefasrh.model.Complexidade;
import com.potiguar.tarefasrh.model.Categoria;
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
            if (t.getStatus() == Status.PENDENTE && t.getDataPrazo().isBefore(hoje)) {
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

    private Map<String, Object> emptyPaginatedResponse(int page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", Collections.emptyList());
        response.put("currentPage", page);
        response.put("totalItems", 0L);
        response.put("totalPages", 0);
        return response;
    }

    @GetMapping
    public Map<String, Object> listar(
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) Long timeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Complexidade complexidade,
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
            
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("dataCriacao").descending());
            
            org.springframework.data.domain.Page<Tarefa> pageResult = tarefaRepository.findComFiltros(
                    responsavelId, timeId, status, complexidade, categoria, 
                    (search != null && !search.isBlank()) ? search : null, 
                    startDateTime, endDateTime, pageable);

            // Atualização de status em tempo de execução (apenas visual para o retorno)
            LocalDate hoje = LocalDate.now();
            pageResult.getContent().forEach(t -> {
                if (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(hoje)) {
                    t.setStatus(Status.ATRASADA);
                }
            });

            Map<String, Object> response = new HashMap<>();
            response.put("content", pageResult.getContent());
            response.put("currentPage", pageResult.getNumber());
            response.put("totalItems", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());

            return response;
        } catch (Exception e) {
            return emptyPaginatedResponse(page);
        }
    }

    @PostMapping("/sync-sheets")
    public ResponseEntity<?> forceSyncSheets() {
        googleSheetsService.syncAllTasks();
        return ResponseEntity.ok(Map.of("message", "Sincronização iniciada com sucesso."));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> criar(@RequestBody Tarefa tarefa) {
        if (tarefa.getResponsaveis() != null && !tarefa.getResponsaveis().isEmpty() && tarefa.getTime() != null && tarefa.getTime().getId() != null) {
            return ResponseEntity.badRequest().body("Selecione apenas um tipo de atribuição.");
        }
        if (tarefa.getResponsaveis() != null) {
            java.util.Set<Usuario> responsaveisCompletos = tarefa.getResponsaveis().stream()
                    .map(u -> usuarioRepository.findById(u.getId()).orElseThrow())
                    .collect(Collectors.toSet());
            tarefa.setResponsaveis(responsaveisCompletos);
        }
        if (tarefa.getTime() != null && tarefa.getTime().getId() != null) {
            Time timeCompleto = timeRepository.findById(tarefa.getTime().getId()).orElseThrow();
            tarefa.setTime(timeCompleto);
        }
        Tarefa salva = tarefaRepository.saveAndFlush(tarefa);
        googleSheetsService.syncAllTasks();
        return ResponseEntity.ok(salva);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tarefa> buscar(@PathVariable Long id, @RequestParam(required = false) Long usuarioId) {
        return tarefaRepository.findById(id)
                .map(t -> {
                    // IDOR Mitigation: Basic check if user is associated with the task or is a manager
                    if (usuarioId != null) {
                        Usuario user = usuarioRepository.findById(usuarioId).orElse(null);
                        if (user != null && user.getNivel() != com.potiguar.tarefasrh.model.Nivel.GESTOR) {
                            boolean isResponsavel = t.getResponsaveis().stream().anyMatch(r -> r.getId().equals(usuarioId));
                            boolean isDoTime = t.getTime() != null && user.getTime() != null && t.getTime().getId().equals(user.getTime().getId());
                            if (!isResponsavel && !isDoTime) {
                                return ResponseEntity.status(403).<Tarefa>build();
                            }
                        }
                    }

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
                if (evidencia == null || evidencia.trim().isEmpty()) return ResponseEntity.badRequest().<Tarefa>build();
                t.setEvidencia(evidencia);
                t.setDataConclusao(LocalDateTime.now());
                if (concluidoPorId != null) {
                    Usuario executor = usuarioRepository.findById(Long.parseLong(concluidoPorId)).orElse(null);
                    t.setConcluidoPor(executor);
                }
                if (previstoStr != null) t.setPrevistoNoCargoColaborador(Boolean.parseBoolean(previstoStr));
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
                if (gestorIdObj == null) return ResponseEntity.badRequest().body("ID do gestor não fornecido.");
                Long gestorId = Long.valueOf(gestorIdObj.toString());
                Usuario gestor = usuarioRepository.findById(gestorId).orElseThrow();
                Feedback novoFeedback = Feedback.builder().tarefa(t).gestor(gestor).mensagem(body.get("feedback").toString()).build();
                Feedback salvo = feedbackRepository.save(novoFeedback);
                String msg = "O gestor " + gestor.getNome() + " deixou um feedback na tarefa: " + t.getTitulo();
                t.getResponsaveis().forEach(u -> {
                    notificacaoRepository.save(com.potiguar.tarefasrh.model.Notificacao.builder().tipo("FEEDBACK").mensagem(msg).usuario(u).referenciaId(t.getId()).build());
                });
                if (t.getTime() != null) {
                    usuarioRepository.findAll().stream().filter(u -> u.getTime() != null && u.getTime().getId().equals(t.getTime().getId()))
                            .forEach(u -> {
                                notificacaoRepository.save(com.potiguar.tarefasrh.model.Notificacao.builder().tipo("FEEDBACK").mensagem(msg).usuario(u).referenciaId(t.getId()).build());
                            });
                }
                googleSheetsService.syncAllTasks();
                return ResponseEntity.ok(salvo);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao salvar feedback: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/feedbacks")
    public List<Feedback> listarFeedbacks(@PathVariable Long id) {
        Tarefa t = tarefaRepository.findById(id).orElseThrow();
        return feedbackRepository.findByTarefaOrderByDataCriacaoDesc(t);
    }

    @GetMapping("/calendario")
    public List<Map<String, Object>> getCalendario(
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) Long timeId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<Tarefa> tarefas = tarefaRepository.findForCalendario(responsavelId, timeId, start, end);

        LocalDate hoje = LocalDate.now();

        return tarefas.stream().map(t -> {
            Map<String, Object> event = new HashMap<>();
            event.put("id", t.getId());
            event.put("title", t.getTitulo());
            event.put("start", t.getDataPrazo().toString());
            
            Status currentStatus = t.getStatus();
            if (currentStatus != Status.CONCLUIDA && t.getDataPrazo().isBefore(hoje)) {
                currentStatus = Status.ATRASADA;
            }
            
            event.put("status", currentStatus.toString());
            return event;
        }).collect(Collectors.toList());
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean analyticalMode) {
        
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusYears(10);
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        Map<String, Object> stats = new HashMap<>();
        long pendentes = tarefaRepository.countPendentesNaoAtrasadas(startDateTime, endDateTime);
        long concluidas = tarefaRepository.countByStatusAndDataCriacaoBetween(Status.CONCLUIDA, startDateTime, endDateTime);
        long atrasadas = tarefaRepository.countAtrasadas(startDateTime, endDateTime);
        long emAndamento = tarefaRepository.countByStatusAndDataCriacaoBetween(Status.EM_ANDAMENTO, startDateTime, endDateTime);

        stats.put("pendente", pendentes);
        stats.put("concluida", concluidas);
        stats.put("atrasada", atrasadas);
        stats.put("em_andamento", emAndamento);
        stats.put("total", pendentes + concluidas + atrasadas + emAndamento);
        
        stats.put("aderencia_gestor_sim", tarefaRepository.countByAderenciaGestor(true, startDateTime, endDateTime));
        stats.put("aderencia_gestor_nao", tarefaRepository.countByAderenciaGestor(false, startDateTime, endDateTime));

        stats.put("total_times", timeRepository.count());

        // --- Top Atrasadas para o Dashboard ---
        stats.put("topAtrasadas", tarefaRepository.findTopAtrasadas(startDateTime, endDateTime, org.springframework.data.domain.PageRequest.of(0, 5)));

        // --- Ranking Otimizado ---
        List<Object[]> rankingRaw = tarefaRepository.findRankingData(startDateTime, endDateTime, org.springframework.data.domain.PageRequest.of(0, 5));
        List<Map<String, Object>> ranking = rankingRaw.stream().map(row -> {
            Map<String, Object> item = new HashMap<>();
            item.put("nome", row[0]);
            item.put("pontos", row[1]);
            return item;
        }).collect(Collectors.toList());
        stats.put("ranking", ranking);

        // --- Esforço Concluído ---
        long esforcoConcluido = tarefaRepository.sumEsforcoConcluido(startDateTime, endDateTime);
        stats.put("esforco_concluido", esforcoConcluido);
        stats.put("concluidas_horas_est", esforcoConcluido * 3);
        
        // Capacity calculation (remains somewhat manual for now but optimized)
        List<Usuario> todosUsuarios = usuarioRepository.findAll();
        double totalHorasEst = 0;
        for (Usuario u : todosUsuarios) {
            LocalDate admission = u.getDataCriacao().toLocalDate();
            LocalDate deactivation = u.getDataDesativacao() != null ? u.getDataDesativacao().toLocalDate() : endDate != null ? endDate : LocalDate.now();
            LocalDate activeStart = admission.isAfter(startDate != null ? startDate : admission) ? admission : (startDate != null ? startDate : admission);
            LocalDate activeEnd = deactivation.isBefore(endDate != null ? endDate : LocalDate.now()) ? deactivation : (endDate != null ? endDate : LocalDate.now());
            
            if (!activeStart.isAfter(activeEnd)) {
                long activeDays = 0;
                LocalDate current = activeStart;
                while (!current.isAfter(activeEnd)) {
                    if (current.getDayOfWeek() != java.time.DayOfWeek.SATURDAY && current.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                        activeDays++;
                    }
                    current = current.plusDays(1);
                }
                totalHorasEst += (activeDays * 8.0);
            }
        }
        stats.put("total_horas_est", (long)totalHorasEst);

        return stats;
    }
}
