<template>
  <a-modal
      v-model:open="visible"
      title="选择 AI 角色"
      :width="800"
      :footer="null"
      :maskClosable="false"
      @cancel="handleCancel"
  >
    <div class="role-selector">
      <!-- 搜索框 -->
      <div style="margin-bottom: 1rem;">
        <a-input
            v-model:value="searchKeyword"
            placeholder="搜索角色名称或简介..."
            allowClear
            @input="handleSearch"
        >
          <template #prefix>
            <SearchOutlined/>
          </template>
        </a-input>
      </div>

      <!-- 角色列表 -->
      <div class="role-list" v-loading="loading">
        <div class="grid grid-cols-1 max-h-96 overflow-y-auto" style="gap: 0.75rem;">
          <div
              v-for="role in filteredRoles"
              :key="role.id"
              class="role-item border border-gray-200 cursor-pointer transition-all hover:border-blue-400 hover:shadow-md"
              style="padding: 1rem; border-radius: 0.75rem;"
              :class="{ 'border-blue-500 bg-blue-50': selectedRole?.id === role.id }"
              @click="selectRole(role)"
          >
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <!-- 角色头像和基本信息 -->
                <div class="flex items-center" style="margin-bottom: 0.5rem;">
                  <div
                      class="role-avatar w-10 h-10 rounded-full flex items-center justify-center border border-gray-300"
                      style="margin-right: 0.75rem;"
                      :style="{ backgroundColor: getAvatarColor(role.voiceCode) }">
                    <span class="text-white font-medium">{{ role.name?.charAt(0) || '角' }}</span>
                  </div>
                  <div>
                    <h3 class="font-medium text-gray-800 text-base">{{ role.name }}</h3>
                    <p class="text-xs text-gray-500" style="margin-top: 0.25rem;">{{ role.voiceCode || 'default' }}</p>
                  </div>
                </div>

                <!-- 角色简介 -->
                <p class="text-sm text-gray-600 line-clamp-2" style="margin-bottom: 0.5rem;">
                  {{ role.introduction || '暂无简介' }}
                </p>

                <!-- 角色特性标签 -->
                <div class="flex flex-wrap" style="gap: 0.25rem;">
                  <span v-if="role.language" class="tag">{{ role.language }}</span>
                  <span v-if="role.voiceModelName" class="tag">{{ role.voiceModelName }}</span>
                </div>
              </div>

              <!-- 选择状态 -->
              <div v-if="selectedRole?.id === role.id" style="margin-left: 0.75rem;">
                <CheckCircleFilled class="text-blue-500 text-lg"/>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && filteredRoles.length === 0" class="text-center text-gray-500" style="padding: 2rem 0;">
          <UserOutlined class="text-4xl" style="margin-bottom: 0.5rem;"/>
          <p>{{ searchKeyword ? '未找到匹配的角色' : '暂无可用角色' }}</p>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > pagination.pageSize" class="mt-4 text-center">
        <a-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :show-size-changer="false"
            :show-quick-jumper="false"
            :show-total="(total) => `共 ${total} 个角色`"
            @change="loadRoles"
        />
      </div>

      <!-- 底部操作按钮 -->
      <div class="flex justify-end border-t border-gray-200"
           style="gap: 0.75rem; margin-top: 1.5rem; padding-top: 1rem;">
        <a-button @click="handleCancel">取消</a-button>
        <a-button
            type="primary"
            :disabled="!selectedRole"
            @click="handleConfirm"
        >
          确认选择
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {message} from 'ant-design-vue'
import {CheckCircleFilled, SearchOutlined, UserOutlined} from '@ant-design/icons-vue'

// Props
const props = defineProps({
  open: {
    type: Boolean,
    default: false
  },
  defaultRoleId: {
    type: Number,
    default: null
  }
})

// Emits
const emit = defineEmits(['update:open', 'confirm', 'cancel'])

// 响应式数据
const visible = ref(props.open)
const loading = ref(false)
const searchKeyword = ref('')
const selectedRole = ref(null)
const roleList = ref([])

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// 计算属性 - 过滤后的角色列表
const filteredRoles = computed(() => {
  if (!searchKeyword.value) {
    return roleList.value
  }
  return roleList.value.filter(role =>
      role.name?.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      role.introduction?.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 监听props变化
watch(() => props.open, (newVal) => {
  visible.value = newVal
  if (newVal) {
    loadRoles()
    // 如果有默认角色ID，设置为选中状态
    if (props.defaultRoleId) {
      const defaultRole = roleList.value.find(role => role.id === props.defaultRoleId)
      if (defaultRole) {
        selectedRole.value = defaultRole
      }
    }
  }
})

watch(visible, (newVal) => {
  emit('update:open', newVal)
})

// 加载角色列表
const loadRoles = async (current = 1) => {
  try {
    loading.value = true

    const response = await fetch('http://localhost:8081/role/page', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        current,
        size: pagination.pageSize
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const result = await response.json()
    if (result.success) {
      roleList.value = result.data || []
      pagination.total = result.total || 0
      pagination.current = current
    } else {
      message.error('加载角色列表失败')
    }
  } catch (error) {
    console.error('加载角色列表错误:', error)
    message.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  // 这里可以添加防抖处理
}

// 选择角色
const selectRole = (role) => {
  selectedRole.value = role
}

// 生成头像颜色
const getAvatarColor = (voiceCode) => {
  const colors = [
    '#1890ff', '#52c41a', '#faad14', '#f5222d',
    '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'
  ]
  const index = voiceCode ? voiceCode.charCodeAt(0) % colors.length : 0
  return colors[index]
}

// 确认选择
const handleConfirm = () => {
  if (!selectedRole.value) {
    message.warning('请选择一个角色')
    return
  }

  emit('confirm', selectedRole.value)
  handleCancel()
}

// 取消
const handleCancel = () => {
  visible.value = false
  selectedRole.value = null
  searchKeyword.value = ''
  emit('cancel')
}

// 组件挂载时加载数据
onMounted(() => {
  if (props.open) {
    loadRoles()
  }
})
</script>

<style scoped>
.role-selector {
  max-height: 70vh;
}

.role-item {
  transition: all 0.3s ease;
}

.role-item:hover {
  transform: translateY(-1px);
}

.role-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.tag {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  background-color: #f3f4f6;
  color: #4b5563;
  border-radius: 0.25rem;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 自定义滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
