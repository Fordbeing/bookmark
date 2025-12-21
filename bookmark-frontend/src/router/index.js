import { createRouter, createWebHistory } from 'vue-router';
import App from '../App.vue';
import PublicSharePage from '../components/PublicSharePage.vue';

const routes = [
    {
        path: '/',
        name: 'Home',
        component: App
    },
    {
        path: '/public/share/:code',
        name: 'PublicShare',
        component: PublicSharePage,
        meta: { isPublic: true }
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;
