<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const loading = ref(false)

const handleLogin = async () => {
    if (!username.value || !password.value) {
        errorMessage.value = '请输入用户名和密码'
        return
    }

    loading.value = true
    errorMessage.value = ''

    try {
        const response = await fetch('http://localhost:8080/user/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username.value,
                password: password.value
            })
        })

        const result = await response.json()

        if (result.code === 200) {
            userStore.setUserInfo(result.data)
            router.push('/user')
        } else {
            errorMessage.value = result.message || '登录失败'
        }
    } catch (error) {
        errorMessage.value = '网络错误，请稍后重试'
        console.error(error)
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="min-h-screen flex items-center justify-center pt-16">
        <div class="card w-96 bg-base-100 shadow-xl border border-base-200 backdrop-blur-sm bg-opacity-80">
            <div class="card-body">
                <h2 class="card-title justify-center text-2xl font-bold mb-4">登录</h2>

                <div class="form-control w-full">
                    <label class="label">
                        <span class="label-text">用户名</span>
                    </label>
                    <input type="text" v-model="username" placeholder="请输入用户名" class="input input-bordered w-full"
                        @keyup.enter="handleLogin" />
                </div>

                <div class="form-control w-full mt-4">
                    <label class="label">
                        <span class="label-text">密码</span>
                    </label>
                    <input type="password" v-model="password" placeholder="请输入密码" class="input input-bordered w-full"
                        @keyup.enter="handleLogin" />
                </div>

                <div v-if="errorMessage" class="text-error text-sm mt-4 text-center">
                    {{ errorMessage }}
                </div>

                <div class="card-actions justify-center mt-8 gap-4 flex-col">
                    <button class="btn btn-primary w-full" @click="handleLogin" :disabled="loading">
                        <span v-if="loading" class="loading loading-spinner"></span>
                        登录
                    </button>
                    <div class="text-sm text-center">
                        还没有账号？ <RouterLink to="/register" class="link link-primary">去注册</RouterLink>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
