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
        
        List<Tarefa> paginatedContent = tarefas.subList(startIdx, endIdx);

        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedContent);
        response.put("currentPage", page);
        response.put("totalItems", totalItems);
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
        return tarefaRepository.findById(id).map(t -> {
            if (t.getStatus() != Status.CONCLUIDA && t.getDataPrazo().isBefore(LocalDate.now())) {
                t.setStatus(Status.ATRASADA);
            }
            return ResponseEntity.ok(t);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Tarefa> atualizarStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String concluidoPorId = body.get("concluidoPorId");
        if (concluidoPorId == null) return ResponseEntity.badRequest().build();
        Long usuarioId = Long.parseLong(concluidoPorId);

        return tarefaRepository.findById(id).map(t -> {
            Usuario user = usuarioRepository.findById(usuarioId).orElse(null);
            if (user == null) return ResponseEntity.status(401).<Tarefa>build();

            boolean isGestor = user.getNivel() == com.potiguar.tarefasrh.model.Nivel.GESTOR;
            boolean isResponsavel = t.getResponsaveis().stream().anyMatch(r -> r.getId().equals(usuarioId));
            boolean isDoTime = t.getTime() != null && user.getTime() != null && t.getTime().getId().equals(user.getTime().getId());

            if (!isGestor && !isResponsavel && !isDoTime) {
                return ResponseEntity.status(403).<Tarefa>build();
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
            return ResponseEntity.ok(salva);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean analyticalMode) {
        
        List<Tarefa> todasBase = tarefaRepository.findAll();
        todasBase = atualizarStatusAtrasadas(todasBase);

        // RÉGUA ÚNICA PARA CARDS (Criação): Garante Total = P + E + C + A
        List<Tarefa> tarefasOperacionais = filtrarPorPeriodo(todasBase, startDate, endDate, false);
        
        // RÉGUA ANALÍTICA (Conclusão): Para Ranking e Aderência (Resultados do Mês)
        List<Tarefa> tarefasPerformance = filtrarPorPeriodo(todasBase, startDate, endDate, true);

        // Esforço TOTAL das tarefas nascidas no período (Carga Planejada)
        long esforcoCriado = tarefasOperacionais.stream()
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        // Esforço das tarefas CONCLUÍDAS no período (Carga Entregue)
        long esforcoEntregue = tarefasPerformance.stream()
                .filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null)
                .mapToLong(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0))
                .sum();

        // Aderência ao Cargo (Baseada em tudo o que foi entregue no mês)
        long aderenciaGestorSim = tarefasPerformance.stream().filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null && t.getPrevistoNoCargoGestor()).count();
        long aderenciaGestorTotal = tarefasPerformance.stream().filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null).count();
        long aderenciaColabSim = tarefasPerformance.stream().filter(t -> t.getStatus() == Status.CONCLUIDA && t.getConcluidoPor() != null && t.getPrevistoNoCargoColaborador() != null && t.getPrevistoNoCargoColaborador()).count();

        // Carga Horária Estimada (trabalho real das 87 tarefas, não capacidade teórica)
        long totalHorasTrabalho = esforcoCriado * 3;
        long concluidaHorasTrabalho = esforcoEntregue * 3;

        // Atenção Prioritária (Atrasadas do Período)
        List<Tarefa> topAtrasadas = tarefasOperacionais.stream()
                .filter(t -> t.getStatus() == Status.ATRASADA)
                .sorted((a, b) -> a.getDataPrazo().compareTo(b.getDataPrazo()))
                .limit(5).collect(Collectors.toList());

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
        List<Usuario> todosUsuarios = usuarioRepository.findAll();
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
        // CARDS (Tudo do balde OPERACIONAL)
        stats.put("total", (long) tarefasOperacionais.size());
        stats.put("pendente", tarefasOperacionais.stream().filter(t -> t.getStatus() == Status.PENDENTE).count());
        stats.put("em_andamento", tarefasOperacionais.stream().filter(t -> t.getStatus() == Status.EM_ANDAMENTO).count());
        stats.put("concluida", tarefasOperacionais.stream().filter(t -> t.getStatus() == Status.CONCLUIDA).count()); 
        stats.put("atrasada", tarefasOperacionais.stream().filter(t -> t.getStatus() == Status.ATRASADA).count());
        
        stats.put("total_times", timeRepository.count());
        
        // MÉTRICAS (Usa entrega vs planejado)
        stats.put("total_horas_est", totalHorasTrabalho); // Carga total de trabalho
        stats.put("concluidas_horas_est", concluidaHorasTrabalho); // Carga entregue
        stats.put("esforco_total", esforcoCriado); 
        stats.put("esforco_concluido", esforcoEntregue);
        
        stats.put("aderencia_gestor_sim", aderenciaGestorSim);
        stats.put("aderencia_gestor_nao", aderenciaGestorTotal - aderenciaGestorSim);
        stats.put("aderencia_colab_sim", aderenciaColabSim);
        
        stats.put("ranking", ranking);
        stats.put("topAtrasadas", topAtrasadas);
        stats.put("turnover", turnoverData);
        
        return stats;
    }
}
