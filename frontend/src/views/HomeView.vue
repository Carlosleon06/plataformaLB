<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { formatDateTimeShort } from '../lib/format'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
</script>

<template>
  <div class="space-y-8">
    <div class="lb-card px-6 py-7 md:px-8 md:py-8">
      <p class="lb-hero-badge">Arena competitiva · L-Coins · brackets</p>
      <h1 class="lb-page-title mt-4 max-w-3xl">Tu hub de equipos y torneos.</h1>
      <p class="mt-3 max-w-2xl text-sm leading-relaxed text-zinc-400">
        Torneos con economía ligera (L-Coins), palmarés por lugar y pozo común (parimutuel) en partidas. Si eres admin: equipos
        pendientes, crear torneo y armar el bracket desde cada torneo.
      </p>
      <div class="mt-6 flex flex-wrap gap-3 text-sm">
        <RouterLink class="lb-btn-primary" to="/tournaments">Ver torneos</RouterLink>
        <RouterLink class="lb-btn-ghost" to="/teams">Ver equipos</RouterLink>
        <RouterLink
          class="lb-btn-ghost border-emerald-800/55 text-emerald-100 hover:border-emerald-500/40 hover:text-emerald-50"
          to="/leaderboards"
        >
          Rankings
        </RouterLink>
        <RouterLink
          v-if="auth.me?.role === 'ADMIN'"
          class="lb-btn-ghost border-amber-800/55 text-amber-100 hover:border-amber-500/35"
          to="/admin/teams"
        >
          Admin equipos
        </RouterLink>
        <RouterLink
          v-if="auth.me?.role === 'ADMIN'"
          class="lb-btn-ghost border-amber-800/55 text-amber-100 hover:border-amber-500/35"
          to="/admin/tournaments/create"
        >
          Crear torneo
        </RouterLink>
      </div>
    </div>

    <div v-if="!auth.isAuthed" class="lb-card p-6">
      <p class="text-sm text-zinc-300">Entra al lobby: inicia sesión con tu cuenta o regístrate gratis.</p>
      <div class="mt-4 flex flex-wrap gap-3">
        <RouterLink class="lb-btn-ghost" to="/login">Entrar</RouterLink>
        <RouterLink class="lb-btn-primary" to="/register">Crear cuenta</RouterLink>
      </div>
    </div>

    <div v-else class="grid gap-5 lg:grid-cols-2">
      <section class="lb-card p-6">
        <h2 class="border-b border-zinc-800/80 pb-2 font-display text-sm font-semibold uppercase tracking-wider text-violet-200/95">
          Perfil
        </h2>
        <dl class="mt-4 space-y-2 text-sm">
          <div v-if="auth.me?.leonPlayerNumber != null" class="flex justify-between gap-4">
            <dt class="text-zinc-500">Jugador #</dt>
            <dd class="font-mono text-zinc-100">{{ auth.me.leonPlayerNumber }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-zinc-500">Usuario</dt>
            <dd class="font-mono text-zinc-100">{{ auth.me?.username }}</dd>
          </div>
          <div class="flex justify-between gap-4">
            <dt class="text-zinc-500">Email</dt>
            <dd class="max-w-[12rem] truncate font-mono text-xs text-zinc-100">{{ auth.me?.email ?? '—' }}</dd>
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
            class="lb-btn-primary disabled:opacity-50"
            :disabled="auth.busy"
            @click="auth.dailyClaim()"
          >
            Daily claim (+100)
          </button>
          <button type="button" class="lb-btn-ghost disabled:opacity-50" :disabled="auth.busy" @click="auth.refreshAll()">
            Refrescar
          </button>
        </div>

        <p v-if="auth.error" class="mt-3 text-sm text-rose-300">{{ auth.error }}</p>
      </section>

      <section class="lb-card p-6">
        <h2 class="border-b border-zinc-800/80 pb-2 font-display text-sm font-semibold uppercase tracking-wider text-cyan-200/90">
          Historial económico
        </h2>
        <div class="lb-table-shell mt-4 !rounded-lg !shadow-none">
          <table class="w-full text-left text-xs">
            <thead class="border-b border-zinc-800/80 bg-zinc-950/90 text-[10px] font-semibold uppercase tracking-wider text-zinc-500">
              <tr>
                <th class="px-3 py-2">Fecha</th>
                <th class="px-3 py-2">Tipo</th>
                <th class="px-3 py-2 text-right">Monto</th>
                <th class="px-3 py-2 text-right">Saldo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in auth.transactions" :key="t.id" class="border-t border-zinc-800 bg-zinc-950/40">
                <td class="px-3 py-2 font-mono text-zinc-300">{{ formatDateTimeShort(t.createdAt) }}</td>
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
