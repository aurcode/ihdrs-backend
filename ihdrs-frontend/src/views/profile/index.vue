<template>
  <div class="page-profile">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>个人中心</h1>
    </div>

    <el-row :gutter="20">
      <!-- 基本信息 -->
      <el-col :xs="24" :sm="24" :md="16" :span="16">
        <el-card class="profile-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <i class="el-icon-user"></i>
                <span>基本信息</span>
              </div>
              <el-button
                  v-if="!editing"
                  class="edit-btn"
                  type="primary"
                  size="default"
                  @click="editing = true"
              >
                <i class="el-icon-edit"></i>
                编辑
              </el-button>
            </div>
          </template>

          <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="80px"
              :disabled="!editing"
              class="profile-form"
          >
            <!-- 用户名可编辑 -->
            <el-form-item label="用户名" prop="username">
              <el-input
                  v-model="form.username"
                  placeholder="请输入用户名"
                  class="left-align-input"
                  :class="{ 'disabled-input': !editing }"
              />
            </el-form-item>

            <el-form-item label="角色">
              <div class="role-tags">
                <template v-if="form.roleList && form.roleList.length">
                  <el-tag
                      v-for="r in form.roleList"
                      :key="r"
                      type="primary"
                      class="role-tag"
                  >
                    {{ r }}
                  </el-tag>
                </template>
                <span v-else class="empty-text">无角色</span>
              </div>
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input
                  v-model="form.email"
                  placeholder="name@example.com"
                  class="left-align-input"
                  clearable
              />
            </el-form-item>

            <el-form-item label="电话" prop="phone">
              <el-input
                  v-model="form.phone"
                  placeholder="请输入11位手机号"
                  class="left-align-input"
                  maxlength="11"
                  clearable
              />
            </el-form-item>

            <el-form-item v-if="editing" class="form-actions">
              <el-button type="primary" :loading="saving" @click="onSave">
                <i class="el-icon-check"></i>
                保存更改
              </el-button>
              <el-button @click="cancelEdit">
                <i class="el-icon-close"></i>
                取消
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 系统信息 -->
      <el-col :xs="24" :sm="24" :md="8" :span="8">
        <el-card class="system-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <i class="el-icon-monitor"></i>
                <span>系统信息</span>
              </div>
            </div>
          </template>

          <div class="info-vertical">
            <div class="info-item">
              <span class="info-label">账号状态：</span>
              <el-tag :type="form.status ? 'success' : 'danger'" class="status-tag">
                {{ form.status ? '正常' : '禁用' }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="info-label">登录次数：</span>
              <span class="info-value">{{ form.loginCount || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">上次登录：</span>
              <span class="info-value">{{ show(form.lastLoginTime) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">创建时间：</span>
              <span class="info-value">{{ show(form.createTime) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码 -->
    <el-row :gutter="20" style="margin-top: 40px;">
      <el-col :span="24">
        <el-card class="password-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <i class="el-icon-lock"></i>
                <span>修改密码</span>
              </div>
            </div>
          </template>

          <el-form
              ref="pwdRef"
              :model="pwd"
              :rules="pwdRules"
              label-width="100px"
              class="password-form"
          >
            <el-row :gutter="30">
              <el-col :xs="24" :sm="24" :md="14" :span="14">
                <el-form-item label="原密码" prop="oldPassword" class="password-item">
                  <el-input
                      v-model="pwd.oldPassword"
                      type="password"
                      show-password
                      placeholder="请输入当前密码"
                      class="left-align-input"
                  />
                </el-form-item>

                <el-form-item label="新密码" prop="newPassword" class="password-item">
                  <el-input
                      v-model="pwd.newPassword"
                      type="password"
                      show-password
                      placeholder="请输入新密码"
                      class="left-align-input"
                  />
                </el-form-item>

                <el-form-item label="确认新密码" prop="confirm" class="password-item">
                  <el-input
                      v-model="pwd.confirm"
                      type="password"
                      show-password
                      placeholder="请再次输入新密码"
                      class="left-align-input"
                  />
                </el-form-item>

                <el-form-item class="password-actions">
                  <el-button
                      type="warning"
                      :loading="pwdSaving"
                      @click="onChangePwd"
                      class="change-pwd-btn"
                  >
                    <i class="el-icon-refresh-right"></i>
                    修改密码
                  </el-button>
                </el-form-item>
              </el-col>

              <el-col :xs="24" :sm="24" :md="10" :span="10">
                <div class="password-tips">
                  <h4>密码安全提示：</h4>
                  <ul>
                    <li>密码长度至少6位</li>
                    <li>必须包含字母和数字</li>
                    <li>不要使用常见密码</li>
                    <li>定期更换密码</li>
                  </ul>
                </div>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMe, updateMe, changeMyPwd, checkUsernameExists } from '@/api/user'

defineOptions({
  name: 'ProfilePage'
})

const formRef = ref(null)
const pwdRef = ref(null)
const editing = ref(false)
const saving = ref(false)
const pwdSaving = ref(false)
const originalUsername = ref('')

const form = reactive({
  userId: '',
  username: '',
  roleList: [],
  email: '',
  phone: '',
  status: true,
  loginCount: 0,
  lastLoginTime: '',
  createTime: '',
  updateTime: ''
})

const pwd = reactive({
  oldPassword: '',
  newPassword: '',
  confirm: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '用户名只能包含字母、数字和下划线',
      trigger: 'blur'
    },
    // 用户名唯一性检查（异步验证）
    {
      validator: async (rule, value, callback) => {
        if (value === originalUsername.value) {
          // 用户名没改，不需要检查
          callback()
          return
        }

        // 如果为空，由其他规则处理
        if (!value) {
          callback()
          return
        }

        try {
          console.log('🔍 检查用户名:', value)

          // 调用接口
          const res = await checkUsernameExists(value)
          console.log('📦 检查结果:', res)

          const exists = res?.data !== undefined ? res.data : res

          if (exists === true) {
            callback(new Error('用户名已存在'))
          } else {
            callback()
          }
        } catch (e) {
          console.error('检查用户名失败:', e)
          // 网络错误时，不阻止用户提交
          callback()
          // 或者阻止提交：
          // callback(new Error('无法验证用户名，请稍后重试'))
        }
      },
      trigger: 'blur'
    }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的11位手机号',
      trigger: ['blur', 'change']
    }
  ]
}

const pwdRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/,
      message: '密码必须包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, v, cb) => {
        if (v === pwd.newPassword) {
          cb()
        } else {
          cb(new Error('两次输入不一致'))
        }
      },
      trigger: 'blur'
    }
  ]
}

const show = (value) => {
  if (!value) return '-'

  try {
    // 处理 ISO 8601 格式
    if (typeof value === 'string' && (value.includes('T') || value.includes('-'))) {
      const date = new Date(value)

      // 检查日期是否有效
      if (isNaN(date.getTime())) {
        return '-'
      }

      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).replace(/\//g, '-')
    }

    return value
  } catch (e) {
    console.error('时间格式化错误:', e)
    return '-'
  }
}

watch(() => pwd.newPassword, () => {
  if (pwdRef.value && pwd.confirm) {
    pwdRef.value.validateField('confirm').catch(() => {})
  }
})

/* ========== 加载用户信息 ========== */
async function load() {
  try {
    console.log('开始加载用户信息...')

    const res = await getMe()
    console.log('getMe 返回:', res)

    const d = res?.data ? res.data : (res || {})
    console.log('解析后数据:', d)

    form.userId = d.userId ?? d.id ?? ''
    form.username = d.username ?? ''
    originalUsername.value = d.username ?? ''

    // 角色处理
    if (Array.isArray(d.roles)) {
      form.roleList = d.roles
    } else if (d.role) {
      form.roleList = [d.role]
    } else {
      form.roleList = []
    }

    form.email = d.email ?? ''
    form.phone = d.phone ?? d.telephone ?? ''
    form.status = d.status ?? true
    form.loginCount = d.loginCount ?? 0
    form.lastLoginTime = d.lastLoginTime ?? ''
    form.createTime = d.createTime ?? ''
    form.updateTime = d.updateTime ?? ''

    console.log('加载完成:', form)
  } catch (e) {
    console.error('加载用户信息失败:', e)
    ElMessage.error('加载用户信息失败，请刷新页面重试')
  }
}

/* ========== 保存时包含用户名 ========== */
async function onSave() {
  try {
    if (formRef.value) {
      await formRef.value.validate()
    }

    saving.value = true

    console.log('保存数据:', {
      username: form.username,
      email: form.email,
      telephone: form.phone
    })

    // 发送更新请求（包含用户名）
    await updateMe({
      username: form.username, // 包含用户名
      email: form.email || null, // 空值传 null
      telephone: form.phone || null // 空值传 null
    })

    ElMessage.success('保存成功')
    editing.value = false
    originalUsername.value = form.username // 更新原始用户名
    await load()
  } catch (e) {
    console.error('保存失败:', e)
    if (e.errors) {
      return
    }
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function cancelEdit() {
  editing.value = false
  load()
}

/* ========== 修改密码时验证原密码 ========== */
async function onChangePwd() {
  try {
    if (pwdRef.value) {
      await pwdRef.value.validate()
    }

    pwdSaving.value = true

    console.log('🔐 修改密码...')

    await changeMyPwd({
      oldPassword: pwd.oldPassword,
      newPassword: pwd.newPassword
    })

    ElMessage.success('密码已更新')

    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''

    if (pwdRef.value) {
      pwdRef.value.clearValidate()
    }
  } catch (e) {
    console.error('修改密码失败:', e)

    // 区分原密码错误
    if (e?.response?.status === 400) {
      ElMessage.error('原密码不正确')
    } else if (e?.response?.status === 500) {
      ElMessage.error('密码修改失败，请稍后重试')
    } else if (e.errors) {
      return
    } else if (e?.response?.data?.message) {
      ElMessage.error(e.response.data.message)
    } else {
      ElMessage.error(e?.message || '修改失败')
    }
  } finally {
    pwdSaving.value = false
  }
}

onMounted(() => {
  console.log('组件已挂载')
  load()
})
</script>

<style scoped>
.page-profile {
  padding: 20px;
  background: var(--el-bg-color-page);
  min-height: calc(100vh - 60px);
}

.page-header {
  margin-bottom: 24px;
  padding: 0 8px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.profile-card,
.system-card,
.password-card {
  border-radius: 8px;
  border: none;
  background-color: var(--el-bg-color);
}

.profile-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color);
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.system-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color);
  padding: 16px 20px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.password-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color);
  padding: 16px 20px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  color: white;
  font-weight: 500;
}

.card-title i {
  margin-right: 8px;
  font-size: 16px;
}

.profile-form,
.password-form {
  padding: 8px 0;
}

.profile-form :deep(.el-form-item__label),
.password-form :deep(.el-form-item__label) {
  color: var(--el-text-color-regular);
}

/* 禁用状态 */
.disabled-input :deep(.el-input__inner) {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-placeholder);
  border-color: var(--el-border-color);
  cursor: not-allowed;
}

.left-align-input :deep(.el-input__inner) {
  text-align: left;
  padding-left: 15px;
}

.left-align-input :deep(.el-input__inner)::placeholder {
  text-align: left;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tag {
  border-radius: 12px;
  padding: 4px 12px;
}

.empty-text {
  color: var(--el-text-color-placeholder);
  font-size: 14px;
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed var(--el-border-color);
}

.password-actions {
  margin-top: 24px;
}

.change-pwd-btn {
  min-width: 120px;
}

.password-item {
  margin-bottom: 20px;
}

.info-vertical {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.info-label {
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.info-value {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.password-tips {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 20px;
  border-left: 4px solid #409eff;
  height: fit-content;
  margin-top: 10px;
}

.password-tips h4 {
  margin: 0 0 16px 0;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
}

.password-tips ul {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
}

.password-tips li {
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.4;
}

.el-row {
  display: flex;
  align-items: stretch;
}

.profile-card,
.system-card {
  height: 100%;
}

@media (max-width: 768px) {
  .page-profile {
    padding: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .password-tips {
    margin-top: 20px;
    border-left: none;
    border-top: 4px solid #409eff;
  }

  .password-item {
    margin-bottom: 16px;
  }
}
</style>