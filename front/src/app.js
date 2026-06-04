const express = require('express');
const session = require('express-session');
const path = require('path');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;

// Configurações
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.set('trust proxy', 1); // Confiar no proxy do Railway (importante para sessões)
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Middleware para evitar cache (impede voltar após logout)
app.use((req, res, next) => {
    res.set('Cache-Control', 'no-cache, private, no-store, must-revalidate, max-stale=0, post-check=0, pre-check=0');
    next();
});

app.use(session({
    secret: process.env.SESSION_SECRET || 'potiguar_rh_secret_key_123',
    resave: true, // Forçar o salvamento da sessão
    saveUninitialized: true, // Forçar a criação da sessão mesmo vazia
    proxy: true,
    name: 'tarefasrh.sid', // Nome personalizado para o cookie
    cookie: { 
        secure: true, // Railway sempre usa HTTPS
        httpOnly: true,
        maxAge: 1000 * 60 * 60 * 24, // 24 horas
        sameSite: 'lax'
    }
}));

// Ajuste para desenvolvimento local (onde não tem HTTPS)
if (process.env.NODE_ENV !== 'production') {
    app.set('trust proxy', 0);
    const sessionConfig = app.get('sessionConfig'); // Isso é só ilustrativo, vamos aplicar direto no middleware acima
}

// Middleware de Autenticação
const authMiddleware = (req, res, next) => {
    if (!req.session.usuario) {
        return res.redirect('/login');
    }
    next();
};

const gestorMiddleware = (req, res, next) => {
    if (!req.session.usuario || req.session.usuario.nivel !== 'GESTOR') {
        return res.status(403).send('Acesso Negado');
    }
    next();
};

// Injeção de usuário e mensagens nas views
app.use((req, res, next) => {
    res.locals.usuario = req.session.usuario || null;
    res.locals.success = req.session.success || null;
    res.locals.error = req.session.error || null;
    res.locals.googleSheetsUrl = process.env.GOOGLE_SHEETS_URL || null;
    res.locals.lookerStudioUrl = process.env.LOOKER_STUDIO_URL || null;
    delete req.session.success;
    delete req.session.error;
    next();
});

// Rotas
const apiService = require('./services/api');

app.get('/', (req, res) => {
    if (req.session.usuario) {
        return res.redirect('/dashboard');
    }
    res.redirect('/login');
});

app.get('/login', (req, res) => {
    res.render('auth/login', { error: null, email: '' });
});

app.post('/login', async (req, res) => {
    const { email, senha } = req.body;
    try {
        const response = await apiService.login(email, senha);
        req.session.usuario = response.data;
        res.redirect('/dashboard');
    } catch (error) {
        res.render('auth/login', { error: 'E-mail ou senha inválidos', email });
    }
});

app.get('/logout', (req, res) => {
    req.session.destroy((err) => {
        if (err) {
            console.error('Erro ao destruir sessão:', err);
        }
        res.clearCookie('connect.sid'); // Limpa o cookie da sessão
        res.redirect('/login');
    });
});

app.get('/perfil', authMiddleware, async (req, res) => {
    try {
        const tarefas = (await apiService.getTarefas(req.session.usuario.id)).data;
        const stats = {
            total: tarefas.length,
            concluida: tarefas.filter(t => t.status === 'CONCLUIDA').length,
            atrasada: tarefas.filter(t => t.status === 'ATRASADA').length
        };
        res.render('usuarios/perfil', { stats, currentPage: 'perfil' });
    } catch (error) {
        res.status(500).send('Erro ao carregar perfil');
    }
});

app.get('/dashboard', authMiddleware, async (req, res) => {
    try {
        if (req.session.usuario.nivel === 'GESTOR') {
            const stats = (await apiService.getStats()).data;
            const tarefas = (await apiService.getTarefas()).data;
            const times = (await apiService.getTimes()).data;
            res.render('dashboard/gestor', { stats, tarefas, times, currentPage: 'dashboard' });
        } else {
            const minhasTarefas = (await apiService.getTarefas(req.session.usuario.id)).data;
            let todasTarefas = [];
            if (req.session.usuario.time) {
                todasTarefas = (await apiService.getTarefas(null, req.session.usuario.time.id)).data;
            }
            res.render('dashboard/colaborador', { 
                tarefas: minhasTarefas, // Para os cards de estatísticas
                minhasTarefas, 
                todasTarefas, 
                currentPage: 'dashboard' 
            });
        }
    } catch (error) {
        res.status(500).send('Erro ao carregar dashboard');
    }
});

app.get('/tarefas', authMiddleware, async (req, res) => {
    try {
        if (req.session.usuario.nivel === 'GESTOR') {
            res.redirect('/dashboard');
        } else {
            res.redirect('/dashboard'); // Colaborador agora tem tudo no dashboard
        }
    } catch (error) {
        res.status(500).send('Erro ao redirecionar tarefas');
    }
});

// Tarefas
app.get('/tarefas/nova', authMiddleware, async (req, res) => {
    try {
        if (req.session.usuario.nivel === 'GESTOR') {
            const usuarios = (await apiService.getUsuarios()).data;
            const times = (await apiService.getTimes()).data;
            res.render('tarefas/form', { usuarios, times, currentPage: 'tarefas' });
        } else {
            res.render('tarefas/form_colaborador', { currentPage: 'dashboard' });
        }
    } catch (error) {
        res.status(500).send('Erro ao carregar formulário');
    }
});

app.post('/tarefas', authMiddleware, async (req, res) => {
    try {
        const { titulo, descricao, complexidade, dataPrazo, responsavelId, timeId } = req.body;
        const payload = {
            titulo,
            descricao,
            complexidade,
            dataPrazo,
            criadoPor: { id: req.session.usuario.id }
        };

        if (req.session.usuario.nivel === 'COLABORADOR') {
            // Força auto-atribuição para colaborador
            payload.responsaveis = [{ id: req.session.usuario.id }];
            payload.time = null;
        } else {
            if (Array.isArray(responsavelId)) {
                payload.responsaveis = responsavelId.map(id => ({ id: parseInt(id) }));
            } else if (responsavelId) {
                payload.responsaveis = [{ id: parseInt(responsavelId) }];
            } else {
                payload.responsaveis = [];
            }
            payload.time = timeId ? { id: parseInt(timeId) } : null;
        }

        await apiService.criarTarefa(payload);
        req.session.success = 'Tarefa criada com sucesso!';
        res.redirect('/dashboard');
    } catch (error) {
        req.session.error = 'Erro ao criar tarefa. Tente novamente.';
        res.status(500).send('Erro ao criar tarefa');
    }
});

app.get('/tarefas/:id', authMiddleware, async (req, res) => {
    try {
        const tarefa = (await apiService.getTarefa(req.params.id)).data;
        
        // Verificação de autorização: Gestor, Responsável ou Membro do Time
        const isGestor = req.session.usuario.nivel === 'GESTOR';
        const isResponsavel = tarefa.responsaveis && tarefa.responsaveis.some(r => r.id === req.session.usuario.id);
        const isDoTime = tarefa.time && req.session.usuario.time && tarefa.time.id === req.session.usuario.time.id;

        if (!isGestor && !isResponsavel && !isDoTime) {
            return res.render('errors/403', { 
                message: 'Ops! Você não tem autorização para visualizar esta tarefa.',
                redirectUrl: '/dashboard'
            });
        }

        res.render('tarefas/detalhes', { tarefa, currentPage: 'tarefas' });
    } catch (error) {
        res.render('errors/403', { 
            message: 'Tarefa não encontrada ou acesso negado.',
            redirectUrl: '/dashboard'
        });
    }
});

app.post('/tarefas/:id/status', authMiddleware, async (req, res) => {
    try {
        const tarefa = (await apiService.getTarefa(req.params.id)).data;
        
        // Verificação de autorização: Gestor, Responsável ou Membro do Time
        const isGestor = req.session.usuario.nivel === 'GESTOR';
        const isResponsavel = tarefa.responsaveis && tarefa.responsaveis.some(r => r.id === req.session.usuario.id);
        const isDoTime = tarefa.time && req.session.usuario.time && tarefa.time.id === req.session.usuario.time.id;

        if (!isGestor && !isResponsavel && !isDoTime) {
            req.session.error = 'Acesso negado para alteração de status.';
            return res.redirect('/dashboard');
        }

        const { status, evidencia } = req.body;
        await apiService.atualizarStatus(req.params.id, status, evidencia);
        req.session.success = 'Status da tarefa atualizado com sucesso!';
        res.redirect('/dashboard');
    } catch (error) {
        req.session.error = 'Erro ao atualizar status.';
        res.redirect('/dashboard');
    }
});

// Usuários
app.get('/usuarios', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        const usuarios = (await apiService.getUsuarios()).data;
        const times = (await apiService.getTimes()).data;
        res.render('usuarios/listagem', { usuarios, times, currentPage: 'usuarios' });
    } catch (error) {
        res.status(500).send('Erro ao listar usuários');
    }
});

app.get('/usuarios/:id', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        const usuarioPerfil = (await apiService.getUsuario(req.params.id)).data;
        // Opcional: buscar tarefas deste usuário para mostrar no perfil
        const tarefas = (await apiService.getTarefas(req.params.id)).data;
        res.render('usuarios/detalhes', { usuarioPerfil, tarefas, currentPage: 'usuarios' });
    } catch (error) {
        res.status(500).send('Erro ao buscar detalhes do usuário');
    }
});

app.post('/usuarios', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        await apiService.salvarUsuario(req.body);
        req.session.success = 'Usuário salvo com sucesso!';
        res.redirect('/usuarios');
    } catch (error) {
        console.error('Erro ao salvar usuário:', error.response?.data || error.message);
        req.session.error = 'Erro ao salvar usuário. Verifique se o e-mail ou código já existem.';
        res.redirect('/usuarios');
    }
});

app.post('/usuarios/:id/toggle', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        await apiService.toggleUsuarioStatus(req.params.id);
        req.session.success = 'Status do usuário alterado!';
        res.redirect('/usuarios');
    } catch (error) {
        req.session.error = 'Erro ao alterar status do usuário.';
        res.status(500).send('Erro ao alterar status do usuário');
    }
});

app.listen(port, () => {
    console.log(`Frontend rodando em http://localhost:${port}`);
});
