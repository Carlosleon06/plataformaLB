<script setup lang="ts">
import { computed } from 'vue'
import { useNotificationsStore } from '../stores/notifications'

const notif = useNotificationsStore()
const rows = computed(() => notif.activeToasts)
</script>

<template>
  <div class="pointer-events-none fixed bottom-6 right-4 z-[9999] flex max-w-[min(100vw-2rem,380px)] flex-col gap-2">
    <div
      v-for="t in rows"
      :key="t._toastKey"
      class="pointer-events-auto rounded-lg border border-zinc-700 bg-zinc-950/95 px-4 py-3 shadow-2xl ring-1 ring-white/10 backdrop-blur"
    >
      <div class="flex items-start gap-3">
        <div class="min-w-0 flex-1">
          <div class="text-[11px] font-semibold uppercase tracking-wide text-sky-200/85">{{ t.title }}</div>
          <div class="mt-1 text-sm text-zinc-100">{{ t.summary }}</div>
        </div>
        <button type="button" class="mt-0.5 shrink-0 text-xs text-zinc-500 hover:text-zinc-200" @click="notif.dismissToast(t._toastKey)">
          Cerrar
        </button>
      </div>
    </div>
  </div>
</template>
