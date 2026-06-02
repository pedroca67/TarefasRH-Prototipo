# TarefasRH - Frontend Web

Interface web para o sistema de gestão de tarefas do RH, desenvolvida com Node.js e Express.

## Stack Tecnológica
- **Runtime:** Node.js
- **Framework Web:** Express
- **Template Engine:** EJS
- **Estilização:** Bulma CSS
- **Cliente HTTP:** Axios
- **Sessão:** express-session

## Estrutura do Projeto
- `src/app.js`: Configuração do servidor, middlewares de autenticação e rotas principais.
- `src/services/api.js`: Camada de comunicação com o Backend (Axios).
- `src/views/`: Templates HTML (EJS) divididos por módulos (auth, dashboard, tarefas, usuarios).
- `src/public/`: Arquivos estáticos (CSS, Imagens).

## Configuração (.env)
Crie um arquivo `.env` na pasta `front/` com as seguintes variáveis:
```env
PORT=3000
API_URL=http://localhost:8080/api
SESSION_SECRET=seu_segredo_aqui
```

## Funcionalidades de Interface
- **Dashboard Dinâmico:** Visões diferentes para Gestores e Colaboradores.
- **Filtros Automáticos:** Colaboradores vêem apenas suas tarefas e as do seu time.
- **Gestão de Status:** Interface simplificada para mudar status de tarefas com upload de evidência (texto/link).
- **Administração:** Cadastro de colaboradores e gestão de ativos/inativos.

## Execução
```bash
npm install
npm start
```
Acesse em: `http://localhost:3000`
