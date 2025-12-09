<script setup>
import ThemeSelect from '@/components/ThemeSelect.vue'
import { gloableStore } from '@/stores/gloableStore'
import { useUserStore } from '@/stores/userStore'

const store = gloableStore()
const userStore = useUserStore()
</script>

<template>
  <header class="sticky top-0 z-50 bg-base-100/70 backdrop-blur-md shadow-md transition-all duration-300">
    <div class="navbar container mx-auto px-4">
      <div class="navbar-start">
        <div class="dropdown">
          <div tabindex="0" role="button" class="btn btn-ghost lg:hidden">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
              stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h8m-8 6h16" />
            </svg>
          </div>
          <ul tabindex="0" class="menu menu-sm dropdown-content mt-3 z-[1] p-2 shadow bg-base-100 rounded-box w-52">
            <li>
              <RouterLink to="/">首页</RouterLink>
            </li>
            <li>
              <RouterLink to="/about">关于</RouterLink>
            </li>
          </ul>
        </div>
        <RouterLink to="/" class="btn btn-ghost text-xl h-auto py-2 gap-2">
          <img src="@/assets/logo.png" alt="Tagilla 社区 Logo" class="h-8 w-auto transition-transform hover:scale-110" />
          <span class="font-bold bg-clip-text text-transparent bg-gradient-to-r from-primary to-secondary">Tagilla
            社区</span>
        </RouterLink>
      </div>
      <div class="navbar-center hidden lg:flex">
        <ul class="menu menu-horizontal px-1 gap-2">
          <li>
            <RouterLink to="/" active-class="active font-bold text-primary">首页</RouterLink>
          </li>
          <li>
            <RouterLink to="/about" active-class="active font-bold text-primary">关于</RouterLink>
          </li>
        </ul>
      </div>
      <div class="navbar-end gap-2">
        <button class="btn btn-ghost btn-circle hover:text-primary transition-colors" v-if="!store.isMusicPlayerVisible"
          @click="store.setMusicPlayerVisibility(true)" title="显示播放器">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
          </svg>
        </button>
        <template v-if="!userStore.userInfo">
          <RouterLink to="/login" class="btn btn-primary btn-sm">登录</RouterLink>
        </template>
        <template v-else>
          <div class="dropdown dropdown-end">
            <div tabindex="0" role="button" class="btn btn-ghost btn-circle avatar">
              <div class="w-10 rounded-full">
                <img
                  :src="userStore.userInfo.avatar || 'https://daisyui.com/images/stock/photo-1534528741775-53994a69daeb.jpg'" />
              </div>
            </div>
            <ul tabindex="0" class="menu menu-sm dropdown-content mt-3 z-[1] p-2 shadow bg-base-100 rounded-box w-52">
              <li>
                <RouterLink to="/user" class="justify-between">
                  个人中心
                  <span class="badge" v-if="userStore.userInfo.role === 'VIP'">VIP</span>
                </RouterLink>
              </li>
              <li><a @click="userStore.logout(); $router.push('/login')">退出登录</a></li>
            </ul>
          </div>
        </template>
        <ThemeSelect />
      </div>
    </div>
  </header>
</template>
