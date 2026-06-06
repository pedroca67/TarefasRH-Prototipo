-- ======================================================
-- SCRIPT DE HARD RESET E GERAÇÃO MASSIVA (PROCEDURES)
-- ======================================================

SET NAMES utf8mb4;
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

-- 1. ESTRUTURA
INSERT INTO time (id, nome) VALUES 
(1, 'Recrutamento e Seleção'), (2, 'Departamento Pessoal'), 
(3, 'Treinamento e Desenvolvimento'), (4, 'Benefícios');
-- 2. GESTOR (Admitido em Jan/2024) - Senha: admin123
INSERT INTO usuario (id, nome, email, senha, nivel, loja, foto_url, ativo, data_criacao, codigo_funcionario) VALUES 
(1, 'Gestor Admin', 'gestor@potiguar.com.br', '$2a$10$a4PrVfZ13kaB8atN//DOPOFhhPOu9oEWqtgBU8/ezKiKtITj95lfi', 'GESTOR', 'Matriz', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Gestor', 1, '2024-01-01 08:00:00', '001');

-- 2. PROCEDURE COLABORADORES
DROP PROCEDURE IF EXISTS GerarColaboradoresMassivo;
DELIMITER $$
CREATE PROCEDURE GerarColaboradoresMassivo(IN qtd INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE r_time INT;
    DECLARE r_loja VARCHAR(50);
    DECLARE r_ativo TINYINT(1);
    DECLARE r_data_admissao DATETIME;
    DECLARE start_code INT DEFAULT 1000;

    WHILE i < qtd DO
        SET r_time = FLOOR(1 + (RAND() * 4));
        SET r_loja = ELT(FLOOR(1 + (RAND() * 5)), 'Cohama', 'Forquilha', 'Centro', 'Africanos', 'Imperatriz');
        SET r_data_admissao = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 600) DAY);
        SET r_ativo = IF(RAND() > 0.1, 1, 0); 

        INSERT INTO usuario (nome, email, senha, nivel, loja, time_id, foto_url, ativo, data_criacao, data_desativacao, codigo_funcionario)
        VALUES (
            CONCAT('Colaborador ', i+2),
            CONCAT('user', i+2, '@potiguar.com.br'),
            '$2a$10$6e5jq348SirFXZR9779U6.LbW5jvJlAm2bXv5CJaRZIepHqS3eSTu', -- Senha: 123456
            'COLABORADOR',
            r_loja,
            r_time,
            CONCAT('https://api.dicebear.com/7.x/avataaars/svg?seed=User', i+2),
            r_ativo,
            r_data_admissao,
            IF(r_ativo = 0, DATE_ADD(r_data_admissao, INTERVAL FLOOR(RAND() * 120) DAY), NULL),
            CAST(start_code + i AS CHAR)
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 3. PROCEDURE TAREFAS
DROP PROCEDURE IF EXISTS GerarTarefasMassivo;
DELIMITER $$
CREATE PROCEDURE GerarTarefasMassivo(IN qtd INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE r_status VARCHAR(20);
    DECLARE r_complex VARCHAR(10);
    DECLARE r_cat VARCHAR(30);
    DECLARE r_colab_id INT;
    DECLARE r_data_criacao DATETIME;
    DECLARE r_data_prazo DATE;
    DECLARE r_data_conclusao DATETIME;
    DECLARE t_id INT;
    
    WHILE i < qtd DO
        SET r_status = ELT(FLOOR(1 + (RAND() * 4)), 'PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA', 'CONCLUIDA');
        SET r_complex = ELT(FLOOR(1 + (RAND() * 3)), 'BAIXA', 'MEDIA', 'ALTA');
        SET r_cat = ELT(FLOOR(1 + (RAND() * 8)), 'RECRUTAMENTO_E_SELECAO', 'DEPARTAMENTO_PESSOAL', 'TREINAMENTO_E_DESENVOLVIMENTO', 'CARGOS_E_SALARIOS', 'BENEFICIOS', 'ENDOMARKETING', 'SAUDE_E_SEGURANCA', 'OUTROS');
        SET r_colab_id = (SELECT id FROM usuario WHERE nivel = 'COLABORADOR' ORDER BY RAND() LIMIT 1);
        SET r_data_criacao = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 550) DAY);
        SET r_data_prazo = DATE_ADD(r_data_criacao, INTERVAL FLOOR(5 + RAND() * 20) DAY);
        
        IF r_status = 'CONCLUIDA' THEN
            SET r_data_conclusao = DATE_SUB(r_data_prazo, INTERVAL FLOOR(RAND() * 5) DAY);
            IF r_data_conclusao > NOW() THEN SET r_data_conclusao = NOW(); END IF;
        ELSE
            SET r_data_conclusao = NULL;
            IF r_data_prazo < CURDATE() THEN SET r_status = 'ATRASADA'; END IF;
        END IF;

        -- Colunas corrigidas: criado_por e concluido_por
        INSERT INTO tarefa (titulo, descricao, categoria, complexidade, status, data_prazo, data_conclusao, criado_por, concluido_por, evidencia, previsto_no_cargo_gestor, previsto_no_cargo_colaborador, data_criacao)
        VALUES (
            CONCAT('Demanda ', r_cat, ' #', i+1),
            'Gerado automaticamente para análise de BI.',
            r_cat, r_complex, r_status, r_data_prazo, r_data_conclusao,
            1, IF(r_status = 'CONCLUIDA', r_colab_id, NULL),
            IF(r_status = 'CONCLUIDA', 'Evidência de conclusão validada.', NULL),
            IF(RAND() > 0.2, 1, 0), IF(r_status = 'CONCLUIDA', IF(RAND() > 0.3, 1, 0), NULL),
            r_data_criacao
        );
        
        SET t_id = LAST_INSERT_ID();
        INSERT INTO tarefa_responsavel (tarefa_id, usuario_id) VALUES (t_id, r_colab_id);
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 4. EXECUÇÃO
CALL GerarColaboradoresMassivo(30);
CALL GerarTarefasMassivo(2000);

SET SQL_SAFE_UPDATES = 1;
COMMIT;
