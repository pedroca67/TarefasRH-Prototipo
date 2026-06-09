package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.dto.LoginRequest;
import com.potiguar.tarefasrh.dto.UsuarioDTO;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String identificador = request.getEmail();
        Usuario usuario;
        
        if (identificador.contains("@")) {
            usuario = usuarioRepository.findByEmail(identificador).orElse(null);
        } else {
            usuario = usuarioRepository.findByCodigoFuncionario(identificador).orElse(null);
        }

        if (usuario == null) {
            String msg = identificador.contains("@") ? "E-mail não encontrado." : "Código de funcionário não encontrado.";
            return ResponseEntity.status(401).body(msg);
        }

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            return ResponseEntity.status(401).body("Senha incorreta.");
        }

        if (!usuario.isAtivo()) {
            return ResponseEntity.status(403).body("Esta conta foi desativada pelo RH. Entre em contato com seu gestor.");
        }

        return ResponseEntity.ok(UsuarioDTO.fromEntity(usuario));
    }
}
