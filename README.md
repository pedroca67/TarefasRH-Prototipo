# TarefasRH Potiguar - Protótipo Functional

Este projeto é um protótipo funcional para o sistema de gestão de tarefas do RH da Potiguar Home Center. O objetivo é centralizar as demandas, permitir o acompanhamento de prazos e garantir a entrega de evidências de conclusão.

## Estrutura do Projeto

O sistema é dividido em duas partes independentes que se comunicam via API REST:

1.  **[Backend (Java/Spring Boot)](./back/README.md):** Responsável por toda a lógica de negócio, persistência de dados e segurança.
2.  **[Frontend (Node.js/Express)](./front/README.md):** Interface web para interação dos usuários (Gestores e Colaboradores).

---

## Como Executar (Guia Rápido)

### 1. Requisitos
- Java 21+
- Node.js 18+
- MySQL 8.0 rodando (porta 3306)

### 2. Configuração do Banco
O sistema usa o banco `tarefasrh_prototipo`. O Hibernate criará as tabelas automaticamente.
Certifique-se de que o MySQL está acessível (padrão: `root` sem senha).

### 3. Rodar o Backend
```bash
cd back
./mvnw spring-boot:run
```
A API estará disponível em `http://localhost:8080`.

### 4. Rodar o Frontend
```bash
cd front
npm install
npm start
```
Acesse `http://localhost:3000`.

---

## Credenciais de Teste
- **E-mail:** `gestor@potiguar.com.br`
- **Senha:** `admin123`

---

## Funcionalidades Implementadas
- **Dashboard Gerencial:** Visão consolidada de produtividade e status.
- **Gestão de Times:** Agrupamento de colaboradores por setor.
- **Ciclo de Vida de Tarefas:** Criação, atribuição, execução com evidência e auditoria de atrasos.
- **Controle de Acesso:** Diferenciação de permissões entre Gestor e Colaborador.
