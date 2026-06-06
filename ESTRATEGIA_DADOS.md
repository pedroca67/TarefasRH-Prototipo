# 📊 Estratégia de Dados e BI (Looker Studio) - Grupo 5

Este documento define a separação entre a camada **Operacional (Aplicativo)** e a camada **Estratégica (Looker Studio)**, focando em resolver as dores da Potiguar Home Center.

---

## 🎯 Divisão de Responsabilidades

### 1. Camada Operacional (Dashboard do App)
**Objetivo:** Ação imediata e gestão do "agora".
- **Atenção Prioritária:** Lista de tarefas que venceram hoje ou estão atrasadas para cobrança rápida.
- **Feedbacks Pendentes:** Tarefas concluídas que aguardam avaliação do gestor.
- **Gamificação em Tempo Real:** Ranking de pontos do mês para motivar a equipe.
- **Status do Dia:** Distribuição percentual das tarefas abertas hoje.

### 2. Camada Estratégica (Looker Studio)
**Objetivo:** Análise de tendências, histórico e causa raiz dos problemas.
- **Turnover e Retenção:** Comparativo histórico de entradas e saídas.
- **Lacuna de Cargo (The "Gap"):** Análise da diferença entre o que o gestor pede e o que o colaborador faz.
- **Capacidade vs. Demanda:** Tendência de carga horária ao longo do tempo.

---

## 📈 Sugestões de Visualizações para o Looker Studio

### 1. Painel de Aderência ao Cargo (Foco no Problema)
*   **Gráfico de Barras Empilhadas:** "Visão Gestor" vs "Visão Colaborador" por Categoria de Tarefa.
    *   *Insight:* Identificar se o RH está gastando muito tempo em tarefas "Extra-Cargo" (ex: atividades manuais que não deveriam ser do setor).
*   **Gráfico de Dispersão:** Impacto (Pontos) vs Aderência (%).
    *   *Insight:* Colaboradores que produzem muito, mas em tarefas fora do cargo (risco de desmotivação).

### 2. Painel de Turnover (Foco na Evidência)
*   **Gráfico de Linha:** Taxa de Turnover Mensal (Admissões vs Desligamentos).
*   **Gráfico de Pizza:** Motivos de Desligamento (se integrados futuramente) ou Desligamentos por Unidade (Lojas).
    *   *Insight:* Verificar se lojas específicas em São Luís têm rotatividade maior.

### 3. Painel de Produtividade Histórica
*   **Série Temporal:** Pontos de Impacto entregues por mês.
    *   *Insight:* O setor está ficando mais eficiente ou a produtividade está caindo?
*   **Tabela de Calor (Heatmap):** Complexidade de tarefas por dia da semana.
    *   *Insight:* Descobrir "gargalos" em dias de fechamento de folha ou contratação.

---

## 🛠️ Fontes de Dados (Planilha Base)
O Looker Studio deve ser conectado à planilha integrada através de duas fontes distintas (Abas):

### 1. Aba `BASE_TAREFAS`
Contém o fluxo operacional detalhado. Use esta aba para todos os gráficos de produtividade.
- **Colunas:** ID, Título, Descrição, Responsável(is), Time, Categoria, Previsto Cargo (Gestor), Previsto Cargo (Colab), Criado Por, Unidade do Criador, Executor de Fato, Status, Complexidade, Esforço (Pts), Horas Est., Prazo, Conclusão, Evidência, Feedback Gestor.

### 2. Aba `BASE_TURNOVER`
Contém o cadastro histórico da equipe. Use esta aba para análise de rotatividade.
- **Colunas:** ID_Usuario, Nome, E-mail, Loja, Time, Nível, Status (ATIVO/INATIVO), Data_Admissao, Data_Desligamento.

### 3. Aba `RESUMO_METRICAS` (NOVA)
Contém indicadores consolidados para análise de eficiência global da empresa.
- **Colunas:** Mês/Ano, Capacidade Total (Horas), Horas Entregues (Produtividade Real).
- **Uso no Looker:** Criar o gráfico de **Eficiência Global** (Capacidade vs. Realidade).

---

## 🛠️ Guia de Configuração de Gráficos (Looker Studio)

### A. Usando a aba `BASE_TAREFAS`:
1.  **Visão Geral (Scorecards):** `SUM(Esforço (Pts))`, `SUM(Horas Est.)`.
2.  **Mismatch de Cargos (Pizza):** Dimensão `Previsto Cargo (Colab)`.
3.  **Produtividade (Barras):** Dimensão `Categoria` | Métrica `SUM(Esforço (Pts))`.
4.  **Mapa de Demandas:** Dimensão `Unidade do Criador`.

### B. Usando a aba `BASE_TURNOVER`:
1.  **Taxa de Turnover (Série Temporal):**
    *   **Dimensão de Período:** `Data_Admissao` (para crescimento) ou `Data_Desligamento` (para perda).
    *   **Métrica:** `COUNT(ID_Usuario)`.

### C. Usando a aba `RESUMO_METRICAS`:
1.  **Eficiência Global (Gráfico de Combinação):**
    *   **Dimensão de Período:** `Mês/Ano`.
    *   **Métrica 1:** `SUM(Capacidade Total (Horas))` - Barra.
    *   **Métrica 2:** `SUM(Horas Entregues (Produtividade Real))` - Linha.
    *   *Insight:* Mostrar para a diretoria o gap entre horas pagas e tarefas registradas.

---

## 💡 Argumento para Apresentação
*"Enquanto nosso sistema garante que nenhuma tarefa seja esquecida e que o feedback chegue ao colaborador na hora certa, o Looker Studio nos permite olhar para trás e entender se a estrutura do nosso RH está saudável ou se precisamos atualizar nossas descrições de cargo."*
