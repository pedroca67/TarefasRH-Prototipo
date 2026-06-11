package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.dto.CalendarioEventoDTO;
import com.potiguar.tarefasrh.dto.TarefaDTO;
import com.potiguar.tarefasrh.dto.UsuarioDTO;
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
@lombok.extern.slf4j.Slf4j
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
                if (startDate != null) matches = matches && !dataConc.isBefore(startDate);
                if (endDate != null) matches = matches && !dataConc.isAfter(endDate);
            } else {
                LocalDate dataRef = t.getDataCriacao().toLocalDate();
                if (startDate != null) matches = matches && !dataRef.isBefore(startDate);
                if (endDate != null) matches = matches && !dataRef.isAfter(endDate);
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
        
        List<Tarefa> tarefas;
        try {
            if (responsavelId != null) {
                Usuario resp = usuarioRepository.findById(responsavelId).orElse(null);
                if (resp == null) return emptyPaginatedResponse(page);
                tarefas = tarefaRepository.findByResponsaveisContaining(resp);
            } else if (timeId != null) {
                Time time = timeRepository.findById(timeId).orElse(null);
                if (time == null) return emptyPaginatedResponse(page);
                tarefas = tarefaRepository.findByTime(time);
            } else {
                tarefas = tarefaRepository.findAll();
            }
        } catch (Exception e) {
            return emptyPaginatedResponse(page);
        }

        if (search != null && !search.isBlank()) {
            String s = search.toLowerCase();
            tarefas = tarefas.stream().filter(t -> t.getTitulo().toLowerCase().contains(s)).collect(Collectors.toList());
        }
        if (status != null) tarefas = tarefas.stream().filter(t -> t.getStatus() == status).collect(Collectors.toList());
        if (complexidade != null) tarefas = tarefas.stream().filter(t -> t.getComplexidade() == complexidade).collect(Collectors.toList());
        if (categoria != null) tarefas = tarefas.stream().filter(t -> t.getCategoria() == categoria).collect(Collectors.toList());
        
        tarefas = filtrarPorPeriodo(tarefas, startDate, endDate, false);
        tarefas = atualizarStatusAtrasadas(tarefas);
        tarefas.sort((a, b) -> b.getDataCriacao().compareTo(a.getDataCriacao()));

        int totalItems = tarefas.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int startIdx = Math.min(page * size, totalItems);
        int endIdx = Math.min(startIdx + size, totalItems);
        
        List<TarefaDTO> paginatedContent = tarefas.subList(startIdx, endIdx).stream()
                .map(TarefaDTO::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedContent);
        response.put("currentPage", page);
        response.put("totalItems", (long)totalItems);
        response.put("totalPages", totalPages);

        return response;
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
        tarefa.setResponsaveis(new java.util.HashSet<>(responsaveisCompletos));

        if (!responsaveisCompletos.isEmpty()) {
            tarefa.setResponsavel(responsaveisCompletos.get(0));
        } else if (tarefa.getTime() != null && tarefa.getTime().getId() != null) {
            timeRepository.findById(tarefa.getTime().getId()).ifPresent(time -> {
                if (time.getMembros() != null && !time.getMembros().isEmpty()) {
                    tarefa.setResponsavel(time.getMembros().iterator().next());
                }
            });
        }

        if (tarefa.getTime() != null && tarefa.getTime().getId() != null) {
            Time timeCompleto = timeRepository.findById(tarefa.getTime().getId()).orElseThrow();
            tarefa.setTime(timeCompleto);
        }
        Tarefa salva = tarefaRepository.saveAndFlush(tarefa);
        googleSheetsService.syncAllTasks();
        return ResponseEntity.ok(TarefaDTO.fromEntity(salva));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarefaDTO> buscar(@PathVariable Long id, @RequestParam(required = false) Long usuarioId) {
        return tarefaRepository.findById(id).map(t -> {
            if (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(LocalDate.now())) {
                t.setStatus(Status.ATRASADA);
            }
            return ResponseEntity.ok(TarefaDTO.fromEntity(t));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Tarefa dados, @RequestParam Long usuarioId) {
        try {
            log.info("Iniciando atualização da tarefa {}. Usuário logado: {}. Responsáveis no payload: {}", 
                id, usuarioId, dados.getResponsaveis());

            return tarefaRepository.findById(id).map(t -> {
                Usuario user = usuarioRepository.findById(usuarioId).orElse(null);
                if (user == null) return ResponseEntity.status(401).body("Usuário não encontrado.");

                boolean isGestor = user.getNivel() == com.potiguar.tarefasrh.model.Nivel.GESTOR;
                boolean isCriador = t.getCriadoPor() != null && t.getCriadoPor().getId().equals(usuarioId);

                if (!isGestor && !isCriador) {
                    return ResponseEntity.status(403).body("Apenas o criador ou gestores podem editar esta tarefa.");
                }

                t.setTitulo(dados.getTitulo());
                t.setDescricao(dados.getDescricao());
                t.setComplexidade(dados.getComplexidade());
                t.setCategoria(dados.getCategoria());
                t.setDataPrazo(dados.getDataPrazo());
                t.setPrevistoNoCargoGestor(dados.getPrevistoNoCargoGestor());

                // Lógica de Atribuição (Time ou Responsáveis)
                if (dados.getTime() != null && dados.getTime().getId() != null) {
                    Time time = timeRepository.findById(dados.getTime().getId()).orElse(null);
                    t.setTime(time);
                    t.setResponsaveis(new java.util.HashSet<>());
                    
                    if (t.getResponsavel() == null) {
                        t.setResponsavel(user);
                    }
                } else {
                    t.setTime(null);
                    if (dados.getResponsaveis() != null && !dados.getResponsaveis().isEmpty()) {
                        java.util.Set<Usuario> resps = dados.getResponsaveis().stream()
                            .map(u -> usuarioRepository.findById(u.getId()).orElseThrow())
                            .collect(Collectors.toSet());
                        t.setResponsaveis(resps);
                        t.setResponsavel(resps.iterator().next());
                    }
                }

                Tarefa salva = tarefaRepository.saveAndFlush(t);
                googleSheetsService.syncAllTasks();
                return ResponseEntity.ok(TarefaDTO.fromEntity(salva));
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("ERRO CRÍTICO NA ATUALIZAÇÃO DA TAREFA {}: ", id, e);
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao atualizar tarefa: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TarefaDTO> atualizarStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String concluidoPorId = body.get("concluidoPorId");
        if (concluidoPorId == null) return ResponseEntity.badRequest().build();
        Long usuarioId = Long.parseLong(concluidoPorId);

        return tarefaRepository.findById(id).map(t -> {
            Usuario user = usuarioRepository.findById(usuarioId).orElse(null);
            if (user == null) return ResponseEntity.status(401).<TarefaDTO>build();

            boolean isGestor = user.getNivel() == com.potiguar.tarefasrh.model.Nivel.GESTOR;
            boolean isResponsavel = t.getResponsaveis().stream().anyMatch(r -> r.getId().equals(usuarioId));
            boolean isDoTime = t.getTime() != null && user.getTime() != null && t.getTime().getId().equals(user.getTime().getId());

            if (!isGestor && !isResponsavel && !isDoTime) {
                return ResponseEntity.status(403).<TarefaDTO>build();
            }

            Status novoStatus = Status.valueOf(body.get("status"));
            if (novoStatus == Status.CONCLUIDA) {
                t.setEvidencia(body.get("evidencia"));
                t.setDataConclusao(LocalDateTime.now());
                t.setConcluidoPor(user);
                if (body.get("previstoNoCargoColaborador") != null) t.setPrevistoNoCargoColaborador(Boolean.parseBoolean(body.get("previstoNoCargoColaborador")));
            } else {
                t.setDataConclusao(null);
                t.setConcluidoPor(null);
            }
            t.setStatus(novoStatus);
            Tarefa salva = tarefaRepository.save(t);
            googleSheetsService.syncAllTasks();
            return ResponseEntity.ok(TarefaDTO.fromEntity(salva));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/feedbacks")
    public ResponseEntity<List<Feedback>> listarFeedbacks(@PathVariable Long id) {
        return tarefaRepository.findById(id)
            .map(t -> {
                List<Feedback> list = new java.util.ArrayList<>(t.getFeedbacks());
                return ResponseEntity.ok(list);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/feedback")
    @Transactional
    public ResponseEntity<?> salvarFeedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object gestorIdObj = body.get("gestorId");
        if (gestorIdObj == null) return ResponseEntity.badRequest().body("gestorId ausente.");
        Long gestorId = Long.parseLong(gestorIdObj.toString());

        Usuario gestor = usuarioRepository.findById(gestorId).orElse(null);
        if (gestor == null || gestor.getNivel() != com.potiguar.tarefasrh.model.Nivel.GESTOR) {
            return ResponseEntity.status(403).build();
        }

        return tarefaRepository.findById(id).map(t -> {
            String texto = body.containsKey("feedback") ? body.get("feedback").toString() : 
                           (body.containsKey("mensagem") ? body.get("mensagem").toString() : null);
            
            if (texto == null || texto.isBlank()) return ResponseEntity.badRequest().body("Mensagem vazia.");

            Feedback fb = new Feedback();
            fb.setTarefa(t);
            fb.setGestor(gestor);
            fb.setMensagem(texto);
            fb.setDataCriacao(LocalDateTime.now());
            feedbackRepository.save(fb);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/calendario")
    public List<CalendarioEventoDTO> calendario(
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) Long timeId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<Tarefa> tarefas = tarefaRepository.findForCalendario(responsavelId, timeId, start, end);
        LocalDate hoje = LocalDate.now();

        return tarefas.stream().map(t -> {
            boolean atrasada = t.getStatus() == Status.ATRASADA || 
                               (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(hoje));
            
            String color = "#ffc107"; // PENDENTE default
            if (t.getStatus() == Status.CONCLUIDA) color = "#28a745";
            else if (atrasada) color = "#dc3545";
            else if (t.getStatus() == Status.EM_ANDAMENTO) color = "#0d6efd";

            return CalendarioEventoDTO.builder()
                .id(t.getId())
                .title(t.getTitulo())
                .start(t.getDataPrazo().toString())
                .color(color)
                .status(atrasada ? "ATRASADA" : t.getStatus().name())
                .url("/tarefas/" + t.getId())
                .build();
        }).collect(Collectors.toList());
    }

    @GetMapping("/admin/sync")
    public ResponseEntity<?> forcSync() {
        googleSheetsService.syncAllTasks();
        return ResponseEntity.ok("Sincronização iniciada.");
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean analyticalMode) {
        
        List<Tarefa> todasBase = tarefaRepository.findAll();
        todasBase = atualizarStatusAtrasadas(todasBase);

        // RÉGUA OPERACIONAL: Filtra por Criação
        List<Tarefa> tarefasStatus = filtrarPorPeriodo(todasBase, startDate, endDate, false);
        
        // RÉGUA ANALÍTICA: Filtra por Conclusão
        List<Tarefa> tarefasPerformance = filtrarPorPeriodo(todasBase, startDate, endDate, true);

        // Esforço das concluídas no período (Analítico)
        long esforcoConcluido = tarefasPerformance.stream()
                .filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null)
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        long esforcoTotalPeriodo = tarefasStatus.stream()
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        long aderenciaGestorSim = tarefasPerformance.stream().filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null && t.getPrevistoNoCargoGestor()).count();
        long aderenciaGestorTotal = tarefasPerformance.stream().filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null).count();
        long aderenciaColabSim = tarefasPerformance.stream().filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null && t.getPrevistoNoCargoColaborador() != null && t.getPrevistoNoCargoColaborador()).count();

        // Capacidade (Restaurando lógica de Horas Estimadas baseada em usuários ativos)
        List<Usuario> todosUsuarios = usuarioRepository.findAll();
        double totalHorasEst = 0;
        LocalDate dataMinimaAdmissao = todosUsuarios.stream().map(u -> u.getDataCriacao().toLocalDate()).min(LocalDate::compareTo).orElse(LocalDate.now().minusDays(30));
        LocalDate filterStart = startDate != null ? startDate : dataMinimaAdmissao;
        LocalDate filterEnd = endDate != null ? endDate : LocalDate.now();

        for (Usuario u : todosUsuarios) {
            LocalDate admission = u.getDataCriacao().toLocalDate();
            LocalDate deactivation = u.getDataDesativacao() != null ? u.getDataDesativacao().toLocalDate() : filterEnd.plusDays(1);
            LocalDate activeStart = admission.isAfter(filterStart) ? admission : filterStart;
            LocalDate activeEnd = deactivation.isBefore(filterEnd) ? deactivation : filterEnd;
            if (!activeStart.isAfter(activeEnd)) {
                long activeDays = java.time.temporal.ChronoUnit.DAYS.between(activeStart, activeEnd) + 1;
                totalHorasEst += (activeDays * (40.0 / 7.0));
            }
        }
        
        long concluidasHorasEst = esforcoConcluido * 3;

        // Atenção Prioritária (Respeitando o filtro de período pelo Prazo conforme solicitado)
        List<TarefaDTO> topAtrasadas = todasBase.stream()
                .filter(t -> t.getStatus() == Status.ATRASADA)
                .filter(t -> {
                    LocalDate prazo = t.getDataPrazo();
                    return (startDate == null || !prazo.isBefore(startDate)) && (endDate == null || !prazo.isAfter(endDate));
                })
                .sorted((a, b) -> a.getDataPrazo().compareTo(b.getDataPrazo()))
                .limit(5)
                .map(TarefaDTO::fromEntity)
                .collect(Collectors.toList());

        // Ranking de Performance (Baseado em quem entregou no mês)
        Map<Usuario, Long> pontosPorUsuario = new HashMap<>();
        tarefasPerformance.stream()
            .filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null)
            .forEach(t -> {
                Usuario executor = t.getConcluidoPor();
                long pontos = PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0);
                pontosPorUsuario.put(executor, pontosPorUsuario.getOrDefault(executor, 0L) + pontos);
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
                }).collect(Collectors.toList());

        // Turnover
        List<Map<String, Object>> turnoverData = new java.util.ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate mesRef = LocalDate.now().minusMonths(i);
            long adm = todosUsuarios.stream().filter(u -> u.getDataCriacao().getMonthValue() == mesRef.getMonthValue() && u.getDataCriacao().getYear() == mesRef.getYear()).count();
            long des = todosUsuarios.stream().filter(u -> u.getDataDesativacao() != null && u.getDataDesativacao().getMonthValue() == mesRef.getMonthValue() && u.getDataDesativacao().getYear() == mesRef.getYear()).count();
            Map<String, Object> m = new HashMap<>();
            m.put("mes", mesRef.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, new java.util.Locale("pt", "BR")));
            m.put("admissoes", adm);
            m.put("desligamentos", des);
            turnoverData.add(m);
        }

        Map<String, Object> stats = new HashMap<>();
        // Bloco 1 — Situação Atual (sem filtro de data)
        stats.put("total", todasBase.stream().filter(t -> t.getStatus() != Status.CONCLUIDA).count());
        stats.put("pendente", todasBase.stream().filter(t -> t.getStatus() == Status.PENDENTE).count());
        stats.put("em_andamento", todasBase.stream().filter(t -> t.getStatus() == Status.EM_ANDAMENTO).count());
        stats.put("atrasada", todasBase.stream().filter(t -> t.getStatus() == Status.ATRASADA).count());
        // Bloco 2 — Desempenho do Período
        stats.put("concluida", (long) tarefasPerformance.stream().filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null).count()); 
        
        stats.put("total_times", timeRepository.count());
        stats.put("esforco_total", (long)totalHorasEst); 
        stats.put("esforco_concluido", esforcoConcluido);
        
        stats.put("aderencia_gestor_sim", aderenciaGestorSim);
        stats.put("aderencia_gestor_nao", aderenciaGestorTotal - aderenciaGestorSim);
        stats.put("aderencia_colab_sim", aderenciaColabSim);
        
        stats.put("total_horas_est", esforcoTotalPeriodo);
        stats.put("concluidas_horas_est", esforcoConcluido);
        stats.put("ranking", ranking);
        stats.put("topAtrasadas", topAtrasadas);
        stats.put("turnover", turnoverData);
        
        return stats;
    }
}
