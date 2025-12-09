<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const channelId = ref(route.params.channelId)
const messages = ref([])
const onlineUsers = ref([])
const inputMessage = ref('')
const isMuted = ref(false)
const muteExpireTime = ref('')
const socket = ref(null)
const chatContainer = ref(null)

// Context Menu State
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const selectedUser = ref(null)

// Message Context Menu State
const messageContextMenuVisible = ref(false)
const messageContextMenuX = ref(0)
const messageContextMenuY = ref(0)
const selectedMessage = ref(null)

const connectWebSocket = () => {
    if (!userStore.userInfo) {
        alert('请先登录')
        router.push('/login')
        return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//localhost:8080/ws/chat?channel=${channelId.value}&token=${userStore.userInfo.token}`

    socket.value = new WebSocket(wsUrl)

    socket.value.onopen = () => {
        addSystemMessage(`已连接到频道: ${channelId.value}`)
    }

    socket.value.onmessage = (event) => {
        const data = JSON.parse(event.data)
        handleSocketMessage(data)
    }

    socket.value.onclose = () => {
        addSystemMessage('连接已断开')
    }

    socket.value.onerror = (error) => {
        console.error('WebSocket Error', error)
        addSystemMessage('连接发生错误')
    }
}

const handleSocketMessage = (data) => {
    if (data.type === 'CHAT') {
        messages.value.push(data)
        scrollToBottom()
    } else if (data.type === 'JOIN') {
        // Add to online list if not exists
        if (!onlineUsers.value.find(u => u.id === data.userId)) {
            onlineUsers.value.push({
                id: data.userId,
                username: data.username,
                avatar: data.avatar,
                role: data.role
            })
        }
        addSystemMessage(`${data.username} 加入了聊天室`)
    } else if (data.type === 'LEAVE') {
        onlineUsers.value = onlineUsers.value.filter(u => u.id !== data.userId)
        addSystemMessage(`${data.username} 离开了聊天室`)
    } else if (data.type === 'ERROR') {
        alert(data.content)
    } else if (data.type === 'INITIAL_LIST') {
        onlineUsers.value = data.users
    } else if (data.type === 'HISTORY') {
        // Prepend history (though usually empty at start)
        messages.value = [...data.messages, ...messages.value]
        scrollToBottom()
    } else if (data.type === 'MUTE_STATUS') {
        const previousMuted = isMuted.value
        isMuted.value = data.muted

        let timeStr = ''
        if (data.muted && data.expireTime) {
            const date = new Date(data.expireTime)
            timeStr = date.toLocaleTimeString()
            muteExpireTime.value = timeStr
        } else {
            muteExpireTime.value = ''
        }

        // Only show notification if status changed
        if (previousMuted !== data.muted) {
            if (data.muted) {
                addSystemMessage(`您已被禁言，解除时间：${timeStr}`)
            } else {
                addSystemMessage('您的禁言已被解除')
            }
        }
    } else if (data.type === 'RECALL') {
        messages.value = messages.value.filter(m => m.messageId !== data.messageId)
    }
}

const sendMessage = () => {
    if (!inputMessage.value.trim() || !socket.value) return
    socket.value.send(inputMessage.value)
    inputMessage.value = ''
}

const addSystemMessage = (text) => {
    messages.value.push({
        type: 'SYSTEM',
        content: text,
        time: new Date().toLocaleTimeString()
    })
    scrollToBottom()
}

const scrollToBottom = () => {
    nextTick(() => {
        if (chatContainer.value) {
            chatContainer.value.scrollTop = chatContainer.value.scrollHeight
        }
    })
}

const muteDuration = ref(10)
const showMuteModal = ref(false)

// Admin Context Menu Logic
const showContextMenu = (e, user) => {
    // Only Admin can mute
    if (userStore.userInfo.role !== 'ADMIN') return
    // Cannot mute self or other Admins
    if (user.id === userStore.userInfo.id) return
    if (user.role === 'ADMIN') return

    e.preventDefault()
    selectedUser.value = user
    contextMenuX.value = e.clientX
    contextMenuY.value = e.clientY
    contextMenuVisible.value = true
}

const closeContextMenu = () => {
    contextMenuVisible.value = false
}

// Message Context Menu Logic
const showMessageContextMenu = (e, msg) => {
    // Permission Check
    const isMyMessage = msg.userId === userStore.userInfo?.id
    const isAdmin = userStore.userInfo?.role === 'ADMIN'
    const targetIsAdmin = msg.role === 'ADMIN'

    // Allow if: My Message OR (I am Admin AND Target is not Admin)
    if (isMyMessage || (isAdmin && !targetIsAdmin)) {
        e.preventDefault()
        selectedMessage.value = msg
        messageContextMenuX.value = e.clientX
        messageContextMenuY.value = e.clientY
        messageContextMenuVisible.value = true
        // Close user context menu if open
        contextMenuVisible.value = false
        showMuteModal.value = false
    }
}

const closeMessageContextMenu = () => {
    messageContextMenuVisible.value = false
}

const recallMessage = () => {
    if (!selectedMessage.value || !socket.value) return

    const payload = {
        type: 'RECALL',
        messageId: selectedMessage.value.messageId
    }
    socket.value.send(JSON.stringify(payload))
    messageContextMenuVisible.value = false
}

const openMuteModal = () => {
    showMuteModal.value = true
    closeContextMenu()
}

const confirmMute = async () => {
    if (!selectedUser.value) return
    try {
        const response = await fetch(`http://localhost:8080/chat/mute?userId=${selectedUser.value.id}&channelId=${channelId.value}&durationMinutes=${muteDuration.value}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${userStore.userInfo.token}`
            }
        })
        const result = await response.json()
        if (result.code === 200) {
            addSystemMessage(`管理员操作: ${selectedUser.value.username} 已被禁言 ${muteDuration.value}分钟`)
        } else {
            alert(result.message)
        }
    } catch (e) {
        alert('操作失败')
    }
    showMuteModal.value = false
}

const unmuteUser = async () => {
    if (!selectedUser.value) return
    try {
        const response = await fetch(`http://localhost:8080/chat/unmute?userId=${selectedUser.value.id}&channelId=${channelId.value}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${userStore.userInfo.token}`
            }
        })
        const result = await response.json()
        if (result.code === 200) {
            addSystemMessage(`管理员操作: ${selectedUser.value.username} 已解除禁言`)
        } else {
            alert(result.message)
        }
    } catch (e) {
        alert('操作失败')
    }
    closeContextMenu()
}

onMounted(() => {
    connectWebSocket()
    window.addEventListener('click', closeContextMenu)
    window.addEventListener('click', closeMessageContextMenu)
})

onUnmounted(() => {
    if (socket.value) socket.value.close()
    window.removeEventListener('click', closeContextMenu)
    window.removeEventListener('click', closeMessageContextMenu)
})
</script>

<template>
    <div class="flex h-[calc(100vh-5rem)] pt-3 bg-base-200">
        <!-- Sidebar: Online Users -->
        <div class="w-64 bg-base-100 border-r border-base-content/10 hidden md:flex flex-col">
            <div class="p-4 border-b border-base-content/10">
                <h2 class="font-bold text-lg">在线用户 ({{ onlineUsers.length }})</h2>
                <p class="text-xs text-base-content/60" v-if="userStore.userInfo?.role === 'ADMIN'">右键用户管理</p>
            </div>
            <div class="flex-1 overflow-y-auto p-2 space-y-2">
                <div v-for="user in onlineUsers" :key="user.id"
                    class="flex items-center gap-3 p-2 hover:bg-base-200 rounded-lg cursor-pointer transition-colors"
                    @contextmenu="showContextMenu($event, user)">
                    <div class="avatar placeholder">
                        <div class="bg-neutral text-neutral-content rounded-full w-8">
                            <img v-if="user.avatar" :src="user.avatar" />
                            <span v-else class="text-xs">{{ user.username.substring(0, 1) }}</span>
                        </div>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-sm font-medium" :class="{ 'text-primary': user.role === 'ADMIN' }">
                            {{ user.username }}
                            <span v-if="user.role === 'ADMIN'" class="badge badge-xs badge-primary ml-1">ADMIN</span>
                        </span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Main Chat Area -->
        <div class="flex-1 flex flex-col min-w-0">
            <!-- Header -->
            <div class="h-16 bg-base-100 border-b border-base-content/10 flex items-center px-6 justify-between">
                <div class="flex items-center gap-2">
                    <span class="font-bold text-xl"># {{ channelId }}</span>
                    <span class="badge badge-success badge-sm">Online</span>
                </div>
                <!-- Mobile toggle for list could go here -->
            </div>

            <!-- Messages -->
            <div ref="chatContainer" class="flex-1 overflow-y-auto p-6 space-y-4">
                <div v-for="(msg, index) in messages" :key="index">
                    <!-- System Message -->
                    <div v-if="msg.type === 'SYSTEM'" class="text-center my-4">
                        <span class="text-xs text-base-content/50 bg-base-300 px-2 py-1 rounded-full">{{ msg.content
                        }}</span>
                    </div>

                    <!-- Chat Message -->
                    <div v-else class="chat" :class="msg.userId === userStore.userInfo?.id ? 'chat-end' : 'chat-start'">
                        <div class="chat-image avatar">
                            <div class="w-10 rounded-full">
                                <img v-if="msg.avatar" :src="msg.avatar" />
                                <img v-else src="https://placehold.co/40x40" />
                            </div>
                        </div>
                        <div class="chat-header">
                            {{ msg.username }}
                            <time class="text-xs opacity-50 ml-1">{{ msg.time.split('T')[1].split('.')[0] }}</time>
                        </div>
                        <div class="chat-bubble"
                            :class="msg.userId === userStore.userInfo?.id ? 'chat-bubble-primary' : 'chat-bubble-secondary'"
                            @contextmenu="showMessageContextMenu($event, msg)">
                            {{ msg.content }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- Input Area -->
            <div class="p-4 bg-base-100 border-t border-base-content/10">
                <form @submit.prevent="sendMessage" class="flex gap-2">
                    <input v-model="inputMessage" type="text"
                        :placeholder="isMuted ? '您已被禁言，解除时间：' + muteExpireTime + '，无法发送消息' : '发送消息...'"
                        class="input input-bordered flex-1" :disabled="isMuted" />
                    <button type="submit" class="btn btn-primary"
                        :disabled="!inputMessage.trim() || isMuted">发送</button>
                </form>
            </div>
        </div>

        <!-- User Context Menu -->
        <div v-if="contextMenuVisible" :style="{ top: contextMenuY + 'px', left: contextMenuX + 'px' }"
            class="fixed bg-base-100 border border-base-content/10 shadow-xl rounded-lg py-1 z-50 min-w-[120px]">
            <button
                class="w-full text-left px-4 py-2 hover:bg-base-200 text-error text-sm font-medium border-b border-base-content/5"
                @click="openMuteModal">
                🚫 禁言用户
            </button>
            <button class="w-full text-left px-4 py-2 hover:bg-base-200 text-success text-sm font-medium"
                @click="unmuteUser">
                ✅ 解除禁言
            </button>
        </div>

        <!-- Message Context Menu -->
        <div v-if="messageContextMenuVisible"
            :style="{ top: messageContextMenuY + 'px', left: messageContextMenuX + 'px' }"
            class="fixed bg-base-100 border border-base-content/10 shadow-xl rounded-lg py-1 z-50 min-w-[120px]">
            <button class="w-full text-left px-4 py-2 hover:bg-base-200 text-error text-sm font-medium"
                @click="recallMessage">
                ↩️ 撤回消息
            </button>
        </div>

        <!-- Mute Duration Modal -->
        <dialog id="mute_modal" class="modal" :class="{ 'modal-open': showMuteModal }">
            <div class="modal-box">
                <h3 class="font-bold text-lg">设置禁言时间</h3>
                <p class="py-4">请选择对 {{ selectedUser?.username }} 的禁言时长：</p>
                <select v-model="muteDuration" class="select select-bordered w-full max-w-xs">
                    <option :value="10">10 分钟 (默认)</option>
                    <option :value="60">1 小时</option>
                    <option :value="720">12 小时</option>
                    <option :value="1440">24 小时</option>
                </select>
                <div class="modal-action">
                    <button class="btn" @click="showMuteModal = false">取消</button>
                    <button class="btn btn-error" @click="confirmMute">确认禁言</button>
                </div>
            </div>
            <form method="dialog" class="modal-backdrop">
                <button @click="showMuteModal = false">close</button>
            </form>
        </dialog>
    </div>
</template>
