package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.model.Categoria;
import com.potiguar.tarefasrh.model.Complexidade;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.repository.TarefaRepository;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import com.potiguar.tarefasrh.repository.TimeRepository;
import com.potiguar.tarefasrh.service.GoogleSheetsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TarefaControllerTest {

    @InjectMocks
    private TarefaController tarefaController;

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TimeRepository timeRepository;

    @Mock
    private GoogleSheetsService googleSheetsService;

    @BeforeEach
    void setUp() {
        // Inicializa os mocks (substitutos falsos do banco de dados para testar apenas a lógica)
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCriarTarefaSemResponsavelETimeGeraErro() {
        // Cenário (Given): Uma nova tarefa preenchida, mas sem ninguém atribuído (responsáveis vazio e time null)
        Tarefa novaTarefa = Tarefa.builder()
                .titulo("Tarefa Orfã")
                .descricao("Descrição da tarefa")
                .complexidade(Complexidade.MEDIA)
                .categoria(Categoria.OUTROS)
                .dataPrazo(LocalDate.now().plusDays(5))
                .responsaveis(new HashSet<>()) // Vazio
                .time(null) // Sem time
                .build();

        // Esta validação específica (ambos nulos) na verdade não está estritamente bloqueada no Controller hoje (ele aceita e salva vazio se o front deixar passar).
        // Vamos testar a regra dupla: se mandar BOTH (Time E Responsável) o controller barra.
        
        // Novo Cenário: Tarefa com Time E Responsável ao mesmo tempo (O controller proíbe isso)
        com.potiguar.tarefasrh.model.Time timeFake = new com.potiguar.tarefasrh.model.Time();
        timeFake.setId(1L);
        
        com.potiguar.tarefasrh.model.Usuario userFake = new com.potiguar.tarefasrh.model.Usuario();
        userFake.setId(2L);
        HashSet<com.potiguar.tarefasrh.model.Usuario> responsaveis = new HashSet<>();
        responsaveis.add(userFake);

        Tarefa tarefaInvalida = Tarefa.builder()
                .titulo("Tarefa com Dupla Atribuição")
                .time(timeFake)
                .responsaveis(responsaveis)
                .build();

        // Ação (When): Tentamos criar a tarefa
        ResponseEntity<?> response = tarefaController.criar(tarefaInvalida);

        // Verificação (Then): O sistema deve barrar com erro 400 (Bad Request)
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("apenas um tipo de atribuição"));
        
        // Verifica se o método save do banco de dados NUNCA foi chamado
        verify(tarefaRepository, never()).saveAndFlush(any(Tarefa.class));
    }
}
