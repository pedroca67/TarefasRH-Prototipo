package com.potiguar.tarefasrh.dto;

import com.potiguar.tarefasrh.model.Status;
import com.potiguar.tarefasrh.model.Complexidade;
import com.potiguar.tarefasrh.model.Categoria;
import com.potiguar.tarefasrh.model.Tarefa;
import com.potiguar.tarefasrh.model.Time;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class TarefaDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private Complexidade complexidade;
    private Categoria categoria;
    private Boolean previstoNoCargoGestor;
    private Boolean previstoNoCargoColaborador;
    private Status status;
    private String evidencia;
    private LocalDate dataPrazo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataConclusao;
    private UsuarioDTO criadoPor;
    private UsuarioDTO concluidoPor;
    private UsuarioDTO responsavel;
    private Set<UsuarioDTO> responsaveis;
    private Time time;

    public static TarefaDTO fromEntity(Tarefa t) {
        if (t == null) return null;
        TarefaDTO dto = new TarefaDTO();
        dto.setId(t.getId());
        dto.setTitulo(t.getTitulo());
        dto.setDescricao(t.getDescricao());
        dto.setComplexidade(t.getComplexidade());
        dto.setCategoria(t.getCategoria());
        dto.setPrevistoNoCargoGestor(t.getPrevistoNoCargoGestor());
        dto.setPrevistoNoCargoColaborador(t.getPrevistoNoCargoColaborador());
        dto.setStatus(t.getStatus());
        dto.setEvidencia(t.getEvidencia());
        dto.setDataPrazo(t.getDataPrazo());
        dto.setDataCriacao(t.getDataCriacao());
        dto.setDataConclusao(t.getDataConclusao());
        dto.setCriadoPor(UsuarioDTO.fromEntity(t.getCriadoPor()));
        dto.setConcluidoPor(UsuarioDTO.fromEntity(t.getConcluidoPor()));
        dto.setResponsavel(UsuarioDTO.fromEntity(t.getResponsavel()));
        if (t.getResponsaveis() != null) {
            dto.setResponsaveis(t.getResponsaveis().stream()
                .map(UsuarioDTO::fromEntity)
                .collect(Collectors.toSet()));
        }
        dto.setTime(t.getTime());
        return dto;
    }
}
