import { createRouter, createWebHistory } from 'vue-router';
import App from '../App.vue';
import PublicSharePage from '../components/PublicSharePage.vue';
import PublicBatchSharePage from '../components/PublicBatchSharePage.vue';

const routes = [
    {
        path: '/',
        name: 'Home',
        component: App
    },
    {
        path: '/public/share/batch/:code',
        name: 'PublicBatchShare',
        component: PublicBatchSharePage,
        meta: { isPublic: true }
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
