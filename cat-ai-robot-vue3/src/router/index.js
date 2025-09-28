import {createRouter, createWebHashHistory} from 'vue-router'

// 统一在这里声明所有路由
const routes = [
    {
        path: '/',
        component: () => import('@/layouts/Layout.vue'),
        meta: {
            title: 'Cat AI 机器人'
        }
    },
    {
        path: '/role-management',
        component: () => import('@/views/RoleManagement.vue'),
        meta: {
            title: '角色管理 - Cat AI 机器人'
        }
    }
]

// 创建路由
const router = createRouter({
    history: createWebHashHistory(),
    routes,
})

export default router
