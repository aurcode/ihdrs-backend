// views/auth/Register.vue

<template>
  <div class="register-container">
    <div class="register-wrapper">
      <!-- 左侧背景 -->
      <div class="register-bg">
        <div class="bg-content">
          <h2 class="bg-title">加入我们</h2>
          <p class="bg-subtitle">体验智能手写数字识别的强大功能</p>
          <div class="bg-features">
            <div class="feature-item">
              <el-icon size="20">
                <UserFilled/>
              </el-icon>
              <span>用户友好</span>
            </div>
            <div class="feature-item">
              <el-icon size="20">
                <Cpu/>
              </el-icon>
              <span>安全可靠</span>
            </div>
            <div class="feature-item">
              <el-icon size="20">
                <Star/>
              </el-icon>
              <span>功能强大</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧注册表单 -->
      <div class="register-form-wrapper">
        <div class="register-form">
          <div class="form-header">
            <h3 class="form-title">创建账号</h3>
            <p class="form-subtitle">填写信息完成注册</p>
          </div>

          <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              size="large"
          >
            <el-form-item prop="username">
              <el-input
                  v-model="registerForm.username"
                  placeholder="请输入用户名"
                  prefix-icon="User"
                  clearable
              />
            </el-form-item>

            <el-form-item prop="email">
              <el-input
                  v-model="registerForm.email"
                  placeholder="请输入邮箱（可选）"
                  prefix-icon="Message"
                  clearable
              />
            </el-form-item>

            <el-form-item prop="phone">
              <el-input
                  v-model="registerForm.phone"
                  placeholder="请输入手机号（可选）"
                  prefix-icon="Phone"
                  clearable
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请输入密码"
                  prefix-icon="Lock"
                  show-password
                  clearable
              />
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请确认密码"
                  prefix-icon="Lock"
                  show-password
                  clearable
              />
            </el-form-item>

            <el-form-item prop="agreement">
              <el-checkbox v-model="registerForm.agreement">
                我已阅读并同意
                <el-link type="primary">《用户协议》</el-link>
                和
                <el-link type="primary">《隐私政策》</el-link>
              </el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  @click="handleRegister"
                  class="register-btn"
              >
                {{ loading ? '注册中...' : '注册' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>已有账号？</span>
            <el-link type="primary" @click="$router.push('/login')">
              立即登录
            </el-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import {useUserStore} from '@/stores/user'
import {Cpu, Star, UserFilled} from "@element-plus/icons-vue";

const userStore = useUserStore()
const registerFormRef = ref()
const loading = ref(false)

// 表单数据
const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  agreement: false
})

// 验证确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

// 验证协议
const validateAgreement = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请阅读并同意用户协议'))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules = reactive({
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, max: 50, message: '用户名长度为3-50个字符', trigger: 'blur'},
    {pattern: /^[a-zA-Z0-9_]{3,50}$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur'}
  ],
  email: [
    {type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur'}
  ],
  phone: [
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur'}
  ],
  confirmPassword: [
    {required: true, message: '请确认密码', trigger: 'blur'},
    {validator: validateConfirmPassword, trigger: 'blur'}
  ],
  agreement: [
    {validator: validateAgreement, trigger: 'change'}
  ]
})

// 注册处理
const handleRegister = async () => {
  try {
    await registerFormRef.value.validate()

    loading.value = true

    const registerData = {
      username: registerForm.username,
      password: registerForm.password,
      email: registerForm.email || undefined,
      phone: registerForm.phone || undefined
    }

    await userStore.register(registerData)

  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-container {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-wrapper {
  display: flex;
  width: 100%;
  max-width: 1000px;
  min-height: 650px;
  background: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.register-bg {
  flex: 1;
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
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

.register-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.register-form {
  width: 100%;
  max-width: 400px;

  .form-header {
    text-align: center;
    margin-bottom: 30px;

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

  .register-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    border-radius: 8px;
    background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
    border: none;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(118, 75, 162, 0.4);
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