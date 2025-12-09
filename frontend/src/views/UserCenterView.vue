<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const userStore = useUserStore()

const isEditing = ref(false)
const avatarInput = ref('')
const signatureInput = ref('')
const loading = ref(false)
const message = ref('')

const initData = () => {
    if (!userStore.userInfo) {
        router.push('/login')
        return
    }
    avatarInput.value = userStore.userInfo.avatar || ''
    signatureInput.value = userStore.userInfo.signature || ''
}

onMounted(() => {
    initData()
})

const handleLogout = () => {
    userStore.logout()
    router.push('/login')
}

const handleUpdateProfile = async () => {
    loading.value = true
    message.value = ''
    try {
        // Construct query params manually since backend expects RequestParam
        const params = new URLSearchParams()
        params.append('userId', userStore.userInfo.id)
        params.append('avatar', avatarInput.value)
        params.append('signature', signatureInput.value)

        const response = await fetch(`http://localhost:8080/user/update?${params.toString()}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${userStore.userInfo.token}`
            }
        })

        const result = await response.json()

        if (result.code === 200) {
            userStore.setUserInfo(result.data)
            isEditing.value = false
            message.value = '更新成功'
        } else {
            message.value = result.message || '更新失败'
        }

    } catch (error) {
        message.value = '网络错误'
    } finally {
        loading.value = false
    }
}

const roleBadgeClass = (role) => {
    switch (role) {
        case 'ADMIN': return 'badge-error'
        case 'VIP': return 'badge-warning'
        default: return 'badge-ghost'
    }
}
</script>

<template>
    <div class="min-h-screen pt-20 pb-10 px-4 container mx-auto max-w-4xl">
        <div class="card bg-base-100 shadow-xl border border-base-200 backdrop-blur-sm bg-opacity-90"
            v-if="userStore.userInfo">

            <!-- Cover / Header -->
            <figure class="h-48 bg-gradient-to-r from-primary to-secondary relative">
                <div class="absolute bottom-4 right-4">
                    <button class="btn btn-sm btn-ghost text-white" @click="handleLogout">退出登录</button>
                </div>
            </figure>

            <div class="card-body -mt-16">

                <!-- Avatar & Basic Info -->
                <div class="flex flex-col md:flex-row gap-6 items-start">
                    <div class="avatar online">
                        <div class="w-32 rounded-full ring ring-primary ring-offset-base-100 ring-offset-2 bg-base-100">
                            <img
                                :src="userStore.userInfo.avatar || 'https://daisyui.com/images/stock/photo-1534528741775-53994a69daeb.jpg'" />
                        </div>
                    </div>

                    <div class="flex-1 pt-14">
                        <div class="flex items-center gap-2">
                            <h1 class="text-3xl font-bold">{{ userStore.userInfo.username }}</h1>
                            <div class="badge" :class="roleBadgeClass(userStore.userInfo.role)">{{
                                userStore.userInfo.role }}</div>
                        </div>

                        <div class="mt-2 flex flex-wrap gap-2" v-if="userStore.userInfo.tags">
                            <div class="badge badge-accent badge-outline"
                                v-for="tag in userStore.userInfo.tags.split(',')" :key="tag">
                                {{ tag }}
                            </div>
                        </div>

                        <div class="mt-4 text-base-content/70">
                            <p v-if="!isEditing">{{ userStore.userInfo.signature || '这个人很懒，什么都没有写...' }}</p>
                            <div v-else class="form-control">
                                <textarea class="textarea textarea-bordered h-24" v-model="signatureInput"
                                    placeholder="写点什么..."></textarea>
                            </div>
                        </div>
                    </div>

                    <div class="pt-14">
                        <button class="btn btn-outline btn-primary" v-if="!isEditing"
                            @click="isEditing = true">编辑资料</button>
                        <div class="flex gap-2" v-else>
                            <button class="btn btn-primary" @click="handleUpdateProfile" :disabled="loading">保存</button>
                            <button class="btn btn-ghost" @click="isEditing = false">取消</button>
                        </div>
                    </div>
                </div>

                <div class="divider"></div>

                <!-- Detailed Edit Form -->
                <div v-if="isEditing" class="grid grid-cols-1 md:grid-cols-2 gap-6 animate-fade-in-down">
                    <div class="form-control">
                        <label class="label">
                            <span class="label-text">头像 URL</span>
                        </label>
                        <input type="text" v-model="avatarInput" placeholder="输入图片链接" class="input input-bordered" />
                        <label class="label">
                            <span class="label-text-alt">预览:</span>
                        </label>
                        <div class="avatar">
                            <div class="w-16 rounded">
                                <img
                                    :src="avatarInput || 'https://daisyui.com/images/stock/photo-1534528741775-53994a69daeb.jpg'" />
                            </div>
                        </div>
                    </div>
                </div>

                <div v-if="message" class="alert mt-4"
                    :class="message.includes('失败') || message.includes('错误') ? 'alert-error' : 'alert-success'">
                    <span>{{ message }}</span>
                </div>

            </div>
        </div>

        <div v-else class="flex justify-center items-center h-64">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    </div>
</template>

<style scoped>
.animate-fade-in-down {
    animation: fadeInDown 0.3s ease-out;
}

@keyframes fadeInDown {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
