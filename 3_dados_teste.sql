USE railway;

-- 1. Limpeza Completa (Mantendo Admin e Times)
DELETE FROM tarefa_responsavel;
DELETE FROM notificacao;
DELETE FROM feedback;
DELETE FROM tarefa;
DELETE FROM usuario WHERE id > 1;

-- Reset de Auto-incremento (opcional, dependendo do banco)
ALTER TABLE tarefa AUTO_INCREMENT = 1;
ALTER TABLE usuario AUTO_INCREMENT = 2;

-- 2. Recriação dos 30 Colaboradores (Distribuídos em 4 times)
INSERT INTO usuario (id, nome, email, senha, nivel, ativo, data_criacao, time_id, loja, codigo_funcionario) VALUES
(2, 'Colaborador 2', 'colab2@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 1, 'LOJA COHAMA', 'POT-C1000'),
(3, 'Colaborador 3', 'colab3@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 1, 'LOJA COHAMA', 'POT-C1001'),
(4, 'Colaborador 4', 'colab4@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 1, 'LOJA COHAMA', 'POT-C1002'),
(5, 'Colaborador 5', 'colab5@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 1, 'LOJA COHAMA', 'POT-C1003'),
(6, 'Colaborador 6', 'colab6@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 1, 'LOJA COHAMA', 'POT-C1004'),
(7, 'Colaborador 7', 'colab7@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 1, 'LOJA COHAMA', 'POT-C1005'),
(8, 'Colaborador 8', 'colab8@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 2, 'LOJA COHATRAC', 'POT-C1006'),
(9, 'Colaborador 9', 'colab9@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 2, 'LOJA COHATRAC', 'POT-C1007'),
(10, 'Colaborador 10', 'colab10@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 2, 'LOJA COHATRAC', 'POT-C1008'),
(11, 'Colaborador 11', 'colab11@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 2, 'LOJA COHATRAC', 'POT-C1009'),
(12, 'Colaborador 12', 'colab12@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 2, 'LOJA COHATRAC', 'POT-C1010'),
(13, 'Colaborador 13', 'colab13@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 2, 'LOJA COHATRAC', 'POT-C1011'),
(14, 'Colaborador 14', 'colab14@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 2, 'LOJA COHATRAC', 'POT-C1012'),
(15, 'Colaborador 15', 'colab15@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 3, 'LOJA CENTRO', 'POT-C1013'),
(16, 'Colaborador 16', 'colab16@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 3, 'LOJA CENTRO', 'POT-C1014'),
(17, 'Colaborador 17', 'colab17@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 3, 'LOJA CENTRO', 'POT-C1015'),
(18, 'Colaborador 18', 'colab18@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 3, 'LOJA CENTRO', 'POT-C1016'),
(19, 'Colaborador 19', 'colab19@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 3, 'LOJA CENTRO', 'POT-C1017'),
(20, 'Colaborador 20', 'colab20@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 3, 'LOJA CENTRO', 'POT-C1018'),
(21, 'Colaborador 21', 'colab21@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 3, 'LOJA CENTRO', 'POT-C1019'),
(22, 'Colaborador 22', 'colab22@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1020'),
(23, 'Colaborador 23', 'colab23@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1021'),
(24, 'Colaborador 24', 'colab24@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1022'),
(25, 'Colaborador 25', 'colab25@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1023'),
(26, 'Colaborador 26', 'colab26@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1024'),
(27, 'Colaborador 27', 'colab27@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1025'),
(28, 'Colaborador 28', 'colab28@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1026'),
(29, 'Colaborador 29', 'colab29@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1027'),
(30, 'Colaborador 30', 'colab30@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1028'),
(31, 'Colaborador 31', 'colab31@potiguar.com.br', '$2a$10$8.UnVuG9HHgffUDAlk8qnOn6CpfMXVG7tt97ViqZzGdzbhQEBPf92', 'COLABORADOR', 1, NOW(), 4, 'LOJA CALHAU', 'POT-C1029');

-- 3. Geração das 300 Tarefas (Exemplo reduzido para o script SQL mas completo na lógica)
-- Utilizaremos uma Procedure para gerar a massa de dados de forma randômica e precisa
DELIMITER //

CREATE PROCEDURE PopularMassaDados()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE var_status VARCHAR(20);
    DECLARE var_prazo DATE;
    DECLARE var_criacao DATETIME;
    DECLARE var_colab_id BIGINT;
    DECLARE var_time_id BIGINT;
    DECLARE var_categoria VARCHAR(50);
    DECLARE var_complexidade VARCHAR(10);
    
    WHILE i <= 300 DO
        -- Escolher Categoria
        SET var_categoria = ELT(FLOOR(1 + RAND() * 8), 'RECRUTAMENTO_E_SELECAO', 'DEPARTAMENTO_PESSOAL', 'TREINAMENTO_E_DESENVOLVIMENTO', 'CARGOS_E_SALARIOS', 'BENEFICIOS', 'ENDOMARKETING', 'SAUDE_E_SEGURANCA', 'OUTROS');
        SET var_complexidade = ELT(FLOOR(1 + RAND() * 3), 'BAIXA', 'MEDIA', 'ALTA');
        
        -- Regra 7: Data de criação nos últimos 180 dias
        SET var_criacao = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY);
        
        -- Regra 6: Prazos (60% futuro, 40% passado)
        IF i <= 120 THEN
            SET var_prazo = DATE_SUB(CURDATE(), INTERVAL FLOOR(1 + RAND() * 60) DAY);
        ELSE
            SET var_prazo = DATE_ADD(CURDATE(), INTERVAL FLOOR(RAND() * 60) DAY);
        END IF;

        -- Distribuição de Status (~40% Concluída, ~30% Em Andamento, ~30% Pendente)
        IF i <= 120 THEN SET var_status = 'CONCLUIDA';
        ELSEIF i <= 210 THEN SET var_status = 'EM_ANDAMENTO';
        ELSE SET var_status = 'PENDENTE';
        END IF;

        -- Selecionar Colaborador e Time
        SET var_colab_id = FLOOR(2 + RAND() * 30);
        SELECT time_id INTO var_time_id FROM usuario WHERE id = var_colab_id;

        INSERT INTO tarefa (
            titulo, descricao, complexidade, categoria, status, 
            data_criacao, data_prazo, criado_por, responsavel_id, time_id, previsto_no_cargo_gestor
        ) VALUES (
            CONCAT('Demanda ', var_categoria, ' #', i),
            CONCAT('Descrição detalhada da atividade operacional ', i),
            var_complexidade, var_categoria, var_status,
            var_criacao, var_prazo, 1, var_colab_id, var_time_id, 1
        );

        -- Tabela Associativa (Regra 3)
        INSERT INTO tarefa_responsavel (tarefa_id, usuario_id) VALUES (LAST_INSERT_ID(), var_colab_id);

        -- Regra 5: Preencher campos se CONCLUIDA
        IF var_status = 'CONCLUIDA' THEN
            UPDATE tarefa SET 
                concluido_por = var_colab_id,
                data_conclusao = DATE_ADD(var_criacao, INTERVAL FLOOR(RAND() * 5) DAY),
                evidencia = 'Atividade realizada conforme procedimento padrão. Documentos arquivados.',
                previsto_no_cargo_colaborador = 1
            WHERE id = LAST_INSERT_ID();
            
            -- Regra 8: 20% das concluídas com feedback
            IF i <= 24 THEN
                UPDATE tarefa SET 
                    data_feedback = NOW(),
                    feedback_gestor = 'Excelente entrega. Prazo respeitado e qualidade impecável.'
                WHERE id = LAST_INSERT_ID();
            END IF;
        END IF;

        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

-- Executa a procedure para gerar os dados
CALL PopularMassaDados();

-- Remove a procedure após o uso
DROP PROCEDURE PopularMassaDados;

-- 4. Verificação Final
SELECT status, COUNT(*) FROM tarefa GROUP BY status;
