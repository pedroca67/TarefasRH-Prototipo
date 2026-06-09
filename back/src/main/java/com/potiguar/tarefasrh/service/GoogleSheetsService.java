package com.potiguar.tarefasrh.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.repository.TarefaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private final TarefaRepository tarefaRepository;
    private final com.potiguar.tarefasrh.repository.UsuarioRepository usuarioRepository;
    
    private static final java.util.concurrent.atomic.AtomicBoolean IS_SYNCING = new java.util.concurrent.atomic.AtomicBoolean(false);

    @Value("${google.sheets.id:}")
    private String spreadsheetId;

    @Value("${google.credentials.path:secrets/google-credentials.json}")
    private String googleCredentialsPath;

    private static final Map<String, Integer> PESO_ESFORCO = Map.of(
            "BAIXA", 1,
            "MEDIA", 3,
            "ALTA", 5
    );

    @org.springframework.scheduling.annotation.Async
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void syncAllTasks() {
        if (spreadsheetId.isEmpty()) return;
        if (!IS_SYNCING.compareAndSet(false, true)) return;

        try {
            Sheets service = getSheetsService();
            List<Tarefa> tarefas = tarefaRepository.findTarefasForExport(); 
            
            // --- ABA 1: BASE_TAREFAS ---
            List<List<Object>> valuesTarefas = new ArrayList<>();
            valuesTarefas.add(Arrays.asList("ID", "Título", "Descrição", "Responsável(is)", "Time", "Categoria", "Previsto Cargo (Gestor)", "Previsto Cargo (Colab)", "Criado Por", "Unidade do Criador", "Executor de Fato", "Status", "Complexidade", "Esforço (Pts)", "Horas Est.", "Criação", "Prazo", "Conclusão", "Evidência", "Feedback Gestor"));
            
            for (Tarefa t : tarefas) {
                String responsaveisLabel;
                if (t.getTime() != null) {
                    responsaveisLabel = "Time: " + t.getTime().getNome();
                } else if (t.getResponsaveis() != null && !t.getResponsaveis().isEmpty()) {
                    String respNomes = t.getResponsaveis().stream().map(com.potiguar.tarefasrh.model.Usuario::getNome).collect(Collectors.joining(", "));
                    responsaveisLabel = !respNomes.isEmpty() ? respNomes : "-";
                } else {
                    responsaveisLabel = "-";
                }
                
                int esforco = PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0);
                String feedbacks = (t.getFeedbacks() != null && !t.getFeedbacks().isEmpty()) 
                    ? t.getFeedbacks().stream().map(f -> f.getGestor().getNome() + ": " + f.getMensagem()).collect(Collectors.joining(" | ")) 
                    : "-";

                valuesTarefas.add(Arrays.asList(
                    t.getId().toString(),
                    t.getTitulo(),
                    t.getDescricao() != null ? t.getDescricao() : "-",
                    responsaveisLabel, t.getTime() != null ? t.getTime().getNome() : "-",
                    t.getCategoria().toString(), t.isPrevistoNoCargoGestor() ? "SIM" : "NÃO",
                    t.getPrevistoNoCargoColaborador() == null ? "-" : (t.getPrevistoNoCargoColaborador() ? "SIM" : "NÃO"),
                    t.getCriadoPor() != null ? t.getCriadoPor().getNome() : "Sistema",
                    t.getCriadoPor() != null ? t.getCriadoPor().getLoja() : "-",
                    t.getConcluidoPor() != null ? t.getConcluidoPor().getNome() : "-",
                    t.getStatus().toString(), t.getComplexidade().toString(), esforco, esforco * 3,
                    t.getDataCriacao() != null ? t.getDataCriacao().toLocalDate().toString() : "-",
                    t.getDataPrazo().toString(), 
                    t.getDataConclusao() != null ? t.getDataConclusao().toLocalDate().toString() : "-",
                    t.getEvidencia() != null ? t.getEvidencia() : "-", feedbacks.isEmpty() ? "-" : feedbacks
                ));
            }

            // --- ABA 2: BASE_TURNOVER ---
            List<com.potiguar.tarefasrh.model.Usuario> usuarios = usuarioRepository.findAll();
            List<List<Object>> valuesTurnover = new ArrayList<>();
            valuesTurnover.add(Arrays.asList("ID_Usuario", "Nome", "E-mail", "Loja", "Time", "Nível", "Status", "Data_Admissao", "Data_Desligamento"));
            for (com.potiguar.tarefasrh.model.Usuario u : usuarios) {
                valuesTurnover.add(Arrays.asList(u.getId().toString(), u.getNome(), u.getEmail(), u.getLoja() != null ? u.getLoja() : "-", u.getTime() != null ? u.getTime().getNome() : "-", u.getNivel().toString(), u.isAtivo() ? "ATIVO" : "INATIVO", u.getDataCriacao() != null ? u.getDataCriacao().toLocalDate().toString() : "-", u.getDataDesativacao() != null ? u.getDataDesativacao().toLocalDate().toString() : "-"));
            }

            // --- ABA 3: RESUMO_METRICAS ---
            List<List<Object>> valuesResumo = new ArrayList<>();
            valuesResumo.add(Arrays.asList("Mês/Ano", "Capacidade Total (Horas)", "Horas Entregues (Produtividade Real)"));
            Map<String, Double> horasPorMes = tarefaRepository.findMonthlyEffortData().stream().collect(Collectors.toMap(row -> row[0].toString() + "-" + String.format("%02d", ((Number)row[1]).intValue()), row -> ((Number)row[2]).doubleValue()));
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            for (int i = 0; i < 12; i++) {
                java.time.YearMonth targetMonth = currentMonth.minusMonths(i);
                String key = targetMonth.getYear() + "-" + String.format("%02d", targetMonth.getMonthValue());
                long activeInMonth = usuarios.stream().filter(u -> {
                    java.time.LocalDateTime adm = u.getDataCriacao();
                    java.time.LocalDateTime des = u.getDataDesativacao();
                    boolean admitted = adm != null && (adm.getYear() < targetMonth.getYear() || (adm.getYear() == targetMonth.getYear() && adm.getMonthValue() <= targetMonth.getMonthValue()));
                    boolean notDes = des == null || (des.getYear() > targetMonth.getYear() || (des.getYear() == targetMonth.getYear() && des.getMonthValue() >= targetMonth.getMonthValue()));
                    return admitted && notDes;
                }).count();
                valuesResumo.add(Arrays.asList(key, activeInMonth * 160, horasPorMes.getOrDefault(key, 0.0)));
            }

            // --- ABA 4: LOOKER_DASHBOARD ---
            List<List<Object>> valuesLooker = new ArrayList<>();
            valuesLooker.add(Arrays.asList("ID", "Título", "Responsável(is)", "Time", "Loja", "Categoria", "Status", "Complexidade", "Esforço (Pts)", "Horas Est.", "Previsto Cargo (Gestor)", "Previsto Cargo (Colab)", "Criação", "Prazo", "Conclusão"));
            for (Tarefa t : tarefas) {
                String responsaveisLabel;
                if (t.getTime() != null) responsaveisLabel = "Time: " + t.getTime().getNome();
                else if (t.getResponsaveis() != null && !t.getResponsaveis().isEmpty()) responsaveisLabel = t.getResponsaveis().stream().map(com.potiguar.tarefasrh.model.Usuario::getNome).collect(Collectors.joining(", "));
                else responsaveisLabel = "-";
                
                valuesLooker.add(Arrays.asList(
                    t.getId().toString(),
                    t.getTitulo(), responsaveisLabel, t.getTime() != null ? t.getTime().getNome() : "-",
                    t.getConcluidoPor() != null ? t.getConcluidoPor().getLoja() : (t.getCriadoPor() != null ? t.getCriadoPor().getLoja() : "-"),
                    t.getCategoria().toString(), t.getStatus().toString(), t.getComplexidade().toString(),
                    PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0), 
                    PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0) * 3,
                    t.isPrevistoNoCargoGestor() ? "SIM" : "NÃO",
                    t.getPrevistoNoCargoColaborador() == null ? "-" : (t.getPrevistoNoCargoColaborador() ? "SIM" : "NÃO"),
                    t.getDataCriacao() != null ? t.getDataCriacao().toLocalDate().toString() : "-",
                    t.getDataPrazo().toString(), t.getDataConclusao() != null ? t.getDataConclusao().toLocalDate().toString() : "-"
                ));
            }

            try { updateSheet(service, "BASE_TAREFAS!A1", valuesTarefas); } catch (Exception e) { System.err.println("Erro BASE_TAREFAS: " + e.getMessage()); }
            try { updateSheet(service, "BASE_TURNOVER!A1", valuesTurnover); } catch (Exception e) { System.err.println("Erro BASE_TURNOVER: " + e.getMessage()); }
            try { updateSheet(service, "RESUMO_METRICAS!A1", valuesResumo); } catch (Exception e) { System.err.println("Erro RESUMO_METRICAS: " + e.getMessage()); }
            try { updateSheet(service, "LOOKER_DASHBOARD!A1", valuesLooker); } catch (Exception e) { System.err.println("Erro LOOKER_DASHBOARD: " + e.getMessage()); }

        } catch (Exception e) {
            System.err.println("❌ ERRO CRÍTICO NO GOOGLE SHEETS: " + e.getMessage());
        } finally {
            IS_SYNCING.set(false);
        }
    }

    private void updateSheet(Sheets service, String range, List<List<Object>> values) throws Exception {
        String sheetName = range.contains("!") ? range.split("!")[0] : range;
        service.spreadsheets().values().clear(spreadsheetId, sheetName + "!A1:Z10000", null).execute();
        ValueRange body = new ValueRange().setValues(values);
        service.spreadsheets().values().update(spreadsheetId, range, body).setValueInputOption("RAW").execute();
    }

    private Sheets getSheetsService() throws Exception {
        GoogleCredentials credentials;
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(credentialsJson.trim().getBytes(StandardCharsets.UTF_8)));
        } else {
            try (InputStream credentialsStream = Files.newInputStream(Path.of(googleCredentialsPath))) {
                credentials = GoogleCredentials.fromStream(credentialsStream);
            }
        }
        credentials = credentials.createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials)).setApplicationName("TarefasRH-Potiguar").build();
    }
}
