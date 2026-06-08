# Atividade 5: Estruturação dos Elementos Finais da Proposta
**Grupo:** 05  
**Projeto:** TarefasRH Potiguar (Inteligência Administrativa)  
**Empresa Focal:** Potiguar Home Center SA  

---

### Elemento 1: O que faz
**Tabula** as atividades administrativas em tempo real e **audita** a aderência ao cargo de forma compartilhada (Gestor/Colaborador), visando atualizar descrições obsoletas e eliminar as 60 horas extras mensais do setor.

---

### Elemento 2: Quais dados usa

| Dado | De onde vem | Quem registra | Frequência | Crítico? |
| :--- | :--- | :--- | :--- | :--- |
| **Complexidade Subjetiva** | App TarefasRH | Gestor ou Colaborador | A cada tarefa | **Sim** |
| **Aderência ao Cargo** | Descrição do cargo atual | Gestor (Solicitante) | No registro | **Sim** |
| **Auto-auditoria de Função** | Percepção técnica do executor | Colaborador (Executor) | Na conclusão | **Sim** |
| **Volume de Esforço (pts)** | Motor de conversão (1, 3, 5) | Automático (Sistema) | No registro | **Sim** |
| **Tempo de Resposta** | Logs de criação e entrega | Automático (Sistema) | Por tarefa | **Sim** |

---

### Elemento 3: Como funciona

*   **Passo 1 (Registro Integrado):** O Gestor e o Colaborador alimentam continuamente o ambiente virtual com as demandas do dia, definindo a complexidade e a expectativa de cargo para cada tarefa.
*   **Passo 2 (Auto-auditoria):** Ao concluir uma tarefa, o colaborador valida se aquela atividade pertence ao seu escopo oficial, gerando o dado necessário para identificar descrições de cargo desatualizadas.
*   **Passo 3 (Tabulação Macro):** O sistema processa os dados e sincroniza automaticamente com o Google Sheets, eliminando o preenchimento manual e garantindo a atualização em tempo real para a diretoria.
*   **Passo 4 (Visualização Semanal/Mensal):** Através de Dashboards no Looker Studio, o Gestor visualiza os percentuais de esforço e identifica rapidamente quais atividades "extra-cargo" estão gerando o banco de horas extras.

---

### Elemento 4: Resultado esperado

**Reduzir** o volume de horas extras do setor administrativo de **60h para 45h mensais** (redução de 25%) nos primeiros **90 dias** de uso, fornecendo dados concretos para a atualização das descrições de cargo e automação de processos manuais.

---
**Nota:** Documento desenvolvido para a Atividade 5 da Fundação Digital, alinhado ao MVP funcional (Java/Node.js) e à necessidade estratégica da Potiguar Home Center SA.
