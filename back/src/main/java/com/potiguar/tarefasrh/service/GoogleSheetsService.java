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
            List<Tarefa> tarefas = tarefaRepository.findAll();

            List<List<Object>> values = new ArrayList<>();
            // Cabeçalho
            values.add(Arrays.asList("ID", "Título", "Descrição", "Responsável(is)", "Time", "Categoria", "Previsto Cargo (Gestor)", "Previsto Cargo (Colab)", "Criado Por", "Unidade do Criador", "Executor de Fato", "Status", "Complexidade", "Esforço (Pts)", "Horas Est.", "Prazo", "Conclusão", "Evidência", "Feedback Gestor"));
            for (Tarefa t : tarefas) {
                String responsaveis = t.getResponsaveis().stream()
                        .map(com.potiguar.tarefasrh.model.Usuario::getNome)
                        .collect(Collectors.joining(", "));

                int esforco = PESO_ESFORCO.getOrDefault(t.getComplexidade().toString(), 0);
                
                String feedbacksConcatenados = t.getFeedbacks().stream()
                        .map(f -> f.getGestor().getNome() + ": " + f.getMensagem())
                        .collect(Collectors.joining(" | "));

                values.add(Arrays.asList(
                        t.getId().toString(),
                        t.getTitulo(),
                        t.getDescricao() != null ? t.getDescricao() : "-",
                        responsaveis.isEmpty() ? "-" : responsaveis,
                        t.getTime() != null ? t.getTime().getNome() : "-",
                        t.getCategoria().toString(),
                        t.isPrevistoNoCargoGestor() ? "SIM" : "NÃO",
                        t.getPrevistoNoCargoColaborador() == null ? "-" : (t.getPrevistoNoCargoColaborador() ? "SIM" : "NÃO"),
                        t.getCriadoPor() != null ? t.getCriadoPor().getNome() : "Sistema",
                        t.getCriadoPor() != null ? t.getCriadoPor().getLoja() : "-",
                        t.getConcluidoPor() != null ? t.getConcluidoPor().getNome() : "-",
                        t.getStatus().toString(),
                        t.getComplexidade().toString(),
                        esforco,
                        esforco * 2, // 1pt = 2h
                        t.getDataPrazo().toString(),
                        t.getDataConclusao() != null ? t.getDataConclusao().toString() : "-",
                        t.getEvidencia() != null ? t.getEvidencia() : "-",
                        feedbacksConcatenados.isEmpty() ? "-" : feedbacksConcatenados
                ));
            }

            // Limpa a planilha antes de atualizar
            service.spreadsheets().values()
                    .clear(spreadsheetId, "A1:Z1000", null)
                    .execute();

            // Escreve os novos dados
            ValueRange body = new ValueRange().setValues(values);
            service.spreadsheets().values()
                    .update(spreadsheetId, "A1", body)
                    .setValueInputOption("RAW")
                    .execute();

            System.out.println("Sincronização com Google Sheets concluída.");

        } catch (Exception e) {
            System.err.println("Erro ao sincronizar com Google Sheets: " + e.getMessage());
        }
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
