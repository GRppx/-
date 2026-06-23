import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'

const API = 'http://localhost:8080/api'
const USER_STORAGE_KEY = 'smarttodo-user'

const loadUserFromStorage = () => {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    console.error('读取本地用户信息失败', e)
    return null
  }
}

const saveUserToStorage = (user: { id: number, username: string, nickname: string, role: string }) => {
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
}

const clearUserStorage = () => {
  localStorage.removeItem(USER_STORAGE_KEY)
}

export const useUserStore = defineStore('user', () => {
  const storedUser = loadUserFromStorage()
  const id = ref<number>(storedUser?.id ?? 0)
  const username = ref(storedUser?.username ?? '')
  const nickname = ref(storedUser?.nickname ?? '')
  const role = ref(storedUser?.role ?? 'USER')

  const isLoggedIn = () => id.value > 0

  const login = async (uname: string, password: string) => {
    const res = await axios.post(`${API}/users/login`, { username: uname, password })
    if (res.data.code === 200) {
      id.value = res.data.data.id
      username.value = res.data.data.username
      nickname.value = res.data.data.nickname || ''
      role.value = res.data.data.role
      saveUserToStorage({ id: id.value, username: username.value, nickname: nickname.value, role: role.value })
      return true
    }
    return false
  }

  const logout = () => {
    id.value = 0
    username.value = ''
    nickname.value = ''
    role.value = 'USER'
    clearUserStorage()
  }

  return { id, username, nickname, role, isLoggedIn, login, logout }
})