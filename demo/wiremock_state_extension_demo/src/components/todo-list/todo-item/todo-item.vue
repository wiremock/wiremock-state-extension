<script lang="ts" setup>
import { ref } from 'vue'
import axios from 'axios'

const props = defineProps({
  id: Number,
  title: String,
  description: String,
})

const emit = defineEmits(['deleteEvent'])

const expand = ref(false)

function toggleExpand() {
  expand.value = !expand.value
}

function deleteItem() {
  axios.delete(`/api/v1/todolist/${props.id}`).finally(() => emit('deleteEvent'))
}
</script>

<template>
  <tr>
    <td @click="toggleExpand()">{{ id }}</td>
    <td @click="toggleExpand()">{{ title }}</td>
    <td class="text-right">
      <v-btn
        class="foreground"
        icon="mdi-delete-outline"
        variant="text"
        @click="deleteItem"
      ></v-btn>
    </td>
  </tr>
  <tr v-if="expand">
    <td colspan="3" @click="toggleExpand()">
      <v-container fluid>{{ description }}</v-container>
    </td>
  </tr>
</template>

<style scoped></style>