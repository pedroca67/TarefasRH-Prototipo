# Atividade 5: Estruturação dos Elementos Finais da Proposta
**Grupo:** 05  
**Projeto:** TarefasRH Potiguar (Automação e Inteligência Administrativa)  
**Empresa Focal:** Potiguar Home Center SA  

---

### Elemento 1: O que faz
**Automatiza** a coleta de dados operacionais do RH através de um ambiente virtual e **gera** tabulações inteligentes em tempo real, eliminando o preenchimento manual de planilhas e auditando desvios de função que causam as 60h extras do setor.

---

### Elemento 2: Quais dados usa

| Dado | De onde vem | Quem registra | Frequência | Crítico? |
| :--- | :--- | :--- | :--- | :--- |
| **Complexidade Subjetiva** | Sistema TarefasRH | Gestor ou Colaborador | A cada tarefa | **Sim** |
| **Aderência ao Cargo** | Descrição do cargo atual | Gestor (Solicitante) | No registro | **Sim** |
| **Auto-auditoria de Função** | Percepção técnica do executor | Colaborador (Executor) | Na conclusão | **Sim** |
| **Volume de Esforço (pts)** | Algoritmo de conversão | Automático (Sistema) | No registro | **Sim** |
| **Horas de Capacidade** | Cadastro de Admissão | Automático (Sistema) | Mensal | **Sim** |

---

### Elemento 3: Como funciona

*   **Passo 1 (Coleta Descentralizada):** O sistema substitui o registro manual por um ambiente em nuvem onde Gestores e Colaboradores registram demandas, complexidade e aderência ao cargo em segundos.
*   **Passo 2 (Sincronização Ativa):** O "motor" da aplicação processa as interações e alimenta automaticamente uma base de dados no Google Sheets, garantindo integridade e eliminando o erro humano de digitação.
*   **Passo 3 (Análise de GAP):** O sistema cruza os dados de entrega com a descrição do cargo (auto-auditoria), gerando instantaneamente o indicador de desvio de função.
*   **Passo 4 (Visualização Gerencial):** Os dados tabulados são espelhados em um Dashboard no Looker Studio, permitindo que a Marianna acompanhe o desempenho semanal/mensal de forma visual e tome decisões rápidas.

---

### Elemento 4: Resultado esperado

**Reduzir** em **25%** o volume de horas extras do setor administrativo (de 60h para 45h mensais) nos primeiros **90 dias**, através da automação do fluxo de dados e da correção estratégica dos desvios de função identificados pela ferramenta.

---
**Diferencial Grupo 5:** A proposta evoluiu de uma simples planilha para um **Ecossistema de Automação**, garantindo que o dado chegue ao dashboard de forma limpa, frequente e sem dependência de preenchimentos manuais retroativos.
