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

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/tournaments', name: 'tournaments', component: TournamentsBrowseView },
    { path: '/tournaments/:tournamentId', name: 'tournament-detail', component: TournamentDetailView },
    { path: '/teams', name: 'teams', component: TeamsBrowseView },
    { path: '/teams/create', name: 'teams-create', component: TeamCreateView },
    { path: '/teams/:teamId', name: 'team-detail', component: TeamDetailView },
    { path: '/admin/teams', name: 'admin-teams-pending', component: AdminTeamsPendingView },
    { path: '/admin/tournaments', name: 'admin-tournaments', component: AdminTournamentsListView },
    { path: '/admin/tournaments/create', name: 'admin-tournaments-create', component: AdminTournamentCreateView },
  ],
})
