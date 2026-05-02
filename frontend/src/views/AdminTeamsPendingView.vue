<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useTeamsStore, type PendingTeamAdminRow } from '../stores/teams'

const auth = useAuthStore()
const teams = useTeamsStore()

const rows = ref<PendingTeamAdminRow[]>([])
const localError = ref<string | null>(null)
const rowBusy = ref<string | null>(null)

async function load() {
  if (!auth.token) return
  localError.value = null
  try {
    rows.value = await teams.listPendingTeamsAdmin(auth.token)
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  }
}

onMounted(async () => {
  await load()
})

function fmt(iso: string) {
  return new Date(iso).toLocaleString()
}

async function approve(teamId: string) {
  if (!auth.token) return
  rowBusy.value = teamId
  localError.value = null
  try {
    await teams.approveTeamAdmin(auth.token, teamId)
    await load()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    rowBusy.value = null
  }
}

async function reject(teamId: string) {
  if (!auth.token) return
  rowBusy.value = teamId
  localError.value = null
  try {
    await teams.rejectTeamAdmin(auth.token, teamId)
    await load()
  } catch (e) {
    localError.value = e instanceof Error ? e.message : 'Error'
  } finally {
    rowBusy.value = null
  }
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-semibold tracking-tight text-zinc-100">Admin — equipos pendientes</h1>
      <p class="mt-2 max-w-2xl text-sm text-zinc-400">
        Equipos en estado PENDING. Desde aquí puedes aprobar o rechazar; también puedes abrir el detalle para ver roster y
        logo.
      </p>
    </div>

    <div class="flex flex-wrap gap-2">
      <RouterLink to="/teams" class="text-sm text-zinc-400 hover:text-zinc-200">← Equipos públicos</RouterLink>
      <button
        type="button"
        class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-1.5 text-sm text-zinc-200 hover:bg-zinc-900"
        @click="load()"
      >
        Refrescar
      </button>
    </div>

    <p v-if="!auth.isAuthed" class="text-sm text-zinc-400">
      <RouterLink class="text-sky-400 hover:underline" to="/login">Inicia sesión</RouterLink>
      con una cuenta admin.
    </p>

    <p v-if="localError" class="text-sm text-rose-300">{{ localError }}</p>

    <div v-if="auth.isAuthed && rows.length === 0 && !localError" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-6 text-sm text-zinc-400">
      No hay equipos pendientes de aprobación.
    </div>

    <div v-if="rows.length > 0" class="overflow-hidden rounded-xl border border-zinc-800">
      <table class="w-full text-left text-sm">
        <thead class="bg-zinc-950 text-xs text-zinc-400">
          <tr>
            <th class="px-4 py-2">Equipo</th>
            <th class="hidden px-4 py-2 sm:table-cell">Capitán</th>
            <th class="px-4 py-2">Miembros</th>
            <th class="hidden px-4 py-2 md:table-cell">Creado</th>
            <th class="px-4 py-2 text-right">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rows" :key="r.id" class="border-t border-zinc-800 bg-zinc-950/40">
            <td class="px-4 py-3">
              <RouterLink class="font-medium text-zinc-100 hover:underline" :to="`/teams/${r.id}`">{{ r.name }}</RouterLink>
              <span class="ml-2 font-mono text-xs text-zinc-500">[{{ r.tag }}]</span>
            </td>
            <td class="hidden px-4 py-3 text-zinc-300 sm:table-cell">{{ r.captainUsername }}</td>
            <td class="px-4 py-3 text-zinc-400">{{ r.memberCount }}</td>
            <td class="hidden px-4 py-3 text-xs text-zinc-500 md:table-cell">{{ fmt(r.createdAt) }}</td>
            <td class="px-4 py-3 text-right">
              <button
                type="button"
                class="mr-1 rounded border border-emerald-800 px-2 py-1 text-xs text-emerald-300 hover:bg-emerald-950/60 disabled:opacity-40"
                :disabled="rowBusy === r.id"
                @click="approve(r.id)"
              >
                Aprobar
              </button>
              <button
                type="button"
                class="rounded border border-rose-900 px-2 py-1 text-xs text-rose-300 hover:bg-rose-950/40 disabled:opacity-40"
                :disabled="rowBusy === r.id"
                @click="reject(r.id)"
              >
                Rechazar
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
