# 🚀 Guia Definitivo: Montagem do Looker Studio (Grupo 5)

Este guia é o seu "passo a passo" técnico para transformar as abas da Planilha Google em um Dashboard de nível executivo para a Potiguar Home Center.

---

## 🛠️ Passo 0: Conexão de Dados
1. No Looker Studio, clique em **Criar > Relatório**.
2. Selecione **Planilhas Google**.
3. Adicione **3 fontes de dados** (uma para cada aba):
    *   Fonte A: `BASE_TAREFAS`
    *   Fonte B: `BASE_TURNOVER`
    *   Fonte C: `RESUMO_METRICAS`

---

## 📊 Página 1: Performance e Produtividade (Aba `BASE_TAREFAS`)
*Esta tela foca em mostrar QUANTO o RH está produzindo.*

### 1. Indicadores de Topo (Scorecards)
*   **Card 1:** Impacto Total. Métrica: `SUM(Esforço (Pts))`.
*   **Card 2:** Horas Registradas. Métrica: `SUM(Horas Est.)`.
*   **Card 3:** Média de Pontos/Tarefa. Métrica: `AVG(Esforço (Pts))`.

### 2. Distribuição por Categoria (Gráfico de Barras Horizontais)
*   **Dimensão:** `Categoria`.
*   **Métrica:** `SUM(Esforço (Pts))`.
*   **Dica:** Use a cor **Azul Potiguar (#170e4a)**.

### 3. Ranking de Talentos Histórico (Tabela com Mapas de Calor)
*   **Dimensão:** `Executor de Fato`.
*   **Métrica:** `SUM(Esforço (Pts))`.
*   **Estilo:** Aplique "Mapa de calor" na coluna da métrica para destacar os melhores.

---

## ⚠️ Página 2: A Chaga do Problema - Aderência ao Cargo (Aba `BASE_TAREFAS`)
*Esta é a tela mais importante. Ela prova que a descrição de cargo está falha.*

### 1. O "Gap" de Funções (Gráfico de Pizza)
*   **Dimensão:** `Previsto Cargo (Colab)`.
*   **Métrica:** `COUNT(ID)`.
*   **Legenda:** SIM (No Cargo) / NÃO (Extra-Cargo).
*   **Cores:** Verde para SIM, Vermelho para NÃO.
*   **Insight:** Se o "NÃO" for grande, você prova o desvio de função.

### 2. Aderência por Unidade/Loja (Gráfico de Barras Empilhadas)
*   **Dimensão:** `Unidade do Criador`.
*   **Métrica:** `Record Count`.
*   **Quebra de Dimensão:** `Previsto Cargo (Colab)`.
*   **Insight:** Descubra qual loja gera mais demandas "aleatórias" para o RH.

---

## 📈 Página 3: Eficiência Global e Turnover (Abas `BASE_TURNOVER` e `RESUMO_METRICAS`)
*Visão macro para a diretoria.*

### 1. Eficiência Global (Gráfico de Combinação) -> **USAR FONTE `RESUMO_METRICAS`**
*   **Dimensão:** `Mês/Ano`.
*   **Métrica 1 (Barra):** `SUM(Capacidade Total (Horas))`. -> Cor: Cinza Claro.
*   **Métrica 2 (Linha):** `SUM(Horas Entregues)`. -> Cor: **Vermelho Potiguar (#ed1a25)**.
*   **Narrativa:** A área entre a linha e a barra é o **dinheiro desperdiçado** com falta de registro.

### 2. Movimentação de Equipe (Gráfico de Linhas) -> **USAR FONTE `BASE_TURNOVER`**
*   **Dimensão de Período:** `Data_Admissao`.
*   **Métrica:** `COUNT(ID_Usuario)`.
*   **Comparativo:** Adicione uma métrica de `COUNT` para `Data_Desligamento`.

---

## 🎨 Dicas de Design (Visual Potiguar)
*   **Cores Principais:**
    *   Azul Escuro: `#170e4a` (Títulos e Barras principais).
    *   Amarelo: `#d5de23` (Destaques e Alertas).
    *   Vermelho: `#ed1a25` (Atrasos e Desligamentos).
*   **Filtros de Página:** Coloque um "Filtro de Período" e um "Filtro de Time" fixos no topo de todas as páginas.

---

## 💡 Script de Apresentação (Use estas falas)
*   *"Enquanto o sistema operacional cuida das 2.000 tarefas que vocês veem aqui..."*
*   *"O Looker nos mostra que 30% do tempo do nosso time em São Luís está sendo gasto em atividades que não pertencem ao RH."*
*   *"Nossa Eficiência Global está em 40%, o que justifica a implementação deste sistema para automatizar os outros 60%."*
