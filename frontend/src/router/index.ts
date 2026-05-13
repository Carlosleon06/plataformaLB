import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import TeamsBrowseView from '../views/TeamsBrowseView.vue'
import TeamCreateView from '../views/TeamCreateView.vue'
import TeamDetailView from '../views/TeamDetailView.vue'
import TournamentsBrowseView from '../views/TournamentsBrowseView.vue'
import TournamentDetailView from '../views/TournamentDetailView.vue'
import AdminTeamsPendingView from '../views/AdminTeamsPendingView.vue'
import AdminTournamentCreateView from '../views/AdminTournamentCreateView.vue'
import AdminTournamentsListView from '../views/AdminTournamentsListView.vue'
import ProfileEditView from '../views/ProfileEditView.vue'
import UserProfilePublicView from '../views/UserProfilePublicView.vue'
import LeaderboardsView from '../views/LeaderboardsView.vue'
import { useAuthStore } from '../stores/auth'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/tournaments', name: 'tournaments', component: TournamentsBrowseView },
    { path: '/tournaments/:tournamentId', name: 'tournament-detail', component: TournamentDetailView },
    { path: '/teams', name: 'teams', component: TeamsBrowseView },
    { path: '/teams/create', name: 'teams-create', component: TeamCreateView, meta: { requiresAuth: true } },
    { path: '/teams/:teamId', name: 'team-detail', component: TeamDetailView },
    { path: '/profile/edit', name: 'profile-edit', component: ProfileEditView, meta: { requiresAuth: true } },
    { path: '/leaderboards', name: 'leaderboards', component: LeaderboardsView },
    { path: '/users/:userId', name: 'user-public-profile', component: UserProfilePublicView },
    { path: '/admin/teams', name: 'admin-teams-pending', component: AdminTeamsPendingView, meta: { requiresAdmin: true } },
    { path: '/admin/tournaments', name: 'admin-tournaments', component: AdminTournamentsListView, meta: { requiresAdmin: true } },
    {
      path: '/admin/tournaments/create',
      name: 'admin-tournaments-create',
      component: AdminTournamentCreateView,
      meta: { requiresAdmin: true },
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const requiresAuth = Boolean(to.meta.requiresAuth)
  const requiresAdmin = Boolean(to.meta.requiresAdmin)
  if (requiresAuth && !auth.isAuthed) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (requiresAdmin) {
    if (!auth.isAuthed) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (auth.me?.role !== 'ADMIN') {
      return { name: 'home' }
    }
  }
  return true
})
