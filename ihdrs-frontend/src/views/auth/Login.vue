// views/auth/Login.vue

<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 左侧背景 -->
      <div class="login-bg">
        <div class="bg-content">
          <h2 class="bg-title">智能手写数字识别系统</h2>
          <p class="bg-subtitle">IHDRS - Intelligent Handwritten Digit Recognition System</p>
          <div class="bg-features">
            <div class="feature-item">
              <el-icon size="24">
                <View/>
              </el-icon>
              <span>高精度识别</span>
            </div>
            <div class="feature-item">
              <el-icon size="24">
                <Operation/>
              </el-icon>
              <span>智能训练</span>
            </div>
            <div class="feature-item">
              <el-icon size="24">
                <DataAnalysis/>
              </el-icon>
              <span>实时统计</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-wrapper">
        <div class="login-form">
          <div class="form-header">
            <h3 class="form-title">欢迎登录</h3>
            <p class="form-subtitle">管理员控制台</p>
          </div>

          <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              size="large"
              @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  prefix-icon="User"
                  clearable
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  prefix-icon="Lock"
                  show-password
                  clearable
              />
            </el-form-item>

            <el-form-item>
              <div class="form-options">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  @click="handleLogin"
                  class="login-btn"
              >
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>还没有账号？</span>
            <el-link type="primary" @click="$router.push('/register')">
              立即注册
            </el-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {useUserStore} from '@/stores/user'
import {DataAnalysis, Operation, View} from "@element-plus/icons-vue";

const userStore = useUserStore()
const loginFormRef = ref()
const loading = ref(false)
const rememberMe = ref(false)

// 表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules = reactive({
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6个字符', trigger: 'blur'}
  ]
})

// 登录处理
const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()

    loading.value = true

    await userStore.login(loginForm)

    // 如果选择记住我，保存用户名
    if (rememberMe.value) {
      localStorage.setItem('ihdrs_remember_username', loginForm.username)
    } else {
      localStorage.removeItem('ihdrs_remember_username')
    }

  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

// 初始化记住的用户名
const initRememberData = () => {
  const rememberedUsername = localStorage.getItem('ihdrs_remember_username')
  if (rememberedUsername) {
    loginForm.username = rememberedUsername
    rememberMe.value = true
  }
}

// 组件挂载时初始化
onMounted(() => {
  initRememberData()
})
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-wrapper {
  display: flex;
  width: 100%;
  max-width: 1000px;
  height: 600px;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.login-bg {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="0.5"/></pattern></defs><rect width="100" height="100" fill="url(%23grid)"/></svg>');
    animation: float 20s ease-in-out infinite;
  }

  .bg-content {
    position: relative;
    z-index: 1;
    text-align: center;
    padding: 40px;
  }

  .bg-title {
    font-size: 32px;
    font-weight: bold;
    margin-bottom: 16px;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  }

  .bg-subtitle {
    font-size: 16px;
    opacity: 0.9;
    margin-bottom: 40px;
  }

  .bg-features {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .feature-item {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 16px;
    opacity: 0.9;
  }
}

.login-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-form {
  width: 100%;
  max-width: 400px;

  .form-header {
    text-align: center;
    margin-bottom: 40px;

    .form-title {
      font-size: 28px;
      font-weight: bold;
      color: #2c3e50;
      margin-bottom: 8px;
    }

    .form-subtitle {
      color: #7f8c8d;
      font-size: 14px;
    }
  }

  .form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
  }

  .login-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    border-radius: 8px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
    }
  }

  .form-footer {
    text-align: center;
    margin-top: 24px;
    color: #7f8c8d;
    font-size: 14px;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

</style>