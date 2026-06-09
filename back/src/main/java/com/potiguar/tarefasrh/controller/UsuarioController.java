package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.dto.UsuarioDTO;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final com.potiguar.tarefasrh.service.UsuarioService usuarioService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(u -> ResponseEntity.ok(UsuarioDTO.fromEntity(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody Usuario usuario) {
        if (usuario.getId() == null) {
            if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Este e-mail já está em uso.");
            }
            
            // Geração automática do código de funcionário
            usuario.setCodigoFuncionario(usuarioService.gerarProximoCodigo(usuario.getNivel()));
            
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        } else {
            Usuario existing = usuarioRepository.findById(usuario.getId()).orElseThrow();
            if (usuario.getSenha() != null && !usuario.getSenha().equals(existing.getSenha()) && !usuario.getSenha().startsWith("$2a$")) {
                usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            } else {
                usuario.setSenha(existing.getSenha());
            }
            // Manter código existente na edição
            usuario.setCodigoFuncionario(existing.getCodigoFuncionario());
        }
        
        return ResponseEntity.ok(UsuarioDTO.fromEntity(usuarioRepository.save(usuario)));
    }

    @PatchMapping("/{id}/status")
    public void alternarStatus(@PathVariable Long id) {
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setAtivo(!u.isAtivo());
            if (!u.isAtivo()) {
                u.setDataDesativacao(java.time.LocalDateTime.now());
            } else {
                u.setDataDesativacao(null);
            }
            usuarioRepository.save(u);
        });
    }
}
