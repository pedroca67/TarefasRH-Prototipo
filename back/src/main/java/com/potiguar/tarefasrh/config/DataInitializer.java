package com.potiguar.tarefasrh.config;

import com.potiguar.tarefasrh.model.Nivel;
import com.potiguar.tarefasrh.model.Time;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.repository.TarefaRepository;
import com.potiguar.tarefasrh.repository.TimeRepository;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final TimeRepository timeRepository;
    private final TarefaRepository tarefaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (timeRepository.count() == 0) {
            timeRepository.save(Time.builder().nome("Recrutamento e Seleção").build());
            timeRepository.save(Time.builder().nome("Departamento Pessoal").build());
            timeRepository.save(Time.builder().nome("Treinamento e Desenvolvimento").build());
            timeRepository.save(Time.builder().nome("Benefícios").build());
            System.out.println("Times iniciais criados.");
        }

        if (usuarioRepository.findByEmail("gestor@potiguar.com.br").isEmpty()) {
            Usuario gestor = Usuario.builder()
                    .nome("Gestor Admin")
                    .email("gestor@potiguar.com.br")
                    .senha(passwordEncoder.encode("admin123"))
                    .nivel(Nivel.GESTOR)
                    .loja("Matriz - Centro")
                    .fotoUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=Gestor")
                    .ativo(true)
                    .build();

            usuarioRepository.save(gestor);
            System.out.println("Usuário Gestor criado.");
        }
    }

    private void saveColaborador(String nome, String email, String loja, String timeNome) {
        if (usuarioRepository.findByEmail(email).isEmpty()) {
            Time time = timeRepository.findAll().stream()
                    .filter(t -> t.getNome().equals(timeNome))
                    .findFirst().orElse(null);
            
            Usuario colab = Usuario.builder()
                    .nome(nome)
                    .email(email)
                    .senha(passwordEncoder.encode("123456"))
                    .nivel(Nivel.COLABORADOR)
                    .loja(loja)
                    .time(time)
                    .fotoUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + nome.replace(" ", ""))
                    .ativo(true)
                    .build();
            usuarioRepository.save(colab);
        }
    }
}
