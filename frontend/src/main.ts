import { createApp } from 'vue'
import {
  ElButton,
  ElConfigProvider,
  ElDatePicker,
  ElDialog,
  ElDrawer,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElOption,
  ElPagination,
  ElProgress,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/main.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

const elementComponents = [
  ElButton,
  ElConfigProvider,
  ElDatePicker,
  ElDialog,
  ElDrawer,
  ElDropdown,
  ElDropdownItem,
  ElDropdownMenu,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElIcon,
  ElInput,
  ElInputNumber,
  ElOption,
  ElPagination,
  ElProgress,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
]

elementComponents.forEach(component => app.use(component))
app.use(router).mount('#app')
