# TarefasRH Potiguar - Protótipo Funcional (Grupo 5)

Este projeto é um protótipo funcional desenvolvido para o setor de RH da **Potiguar Home Center SA**, com o objetivo de solucionar a falta de métricas de produtividade e alinhar as responsabilidades dos colaboradores.

---

## 📋 Contexto do Desafio
A Potiguar enfrenta dificuldades em acompanhar a produtividade individual e coletiva do RH. Descrições de cargo desatualizadas geram confusão de responsabilidades, resultando em cerca de **60h extras mensais**, retrabalho e processos manuais.

**Statement de Dor:**
> *"Os gestores do setor de RH enfrentam a falta de dados sobre a produtividade diária dos funcionários, gerando a ausência de métricas para as avaliações periódicas da eficiência e do rendimento dos funcionários e das equipes."*

---

## ✅ O Que o Sistema Já Entrega
- **Registro de Tarefas:** Cadastro completo com status, complexidade (pontuada), categoria e prazo.
- **Atribuição Flexível:** Atribuição individual ou para times inteiros.
- **Dashboard Gerencial em Tempo Real:** 
    - Indicadores de produtividade e esforço concluído.
    - Métrica de **Aderência ao Cargo** (Expectativa do Gestor vs Realidade do Colaborador).
    - **Ranking de Top Performers:** Visualização mensal da produtividade por funcionário baseado em pontos de impacto.
    - Carga Horária Estimada dinâmica baseada na equipe ativa.
- **Sistema de Múltiplos Feedbacks:** Histórico completo de orientações do gestor em cada tarefa, com fuso horário local (São Luís/Nordeste).
- **Notificações Inteligentes:** Sino no header com alertas de novos feedbacks e mudanças de status.
- **Integração de Dados:** Exportação automática para **Google Sheets** e pronto para consumo no **Looker Studio**.
- **Acompanhamento Remoto:** Gestão total das atividades à distância com envio de evidências de conclusão.
- **Memória de Navegação:** Persistência de filtros, abas e posição de rolagem para uma experiência fluida.

---

## ⏳ O Que Ainda Falta
- **Indicador de Rotatividade (Turnover):** Módulo para monitorar a entrada e saída de funcionários do setor.
- **Relatório Individual de Impacto:** Geração de PDF detalhando o desempenho histórico de um colaborador específico.
- **Gamificação Avançada:** Sistema de conquistas e badges além do ranking de pontos.

---

## 🛠️ Estrutura Técnica
- **Backend:** Java 21, Spring Boot, Spring Security, JPA/Hibernate, MySQL.
- **Frontend:** Node.js, Express, EJS, Bootstrap 5, Chart.js.
- **Fuso Horário:** Configurado para `America/Fortaleza` (Horário de São Luís).

---

## 🚀 Como Executar

### 1. Requisitos
- Java 21+
- Node.js 18+
- MySQL 8.0 (porta 3306)

### 2. Execução Rápida (Windows)
Basta executar o arquivo `iniciar.bat` na raiz do projeto. Ele abrirá automaticamente as janelas do Backend e Frontend.

### 3. Credenciais de Teste
- **Gestor:** `gestor@potiguar.com.br` / `admin123`
- **Colaborador:** `colaborador@potiguar.com.br` / `user123`
