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
    salvarUsuario: (usuario) => api.post('/usuarios', usuario),
    toggleUsuarioStatus: (id) => api.patch(`/usuarios/${id}/status`),

    // Times
    getTimes: () => api.get('/times'),

    // Tarefas
    getTarefas: (responsavelId, timeId) => api.get('/tarefas', { params: { responsavelId, timeId } }),
    getTarefa: (id) => api.get(`/tarefas/${id}`),
    criarTarefa: (tarefa) => api.post('/tarefas', tarefa),
    atualizarStatus: (id, status, evidencia, previstoNoCargoColaborador, concluidoPorId) => api.put(`/tarefas/${id}/status`, { status, evidencia, previstoNoCargoColaborador, concluidoPorId }),
    enviarFeedback: (id, feedback) => api.put(`/tarefas/${id}/feedback`, { feedback }),
    
    // Notificações
    getUnreadNotificationsCount: (usuarioId) => api.get('/notificacoes/unread-count', { params: { usuarioId } }),
    marcarNotificacoesComoLidas: (taskId, usuarioId) => api.patch(`/notificacoes/read-by-task/${taskId}`, null, { params: { usuarioId } }),
    
    getStats: () => api.get('/tarefas/stats')
};
