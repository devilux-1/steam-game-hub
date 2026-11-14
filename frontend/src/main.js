// 应用入口：注册路由、Pinia 与 Element Plus，站点固定使用浅色皮肤。
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './styles/main.css'
import App from './App.vue'
import router from './router'

// Element Plus 默认即浅色皮肤，主色与面板色由 main.css 第 11 节统一覆盖。
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
