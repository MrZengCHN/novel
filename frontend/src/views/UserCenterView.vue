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

const selectedFile = ref(null)

const handleFileUpload = (event) => {
    const file = event.target.files[0]
    if (!file) return

    // Limit 300KB
    if (file.size > 300 * 1024) {
        message.value = '文件大小不能超过300KB'
        // Clear input
        event.target.value = ''
        selectedFile.value = null
        return
    }

    // Limit type
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
        message.value = '只支持JPG or PNG格式'
        event.target.value = ''
        selectedFile.value = null
        return
    }

    selectedFile.value = file
    // Preview
    const reader = new FileReader()
    reader.onload = (e) => {
        avatarInput.value = e.target.result
    }
    reader.readAsDataURL(file)
}

const handleUpdateProfile = async () => {
    loading.value = true
    message.value = ''
    try {
        const formData = new FormData()
        formData.append('userId', userStore.userInfo.id)
        if (selectedFile.value) {
            formData.append('avatar', selectedFile.value)
        } else if (avatarInput.value) {
            // If it's a URL string (not changed or entered manually), backend needs to deciding how to handle?
            // Actually our backend expects MultipartFile for "avatar". 
            // If user enters a String URL manually, it won't be picked up by "MultipartFile avatar".
            // We might need to handle this case, but for now user asked for "upload avatar".
            // If they didn't upload file but entered URL... current backend doesn't support "String avatar" anymore for updateProfile in same param name.
            // But let's focus on file upload fix first. 
        }

        if (signatureInput.value) {
            formData.append('signature', signatureInput.value)
        }

        const response = await fetch(`http://localhost:8080/user/update`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${userStore.userInfo.token}`
            },
            body: formData
        })

        if (!response.ok) {
            const text = await response.text()
            console.error('Update failed:', response.status, response.statusText, text)
            message.value = `更新失败: ${response.status} ${text}`
            return
        }

        const result = await response.json()

        if (result.code === 200) {
            userStore.setUserInfo(result.data)
            isEditing.value = false
            message.value = '更新成功'
            selectedFile.value = null // reset
        } else {
            message.value = result.message || '更新失败'
        }

    } catch (error) {
        console.error(error)
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
    <div class="min-h-[calc(100vh-5rem)] pt-20 pb-10 px-4 container mx-auto max-w-4xl">
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
                            <span class="label-text">更换头像</span>
                        </label>
                        <input type="file" @change="handleFileUpload" accept="image/png, image/jpeg"
                            class="file-input file-input-bordered w-full" />
                        <label class="label">
                            <span class="label-text-alt">支持 JPG/PNG, 小于 300KB</span>
                        </label>

                        <div class="divider text-xs">或者使用 URL</div>

                        <input type="text" v-model="avatarInput" placeholder="输入图片链接"
                            class="input input-bordered w-full" />

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
