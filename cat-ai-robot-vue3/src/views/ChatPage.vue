<template>
  <div class="h-screen flex flex-col overflow-y-auto" ref="chatContainer">
    <!-- 聊天记录区域 -->
    <div class="flex-1 max-w-3xl mx-auto pb-24 pt-4 px-4">
        <!-- 遍历聊天记录 -->
        <template v-for="(chat, index) in chatList" :key="index">
          <!-- 用户提问消息（靠右） -->
          <div v-if="chat.role === 'user'" class="flex justify-end mb-4">
            <div class="quesiton-container">
              <p>{{ chat.content }}</p>
            </div>
          </div>

          <!-- 大模型回复消息（靠左） -->
          <div v-else class="flex mb-4">
            <!-- 头像 -->
            <div class="flex-shrink-0 mr-3">
              <div class="w-8 h-8 rounded-full flex items-center justify-center border border-gray-200">
                <SvgIcon name="deepseek-logo" customCss="w-5 h-5"></SvgIcon>
              </div>
            </div>
            <!-- 回复的内容 -->
            <div class="p-1 mb-2 max-w-[90%]">
              <StreamMarkdownRender :content="chat.content" />
            </div>
          </div>
        </template>
    </div>

    <!-- 提问输入框 -->
    <div class="sticky max-w-3xl mx-auto bg-white bottom-0 left-0 w-full">
      <!-- 设置面板 -->
      <div v-if="showSettings" class="mx-4 mb-3 p-4 bg-gray-50 rounded-2xl border border-gray-200">
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
            <label class="block text-sm font-medium text-gray-700 mb-2">创造性 ({{ chatSettings.temperature }})</label>
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
      </div>

      <div class="bg-gray-100 rounded-3xl px-4 py-3 mx-4 border border-gray-200 flex flex-col">
        <textarea 
          v-model="message" 
          placeholder="给 AI 机器人发送消息"
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
          <!-- 语音按钮 -->
          <button
              @click="toggleVoice"
              :class="isRecording ? 'bg-red-500 hover:bg-red-600' : 'bg-gray-200 hover:bg-gray-300'"
              class="flex items-center justify-center rounded-full w-8 h-8 border border-gray-300 transition-colors"
              :title="isRecording ? '停止录音' : '开始语音聊天'"
          >
            <!-- 图标：录音时显示红色方块，平时显示麦克风 -->
            <svg v-if="!isRecording" class="w-4 h-4 text-gray-600" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd"
                    d="M7 4a3 3 0 016 0v4a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 015 8a1 1 0 00-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z"
                    clip-rule="evenodd"/>
            </svg>
            <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
              <rect x="6" y="6" width="8" height="8" rx="1"/>
            </svg>
          </button>
          <!-- 设置按钮 -->
          <button 
            @click="showSettings = !showSettings"
            class="flex items-center justify-center bg-gray-200 rounded-full w-8 h-8 border border-gray-300 hover:bg-gray-300 transition-colors"
            title="聊天设置"
          >
            <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
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
</template>

<script setup>
import {ref, onBeforeUnmount, nextTick, onMounted, watch} from 'vue';
import SvgIcon from '@/components/SvgIcon.vue'
import StreamMarkdownRender from '@/components/StreamMarkdownRender.vue'
import { sendChatMessage } from '@/api/chat.js'

// 接收父组件传递的当前对话ID和历史消息
const props = defineProps({
  currentChatId: {
    type: String,
    default: null
  },
  initialMessages: {
    type: Array,
    default: () => []
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
const chatList = ref([
  { role: 'assistant', content: '我是智能 AI 助手！✨ 我可以帮你解答各种问题，无论是学习、工作，还是日常生活中的小困惑，都可以找我聊聊。有什么我可以帮你的吗？😊' }
])

// 聊天设置
const chatSettings = ref({
  modelName: 'qwen-plus',
  temperature: 0.7,
  networkSearch: false
})

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

  // 将用户发送的消息添加到 chatList 聊天列表中
  const userMessage = message.value.trim()
  chatList.value.push({ role: 'user', content: userMessage })

  // 通知父组件有新消息
  emit('new-message', { role: 'user', content: userMessage })
  
  // 等待确保有chatId
  let actualChatId = props.currentChatId
  if (!actualChatId) {
    // 等待父组件创建对话并设置chatId
    let retries = 0
    while (!actualChatId && retries < 50) { // 最多等待5秒
      await new Promise(resolve => setTimeout(resolve, 100))
      actualChatId = props.currentChatId
      retries++
    }
    if (!actualChatId) {
      throw new Error('无法获取对话ID')
    }
  }

  // 点击发送按钮后，清空输入框
  message.value = ''
  // 将输入框的高度重置
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  // 添加一个占位的回复消息
  chatList.value.push({ role: 'assistant', content: '' })
  
  isLoading.value = true
  isStreaming.value = true

  try {
    // 直接使用fetch处理SSE流
    const response = await fetch('http://localhost:8080/chat/completion', {
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
        networkSearch: chatSettings.value.networkSearch
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
        const { done, value } = await reader.read()
        
        if (done) {
          break
        }
        
        // 解码数据块
        const chunk = decoder.decode(value, { stream: true })
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
      emit('new-message', { role: 'assistant', content: responseText })
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

// 监听props变化，加载历史消息
onMounted(() => {
  if (props.initialMessages && props.initialMessages.length > 0) {
    chatList.value = [...props.initialMessages]
    nextTick(() => {
      scrollToBottom()
    })
  }
})


/* -------- 语音聊天 -------- */
const isRecording = ref(false)          // 是否正在录音

// 点击语音按钮的回调
async function toggleVoice () {
  if (isRecording.value) {
    await stopRecording()
  } else {
    await startRecording()
  }
}

// ① 开始录音（TODO：真正打开麦克风）
async function startRecording () {
  console.log('[Voice] startRecording')
  // 预留：获取麦克风权限、创建 MediaRecorder、websocket 连接等
  isRecording.value = true
}

// ② 停止录音（TODO：真正关闭麦克风并上传）
async function stopRecording () {
  console.log('[Voice] stopRecording')
  isRecording.value = false
  // 预留：拿到音频 Blob，调用 uploadAudio(blob)
}

// ③ 上传音频文件（TODO：把 Blob 传给后端 ASR）
async function uploadAudio (audioBlob) {
  console.log('[Voice] uploadAudio', audioBlob)
  // 预留：FormData 上传，拿到转写文本后自动填入 message 并触发 sendMessage
  // const text = await yourASR(audioBlob)
  // message.value = text
  // sendMessage()
}

watch(
    () => props.initialMessages,
    (newVal) => {
      chatList.value = newVal.length
          ? [...newVal]
          : [{ role: 'assistant', content: '我是智能 AI 助手！✨ 有什么可以帮你的吗？😊' }]
      nextTick(() => scrollToBottom())
    },
    { deep: true, immediate: true }   // 立即执行一次，保证第一次也生效
)
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
  max-width: calc(100% - 48px);
  position: relative;
}

/* 聊天内容区域样式 */
.overflow-y-auto {
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent; /* 自定义滚动条颜色 */
}
</style>