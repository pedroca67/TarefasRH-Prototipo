# Atividade 5: Estruturação dos Elementos Finais da Proposta
**Projeto:** TarefasRH Potiguar  
**Empresa:** Potiguar Home Center SA  
**Data:** 08 de Junho de 2026  

---

### Elemento 1: O que faz
> "Centraliza a demanda operacional do RH e metrifica a produtividade individual em tempo real, evidenciando desvios de função através do indicador de aderência ao cargo."

---

### Elemento 2: Quais dados usa
Abaixo, os dados críticos para o funcionamento da solução:

| Dado | De onde vem | Quem registra | Frequência |
| :--- | :--- | :--- | :--- |
| **Registro de Tarefa** (Título, Categoria, Prazo) | TarefasRH (MVP built) | Gestor ou Colaborador | A cada nova demanda |
| **Nível de Complexidade** (Baixa, Média, Alta) | Lógica interna de Pesos | Gestor ou Colaborador | No ato do registro |
| **Expectativa de Cargo** (Previsto/Extra) | Filtro de Auditoria | Gestor ou Colaborador | No ato do registro |
| **Evidência de Entrega** (Texto/Link/Doc) | TarefasRH (MVP built) | Colaborador (Executor) | No ato da conclusão |

---

### Elemento 3: Como funciona
O fluxo da solução segue 4 passos fundamentais:

*   **Passo 1:** A demanda é registrada no sistema, seja pelo **Colaborador** (autonomia) ou pelo **Gestor** (delegação direta para indivíduos ou times inteiros). No registro, define-se a complexidade e a aderência ao cargo.
*   **Passo 2:** O motor do sistema converte a complexidade em **Pontos de Impacto** e organiza as entregas no calendário visual e na lista de "Atenção Prioritária".
*   **Passo 3:** O sistema sincroniza esses dados em segundo plano com o Google Sheets, alimentando automaticamente um painel de **Business Intelligence (Looker Studio)**.
*   **Passo 4:** O Gestor utiliza os indicadores de **Aderência ao Cargo** para identificar gargalos operacionais e atualizar as descrições de cargo desatualizadas, eliminando o retrabalho.

---

### Elemento 4: Resultado esperado
O impacto numérico direto no Statement de Dor:

> "Reduzir o volume de horas extras do setor de RH de **60h/mês** para menos de **20h/mês** nos primeiros 90 dias após a implantação total do sistema."

---

**Nota:** Este documento reflete o MVP funcional já desenvolvido e validado tecnicamente pelo Grupo 5.
