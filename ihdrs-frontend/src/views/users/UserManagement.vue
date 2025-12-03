<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, User, UserFilled, Avatar, Lock } from '@element-plus/icons-vue'
import { getUserList, updateUserRole, updateUserStatus, getUserLogs } from '@/api/admin'
import { getMe } from '@/api/user'

// 搜索表单
const searchForm = reactive({
  username: '',
  role: '',
  status: ''
})

// 搜索和筛选（保持兼容）
const searchQuery = ref('')
const filterRole = ref('')
const filterStatus = ref('')

// 表格数据
const loading = ref(false)
const userList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 当前登录用户ID（防止禁用自己）
const currentUserId = ref(null)

// 统计数据
const statistics = ref({
  total: 0,
  admin: 0,
  user: 0,
  active: 0
})

// 日志弹窗
const logDialogVisible = ref(false)
const logLoading = ref(false)
const userLogs = ref([])
const logCurrentPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const selectedUserId = ref(null)

// 详情弹窗
const detailDialogVisible = ref(false)
const selectedUser = ref(null)

// 获取用户列表
const fetchUserList = async () => {
  try {
    loading.value = true
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      username: searchQuery.value,
      role: filterRole.value,
      status: filterStatus.value
    }

    const response = await getUserList(params)
    userList.value = response.data.records
    total.value = response.data.total

    // 更新统计数据
    updateStatistics(response.data.records)

  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取当前登录用户信息
const fetchCurrentUser = async () => {
  try {
    const response = await getMe()
    currentUserId.value = response.data.userId || response.data.id
  } catch (error) {
    console.error('获取当前用户信息失败', error)
  }
}

// 更新统计数据（基于当前页数据，实际应该从后端获取总数）
const updateStatistics = (users) => {
  // 注意：这里只统计当前页的数据，实际应该从后端API获取总数
  // 如果后端API返回了总数，应该使用总数而不是当前页数据
  statistics.value.total = total.value || users.length
  statistics.value.admin = users.filter(u => u.role === 'ADMIN').length
  statistics.value.user = users.filter(u => u.role === 'USER').length
  statistics.value.active = users.filter(u => u.status === true).length
}

// 搜索
const handleSearch = () => {
  searchQuery.value = searchForm.username
  filterRole.value = searchForm.role
  filterStatus.value = searchForm.status
  currentPage.value = 1
  fetchUserList()
}

// 重置
const handleReset = () => {
  searchForm.username = ''
  searchForm.role = ''
  searchForm.status = ''
  searchQuery.value = ''
  filterRole.value = ''
  filterStatus.value = ''
  currentPage.value = 1
  fetchUserList()
}

// 角色变更
const handleRoleChange = async (user) => {
  try {
    await ElMessageBox.confirm(
        `确定要将 ${user.username} 的角色改为 ${user.role === 'ADMIN' ? '管理员' : '普通用户'} 吗？`,
        '角色变更',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    await updateUserRole(user.userId, user.role)
    ElMessage.success('角色更新成功')
    fetchUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('角色更新失败')
      fetchUserList() // 刷新列表恢复原状态
    }
  }
}

// 状态变更
const handleStatusChange = async (user) => {
  try {
    await ElMessageBox.confirm(
        `确定要${user.status ? '启用' : '禁用'} ${user.username} 吗？`,
        '状态变更',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )
    await updateUserStatus(user.userId, user.status)
    ElMessage.success('状态更新成功')
  } catch (error) {
    if (error === 'cancel') {
      user.status = !user.status
    } else {
      // API 请求出错或其他错误
      ElMessage.error('状态更新失败')
      fetchUserList() // 刷新列表恢复原状态
    }
  }
}

// 分页
const handlePageChange = (page) => {
  currentPage.value = page
  fetchUserList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchUserList()
}

// 查看用户日志
const viewUserLogs = async (user) => {
  selectedUserId.value = user.userId
  logDialogVisible.value = true
  logCurrentPage.value = 1
  await fetchUserLogs()
}

const fetchUserLogs = async () => {
  try {
    logLoading.value = true
    const response = await getUserLogs({
      userId: selectedUserId.value,
      page: logCurrentPage.value,
      size: logPageSize.value
    })
    userLogs.value = response.data.records
    logTotal.value = response.data.total
  } catch (error) {
    ElMessage.error('获取用户日志失败')
  } finally {
    logLoading.value = false
  }
}

const handleLogPageChange = (page) => {
  logCurrentPage.value = page
  fetchUserLogs()
}

// 查看用户详情
const viewUserDetail = (user) => {
  selectedUser.value = user
  detailDialogVisible.value = true
}

// 格式化时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 粒子效果样式
const getParticleStyle = (index) => {
  const size = Math.random() * 3 + 1
  const x = Math.random() * 100
  const y = Math.random() * 100
  const duration = Math.random() * 3 + 2
  const delay = Math.random() * 2

  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${x}%`,
    top: `${y}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
    opacity: Math.random() * 0.3 + 0.1
  }
}

// 初始化
onMounted(async () => {
  await fetchCurrentUser()
  await fetchUserList()
})
</script>

<template>
  <div class="user-management">
    <!-- 动态背景粒子效果 -->
    <div class="background-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
    </div>

    <!-- 背景装饰圆形 -->
    <div class="background-circles">
      <div class="circle circle-top"></div>
      <div class="circle circle-bottom"></div>
      <div class="circle circle-middle"></div>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <!-- 头部Logo区域 -->
      <div class="header-section">
        <div class="logo-container">
          <div class="logo-circle">
            <el-icon size="50" color="#2563eb">
              <UserFilled />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">用户管理</h1>
        <p class="header-subtitle">User Management</p>
      </div>

      <!-- 搜索和筛选区域 -->
      <el-card class="search-card modern-card" shadow="hover">
        <el-form :model="searchForm" inline>
          <el-form-item label="用户名">
            <el-input
                v-model="searchForm.username"
                placeholder="请输入用户名"
                clearable
                style="width: 300px"
            />
          </el-form-item>

          <el-form-item label="角色">
            <el-select v-model="searchForm.role" placeholder="请选择" clearable style="width: 150px">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通用户" value="USER" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 150px">
              <el-option label="正常" :value="true" />
              <el-option label="禁用" :value="false" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 数据统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card primary">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <UserFilled/>
                </el-icon>
              </div>
              <div class="stat-label">总用户数</div>
              <div class="stat-value">{{ statistics.total || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card warning">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <User/>
                </el-icon>
              </div>
              <div class="stat-label">管理员</div>
              <div class="stat-value">{{ statistics.admin || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Avatar/>
                </el-icon>
              </div>
              <div class="stat-label">普通用户</div>
              <div class="stat-value">{{ statistics.user || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card danger">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Lock/>
                </el-icon>
              </div>
              <div class="stat-label">活跃用户</div>
              <div class="stat-value">{{ statistics.active || 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 用户列表表格 -->
      <el-card class="table-card modern-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="title">用户列表</span>
          </div>
        </template>
        <el-table
            v-loading="loading"
            :data="userList"
            stripe
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa' }"
        >
          <el-table-column prop="userId" label="用户ID" width="80" />

          <el-table-column prop="username" label="用户名" width="150">
            <template #default="{ row }">
              <div class="username-cell">
                <span>{{ row.username }}</span>
                <el-tag v-if="row.role === 'ADMIN'" size="small" type="primary">
                  管理员
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="email" label="邮箱" width="200" />

          <el-table-column prop="phone" label="电话" width="130" />

          <el-table-column prop="role" label="角色" width="120">
            <template #default="{ row }">
              <el-select
                  v-model="row.role"
                  size="small"
                  @change="handleRoleChange(row)"
                  :disabled="row.userId === currentUserId"
              >
                <el-option label="普通用户" value="USER" />
                <el-option label="管理员" value="ADMIN" />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="150">
            <template #default="{ row }">
              <el-switch
                  v-model="row.status"
                  @change="handleStatusChange(row)"
                  :disabled="row.userId === currentUserId"
                  active-text="正常"
                  inactive-text="禁用"
              />
            </template>
          </el-table-column>

          <el-table-column prop="loginCount" label="登录次数" width="100" />

          <el-table-column prop="lastLoginTime" label="最后登录" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.lastLoginTime) }}
            </template>
          </el-table-column>

          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                  type="primary"
                  size="small"
                  link
                  @click="viewUserLogs(row)"
              >
                查看日志
              </el-button>
              <el-button
                  type="info"
                  size="small"
                  link
                  @click="viewUserDetail(row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 用户日志弹窗 -->
    <el-dialog
        v-model="logDialogVisible"
        title="用户行为日志"
        width="70%"
        :close-on-click-modal="false"
    >
      <el-table
          v-loading="logLoading"
          :data="userLogs"
          stripe
          max-height="400"
      >
        <el-table-column prop="action" label="操作" width="150" />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="userAgent" label="设备信息" min-width="200" />
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            v-model:current-page="logCurrentPage"
            v-model:page-size="logPageSize"
            :total="logTotal"
            layout="total, prev, pager, next"
            @current-change="handleLogPageChange"
        />
      </div>
    </el-dialog>

    <!-- 用户详情弹窗 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="用户详情"
        width="50%"
    >
      <div v-if="selectedUser" class="user-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">
            {{ selectedUser.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="用户名">
            {{ selectedUser.username }}
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="selectedUser.role === 'ADMIN' ? 'primary' : 'info'">
              {{ selectedUser.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedUser.status ? 'success' : 'danger'">
              {{ selectedUser.status ? '正常' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ selectedUser.email }}
          </el-descriptions-item>
          <el-descriptions-item label="电话">
            {{ selectedUser.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="登录次数">
            {{ selectedUser.loginCount }}
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">
            {{ formatDateTime(selectedUser.lastLoginTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDateTime(selectedUser.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.user-management {
  position: relative;
  min-height: 100vh;
  padding: 24px;
  padding-bottom: 60px;
  overflow: visible;
  width: 100%;
  box-sizing: border-box;

  // 背景粒子效果
  .background-particles {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;

    .particle {
      position: absolute;
      background: rgba(59, 130, 246, 0.3);
      border-radius: 50%;
      animation: float-particle infinite ease-in-out;
    }
  }

  // 背景装饰圆形
  .background-circles {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;
    overflow: hidden;

    .circle {
      position: absolute;
      border-radius: 50%;
      animation: float-circle 20s ease-in-out infinite;

      &.circle-top {
        width: 600px;
        height: 600px;
        background: rgba(147, 197, 253, 0.2);
        top: -200px;
        right: -150px;
      }

      &.circle-bottom {
        width: 500px;
        height: 500px;
        background: rgba(191, 219, 254, 0.2);
        bottom: -150px;
        left: -100px;
        animation-duration: 25s;
        animation-direction: reverse;
      }

      &.circle-middle {
        width: 400px;
        height: 400px;
        background: rgba(224, 242, 254, 0.3);
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        animation-duration: 30s;
      }
    }
  }

  // 内容区域
  .content-wrapper {
    position: relative;
    z-index: 1;
  }

  // 头部Logo区域
  .header-section {
    text-align: center;
    margin-bottom: 30px;
    padding-top: 10px;

    .logo-container {
      margin-bottom: 10px;

      .logo-circle {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.95);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
        border: 3px solid rgba(59, 130, 246, 0.2);
        transition: all 0.3s ease;

        &:hover {
          transform: scale(1.05);
          box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
        }

        .el-icon {
          filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
        }
      }
    }

    .header-title {
      font-size: 36px;
      font-weight: 800;
      color: #1e293b;
      margin: 0 0 10px 0;
      text-align: center;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      letter-spacing: 1px;
    }

    .header-subtitle {
      font-size: 18px;
      color: #475569;
      font-weight: 500;
      margin: 0;
      text-align: center;
      letter-spacing: 0.5px;
    }
  }

  // 深色模式适配
  html.dark & {
    .header-title {
      color: #ffffff;
      text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5), 0 0 20px rgba(255, 255, 255, 0.3);
    }

    .header-subtitle {
      color: #e2e8f0;
      text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
    }
  }

  // 通用卡片样式
  .modern-card {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px);
    border-radius: 28px;
    box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.5);
    transition: all 0.3s ease;
    overflow: visible;
    position: relative;
    z-index: 1;

    &:hover {
      transform: translateY(-6px);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
      background: rgba(255, 255, 255, 0.98);
    }
  }

  .search-card {
    margin-bottom: 24px;
    padding: 8px 12px;

    :deep(.el-form) {
      display: flex;
      align-items: center;
      flex-wrap: wrap;

      .el-form-item {
        margin-bottom: 0;
        margin-right: 20px;
      }
    }
  }

  .stats-row {
    margin-bottom: 24px;

    .stat-card {
      position: relative;
      height: 130px;
      border-radius: 24px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s ease;
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.3);
      backdrop-filter: blur(10px);

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);

        .stat-background {
          transform: scale(1.1);
        }
      }

      .stat-background {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        transition: transform 0.3s ease;
      }

      .stat-content {
        position: relative;
        z-index: 2;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 16px;
        height: 100%;
        color: white;

        .stat-icon {
          font-size: 32px;
          margin-bottom: 0;
          filter: brightness(1.8);
        }

        .stat-label {
          font-size: 14px;
          opacity: 1;
          font-weight: 400;
          margin-bottom: 6px;
          margin-top: -6px;
          text-align: center;
        }

        .stat-value {
          font-size: 26px;
          font-weight: 700;
          line-height: 1;
          text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        }
      }

      // 第2个和第4个卡片的图标更亮
      &.warning .stat-content .stat-icon,
      &.danger .stat-content .stat-icon {
        filter: brightness(2.2);
      }

      // 不同主题色
      &.primary .stat-background {
        background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
      }

      &.warning .stat-background {
        background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
      }

      &.success .stat-background {
        background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
      }

      &.danger .stat-background {
        background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
      }
    }
  }

  .table-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: 16px;
        font-weight: bold;
        color: #303133;
      }
    }
  }

  .username-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .user-detail {
    padding: 20px 0;
  }
}

// 动画效果
@keyframes float-particle {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(20px, -20px);
  }
}

@keyframes float-circle {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, -30px) scale(1.1);
  }
}

/* Element Plus 表格深色模式适配 */
html.dark :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(51, 65, 85, 0.5);
  --el-table-row-hover-bg-color: rgba(51, 65, 85, 0.4);
  --el-table-text-color: #e2e8f0;
  --el-table-header-text-color: #f1f5f9;
  --el-table-border-color: rgba(71, 85, 105, 0.3);
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table__header th) {
  color: #606266;
  font-weight: 600;
}

html.dark :deep(.el-table__header th) {
  color: #f1f5f9;
  background-color: rgba(51, 65, 85, 0.5);
}

html.dark :deep(.el-table__body td) {
  color: #e2e8f0;
  border-color: rgba(71, 85, 105, 0.3);
}

html.dark :deep(.el-table__empty-text) {
  color: #94a3b8;
}

html.dark :deep(.el-table__inner-wrapper::before) {
  background-color: rgba(71, 85, 105, 0.3);
}

:deep(.el-switch__label) {
  font-size: 12px;
}

html.dark :deep(.el-switch__label) {
  color: #e2e8f0;
}

:deep(.el-dialog__body) {
  padding: 20px 30px;
}

/* 深色模式：弹窗适配 */
html.dark :deep(.el-dialog) {
  --el-dialog-bg-color: rgba(30, 41, 59, 0.95);
  --el-text-color-primary: #e2e8f0;
  border: 1px solid rgba(100, 116, 139, 0.3);
  backdrop-filter: blur(10px);
}

html.dark :deep(.el-dialog__header) {
  border-bottom: 1px solid rgba(71, 85, 105, 0.3);
}

html.dark :deep(.el-dialog__title) {
  color: #f1f5f9;
}

/* 深色模式：输入框和选择器 */
html.dark :deep(.el-input__wrapper) {
  background-color: rgba(51, 65, 85, 0.5);
  box-shadow: 0 0 0 1px rgba(100, 116, 139, 0.3) inset;
}

html.dark :deep(.el-input__inner) {
  color: #e2e8f0;
}

html.dark :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

html.dark :deep(.el-select__wrapper) {
  background-color: rgba(51, 65, 85, 0.5);
  box-shadow: 0 0 0 1px rgba(100, 116, 139, 0.3) inset;
}

html.dark :deep(.el-select .el-input__inner) {
  color: #e2e8f0;
}

html.dark :deep(.el-select-dropdown) {
  background-color: rgba(30, 41, 59, 0.95);
  border: 1px solid rgba(100, 116, 139, 0.3);
  backdrop-filter: blur(10px);
}

html.dark :deep(.el-select-dropdown__item) {
  color: #e2e8f0;
}

html.dark :deep(.el-select-dropdown__item:hover) {
  background-color: rgba(51, 65, 85, 0.6);
}

html.dark :deep(.el-select-dropdown__item.is-selected) {
  color: #818cf8;
  font-weight: 600;
  background-color: rgba(129, 140, 248, 0.1);
}

/* 深色模式：按钮适配 */
html.dark :deep(.el-button--primary) {
  --el-button-bg-color: #667eea;
  --el-button-border-color: #667eea;
  --el-button-hover-bg-color: #818cf8;
  --el-button-hover-border-color: #818cf8;
}

html.dark :deep(.el-button.is-link) {
  color: #a5b4fc;
}

html.dark :deep(.el-button.is-link:hover) {
  color: #c7d2fe;
}

/* 深色模式：分页器适配 */
html.dark :deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #e2e8f0;
  --el-pagination-button-color: #e2e8f0;
  --el-pagination-button-bg-color: rgba(51, 65, 85, 0.5);
  --el-pagination-hover-color: #818cf8;
}

html.dark :deep(.el-pagination button) {
  background-color: rgba(51, 65, 85, 0.5);
}

html.dark :deep(.el-pagination button:disabled) {
  background-color: rgba(51, 65, 85, 0.3);
  color: #64748b;
}

html.dark :deep(.el-pager li) {
  background-color: rgba(51, 65, 85, 0.5);
  color: #e2e8f0;
}

html.dark :deep(.el-pager li:hover) {
  color: #818cf8;
  background-color: rgba(129, 140, 248, 0.1);
}

html.dark :deep(.el-pager li.is-active) {
  color: #fff;
  background-color: #667eea;
}

html.dark :deep(.el-pagination__sizes .el-select .el-input__wrapper) {
  background-color: rgba(51, 65, 85, 0.5);
}

/* 深色模式：描述列表适配 */
html.dark :deep(.el-descriptions__label) {
  color: #94a3b8;
}

html.dark :deep(.el-descriptions__content) {
  color: #e2e8f0;
}

html.dark :deep(.el-descriptions__table) {
  border-color: rgba(71, 85, 105, 0.3);
}

html.dark :deep(.el-descriptions__cell) {
  border-color: rgba(71, 85, 105, 0.3);
  background-color: transparent;
}

html.dark :deep(.el-descriptions__header) {
  background-color: rgba(51, 65, 85, 0.3);
}

/* 深色模式：标签适配 */
html.dark :deep(.el-tag) {
  border-color: rgba(100, 116, 139, 0.3);
  background-color: rgba(51, 65, 85, 0.5);
}

html.dark :deep(.el-tag--primary) {
  background-color: rgba(102, 126, 234, 0.8);
  border-color: #667eea;
  color: #fff;
}

html.dark :deep(.el-tag--info) {
  background-color: rgba(51, 65, 85, 0.6);
  border-color: rgba(100, 116, 139, 0.3);
  color: #e2e8f0;
}

html.dark :deep(.el-tag--success) {
  background-color: rgba(16, 185, 129, 0.8);
  border-color: #10b981;
  color: #fff;
}

html.dark :deep(.el-tag--danger) {
  background-color: rgba(239, 68, 68, 0.8);
  border-color: #ef4444;
  color: #fff;
}

/* 深色模式：消息框适配 */
html.dark :deep(.el-message-box) {
  background-color: rgba(30, 41, 59, 0.95);
  border: 1px solid rgba(100, 116, 139, 0.3);
  backdrop-filter: blur(10px);
}

html.dark :deep(.el-message-box__title) {
  color: #f1f5f9;
}

html.dark :deep(.el-message-box__content) {
  color: #e2e8f0;
}

html.dark :deep(.el-message-box__message) {
  color: #e2e8f0;
}

/* 深色模式：开关按钮 */
html.dark :deep(.el-switch.is-checked .el-switch__core) {
  background-color: #667eea;
}

html.dark :deep(.el-switch__core) {
  background-color: rgba(100, 116, 139, 0.5);
  border-color: rgba(100, 116, 139, 0.3);
}

:deep(.el-switch__label) {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
  &.is-active {
    color: #667eea !important;
    text-shadow: 0 0 4px rgba(0, 0, 0, 0.2);
  }
}
</style>