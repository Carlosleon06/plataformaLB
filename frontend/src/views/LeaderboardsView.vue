<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { usePlatformStore, type LeaderboardRow } from '../stores/platform'

const platform = usePlatformStore()

const tab = ref<'VALORANT' | 'FORTNITE' | 'MLB'>('VALORANT')
const rows = ref<LeaderboardRow[]>([])
const err = ref<string | null>(null)

async function load() {
  err.value = null
  try {
    rows.value = await platform.fetchLeaderboard(tab.value, 20)
  } catch (e) {
    err.value = e instanceof Error ? e.message : 'Error'
    rows.value = []
  }
}

onMounted(load)
watch(tab, load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-wrap gap-4 text-sm">
      <RouterLink to="/tournaments" class="text-zinc-400 hover:text-zinc-200">← Torneos</RouterLink>
    </div>

    <div>
      <h1 class="text-2xl font-semibold tracking-tight text-zinc-100">Rankings internos</h1>
      <p class="mt-2 max-w-2xl text-sm text-zinc-400">
        Top victorias en partidas de bracket ya marcadas como completadas, por videojuego. No hay datos externos: sólo cuentan los
        torneos cargados aquí y el resultado admin del bracket.
      </p>
    </div>

    <div class="flex flex-wrap gap-2 text-sm">
      <button
        v-for="x in ['VALORANT', 'FORTNITE', 'MLB'] as const"
        :key="x"
        type="button"
        class="rounded-md border px-3 py-2"
        :class="
          tab === x
            ? 'border-emerald-500/70 bg-emerald-950/30 text-emerald-100'
            : 'border-zinc-700 bg-zinc-950 text-zinc-300 hover:bg-zinc-900'
        "
        @click="tab = x"
      >
        {{ x }}
      </button>
    </div>

    <p v-if="err" class="text-sm text-rose-300">{{ err }}</p>

    <div class="overflow-hidden rounded-xl border border-zinc-800">
      <table class="w-full text-left text-sm">
        <thead class="bg-zinc-950 text-xs text-zinc-400">
          <tr>
            <th class="px-4 py-2">#</th>
            <th class="px-4 py-2">Jugador</th>
            <th class="px-4 py-2 text-right">Victorias</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="rows.length === 0" class="border-t border-zinc-800">
            <td class="px-4 py-6 text-center text-zinc-500" colspan="3">Sin datos suficientes aún para este juego.</td>
          </tr>
          <tr
            v-for="(row, idx) in rows"
            :key="row.userId"
            class="border-t border-zinc-800 bg-zinc-900/30 hover:bg-zinc-900/50"
          >
            <td class="px-4 py-2 font-mono text-zinc-500">{{ idx + 1 }}</td>
            <td class="px-4 py-2">
              <RouterLink :to="`/users/${row.userId}`" class="text-sky-300 hover:underline">
                {{ row.nickname?.trim() ? row.nickname : row.username }}
              </RouterLink>
              <div class="font-mono text-[11px] text-zinc-500">@{{ row.username }}</div>
            </td>
            <td class="px-4 py-2 text-right font-mono text-zinc-100">{{ row.bracketMatchWins }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
