import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

const auth = useAuthStore()
auth.hydrateFromStorage()

async function bootstrap() {
  if (auth.token) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clear()
    }
  }
  app.mount('#app')
}

void bootstrap()
