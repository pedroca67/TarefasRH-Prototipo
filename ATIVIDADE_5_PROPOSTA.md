# 📑 Atividade 5: Estruturação dos Elementos Finais da Proposta
**Projeto:** Inteligência Operacional e Auditoria de Cargo (TarefasRH)  
**Grupo:** 05  
**Empresa:** Potiguar Home Center SA  
**Data:** 08 de Junho de 2026  

---

### Elemento 1: O que faz
**Monitora** a produtividade individual através de pontos de impacto e **audita** o desvio de função em tempo real para identificar as tarefas manuais que geram as 60 horas extras mensais do setor de RH.

---

### Elemento 2: Quais dados usa

| Dado | De onde vem | Quem registra | Frequência | Crítico? |
| :--- | :--- | :--- | :--- | :--- |
| **Peso de Impacto** | Input do Sistema (1, 3, 5) | Gestor (na criação) | A cada tarefa | **Sim** |
| **Aderência Prevista** | Descrição de Cargo | Gestor (na criação) | A cada tarefa | **Sim** |
| **Aderência Real** | Percepção do Colaborador | Colaborador (na entrega) | Na conclusão | **Sim** |
| **Capacidade em Horas** | Banco de Dados | Sistema (via Admissão) | Mensal | **Sim** |
| **Evidência de Entrega** | Upload de Arquivo/Link | Colaborador | Na conclusão | Não |

---

### Elemento 3: Como funciona

*   **Passo 1:** O Gestor distribui as demandas atribuindo pesos de complexidade (1 a 5) e sinalizando se a tarefa é prevista no cargo oficial do colaborador.
*   **Passo 2:** O Colaborador executa a tarefa e, ao concluir, realiza uma **auto-auditoria**, confirmando se aquela atividade faz parte das suas atribuições reais.
*   **Passo 3:** O sistema cruza as duas visões (Gestor vs. Colaborador) e gera o indicador de **Aderência ao Cargo**, evidenciando visualmente onde o tempo está sendo desperdiçado com tarefas "extra-cargo".
*   **Passo 4:** Os dados são exportados automaticamente para o **Looker Studio**, onde a diretoria visualiza o GAP de produtividade e decide quais processos automatizar para eliminar as horas extras.

---

### Elemento 4: Resultado esperado

**Reduzir** o volume de horas extras do RH de **60h para menos de 45h mensais** (redução de 25%) nos primeiros **90 dias** de uso, através da readequação de funções e eliminação de tarefas de baixo impacto identificadas pela auditoria do sistema.

---

**Nota:** Este documento reflete o protótipo funcional desenvolvido pelo Grupo 5, integrando Backend Java, Frontend Node.js e Business Intelligence via Looker Studio.
