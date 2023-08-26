import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import '@mdi/font/css/materialdesignicons.css'
import { VBtn } from 'vuetify/components/VBtn'

export const light = {
  dark: false,
  colors: {
    background: '#FFFFFF',
    surface: '#FFFFFF',
    primary: '#0FB2EF',
    'primary-darken-1': '#005596',
    secondary: '#F59121',
    'secondary-darken-1': '#de5c17',
    error: '#B00020',
    info: '#0FB2EF',
    success: '#4CAF50',
    warning: '#F59121',
  },
}
export const dark = {
  dark: true,
  colors: {
    background: '#000000',
    surface: '#000000',
    primary: '#0FB2EF',
    'primary-darken-1': '#005596',
    secondary: '#F59121',
    'secondary-darken-1': '#de5c17',
    error: '#B00020',
    info: '#0FB2EF',
    success: '#4CAF50',
    warning: '#F59121',
  },
}

export const vuetify = createVuetify({
  aliases: {
    VBtnSecondary: VBtn,
    VBtnTertiary: VBtn,
  },
  defaults: {
    VBtn: {
      color: 'primary',
      variant: 'flat',
    },
    VBtnSecondary: {
      color: 'secondary',
      variant: 'outlined',
    },
    VBtnTertiary: {
      rounded: true,
      variant: 'plain',
    },
  },
  components,
  directives,
  theme: {
    themes: {
      light,
      dark,
    },
  },
})
