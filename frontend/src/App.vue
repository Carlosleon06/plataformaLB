<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { onMounted, onUnmounted, watch } from 'vue'
import type { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import { useAuthStore } from './stores/auth'
import { type UserNotificationPayload, useNotificationsStore } from './stores/notifications'
import { coerceBearerToken } from './lib/api'
import { createSockJsStompClient } from './lib/stompSockJs'
import ToastStack from './components/ToastStack.vue'

const auth = useAuthStore()
const notif = useNotificationsStore()

let pollTimer: ReturnType<typeof setInterval> | null = null
let notifStomp: Client | null = null
let notifSub: StompSubscription | null = null

function stopPoll() {
  if (pollTimer != null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function disconnectNotifStomp() {
  if (notifSub) {
    try {
      notifSub.unsubscribe()
    } catch {
      /* ignore */
    }
    notifSub = null
  }
  if (notifStomp) {
    void notifStomp.deactivate()
    notifStomp = null
  }
}

function connectNotifStomp() {
  const bearer = coerceBearerToken(auth.token)
  if (!bearer) {
    disconnectNotifStomp()
    return
  }
  disconnectNotifStomp()
  const c = createSockJsStompClient({ Authorization: `Bearer ${bearer}` })
  notifStomp = c
  c.onConnect = () => {
    if (notifSub) {
      try {
        notifSub.unsubscribe()
      } catch {
        /* ignore */
      }
      notifSub = null
    }
    notifSub = c.subscribe('/user/queue/notifications', (msg: IMessage) => {
      const row = JSON.parse(msg.body) as UserNotificationPayload
      notif.enqueueToast(row)
      notif.bumpNotificationCursor(row.createdAt)
    })
  }
  c.activate()
}

function startPoll() {
  stopPoll()
  if (!auth.isAuthed) return
  void notif.poll(auth.token)
  pollTimer = window.setInterval(() => {
    void notif.poll(auth.token)
  }, 60_000)
}

onMounted(() => {
  void auth.bootstrap().finally(() => {
    startPoll()
    connectNotifStomp()
  })
})

watch(
  () => `${auth.isAuthed}:${coerceBearerToken(auth.token) ?? ''}`,
  () => {
    startPoll()
    connectNotifStomp()
  },
)

onUnmounted(() => {
  stopPoll()
  disconnectNotifStomp()
})
</script>

<template>
  <div class="relative min-h-full">
    <div class="lb-main-bg" aria-hidden="true" />
    <div class="lb-scanlines" aria-hidden="true" />

    <header
      class="sticky top-0 z-40 border-b border-violet-500/20 bg-zinc-950/75 shadow-lg shadow-black/30 backdrop-blur-xl"
    >
      <div class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 md:px-5">
        <RouterLink
          to="/"
          class="group font-display text-base font-bold uppercase tracking-wide text-transparent transition"
        >
          <span
            class="bg-gradient-to-r from-violet-200 via-white to-cyan-200 bg-clip-text drop-shadow-[0_0_28px_rgba(167,139,250,0.45)] group-hover:from-violet-100 group-hover:via-violet-50 group-hover:to-cyan-100"
            >BON e-sports</span
          >
        </RouterLink>

        <nav class="flex flex-wrap items-center justify-end gap-x-2 gap-y-1 text-sm md:gap-x-1">
          <RouterLink class="lb-nav-link font-medium" active-class="lb-nav-link-active" to="/tournaments"
            >Torneos</RouterLink
          >
          <RouterLink class="lb-nav-link font-medium" active-class="lb-nav-link-active" to="/teams">Equipos</RouterLink>
          <RouterLink class="lb-nav-link font-medium" active-class="lb-nav-link-active" to="/leaderboards"
            >Rankings</RouterLink
          >
          <template v-if="auth.me?.role === 'ADMIN'">
            <span class="hidden px-1 text-zinc-700 sm:inline" aria-hidden="true">·</span>
            <RouterLink
              class="lb-nav-link font-medium text-amber-200/90 hover:text-amber-50"
              active-class="lb-nav-link-active text-amber-100"
              to="/admin/teams"
              >Admin equipos</RouterLink
            >
            <RouterLink
              class="lb-nav-link font-medium text-amber-200/90 hover:text-amber-50"
              active-class="lb-nav-link-active text-amber-100"
              to="/admin/tournaments"
              >Admin torneos</RouterLink
            >
            <RouterLink
              class="lb-nav-link font-medium text-amber-200/90 hover:text-amber-50"
              to="/admin/tournaments/create"
              >Crear torneo</RouterLink
            >
          </template>
          <template v-if="auth.isAuthed">
            <span class="hidden px-1 text-zinc-700 md:inline" aria-hidden="true">·</span>
            <RouterLink
              v-if="auth.me?.id"
              class="lb-nav-link hidden font-medium text-emerald-200/90 hover:text-emerald-50 sm:inline-block"
              active-class="lb-nav-link-active"
              :to="`/users/${auth.me.id}`"
              >Perfil</RouterLink
            >
            <RouterLink
              class="lb-nav-link hidden font-medium sm:inline-block"
              active-class="lb-nav-link-active"
              to="/profile/edit"
              >Editar</RouterLink
            >
            <RouterLink
              to="/"
              title="Inicio — perfil, historial y claim diario"
              class="hidden items-center gap-1 rounded-lg border border-zinc-800/80 bg-black/30 px-2.5 py-1 font-mono text-xs text-cyan-100/90 no-underline transition hover:border-cyan-500/40 hover:bg-cyan-950/20 hover:text-cyan-50 hover:shadow-[0_0_20px_-6px_rgba(34,211,238,0.35)] md:inline-flex focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-cyan-500/55"
              aria-label="Ir al inicio: L-Coins, historial y claim diario"
            >
              <span class="text-zinc-500">LC</span>
              <span class="font-semibold">{{ auth.me?.leonCoinsBalance ?? '—' }}</span>
            </RouterLink>
            <button type="button" class="lb-btn-ghost !px-2.5 !py-1.5 text-xs" @click="auth.logout()">Salir</button>
          </template>
          <template v-else>
            <RouterLink class="lb-nav-link font-medium" to="/login">Entrar</RouterLink>
            <RouterLink class="lb-btn-primary !px-3 !py-1.5 text-xs" to="/register">Registro</RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-6xl px-4 py-8 md:px-5 md:py-10">
      <RouterView />
    </main>

    <ToastStack />
  </div>
</template>
