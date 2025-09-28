<template>
  <!-- 左边栏 -->
  <div
      :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
      class="w-64 bg-[#f9fbff] border-r border-gray-200 fixed left-0 top-0 h-full transition-transform duration-300 ease-in-out z-10 overflow-y-auto">
    <!-- 侧边栏内容区域 -->
    <div class="p-0 h-full flex flex-col">
      <!-- Logo 与应用名称 -->
      <div class="flex items-center justify-center p-4 cursor-pointer">
        <SvgIcon name="cat" customCss="w-8 h-8 text-gray-700 mr-3"/>

        <span class="text-2xl font-bold font-sans tracking-wide text-gray-800">Cat AI机器人</span>
      </div>


      <!-- 功能按钮区域 -->
      <div class="px-4 mb-4 space-y-2">
        <!-- 开启新对话按钮 -->
        <button
            @click="handleNewChat"
            :class="[
                  'w-full px-4 py-2 rounded-xl transition-colors cursor-pointer flex items-center justify-center',
                  currentView === 'chat' ? 'new-chat-btn-active' : 'new-chat-btn'
                ]">
          <SvgIcon name="new-chat" customCss="w-5 h-5 mr-2 inline text-[#4d6bfe]"/>
          开启新对话
        </button>

        <!-- 角色管理按钮 -->
        <button
            @click="handleRoleManagement"
            :class="[
                  'w-full px-4 py-2 border rounded-xl transition-colors cursor-pointer flex items-center justify-center',
                  currentView === 'role-management' ? 'role-management-btn-active' : 'role-management-btn'
                ]">
          <UserOutlined
              :class="['w-5 h-5 mr-2', currentView === 'role-management' ? 'text-blue-600' : 'text-gray-600']"/>
          角色管理
        </button>
      </div>

      <!-- 历史对话区域 -->
      <div class="my-4 px-2 overflow-y-auto overflow-x-hidden flex-1">
        <div class="space-y-1">
          <div class="text-xs px-3 py-1 text-gray-500">历史对话</div>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="flex justify-center py-4">
            <div class="w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin"></div>
          </div>

          <!-- 对话列表 -->
          <div v-for="(historyChat, index) in historyChats" :key="historyChat.id || index"
               class="relative px-3 py-2 rounded-xl hover:bg-[rgb(239,246,255)] cursor-pointer transition-colors flex items-start justify-between min-h-[56px]"
               :class="{ 'bg-blue-50 border border-blue-200': currentChatId === historyChat.uuid }"
               @click="selectChat(historyChat)"
               @mouseenter="showButton = historyChat.uuid"
               @mouseleave="showButton = null">

            <!-- 重命名输入框 -->
            <div v-if="renamingChatId === historyChat.id" class="flex-1 mr-2" @click.stop>
              <div class="space-y-1">
                <!-- 角色名（不可编辑） -->
                <div class="text-xs text-blue-600 font-medium overflow-hidden whitespace-nowrap"
                     :title="historyChat.roleName">
                  {{ historyChat.roleName }}
                </div>
                <!-- 摘要输入框（可编辑） -->
                <input
                    v-model="newChatTitle"
                    @keydown.enter="handleEnterKey"
                    @keydown.esc="cancelRename"
                    @blur="handleBlur"
                    class="w-full px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:border-blue-500"
                    placeholder="请输入对话摘要"
                    autofocus
                />
              </div>
            </div>

            <!-- 对话显示 -->
            <div v-else class="flex-1 overflow-hidden">
              <!-- 角色名 -->
              <div class="text-xs text-blue-600 font-medium overflow-hidden whitespace-nowrap mb-1"
                   :title="historyChat.roleName">
                {{ historyChat.roleName }}
              </div>
              <!-- 对话摘要 -->
              <div class="text-[14px] text-gray-800 overflow-hidden whitespace-nowrap"
                   :title="historyChat.summary">
                {{ historyChat.summary || '新对话' }}
              </div>
            </div>

            <!-- 下拉菜单 -->
            <a-dropdown v-if="renamingChatId !== historyChat.id" trigger="click">
              <template #overlay>
                <a-menu @click="({ key }) => handleMenuClick(key, historyChat)">
                  <a-menu-item key="rename">
                    <EditOutlined/>
                    重命名
                  </a-menu-item>
                  <a-menu-item key="delete" danger>
                    <DeleteOutlined/>
                    删除
                  </a-menu-item>
                </a-menu>
              </template>
              <!-- 右边菜单按钮 -->
              <button
                  @click.stop
                  class="z-10 rounded-lg outline-none justify-center items-center bg-white
                            w-6 h-6 flex absolute right-2 top-2 transition-all duration-300 hover:bg-gray-50"
                  :style="{ opacity: showButton === historyChat.uuid ? 1 : 0 }">
                <EllipsisOutlined class="w-4 h-4 text-gray-500"/>
              </button>
            </a-dropdown>
          </div>

          <!-- 空状态 -->
          <div v-if="!isLoading && historyChats.length === 0" class="text-center py-8 text-gray-500 text-sm">
            暂无历史对话
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 侧边栏切换按钮 -->
  <a-tooltip placement="bottom">
    <!-- Tooltip 提示文字 -->
    <template #title>
      <span>{{ sidebarOpen ? '收缩边栏' : '打开边栏' }}</span>
    </template>

    <button
        :class="sidebarOpen ? 'left-64' : 'left-0'"
        @click="toggleSidebar"
        class="fixed top-4 z-20 bg-white border border-gray-200 rounded-r-lg p-2 transition-all duration-300">
      <!-- 图标 -->
      <SvgIcon :name="sidebarOpen ? 'sidebar-open' : 'sidebar-close'"
               :customCss="sidebarOpen ? 'w-6 h-6 text-gray-400' : 'w-7 h-7 text-gray-400'"/>
    </button>
  </a-tooltip>


</template>

<script setup>
import {onMounted, ref} from 'vue'
import {message} from 'ant-design-vue'
import SvgIcon from '@/components/SvgIcon.vue'
import {DeleteOutlined, EditOutlined, EllipsisOutlined, UserOutlined} from '@ant-design/icons-vue'
import {deleteChat as deleteChatApi, getChatHistory, renameChat} from '@/api/chat.js'
import {useRouter} from 'vue-router'

// 定义 props, 对外部暴露配置项
const props = defineProps({
  sidebarOpen: {type: Boolean, required: true}, // 左边栏是否展开
  currentChatId: {type: String, default: null}, // 当前选中的对话ID
  currentView: {type: String, default: 'chat'}, // 当前视图
})

// 定义emits
const emit = defineEmits(['toggle-sidebar', 'select-chat', 'new-chat', 'chat-deleted', 'switch-view'])

// 路由
const router = useRouter()

// 切换侧边栏显示/隐藏
const toggleSidebar = () => {
  emit('toggle-sidebar')
}

// 历史对话
const historyChats = ref([])
const isLoading = ref(false)
const isRenaming = ref(false)
const renamingChatId = ref(null)
const newChatTitle = ref('')

// 当前显示右侧栏按钮的聊天 ID
const showButton = ref(null)

// 加载历史对话列表
const loadChatHistory = async () => {
  try {
    isLoading.value = true
    const response = await getChatHistory(1, 50)
    if (response.success) {
      historyChats.value = response.data || []
    } else {
      message.error('加载历史对话失败')
    }
  } catch (error) {
    console.error('加载历史对话错误:', error)
    message.error('加载历史对话失败')
  } finally {
    isLoading.value = false
  }
}

// 新建对话
const handleNewChat = () => {
  emit('switch-view', 'chat')
  emit('new-chat')
}

// 角色管理
const handleRoleManagement = () => {
  emit('switch-view', 'role-management')
}

// 选择对话
const selectChat = (chat) => {
  emit('switch-view', 'chat')
  emit('select-chat', chat)
}

// 处理菜单点击
const handleMenuClick = (key, chat) => {
  if (key === 'rename') {
    startRename(chat)
  } else if (key === 'delete') {
    confirmDelete(chat)
  }
}

// 开始重命名
const startRename = (chat) => {
  renamingChatId.value = chat.id
  newChatTitle.value = chat.summary
  isRenaming.value = true
}

// 确认重命名
const confirmRename = async () => {
  // 防止重复调用
  if (isRenaming.value === false) {
    return
  }

  try {
    // 找到当前重命名的聊天对象
    const currentChat = historyChats.value.find(c => c.id === renamingChatId.value)
    if (!currentChat) {
      message.error('找不到对话')
      cancelRename()
      return
    }

    // 检查摘要是否为空
    if (!newChatTitle.value.trim()) {
      message.error('对话摘要不能为空')
      return
    }

    const response = await renameChat(currentChat.uuid, newChatTitle.value.trim())
    if (response.success) {
      // 更新本地数据
      const chatIndex = historyChats.value.findIndex(c => c.id === renamingChatId.value)
      if (chatIndex !== -1) {
        historyChats.value[chatIndex].summary = newChatTitle.value.trim()
      }
      message.success('重命名成功')
    } else {
      message.error('重命名失败')
    }
  } catch (error) {
    console.error('重命名错误:', error)
    message.error('重命名失败')
  } finally {
    cancelRename()
  }
}

// 处理回车键
const handleEnterKey = (event) => {
  event.preventDefault()
  confirmRename()
}

// 处理失焦事件
const handleBlur = () => {
  // 延迟一点点执行，防止与Enter键冲突
  setTimeout(() => {
    if (isRenaming.value) {
      confirmRename()
    }
  }, 100)
}

// 取消重命名
const cancelRename = () => {
  isRenaming.value = false
  renamingChatId.value = null
  newChatTitle.value = ''
}

// 确认删除
const confirmDelete = async (chat) => {
  try {
    const response = await deleteChatApi(chat.uuid)
    if (response.success) {
      // 从本地列表中移除
      historyChats.value = historyChats.value.filter(c => c.id !== chat.id)
      message.success('删除成功')

      // 如果删除的是当前选中的对话，通知父组件
      if (props.currentChatId === chat.uuid) {
        emit('chat-deleted')
      }
    } else {
      message.error('删除失败')
    }
  } catch (error) {
    console.error('删除错误:', error)
    message.error('删除失败')
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadChatHistory()
})

// 暴露方法给父组件
defineExpose({
  loadChatHistory
})

</script>

<style scoped>
.overflow-y-auto {
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent; /* 自定义滚动条颜色 */
}

.new-chat-btn {
  background-color: rgb(219 234 254);
  color: #4d6bfe;
}

.new-chat-btn:hover {
  background-color: #c6dcf8;
}

.new-chat-btn-active {
  background-color: #3b82f6;
  color: white;
}

.new-chat-btn-active:hover {
  background-color: #2563eb;
}

.role-management-btn {
  background-color: white;
  color: #64748b;
  border-color: #d1d5db;
}

.role-management-btn:hover {
  background-color: #f8fafc;
  border-color: #9ca3af;
}

.role-management-btn-active {
  background-color: #dbeafe;
  color: #1e40af;
  border-color: #3b82f6;
}

.role-management-btn-active:hover {
  background-color: #bfdbfe;
  border-color: #2563eb;
}
</style>