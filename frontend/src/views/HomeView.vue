<script setup>
import { ref, onMounted } from 'vue'

const games = ref([])

// 使用高质量占位图（或后续替换为实际assets）
const carouselImages = ref([
    'https://placehold.co/1200x500/1e1e2e/FFFFFF?text=Welcome+to+Tagilla',
    'https://placehold.co/1200x500/2f2f45/FFFFFF?text=New+Events',
    'https://placehold.co/1200x500/3e3e5e/FFFFFF?text=Join+Us'
])

const announcements = ref([
    '🔥 2024夏季社区挑战赛正式开启！',
    '📢 关于服务器维护的通知 (12/10)',
    '🎉 恭喜用户 "TagillaKing" 成为本周MVP',
    '⚠️ 社区守则更新：请务必阅读',
    '💬 官方交流群：12345678'
])

const currentSlide = ref(0)
const carouselRef = ref(null)

const fetchGames = async () => {
    try {
        const response = await fetch('http://localhost:8080/game/list')
        const result = await response.json()
        if (result.code === 200) {
            // Filter only active games if needed, or backend does it. 
            // Currently backend returns all. Let's filter for status=1 (Active)
            games.value = result.data.filter(g => g.status === 1)
        }
    } catch (error) {
        console.error('Fetch games failed', error)
    }
}

onMounted(() => {
    fetchGames()
})

const scrollToSlide = (index) => {
    currentSlide.value = index
    if (carouselRef.value) {
        const width = carouselRef.value.clientWidth
        carouselRef.value.scrollTo({
            left: width * index,
            behavior: 'smooth'
        })
    }
}

const nextSlide = () => {
    const next = (currentSlide.value + 1) % carouselImages.value.length
    scrollToSlide(next)
}

const prevSlide = () => {
    const prev = (currentSlide.value - 1 + carouselImages.value.length) % carouselImages.value.length
    scrollToSlide(prev)
}
</script>

<template>
    <div class="container mx-auto px-4 pt-8 pb-10 min-h-[calc(100vh-5rem)]">
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 h-full">
            <!-- Left: Game Column (Span 5) -->
            <div class="lg:col-span-5 flex flex-col h-full">
                <div
                    class="card bg-base-100/80 backdrop-blur-md shadow-xl border border-white/20 h-full hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-1">
                    <div class="card-body p-6">
                        <h2
                            class="card-title text-3xl font-extrabold mb-8 bg-clip-text text-transparent bg-gradient-to-r from-primary to-secondary drop-shadow-sm">
                            <span class="mr-3 text-3xl filter drop-shadow">🎮</span> 游戏专栏
                        </h2>

                        <!-- Game Item -->
                        <div v-for="game in games" :key="game.id"
                            class="group relative overflow-hidden rounded-2xl border border-base-content/5 bg-base-50/50 hover:bg-base-100 transition-colors duration-300 mb-6 last:mb-0">
                            <div class="flex flex-col sm:flex-row gap-0 sm:gap-6 p-4">
                                <!-- Poster -->
                                <figure
                                    class="w-full sm:w-40 h-56 sm:h-48 rounded-xl overflow-hidden shadow-lg flex-shrink-0 relative">
                                    <img :src="game.cover || 'https://placehold.co/300x400/2a1b3d/FFF?text=No+Cover'"
                                        class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                                        alt="Game Poster" />
                                    <div
                                        class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60">
                                    </div>
                                </figure>

                                <!-- Content -->
                                <div class="flex flex-col flex-1 py-1 gap-3 z-10 justify-between">
                                    <div>
                                        <h3
                                            class="text-2xl font-bold text-base-content group-hover:text-primary transition-colors">
                                            {{ game.name }}</h3>
                                        <div class="flex gap-2 mt-2 flex-wrap">
                                            <div v-for="tag in (game.tags ? game.tags.split(',') : [])" :key="tag"
                                                class="badge badge-accent badge-xs text-[10px] uppercase font-bold tracking-wider">
                                                {{ tag }}</div>
                                        </div>
                                        <p class="text-sm text-base-content/70 mt-3 line-clamp-2 leading-relaxed">
                                            {{ game.description }}</p>
                                    </div>

                                    <div class="grid grid-cols-2 gap-3 mt-4">
                                        <a :href="game.chatroomLink" target="_blank"
                                            class="btn btn-primary btn-sm shadow-lg shadow-primary/30 border-none bg-gradient-to-r from-primary to-secondary text-white hover:brightness-110 border-0">
                                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" fill="none"
                                                viewBox="0 0 24 24" stroke="currentColor">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                                    d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                            </svg>
                                            聊天室
                                        </a>
                                        <a :href="game.lobbyLink" target="_blank"
                                            class="btn btn-outline btn-sm hover:btn-secondary hover:text-white transition-all shadow-sm">
                                            组队大厅
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Empty State -->
                        <div v-if="games.length === 0" class="text-center py-10 text-gray-500">
                            暂无游戏数据，请联系管理员添加
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right: Announcement & Carousel (Span 7) -->
            <div class="lg:col-span-7 flex flex-col h-full">
                <div
                    class="card bg-base-100/80 backdrop-blur-md shadow-xl border border-white/20 h-full hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-1">
                    <div class="card-body p-6">
                        <h2
                            class="card-title text-3xl font-extrabold mb-8 bg-clip-text text-transparent bg-gradient-to-r from-secondary to-primary drop-shadow-sm">
                            <span class="mr-3 text-3xl filter drop-shadow">📢</span> 社区动态
                        </h2>

                        <div class="flex flex-col gap-8">
                            <!-- Carousel -->
                            <div class="w-full relative group">
                                <div ref="carouselRef"
                                    class="carousel w-full aspect-[21/9] rounded-2xl shadow-lg border border-base-content/5 overflow-hidden relative scroll-smooth">
                                    <div v-for="(img, index) in carouselImages" :key="index"
                                        class="carousel-item w-full relative h-full">
                                        <img :src="img" class="w-full h-full object-cover" />
                                        <!-- Gradient Overlay -->
                                        <div
                                            class="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-black/80 to-transparent">
                                        </div>
                                        <!-- Caption -->
                                        <div class="absolute bottom-6 left-6 max-w-[80%]">
                                            <h3
                                                class="text-white font-bold text-2xl drop-shadow-lg opacity-0 group-hover:opacity-100 transition-all duration-500 transform translate-y-4 group-hover:translate-y-0">
                                                精彩活动 {{ index + 1 }}
                                            </h3>
                                            <p
                                                class="text-white/80 text-sm mt-1 opacity-0 group-hover:opacity-100 transition-all duration-500 delay-100 transform translate-y-4 group-hover:translate-y-0">
                                                点击查看详情，参与赢取丰厚奖励！
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                <!-- Hover Navigation -->
                                <div
                                    class="absolute inset-0 flex justify-between items-center px-4 pointer-events-none">
                                    <button @click="prevSlide"
                                        class="btn btn-circle btn-sm bg-black/30 border-none text-white backdrop-blur-sm pointer-events-auto opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:bg-black/50">❮</button>
                                    <button @click="nextSlide"
                                        class="btn btn-circle btn-sm bg-black/30 border-none text-white backdrop-blur-sm pointer-events-auto opacity-0 group-hover:opacity-100 transition-opacity duration-300 hover:bg-black/50">❯</button>
                                </div>

                                <!-- Indicators -->
                                <div class="flex justify-center w-full py-4 gap-2 absolute -bottom-8 left-0 right-0">
                                    <button v-for="(img, index) in carouselImages" :key="index"
                                        @click="scrollToSlide(index)"
                                        class="w-2.5 h-2.5 rounded-full transition-all duration-300"
                                        :class="currentSlide === index ? 'bg-primary scale-125' : 'bg-base-content/20 hover:bg-primary/50'">
                                    </button>
                                </div>
                            </div>

                            <!-- Text Announcements List -->
                            <div class="bg-base-50/50 rounded-2xl p-2 border border-base-content/5">
                                <ul class="space-y-2">
                                    <li v-for="(ann, index) in announcements" :key="index"
                                        class="flex items-center gap-4 p-3 rounded-xl hover:bg-white/80 hover:shadow-md hover:-translate-x-[-4px] transition-all duration-300 cursor-pointer group">
                                        <div
                                            class="w-10 h-10 rounded-full bg-secondary/10 flex items-center justify-center group-hover:bg-secondary group-hover:text-white transition-colors">
                                            <span class="font-bold text-sm">{{ index + 1 }}</span>
                                        </div>
                                        <div class="flex-1">
                                            <span
                                                class="text-base-content/90 font-medium group-hover:text-primary transition-colors text-lg">{{
                                                    ann }}</span>
                                        </div>
                                        <span v-if="index < 2"
                                            class="badge badge-accent badge-sm uppercase font-bold text-[10px] tracking-wider">NEW</span>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
