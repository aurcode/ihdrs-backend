// layout/index.vue

<template>
  <div class="app-wrapper">
    <el-container class="layout-container">
      <!-- 侧边栏 -->
      <el-aside
          :width="sidebarCollapsed ? '64px' : '220px'"
          class="sidebar-container"
      >
        <div class="sidebar-header">
          <div class="logo">
            <el-icon size="32" color="#409EFF">
              <DataBoard />
            </el-icon>
            <transition name="fade">
              <span v-show="!sidebarCollapsed" class="logo-text">IHDRS</span>
            </transition>
          </div>
        </div>

        <el-scrollbar class="sidebar-scrollbar">
          <el-menu
              :default-active="activeMenu"
              :collapse="sidebarCollapsed"
              :unique-opened="false"
              class="sidebar-menu"
              router
          >
            <template v-for="route in menuRoutes" :key="route.path">
              <!-- 有子菜单的情况 -->
              <el-sub-menu
                  v-if="route.children && route.children.length > 1"
                  :index="route.path"
              >
                <template #title>
                  <el-icon>
                    <component :is="route.meta?.icon" />
                  </el-icon>
                  <span>{{ route.meta?.title }}</span>
                </template>

                <el-menu-item
                    v-for="child in route.children"
                    :key="child.path"
                    :index="child.path"
                >
                  <el-icon>
                    <component :is="child.meta?.icon" />
                  </el-icon>
                  <span>{{ child.meta?.title }}</span>
                </el-menu-item>
              </el-sub-menu>

              <!-- 单菜单的情况 -->
              <el-menu-item
                  v-else
                  :index="route.children?.[0]?.path || route.path"
              >
                <el-icon>
                  <component :is="route.meta?.icon || route.children?.[0]?.meta?.icon" />
                </el-icon>
                <span>{{ route.meta?.title || route.children?.[0]?.meta?.title }}</span>
              </el-menu-item>
            </template>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <!-- 主体区域 -->
      <el-container class="main-container">
        <!-- 顶部导航 -->
        <el-header class="navbar-container">
          <div class="navbar-left">
            <el-button
                text
                @click="toggleSidebar"
                class="sidebar-toggle"
            >
              <el-icon size="18">
                <Expand v-if="sidebarCollapsed" />
                <Fold v-else />
              </el-icon>
            </el-button>

            <el-breadcrumb class="app-breadcrumb" separator="/">
              <el-breadcrumb-item
                  v-for="item in breadcrumbItems"
                  :key="item.path"
                  :to="item.path"
              >
                {{ item.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="navbar-right">
            <!-- 全屏按钮 -->
            <el-tooltip content="全屏" placement="bottom">
              <el-button text @click="toggleFullscreen" class="navbar-btn">
                <el-icon size="18">
                  <FullScreen />
                </el-icon>
              </el-button>
            </el-tooltip>

            <!-- 主题切换 -->
            <el-tooltip content="主题" placement="bottom">
              <el-button text @click="toggleTheme" class="navbar-btn">
                <el-icon size="18">
                  <Sunny v-if="isDark" />
                  <Moon v-else />
                </el-icon>
              </el-button>
            </el-tooltip>

            <!-- 用户菜单 -->
            <el-dropdown trigger="click" @command="handleUserCommand">
              <div class="user-avatar">
                <el-avatar :size="36" :src="userStore.userInfo.avatar">
                  {{ userStore.username.charAt(0).toUpperCase() }}
                </el-avatar>
                <span class="username">{{ userStore.username }}</span>
                <el-icon class="dropdown-icon">
                  <ArrowDown />
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人资料
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>
                    系统设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容区域 -->
        <el-main class="app-main">
          <router-view v-slot="{ Component, route }">
            <transition name="fade-transform" mode="out-in">
              <keep-alive :include="cachedViews">
                <component :is="Component" :key="route.path" />
              </keep-alive>
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { useDark, useToggle, useFullscreen } from '@vueuse/core'
import {
  ArrowDown,
  DataBoard,
  Expand,
  Fold,
  FullScreen,
  Moon,
  Setting,
  Sunny,
  SwitchButton,
  User
} from "@element-plus/icons-vue";

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 响应式状态
const sidebarCollapsed = ref(false)
const cachedViews = ref([])

// 主题相关
const isDark = useDark()
const toggleTheme = useToggle(isDark)

// 全屏相关
const { isFullscreen, toggle: toggleFullscreen } = useFullscreen()

// 计算属性
const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

// 面包屑导航
const breadcrumbItems = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  const breadcrumbs = []

  matched.forEach(item => {
    breadcrumbs.push({
      path: item.path,
      title: item.meta.title
    })
  })

  return breadcrumbs
})

// 菜单路由过滤
const menuRoutes = computed(() => {
  const routes = router.getRoutes()
  return routes.filter(route => {
    return route.path === '/' || (
        route.meta &&
        route.meta.title &&
        !route.meta.hideInMenu &&
        hasPermission(route)
    )
  }).map(route => {
    if (route.children) {
      route.children = route.children.filter(child =>
          child.meta &&
          child.meta.title &&
          !child.meta.hideInMenu &&
          hasPermission(child)
      )
    }
    return route
  })
})

// 权限检查
const hasPermission = (route) => {
  if (!route.meta?.roles) return true
  return route.meta.roles.some(role => userStore.hasRole(role))
}

// 方法
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
        '确定要退出登录吗？',
        '退出确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )
    userStore.logout()
  } catch {
    // 用户取消操作
  }
}

// 监听路由变化
watch(route, (to) => {
  // 添加缓存页面
  if (to.meta.keepAlive) {
    const componentName = to.matched[to.matched.length - 1]?.components?.default?.name
    if (componentName && !cachedViews.value.includes(componentName)) {
      cachedViews.value.push(componentName)
    }
  }
})
</script>

<style lang="scss" scoped>
.app-wrapper {
  height: 100vh;
  width: 100%;
}

.layout-container {
  height: 100%;
}

.sidebar-container {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  transition: width 0.3s;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.05);

  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid #e4e7ed;

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 20px;
      font-weight: bold;
      color: #409EFF;

      .logo-text {
        white-space: nowrap;
      }
    }
  }

  .sidebar-scrollbar {
    height: calc(100% - 60px);
  }

  .sidebar-menu {
    border: none;
    height: 100%;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      height: 50px;
      line-height: 50px;

      &:hover {
        background-color: #f5f7fa;
      }

      &.is-active {
        background-color: #e6f7ff;
        color: #409EFF;
        border-right: 3px solid #409EFF;
      }
    }
  }
}

.main-container {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.navbar-container {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .navbar-left {
    display: flex;
    align-items: center;
    gap: 20px;

    .sidebar-toggle {
      font-size: 18px;
      color: #606266;

      &:hover {
        color: #409EFF;
      }
    }

    .app-breadcrumb {
      :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
        color: #909399;
      }
    }
  }

  .navbar-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .navbar-btn {
      color: #606266;

      &:hover {
        color: #409EFF;
      }
    }

    .user-avatar {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 8px;
      border-radius: 6px;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }

      .username {
        font-size: 14px;
        color: #606266;
      }

      .dropdown-icon {
        color: #909399;
        font-size: 12px;
      }
    }
  }
}

.app-main {
  padding: 20px;
  background: #f5f7fa;
  overflow: auto;
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

// 暗色主题
.dark {
  .sidebar-container {
    background: #001428;
    border-right-color: #303030;

    .sidebar-header {
      border-bottom-color: #303030;
    }

    .sidebar-menu {
      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        color: #bfbfbf;

        &:hover {
          background-color: #1f1f1f;
        }

        &.is-active {
          background-color: #1890ff;
          color: #fff;
        }
      }
    }
  }

  .navbar-container {
    background: #001428;
    border-bottom-color: #303030;

    .navbar-left,
    .navbar-right {
      .navbar-btn,
      .sidebar-toggle {
        color: #bfbfbf;

        &:hover {
          color: #1890ff;
        }
      }

      .user-avatar {
        &:hover {
          background-color: #1f1f1f;
        }

        .username {
          color: #bfbfbf;
        }
      }
    }
  }

  .app-main {
    background: #000b14;
  }
}
</style>