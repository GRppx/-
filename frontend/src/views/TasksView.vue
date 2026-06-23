<template>
  <div class="tasks-page">
    <!-- 顶部导航栏 -->
    <div class="header">
      <h1>智能待办助手</h1>
      <div class="header-right">
        <span class="username">{{ userStore.nickname || userStore.username }}</span>
        <el-tag type="success" size="small" class="role-tag">{{ isAdmin ? '管理员' : '普通用户' }}</el-tag>
        <el-button type="danger" size="small" @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="role-note">
      当前为 <strong>{{ isAdmin ? '管理员' : '普通用户' }}</strong>，
      {{ isAdmin ? '可查看并管理所有用户任务' : '仅可查看和管理自己的任务' }}。
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
          <el-button type="primary" @click="showAddDialog">新建任务</el-button>
      <el-button type="success" @click="$router.push('/dashboard')">数据看板</el-button>
      <el-button v-if="isAdmin" type="warning" size="mini" @click="toggleShowAllTasks" style="margin-left:12px">
        {{ showAllTasks ? '只看我的任务' : '查看全部任务' }}
      </el-button>
      <el-select v-if="isAdmin" v-model="selectedUserId" placeholder="查看指定用户" clearable style="width:200px;margin-left:12px" @change="loadTasks" :disabled="showAllTasks || loadingUsers">
        <el-option v-for="user in users" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="筛选状态" clearable style="width:150px;margin-left:12px" @change="loadTasks">
        <el-option label="全部" value="" />
        <el-option label="待处理" value="PENDING" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
    </div>

    <!-- 任务列表 -->
    <el-table :data="filteredTasks" stripe style="width:100%" empty-text="暂无任务，点击新建任务开始吧！" row-key="id">
      <el-table-column prop="title" label="任务名称" min-width="180">
        <template #default="{ row }">
          <div>{{ row.title || '未命名任务' }}</div>
          <div v-if="row.adminCreated" style="margin-top:6px">
            <el-tag type="success" size="mini">管理员分配</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="100">
        <template #default="{ row }">
          <el-tag :type="priorityTag(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止日期" width="120">
        <template #default="{ row }">
          {{ row.deadline ? formatDate(row.deadline) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="completedAt" label="完成时间" width="160">
        <template #default="{ row }">
          {{ row.completedAt ? formatDateTime(row.completedAt) : '-' }}
        </template>
      </el-table-column>
      <el-table-column v-if="isAdmin" prop="ownerName" label="任务归属" width="140">
        <template #default="{ row }">
          {{ row.ownerName || row.userId }}
        </template>
      </el-table-column>
      <el-table-column label="计时" width="100">
        <template #default="{ row }">
          <span>{{ formatTime(getRowSeconds(row)) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <template v-if="canCompleteOnly(row)">
            <el-button size="small" type="success" @click="completeTask(row)">已完成</el-button>
          </template>
          <template v-else>
            <el-button v-if="canToggleTimer(row)" size="small" :type="row.status === 'IN_PROGRESS' ? 'warning' : 'success'" @click="toggleTimer(row)">
              {{ row.status === 'IN_PROGRESS' ? '暂停' : '开始' }}
            </el-button>
            <el-button v-if="canEditTask(row)" size="small" type="primary" @click="showEditDialog(row)">编辑</el-button>
            <el-button v-if="canChangeStatus(row)" size="small" type="info" @click="showStatusDialog(row)">状态</el-button>
            <el-button v-if="canDeleteTask(row)" size="small" type="danger" @click="deleteTask(row.id)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑任务弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新建任务'" width="500px">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务名称">
          <el-input v-model="taskForm.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority" style="width:100%">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="任务归属">
          <el-select v-model="taskForm.userId" placeholder="选择用户" style="width:100%">
            <el-option v-for="user in users" :key="user.id" :label="user.nickname || user.username" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="taskForm.deadline" type="datetime" placeholder="选择截止日期" style="width:100%" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" title="选择任务状态" width="400px">
      <el-form label-width="80px">
        <el-form-item label="状态">
          <el-radio-group v-model="statusDialogStatus">
            <el-radio-button label="PENDING">待处理</el-radio-button>
            <el-radio-button label="IN_PROGRESS">进行中</el-radio-button>
            <el-radio-button label="COMPLETED">已完成</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmChangeStatus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const userId = computed(() => userStore.id)
const isAdmin = computed(() => userStore.role === 'ADMIN')
const tasks = ref([])
const statusFilter = ref('')
const showAllTasks = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const editTask = ref(null)
const timerInterval = ref(null)
const displayInterval = ref(null)
const timerTaskId = ref(null)
const timerSeconds = ref(0)
const timerStartAt = ref(null)
const statusDialogVisible = ref(false)
const statusDialogStatus = ref('PENDING')
const statusDialogTask = ref(null)

const saveActiveTimer = (id, startedAt) => {
  localStorage.setItem(getTimerKey(), JSON.stringify({ id, startedAt }))
}

const clearActiveTimer = () => {
  localStorage.removeItem(getTimerKey())
}

const loadActiveTimer = () => {
  try {
    const raw = localStorage.getItem(getTimerKey())
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    console.error('读取活动计时器失败', e)
    return null
  }
}

const formatLocalDateTime = (date = new Date()) => {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const getSecondsSince = (startAt) => {
  if (!startAt) return 0
  const startDate = startAt instanceof Date ? startAt : parseDateTime(startAt)
  if (!startDate) return 0
  const diff = Math.floor((Date.now() - startDate.getTime()) / 1000)
  return diff > 0 ? diff : 0
}

const startTimerInterval = () => {
  if (timerInterval.value) return
  timerSeconds.value = getSecondsSince(timerStartAt.value)
  timerInterval.value = setInterval(() => {
    timerSeconds.value = getSecondsSince(timerStartAt.value)
  }, 1000)
  if (!displayInterval.value) {
    displayInterval.value = setInterval(() => {
      tasks.value = [...tasks.value]
    }, 1000)
  }
}

const users = ref([])
const selectedUserId = ref(null)
const loadingUsers = ref(false)

const taskForm = reactive({
  title: '',
  description: '',
  priority: 'MEDIUM',
  deadline: '',
  userId: null
})

const API = 'http://localhost:8080/api'
const getTimerKey = () => `smarttodo-active-timer-${userId.value}`

const filteredTasks = computed(() => {
  if (!statusFilter.value) return tasks.value
  return tasks.value.filter(t => t.status === statusFilter.value)
})

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

// 加载任务列表
const loadTasks = async () => {
  try {
    const cachedTimer = loadActiveTimer()
    const url = isAdmin.value && showAllTasks.value
      ? `${API}/tasks/all?requesterId=${userId.value}`
      : (isAdmin.value && selectedUserId.value
        ? `${API}/tasks/user/${selectedUserId.value}?requesterId=${userId.value}`
        : `${API}/tasks/user/${userId.value}?requesterId=${userId.value}`)
    const res = await axios.get(url)
    tasks.value = res.data.data || []
    let activeTask = null

    if (cachedTimer) {
      activeTask = tasks.value.find(t => t.id === cachedTimer.id && t.status === 'IN_PROGRESS')
      if (activeTask && !activeTask.startedAt) {
        activeTask.startedAt = cachedTimer.startedAt
      }
    }

    if (!activeTask) {
      activeTask = tasks.value.find(t => t.status === 'IN_PROGRESS')
    }

    if (activeTask) {
      if (!activeTask.startedAt && cachedTimer && cachedTimer.id === activeTask.id) {
        activeTask.startedAt = cachedTimer.startedAt
      }
      if (timerTaskId.value !== activeTask.id || !timerInterval.value) {
        stopTimer()
        restoreActiveTimer(activeTask)
      }
      saveActiveTimer(activeTask.id, activeTask.startedAt)
      return
    }

    stopTimer()
    clearActiveTimer()
  } catch (e) {
    ElMessage.error('加载任务失败')
  }
}

// 显示新建弹窗
const showAddDialog = () => {
  isEdit.value = false
  editId.value = null
  taskForm.title = ''
  taskForm.description = ''
  taskForm.priority = 'MEDIUM'
  taskForm.deadline = ''
  taskForm.userId = isAdmin.value ? (selectedUserId.value || userId.value) : userId.value
  dialogVisible.value = true
}

// 显示编辑弹窗
const showEditDialog = (row) => {
  isEdit.value = true
  editId.value = row.id
  editTask.value = row
  taskForm.title = row.title
  taskForm.description = row.description
  taskForm.priority = row.priority
  taskForm.deadline = row.deadline ? row.deadline.substring(0, 19) : ''
  taskForm.userId = row.userId || userId.value
  dialogVisible.value = true
}

const toggleShowAllTasks = () => {
  showAllTasks.value = !showAllTasks.value
  loadTasks()
}

// 提交任务
const submitTask = async () => {
  if (!taskForm.title) {
    ElMessage.warning('请输入任务名称')
    return
  }
  try {
    if (isEdit.value) {
      await axios.put(`${API}/tasks/${editId.value}?requesterId=${userId.value}`, {
        title: taskForm.title,
        description: taskForm.description,
        priority: taskForm.priority,
        deadline: taskForm.deadline || null,
        status: editTask.value?.status,
        startedAt: editTask.value?.startedAt
      })
      ElMessage.success('更新成功')
      editTask.value = null
    } else {
      const requestBody = {
        title: taskForm.title,
        description: taskForm.description,
        priority: taskForm.priority,
        deadline: taskForm.deadline || null,
        userId: isAdmin.value && taskForm.userId ? taskForm.userId : userId.value
      }
      if (isAdmin.value && taskForm.userId && taskForm.userId !== userId.value) {
        requestBody.status = 'IN_PROGRESS'
        requestBody.startedAt = formatLocalDateTime()
      }
      await axios.post(`${API}/tasks?requesterId=${userId.value}`, requestBody)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTasks()
  } catch (e) {
    const msg = e?.response?.data?.message || '操作失败'
    ElMessage.error(msg)
  }
}

// 删除任务
const deleteTask = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？', '提示', { type: 'warning' })
    await axios.delete(`${API}/tasks/${id}?requesterId=${userId.value}`)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 切换任务状态
const showStatusDialog = (row) => {
  statusDialogTask.value = row
  statusDialogStatus.value = row.status
  statusDialogVisible.value = true
}

const confirmChangeStatus = async () => {
  if (!statusDialogTask.value) return
  const row = statusDialogTask.value
  const newStatus = statusDialogStatus.value

  if (row.status === 'IN_PROGRESS' && timerTaskId.value === row.id && newStatus !== 'IN_PROGRESS') {
    const secondsToSave = timerSeconds.value
    stopTimer()
    clearActiveTimer()
    try {
      await axios.post(`${API}/tasks/${row.id}/time?requesterId=${userId.value}`, { seconds: secondsToSave })
      await axios.put(`${API}/tasks/${row.id}?requesterId=${userId.value}`, { ...row, status: newStatus, startedAt: null })
      ElMessage.success('状态已更新，计时已保存')
      statusDialogVisible.value = false
      loadTasks()
    } catch (e) {
      ElMessage.error('更新状态失败')
    }
    return
  }

  if (newStatus === 'IN_PROGRESS') {
    const previousStatus = row.status
    const previousStartedAt = row.startedAt

    if (timerInterval.value && timerTaskId.value && timerTaskId.value !== row.id) {
      const previousSeconds = timerSeconds.value
      const previousTaskId = timerTaskId.value
      stopTimer()
      try {
        await axios.post(`${API}/tasks/${previousTaskId}/time?requesterId=${userId.value}`, { seconds: previousSeconds })
        await axios.put(`${API}/tasks/${previousTaskId}?requesterId=${userId.value}`, { status: 'PENDING', startedAt: null })
      } catch (e) {
        console.error('保存上一个计时任务失败', e)
      }
      clearActiveTimer()
    }

    const newStartAt = formatLocalDateTime()
    row.status = 'IN_PROGRESS'
    row.startedAt = newStartAt
    timerTaskId.value = row.id
    timerStartAt.value = newStartAt
    timerSeconds.value = 0
    startTimerInterval()
    saveActiveTimer(row.id, newStartAt)

    try {
      await axios.put(`${API}/tasks/${row.id}?requesterId=${userId.value}`, { ...row, status: 'IN_PROGRESS', startedAt: newStartAt })
      ElMessage.success('状态已更新，开始计时')
      statusDialogVisible.value = false
      loadTasks()
    } catch (e) {
      stopTimer()
      clearActiveTimer()
      row.status = previousStatus
      row.startedAt = previousStartedAt || null
      ElMessage.error('更新状态失败')
    }
    return
  }

  try {
    await axios.put(`${API}/tasks/${row.id}?requesterId=${userId.value}`, { ...row, status: newStatus })
    ElMessage.success('状态已更新')
    statusDialogVisible.value = false
    loadTasks()
  } catch (e) {
    ElMessage.error('更新状态失败')
  }
}

// 计时器：开始/暂停
const completeTask = async (row) => {
  if (row.status !== 'IN_PROGRESS') {
    return
  }
  const secondsToSave = getActiveIntervalSeconds(row)
  try {
    if (timerTaskId.value === row.id) {
      stopTimer()
      clearActiveTimer()
    }
    await axios.post(`${API}/tasks/${row.id}/complete?requesterId=${userId.value}`, { seconds: secondsToSave })
    ElMessage.success('任务已完成，计时已保存')
    loadTasks()
  } catch (e) {
    const msg = e?.response?.data?.message || '完成任务失败'
    ElMessage.error(msg)
  }
}

const toggleTimer = async (row) => {
  if (canCompleteOnly(row)) {
    return
  }
  if (row.status === 'IN_PROGRESS') {
    const secondsToSave = getActiveIntervalSeconds(row)
    stopTimer()
    clearActiveTimer()
    try {
      await axios.post(`${API}/tasks/${row.id}/time?requesterId=${userId.value}`, { seconds: secondsToSave })
      await axios.put(`${API}/tasks/${row.id}?requesterId=${userId.value}`, { ...row, status: 'PENDING', startedAt: null })
      ElMessage.success('已暂停计时并保存进度')
      loadTasks()
    } catch (e) {
      ElMessage.error('保存计时失败')
    }
    return
  }

  if (timerInterval.value) {
    const previousTaskId = timerTaskId.value
    const previousSeconds = timerSeconds.value
    stopTimer()
    if (previousTaskId) {
      try {
        await axios.post(`${API}/tasks/${previousTaskId}/time?requesterId=${userId.value}`, { seconds: previousSeconds })
        await axios.put(`${API}/tasks/${previousTaskId}?requesterId=${userId.value}`, { status: 'PENDING', startedAt: null })
        const previousTask = tasks.value.find(t => t.id === previousTaskId)
        if (previousTask) {
          previousTask.status = 'PENDING'
          previousTask.startedAt = null
          previousTask.totalSeconds = (previousTask.totalSeconds || 0) + previousSeconds
        }
      } catch (e) {
        console.error('保存上一个计时任务失败', e)
      }
      clearActiveTimer()
    }
  }

  const newStartAt = formatLocalDateTime()
  row.status = 'IN_PROGRESS'
  row.startedAt = newStartAt
  timerTaskId.value = row.id
  timerStartAt.value = newStartAt
  timerSeconds.value = 0
  startTimerInterval()

  saveActiveTimer(row.id, newStartAt)

  try {
    await axios.put(`${API}/tasks/${row.id}?requesterId=${userId.value}`, { ...row, status: 'IN_PROGRESS', startedAt: newStartAt })
    ElMessage.success('任务已开始')
  } catch (e) {
    stopTimer()
    clearActiveTimer()
    row.status = 'PENDING'
    row.startedAt = null
    ElMessage.error('开始任务失败')
  }
}

const parseDateTime = (value) => {
  if (!value) return null
  if (value instanceof Date) return value

  const raw = String(value).trim()
  let normalized = raw.replace(' ', 'T')
  normalized = normalized.replace(/\.[0-9]+$/, '')

  const parsed = new Date(normalized)
  if (!Number.isNaN(parsed.getTime())) {
    return parsed
  }

  const parts = normalized.split(/[^0-9]/).filter(Boolean)
  if (parts.length >= 6) {
    return new Date(
      Number(parts[0]),
      Number(parts[1]) - 1,
      Number(parts[2]),
      Number(parts[3]),
      Number(parts[4]),
      Number(parts[5])
    )
  }
  return null
}

const getStartedDate = (row) => {
  if (!row || !row.startedAt) return null
  let date = parseDateTime(row.startedAt)
  if (date) return date

  const cachedTimer = loadActiveTimer()
  if (cachedTimer && cachedTimer.id === row.id) {
    date = parseDateTime(cachedTimer.startedAt)
    if (date) return date
  }
  return null
}

  const getStartedSeconds = (row) => {
    const startDate = getStartedDate(row)
    if (!startDate) return 0
    const diff = Math.floor((Date.now() - startDate.getTime()) / 1000)
    return diff > 0 ? diff : 0
  }

  const getActiveIntervalSeconds = (row) => {
    if (timerTaskId.value === row.id) {
      return timerSeconds.value
    }
    if (row.status === 'IN_PROGRESS') {
      return Math.max(0, getRowSeconds(row) - (row.totalSeconds || 0))
    }
    return 0
  }

  const getRowSeconds = (row) => {
    if (timerTaskId.value === row.id && timerStartAt.value) {
      return (row.totalSeconds || 0) + timerSeconds.value
    }
    if (row.status === 'IN_PROGRESS' && row.startedAt) {
      return (row.totalSeconds || 0) + getStartedSeconds(row)
    }
    return row.totalSeconds || 0
  }

const isAdminCreatedTask = (row) => row?.adminCreated === true

const canCompleteOnly = (row) => {
  return !isAdmin.value && isAdminCreatedTask(row) && row.userId === userId.value && row.status === 'IN_PROGRESS'
}

const canManageAdminCreatedTask = (row) => {
  return isAdmin.value || !isAdminCreatedTask(row)
}

const canDeleteTask = (row) => {
  return canManageAdminCreatedTask(row)
}

const canEditTask = (row) => {
  return canManageAdminCreatedTask(row)
}

const canChangeStatus = (row) => {
  return canManageAdminCreatedTask(row)
}

const canToggleTimer = (row) => {
  return canManageAdminCreatedTask(row)
}

const stopTimer = () => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
    timerInterval.value = null
  }
  if (displayInterval.value) {
    clearInterval(displayInterval.value)
    displayInterval.value = null
  }
  timerTaskId.value = null
  timerSeconds.value = 0
  timerStartAt.value = null
}

const restoreActiveTimer = (activeTask) => {
  if (!activeTask) return
  timerTaskId.value = activeTask.id
  const cachedTimer = loadActiveTimer()
  timerStartAt.value = activeTask.startedAt || (cachedTimer && cachedTimer.id === activeTask.id ? cachedTimer.startedAt : null)
  timerSeconds.value = getSecondsSince(timerStartAt.value)
  startTimerInterval()
}

// 退出登录
const logout = () => {
  userStore.logout()
  router.push('/login')
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds) return '00:00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 格式化日期
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

// 优先级标签
const priorityTag = (p) => ({ LOW: 'info', MEDIUM: 'warning', HIGH: 'danger' }[p] || 'info')
const priorityLabel = (p) => ({ LOW: '低', MEDIUM: '中', HIGH: '高' }[p] || '中')

// 状态标签
const statusTag = (s) => ({ PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success' }[s] || 'info')
const statusLabel = (s) => ({ PENDING: '待处理', IN_PROGRESS: '进行中', COMPLETED: '已完成' }[s] || '待处理')

onMounted(() => {
  if (isAdmin.value) {
    loadUsers()
  }
  loadTasks()
})

watch([userId, isAdmin], ([newId, admin]) => {
  if (newId) {
    if (admin) {
      loadUsers()
    }
    loadTasks()
  } else {
    tasks.value = []
    users.value = []
    stopTimer()
    clearActiveTimer()
  }
})

watch(selectedUserId, (newUserId) => {
  if (newUserId && !showAllTasks.value) {
    loadTasks()
  }
})

watch(showAllTasks, (showAll) => {
  if (showAll) {
    selectedUserId.value = 0
  }
  loadTasks()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.tasks-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header h1 {
  color: #333;
  font-size: 24px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.username {
  color: #666;
}
.toolbar {
  margin-bottom: 20px;
}
</style>