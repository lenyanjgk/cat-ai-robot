<template>
    <div class="h-screen flex overflow-hidden overflow-x-hidden">
        <!-- 左边栏 -->
        <Sidebar
            ref="sidebarRef"
            :sidebarOpen="sidebarOpen"
            :currentChatId="currentChatId"
            @toggle-sidebar="toggleSidebar"
            @select-chat="handleSelectChat"
            @new-chat="handleNewChat"
            @chat-deleted="handleChatDeleted"
            />
        
        <!-- 主内容区域 -->
        <div :class="sidebarOpen ? 'ml-64' : 'ml-0'" class="flex-1 transition-all duration-300">
          <ChatPage
              :key="currentChatId || 'empty'"
              :currentChatId="currentChatId"
              :initialMessages="currentChatMessages"
              @new-message="handleNewMessage"
          />
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import Sidebar from '@/components/Sidebar.vue'
import ChatPage from '@/views/ChatPage.vue'
import { newChat, getChatMessages } from '@/api/chat.js'


// 左边栏状态
const sidebarOpen = ref(true)
// 当前选中的对话ID
const currentChatId = ref(null)
// 当前对话的消息列表
const currentChatMessages = ref([])
// 侧边栏引用
const sidebarRef = ref(null)

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value;
}
const props = defineProps({
  currentChatId: String,
  initialMessages: { type: Array, default: () => [] }
})

// 选择对话
const handleSelectChat = async (chat) => {
  try {
    currentChatId.value = chat.uuid
    
    // 加载对话的历史消息
    const response = await getChatMessages(chat.uuid, 1, 100)
    if (response.success && response.data) {
      currentChatMessages.value = response.data.map(msg => ({
        role: msg.role,
        content: msg.content
      }))
    } else {
      currentChatMessages.value = []
    }
  } catch (error) {
    console.error('加载对话消息失败:', error)
    message.error('加载对话消息失败')
    currentChatMessages.value = []
  }
}

// 新建对话（现在能收到子组件抛出的角色对象）
const handleNewChat = async (role) => {
  console.log('[Parent] 选中角色', role)          // ★ 调试用，确认收到
  currentChatId.value = null
  currentChatMessages.value = [{
    role: 'assistant',
    content: `我是 ${role.name}！✨ 有什么可以帮你的吗？😊`
  }]

  // 如果希望打开对话时就带着角色 key 创建，可在这里多传参数
  // 你的 newChat 接口支持额外字段时直接带上去即可
  // await newChat(`角色:${role.key}`)   // 按需要自行调整
}

// 处理新消息（用户发送或AI回复）
const handleNewMessage = async (messageData) => {
  try {
    // 如果没有当前对话ID，说明是新对话，需要先创建
    if (!currentChatId.value && messageData.role === 'user') {
      console.log('创建新对话，消息:', messageData.content)
      const response = await newChat(messageData.content)
      if (response.success) {
        currentChatId.value = response.data.uuid
        console.log('对话创建成功，ID:', currentChatId.value)
        // 刷新侧边栏的对话列表
        if (sidebarRef.value) {
          sidebarRef.value.loadChatHistory()
        }
        return response.data.uuid // 返回新创建的chatId
      } else {
        message.error('创建对话失败')
        throw new Error('创建对话失败')
      }
    }
    return currentChatId.value
  } catch (error) {
    console.error('处理新消息失败:', error)
    message.error('处理消息失败')
    throw error
  }
}

// 对话被删除后的处理
const handleChatDeleted = () => {
  // 如果删除的是当前对话，重置状态
  currentChatId.value = null
  currentChatMessages.value = [{
    role: 'assistant', 
    content: '我是cat智能 AI 助手！✨ 我可以帮你解答各种问题，无论是学习、工作，还是日常生活中的小困惑，都可以找我聊聊。有什么我可以帮你的吗？😊'
  }]
}



</script>

