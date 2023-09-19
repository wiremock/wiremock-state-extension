<script lang="ts" setup>
import router from '@/router'
import { inject, onMounted, ref } from 'vue'
import axios from 'axios'
import type { Emitter, EventType } from 'mitt'
import { eventEmitter } from '@/injects/event-emitter-inject'
import parseDataURL from 'data-urls'

const firstName = ref<string>('')
const familyName = ref<string>('')
const street = ref<string>('')
const city = ref<string>('')
const chosenFile = ref<File[]>()
const fileUpdated = ref<boolean>(false)
const image = ref<string | null>(null)
const emitter = inject<Emitter<Record<EventType, unknown>>>(eventEmitter)!!

function getProfileImage() {
  axios
    .get('/api/v1/profile/image', {
      responseType: 'text',
    })
    .then((response) => {
      updateProfileImage(response.data)
    })
    .catch((err) => {
      console.dir('fetching profile image failed: ' + err)
    })
}

function getProfileData() {
  axios
    .get('/api/v1/profile')
    .then((response) => {
      firstName.value = response.data.firstName
      familyName.value = response.data.familyName
      street.value = response.data.street
      city.value = response.data.city
    })
    .catch((err) => {
      console.dir('fetching profile failed: ' + err)
    })
}

onMounted(() => {
  getProfileData()
  getProfileImage()
})

function backToTodoList() {
  router.push({ name: 'Todo List' }).catch((error) => console.error(error))
}

function submit() {
  const requestObject = {
    firstName: firstName.value,
    familyName: familyName.value,
    street: street.value,
    city: city.value,
  }
  if (fileUpdated.value) {
    if (image.value === null) {
      axios
        .delete('/api/v1/profile/image')
        .finally(() => emitter.emit('profileUpdated', requestObject))
    } else {
      const url = parseDataURL(image.value!!)
      axios
        .post('/api/v1/profile/image', image.value!!, {
          headers: {
            'Content-Type': url?.mimeType.essence,
          },
        })
        .finally(() => emitter.emit('profileUpdated', requestObject))
    }
  }
  axios
    .post('/api/v1/profile', requestObject)
    .catch((err) => {
      console.dir('posting profile failed: ' + err)
    })
    .finally(() => backToTodoList())
}

function updateProfileImage(imageValue: string) {
  image.value = imageValue
  fileUpdated.value = true
}

function previewImage() {
  if (chosenFile.value != undefined && chosenFile.value?.length > 0) {
    const reader = new FileReader()
    reader.onload = (e) => {
      if (e.target !== null) {
        updateProfileImage(e.target.result as string)
      }
    }
    reader.readAsDataURL(chosenFile.value[0])
  }
}

function clearImage() {
  image.value = null
  fileUpdated.value = true
}
</script>

<template>
  <form @submit.prevent="submit">
    <v-container fluid>
      <v-row>
        <v-col class="d-flex flex-column align-center">
          <div class="flex-grow-1">
            <template v-if="image === null">
              <v-avatar color="grey" size="120"></v-avatar>
            </template>
            <template v-else>
              <v-avatar size="120">
                <v-img :src="image" alt="profile image preview"></v-img>
              </v-avatar>
            </template>
          </div>
          <v-btn-secondary class="w-25 flex-grow-0 ma-6" @click="clearImage()">
            clear image
          </v-btn-secondary>
          <v-file-input
            v-model="chosenFile"
            class="w-50 flex-grow-0"
            label="Upload profile picture"
            prepend-icon="mdi-account"
            @change="previewImage()"
            @click:clear="clearImage()"
          ></v-file-input>
        </v-col>
        <v-col>
          <v-row>
            <v-text-field
              v-model.trim="firstName"
              clearable
              label="Firstname"
              required
            ></v-text-field>
          </v-row>
          <v-row>
            <v-text-field
              v-model.trim="familyName"
              clearable
              label="Familyname"
              required
            ></v-text-field>
          </v-row>
          <v-row>
            <v-text-field v-model.trim="street" clearable label="Street" required></v-text-field>
          </v-row>
          <v-row>
            <v-text-field v-model.trim="city" clearable label="City" required></v-text-field>
          </v-row>
        </v-col>
      </v-row>
      <v-row class="d-flex flex-row-reverse pa-0" fluid>
        <v-btn class="ml-2" type="submit">Submit</v-btn>
        <v-btn-secondary class="ml-2" @click="backToTodoList()">Cancel</v-btn-secondary>
      </v-row>
    </v-container>
  </form>
</template>
<style scoped></style>