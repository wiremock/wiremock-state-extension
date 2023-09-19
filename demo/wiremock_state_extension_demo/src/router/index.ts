import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'Todo List',
      component: () => import('@/components/todo-list/todo-list.vue'),
    },
    {
      path: '/todo-items/new',
      name: 'New Todo Item',
      component: () => import('@/components/todo-list/todo-item/new-todo-item.vue'),
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/components/profile/edit-profile.vue'),
    },
  ],
})

export default router
