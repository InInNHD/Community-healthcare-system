import { createRouter, createWebHistory } from 'vue-router'
import { auth, hasAnyRole, homeForCurrentUser, logout } from './auth'
import { resources } from './resource-config'
import { configuredEntryPortal, entryHome, isPortalAllowed } from './config/entry-portal'

const LoginView = () => import('./views/LoginView.vue')
const ChangePasswordView = () => import('./views/ChangePasswordView.vue')
const DashboardView = () => import('./views/DashboardView.vue')
const ResourceView = () => import('./views/ResourceView.vue')
const StaffPortalView = () => import('./views/StaffPortalView.vue')
const ResidentPortalView = () => import('./views/ResidentPortalView.vue')
const PlatformWorkbenchView = () => import('./views/PlatformWorkbenchView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true, title: '登录' } },
    { path: '/change-password', component: ChangePasswordView, meta: { title: '首次登录修改密码', selfContained: true } },
    { path: '/', redirect: '/admin' },
    { path: '/admin', component: DashboardView, meta: { title: '管理工作台', roles: ['ADMIN'], portal: 'admin' } },
    { path: '/admin/platform', component: PlatformWorkbenchView, props: { portal: 'admin' }, meta: { title: '平台治理中心', roles: ['ADMIN'], portal: 'admin' } },
    ...Object.entries(resources).map(([path, config]) => ({ path: `/admin/${path}`, component: ResourceView, props: { config }, meta: { title: config.title, roles: ['ADMIN'], portal: 'admin' } })),
    { path: '/staff', component: StaffPortalView, meta: { title: '医护工作台', roles: ['DOCTOR', 'NURSE', 'PHARMACIST', 'REGISTRAR'], portal: 'staff', standalone: true } },
    { path: '/staff/operations', component: PlatformWorkbenchView, props: { portal: 'staff' }, meta: { title: '医护业务中心', roles: ['DOCTOR', 'NURSE', 'PHARMACIST', 'REGISTRAR'], portal: 'staff', standalone: true } },
    { path: '/resident', component: ResidentPortalView, meta: { title: '居民健康门户', roles: ['RESIDENT'], portal: 'resident', standalone: true, selfContained: true } },
    { path: '/resident/services', component: PlatformWorkbenchView, props: { portal: 'resident' }, meta: { title: '居民业务中心', roles: ['RESIDENT'], portal: 'resident', standalone: true } },
    ...Object.keys(resources).map(path => ({ path: `/${path}`, redirect: `/admin/${path}` })),
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  document.title = `${String(to.meta.title ?? '工作台')} · 社区健康云`
  if (configuredEntryPortal && auth.user && auth.user.portal !== configuredEntryPortal) await logout()
  const targetPortal = to.meta.portal as 'admin' | 'staff' | 'resident' | undefined
  if (targetPortal && !isPortalAllowed(configuredEntryPortal, targetPortal)) {
    return auth.authenticated && configuredEntryPortal ? entryHome(configuredEntryPortal) : { path: '/login', query: { portal: configuredEntryPortal } }
  }
  if (!to.meta.public && !auth.authenticated) return '/login'
  if (to.path === '/login' && auth.authenticated) return homeForCurrentUser()
  if (auth.authenticated && auth.user?.mustChangePassword && to.path !== '/change-password') return '/change-password'
  if (to.path === '/change-password' && !auth.user?.mustChangePassword) return homeForCurrentUser()
  const roles = to.meta.roles as string[] | undefined
  if (roles && !hasAnyRole(roles)) return homeForCurrentUser()
  if (to.path === '/' && auth.authenticated) return homeForCurrentUser()
})

export default router
