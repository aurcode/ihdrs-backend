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
                    :index="normalizeChildPath(route, child)"
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
                  :index="normalizePath(route)"
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
      <el-container :class="['main-container', { 'sidebar-collapsed': sidebarCollapsed }]">
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
  import { routes as constantRoutes } from '@/router'
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
  const isDark = useDark({
    initialValue: "light"
  })
  const toggleTheme = () => {
    isDark.value = !isDark.value
  }


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
    return constantRoutes
        .filter(route => !route.meta?.hideInMenu && hasPermission(route))
        .map(route => {
          const children = route.children?.filter(
              child => !child.meta?.hideInMenu && hasPermission(child)
          )
          return { ...route, children }
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

  // 确保菜单路径为绝对路径
  const normalizePath = (route) => {
    const path = route.children?.[0]?.path || route.path
    if (!path.startsWith('/')) {
      const parentPath = route.path === '/' ? '' : route.path
      return `${parentPath}/${path}`.replace(/\/+/g, '/')
    }
    return path
  }

  const normalizeChildPath = (parent, child) => {
    if (child.path.startsWith('/')) return child.path
    return `${parent.path}/${child.path}`.replace(/\/+/g, '/')
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

<style lang="scss">
  // 全局样式 - 折叠菜单居中
  .sidebar-container .el-menu--collapse {
    width: 64px !important;

    .el-menu-item,
    .el-sub-menu__title {
      padding: 0 !important;
      margin: 4px 8px !important;
      width: calc(100% - 16px) !important;
      min-width: auto !important;
      height: 56px !important;
      line-height: 56px !important;
      display: flex !important;
      justify-content: center !important;
      align-items: center !important;
      position: relative !important;
      text-align: center !important;
      box-sizing: border-box !important;

      & > * {
        margin: 0 !important;
        padding: 0 !important;
      }

      .el-icon {
        position: absolute !important;
        left: 50% !important;
        top: 50% !important;
        transform: translate(-50%, -50%) !important;
        margin: 0 !important;
        flex-shrink: 0;
      }

      span {
        display: none !important;
        visibility: hidden !important;
        width: 0 !important;
        height: 0 !important;
        overflow: hidden !important;
        opacity: 0 !important;
      }
    }

    .el-sub-menu {
      .el-sub-menu__title {
        padding: 0 !important;
        display: flex !important;
        justify-content: center !important;
        align-items: center !important;
        position: relative !important;
        text-align: center !important;

        .el-icon {
          position: absolute !important;
          left: 50% !important;
          top: 50% !important;
          transform: translate(-50%, -50%) !important;
          margin: 0 !important;
        }

        span {
          display: none !important;
          visibility: hidden !important;
        }
      }

      .el-menu {
        .el-menu-item {
          padding: 0 !important;
          margin: 4px 8px !important;
          display: flex !important;
          justify-content: center !important;
          align-items: center !important;
          position: relative !important;
          text-align: center !important;

          .el-icon {
            position: absolute !important;
            left: 50% !important;
            top: 50% !important;
            transform: translate(-50%, -50%) !important;
            margin: 0 !important;
          }

          span {
            display: none !important;
            visibility: hidden !important;
          }
        }
      }
    }
  }
</style>

<style lang="scss" scoped>
  .app-wrapper {
    height: 100vh;
    width: 100%;
  }

  .layout-container {
    height: 100%;
  }

  .sidebar-container {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    background: linear-gradient(180deg, #3b82f6 0%, #2563eb 30%, #1e40af 60%, #1e3a8a 100%);
    border-right: none;
    transition: width 0.3s;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    z-index: 1000;

    // 背景装饰圆形 - 使用蓝色系，类似登录界面
    &::before {
      content: '';
      position: absolute;
      width: 400px;
      height: 400px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(147, 197, 253, 0.25) 0%, rgba(96, 165, 250, 0.15) 50%, transparent 100%);
      top: -200px;
      right: -150px;
      z-index: 0;
      filter: blur(40px);
    }

    &::after {
      content: '';
      position: absolute;
      width: 350px;
      height: 350px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(191, 219, 254, 0.2) 0%, rgba(147, 197, 253, 0.1) 50%, transparent 100%);
      bottom: -150px;
      left: -100px;
      z-index: 0;
      filter: blur(40px);
    }

    .sidebar-header {
      position: relative;
      z-index: 1;
      height: 80px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-bottom: 1px solid rgba(255, 255, 255, 0.15);
      background: linear-gradient(180deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
      backdrop-filter: blur(20px);

      .logo {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        font-size: 24px;
        font-weight: bold;
        color: #ffffff;
        width: 100%;

        .logo-text {
          white-space: nowrap;
          text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        :deep(.el-icon) {
          color: #ffffff;
          filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
          flex-shrink: 0;
        }
      }
    }

    .sidebar-scrollbar {
      position: relative;
      z-index: 1;
      height: calc(100% - 80px);
      background: transparent;
    }

    .sidebar-menu {
      border: none;
      height: 100%;
      background: transparent !important;

      // 折叠状态下的样式 - 使用更具体的选择器
      :deep(.el-menu--collapse) {
        width: 64px !important;

        .el-menu-item,
        .el-sub-menu__title {
          padding: 0 !important;
          margin: 4px 8px !important;
          width: calc(100% - 16px) !important;
          min-width: auto !important;
          height: 56px !important;
          line-height: 56px !important;
          display: flex !important;
          justify-content: center !important;
          align-items: center !important;
          position: relative !important;
          text-align: center !important;
          box-sizing: border-box !important;

          // 清除所有内部元素的 margin 和 padding
          & > * {
            margin: 0 !important;
            padding: 0 !important;
          }

          // 图标绝对居中
          .el-icon {
            position: absolute !important;
            left: 50% !important;
            top: 50% !important;
            transform: translate(-50%, -50%) !important;
            margin: 0 !important;
            flex-shrink: 0;
            width: auto !important;
            height: auto !important;
          }

          // 隐藏文字
          span {
            display: none !important;
            visibility: hidden !important;
            width: 0 !important;
            height: 0 !important;
            overflow: hidden !important;
            opacity: 0 !important;
          }
        }

        .el-sub-menu {
          .el-sub-menu__title {
            padding: 0 !important;
            display: flex !important;
            justify-content: center !important;
            align-items: center !important;
            position: relative !important;
            text-align: center !important;

            .el-icon {
              position: absolute !important;
              left: 50% !important;
              top: 50% !important;
              transform: translate(-50%, -50%) !important;
              margin: 0 !important;
            }

            span {
              display: none !important;
              visibility: hidden !important;
            }
          }

          .el-menu {
            .el-menu-item {
              padding: 0 !important;
              margin: 4px 8px !important;
              display: flex !important;
              justify-content: center !important;
              align-items: center !important;
              position: relative !important;
              text-align: center !important;

              .el-icon {
                position: absolute !important;
                left: 50% !important;
                top: 50% !important;
                transform: translate(-50%, -50%) !important;
                margin: 0 !important;
              }

              span {
                display: none !important;
                visibility: hidden !important;
              }
            }
          }
        }
      }

      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        height: 56px;
        line-height: 56px;
        color: rgba(255, 255, 255, 0.9);
        margin: 4px 12px;
        border-radius: 12px;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        justify-content: flex-start;
        padding-left: 20px !important;
        box-sizing: border-box;

        &:hover {
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.15) 100%) !important;
          color: #ffffff;
          transform: translateX(2px);
        }

        &.is-active {
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0.25) 100%) !important;
          color: #ffffff;
          border-right: none;
          box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.3);
          font-weight: 600;
        }

        .el-icon {
          color: inherit;
          margin-right: 12px;
          flex-shrink: 0;
          width: 20px;
          text-align: center;
        }

        span {
          flex: 1;
        }
      }

      :deep(.el-sub-menu) {
        .el-sub-menu__title {
          color: rgba(255, 255, 255, 0.9);
        }

        &.is-opened .el-sub-menu__title {
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0.1) 100%);
          color: #ffffff;
        }

        .el-menu {
          background: rgba(0, 0, 0, 0.1) !important;

          .el-menu-item {
            padding-left: 56px !important;
            color: rgba(255, 255, 255, 0.8);

            &:hover {
              background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.15) 100%) !important;
              color: #ffffff;
            }

            &.is-active {
              background: linear-gradient(135deg, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0.25) 100%) !important;
              color: #ffffff;
            }
          }
        }
      }
    }
  }

  .main-container {
    display: flex;
    flex-direction: column;
    min-height: 0;
    margin-left: 220px;
    transition: margin-left 0.3s;

    // 当侧边栏折叠时调整左边距
    &.sidebar-collapsed {
      margin-left: 64px;
    }
  }

  .navbar-container {
    position: relative;
    z-index: 10;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    height: 60px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);

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
    position: relative;
    padding: 0;
    background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
    background-attachment: fixed;
    overflow-x: hidden;
    overflow-y: auto;
    min-height: 100vh;
    width: 100%;
    box-sizing: border-box;

    // 动态背景装饰 - 使用登录界面的蓝色系
    &::before {
      content: '';
      position: fixed;
      width: 600px;
      height: 600px;
      border-radius: 50%;
      background: rgba(147, 197, 253, 0.2);
      top: -200px;
      right: -150px;
      z-index: 0;
      animation: float 20s ease-in-out infinite;
    }

    &::after {
      content: '';
      position: fixed;
      width: 500px;
      height: 500px;
      border-radius: 50%;
      background: rgba(191, 219, 254, 0.2);
      bottom: -150px;
      left: -100px;
      z-index: 0;
      animation: float 25s ease-in-out infinite reverse;
    }

    // 内容容器
    > * {
      position: relative;
      z-index: 1;
    }
  }

  @keyframes float {
    0%, 100% {
      transform: translate(0, 0) scale(1);
    }
    50% {
      transform: translate(30px, -30px) scale(1.1);
    }
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
      background: #000000;
    }
  }
</style>