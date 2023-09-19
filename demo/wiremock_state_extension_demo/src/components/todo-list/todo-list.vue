<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import TodoItem from '@/components/todo-list/todo-item/todo-item.vue'
import axios from 'axios'

export interface TodoItemResponse {
  id: number
  title: string
  description: string
}

const listResponse = ref<Array<TodoItemResponse>>([])

onMounted(() => {
  refresh()
})

function refresh() {
  axios
    .get('/api/v1/todolist')
    .then((response) => {
      listResponse.value = response.data
    })
    .catch((err) => {
      console.log('Fetching TodoList failed: ' + err)
      listResponse.value = []
    })
}
</script>

<template>
  <h1>ToDo list</h1>
  <v-container class="d-flex flex-row-reverse pa-0" fluid>
    <v-btn prepend-icon="mdi-plus-thick" @click="$router.push('/todo-items/new')">new item</v-btn>
  </v-container>

  <v-table class="w-100" hover>
    <thead>
      <tr>
        <th class="text-left">#</th>
        <th class="text-left">title</th>
        <th></th>
      </tr>
    </thead>
    <template v-if="listResponse.length == 0">
      <v-container fluid>
        <div class="align-self-center">No items found</div>
      </v-container>
    </template>
    <template v-else>
      <tbody>
        <template v-for="item in listResponse" :key="item.id">
          <TodoItem
            :id="item.id"
            :description="item.description"
            :title="item.title"
            @delete-event="refresh"
          ></TodoItem>
        </template>
      </tbody>
    </template>
  </v-table>
</template>

<style scoped></style>