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
O Looker Studio deve ser conectado à planilha integrada, utilizando as colunas exportadas automaticamente pelo sistema:

1.  **ID**: Identificador único da tarefa.
2.  **Título**: Nome da atividade.
3.  **Descrição**: Detalhamento do que foi solicitado.
4.  **Responsável(is)**: Lista de nomes dos colaboradores atribuídos.
5.  **Time**: Nome do setor/equipe responsável.
6.  **Categoria**: Área do RH (Recrutamento, DP, etc).
7.  **Previsto Cargo (Gestor)**: Expectativa inicial do gestor (SIM/NÃO).
8.  **Previsto Cargo (Colab)**: Realidade percebida pelo executor na conclusão (SIM/NÃO).
9.  **Criado Por**: Nome do usuário que abriu a tarefa.
10. **Unidade do Criador**: Loja de origem da demanda.
11. **Executor de Fato**: Nome de quem realmente marcou como concluída.
12. **Status**: Estado atual (CONCLUIDA, ATRASADA, etc).
13. **Complexidade**: Nível de dificuldade (BAIXA, MEDIA, ALTA).
14. **Esforço (Pts)**: Pontuação de impacto (1, 3 ou 5).
15. **Horas Est.**: Conversão de esforço em tempo (1pt = 2h).
16. **Prazo**: Data limite original.
17. **Conclusão**: Data e hora real da entrega.
18. **Evidência**: Texto descritivo da entrega feita pelo colaborador.
19. **Feedback Gestor**: Histórico consolidado de feedbacks dos gestores.

---

## 🛠️ Guia de Configuração de Gráficos (Looker Studio)

Para resolver o desafio da Potiguar, configure seus gráficos no Looker utilizando estas combinações de **Dimensões** e **Métricas**:

### 1. Visão Geral de Impacto (Scorecards / Visão Geral)
*   **Dado:** Total de Pontos de Impacto
    *   **Métrica:** `SUM(Esforço (Pts))`
*   **Dado:** Total de Horas Estimadas
    *   **Métrica:** `SUM(Horas Est.)`
*   **Dado:** Tarefas Atrasadas
    *   **Métrica:** `COUNT(ID)` onde `Status = "ATRASADA"`

### 2. O Mismatch de Cargos (Gráfico de Pizza ou Barras Empilhadas)
*   **Objetivo:** Mostrar o "Gap" entre o que o gestor pede e o que o RH faz de fato.
*   **Dimensão:** `Previsto Cargo (Colab)`
*   **Métrica:** `COUNT(ID)`
*   **Sort:** Descrescente por Valor.
*   *Dica:* Use filtros para ver por `Time` ou `Unidade do Criador`.

### 3. Produtividade por Área (Gráfico de Barras)
*   **Objetivo:** Identificar qual categoria de RH consome mais esforço.
*   **Dimensão:** `Categoria`
*   **Métrica:** `SUM(Esforço (Pts))`
*   **Sort:** Descrescente por Métrica.
*   **Tipo:** Barras Horizontais.

### 4. Ranking Histórico de Talentos (Tabela Dinâmica)
*   **Linha (Dimensão):** `Executor de Fato`
*   **Coluna (Dimensão):** `Conclusão` (Granularidade: Mês)
*   **Métrica:** `SUM(Esforço (Pts))`
*   **Sort:** Descrescente pelo Total.

### 5. Tendência de Carga de Trabalho (Série Temporal)
*   **Dimensão de Período:** `Prazo` ou `Conclusão`
*   **Métrica:** `SUM(Horas Est.)`
*   **Quebra de Dimensão:** `Status`
*   *Insight:* Ver picos de demanda ao longo do mês.

### 6. Mapa de Calor de Demandas (Google Maps / Gráfico de Mapa)
*   **Dimensão:** `Unidade do Criador` (Ex: Cohama, Centro, Imperatriz)
*   **Métrica:** `COUNT(ID)`
*   *Insight:* Visualizar quais lojas de São Luís e Interior geram mais demandas para o RH.

### 7. Funil de Eficiência (Gráfico de Funil)
*   **Dimensão:** `Status` (Etapas: PENDENTE -> EM_ANDAMENTO -> CONCLUIDA)
*   **Métrica:** `COUNT(ID)`
*   *Insight:* Ver em qual etapa as tarefas estão ficando "travadas".

### 8. Detalhamento de Auditoria (Tabela)
*   **Dimensões:** `Título`, `Executor de Fato`, `Conclusão`, `Evidência`, `Feedback Gestor`.
*   **Métrica:** `Esforço (Pts)`
*   **Sort:** `Conclusão` (Mais recente primeiro).

---

## 💡 Argumento para Apresentação
*"Enquanto nosso sistema garante que nenhuma tarefa seja esquecida e que o feedback chegue ao colaborador na hora certa, o Looker Studio nos permite olhar para trás e entender se a estrutura do nosso RH está saudável ou se precisamos atualizar nossas descrições de cargo."*
