<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const games = ref([])
const loading = ref(false)
const modalOpen = ref(false)
const modalTitle = ref('添加游戏')
const submitLoading = ref(false)

const formData = ref({
    id: null,
    name: '',
    description: '',
    tags: '',
    chatroomLink: '',
    lobbyLink: '',
    status: 1
})
const selectedFile = ref(null)
const previewImage = ref(null)

const fetchGames = async () => {
    loading.value = true
    try {
        const response = await fetch('http://localhost:8080/game/list')
        const result = await response.json()
        if (result.code === 200) {
            games.value = result.data
        }
    } catch (error) {
        console.error('Fetch games failed', error)
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    // Basic Admin Check (Route guard should handle this too, but double check)
    if (!userStore.userInfo || userStore.userInfo.role !== 'ADMIN') {
        router.push('/')
        return
    }
    fetchGames()
})

const openModal = (game = null) => {
    if (game) {
        modalTitle.value = '编辑游戏'
        formData.value = { ...game }
        previewImage.value = game.cover
        selectedFile.value = null
    } else {
        modalTitle.value = '添加游戏'
        formData.value = {
            id: null,
            name: '',
            description: '',
            tags: '',
            chatroomLink: '',
            lobbyLink: '',
            status: 1
        }
        previewImage.value = null
        selectedFile.value = null
    }
    modalOpen.value = true
}

const handleFileChange = (event) => {
    const file = event.target.files[0]
    if (file) {
        selectedFile.value = file
        const reader = new FileReader()
        reader.onload = (e) => {
            previewImage.value = e.target.result
        }
        reader.readAsDataURL(file)
    }
}

const handleSubmit = async () => {
    submitLoading.value = true
    try {
        const data = new FormData()
        data.append('name', formData.value.name)
        if (selectedFile.value) {
            data.append('cover', selectedFile.value)
        }
        data.append('description', formData.value.description)
        data.append('tags', formData.value.tags)
        data.append('chatroomLink', formData.value.chatroomLink)
        data.append('lobbyLink', formData.value.lobbyLink)

        let url = 'http://localhost:8080/game/add'
        let method = 'POST'

        if (formData.value.id) {
            url = 'http://localhost:8080/game/update'
            method = 'PUT'
            data.append('id', formData.value.id)
            data.append('status', formData.value.status)
        }

        const response = await fetch(url, {
            method: method,
            headers: {
                'Authorization': `Bearer ${userStore.userInfo.token}`
            },
            body: data
        })

        const result = await response.json()
        if (result.code === 200) {
            modalOpen.value = false
            fetchGames() // Refresh list
        } else {
            alert(result.message || '操作失败')
        }
    } catch (error) {
        console.error(error)
        alert('网络错误')
    } finally {
        submitLoading.value = false
    }
}

const handleDelete = async (id) => {
    if (!confirm('确定要删除这个游戏吗？')) return

    try {
        const response = await fetch(`http://localhost:8080/game/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${userStore.userInfo.token}`
            }
        })
        const result = await response.json()
        if (result.code === 200) {
            fetchGames()
        } else {
            alert(result.message || '删除失败')
        }
    } catch (e) {
        alert('删除失败')
    }
}
</script>

<template>
    <div class="container mx-auto px-4 pt-20 pb-10 min-h-[calc(100vh-5rem)]">
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-3xl font-bold">首页管理</h1>
            <button class="btn btn-primary" @click="openModal(null)">添加游戏</button>
        </div>

        <div class="overflow-x-auto bg-base-100 rounded-box shadow">
            <table class="table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>封面</th>
                        <th>名称</th>
                        <th>标签</th>
                        <th>状态</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="game in games" :key="game.id">
                        <td>{{ game.id }}</td>
                        <td>
                            <div class="avatar">
                                <div class="mask mask-squircle w-12 h-12">
                                    <img :src="game.cover" alt="Cover" />
                                </div>
                            </div>
                        </td>
                        <td>
                            <div class="font-bold">{{ game.name }}</div>
                        </td>
                        <td>
                            <div class="flex gap-1 flex-wrap">
                                <span class="badge badge-ghost badge-sm" v-for="tag in (game.tags || '').split(',')"
                                    :key="tag" v-if="game.tags">{{ tag }}</span>
                            </div>
                        </td>
                        <td>
                            <div class="badge" :class="game.status === 1 ? 'badge-success' : 'badge-error'">
                                {{ game.status === 1 ? '启用' : '禁用' }}
                            </div>
                        </td>
                        <td>
                            <button class="btn btn-ghost btn-xs" @click="openModal(game)">编辑</button>
                            <button class="btn btn-ghost btn-xs text-error" @click="handleDelete(game.id)">删除</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- Modal -->
        <dialog class="modal" :class="{ 'modal-open': modalOpen }">
            <div class="modal-box w-11/12 max-w-3xl">
                <h3 class="font-bold text-lg mb-4">{{ modalTitle }}</h3>

                <div class="space-y-4">
                    <!-- Basic Info Group -->
                    <fieldset class="border border-base-300 p-4 rounded-lg">
                        <legend class="px-2 font-bold text-sm">基本信息</legend>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div class="form-control">
                                <label class="label"><span class="label-text">游戏名称</span></label>
                                <input type="text" v-model="formData.name" class="input input-bordered input-sm" />
                            </div>
                            <div class="form-control">
                                <label class="label"><span class="label-text">标签 (逗号分隔)</span></label>
                                <input type="text" v-model="formData.tags" class="input input-bordered input-sm"
                                    placeholder="FPS,Hardcore" />
                            </div>
                            <div class="form-control">
                                <label class="label"><span class="label-text">状态</span></label>
                                <select v-model="formData.status" class="select select-bordered select-sm">
                                    <option :value="1">启用</option>
                                    <option :value="0">禁用</option>
                                </select>
                            </div>
                        </div>
                    </fieldset>

                    <!-- Media & Content Group -->
                    <fieldset class="border border-base-300 p-4 rounded-lg">
                        <legend class="px-2 font-bold text-sm">内容与媒体</legend>
                        <div class="grid grid-cols-1 gap-4">
                            <div class="form-control">
                                <label class="label"><span class="label-text">封面图片</span></label>
                                <input type="file" @change="handleFileChange" accept="image/*"
                                    class="file-input file-input-bordered file-input-sm w-full" />
                                <div class="mt-2 h-40 bg-base-200 rounded flex items-center justify-center overflow-hidden border border-base-300 border-dashed"
                                    v-if="previewImage">
                                    <img :src="previewImage" class="h-full object-contain" />
                                </div>
                            </div>
                            <div class="form-control">
                                <label class="label"><span class="label-text">简介</span></label>
                                <textarea v-model="formData.description"
                                    class="textarea textarea-bordered h-24"></textarea>
                            </div>
                        </div>
                    </fieldset>

                    <!-- Links Group -->
                    <fieldset class="border border-base-300 p-4 rounded-lg">
                        <legend class="px-2 font-bold text-sm">链接配置</legend>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div class="form-control">
                                <label class="label"><span class="label-text">聊天室链接</span></label>
                                <input type="text" v-model="formData.chatroomLink"
                                    class="input input-bordered input-sm" />
                            </div>
                            <div class="form-control">
                                <label class="label"><span class="label-text">大厅链接</span></label>
                                <input type="text" v-model="formData.lobbyLink" class="input input-bordered input-sm" />
                            </div>
                        </div>
                    </fieldset>
                </div>

                <div class="modal-action">
                    <button class="btn" @click="modalOpen = false">取消</button>
                    <button class="btn btn-primary" @click="handleSubmit" :disabled="submitLoading">保存</button>
                </div>
            </div>
            <form method="dialog" class="modal-backdrop">
                <button @click="modalOpen = false">close</button>
            </form>
        </dialog>
    </div>
</template>
