<template>
  <div class="h-screen flex overflow-hidden overflow-x-hidden">
    <!-- 左边栏 -->
    <Sidebar
        ref="sidebarRef"
        :sidebarOpen="sidebarOpen"
        :currentChatId="currentChatId"
        :currentView="currentView"
        @toggle-sidebar="toggleSidebar"
        @select-chat="handleSelectChat"
        @new-chat="handleNewChat"
        @chat-deleted="handleChatDeleted"
        @switch-view="switchView"
    />

    <!-- 主内容区域 -->
    <div :class="sidebarOpen ? 'ml-64' : 'ml-0'" class="flex-1 transition-all duration-300">
      <!-- 聊天页面 -->
      <ChatPage
          v-if="currentView === 'chat'"
          :currentChatId="currentChatId"
          :initialMessages="currentChatMessages"
          :currentRole="currentChatRole"
          :createNewChat="handleCreateNewChat"
          @new-message="handleNewMessage"
      />

      <!-- 角色管理页面 -->
      <RoleManagementView
          v-if="currentView === 'role-management'"
          ref="roleManagementRef"
      />
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {message} from 'ant-design-vue'
import Sidebar from '@/components/Sidebar.vue'
import ChatPage from '@/views/ChatPage.vue'
import RoleManagementView from '@/components/RoleManagementView.vue'
import {getChatMessages, getChatRoleInfo, newChat} from '@/api/chat.js'

// 左边栏状态
const sidebarOpen = ref(true)
// 当前视图：'chat' 或 'role-management'
const currentView = ref('chat')
// 当前选中的对话ID
const currentChatId = ref(null)
// 当前对话的消息列表
const currentChatMessages = ref([])
// 当前对话的角色信息
const currentChatRole = ref(null)
// 侧边栏引用
const sidebarRef = ref(null)
// 角色管理组件引用
const roleManagementRef = ref(null)

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value;
}

// 切换视图
const switchView = (view) => {
  currentView.value = view
  if (view === 'role-management' && roleManagementRef.value) {
    // 切换到角色管理时刷新数据
    roleManagementRef.value.loadRoles()
  }
}

// 选择对话
const handleSelectChat = async (chat) => {
  try {
    currentChatId.value = chat.uuid

    // 并行加载对话的历史消息和角色信息
    const [messagesResponse, roleResponse] = await Promise.all([
      getChatMessages(chat.uuid, 1, 100),
      getChatRoleInfo(chat.uuid)
    ])

    // 处理历史消息
    if (messagesResponse.success && messagesResponse.data) {
      currentChatMessages.value = messagesResponse.data.map(msg => ({
        role: msg.role,
        content: msg.content,
        audioUrl:msg.audioUrl
      }))
    } else {
      currentChatMessages.value = []
    }

    // 处理角色信息
    if (roleResponse.success && roleResponse.data) {
      currentChatRole.value = roleResponse.data
      console.log('当前对话角色信息:', roleResponse.data)
    } else {
      currentChatRole.value = null
      console.log('未找到角色信息')
    }

  } catch (error) {
    console.error('加载对话信息失败:', error)
    message.error('加载对话信息失败')
    currentChatMessages.value = []
    currentChatRole.value = null
  }
}

// 新建对话
const handleNewChat = async () => {
  // 重置当前状态
  currentChatId.value = null
  currentChatMessages.value = [] // 清空消息列表
  currentChatRole.value = null // 清空角色信息
}

// 创建新对话的专用函数
const handleCreateNewChat = async (messageData, roleId) => {
  try {
    console.log('创建新对话，消息:', messageData.content, '角色ID:', roleId)
    const response = await newChat(messageData.content, roleId)
    if (response.success) {
      currentChatId.value = response.data.uuid
      console.log('对话创建成功，ID:', currentChatId.value)
      // 刷新侧边栏的对话列表
      if (sidebarRef.value) {
        sidebarRef.value.loadChatHistory()
      }
      return response.data.uuid
    } else {
      message.error('创建对话失败')
      throw new Error('创建对话失败')
    }
  } catch (error) {
    console.error('创建新对话失败:', error)
    message.error('创建对话失败')
    throw error
  }
}

// 处理新消息（用户发送或AI回复）
const handleNewMessage = async (messageData) => {
  // 这个函数现在主要用于处理AI回复等其他消息
  console.log('处理消息:', messageData)
}

// 对话被删除后的处理
const handleChatDeleted = () => {
  // 如果删除的是当前对话，重置状态
  currentChatId.value = null
  currentChatMessages.value = [] // 清空消息列表
  currentChatRole.value = null // 清空角色信息
}
</script>