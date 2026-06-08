# Atividade 5: Estruturação dos Elementos Finais da Proposta
**Grupo:** 05  
**Projeto:** TarefasRH Potiguar (Inteligência e Automação Administrativa)  
**Solicitante (Empresa):** RH Potiguar Home Center SA  

---

### Elemento 1: O que faz
**Automatiza** a tabulação de atividades administrativas e **metrifica** a aderência ao cargo através de uma interface integrada, transformando o fluxo de trabalho manual em dashboards estratégicos para a eliminação das 60 horas extras do setor.

---

### Elemento 2: Quais dados usa

| Dado | De onde vem | Quem registra | Frequência | Crítico? |
| :--- | :--- | :--- | :--- | :--- |
| **Escopo de Complexidade** | Percepção de esforço (B/M/A) | Executor (Colab/Gestor) | A cada tarefa | **Sim** |
| **Aderência Contratual** | Descrição de cargo oficial | Criador da tarefa | No registro | **Sim** |
| **Aderência Operacional** | Auto-auditoria do executor | Colaborador | Na conclusão | **Sim** |
| **Carimbo de Produtividade** | Logs de tempo do sistema | Automático (Sistema) | Por tarefa | **Sim** |
| **Capacidade Operativa** | Dados de Admissão/Turnover | Administrador | Mensal | **Sim** |

---

### Elemento 3: Como funciona

*   **Passo 1 (Input Inteligente):** O sistema substitui o preenchimento manual de planilhas por uma interface ágil onde o Gestor e o Colaborador registram suas demandas diárias, definindo a complexidade e o alinhamento ao cargo.
*   **Passo 2 (Sincronização Ativa):** O motor Java processa os dados brutos e os converte em "Pontos de Impacto", alimentando automaticamente uma base centralizada no Google Sheets sem intervenção manual.
*   **Passo 3 (Auditoria de Cargos):** O sistema cruza a visão de quem pediu a tarefa com a visão de quem a executou, gerando o indicador de desvio de função solicitado pela empresa.
*   **Passo 4 (Visualização Estratégica):** Os dados são espelhados em Dashboards no Looker Studio, permitindo que a empresa identifique semanalmente quais atividades precisam ser automatizadas ou redistribuídas para zerar as horas extras.

---

### Elemento 4: Resultado esperado

**Reduzir** em **25%** o volume de horas extras (de 60h para 45h mensais) no prazo de **90 dias**, fornecendo à diretoria os dados exatos para a atualização das descrições de cargo e otimização dos processos manuais identificados.

---
**Evolução da Proposta:** O projeto nasceu da necessidade de uma planilha de acompanhamento e evoluiu para um **Ecossistema de Automação**, garantindo que as informações macros solicitadas pela cliente sejam geradas com integridade, agilidade e visão de longo prazo.
