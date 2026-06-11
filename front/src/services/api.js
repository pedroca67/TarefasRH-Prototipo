const axios = require('axios');
require('dotenv').config();

const api = axios.create({
    baseURL: process.env.API_URL,
    headers: {
        'X-API-KEY': 'potiguar_secret_token_2026'
    }
});

module.exports = {
    login: (email, senha) => api.post('/auth/login', { email, senha }),
    
    // Usuários
    getUsuarios: () => api.get('/usuarios'),
    getUsuario: (id) => api.get(`/usuarios/${id}`),
    salvarUsuario: (dados) => api.post('/usuarios', dados),
    toggleUsuarioStatus: (id) => api.patch(`/usuarios/${id}/status`),

    // Times
    getTimes: () => api.get('/times'),

    // Tarefas
    getTarefas: (responsavelId, timeId, startDate, endDate, page, size, search, status, complexidade, categoria) => 
        api.get('/tarefas', { params: { responsavelId, timeId, startDate, endDate, page, size, search, status, complexidade, categoria } }),
    getTarefa: (id, usuarioId) => api.get(`/tarefas/${id}`, { params: { usuarioId } }),
    getCalendario: (responsavelId, timeId, start, end) => 
        api.get('/tarefas/calendario', { params: { responsavelId, timeId, start, end } }),
    criarTarefa: (dados) => api.post('/tarefas', dados),
    atualizarTarefa: (id, dados, usuarioId) => api.put(`/tarefas/${id}`, dados, { params: { usuarioId } }),
    atualizarStatus: (id, status, evidencia, previstoNoCargoColaborador, concluidoPorId) => api.put(`/tarefas/${id}/status`, { status, evidencia, previstoNoCargoColaborador, concluidoPorId }),
    enviarFeedback: (id, feedback, gestorId) => api.post(`/tarefas/${id}/feedback`, { feedback, gestorId }),
    getFeedbacks: (id) => api.get(`/tarefas/${id}/feedbacks`),
    
    // Notificações
    getUnreadNotificationsCount: (usuarioId) => api.get('/notificacoes/unread-count', { params: { usuarioId } }),
    getNotificacoes: (usuarioId) => api.get('/notificacoes', { params: { usuarioId } }),
    marcarNotificacoesComoLidas: (taskId, usuarioId) => api.patch(`/notificacoes/read-by-task/${taskId}`, null, { params: { usuarioId } }),
    
    getStats: (startDate, endDate, analyticalMode = false) => 
        api.get('/tarefas/stats', { params: { startDate, endDate, analyticalMode } }),

    // Admin Tools
    post: (url, data) => api.post(url, data),
    triggerSync: () => api.post('/tarefas/admin/sync')
};
