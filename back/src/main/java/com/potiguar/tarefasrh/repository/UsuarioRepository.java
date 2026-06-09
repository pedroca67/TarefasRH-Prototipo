package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByCodigoFuncionario(String codigoFuncionario);
    Optional<Usuario> findByEmailOrCodigoFuncionario(String email, String codigoFuncionario);
    long countByAtivoTrue();
}
