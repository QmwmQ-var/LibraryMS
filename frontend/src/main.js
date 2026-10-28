import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

import App from './App.vue'
import router from './router'
import http from './api/http'
import { installMock } from './mock'

import 'element-plus/dist/index.css'
import './assets/styles/index.css'

dayjs.locale('zh-cn')

// 本地演示默认接管 /api 请求；接入真实后端时将 VITE_USE_MOCK 设为 false。
if (import.meta.env.VITE_USE_MOCK === 'true') {
  installMock(http)
}

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.mount('#app')
