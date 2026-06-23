<template>
  <div class="dashboard-page">
    <div class="header">
      <h1>数据看板</h1>
      <div>
        <el-button @click="$router.push('/tasks')">返回任务列表</el-button>
        <el-select v-if="isAdmin" v-model="selectedUserId" placeholder="查看指定用户" clearable style="width:200px; margin-left:12px" @change="loadStats" :loading="loadingUsers">
          <el-option v-for="user in users" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
        </el-select>
      </div>
    </div>

    <div class="stats-cards">
      <div class="card card-total">
        <div class="card-value">{{ stats.total }}</div>
        <div class="card-label">总任务数</div>
      </div>
      <div class="card card-pending">
        <div class="card-value">{{ stats.pending }}</div>
        <div class="card-label">待处理</div>
      </div>
      <div class="card card-progress">
        <div class="card-value">{{ stats.inProgress }}</div>
        <div class="card-label">进行中</div>
      </div>
      <div class="card card-done">
        <div class="card-value">{{ stats.completed }}</div>
        <div class="card-label">已完成</div>
      </div>
      <div class="card card-time">
        <div class="card-value">{{ formatTime(stats.totalSeconds) }}</div>
        <div class="card-label">总用时</div>
      </div>
      <div class="card card-rate">
        <div class="card-value">{{ stats.completionRate }}%</div>
        <div class="card-label">完成率</div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-box">
        <h3>任务状态分布</h3>
        <div ref="pieChart" style="height:300px"></div>
      </div>
      <div class="chart-box">
        <h3>完成率</h3>
        <div ref="gaugeChart" style="height:300px"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { useUserStore } from '../stores/user'
import * as echarts from 'echarts'

const userStore = useUserStore()
const userId = computed(() => userStore.id)
const isAdmin = computed(() => userStore.role === 'ADMIN')
const users = ref([])
const selectedUserId = ref(0)
const loadingUsers = ref(false)
const API = 'http://localhost:8080/api'

const stats = reactive({
  total: 0, pending: 0, inProgress: 0,
  completed: 0, totalSeconds: 0, completionRate: 0
})

const pieChart = ref(null)
const gaugeChart = ref(null)
let pieInstance = null
let gaugeInstance = null
let statsTimer = null

const loadUsers = async () => {
  if (!isAdmin.value || !userId.value) return
  loadingUsers.value = true
  try {
    const res = await axios.get(`${API}/users/all?requesterId=${userId.value}`)
    users.value = res.data.data || []
  } catch (e) {
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

const loadStats = async () => {
  if (!userId.value) return
  try {
    const targetUserId = isAdmin.value && selectedUserId.value > 0 ? selectedUserId.value : userId.value
    const res = await axios.get(`${API}/stats/user/${targetUserId}${isAdmin.value ? `?requesterId=${userId.value}` : ''}`)
    Object.assign(stats, res.data.data)
    renderCharts()
    if (statsTimer) {
      clearInterval(statsTimer)
    }
    statsTimer = setInterval(() => {
      if (stats.inProgress > 0) {
        stats.totalSeconds += stats.inProgress
      }
    }, 1000)
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

const renderCharts = () => {
  if (pieChart.value) {
    pieInstance = echarts.init(pieChart.value)
    pieInstance.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 10 },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c}' },
        data: [
          { value: stats.pending, name: '待处理', itemStyle: { color: '#909399' } },
          { value: stats.inProgress, name: '进行中', itemStyle: { color: '#E6A23C' } },
          { value: stats.completed, name: '已完成', itemStyle: { color: '#67C23A' } }
        ]
      }]
    })
  }
  if (gaugeChart.value) {
    gaugeInstance = echarts.init(gaugeChart.value)
    gaugeInstance.setOption({
      series: [{
        type: 'gauge', startAngle: 200, endAngle: -20,
        min: 0, max: 100,
        itemStyle: { color: '#67C23A' },
        progress: { show: true, width: 18 },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 18 } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        title: { fontSize: 14, offsetCenter: [0, '60%'] },
        detail: { valueAnimation: true, fontSize: 36, offsetCenter: [0, '10%'], formatter: '{value}%' },
        data: [{ value: stats.completionRate, name: '完成率' }]
      }]
    })
  }
}

const formatTime = (seconds) => {
  if (!seconds) return '00:00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
}

const handleResize = () => { pieInstance?.resize(); gaugeInstance?.resize() }

watch([userId, isAdmin], ([newUserId, admin]) => {
  if (newUserId) {
    if (admin) {
      loadUsers()
    }
    loadStats()
  }
})

watch(selectedUserId, (newUserId) => {
  if (newUserId || newUserId === 0) {
    loadStats()
  }
})

onMounted(() => {
  if (isAdmin.value) {
    loadUsers()
  }
  loadStats()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  pieInstance?.dispose()
  gaugeInstance?.dispose()
  window.removeEventListener('resize', handleResize)
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }
})
</script>

<style scoped>
.dashboard-page { padding: 20px; max-width: 1200px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header h1 { color: #333; font-size: 24px; }
.stats-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.card { background: #fff; border-radius: 12px; padding: 24px; text-align: center; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.card-value { font-size: 32px; font-weight: bold; margin-bottom: 8px; }
.card-label { color: #999; font-size: 14px; }
.card-total .card-value { color: #409EFF; }
.card-pending .card-value { color: #909399; }
.card-progress .card-value { color: #E6A23C; }
.card-done .card-value { color: #67C23A; }
.card-time .card-value { color: #F56C6C; }
.card-rate .card-value { color: #409EFF; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-box { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.chart-box h3 { color: #333; margin-bottom: 12px; font-size: 16px; }
</style>