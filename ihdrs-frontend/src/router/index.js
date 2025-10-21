// router.index.js

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'

// 路由配置
export const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/auth/Login.vue'),
        meta: {
            title: '登录',
            requireAuth: false,
            hideInMenu: true
        }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/auth/Register.vue'),
        meta: {
            title: '注册',
            requireAuth: false,
            hideInMenu: true
        }
    },
    {
        path: '/',
        component: () => import('@/layout/index.vue'),
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/dashboard/index.vue'),
                meta: {
                    title: '仪表板',
                    icon: 'DataBoard',
                    requireAuth: true
                }
            }
        ]
    },
    {
        path: '/recognition',
        component: () => import('@/layout/index.vue'),
        redirect: '/recognition/history',
        meta: {
            title: '识别管理',
            icon: 'View',
            requireAuth: true
        }/*,
        children: [
            {
                path: 'history',
                name: 'RecognitionHistory',
                component: () => import('@/views/recognition/history.vue'),
                meta: {
                    title: '识别记录',
                    icon: 'List',
                    requireAuth: true
                }
            },
            {
                path: 'feedback',
                name: 'FeedbackManage',
                component: () => import('@/views/recognition/feedback.vue'),
                meta: {
                    title: '用户反馈',
                    icon: 'ChatDotRound',
                    requireAuth: true,
                    roles: ['ADMIN']
                }
            }
        ]*/
    },
    {
        path: '/models',
        component: () => import('@/layout/index.vue'),
        redirect: '/models/list',
        meta: {
            title: '模型管理',
            icon: 'Setting',
            requireAuth: true,
            roles: ['ADMIN']
        }/*,
        children: [
            {
                path: 'list',
                name: 'ModelList',
                component: () => import('@/views/models/list.vue'),
                meta: {
                    title: '模型列表',
                    icon: 'List',
                    requireAuth: true,
                    roles: ['ADMIN']
                }
            },
            {
                path: 'training',
                name: 'ModelTraining',
                component: () => import('@/views/models/training.vue'),
                meta: {
                    title: '模型训练',
                    icon: 'Operation',
                    requireAuth: true,
                    roles: ['ADMIN']
                }
            }
        ]*/
    },
    {
        path: '/users',
        component: () => import('@/layout/index.vue'),
        redirect: '/users/list',
        meta: {
            title: '用户管理',
            icon: 'User',
            requireAuth: true,
            roles: ['ADMIN']
        }/*,
        children: [
            {
                path: 'list',
                name: 'UserList',
                component: () => import('@/views/users/list.vue'),
                meta: {
                    title: '用户列表',
                    icon: 'List',
                    requireAuth: true,
                    roles: ['ADMIN']
                }
            }
        ]*/
    },
    {
        path: '/statistics',
        component: () => import('@/layout/index.vue'),
        redirect: '/statistics/overview',
        meta: {
            title: '统计分析',
            icon: 'DataAnalysis',
            requireAuth: true,
            roles: ['ADMIN']
        }/*,
        children: [
            {
                path: 'overview',
                name: 'StatisticsOverview',
                component: () => import('@/views/statistics/overview.vue'),
                meta: {
                    title: '系统概览',
                    icon: 'DataBoard',
                    requireAuth: true,
                    roles: ['ADMIN']
                }
            }
        ]*/
    }/*,
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/error/404.vue'),
        meta: {
            title: '页面未找到',
            hideInMenu: true
        }
    }*/
]

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition
        } else {
            return { top: 0 }
        }
    }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
    NProgress.start()

    const userStore = useUserStore()

    // 设置页面标题
    document.title = `${to.meta.title || '页面'} - IHDRS管理系统`

    // 不需要认证的页面直接放行
    if (!to.meta.requireAuth) {
        // 如果已登录用户访问登录页，重定向到首页
        if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
            next('/')
            return
        }
        next()
        return
    }

    // 需要认证的页面
    if (!userStore.isLoggedIn) {
        ElMessage.warning('请先登录')
        next({
            path: '/login',
            query: { redirect: to.fullPath }
        })
        return
    }

    // 验证token有效性
    const isTokenValid = await userStore.validateToken()
    if (!isTokenValid) {
        next({
            path: '/login',
            query: { redirect: to.fullPath }
        })
        return
    }

    // 检查角色权限
    if (to.meta.roles && to.meta.roles.length > 0) {
        const hasPermission = to.meta.roles.some(role => userStore.hasRole(role))
        if (!hasPermission) {
            ElMessage.error('权限不足')
            next('/dashboard')
            return
        }
    }

    next()
})

router.afterEach(() => {
    NProgress.done()
})

export default router