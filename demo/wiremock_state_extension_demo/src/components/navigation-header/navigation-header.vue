<script lang="ts" setup>
import { inject, onMounted, ref } from 'vue'
import axios from 'axios'
import { eventEmitter } from '@/injects/event-emitter-inject'
import type { Emitter, EventType } from 'mitt'

const hasImage = ref(false)
const initials = ref('')
const image = ref()

const emitter = inject<Emitter<Record<EventType, unknown>>>(eventEmitter)!!

onMounted(() => {
  loadAvatar()
  emitter.on('profileUpdated', () => loadAvatar())
})

function loadAvatar() {
  axios
    .get('/api/v1/profile/image', {
      responseType: 'text',
    })
    .then((response) => {
      image.value = response.data
      hasImage.value = true
    })
    .catch(() => loadInitials())
}

function loadInitials() {
  axios
    .get('/api/v1/profile')
    .then((response) => {
      initials.value =
        response.data.firstName.charAt(0).toUpperCase() +
        response.data.familyName.charAt(0).toUpperCase()
    })
    .catch((err) => {
      console.log('Fetching profile failed: ' + err)
      initials.value = ''
    })
}
</script>

<template>
  <v-app-bar app title="WireMock State Extension Demo">
    <template v-slot:prepend>
      <v-img :width="125" alt="WireMock Logo" src="/wm.svg"></v-img>
    </template>
    <template v-slot:append>
      <v-avatar color="primary" size="48" @click="$router.push('/profile')">
        <template v-if="image !== null">
          <v-img :src="image" alt="profile image preview"></v-img>
        </template>
        <template v-else>
          {{ initials }}
        </template>
      </v-avatar>
    </template>
  </v-app-bar>
</template>

<style scoped></style>