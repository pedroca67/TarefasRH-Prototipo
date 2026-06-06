-- ======================================================
-- SCRIPT DE HARD RESET E INCLUSÃO DE DADOS - VERSÃO UNIVERSAL (GRUPO 5)
-- ======================================================

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- LIMPEZA TOTAL
DELETE FROM feedback;
DELETE FROM notificacao;
DELETE FROM tarefa_responsavel;
DELETE FROM tarefa;
DELETE FROM usuario;
DELETE FROM time;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. TIMES
INSERT INTO time (id, nome) VALUES 
(1, 'Recrutamento e Seleção'), (2, 'Departamento Pessoal'), (3, 'Treinamento e Desenvolvimento'), (4, 'Benefícios');

-- 2. GESTOR (Admitido em Jan/2024)
INSERT INTO usuario (id, nome, email, senha, nivel, loja, foto_url, ativo, data_criacao) VALUES 
(1, 'Gestor Admin', 'gestor@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'GESTOR', 'Matriz - Centro', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Gestor', 1, '2024-01-10 08:00:00');

-- 3. COLABORADORES (Datas variadas para Turnover)
INSERT INTO usuario (id, nome, email, senha, nivel, loja, time_id, foto_url, ativo, data_criacao) VALUES 
(2, 'Pedro Silva', 'pedro@potiguar.com.br', '$2a$10$vD9C.tK6w8J/ZJ0rXv9FxeU7lZ4Y8u3lJ5iZ5Wp1L8k5n9y6e4XG6', 'COLABORADOR', 'Cohama', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=Pedro', 1, '2024-03-15 09:00:00'),
(3, 'Maria Santos', 'maria@potiguar.com.br', '$2a$10$vD9C.tK6w8J/ZJ0rXv9FxeU7lZ4Y8u3lJ5iZ5Wp1L8k5n9y6e4XG6', 'COLABORADOR', 'Forquilha', 2, 'https://api.dicebear.com/7.x/avataaars/svg?seed=Maria', 1, '2024-06-20 10:30:00'),
(4, 'João Oliveira', 'joao@potiguar.com.br', '$2a$10$vD9C.tK6w8J/ZJ0rXv9FxeU7lZ4Y8u3lJ5iZ5Wp1L8k5n9y6e4XG6', 'COLABORADOR', 'Africanos', 3, 'https://api.dicebear.com/7.x/avataaars/svg?seed=Joao', 1, '2025-01-10 14:00:00'),
(5, 'Ana Costa', 'ana@potiguar.com.br', '$2a$10$vD9C.tK6w8J/ZJ0rXv9FxeU7lZ4Y8u3lJ5iZ5Wp1L8k5n9y6e4XG6', 'COLABORADOR', 'Maiobão', 4, 'https://api.dicebear.com/7.x/avataaars/svg?seed=Ana', 1, '2025-05-15 08:15:00'),
(6, 'Carlos Souza', 'carlos@potiguar.com.br', '$2a$10$vD9C.tK6w8J/ZJ0rXv9FxeU7lZ4Y8u3lJ5iZ5Wp1L8k5n9y6e4XG6', 'COLABORADOR', 'Santa Inês', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=Carlos', 1, '2026-01-20 09:00:00'),
(7, 'Juliana Lima', 'juliana@potiguar.com.br', '$2a$10$vD9C.tK6w8J/ZJ0rXv9FxeU7lZ4Y8u3lJ5iZ5Wp1L8k5n9y6e4XG6', 'COLABORADOR', 'Imperatriz', 2, 'https://api.dicebear.com/7.x/avataaars/svg?seed=Juliana', 1, '2026-03-01 08:00:00');

-- 4. INATIVOS (Turnover histórico)
INSERT INTO usuario (id, nome, email, senha, nivel, loja, ativo, data_criacao, data_desativacao) VALUES 
(8, 'Ex-Funcionario 1', 'ex1@potiguar.com.br', 'x', 'COLABORADOR', 'Centro', 0, '2024-02-01', '2024-11-15'),
(9, 'Ex-Funcionario 2', 'ex2@potiguar.com.br', 'x', 'COLABORADOR', 'Cohama', 0, '2025-02-10', '2025-12-20'),
(10, 'Ex-Funcionario 3', 'ex3@potiguar.com.br', 'x', 'COLABORADOR', 'Forquilha', 0, '2026-02-01', '2026-05-10');

-- 5. TAREFAS 2024 (HISTÓRICO LONGO)
INSERT INTO tarefa (id, titulo, descricao, categoria, complexidade, status, data_prazo, data_conclusao, criado_por_id, concluido_por_id, evidencia, previsto_no_cargo_gestor, previsto_no_cargo_colaborador, data_criacao) VALUES 
(101, 'Projeto RH Digital 2024', 'Migração de arquivos.', 'OUTROS', 'ALTA', 'CONCLUIDA', '2024-12-15', '2024-12-10', 1, 2, 'Arquivos migrados.', 1, 1, '2024-11-01'),
(102, 'Treinamento de Liderança', 'Ciclo 1.', 'TREINAMENTO_E_DESENVOLVIMENTO', 'MEDIA', 'CONCLUIDA', '2024-08-20', '2024-08-18', 1, 4, 'Lista de presença ok.', 1, 1, '2024-08-01');
INSERT INTO tarefa_responsavel (tarefa_id, usuario_id) VALUES (101, 2), (102, 4);

-- 6. TAREFAS 2025 (HISTÓRICO MÉDIO)
INSERT INTO tarefa (id, titulo, descricao, categoria, complexidade, status, data_prazo, data_conclusao, criado_por_id, concluido_por_id, evidencia, previsto_no_cargo_gestor, previsto_no_cargo_colaborador, data_criacao) VALUES 
(201, 'Revisão de Benefícios 2025', 'Novas taxas.', 'BENEFICIOS', 'ALTA', 'CONCLUIDA', '2025-03-30', '2025-03-28', 1, 5, 'Planilha atualizada.', 1, 1, '2025-03-01'),
(202, 'Endomarketing Páscoa', 'Evento lojas.', 'ENDOMARKETING', 'BAIXA', 'CONCLUIDA', '2025-04-10', '2025-04-09', 1, 6, 'Fotos do evento.', 0, 0, '2025-04-01');
INSERT INTO tarefa_responsavel (tarefa_id, usuario_id) VALUES (201, 5), (202, 6);

-- 7. TAREFAS 2026 (ATUALIDADE)
-- Mês Passado (Maio)
INSERT INTO tarefa (id, titulo, descricao, categoria, complexidade, status, data_prazo, data_conclusao, criado_por_id, concluido_por_id, evidencia, previsto_no_cargo_gestor, previsto_no_cargo_colaborador, data_criacao) VALUES 
(301, 'Folha de Pagamento Maio', 'Fechamento mensal.', 'DEPARTAMENTO_PESSOAL', 'ALTA', 'CONCLUIDA', '2026-05-31', '2026-05-30', 1, 3, 'Relatórios enviados.', 1, 1, '2026-05-15');
INSERT INTO tarefa_responsavel (tarefa_id, usuario_id) VALUES (301, 3);

-- Este Mês (Junho) - Concluídas
INSERT INTO tarefa (id, titulo, descricao, categoria, complexidade, status, data_prazo, data_conclusao, criado_por_id, concluido_por_id, evidencia, previsto_no_cargo_gestor, previsto_no_cargo_colaborador, data_criacao) VALUES 
(401, 'Triagem R&S Junho', 'Vagas operacionais.', 'RECRUTAMENTO_E_SELECAO', 'MEDIA', 'CONCLUIDA', CURDATE(), NOW(), 1, 2, 'Contratações feitas.', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY));
INSERT INTO tarefa_responsavel (tarefa_id, usuario_id) VALUES (401, 2);

-- Este Mês (Junho) - Pendentes e Atrasadas
INSERT INTO tarefa (id, titulo, descricao, categoria, complexidade, status, data_prazo, criado_por_id, previsto_no_cargo_gestor, data_criacao) VALUES 
(501, 'Auditoria de Documentos', 'Conferência interna.', 'DEPARTAMENTO_PESSOAL', 'MEDIA', 'ATRASADA', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(502, 'Palestra de Segurança', 'Agendar técnico.', 'SAUDE_E_SEGURANCA', 'BAIXA', 'PENDENTE', DATE_ADD(CURDATE(), INTERVAL 5 DAY), 1, 1, NOW());
INSERT INTO tarefa_responsavel (tarefa_id, usuario_id) VALUES (501, 7), (502, 7);

-- 8. FEEDBACKS VARIADOS
INSERT INTO feedback (id, tarefa_id, gestor_id, mensagem, data_criacao) VALUES 
(1, 101, 1, 'Excelente projeto histórico.', '2024-12-11'),
(2, 301, 1, 'Bom fechamento em Maio.', '2026-05-31'),
(3, 401, 1, 'Pedro, ótimo rendimento neste mês!', NOW());

SET SQL_SAFE_UPDATES = 1;
COMMIT;
