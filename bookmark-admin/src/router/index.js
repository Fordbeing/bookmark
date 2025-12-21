import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/AdminLogin.vue'),
        meta: { guest: true }
    },
    {
        path: '/',
        component: () => import('@/components/layout/AdminLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                name: 'Dashboard',
                component: () => import('@/views/dashboard/Dashboard.vue'),
                meta: { title: '仪表盘' }
            },
            {
                path: 'users',
                name: 'UserList',
                component: () => import('@/views/user/UserList.vue'),
                meta: { title: '用户管理' }
            },
            {
                path: 'users/:id',
                name: 'UserDetail',
                component: () => import('@/views/user/UserDetail.vue'),
                meta: { title: '用户详情' }
            },
            {
                path: 'activation-codes',
                name: 'ActivationCodes',
                component: () => import('@/views/activation/CodeList.vue'),
                meta: { title: '激活码管理' }
            },
            {
                path: 'bookmarks',
                name: 'BookmarkList',
                component: () => import('@/views/content/BookmarkList.vue'),
                meta: { title: '内容审核' }
            },
            {
                path: 'settings',
                name: 'Settings',
                component: () => import('@/views/settings/SystemSettings.vue'),
                meta: { title: '系统设置' }
            },
            {
                path: 'announcements',
                name: 'Announcements',
                component: () => import('@/views/settings/AnnouncementManager.vue'),
                meta: { title: '公告管理' }
            },
            {
                path: 'logs',
                name: 'AuditLog',
                component: () => import('@/views/log/AuditLog.vue'),
                meta: { title: '操作日志' }
            }
        ]
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/'
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        next('/login')
    } else if (to.meta.guest && authStore.isLoggedIn) {
        next('/')
    } else {
        next()
    }
})

export default router
