<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTeamsStore, type JoinRequest, type TeamCaptainView, type TeamPublic } from '../stores/teams'

const route = useRoute()
const auth = useAuthStore()
const teams = useTeamsStore()

const team = ref<TeamPublic | TeamCaptainView | null>(null)
const joinRequests = ref<JoinRequest[]>([])
const localError = ref<string | null>(null)

const teamId = computed(() => String(route.params.teamId))

const isCaptainView = computed(() => {
  const t = team.value
  return Boolean(auth.isAuthed && t && 'captainUserId' in t)
})

const isCaptain = computed(() => {
  const t = team.value
  if (!t || !('captainUserId' in t)) return false
  return (t as TeamCaptainView).captainUserId === auth.me?.id
})

const isMember = computed(() => {
  const t = team.value
  if (!t || !auth.me?.id) return false
  if ('captainUserId' in t) {
    return (t as TeamCaptainView).memberUserIds.includes(auth.me.id)
  }
  return false
})

const isAdmin = computed(() => auth.me?.role === 'ADMIN')

const soleCaptain = computed(() => {
  const t = team.value
  if (!t || !isCaptain.value) return false
  return t.memberCount === 1
})

const logoFile = ref<HTMLInputElement | null>(null)

async function reload() {
  localError.value = null
  team.value = (await teams.getTeam(auth.token, teamId.value)) as TeamPublic | TeamCaptainView

  if (isCaptain.value) {
    joinRequests.value = await teams.listJoinRequests(auth.token!, teamId.value)
  } else {
    joinRequests.value = []
  }
}

onMounted(async () => {
  try {
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
})

async function approveAdmin() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.approveTeamAdmin(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function requestJoin() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.requestJoin(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function accept(reqId: string) {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.acceptJoin(auth.token, teamId.value, reqId)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function reject(reqId: string) {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.rejectJoin(auth.token, teamId.value, reqId)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function resetLogoAdmin() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.resetLogoAdmin(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function uploadLogo() {
  if (!auth.token) return
  const input = logoFile.value
  const file = input?.files?.[0]
  if (!file) return
  localError.value = null
  try {
    await teams.uploadTeamLogo(auth.token, teamId.value, file)
    if (input) input.value = ''
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function resetLogoCaptain() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.resetTeamLogoCaptain(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function leaveTeam() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.leaveTeam(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

async function disbandTeam() {
  if (!auth.token) return
  localError.value = null
  try {
    await teams.disbandTeam(auth.token, teamId.value)
    await reload()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}
</script>

<template>
  <div v-if="!team" class="text-sm text-zinc-400">Cargando…</div>

  <div v-else class="space-y-6">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <div class="text-xs text-zinc-500">{{ team.status }}</div>
        <h1 class="mt-1 text-2xl font-semibold tracking-tight">{{ team.name }}</h1>
        <p class="mt-2 text-sm text-zinc-400">
          {{ team.regionServer }} · {{ team.memberCount }} miembros
          <span v-if="isCaptainView" class="text-zinc-500">
            · capitán: {{ (team as TeamCaptainView).captainUsername }}
          </span>
        </p>
      </div>

      <div class="flex flex-wrap gap-2">
        <button
          type="button"
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900"
          @click="reload()"
        >
          Refrescar
        </button>

        <button
          v-if="isAdmin && team.status === 'PENDING'"
          type="button"
          class="rounded-md bg-emerald-400 px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-emerald-300"
          @click="approveAdmin()"
        >
          Aprobar (admin)
        </button>

        <button
          v-if="isAdmin"
          type="button"
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900"
          @click="resetLogoAdmin()"
        >
          Reset logo (admin)
        </button>

        <button
          v-if="auth.isAuthed && team.status === 'APPROVED' && !isMember && !isCaptain"
          type="button"
          class="rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200"
          @click="requestJoin()"
        >
          Solicitar unirse
        </button>

        <button
          v-if="auth.isAuthed && isMember && !isCaptain && team.status !== 'DISBANDED'"
          type="button"
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900"
          @click="leaveTeam()"
        >
          Salir del equipo
        </button>

        <button
          v-if="auth.isAuthed && isCaptain && soleCaptain && team.status !== 'DISBANDED'"
          type="button"
          class="rounded-md border border-rose-900/40 bg-rose-950/40 px-3 py-2 text-sm text-rose-100 hover:bg-rose-950/60"
          @click="disbandTeam()"
        >
          Disolver equipo
        </button>
      </div>
    </div>

    <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>

    <section class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
      <h2 class="text-sm font-semibold text-zinc-200">Logo</h2>
      <div class="mt-4 flex flex-wrap items-center gap-4">
        <img :src="team.logoUrl" alt="logo" class="h-16 w-16 rounded-md border border-zinc-800 object-cover" />

        <div v-if="isCaptain && (team.status === 'PENDING' || team.status === 'APPROVED')" class="flex flex-col gap-2">
          <input ref="logoFile" type="file" accept=".jpg,.jpeg,.png,.webp" class="text-xs text-zinc-300" />
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              class="rounded-md bg-white px-3 py-2 text-xs font-medium text-zinc-950 hover:bg-zinc-200"
              @click="uploadLogo()"
            >
              Subir logo
            </button>
            <button
              type="button"
              class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-zinc-100 hover:bg-zinc-900"
              @click="resetLogoCaptain()"
            >
              Reset logo
            </button>
          </div>
          <p class="text-xs text-zinc-500">Máx 2MB. Solo JPG/PNG/WEBP.</p>
        </div>
      </div>
    </section>

    <section v-if="isCaptainView" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
      <h2 class="text-sm font-semibold text-zinc-200">Miembros</h2>
      <ul class="mt-3 space-y-2 text-sm text-zinc-200">
        <li v-for="(u, idx) in (team as TeamCaptainView).memberUsernames" :key="idx" class="font-mono text-xs text-zinc-300">
          {{ u }}
        </li>
      </ul>
    </section>

    <section v-if="isCaptain" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
      <h2 class="text-sm font-semibold text-zinc-200">Solicitudes pendientes</h2>
      <div v-if="joinRequests.length === 0" class="mt-3 text-sm text-zinc-500">Sin solicitudes.</div>
      <div v-else class="mt-4 space-y-2">
        <div
          v-for="r in joinRequests"
          :key="r.id"
          class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-zinc-800 bg-zinc-950/40 px-3 py-2"
        >
          <div class="text-sm text-zinc-200">
            <span class="font-mono text-xs text-zinc-400">{{ r.requesterUsername }}</span>
          </div>
          <div class="flex gap-2">
            <button
              type="button"
              class="rounded-md bg-emerald-400 px-3 py-1.5 text-xs font-medium text-zinc-950 hover:bg-emerald-300"
              @click="accept(r.id)"
            >
              Aceptar
            </button>
            <button
              type="button"
              class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-1.5 text-xs text-zinc-100 hover:bg-zinc-900"
              @click="reject(r.id)"
            >
              Rechazar
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
