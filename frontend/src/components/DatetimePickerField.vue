<script setup lang="ts">
import { VueDatePicker } from '@vuepic/vue-datepicker'
import { es } from 'date-fns/locale'
import { computed } from 'vue'
import { formatDatetimeLocal, parseDatetimeLocal } from '../lib/datetimeLocal'
import '@vuepic/vue-datepicker/dist/main.css'

const model = defineModel<string>({ default: '' })

const props = defineProps<{
  label: string
  placeholder?: string
  /** Límite inferior (datetime-local o Date). */
  minLocal?: string
}>()

const minDate = computed(() => {
  if (!props.minLocal) return undefined
  return parseDatetimeLocal(props.minLocal) ?? undefined
})

const pickerModel = computed({
  get: () => parseDatetimeLocal(model.value),
  set: (v: Date | null) => {
    model.value = v ? formatDatetimeLocal(v) : ''
  },
})
</script>

<template>
  <div class="lb-dp-field">
    <label class="block text-xs text-zinc-500">{{ label }}</label>
    <VueDatePicker
      v-model="pickerModel"
      :min-date="minDate"
      :locale="es"
      enable-time-picker
      time-picker-inline
      auto-apply
      dark
      format="dd/MM/yyyy HH:mm"
      preview-format="dd/MM/yyyy HH:mm"
      :placeholder="placeholder ?? 'Toca para abrir calendario'"
      teleport
      :clearable="false"
      class="mt-1"
    />
  </div>
</template>

<style scoped>
.lb-dp-field :deep(.dp__input_wrap) {
  width: 100%;
}
.lb-dp-field :deep(.dp__input) {
  width: 100%;
  border-radius: 0.375rem;
  border: 1px solid rgb(39 39 42);
  background-color: rgb(9 9 11);
  color: rgb(244 244 245);
  font-size: 0.875rem;
  line-height: 1.25rem;
  padding: 0.5rem 0.75rem 0.5rem 2.25rem;
}
.lb-dp-field :deep(.dp__input::placeholder) {
  color: rgb(113 113 122);
}
.lb-dp-field :deep(.dp__input:hover) {
  border-color: rgb(63 63 70);
}
.lb-dp-field :deep(.dp__input:focus) {
  border-color: rgb(113 113 122);
  outline: none;
}
.lb-dp-field :deep(.dp__menu) {
  border: 1px solid rgb(39 39 42);
  box-shadow: 0 10px 40px rgb(0 0 0 / 0.45);
}
</style>
