package com.potiguar.tarefasrh.dto;

import com.potiguar.tarefasrh.model.Nivel;
import com.potiguar.tarefasrh.model.Time;
import com.potiguar.tarefasrh.model.Usuario;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UsuarioDTO {
    private Long id;
    private String nome;
    private String email;
    private String codigoFuncionario;
    private Nivel nivel;
    private Time time;
    private String loja;
    private String fotoUrl;
    private boolean ativo;
    private LocalDateTime dataCriacao;

    public static UsuarioDTO fromEntity(Usuario u) {
        if (u == null) return null;
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setNome(u.getNome());
        dto.setEmail(u.getEmail());
        dto.setCodigoFuncionario(u.getCodigoFuncionario());
        dto.setNivel(u.getNivel());
        dto.setTime(u.getTime());
        dto.setLoja(u.getLoja());
        if (u.getFotoUrl() != null && !u.getFotoUrl().trim().isEmpty()) {
            dto.setFotoUrl(u.getFotoUrl());
        } else {
            String seed = u.getNome() != null ? u.getNome().replace(" ", "") : "Usuario";
            dto.setFotoUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + seed);
        }
        dto.setAtivo(u.isAtivo());
        dto.setDataCriacao(u.getDataCriacao());
        return dto;
    }
}
