# Atividade 5: Estruturação dos Elementos Finais da Proposta
**Grupo:** 05  
**Projeto:** TarefasRH Potiguar  
**Empresa Focal:** Potiguar Home Center SA  

---

### Elemento 1: O que faz
**Sistematiza** a gestão de demandas operacionais do RH e **metrifica** o desvio de função através do cruzamento de dados de aderência ao cargo, fornecendo insumos para a eliminação das 60 horas extras mensais do setor.

---

### Elemento 2: Quais dados usa

| Dado | De onde vem | Quem registra | Frequência | Crítico? |
| :--- | :--- | :--- | :--- | : :--- |
| **Complexidade da Atividade** | Percepção de esforço (Baixa, Média, Alta) | Gestor/Solicitante | A cada nova tarefa | **Sim** |
| **Aderência Teórica** | Descrição de cargo oficial da empresa | Gestor/Solicitante | A cada nova tarefa | **Sim** |
| **Aderência Prática** | Percepção técnica do executor | Colaborador | No ato da conclusão | **Sim** |
| **Horário de Interação** | Logs de sistema (Data Criação/Conclusão) | Automático (Sistema) | Em cada evento | **Sim** |
| **Quadro de Pessoal Ativo** | Cadastro de usuários e data de admissão | Administrador/RH | No ato da contratação | **Sim** |

---

### Elemento 3: Como funciona

*   **Passo 1:** O Gestor registra a demanda no sistema definindo o nível de complexidade subjetiva (Baixa, Média ou Alta) e sinalizando se a atividade é prevista no cargo oficial do colaborador.
*   **Passo 2:** O Colaborador executa a atividade e, ao finalizar, registra sua percepção individual sobre a aderência daquela tarefa às suas funções contratuais.
*   **Passo 3:** O sistema converte os rótulos de complexidade em pesos de impacto numérico e exporta os dados consolidados para o Looker Studio.
*   **Passo 4:** O Gestor utiliza o dashboard para isolar os maiores "ladrões de tempo" (tarefas extra-cargo de alta complexidade) e readequar o quadro de pessoal.

---

### Elemento 4: Resultado esperado

**Reduzir** em **25%** o volume de horas extras registradas no setor de RH (redução de 60h para 45h mensais) em um prazo de **90 dias** após a implantação, através da correção de desvios de função identificados pela ferramenta.

---
**Checklist de Conformidade (Atividade 5):**
- [x] Verbo no presente no Elemento 1.
- [x] Tabela de dados com Fonte, Responsável e Frequência.
- [x] Fluxo de 4 passos com início no dado e fim na ação.
- [x] Resultado numérico ancorado no Statement de Dor.
