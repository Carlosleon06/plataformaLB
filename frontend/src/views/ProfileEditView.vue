<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import type { PatchMyProfilePayload } from '../stores/auth'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const nickname = ref('')
const email = ref('')
const fullName = ref('')
const profileShowFullName = ref(false)
const country = ref('')
const twitch = ref('')
const youtube = ref('')
const xUrl = ref('')
const instagram = ref('')
const discord = ref('')
const preferredGame = ref('')
const ranksJson = ref('')
const busy = ref(false)
const msg = ref<string | null>(null)
const localErr = ref<string | null>(null)

function loadFromMe() {
  const m = auth.me
  if (!m) return
  nickname.value = m.nickname ?? ''
  email.value = m.email ?? ''
  fullName.value = m.fullName ?? ''
  profileShowFullName.value = m.profileShowFullName
  country.value = m.country ?? ''
  const s = m.socialLinks
  twitch.value = s.twitchUrl ?? ''
  youtube.value = s.youtubeUrl ?? ''
  xUrl.value = s.xUrl ?? ''
  instagram.value = s.instagramUrl ?? ''
  discord.value = s.discord ?? ''
  preferredGame.value = m.preferredGame ?? ''
  const rk = m.rankLabelsByGame ?? {}
  ranksJson.value = Object.keys(rk).length === 0 ? '' : JSON.stringify(rk, null, 2)
}

onMounted(() => {
  loadFromMe()
})

async function submit() {
  localErr.value = null
  msg.value = null
  if (!auth.token) return
  const raw = ranksJson.value.trim()
  let rankLabelsByGame: Record<string, string>
  if (raw.length === 0) {
    rankLabelsByGame = {}
  } else {
    try {
      rankLabelsByGame = JSON.parse(raw) as Record<string, string>
    } catch {
      localErr.value = 'Rangos: JSON inválido'
      return
    }
  }
  const payload: Record<string, unknown> = {
    nickname: nickname.value.trim(),
    email: email.value.trim(),
    fullName: fullName.value.trim() || undefined,
    profileShowFullName: profileShowFullName.value,
    country: country.value.trim() || undefined,
    twitchProfileUrl: twitch.value.trim() || undefined,
    youtubeChannelUrl: youtube.value.trim() || undefined,
    xProfileUrl: xUrl.value.trim() || undefined,
    instagramProfileUrl: instagram.value.trim() || undefined,
    discordHandle: discord.value.trim() || undefined,
  }
  if (preferredGame.value.trim() !== '') {
    payload.preferredGame = preferredGame.value.trim().toUpperCase()
  }
  payload.rankLabelsByGame = rankLabelsByGame
  busy.value = true
  try {
    await auth.patchProfile(payload as PatchMyProfilePayload)
    msg.value = 'Guardado.'
    loadFromMe()
  } catch (e) {
    localErr.value = e instanceof Error ? e.message : 'Error'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-lg space-y-4">
    <RouterLink to="/" class="text-sm text-zinc-400 hover:text-zinc-200">← Inicio</RouterLink>
    <div>
      <h1 class="text-xl font-semibold">Editar perfil</h1>
      <p class="mt-2 text-sm text-zinc-400">
        Identidad opcional visible en tu ficha pública cuando lo permitís. Email no aparece públicamente pero ayuda como contacto para
        el staff.
      </p>
    </div>

    <form class="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/40 p-5" @submit.prevent="submit()">
      <label class="block text-sm">
        <span class="text-zinc-400">Nickname</span>
        <input v-model="nickname" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none focus:border-zinc-600" />
      </label>
      <label class="block text-sm">
        <span class="text-zinc-400">Email</span>
        <input
          v-model="email"
          type="email"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none focus:border-zinc-600"
        />
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Nombre legal (solo si querés cargarlo)</span>
        <input v-model="fullName" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none focus:border-zinc-600" />
      </label>
      <label class="flex items-center gap-2 text-sm text-zinc-300">
        <input v-model="profileShowFullName" type="checkbox" class="accent-emerald-500" />
        Mostrar nombre legal en el perfil público
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">País / región</span>
        <input v-model="country" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none focus:border-zinc-600" />
      </label>

      <div class="grid gap-3 sm:grid-cols-2">
        <label class="block text-sm">
          <span class="text-zinc-400">Twitch (URL)</span>
          <input v-model="twitch" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs outline-none focus:border-zinc-600" />
        </label>
        <label class="block text-sm">
          <span class="text-zinc-400">YouTube</span>
          <input v-model="youtube" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs outline-none focus:border-zinc-600" />
        </label>
        <label class="block text-sm">
          <span class="text-zinc-400">X / Twitter</span>
          <input v-model="xUrl" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs outline-none focus:border-zinc-600" />
        </label>
        <label class="block text-sm">
          <span class="text-zinc-400">Instagram</span>
          <input v-model="instagram" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs outline-none focus:border-zinc-600" />
        </label>
      </div>
      <label class="block text-sm">
        <span class="text-zinc-400">Discord</span>
        <input v-model="discord" class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none focus:border-zinc-600" />
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Juego principal</span>
        <select
          v-model="preferredGame"
          class="mt-1 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm outline-none focus:border-zinc-600"
        >
          <option value="">Sin definir</option>
          <option value="VALORANT">Valorant</option>
          <option value="FORTNITE">Fortnite</option>
          <option value="MLB">MLB The Show</option>
        </select>
      </label>

      <label class="block text-sm">
        <span class="text-zinc-400">Rangos por juego (JSON plano ej. {\"VALORANT\":\"Immortal 2\"})</span>
        <textarea
          v-model="ranksJson"
          rows="4"
          class="mt-1 w-full resize-y rounded-md border border-zinc-800 bg-zinc-950 px-3 py-2 font-mono text-xs outline-none focus:border-zinc-600"
        />
      </label>

      <button
        type="submit"
        class="w-full rounded-md bg-white px-3 py-2 text-sm font-medium text-zinc-950 hover:bg-zinc-200 disabled:opacity-50"
        :disabled="busy"
      >
        Guardar cambios
      </button>

      <p v-if="localErr" class="text-sm text-rose-300">{{ localErr }}</p>
      <p v-if="msg" class="text-sm text-emerald-300">{{ msg }}</p>
    </form>
  </div>
</template>
