# TarefasRH - Backend API

Esta é a API REST que sustenta o sistema de gestão de tarefas do RH da Potiguar.

## Stack Tecnológica
- **Linguagem:** Java 21
- **Framework:** Spring Boot 3.4.1
- **Banco de Dados:** MySQL 8.0
- **Segurança:** Spring Security + BCrypt
- **Persistência:** Spring Data JPA + Hibernate

## Arquitetura de Pastas
- `src/main/java/com/potiguar/tarefasrh/model`: Entidades JPA.
- `src/main/java/com/potiguar/tarefasrh/controller`: Endpoints REST.
- `src/main/java/com/potiguar/tarefasrh/service`: Lógica de negócio (incluindo integração com Google Sheets).
- `src/main/java/com/potiguar/tarefasrh/repository`: Interfaces de acesso ao banco.

## Endpoints Principais

### Autenticação
- `POST /api/auth/login`: Autentica usuário e retorna seus dados.

### Usuários
- `GET /api/usuarios`: Lista todos os usuários.
- `POST /api/usuarios`: Cria ou atualiza um usuário.
- `PATCH /api/usuarios/{id}/status`: Ativa/Desativa um usuário.

### Tarefas
- `GET /api/tarefas`: Lista tarefas (filtros: `responsavelId`, `timeId`).
- `GET /api/tarefas/{id}`: Detalhes de uma tarefa.
- `POST /api/tarefas`: Cria uma nova tarefa.
- `PUT /api/tarefas/{id}/status`: Atualiza status e evidência.
- `GET /api/tarefas/stats`: Estatísticas para o Dashboard do Gestor.

### Times
- `GET /api/times`: Lista os times cadastrados.

## Configuração do Banco de Dados
O arquivo `src/main/resources/application.properties` contém as configurações de conexão.
Por padrão:
- **DB:** `tarefasrh_prototipo`
- **User:** `root`
- **Password:** (vazio)

O Hibernate está configurado como `update`, então as tabelas são criadas automaticamente ao iniciar o projeto.

## Execução
```bash
./mvnw spring-boot:run
```
Porta padrão: `8080`
