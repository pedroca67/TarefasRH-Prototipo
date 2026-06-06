const axios = require('axios');
require('dotenv').config();

const api = axios.create({
    baseURL: process.env.API_URL
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
    getTarefas: (responsavelId, timeId, startDate, endDate) => api.get('/tarefas', { params: { responsavelId, timeId, startDate, endDate } }),
    getTarefa: (id) => api.get(`/tarefas/${id}`),
    criarTarefa: (dados) => api.post('/tarefas', dados),
    atualizarStatus: (id, status, evidencia, previstoNoCargoColaborador, concluidoPorId) => api.put(`/tarefas/${id}/status`, { status, evidencia, previstoNoCargoColaborador, concluidoPorId }),
    enviarFeedback: (id, feedback, gestorId) => api.post(`/tarefas/${id}/feedback`, { feedback, gestorId }),
    getFeedbacks: (id) => api.get(`/tarefas/${id}/feedbacks`),
    
    // Notificações
    getUnreadNotificationsCount: (usuarioId) => api.get('/notificacoes/unread-count', { params: { usuarioId } }),
    getNotificacoes: (usuarioId) => api.get('/notificacoes', { params: { usuarioId } }),
    marcarNotificacoesComoLidas: (taskId, usuarioId) => api.patch(`/notificacoes/read-by-task/${taskId}`, null, { params: { usuarioId } }),
    
    getStats: (startDate, endDate) => api.get('/tarefas/stats', { params: { startDate, endDate } })
};
