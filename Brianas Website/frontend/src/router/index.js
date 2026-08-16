import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import AdminView from '../views/AdminView.vue'
import ListingView from '../views/ListingView.vue'
import ListingDetailView from '../views/ListingDetailView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminView,
      meta: { requiresAdmin: true },
    },
    {
      path: '/admin/listings',
      name: 'listings-admin',
      component: ListingView,
      meta: { requiresAdmin: true },
    },
    {
      path: '/listings/:id',
      name: 'listing-detail',
      component: ListingDetailView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.requiresAdmin) {
    return true
  }

  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (user?.role === 'ADMIN') {
      return true
    }
  } catch {
    localStorage.removeItem('user')
  }

  return { name: 'home' }
})

export default router
