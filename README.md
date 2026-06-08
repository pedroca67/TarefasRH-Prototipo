# TarefasRH Potiguar - Protótipo Funcional 

Este projeto é um protótipo funcional de alta performance desenvolvido para o setor de RH da **Potiguar Home Center SA**, com o objetivo de centralizar demandas, mapear produtividade real e evidenciar gargalos de desvio de função.

---

## 📋 O Desafio
A Potiguar enfrenta dificuldades em acompanhar a produtividade do RH. Descrições de cargo desatualizadas geram sobrecarga e falta de visibilidade sobre o "Estoque de Trabalho", resultando em excesso de horas extras e processos manuais.

---

## ✅ Funcionalidades Entregues
- **Gestão de Demanda & Estoque de Trabalho:** Controle de tarefas com foco no acompanhamento da "Dívida Ativa" (tarefas atrasadas e pendentes).
- **Dashboard Executivo:** Indicadores em tempo real de Produtividade, Carga Horária (Capacidade vs. Entrega) e o exclusivo medidor de **Aderência ao Cargo**.
- **Painel do Colaborador & Gamificação:** Workspace individual focado em organizar a rotina ("Atenção Prioritária") e incentivar entregas via medalhas de impacto (Bronze, Prata, Ouro).
- **Calendário Interativo:** Visão mensal e semanal colorida para gestão visual de prazos.
- **Sistema de Múltiplos Feedbacks:** Histórico completo de orientações do gestor com notificações em tempo real.
- **Integração Data Studio (Looker):** Sincronização em segundo plano com o Google Sheets, alimentando painéis de Business Intelligence (BI) de forma invisível.
- **Alta Performance & UX:** Banco de dados otimizado contra vazamentos de memória (OOM), paginação inteligente e restauração global de navegação (Scroll Memory).

---

## 🛠️ Tecnologias Utilizadas
- **Backend:** Java 21, Spring Boot 3.2, Hibernate 6, MySQL 8.0 (Deploy otimizado).
- **Frontend:** Node.js, Express, EJS, Bootstrap 5, FullCalendar, Chart.js.
- **Integração:** Google Sheets API.
- **Fuso Horário:** `America/Fortaleza` (Horário de São Luís).

---

## 🚀 Como Executar Localmente

### 1. Requisitos
- Java 21+
- Node.js 18+

### 2. Passo a Passo
1. Inicie o Backend em Java (porta `8080`).
2. Acesse a pasta `front` e execute `npm run dev`.
3. Acesse `http://localhost:3000` no navegador.

### 3. Ferramentas Administrativas
- **Acesso:** Use os e-mails/matrículas cadastradas no banco de dados.
- **Sincronização Manual (Admin):** Para forçar a carga de dados para o BI sem interferir na interface, acesse a rota oculta `/admin/forcar-sync` estando logado no sistema.
