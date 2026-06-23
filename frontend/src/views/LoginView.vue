<template>
  <div class="login-container">
    <div class="login-card">
      <h2>智能待办助手</h2>
      <p class="subtitle">登录你的账号</p>
      
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
        
        <div class="link-row">
          <span>还没有账号？</span>
          <el-link type="primary" @click="goRegister">去注册</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const success = await userStore.login(form.username, form.password)
        if (success) {
          ElMessage.success('登录成功！')
          router.push('/tasks')
        } else {
          ElMessage.error('登录失败')
        }
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const goRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.login-card h2 {
  text-align: center;
  color: #333;
  margin-bottom: 8px;
}
.subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 30px;
}
.link-row {
  text-align: center;
  color: #999;
}
</style>