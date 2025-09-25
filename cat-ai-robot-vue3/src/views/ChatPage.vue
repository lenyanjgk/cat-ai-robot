<template>
  <div class="h-screen flex flex-col overflow-y-auto" ref="chatContainer">
    <!-- 聊天记录区域 -->
    <div class="flex-1 max-w-3xl mx-auto pb-24 pt-4 px-4">
      <!-- 聊天记录 -->
      <template v-for="(chat, index) in chatList" :key="index">
        <!-- 用户提问消息（靠右） -->
        <div v-if="chat.role === 'user'" class="user-message-wrapper">
          <div class="quesiton-container">
            <p>{{ chat.content }}</p>
          </div>
        </div>

        <!-- 大模型回复消息（靠左） -->
        <div v-else class="assistant-message-wrapper">
          <!-- 头像 -->
          <div class="assistant-avatar">
            <div class="w-8 h-8 rounded-full flex items-center justify-center border border-gray-200">
              <SvgIcon name="deepseek-logo" customCss="w-5 h-5"></SvgIcon>
            </div>
          </div>
          <!-- 回复的内容 -->
          <div class="assistant-message-container">
            <StreamMarkdownRender :content="chat.content"/>
          </div>
        </div>
      </template>
    </div>

    <!-- 提问输入框 -->
    <div class="input-container sticky bottom-0 left-0 right-0 bg-white">
      <div class="max-w-3xl mx-auto px-4">
        <!-- 设置面板 -->
        <div v-if="showSettings" class="mb-3 p-4 bg-gray-50 rounded-2xl border border-gray-200">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <!-- 模型选择 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">AI模型</label>
              <select v-model="chatSettings.modelName" class="w-full p-2 border border-gray-300 rounded-lg text-sm">
                <option value="qwen-plus">通义千问-Plus</option>
                <option value="qwen-turbo">通义千问-Turbo</option>
                <option value="qwen-max">通义千问-Max</option>
              </select>
            </div>

            <!-- 温度设置 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">创造性 ({{
                  chatSettings.temperature
                }})</label>
              <input
                  v-model.number="chatSettings.temperature"
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  class="w-full"
              />
              <div class="flex justify-between text-xs text-gray-500 mt-1">
                <span>保守</span>
                <span>创新</span>
              </div>
            </div>

            <!-- 联网搜索 -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">联网搜索</label>
              <div class="flex items-center">
                <input
                    v-model="chatSettings.networkSearch"
                    type="checkbox"
                    class="mr-2"
                />
                <span class="text-sm text-gray-600">启用实时搜索</span>
              </div>
            </div>
          </div>

          <!-- 角色选择区域 -->
          <div class="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <label class="text-sm font-medium text-gray-700 mr-3">AI角色:</label>
                <span v-if="selectedRole" class="text-sm text-blue-600 font-medium">
                  {{ selectedRole.name }}
                </span>
                <span v-else class="text-sm text-gray-500">未选择角色</span>
              </div>
              <!-- 只在未选择角色时显示选择按钮 -->
              <button
                  v-if="!selectedRole"
                  @click="showRoleSelector = true"
                  class="px-3 py-1 text-sm text-white bg-blue-600 hover:bg-blue-700 rounded-md transition-colors"
              >
                选择角色
              </button>
            </div>
            <p v-if="selectedRole" class="text-xs text-gray-600 mt-1">
              {{ selectedRole.introduction || '暂无简介' }}
            </p>
          </div>
        </div>

        <div class="bg-gray-100 rounded-3xl px-4 py-3 border border-gray-200 flex flex-col">
        <textarea
            v-model="message"
            placeholder="发消息或者输入话题"
            class="bg-transparent border-none outline-none w-full text-sm resize-none min-h-[24px]"
            rows="2"
            @input="autoResize"
            @keydown.enter.prevent="sendMessage"
            ref="textareaRef"
            :disabled="isLoading || isStreaming"
        >
        </textarea>

          <!-- 发送按钮和设置按钮 -->
          <div class="flex justify-between items-center mt-3">
            <!-- 设置按钮 -->
            <button
                @click="showSettings = !showSettings"
                class="flex items-center justify-center bg-gray-200 rounded-full w-8 h-8 border border-gray-300 hover:bg-gray-300 transition-colors"
                title="聊天设置"
            >
              <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
              </svg>
            </button>

            <!-- 发送按钮 -->
            <button
                @click="sendMessage"
                :disabled="!message.trim() || isLoading || isStreaming"
                class="flex items-center justify-center bg-[#4d6bfe] rounded-full w-8 h-8 border border-[#4d6bfe] hover:bg-[#3b5bef] transition-colors
            disabled:opacity-50
            disabled:cursor-not-allowed"
            >
              <SvgIcon v-if="!isLoading && !isStreaming" name="up-arrow" customCss="w-5 h-5 text-white"></SvgIcon>
              <div v-else class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            </button>
          </div>
        </div>
        <!-- 提示文字 -->
        <div class="flex items-center justify-center text-xs text-gray-400 mt-2 mb-2">
          <span v-if="isStreaming">AI正在思考中...</span>
          <span v-else>内容由 AI 生成，请仔细甄别</span>
        </div>
      </div>
    </div>

    <!-- 角色选择器组件 -->
    <RoleSelector
        v-model:open="showRoleSelector"
        :defaultRoleId="chatSettings.roleId"
        @confirm="handleRoleConfirm"
        @cancel="handleRoleCancel"
    />
  </div>
</template>

<script setup>
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import SvgIcon from '@/components/SvgIcon.vue'
import StreamMarkdownRender from '@/components/StreamMarkdownRender.vue'
import RoleSelector from '@/components/RoleSelector.vue'

// 接收父组件传递的当前对话ID和历史消息
const props = defineProps({
  currentChatId: {
    type: String,
    default: null
  },
  initialMessages: {
    type: Array,
    default: () => []
  },
  currentRole: {
    type: Object,
    default: null
  },
  createNewChat: {
    type: Function,
    required: true
  }
})

// 定义事件
const emit = defineEmits(['new-message'])

// 输入的消息
const message = ref('')

// textarea 引用
const textareaRef = ref(null);
// 聊天容器引用
const chatContainer = ref(null)

// 聊天记录
const chatList = ref([])

// 聊天设置
const chatSettings = ref({
  modelName: 'qwen-plus',
  temperature: 0.7,
  networkSearch: false,
  roleId: null // 添加roleId字段
})

// 角色选择器状态
const showRoleSelector = ref(false)
const selectedRole = ref(null)

// 加载状态
const isLoading = ref(false)
const showSettings = ref(false)

// 自动调整文本域高度
const autoResize = () => {
  const textarea = textareaRef.value;
  if (textarea) {
    // 重置高度以获取正确的滚动高度
    textarea.style.height = 'auto'

    // 计算新高度，但最大不超过 300px
    const newHeight = Math.min(textarea.scrollHeight, 300);
    textarea.style.height = newHeight + 'px';

    // 如果内容超出 300px，则启用滚动
    textarea.style.overflowY = textarea.scrollHeight > 300 ? 'auto' : 'hidden';
  }
}


// SSE 相关
let streamReader = null;
let isStreaming = ref(false)

// 发送消息
const sendMessage = async () => {
  // 校验发送的消息不能为空
  if (!message.value.trim() || isLoading.value || isStreaming.value) return

  // 校验roleId是否为空
  if (!chatSettings.value.roleId) {
    showRoleSelector.value = true
    return
  }

  // 将用户发送的消息添加到 chatList 聊天列表中
  const userMessage = message.value.trim()
  chatList.value.push({role: 'user', content: userMessage})

  // 通知父组件有新消息，获取或创建chatId
  let actualChatId = props.currentChatId
  if (!actualChatId) {
    // 创建新对话
    const messageData = {role: 'user', content: userMessage}
    const newChatId = await props.createNewChat(messageData, chatSettings.value.roleId)
    actualChatId = newChatId
  } else {
    // 现有对话，正常发送消息事件
    emit('new-message', {role: 'user', content: userMessage})
  }

  // 点击发送按钮后，清空输入框
  message.value = ''
  // 将输入框的高度重置
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  // 添加一个占位的回复消息
  chatList.value.push({role: 'assistant', content: ''})

  isLoading.value = true
  isStreaming.value = true

  try {
    // 直接使用fetch处理SSE流
    const response = await fetch('http://localhost:8081/chat/completion', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache'
      },
      body: JSON.stringify({
        message: userMessage,
        chatId: actualChatId,
        modelName: chatSettings.value.modelName,
        temperature: chatSettings.value.temperature,
        networkSearch: chatSettings.value.networkSearch,
        roleId: chatSettings.value.roleId
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }


    // 获取ReadableStream
    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    let responseText = ''
    let buffer = ''

    try {
      while (true) {
        const {done, value} = await reader.read()

        if (done) {
          break
        }

        // 解码数据块
        const chunk = decoder.decode(value, {stream: true})
        buffer += chunk
        // 处理每一行数据
        let lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留最后不完整的行

        for (let line of lines) {
          line = line.trim()
          if (!line) continue

          try {
            // 直接尝试解析JSON
            const jsonData = JSON.parse(line)
            if (jsonData && jsonData.v !== undefined) {
              responseText += jsonData.v

              // 实时更新UI
              const lastIndex = chatList.value.length - 1
              if (lastIndex >= 0) {
                chatList.value[lastIndex] = {
                  ...chatList.value[lastIndex],
                  content: responseText
                }
                // 强制Vue更新
                nextTick(() => {
                  scrollToBottom()
                })
              }
            }
          } catch (e) {
            // 尝试SSE格式：data: {...} 或 data:{...}
            if (line.startsWith('data:')) {
              let dataStr = ''
              if (line.startsWith('data: ')) {
                dataStr = line.substring(6).trim()
              } else if (line.startsWith('data:')) {
                dataStr = line.substring(5).trim()
              }

              if (dataStr && dataStr !== '[DONE]') {
                try {
                  const jsonData = JSON.parse(dataStr)
                  if (jsonData && jsonData.v !== undefined) {
                    responseText += jsonData.v

                    // 实时更新UI
                    const lastIndex = chatList.value.length - 1
                    if (lastIndex >= 0) {
                      chatList.value[lastIndex] = {
                        ...chatList.value[lastIndex],
                        content: responseText
                      }
                      // 强制Vue更新
                      nextTick(() => {
                        scrollToBottom()
                      })
                    }
                  }
                } catch (sseError) {
                  // SSE解析失败，跳过
                }
              }
            }
          }
        }
      }

      // 处理最后的缓冲区
      if (buffer.trim()) {
        try {
          const jsonData = JSON.parse(buffer.trim())
          if (jsonData && jsonData.v !== undefined) {
            responseText += jsonData.v
            const lastIndex = chatList.value.length - 1
            if (lastIndex >= 0) {
              chatList.value[lastIndex] = {
                ...chatList.value[lastIndex],
                content: responseText
              }
              nextTick(() => {
                scrollToBottom()
              })
            }
          }
        } catch (e) {
          // 最后缓冲区解析失败，跳过
        }
      }

    } catch (error) {
      console.error('读取流数据错误:', error)
      chatList.value[chatList.value.length - 1].content = '抱歉，请求出错了，请稍后重试。'
    } finally {
      reader.releaseLock()
    }

    // 通知父组件AI回复完成
    if (responseText) {
      emit('new-message', {role: 'assistant', content: responseText})
    }

  } catch (error) {
    console.error('发送消息错误:', error)
    chatList.value[chatList.value.length - 1].content = '抱歉，请求出错了，请稍后重试。'
  } finally {
    isLoading.value = false
    isStreaming.value = false
    streamReader = null
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick() // 等待 Vue.js 完成 DOM 更新
  if (chatContainer.value) { // 若容器存在
    // 将容器的滚动条位置设置到最底部
    const container = chatContainer.value;
    container.scrollTop = container.scrollHeight;
  }
}

// 关闭流式连接
const closeStream = () => {
  if (streamReader) {
    streamReader.cancel()
    streamReader = null
  }
  isStreaming.value = false
}

// 组件卸载时自动关闭连接
onBeforeUnmount(() => {
  closeStream()
})

// 角色选择确认处理
const handleRoleConfirm = (role) => {
  selectedRole.value = role
  chatSettings.value.roleId = role.id
  showRoleSelector.value = false

  // 角色选择完成后，重新发送消息
  nextTick(() => {
    sendMessage()
  })
}

// 角色选择取消处理
const handleRoleCancel = () => {
  showRoleSelector.value = false
}


// 监听props变化，加载历史消息

watch(() => props.initialMessages, (newMessages) => {
  if (newMessages && newMessages.length > 0) {
    chatList.value = [...newMessages]
    nextTick(() => {
      scrollToBottom()
    })
  } else {
    // 如果没有消息，显示空的聊天列表并重置状态
    chatList.value = []
    // 重置角色选择
    chatSettings.value.roleId = null
    selectedRole.value = null
  }
}, {immediate: true, deep: true})

// 监听当前对话ID变化，重置状态
watch(() => props.currentChatId, (newChatId) => {
  if (!newChatId) {
    // 新建对话时重置角色
    chatSettings.value.roleId = null
    selectedRole.value = null
  }
})

// 监听角色信息变化，自动设置角色
watch(() => props.currentRole, (newRole) => {
  if (newRole) {
    // 当有角色信息时，自动设置到聊天设置中
    chatSettings.value.roleId = newRole.id
    selectedRole.value = newRole
    console.log('自动设置角色:', newRole)
  } else {
    // 没有角色信息时重置
    chatSettings.value.roleId = null
    selectedRole.value = null
  }
}, {immediate: true})

onMounted(() => {
  if (props.initialMessages && props.initialMessages.length > 0) {
    chatList.value = [...props.initialMessages]
    nextTick(() => {
      scrollToBottom()
    })
  }
})
</script>

<style scoped>
.quesiton-container {
  font-size: 16px;
  line-height: 28px;
  color: #262626;
  padding: calc((44px - 28px) / 2) 20px;
  box-sizing: border-box;
  white-space: pre-wrap;
  word-break: break-word;
  background-color: #eff6ff;
  border-radius: 14px;
  max-width: calc(100% - 160px); /* 考虑左右固定间距 */
  position: relative;
}

/* 用户消息容器样式 */
.user-message-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
  margin-left: 80px; /* 桌面端固定左侧间距 */
}

/* AI助手消息容器样式 */
.assistant-message-wrapper {
  display: flex;
  margin-bottom: 1rem;
  margin-right: 80px; /* 桌面端固定右侧间距 */
  align-items: flex-start;
}

/* 移动端响应式间距 */
@media (max-width: 768px) {
  .user-message-wrapper {
    margin-left: 40px; /* 移动端减少左侧间距 */
  }

  .assistant-message-wrapper {
    margin-right: 40px; /* 移动端减少右侧间距 */
  }
}

@media (max-width: 480px) {
  .user-message-wrapper {
    margin-left: 20px; /* 小屏幕设备进一步减少间距 */
  }

  .assistant-message-wrapper {
    margin-right: 20px; /* 小屏幕设备进一步减少间距 */
  }
}

.assistant-avatar {
  flex-shrink: 0;
  margin-right: 12px;
}

.assistant-message-container {
  flex: 1;
  padding: 8px 0;
  min-width: 0; /* 允许内容收缩 */
  max-width: calc(100% - 52px); /* 减去头像和间距的宽度 */
}


/* 输入框容器样式 */
.input-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding: 16px 0;
}


/* 聊天内容区域样式 */
.overflow-y-auto {
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent; /* 自定义滚动条颜色 */
}
</style>