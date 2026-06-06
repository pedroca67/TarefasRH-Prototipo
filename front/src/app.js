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
    rolling: true, // Renova o tempo de vida do cookie a cada interação
    name: 'tarefasrh.sid',
    cookie: {
        secure: isProduction,
        httpOnly: true,
        maxAge: 1000 * 60 * 90, // 90 minutos (1h 30m)
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
        res.clearCookie('tarefasrh.sid'); 
        res.redirect('/login');
    });
});

app.get('/perfil', authMiddleware, async (req, res) => {
    try {
        const response = await apiService.getTarefas(req.session.usuario.id, null, null, null, 0, 1000);
        const tarefas = response.data.content;
        const stats = {
            total: tarefas.length,
            concluida: tarefas.filter(t => t.status === 'CONCLUIDA').length,
            atrasada: tarefas.filter(t => t.status === 'ATRASADA').length
        };
        res.render('usuarios/perfil', { stats, currentPage: 'perfil' });
    } catch (error) {
        console.error('Erro ao carregar perfil:', error);
        res.status(500).send('Erro ao carregar perfil');
    }
});

app.get('/dashboard', authMiddleware, async (req, res) => {
    try {
        let { periodo, dataDe, dataAte } = req.query;
        if (!periodo) {
            periodo = req.session.filtroPeriodo || 'mes';
            dataDe = req.session.filtroDataDe;
            dataAte = req.session.filtroDataAte;
        } else {
            req.session.filtroPeriodo = periodo;
            req.session.filtroDataDe = dataDe;
            req.session.filtroDataAte = dataAte;
        }

        let startDate, endDate;
        const hoje = new Date();
        if (periodo === 'semana') {
            const primeira = new Date(hoje.setDate(hoje.getDate() - hoje.getDay()));
            const ultima = new Date(hoje.setDate(hoje.getDate() - hoje.getDay() + 6));
            startDate = primeira.toISOString().split('T')[0];
            endDate = ultima.toISOString().split('T')[0];
        } else if (periodo === 'mes') {
            startDate = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().split('T')[0];
            endDate = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0).toISOString().split('T')[0];
        } else if (periodo === 'ano') {
            startDate = new Date(hoje.getFullYear(), 0, 1).toISOString().split('T')[0];
            endDate = new Date(hoje.getFullYear(), 11, 31).toISOString().split('T')[0];
        } else if (periodo === 'personalizado') {
            startDate = dataDe;
            endDate = dataAte;
        }

        const filtroFormatado = { periodo, dataDe, dataAte };

        if (req.session.usuario.nivel === 'GESTOR') {
            const stats = (await apiService.getStats(startDate, endDate, true)).data;
            const responseTarefas = await apiService.getTarefas(null, null, startDate, endDate, 0, 50);
            const tarefas = responseTarefas.data.content;
            const times = (await apiService.getTimes()).data;
            res.render('dashboard/gestor', { stats, tarefas, times, filtro: filtroFormatado, currentPage: 'dashboard' });
        } else {
            const responseMinhas = await apiService.getTarefas(req.session.usuario.id, null, startDate, endDate, 0, 100);
            const minhasTarefas = responseMinhas.data.content;
            let tarefasTime = [];
            if (req.session.usuario.time) {
                const responseTime = await apiService.getTarefas(null, req.session.usuario.time.id, startDate, endDate, 0, 100);
                tarefasTime = responseTime.data.content;
            }

            const pesoEsforco = { 'BAIXA': 1, 'MEDIA': 3, 'ALTA': 5 };
            const minhasConcluidas = minhasTarefas.filter(t => t.status === 'CONCLUIDA');
            const impactoMensal = minhasConcluidas.reduce((acc, t) => acc + (pesoEsforco[t.complexidade] || 0), 0);
            const aderenciaSim = minhasConcluidas.filter(t => t.previstoNoCargoColaborador === true).length;
            const totalAderenciaResp = minhasConcluidas.filter(t => t.previstoNoCargoColaborador !== null).length;
            const aderenciaPessoal = totalAderenciaResp > 0 ? Math.round((aderenciaSim / totalAderenciaResp) * 100) : 0;

            const proximas = minhasTarefas.filter(t => t.status !== 'CONCLUIDA').sort((a, b) => new Date(a.dataPrazo) - new Date(b.dataPrazo));
            const entregaCritica = proximas.length > 0 ? proximas[0] : null;

            res.render('dashboard/colaborador', { 
                tarefas: minhasTarefas,
                minhasTarefas, 
                tarefasTime, 
                impactoMensal,
                aderenciaPessoal,
                entregaCritica,
                filtro: filtroFormatado,
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
        let { periodo, dataDe, dataAte, page, size, search, status, complexidade, categoria, timeId } = req.query;
        page = parseInt(page) || 0;
        size = parseInt(size) || 10;

        if (!periodo) {
            periodo = req.session.filtroPeriodo || 'mes';
            dataDe = req.session.filtroDataDe;
            dataAte = req.session.filtroDataAte;
        } else {
            req.session.filtroPeriodo = periodo;
            req.session.filtroDataDe = dataDe;
            req.session.filtroDataAte = dataAte;
        }

        let startDate, endDate;
        const hoje = new Date();
        if (periodo === 'semana') {
            const primeira = new Date(hoje.setDate(hoje.getDate() - hoje.getDay()));
            const ultima = new Date(hoje.setDate(hoje.getDate() - hoje.getDay() + 6));
            startDate = primeira.toISOString().split('T')[0];
            endDate = ultima.toISOString().split('T')[0];
        } else if (periodo === 'mes') {
            startDate = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().split('T')[0];
            endDate = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0).toISOString().split('T')[0];
        } else if (periodo === 'ano') {
            startDate = new Date(hoje.getFullYear(), 0, 1).toISOString().split('T')[0];
            endDate = new Date(hoje.getFullYear(), 11, 31).toISOString().split('T')[0];
        } else if (periodo === 'personalizado') {
            startDate = dataDe;
            endDate = dataAte;
        }

        const filtroFormatado = { periodo, dataDe, dataAte, search, status, complexidade, categoria, timeId };

        let response;
        if (req.session.usuario.nivel === 'GESTOR') {
            response = await apiService.getTarefas(null, timeId, startDate, endDate, page, size, search, status, complexidade, categoria);
        } else {
            response = await apiService.getTarefas(req.session.usuario.id, null, startDate, endDate, page, size, search, status, complexidade, categoria);
        }

        const { content, totalPages, totalItems } = response.data;
        const times = (await apiService.getTimes()).data;
        
        res.render('tarefas/listagem', { 
            tarefas: content, 
            times, 
            filtro: filtroFormatado, 
            pagination: { page, totalPages, totalItems, size },
            currentPage: 'tarefas' 
        });
    } catch (error) {
        console.error('Erro ao carregar lista de tarefas:', error);
        res.status(500).send('Erro ao carregar lista de tarefas');
    }
});

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
        console.error('Erro ao carregar form nova tarefa:', error);
        res.status(500).send('Erro ao carregar formulário');
    }
});

app.post('/tarefas', authMiddleware, async (req, res) => {
    try {
        const { titulo, descricao, complexidade, dataPrazo, responsavelId, timeId, previstoNoCargoGestor, categoria } = req.body;
        const payload = {
            titulo, descricao, complexidade, dataPrazo,
            previstoNoCargoGestor: previstoNoCargoGestor === 'on' || previstoNoCargoGestor === true || previstoNoCargoGestor === 'true',
            categoria: categoria || 'OUTROS',
            criadoPor: { id: req.session.usuario.id }
        };

        if (req.session.usuario.nivel === 'COLABORADOR') {
            payload.responsaveis = [{ id: req.session.usuario.id }];
            payload.time = req.session.usuario.time ? { id: req.session.usuario.time.id } : null;
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
        console.error('Erro ao criar tarefa:', error);
        req.session.error = 'Erro ao criar tarefa. Tente novamente.';
        res.redirect('/dashboard');
    }
});

app.get('/tarefas/:id', authMiddleware, async (req, res) => {
    try {
        const tarefa = (await apiService.getTarefa(req.params.id)).data;
        const isGestor = req.session.usuario.nivel === 'GESTOR';
        const isResponsavel = tarefa.responsaveis && tarefa.responsaveis.some(r => r.id === req.session.usuario.id);
        const isDoTime = tarefa.time && req.session.usuario.time && tarefa.time.id === req.session.usuario.time.id;

        if (!isGestor && !isResponsavel && !isDoTime) {
            return res.render('errors/403', { message: 'Acesso negado.', redirectUrl: '/dashboard' });
        }

        try { await apiService.marcarNotificacoesComoLidas(req.params.id, req.session.usuario.id); } catch (nErr) {}

        const feedbacks = (await apiService.getFeedbacks(req.params.id)).data;
        res.render('tarefas/detalhes', { tarefa, feedbacks, currentPage: 'tarefas' });
    } catch (error) {
        console.error('Erro ao buscar tarefa:', error);
        res.render('errors/403', { message: 'Erro ao carregar detalhes.', redirectUrl: '/dashboard' });
    }
});

app.post('/tarefas/:id/status', authMiddleware, async (req, res) => {
    try {
        const { status, evidencia, previstoNoCargoColaborador } = req.body;
        await apiService.atualizarStatus(req.params.id, status, evidencia, previstoNoCargoColaborador, req.session.usuario.id);
        req.session.success = 'Status atualizado!';
        res.redirect('/dashboard');
    } catch (error) {
        console.error('Erro ao atualizar status:', error);
        req.session.error = 'Erro ao atualizar status.';
        res.redirect('/dashboard');
    }
});

app.post('/tarefas/:id/feedback', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        await apiService.enviarFeedback(req.params.id, req.body.feedback, req.session.usuario.id);
        req.session.success = 'Feedback enviado!';
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
        const responseTarefas = await apiService.getTarefas(req.params.id, null, null, null, 0, 1000);
        const tarefas = responseTarefas.data.content;
        res.render('usuarios/detalhes', { usuarioPerfil, tarefas, currentPage: 'usuarios' });
    } catch (error) {
        console.error('Erro ao buscar perfil de usuário:', error);
        res.status(500).send('Erro ao buscar detalhes');
    }
});

app.post('/usuarios', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        await apiService.salvarUsuario(req.body);
        req.session.success = 'Usuário salvo!';
        res.redirect('/usuarios');
    } catch (error) {
        req.session.error = 'Erro ao salvar usuário.';
        res.redirect('/usuarios');
    }
});

app.post('/usuarios/:id/toggle', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        await apiService.toggleUsuarioStatus(req.params.id);
        req.session.success = 'Status alterado!';
        res.redirect('/usuarios');
    } catch (error) {
        res.status(500).send('Erro ao alterar status');
    }
});

app.get('/usuarios/:id/relatorio', authMiddleware, gestorMiddleware, async (req, res) => {
    try {
        const usuarioPerfil = (await apiService.getUsuario(req.params.id)).data;
        const responseTarefas = await apiService.getTarefas(req.params.id, null, null, null, 0, 1000);
        const tarefas = responseTarefas.data.content;
        const pesoEsforco = { 'BAIXA': 1, 'MEDIA': 3, 'ALTA': 5 };
        const concluidas = tarefas.filter(t => t.status === 'CONCLUIDA');
        const impactoTotal = concluidas.reduce((acc, t) => acc + (pesoEsforco[t.complexidade] || 0), 0);
        const totalAderenciaResp = concluidas.filter(t => t.previstoNoCargoColaborador !== null).length;
        const aderenciaPercent = totalAderenciaResp > 0 ? Math.round((concluidas.filter(t => t.previstoNoCargoColaborador === true).length / totalAderenciaResp) * 100) : 0;
        res.render('usuarios/relatorio', { usuarioPerfil, tarefas, impactoTotal, aderenciaPercent, currentPage: 'usuarios' });
    } catch (error) {
        console.error('Erro ao gerar relatório:', error);
        res.status(500).send('Erro ao gerar relatório');
    }
});

app.use((req, res) => res.status(404).render('errors/404'));
app.use((err, req, res, next) => {
    console.error('Erro Global:', err);
    res.status(500).send('Algo deu errado! Tente novamente mais tarde.');
});

app.listen(port, () => console.log(`Frontend rodando em http://localhost:${port}`));
