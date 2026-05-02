<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-semibold tracking-tight">LEON BON — Inicio</h1>
      <p class="mt-2 max-w-2xl text-sm text-zinc-400">
        Economía (L-Coins), equipos y torneos. Si eres admin, usa «Admin equipos» para aprobar equipos nuevos y el detalle
        de torneo para inscripciones.
      </p>
      <div class="mt-4 flex flex-wrap gap-3 text-sm">
        <RouterLink class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-zinc-100 hover:bg-zinc-900" to="/tournaments">
          Ver torneos
        </RouterLink>
        <RouterLink class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-zinc-100 hover:bg-zinc-900" to="/teams">
          Ver equipos
        </RouterLink>
        <RouterLink
          v-if="auth.me?.role === 'ADMIN'"
          class="rounded-md border border-amber-900/50 bg-amber-950/30 px-3 py-2 text-amber-100 hover:bg-amber-950/50"
          to="/admin/teams"
        >
          Admin equipos
        </RouterLink>
      </div>
    </div>

    <div v-if="!auth.isAuthed" class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
      <p class="text-sm text-zinc-300">Aún no hay sesión.</p>
      <div class="mt-4 flex flex-wrap gap-3">
        <RouterLink class="rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200" to="/login">
          Entrar
        </RouterLink>
        <RouterLink
          class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900"
          to="/register"
        >
          Crear cuenta
        </RouterLink>
      </div>
    </div>

    <div v-else class="grid gap-4 lg:grid-cols-2">
      <section class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Perfil</h2>
        <dl class="mt-4 space-y-2 text-sm">
          <div class="flex justify-between gap-4">
            <dt class="text-zinc-500">Usuario</dt>
            <dd class="font-mono text-zinc-100">{{ auth.me?.username }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-zinc-500">Nickname</dt>
            <dd class="text-zinc-100">{{ auth.me?.nickname ?? '—' }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-zinc-500">Estado</dt>
            <dd class="text-zinc-100">{{ auth.me?.status }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-zinc-500">Rol</dt>
            <dd class="text-zinc-100">{{ auth.me?.role }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-zinc-500">L-Coins</dt>
            <dd class="font-mono text-zinc-100">{{ auth.me?.leonCoinsBalance }}</dd>
          </div>
        </dl>

        <div class="mt-5 flex flex-wrap gap-3">
          <button
            type="button"
            class="rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200 disabled:opacity-50"
            :disabled="auth.busy"
            @click="auth.dailyClaim()"
          >
            Daily claim (+100)
          </button>
          <button
            type="button"
            class="rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 hover:bg-zinc-900 disabled:opacity-50"
            :disabled="auth.busy"
            @click="auth.refreshAll()"
          >
            Refrescar
          </button>
        </div>

        <p v-if="auth.error" class="mt-3 text-sm text-rose-300">{{ auth.error }}</p>
      </section>

      <section class="rounded-xl border border-zinc-800 bg-zinc-900/40 p-5">
        <h2 class="text-sm font-semibold text-zinc-200">Historial</h2>
        <div class="mt-4 overflow-hidden rounded-lg border border-zinc-800">
          <table class="w-full text-left text-xs">
            <thead class="bg-zinc-950 text-zinc-400">
              <tr>
                <th class="px-3 py-2">Fecha</th>
                <th class="px-3 py-2">Tipo</th>
                <th class="px-3 py-2 text-right">Monto</th>
                <th class="px-3 py-2 text-right">Saldo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in auth.transactions" :key="t.id" class="border-t border-zinc-800 bg-zinc-950/40">
                <td class="px-3 py-2 font-mono text-zinc-300">{{ new Date(t.createdAt).toLocaleString() }}</td>
                <td class="px-3 py-2 text-zinc-200">{{ t.type }}</td>
                <td class="px-3 py-2 text-right font-mono" :class="t.amount >= 0 ? 'text-emerald-300' : 'text-rose-300'">
                  {{ t.amount >= 0 ? `+${t.amount}` : t.amount }}
                </td>
                <td class="px-3 py-2 text-right font-mono text-zinc-200">{{ t.balanceAfter }}</td>
              </tr>
              <tr v-if="auth.transactions.length === 0">
                <td class="px-3 py-6 text-center text-zinc-500" colspan="4">Sin transacciones aún.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>
