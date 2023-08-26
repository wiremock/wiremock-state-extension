<script lang="ts" setup>
import { RouterView } from 'vue-router'
import NavigationHeader from '@/components/navigation-header/navigation-header.vue'
import { provide } from 'vue'
import type { Emitter, EventType } from 'mitt'
import mitt from 'mitt'
import { eventEmitter } from '@/injects/event-emitter-inject'

const emitter = mitt()
provide<Emitter<Record<EventType, unknown>>>(eventEmitter, emitter)
</script>

<template>
  <v-app>
    <div>
      <navigation-header></navigation-header>
    </div>
    <v-main>
      <v-container fluid>
        <RouterView />
      </v-container>
    </v-main>
  </v-app>
</template>