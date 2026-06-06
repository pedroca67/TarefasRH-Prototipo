package com.potiguar.tarefasrh.controller;

import com.potiguar.tarefasrh.model.Notificacao;
import com.potiguar.tarefasrh.model.Usuario;
import com.potiguar.tarefasrh.repository.NotificacaoRepository;
import com.potiguar.tarefasrh.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        long count = notificacaoRepository.countByUsuarioAndLidaFalse(usuario);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping
    public List<Notificacao> listar(@RequestParam Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        return notificacaoRepository.findByUsuarioOrderByDataCriacaoDesc(usuario);
    }

    @PatchMapping("/read-by-task/{taskId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long taskId, @RequestParam Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        notificacaoRepository.markAsReadByUsuarioAndReferenciaId(usuario, taskId);
        return ResponseEntity.ok().build();
    }
}
