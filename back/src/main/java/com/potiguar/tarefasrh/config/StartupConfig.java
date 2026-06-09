package com.potiguar.tarefasrh.config;

import com.potiguar.tarefasrh.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupConfig implements CommandLineRunner {

    private final UsuarioService usuarioService;

    @Override
    public void run(String... args) throws Exception {
        usuarioService.gerarCodigosFaltantes();
    }
}
