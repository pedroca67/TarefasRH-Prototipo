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
    secret: process.env.SESSION_SECRET || 'strong_fallback_secret_778899_potiguar',
    resave: false,
    saveUninitialized: false,
    rolling: true,
    name: 'tarefasrh.sid',
    cookie: {
        secure: isProduction,
        httpOnly: true,
        maxAge: 1000 * 60 * 60 * 2, // 2 hours
        sameSite: isProduction ? 'strict' : 'lax'
    }
}));

// Middlewares de Autenticação
const authMiddleware = (req, res, next) => {
    if (!req.session.usuario) {
        return res.redirect('/login');
    }
    next();
};

const gestorMiddleware = (req, res, next) => {
    if (!req.session.usuario || req.session.usuario.nivel !== 'GESTOR') {
        return res.status(403).render('errors/403', { message: 'Acesso negado. Apenas gestores podem acessar este recurso.', redirectUrl: '/dashboard' });
    }
    next();
};

// Unified Error Handler for async routes
const asyncHandler = (fn) => (req, res, next) => {
    Promise.resolve(fn(req, res, next)).catch(next);
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

const apiService = require('./services/api');

// Endpoint Interno para Polling de Notificações
app.get('/api/internal/notificacoes/count', authMiddleware, asyncHandler(async (req, res) => {
    const response = await apiService.getUnreadNotificationsCount(req.session.usuario.id);
    res.json(response.data);
}));

app.get('/api/internal/notificacoes', authMiddleware, asyncHandler(async (req, res) => {
    const response = await apiService.getNotificacoes(req.session.usuario.id);
    res.json(response.data);
}));

app.get('/api/tarefas/calendario', authMiddleware, asyncHandler(async (req, res) => {
    const { start, end, responsavelId, timeId } = req.query;
    try {
        const response = await apiService.getCalendario(start, end, responsavelId, timeId);
        res.json(response.data);
    } catch (error) {
        console.error('Erro ao buscar dados do calendário:', error);
        res.status(500).json({ error: 'Erro ao buscar tarefas' });
    }
}));

// Rota Oculta de Administração
app.get('/admin/forcar-sync', authMiddleware, asyncHandler(async (req, res) => {
    try {
        await apiService.triggerSync();
        req.session.success = 'Sincronização com o Google Sheets iniciada em segundo plano com sucesso!';
    } catch (error) {
        req.session.error = 'Erro ao tentar forçar a sincronização.';
        console.error('Erro no forcar-sync:', error);
    }
    res.redirect('/dashboard');
}));

// Rotas
app.get('/', (req, res) => {
    if (req.session.usuario) {
        return res.redirect('/dashboard');
    }
    res.redirect('/login');
});

app.get('/login', (req, res) => {
    res.render('auth/login', { error: null, email: '' });
});

app.post('/login', asyncHandler(async (req, res) => {
    const { email, senha } = req.body;
    try {
        const response = await apiService.login(email, senha);
        req.session.usuario = response.data;
        res.redirect('/dashboard');
    } catch (error) {
        const status = error.response ? error.response.status : 500;
        if (status === 401 || status === 403) {
            const msg = error.response.data.error || error.response.data || 'Credenciais inválidas';
            return res.render('auth/login', { error: msg, email });
        }
        throw error; // Re-throw to global handler
    }
}));

app.get('/logout', (req, res) => {
    req.session.destroy((err) => {
        if (err) console.error('Erro ao destruir sessão:', err);
        res.clearCookie('tarefasrh.sid'); 
        res.redirect('/login');
    });
});

app.get('/perfil', authMiddleware, asyncHandler(async (req, res) => {
    const response = await apiService.getTarefas(req.session.usuario.id, null, null, null, 0, 1000);
    const tarefas = response.data.content;
    const stats = {
        total: tarefas.length,
        concluida: tarefas.filter(t => t.status === 'CONCLUIDA').length,
        atrasada: tarefas.filter(t => t.status === 'ATRASADA').length
    };
    res.render('usuarios/perfil', { stats, currentPage: 'perfil' });
}));

app.get('/dashboard', authMiddleware, asyncHandler(async (req, res) => {
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
        const start = new Date(hoje);
        start.setDate(hoje.getDate() - 7);
        startDate = start.toISOString().split('T')[0];
        endDate = hoje.toISOString().split('T')[0];
    } else if (periodo === 'mes') {
        startDate = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().split('T')[0];
        endDate = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0).toISOString().split('T')[0];
    } else if (periodo === 'ano') {
        startDate = new Date(hoje.getFullYear(), 0, 1).toISOString().split('T')[0];
        endDate = new Date(hoje.getFullYear(), 11, 31).toISOString().split('T')[0];
    } else {
        startDate = dataDe;
        endDate = dataAte;
    }

    const filtroFormatado = { periodo, dataDe, dataAte };

    if (req.session.usuario.nivel === 'GESTOR') {
        const stats = (await apiService.getStats(startDate, endDate, true)).data;
        const responseTarefas = await apiService.getTarefas(null, null, startDate, endDate, 0, 50);
        const times = (await apiService.getTimes()).data;
        res.render('dashboard/gestor', { stats, tarefas: responseTarefas.data.content, times, filtro: filtroFormatado, currentPage: 'dashboard' });
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

        res.render('dashboard/colaborador', { 
            tarefas: minhasTarefas,
            minhasTarefas, 
            tarefasTime, 
            impactoMensal,
            aderenciaPessoal,
            entregaCritica: proximas.length > 0 ? proximas[0] : null,
            filtro: filtroFormatado,
            currentPage: 'dashboard' 
        });
    }
}));

app.get('/tarefas', authMiddleware, asyncHandler(async (req, res) => {
    let { periodo, dataDe, dataAte, page, size, search, status, complexidade, categoria, timeId } = req.query;
    page = parseInt(page) || 0;
    size = parseInt(size) || 10;

    let startDate, endDate;
    const hoje = new Date();
    if (periodo === 'semana') {
        const start = new Date(hoje);
        start.setDate(hoje.getDate() - 7);
        startDate = start.toISOString().split('T')[0];
        endDate = hoje.toISOString().split('T')[0];
    } else if (periodo === 'mes') {
        startDate = new Date(hoje.getFullYear(), hoje.getMonth(), 1).toISOString().split('T')[0];
        endDate = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0).toISOString().split('T')[0];
    } else if (periodo === 'ano') {
        startDate = new Date(hoje.getFullYear(), 0, 1).toISOString().split('T')[0];
        endDate = new Date(hoje.getFullYear(), 11, 31).toISOString().split('T')[0];
    } else {
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
}));

app.get('/tarefas/calendario', authMiddleware, asyncHandler(async (req, res) => {
    res.render('tarefas/calendario', { currentPage: 'calendario' });
}));

app.get('/api/tarefas/calendario', authMiddleware, asyncHandler(async (req, res) => {
    const { start, end, responsavelId, timeId } = req.query;
    const response = await apiService.getCalendario(responsavelId, timeId, start, end);
    res.json(response.data);
}));

app.get('/tarefas/nova', authMiddleware, asyncHandler(async (req, res) => {
    if (req.session.usuario.nivel === 'GESTOR') {
        const usuarios = (await apiService.getUsuarios()).data;
        const times = (await apiService.getTimes()).data;
        res.render('tarefas/form', { usuarios, times, currentPage: 'tarefas' });
    } else {
        res.render('tarefas/form_colaborador', { currentPage: 'dashboard' });
    }
}));

app.get('/tarefas/:id/editar', authMiddleware, asyncHandler(async (req, res) => {
    const response = await apiService.getTarefa(req.params.id, req.session.usuario.id);
    const tarefa = response.data;
    
    const isGestor = req.session.usuario.nivel === 'GESTOR';
    const isCriador = tarefa.criadoPor && tarefa.criadoPor.id === req.session.usuario.id;

    if (!isGestor && !isCriador) {
        return res.status(403).render('errors/403', { message: 'Você não tem permissão para editar esta tarefa.', redirectUrl: `/tarefas/${req.params.id}` });
    }

    if (isGestor) {
        const usuarios = (await apiService.getUsuarios()).data;
        const times = (await apiService.getTimes()).data;
        res.render('tarefas/form', { tarefa, usuarios, times, currentPage: 'tarefas' });
    } else {
        res.render('tarefas/form_colaborador', { tarefa, currentPage: 'dashboard' });
    }
}));

app.post('/tarefas', authMiddleware, asyncHandler(async (req, res) => {
    const { titulo, descricao, complexidade, dataPrazo, responsavelId, timeId, previstoNoCargoGestor, categoria } = req.body;
    const payload = {
        titulo, descricao, complexidade, dataPrazo,
        previstoNoCargoGestor: previstoNoCargoGestor === 'on' || previstoNoCargoGestor === true || previstoNoCargoGestor === 'true',
        categoria: categoria || 'OUTROS',
        criadoPor: { id: req.session.usuario.id }
    };

    if (req.session.usuario.nivel === 'COLABORADOR') {
        payload.responsaveis = [{ id: req.session.usuario.id }];
        payload.time = null;
    } else {
        if (Array.isArray(responsavelId)) {
            payload.responsaveis = responsavelId.map(id => ({ id: parseInt(id) }));
        } else if (responsavelId) {
            payload.responsaveis = [{ id: parseInt(responsavelId) }];
        }
        payload.time = timeId ? { id: parseInt(timeId) } : null;
    }

    await apiService.criarTarefa(payload);
    req.session.success = 'Tarefa criada com sucesso!';
    res.redirect('/dashboard');
}));

app.post('/tarefas/:id', authMiddleware, asyncHandler(async (req, res) => {
    const { titulo, descricao, complexidade, dataPrazo, responsavelId, timeId, previstoNoCargoGestor, categoria } = req.body;
    const payload = {
        titulo, descricao, complexidade, dataPrazo,
        previstoNoCargoGestor: previstoNoCargoGestor === 'on' || previstoNoCargoGestor === true || previstoNoCargoGestor === 'true',
        categoria: categoria || 'OUTROS'
    };

    if (req.session.usuario.nivel === 'COLABORADOR') {
        payload.responsaveis = [{ id: req.session.usuario.id }];
        payload.time = null;
    } else {
        if (Array.isArray(responsavelId)) {
            payload.responsaveis = responsavelId.map(id => ({ id: parseInt(id) }));
        } else if (responsavelId) {
            payload.responsaveis = [{ id: parseInt(responsavelId) }];
        }
        payload.time = timeId ? { id: parseInt(timeId) } : null;
    }

    await apiService.atualizarTarefa(req.params.id, payload, req.session.usuario.id);
    req.session.success = 'Tarefa atualizada com sucesso!';
    res.redirect(`/tarefas/${req.params.id}`);
}));

app.get('/tarefas/:id', authMiddleware, asyncHandler(async (req, res) => {
    const response = await apiService.getTarefa(req.params.id, req.session.usuario.id);
    const tarefa = response.data;
    
    try { await apiService.marcarNotificacoesComoLidas(req.params.id, req.session.usuario.id); } catch (nErr) {}

    const feedbacks = (await apiService.getFeedbacks(req.params.id)).data;
    res.render('tarefas/detalhes', { tarefa, feedbacks, currentPage: 'tarefas' });
}));

app.post('/tarefas/:id/status', authMiddleware, asyncHandler(async (req, res) => {
    const { status, evidencia, previstoNoCargoColaborador } = req.body;
    await apiService.atualizarStatus(req.params.id, status, evidencia, previstoNoCargoColaborador, req.session.usuario.id);
    req.session.success = 'Status atualizado!';
    res.redirect('/dashboard');
}));

app.post('/tarefas/:id/feedback', authMiddleware, gestorMiddleware, asyncHandler(async (req, res) => {
    await apiService.enviarFeedback(req.params.id, req.body.feedback, req.session.usuario.id);
    req.session.success = 'Feedback enviado!';
    res.redirect(`/tarefas/${req.params.id}`);
}));

// Usuários
app.get('/usuarios', authMiddleware, gestorMiddleware, asyncHandler(async (req, res) => {
    const usuarios = (await apiService.getUsuarios()).data;
    const times = (await apiService.getTimes()).data;
    res.render('usuarios/listagem', { usuarios, times, currentPage: 'usuarios' });
}));

app.get('/usuarios/:id', authMiddleware, gestorMiddleware, asyncHandler(async (req, res) => {
    const usuarioPerfil = (await apiService.getUsuario(req.params.id)).data;
    const responseTarefas = await apiService.getTarefas(req.params.id, null, null, null, 0, 1000);
    res.render('usuarios/detalhes', { usuarioPerfil, tarefas: responseTarefas.data.content, currentPage: 'usuarios' });
}));

app.post('/usuarios', authMiddleware, gestorMiddleware, asyncHandler(async (req, res) => {
    await apiService.salvarUsuario(req.body);
    req.session.success = 'Usuário salvo!';
    res.redirect('/usuarios');
}));

app.post('/usuarios/:id/toggle', authMiddleware, gestorMiddleware, asyncHandler(async (req, res) => {
    await apiService.toggleUsuarioStatus(req.params.id);
    req.session.success = 'Status alterado!';
    res.redirect('/usuarios');
}));

app.get('/usuarios/:id/relatorio', authMiddleware, gestorMiddleware, asyncHandler(async (req, res) => {
    const usuarioPerfil = (await apiService.getUsuario(req.params.id)).data;
    const responseTarefas = await apiService.getTarefas(req.params.id, null, null, null, 0, 1000);
    const tarefas = responseTarefas.data.content;
    const pesoEsforco = { 'BAIXA': 1, 'MEDIA': 3, 'ALTA': 5 };
    const concluidas = tarefas.filter(t => t.status === 'CONCLUIDA');
    const impactoTotal = concluidas.reduce((acc, t) => acc + (pesoEsforco[t.complexidade] || 0), 0);
    const totalAderenciaResp = concluidas.filter(t => t.previstoNoCargoColaborador !== null).length;
    const aderenciaPercent = totalAderenciaResp > 0 ? Math.round((concluidas.filter(t => t.previstoNoCargoColaborador === true).length / totalAderenciaResp) * 100) : 0;
    res.render('usuarios/relatorio', { usuarioPerfil, tarefas, impactoTotal, aderenciaPercent, currentPage: 'usuarios' });
}));

app.use((req, res) => res.status(404).render('errors/404'));

// Global Error Handler
app.use((err, req, res, next) => {
    console.error('SERVER_ERROR:', {
        message: err.message,
        stack: isProduction ? 'HIDDEN' : err.stack,
        url: req.originalUrl,
        user: req.session.usuario ? req.session.usuario.id : 'anonymous'
    });
    
    if (res.headersSent) return next(err);

    const status = err.response ? err.response.status : 500;
    const errorMessage = err.response && err.response.data && (err.response.data.error || err.response.data)
        ? (err.response.data.error || err.response.data)
        : 'Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.';

    res.status(status).render('errors/error_general', { 
        status, 
        message: typeof errorMessage === 'string' ? errorMessage : JSON.stringify(errorMessage),
        error: isProduction ? {} : err 
    });
});

app.listen(port, () => console.log(`Frontend rodando em http://localhost:${port}`));
