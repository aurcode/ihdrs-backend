<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getUserList, updateUserRole, updateUserStatus, getUserLogs } from '@/api/admin'

// 搜索和筛选
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
      page: currentPage.value,
      size: pageSize.value,
      search: searchQuery.value,
      role: filterRole.value,
      status: filterStatus.value
    }

    const response = await getUserList(params)
    userList.value = response.data.records
    total.value = response.data.total

    // 获取当前用户ID（假设从localStorage获取）
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    currentUserId.value = userInfo.userId
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
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
    if (error !== 'cancel') {
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

// 初始化
onMounted(() => {
  fetchUserList()
})
</script>

<template>
  <div class="user-management">
    <div class="header">
      <h1>用户管理</h1>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-section">
      <el-input
          v-model="searchQuery"
          placeholder="搜索用户名、邮箱或电话"
          class="search-input"
          clearable
          @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select
          v-model="filterRole"
          placeholder="筛选角色"
          clearable
          class="filter-select"
          @change="handleSearch"
      >
        <el-option label="全部角色" value="" />
        <el-option label="管理员" value="ADMIN" />
        <el-option label="普通用户" value="USER" />
      </el-select>

      <el-select
          v-model="filterStatus"
          placeholder="筛选状态"
          clearable
          class="filter-select"
          @change="handleSearch"
      >
        <el-option label="全部状态" value="" />
        <el-option label="正常" :value="true" />
        <el-option label="禁用" :value="false" />
      </el-select>

      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <!-- 用户列表表格 -->
    <div class="table-section">
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

<style scoped>
.user-management {
  padding: 20px;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* 深色模式：保持渐变背景，但调暗 */
html.dark .user-management {
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 100%);
}

.header {
  margin-bottom: 24px;
  padding: 0 4px;
}

.header h1 {
  color: white;
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.filter-section {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  display: flex;
  gap: 15px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

/* 深色模式：搜索筛选区 - 半透明深色 */
html.dark .filter-section {
  background: rgba(30, 41, 59, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(100, 116, 139, 0.3);
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.filter-select {
  width: 150px;
}

.table-section {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

/* 深色模式：表格区域 - 半透明深色 */
html.dark .table-section {
  background: rgba(30, 41, 59, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(100, 116, 139, 0.3);
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
</style>