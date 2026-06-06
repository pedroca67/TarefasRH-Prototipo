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

    @Value("${google.sheets.id:}")
    private String spreadsheetId;

    @Value("${google.credentials.path:secrets/google-credentials.json}")
    private String googleCredentialsPath;

    private static final Map<String, Integer> PESO_ESFORCO = Map.of(
            "BAIXA", 1,
            "MEDIA", 3,
            "ALTA", 5
    );

    public void syncAllTasks() {
        if (spreadsheetId.isEmpty()) {
            return;
        }

        try {
            Sheets service = getSheetsService();
            
            // --- ABA 1: BASE_TAREFAS ---
            List<Tarefa> tarefas = tarefaRepository.findAll();
            List<List<Object>> valuesTarefas = new ArrayList<>();
            valuesTarefas.add(Arrays.asList("ID", "Título", "Descrição", "Responsável(is)", "Time", "Categoria", "Previsto Cargo (Gestor)", "Previsto Cargo (Colab)", "Criado Por", "Unidade do Criador", "Executor de Fato", "Status", "Complexidade", "Esforço (Pts)", "Horas Est.", "Prazo", "Conclusão", "Evidência", "Feedback Gestor"));
            
            for (Tarefa t : tarefas) {
                String responsaveis = t.getResponsaveis().stream().map(com.potiguar.tarefasrh.model.Usuario::getNome).collect(Collectors.joining(", "));
                int esforco = PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0);
                String feedbacks = t.getFeedbacks().stream().map(f -> f.getGestor().getNome() + ": " + f.getMensagem()).collect(Collectors.joining(" | "));

                valuesTarefas.add(Arrays.asList(
                    t.getId().toString(), t.getTitulo(), t.getDescricao() != null ? t.getDescricao() : "-",
                    responsaveis.isEmpty() ? "-" : responsaveis, t.getTime() != null ? t.getTime().getNome() : "-",
                    t.getCategoria().toString(), t.isPrevistoNoCargoGestor() ? "SIM" : "NÃO",
                    t.getPrevistoNoCargoColaborador() == null ? "-" : (t.getPrevistoNoCargoColaborador() ? "SIM" : "NÃO"),
                    t.getCriadoPor() != null ? t.getCriadoPor().getNome() : "Sistema",
                    t.getCriadoPor() != null ? t.getCriadoPor().getLoja() : "-",
                    t.getConcluidoPor() != null ? t.getConcluidoPor().getNome() : "-",
                    t.getStatus().toString(), t.getComplexidade().toString(), esforco, esforco * 2,
                    t.getDataPrazo().toString(), t.getDataConclusao() != null ? t.getDataConclusao().toString() : "-",
                    t.getEvidencia() != null ? t.getEvidencia() : "-", feedbacks.isEmpty() ? "-" : feedbacks
                ));
            }

            // --- ABA 2: BASE_TURNOVER ---
            List<com.potiguar.tarefasrh.model.Usuario> usuarios = usuarioRepository.findAll();

            List<List<Object>> valuesTurnover = new ArrayList<>();
            valuesTurnover.add(Arrays.asList("ID_Usuario", "Nome", "E-mail", "Loja", "Time", "Nível", "Status", "Data_Admissao", "Data_Desligamento"));
            
            for (com.potiguar.tarefasrh.model.Usuario u : usuarios) {
                valuesTurnover.add(Arrays.asList(
                    u.getId().toString(), u.getNome(), u.getEmail(), u.getLoja() != null ? u.getLoja() : "-",
                    u.getTime() != null ? u.getTime().getNome() : "-", u.getNivel().toString(),
                    u.isAtivo() ? "ATIVO" : "INATIVO",
                    u.getDataCriacao() != null ? u.getDataCriacao().toString() : "-",
                    u.getDataDesativacao() != null ? u.getDataDesativacao().toString() : "-"
                ));
            }

            // --- ABA 3: RESUMO_METRICAS (ESTRATÉGICO) ---
            List<List<Object>> valuesResumo = new ArrayList<>();
            valuesResumo.add(Arrays.asList("Mês/Ano", "Capacidade Total (Horas)", "Horas Entregues (Produtividade Real)"));
            
            long activeUsers = usuarios.stream().filter(com.potiguar.tarefasrh.model.Usuario::isAtivo).count();
            // Agrupar horas entregues por mês
            Map<String, Double> horasPorMes = tarefas.stream()
                .filter(t -> t.getStatus() == com.potiguar.tarefasrh.model.Status.CONCLUIDA && t.getDataConclusao() != null)
                .collect(Collectors.groupingBy(
                    t -> t.getDataConclusao().getYear() + "-" + String.format("%02d", t.getDataConclusao().getMonthValue()),
                    Collectors.summingDouble(t -> PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0) * 2.0)
                ));

            // Adicionar os últimos 12 meses ao resumo
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            for (int i = 0; i < 12; i++) {
                java.time.YearMonth targetMonth = currentMonth.minusMonths(i);
                String key = targetMonth.getYear() + "-" + String.format("%02d", targetMonth.getMonthValue());
                double entregue = horasPorMes.getOrDefault(key, 0.0);
                
                valuesResumo.add(Arrays.asList(
                    key,
                    activeUsers * 160, // Média de 160h mensais por colaborador
                    entregue
                ));
            }

            // Atualiza BASE_TAREFAS
            updateSheet(service, "BASE_TAREFAS!A1:Z2000", valuesTarefas);
            
            // Atualiza BASE_TURNOVER
            updateSheet(service, "BASE_TURNOVER!A1:I1000", valuesTurnover);
            
            // Atualiza RESUMO_METRICAS
            updateSheet(service, "RESUMO_METRICAS!A1:C50", valuesResumo);

            System.out.println("Sincronização multicanal (3 abas) concluída.");

        } catch (Exception e) {
            System.err.println("Erro ao sincronizar com Google Sheets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateSheet(Sheets service, String range, List<List<Object>> values) throws Exception {
        service.spreadsheets().values().clear(spreadsheetId, range, null).execute();
        ValueRange body = new ValueRange().setValues(values);
        service.spreadsheets().values().update(spreadsheetId, range, body)
                .setValueInputOption("RAW").execute();
    }

    private Sheets getSheetsService() throws Exception {
        GoogleCredentials credentials;
        String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");

        if (credentialsJson != null && !credentialsJson.isBlank()) {
            credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))
            );
        } else {
            try (InputStream credentialsStream = Files.newInputStream(Path.of(googleCredentialsPath))) {
                credentials = GoogleCredentials.fromStream(credentialsStream);
            }
        }

        credentials = credentials.createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("TarefasRH-Potiguar").build();
    }
}
