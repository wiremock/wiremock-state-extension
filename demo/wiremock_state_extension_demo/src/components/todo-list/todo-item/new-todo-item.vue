<script lang="ts" setup>
import { ref } from 'vue'
import axios from 'axios'
import router from '@/router'

const title = ref<string>('')
const description = ref<string>('')

function submit() {
  axios
    .post('/api/v1/todolist', {
      title: title.value,
      description: description.value,
    })
    .catch((err) => {
      console.dir('posting item failed: ' + err)
    })
    .finally(() => backToTodoList())
}

function backToTodoList() {
  router.push({ name: 'Todo List' }).catch((error) => console.error(error))
}
</script>

<template>
  <form @submit.prevent="submit">
    <v-container fluid>
      <v-text-field v-model.trim="title" clearable label="Title" required></v-text-field>
      <v-textarea v-model.trim="description" clearable label="Description" required></v-textarea>
      <v-container class="d-flex flex-row-reverse pa-0" fluid>
        <v-btn class="ml-2" type="submit">Submit</v-btn>
        <v-btn-secondary class="ml-2" @click="backToTodoList()">Cancel</v-btn-secondary>
      </v-container>
    </v-container>
  </form>
</template>

<style scoped></style>