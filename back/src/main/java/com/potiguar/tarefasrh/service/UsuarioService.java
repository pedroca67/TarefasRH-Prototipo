package com.potiguar.tarefasrh.service;

import com.potiguar.tarefasrh.model.Nivel;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public String gerarProximoCodigo(Nivel nivel) {
        String prefix = nivel == Nivel.GESTOR ? "POT-G" : "POT-C";
        List<Usuario> usuarios = usuarioRepository.findAll();
        
        int maxNum = 0;
        if (nivel == Nivel.GESTOR) {
            maxNum = usuarios.stream()
                .filter(u -> u.getCodigoFuncionario() != null && u.getCodigoFuncionario().startsWith("POT-G"))
                .map(u -> {
                    try {
                        return Integer.parseInt(u.getCodigoFuncionario().substring(5));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max(Integer::compare)
                .orElse(0);
            return String.format("POT-G%03d", maxNum + 1);
        } else {
            maxNum = usuarios.stream()
                .filter(u -> u.getCodigoFuncionario() != null && u.getCodigoFuncionario().startsWith("POT-C"))
                .map(u -> {
                    try {
                        return Integer.parseInt(u.getCodigoFuncionario().substring(5));
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max(Integer::compare)
                .orElse(999); // Inicia em 1000
            return String.format("POT-C%04d", maxNum + 1);
        }
    }

    @Transactional
    public void gerarCodigosFaltantes() {
        List<Usuario> usuariosSemCodigo = usuarioRepository.findAll().stream()
                .filter(u -> u.getCodigoFuncionario() == null || u.getCodigoFuncionario().isBlank())
                .toList();

        for (Usuario u : usuariosSemCodigo) {
            u.setCodigoFuncionario(gerarProximoCodigo(u.getNivel()));
            usuarioRepository.save(u);
        }
    }
}
