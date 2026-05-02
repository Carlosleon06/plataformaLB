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
  <div class="min-h-full">
    <header class="border-b border-zinc-800 bg-zinc-950/80 backdrop-blur">
      <div class="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
        <RouterLink to="/" class="text-sm font-semibold tracking-wide text-zinc-100">
          LEON BON
        </RouterLink>

        <nav class="flex items-center gap-3 text-sm">
          <RouterLink class="text-zinc-300 hover:text-white" to="/tournaments">Torneos</RouterLink>
          <RouterLink class="text-zinc-300 hover:text-white" to="/teams">Equipos</RouterLink>
          <RouterLink class="text-zinc-300 hover:text-white" to="/leaderboards">Rankings</RouterLink>
          <template v-if="auth.me?.role === 'ADMIN'">
            <RouterLink class="text-amber-200/90 hover:text-amber-100" to="/admin/teams">Admin equipos</RouterLink>
            <RouterLink class="text-amber-200/90 hover:text-amber-100" to="/admin/tournaments">Admin torneos</RouterLink>
            <RouterLink class="text-amber-200/90 hover:text-amber-100" to="/admin/tournaments/create">Crear torneo</RouterLink>
          </template>
          <template v-if="auth.isAuthed">
            <RouterLink
              v-if="auth.me?.id"
              class="hidden text-emerald-200/85 hover:text-emerald-100 sm:inline-block"
              :to="`/users/${auth.me.id}`"
              >Mi perfil</RouterLink
            >
            <RouterLink
              v-if="auth.isAuthed"
              class="hidden text-zinc-300 hover:text-white sm:inline-block"
              to="/profile/edit"
              >Editar perfil</RouterLink
            >
            <div class="hidden text-zinc-300 sm:block">
              <span class="text-zinc-500">L-Coins</span>
              <span class="ml-2 font-mono">{{ auth.me?.leonCoinsBalance ?? '—' }}</span>
            </div>
            <button
              type="button"
              class="rounded-md border border-zinc-800 bg-zinc-900 px-3 py-1.5 text-zinc-100 hover:bg-zinc-800"
              @click="auth.logout()"
            >
              Salir
            </button>
          </template>
          <template v-else>
            <RouterLink class="text-zinc-300 hover:text-white" to="/login">Entrar</RouterLink>
            <RouterLink
              class="rounded-md bg-white px-3 py-1.5 text-sm font-medium text-zinc-950 hover:bg-zinc-200"
              to="/register"
            >
              Registro
            </RouterLink>
          </template>
        </nav>
      </div>
    </header>

    <main class="mx-auto max-w-5xl px-4 py-8">
      <RouterView />
    </main>

    <ToastStack />
  </div>
</template>
