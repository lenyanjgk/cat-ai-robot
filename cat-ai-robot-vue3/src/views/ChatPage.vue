<template>
  <div class="h-screen flex flex-col overflow-y-auto" ref="chatContainer">
    <!-- 聊天记录区域 -->
    <div class="flex-1 w-full mx-auto pb-24 pt-8 px-4">
      <!-- 欢迎界面 -->
      <div v-if="chatList.length === 0" class="welcome-screen">
        <!-- 动态背景装饰 -->
        <div class="welcome-bg-decoration">
          <div class="floating-shape shape-1"></div>
          <div class="floating-shape shape-2"></div>
          <div class="floating-shape shape-3"></div>
          <div class="floating-shape shape-4"></div>
        </div>

        <!-- 主要内容 -->
        <div class="welcome-content">
          <!-- 未选择角色时的欢迎 -->
          <div v-if="!selectedRole" class="role-intro-section">
            <div class="role-avatar-large">
              <div class="avatar-glow"></div>
              <div class="avatar-container">
                <SvgIcon name="cat" customCss="w-24 h-24 text-blue-500"></SvgIcon>
              </div>
            </div>
            
            <div class="role-info-text">
              <h1 class="role-welcome-title">
                欢迎使用 <span class="role-name-highlight">Cat AI机器人</span>
              </h1>
              <p class="role-description">
                智能AI助手，为您提供专业、有趣的对话体验
              </p>
              <div class="select-role-prompt">
                <button @click="showRoleSelector = true" class="select-role-btn">
                  <MessageOutlined class="mr-2" />
                  选择AI角色开始对话
                </button>
              </div>
            </div>
          </div>

          <!-- 已选择角色时的欢迎 -->
          <div v-else class="role-intro-section">
            <div class="role-avatar-large">
              <div class="avatar-glow"></div>
              <div class="avatar-container">
                <img 
                  v-if="selectedRole.avatarUrl" 
                  :src="convertMinioUrl(selectedRole.avatarUrl)" 
                  :alt="selectedRole.name"
                  class="role-avatar-image"
                />
                <SvgIcon v-else name="girl" customCss="w-24 h-24 text-blue-500"></SvgIcon>
              </div>
            </div>
            
            <div class="role-info-text">
              <h1 class="role-welcome-title">
                你好！我是 <span class="role-name-highlight">{{ selectedRole.name }}</span>
              </h1>
              <p class="role-description">
                {{ selectedRole.introduction || '我是您的AI助手，很高兴为您服务！' }}
              </p>
            </div>
          </div>


          <!-- 建议对话 -->
          <div v-if="selectedRole" class="suggestions-section">
            <h3 class="suggestions-title">试试这些话题</h3>
            <div class="suggestion-chips">
              <button 
                v-for="(suggestion, index) in welcomeSuggestions" 
                :key="index"
                @click="handleSuggestionClick(suggestion)"
                class="suggestion-chip"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 聊天记录 -->
      <template v-for="(chat, index) in chatList" :key="index">
        <!-- 用户提问消息（靠右） -->
        <div v-if="chat.role === 'user'" class="user-message-wrapper chat-bubble">
          <div class="quesiton-container">
            <div v-if="chat.audioUrl">
              <audio controls :src="convertAudioUrl(chat.audioUrl)" class="audio-player"></audio>
            </div>
            <p>{{ chat.content }}</p>
          </div>
          <div class="user-avatar">
            <SvgIcon name="cat" customCss="w-12 h-12"></SvgIcon>
          </div>
        </div>

        <!-- 大模型回复消息（靠左） -->
        <div v-else class="assistant-message-wrapper chat-bubble">
          <div class="assistant-avatar">
            <div class="w-12 h-12 rounded-full flex items-center justify-center border border-gray-200 overflow-hidden">
              <img 
                v-if="selectedRole && selectedRole.avatarUrl" 
                :src="convertMinioUrl(selectedRole.avatarUrl)" 
                :alt="selectedRole.name"
                class="w-full h-full object-cover rounded-full"
              />
              <SvgIcon v-else name="girl" customCss="w-16 h-16"></SvgIcon>
            </div>
          </div>

          <div class="quesiton-container">
            <div v-if="chat.audioUrl">
              <audio controls :src="convertAudioUrl(chat.audioUrl)" class="audio-player"></audio>
            </div>
            <div class="assistant-message-container">
              <StreamMarkdownRender :content="chat.content"/>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 提问输入框 -->
    <div class="input-container sticky bottom-0 left-0 right-0 bg-white">
      <div class="max-w-3xl mx-auto px-4">
        <!-- AI角色信息常驻显示 -->
        <div v-if="selectedRole" class="mb-3 p-3 bg-blue-50 rounded-lg border border-blue-200 role-info-card">
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <label class="text-sm font-medium text-gray-700 mr-3">AI角色:</label>
              <span class="text-sm text-blue-600 font-medium">
                {{ selectedRole.name }}
              </span>
            </div>
          </div>
          <p class="text-xs text-gray-600 mt-1">
            {{ selectedRole.introduction || '暂无简介' }}
          </p>
        </div>
        
        <!-- 未选择角色时的提示 -->
        <div v-if="!selectedRole" class="mb-3 p-3 bg-gray-50 rounded-lg border border-gray-200 role-info-card">
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <label class="text-sm font-medium text-gray-700 mr-3">AI角色:</label>
              <span class="text-sm text-gray-500">未选择角色</span>
            </div>
            <button
                @click="showRoleSelector = true"
                class="px-3 py-1 text-sm text-white bg-blue-600 hover:bg-blue-700 rounded-md transition-colors"
            >
              选择角色
            </button>
          </div>
        </div>

        <!-- 设置面板 -->
        <div v-if="showSettings" class="mb-3 p-4 bg-gray-50 rounded-2xl border border-gray-200">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
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
          </div>
        </div>



        <div class="bg-gray-100 rounded-3xl px-4 py-3 border border-gray-200 flex flex-col input-wrapper">
        <textarea
            v-model="message"
            placeholder="发消息或者输入话题"
            class="bg-transparent border-none outline-none w-full text-sm resize-none min-h-[24px] input-field"
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
                :class="isRecording ? 'bg-red-500 hover:bg-red-600 mic-recording' : 'bg-blue-100 hover:bg-blue-200 mic-normal'"
                class="flex items-center justify-center rounded-full w-8 h-8 border border-blue-300 transition-all duration-300 mic-button"
                :title="isRecording ? '停止录音' : '开始语音聊天'"

            >
              <!-- 图标：录音时显示红色方块，平时显示麦克风 -->
              <svg v-if="!isRecording" class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
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
                class="flex items-center justify-center bg-blue-100 rounded-full w-8 h-8 border border-blue-300 hover:bg-blue-200 transition-all duration-300 settings-button"
                title="聊天设置"
            >
              <svg class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
                class="flex items-center justify-center bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full w-8 h-8 border border-blue-500 hover:from-blue-600 hover:to-indigo-700 transition-all duration-300 send-button
            disabled:opacity-50
            disabled:cursor-not-allowed"
            >
              <SvgIcon v-if="!isLoading && !isStreaming" name="up-arrow" customCss="w-5 h-5 text-white"></SvgIcon>
              <div v-else class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            </button>
          </div>
        </div>
        <!-- 提示文字 -->
        <div class="flex flex-col items-center justify-center text-xs text-gray-400 mt-2 mb-2">
          <div class="mb-1">
            <span v-if="isStreaming">AI正在思考中...</span>
            <span v-else>内容由 AI 生成，请仔细甄别</span>
          </div>
          <!-- 免责声明链接 -->
          <div class="cursor-pointer text-blue-500 hover:text-blue-700 transition-colors" 
               @click="showDisclaimer = true">
            免责声明
          </div>
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

    <!-- 免责声明弹窗 -->
    <Transition name="modal">
      <div v-if="showDisclaimer" 
           class="fixed inset-0 backdrop-blur-sm bg-white/30 flex items-center justify-center z-50 modal-backdrop" 
           @click="showDisclaimer = false">
        <div class="bg-white/95 backdrop-blur-md rounded-2xl p-6 mx-4 max-w-lg w-full max-h-96 overflow-y-auto shadow-2xl border border-white/20 modal-content" 
             @click.stop>
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-semibold text-gray-900">免责声明</h3>
            <button @click="showDisclaimer = false" 
                    class="text-gray-400 hover:text-gray-600 transition-all duration-200 hover:scale-110">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
          <div class="text-sm text-gray-700 leading-relaxed space-y-3">
            <p>本模型角色独立于现实世界人物，如有同名纯属巧合。</p>
            <p>本模型的开发和运行严格遵循中国法律法规，坚持社会主义核心价值观，弘扬正能量，维护国家利益和社会稳定。</p>
            <p>我们始终坚持中国共产党的领导，坚决拥护中国特色社会主义制度，致力于为用户提供健康、积极、有益的信息服务。</p>
          </div>
          <div class="mt-6 flex justify-end">
            <button @click="showDisclaimer = false" 
                    class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-all duration-200 hover:scale-105 shadow-lg">
              我知道了
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {message as notification} from 'ant-design-vue'
import {MessageOutlined} from '@ant-design/icons-vue'
import SvgIcon from '@/components/SvgIcon.vue'
import StreamMarkdownRender from '@/components/StreamMarkdownRender.vue'
import RoleSelector from '@/components/RoleSelector.vue'
import {convertMinioUrl, convertAudioUrl} from '@/api/chat.js'

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
  roleId: null
})
/* -------- 语音聊天 -------- */
const isRecording = ref(false)

// 点击语音按钮的回调
async function toggleVoice () {
  if (isRecording.value) {
    await stopRecording()
  } else {
    await startRecording()
  }
}

let mediaRecorder = null
let audioChunks = []

async function startRecording () {
  console.log('[Voice] startRecording')
  
  if (!selectedRole.value) {
    notification.warning('请先选择AI角色后再进行语音聊天')
    showRoleSelector.value = true
    return
  }
  
  try {
    // 获取麦克风流
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    //    这里用 webm
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []
    mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) audioChunks.push(e.data)
    }

    mediaRecorder.onstop = () => {
      const blob = new Blob(audioChunks, { type: 'audio/webm' }) // ★ 上传用
      console.log('[Voice] 录音完成', blob)
      uploadAudio(blob)
      // 释放麦克风
      stream.getTracks().forEach(t => t.stop())
    }
    mediaRecorder.start()
    isRecording.value = true
  } catch (err) {
    console.error('麦克风权限失败', err)
    message.error('无法访问麦克风，请检查权限')
  }
}

async function stopRecording () {
  if (!mediaRecorder || mediaRecorder.state === 'inactive') return
  mediaRecorder.stop()   // 触发 onstop 回调
  isRecording.value = false
}

async function uploadAudio (audioBlob) {
  const form = new FormData()
  form.append('file', audioBlob, 'voice.webm')
  const res = await fetch('https://ip/api/file/upload', {
    method: 'POST',
    body: form
  })
  const json = await res.json();
  console.log(json);
  sendVideoMessage(json.data.url)
}


// 角色选择器状态
const showRoleSelector = ref(false)
const selectedRole = ref(null)

// 加载状态
const isLoading = ref(false)
const showSettings = ref(false)

// 免责声明弹窗状态
const showDisclaimer = ref(false)

// 欢迎界面数据
const welcomeSuggestions = ref([
  '介绍一下你自己',
  '今天天气怎么样？',
  '帮我写一首诗',
  '推荐一些学习方法',
  '讲个有趣的故事'
])

// 自动调整文本域高度
const autoResize = () => {
  const textarea = textareaRef.value;
  if (textarea) {
    textarea.style.height = 'auto'

    const newHeight = Math.min(textarea.scrollHeight, 300);
    textarea.style.height = newHeight + 'px';

    textarea.style.overflowY = textarea.scrollHeight > 300 ? 'auto' : 'hidden';
  }
}


// SSE 相关
let streamReader = null;
let isStreaming = ref(false)

const sendVideoMessage = async (videoUrl) => {
  console.log("接受到了url:" + videoUrl)
  // 校验发送的消息不能为空
  if (!videoUrl || isLoading.value || isStreaming.value) return

  // 校验roleId是否为空
  if (!chatSettings.value.roleId) {
    showRoleSelector.value = true
    return
  }

  // 保存原始URL，因为后面会清空
  const originalVideoUrl = videoUrl

  // 通知父组件有新消息，获取或创建chatId
  let actualChatId = props.currentChatId
  if (!actualChatId) {
    // 创建新对话
    const messageData = { role: 'user', content: '', audioUrl: originalVideoUrl };
    const newChatId = await props.createNewChat(messageData, chatSettings.value.roleId)
    actualChatId = newChatId
  } else {
    // 现有对话，正常发送消息事件
    emit('new-message', {role: 'user', content: '', audioUrl: originalVideoUrl})
  }

  // 点击发送按钮后，清空输入框
  videoUrl = ''
  // 将输入框的高度重置
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  // 添加一个占位的用户消息（包含语音）
  chatList.value.push({
    role: 'user',
    content: '',
    audioUrl: originalVideoUrl  // 使用保存的原始URL
  })

  // 添加一个占位的AI回复消息
  chatList.value.push({
    role: 'assistant',
    content: '',
    audioUrl: null  // 初始化为null，后面会更新
  })

  isLoading.value = true
  isStreaming.value = true

  try {
    const response = await fetch('https://ip/api/chat/voice-chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache'
      },
      body: JSON.stringify({
        audioFileUrl: originalVideoUrl,
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

    // 直接解析JSON响应
    const responseData = await response.json()
    console.log('后端响应:', responseData)

    // 从响应数据中提取信息
    let responseText = ''
    let responseAudioUrl = null

    if (responseData.success && responseData.data) {
      responseText = responseData.data.replyText || ''
      responseAudioUrl = responseData.data.replyAudioUrl || null
    }

    const lastIndex = chatList.value.length - 1
    if (lastIndex >= 0 && chatList.value[lastIndex].role === 'assistant') {
      chatList.value[lastIndex] = {
        ...chatList.value[lastIndex],
        content: responseText,
        audioUrl: responseAudioUrl
      }
      nextTick(() => {
        scrollToBottom()
      })
    }

    // 通知父组件AI回复完成
    if (responseText || responseAudioUrl) {
      emit('new-message', {
        role: 'assistant',
        content: responseText,
        audioUrl: responseAudioUrl
      })
    }

  } catch (error) {
    console.error('发送消息错误:', error)
    const lastIndex = chatList.value.length - 1
    if (lastIndex >= 0 && chatList.value[lastIndex].role === 'assistant') {
      chatList.value[lastIndex].content = '抱歉，请求出错了，请稍后重试。'
    }
  } finally {
    isLoading.value = false
    isStreaming.value = false
  }
}

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
    const response = await fetch('https://ip/api/chat/completion', {
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
        buffer = lines.pop() || ''

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
  await nextTick()
  if (chatContainer.value) {
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

// 处理建议点击
const handleSuggestionClick = (suggestion) => {
  message.value = suggestion
  sendMessage()
}


// 监听props变化，加载历史消息

watch(() => props.initialMessages, (newMessages) => {
  if (newMessages && newMessages.length > 0) {
    chatList.value = [...newMessages]
    nextTick(() => {
      scrollToBottom()
    })
  } else {
    chatList.value = []
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
  max-width: calc(100% - 160px);
  position: relative;
}

/* 用户消息容器样式 */
.user-message-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
  margin-left: 80px;
}

/* AI助手消息容器样式 */
.assistant-message-wrapper {
  display: flex;
  margin-bottom: 1rem;
  margin-right: 80px;
  align-items: flex-start;
}

/* 移动端响应式间距 */
@media (max-width: 768px) {
  .user-message-wrapper {
    margin-left: 40px;
  }

  .assistant-message-wrapper {
    margin-right: 40px;
  }
}

@media (max-width: 480px) {
  .user-message-wrapper {
    margin-left: 20px;
  }

  .assistant-message-wrapper {
    margin-right: 20px;
  }
}

.assistant-avatar {
  flex-shrink: 0;
  margin-right: 12px;
}

.assistant-message-container {
  flex: 1;
  padding: 8px 0;
  min-width: 0;
  max-width: none;
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
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}

/* 欢迎界面样式 */
.welcome-screen {
  position: relative;
  min-height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 动态背景装饰 */
.welcome-bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 100px;
  height: 100px;
  background: linear-gradient(45deg, #667eea, #764ba2);
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 60px;
  height: 60px;
  background: linear-gradient(45deg, #f093fb, #f5576c);
  top: 20%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  width: 80px;
  height: 80px;
  background: linear-gradient(45deg, #4facfe, #00f2fe);
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

.shape-4 {
  width: 120px;
  height: 120px;
  background: linear-gradient(45deg, #43e97b, #38f9d7);
  bottom: 10%;
  right: 10%;
  animation-delay: 1s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  33% {
    transform: translateY(-20px) rotate(120deg);
  }
  66% {
    transform: translateY(20px) rotate(240deg);
  }
}

/* 欢迎内容 */
.welcome-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  width: 100%;
  text-align: center;
  animation: fadeInUp 1s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 角色介绍区域 */
.role-intro-section {
  margin-bottom: 3rem;
}

.role-avatar-large {
  position: relative;
  display: inline-block;
  margin-bottom: 1.5rem;
}

.avatar-glow {
  position: absolute;
  top: -10px;
  left: -10px;
  right: -10px;
  bottom: -10px;
  border-radius: 50%;
  background: linear-gradient(45deg, #667eea, #764ba2, #f093fb, #f5576c);
  opacity: 0.3;
  animation: rotate 3s linear infinite;
  z-index: 0;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.avatar-container {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid white;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  z-index: 1;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

.role-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.role-info-text {
  animation: slideInFromLeft 1s ease-out 0.3s both;
}

@keyframes slideInFromLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.role-welcome-title {
  font-size: 2rem;
  font-weight: 700;
  color: #2d3748;
  margin-bottom: 1rem;
  line-height: 1.2;
}

.role-name-highlight {
  background: linear-gradient(45deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: shimmer 2s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% {
    background-position: -200% center;
  }
  50% {
    background-position: 200% center;
  }
}

.role-description {
  font-size: 1.1rem;
  color: #4a5568;
  line-height: 1.6;
  max-width: 600px;
  margin: 0 auto;
}

.select-role-prompt {
  margin-top: 2rem;
  animation: slideInFromBottom 1s ease-out 0.6s both;
}

@keyframes slideInFromBottom {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.select-role-btn {
  display: inline-flex;
  align-items: center;
  padding: 1rem 2rem;
  background: linear-gradient(45deg, #667eea, #764ba2);
  color: white;
  border: none;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.select-role-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.select-role-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.select-role-btn:hover::before {
  left: 100%;
}

.select-role-btn:active {
  transform: translateY(0);
}


/* 建议对话区域 */
.suggestions-section {
  animation: fadeInUp 1s ease-out 0.8s both;
}

.suggestions-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 1.5rem;
}

.suggestion-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  justify-content: center;
  max-width: 600px;
  margin: 0 auto;
}

.suggestion-chip {
  background: linear-gradient(45deg, #f7fafc, #edf2f7);
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 0.75rem 1.5rem;
  font-size: 0.875rem;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.suggestion-chip::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  transition: left 0.5s ease;
}

.suggestion-chip:hover {
  background: linear-gradient(45deg, #667eea, #764ba2);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.suggestion-chip:hover::before {
  left: 100%;
}

.suggestion-chip:active {
  transform: translateY(0);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-screen {
    min-height: 500px;
    padding: 1rem;
  }
  
  .role-welcome-title {
    font-size: 1.5rem;
  }
  
  .suggestion-chips {
    flex-direction: column;
    align-items: center;
  }
  
  .suggestion-chip {
    width: 100%;
    max-width: 300px;
  }
}

/* 弹窗动画样式 */
.modal-enter-active, .modal-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-enter-from, .modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-backdrop {
  backdrop-filter: blur(0px);
}

/* 角色信息卡片动态效果 */
.role-info-card {
  animation: slideInUp 0.6s ease-out;
  transition: all 0.3s ease;
}

.role-info-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 输入框动态效果 */
.input-wrapper {
  animation: fadeInScale 0.8s ease-out;
  transition: all 0.3s ease;
}

.input-wrapper:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.input-field:focus {
  animation: pulse 0.3s ease-in-out;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
  100% {
    transform: scale(1);
  }
}

/* 按钮动态效果 */
.mic-button {
  position: relative;
  overflow: hidden;
}

.mic-button:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.mic-button:active {
  transform: scale(0.95);
}

.mic-recording {
  animation: pulse-red 1.5s ease-in-out infinite;
}

.mic-normal:hover {
  animation: bounce 0.6s ease-in-out;
}

@keyframes pulse-red {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(239, 68, 68, 0);
  }
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0) scale(1.1);
  }
  40% {
    transform: translateY(-4px) scale(1.1);
  }
  60% {
    transform: translateY(-2px) scale(1.1);
  }
}

.settings-button {
  position: relative;
  overflow: hidden;
}

.settings-button:hover {
  transform: scale(1.1) rotate(90deg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.settings-button:active {
  transform: scale(0.95) rotate(90deg);
}

.send-button {
  position: relative;
  overflow: hidden;
}

.send-button:hover:not(:disabled) {
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.send-button:active:not(:disabled) {
  transform: scale(0.95);
}

.send-button:not(:disabled) {
  animation: glow 2s ease-in-out infinite alternate;
}

@keyframes glow {
  from {
    box-shadow: 0 0 5px rgba(59, 130, 246, 0.5);
  }
  to {
    box-shadow: 0 0 20px rgba(59, 130, 246, 0.8);
  }
}

/* 音频播放器样式 */
.audio-player {
  max-width: 250px;
}

/* 聊天气泡动态效果 */
.chat-bubble {
  animation: slideInMessage 0.5s ease-out;
  transition: all 0.3s ease;
}

.chat-bubble:hover {
  transform: translateY(-2px);
}

.user-message-wrapper:hover {
  transform: translateX(-4px) translateY(-2px);
}

.assistant-message-wrapper:hover {
  transform: translateX(4px) translateY(-2px);
}

@keyframes slideInMessage {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 头像动态效果 */
.user-avatar {
  transition: all 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.1) rotate(5deg);
}

.assistant-avatar {
  transition: all 0.3s ease;
}

.assistant-avatar:hover {
  transform: scale(1.1) rotate(-5deg);
}

.assistant-avatar img {
  transition: all 0.3s ease;
}

.assistant-avatar:hover img {
  transform: scale(1.05);
}

/* 消息容器动态效果 */
.quesiton-container {
  transition: all 0.3s ease;
}

.user-message-wrapper:hover .quesiton-container {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.assistant-message-wrapper:hover .quesiton-container {
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.modal-leave-to .modal-backdrop {
  backdrop-filter: blur(0px);
}

.modal-enter-from .modal-content {
  transform: scale(0.8) translateY(20px);
  opacity: 0;
}

.modal-leave-to .modal-content {
  transform: scale(0.9) translateY(-10px);
  opacity: 0;
}

.modal-enter-to .modal-content, .modal-leave-from .modal-content {
  transform: scale(1) translateY(0);
  opacity: 1;
}
</style>