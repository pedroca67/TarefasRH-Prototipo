package com.potiguar.tarefasrh.repository;

import com.potiguar.tarefasrh.model.Notificacao;
import com.potiguar.tarefasrh.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    
    long countByUsuarioAndLidaFalse(Usuario usuario);
    
    List<Notificacao> findByUsuarioOrderByDataCriacaoDesc(Usuario usuario);

    @Modifying
    @Transactional
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.usuario = :usuario AND n.referenciaId = :referenciaId AND n.lida = false")
    void markAsReadByUsuarioAndReferenciaId(Usuario usuario, Long referenciaId);
}
