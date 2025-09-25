import {createRouter, createWebHashHistory} from 'vue-router'

// 统一在这里声明所有路由
const routes = [
    {
        path: '/', // 路由地址
        component: () => import('@/layouts/Layout.vue'), // 对应组件
        meta: { // meta 信息
            title: 'Cat AI 机器人' // 页面标题
        }
    },
    {
        path: '/role-management', // 角色管理页面
        component: () => import('@/views/RoleManagement.vue'),
        meta: {
            title: '角色管理 - Cat AI 机器人'
        }
    }
]

// 创建路由
const router = createRouter({
    // 指定路由策略，hash 模式指的是 URL 的路径是通过 hash 符号（#）进行标识
    history: createWebHashHistory(),
    // routes: routes 的缩写
    routes,
})

// ES6 模块导出语句，它用于将 router 对象导出，以便其他文件可以导入和使用这个对象
export default router
