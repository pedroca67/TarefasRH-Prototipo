package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.dto.LoginRequest;
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
        return usuarioRepository.findByEmailOrCodigoFuncionario(identificador, identificador)
                .filter(u -> passwordEncoder.matches(request.getSenha(), u.getSenha()))
                .filter(Usuario::isAtivo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }
}
