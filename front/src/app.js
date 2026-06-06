const express = require('express');
const session = require('express-session');
const path = require('path');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;

// Identificar ambiente
const isProduction = process.env.NODE_ENV === 'production';

// Configurações
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Confiar no proxy (Vercel/Railway/HTTPS)
if (isProduction) {
    app.set('trust proxy', 1);
}

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
    resave: true,
    saveUninitialized: true,
    name: 'tarefasrh.sid',
    cookie: {
        secure: isProduction,
        httpOnly: true,
        maxAge: 1000 * 60 * 60 * 24,
        sameSite: isProduction ? 'none' : 'lax'
    }
}));

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

// Endpoint Interno para Polling de Notificações
app.get('/api/internal/notificacoes/count', authMiddleware, async (req, res) => {
    try {
        const response = await apiService.getUnreadNotificationsCount(req.session.usuario.id);
        res.json(response.data);
    } catch (error) {
        res.status(500).json({ count: 0 });
    }
});

app.get('/api/internal/notificacoes', authMiddleware, async (req, res) => {
    try {
        const response = await apiService.getNotificacoes(req.session.usuario.id);
        res.json(response.data);
    } catch (error) {
        res.status(500).json([]);
    }
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
        res.clearCookie('tarefasrh.sid'); // Limpa o cookie personalizado da sessão
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
            let tarefasTime = [];
            if (req.session.usuario.time) {
                tarefasTime = (await apiService.getTarefas(null, req.session.usuario.time.id)).data;
            }

            // Cálculo de métricas pessoais
            const pesoEsforco = { 'BAIXA': 1, 'MEDIA': 3, 'ALTA': 5 };
            const minhasConcluidas = minhasTarefas.filter(t => t.status === 'CONCLUIDA');
            
            const impactoMensal = minhasConcluidas.reduce((acc, t) => {
                const dataConc = new Date(t.dataConclusao);
                const hoje = new Date();
                if (dataConc.getMonth() === hoje.getMonth() && dataConc.getFullYear() === hoje.getFullYear()) {
                    return acc + (pesoEsforco[t.complexidade] || 0);
                }
                return acc;
            }, 0);

            const aderenciaSim = minhasConcluidas.filter(t => t.previstoNoCargoColaborador === true).length;
            const totalAderenciaResp = minhasConcluidas.filter(t => t.previstoNoCargoColaborador !== null).length;
            const aderenciaPessoal = totalAderenciaResp > 0 ? Math.round((aderenciaSim / totalAderenciaResp) * 100) : 0;

            // Identificar Entrega Crítica (mais próxima do prazo)
            const proximas = minhasTarefas
                .filter(t => t.status !== 'CONCLUIDA')
                .sort((a, b) => new Date(a.dataPrazo) - new Date(b.dataPrazo));
            const entregaCritica = proximas.length > 0 ? proximas[0] : null;

            res.render('dashboard/colaborador', { 
                tarefas: minhasTarefas, // Para os cards de estatísticas
                minhasTarefas, 
                tarefasTime, 
                impactoMensal,
                aderenciaPessoal,
                entregaCritica,
                currentPage: 'dashboard' 
            });
        }
    } catch (error) {
        console.error('Erro ao carregar dashboard:', error);
        res.status(500).send('Erro ao carregar dashboard');
    }
});

app.get('/tarefas', authMiddleware, async (req, res) => {
    try {
        const tarefas = (await apiService.getTarefas()).data;
        const times = (await apiService.getTimes()).data;
        res.render('tarefas/listagem', { tarefas, times, currentPage: 'tarefas' });
    } catch (error) {
        res.status(500).send('Erro ao carregar lista de tarefas');
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
        const { titulo, descricao, complexidade, dataPrazo, responsavelId, timeId, previstoNoCargoGestor, categoria } = req.body;
        const payload = {
            titulo,
            descricao,
            complexidade,
            dataPrazo,
            previstoNoCargoGestor: previstoNoCargoGestor === 'on' || previstoNoCargoGestor === true || previstoNoCargoGestor === 'true',
            categoria: categoria || 'OUTROS',
            criadoPor: { id: req.session.usuario.id }
        };

        if (req.session.usuario.nivel === 'COLABORADOR') {
            // Força auto-atribuição para colaborador
            payload.responsaveis = [{ id: req.session.usuario.id }];
            payload.time = req.session.usuario.time ? { id: req.session.usuario.time.id } : null;
            payload.categoria = req.body.categoria || 'OUTROS';
        } else {
            if (Array.isArray(responsavelId)) {
                payload.responsaveis = responsavelId.map(id => ({ id: parseInt(id) }));
            } else if (responsavelId) {
                payload.responsaveis = [{ id: parseInt(responsavelId) }];
            } else {
                payload.responsaveis = [];
            }
            payload.time = timeId ? { id: parseInt(timeId) } : null;
            payload.categoria = req.body.categoria || 'OUTROS';
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

        // Marcar notificações como lidas ao visualizar
        try {
            await apiService.marcarNotificacoesComoLidas(req.params.id, req.session.usuario.id);
        } catch (nErr) {
            console.error('Erro ao limpar notificações:', nErr.message);
        }

        const feedbacks = (await apiService.getFeedbacks(req.params.id)).data;
        res.render('tarefas/detalhes', { tarefa, feedbacks, currentPage: 'tarefas' });
    } catch (error) {
        console.error('Erro ao carregar detalhes da tarefa:', error);
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

        const { status, evidencia, previstoNoCargoColaborador } = req.body;
        const concluidoPorId = req.session.usuario.id;
        await apiService.atualizarStatus(req.params.id, status, evidencia, previstoNoCargoColaborador, concluidoPorId);
        req.session.success = 'Status da tarefa atualizado com sucesso!';
        res.redirect('/dashboard');
    } catch (error) {
        console.error('Erro ao atualizar status:', error);
        req.session.error = 'Erro ao atualizar status.';
        res.redirect('/dashboard');
    }
});

app.post('/tarefas/:id/feedback', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        const { feedback } = req.body;
        await apiService.enviarFeedback(req.params.id, feedback, req.session.usuario.id);
        req.session.success = 'Feedback enviado com sucesso!';
        res.redirect(`/tarefas/${req.params.id}`);
    } catch (error) {
        console.error('Erro ao enviar feedback:', error);
        req.session.error = 'Erro ao enviar feedback.';
        res.redirect(`/tarefas/${req.params.id}`);
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

// Tratamento de 404 (Página não encontrada) - Deve ser a última rota
app.use((req, res) => {
    res.status(404).render('errors/404');
});

// Tratamento de 500 (Erro interno)
app.use((err, req, res, next) => {
    console.error('Erro Global:', err);
    res.status(500).send('Algo deu errado! Tente novamente mais tarde.');
});

app.listen(port, () => {
    console.log(`Frontend rodando em http://localhost:${port}`);
});
