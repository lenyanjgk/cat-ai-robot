<template>
  <div class="role-management-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">角色管理</h1>
        <p class="page-description">管理AI角色信息，包括角色名称、简介、语音模型等配置</p>
      </div>
      <div class="header-actions">
        <button
            @click="showCreateModal = true"
            class="btn-primary"
        >
          <PlusOutlined class="btn-icon"/>
          新增角色
        </button>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <div class="search-card">
        <div class="search-form">
          <div class="search-input-group">
            <SearchOutlined class="search-icon"/>
            <input
                v-model="searchKeyword"
                @input="handleSearch"
                placeholder="搜索角色名称或简介..."
                class="search-input"
            />
          </div>
          <button
              @click="handleSearch"
              class="btn-search"
          >
            搜索
          </button>
          <button
              @click="resetSearch"
              class="btn-reset"
          >
            重置
          </button>
        </div>
      </div>
    </div>

    <!-- 角色列表表格 -->
    <div class="table-section">
      <div class="table-card">
        <div class="table-header">
          <h3 class="table-title">角色列表</h3>
          <div class="table-info">
            <span class="total-count">共 {{ pagination.total }} 个角色</span>
          </div>
        </div>

        <div class="table-container" v-loading="loading">
          <table class="role-table">
            <thead>
            <tr>
              <th class="col-avatar">头像</th>
              <th class="col-name">角色名称</th>
              <th class="col-intro">简介</th>
              <th class="col-voice">语音模型</th>
              <th class="col-language">语言</th>
              <th class="col-time">创建时间</th>
              <th class="col-actions">操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="role in filteredRoles" :key="role.id" class="table-row">
              <td class="col-avatar">
                <div class="role-avatar" :style="{ backgroundColor: getAvatarColor(role.voiceCode) }">
                  <span class="avatar-text">{{ role.name?.charAt(0) || '角' }}</span>
                </div>
              </td>
              <td class="col-name">
                <div class="role-name">{{ role.name }}</div>
                <div class="role-code">{{ role.voiceCode || 'default' }}</div>
              </td>
              <td class="col-intro">
                <div class="role-intro">{{ role.introduction || '暂无简介' }}</div>
              </td>
              <td class="col-voice">
                <div class="voice-info">
                  <span v-if="role.voiceModelName" class="tag tag-blue">{{ role.voiceModelName }}</span>
                  <span v-else class="tag tag-gray">未设置</span>
                  <div v-if="role.voiceModelName" class="voice-params">
                    <span class="param-item">语速: {{ role.speechRate || 1.3 }}x</span>
                    <span class="param-item">语调: {{ role.pitchRate || 1.3 }}x</span>
                  </div>
                </div>
              </td>
              <td class="col-language">
                <span v-if="role.language" class="tag tag-green">{{ role.language }}</span>
                <span v-else class="tag tag-gray">未设置</span>
              </td>
              <td class="col-time">
                <div class="time-text">{{ formatTime(role.createTime) }}</div>
              </td>
              <td class="col-actions">
                <div class="action-buttons">
                  <button
                      @click="editRole(role)"
                      class="btn-action btn-edit"
                      title="编辑"
                  >
                    <EditOutlined/>
                  </button>
                  <button
                      @click="deleteRole(role)"
                      class="btn-action btn-delete"
                      title="删除"
                  >
                    <DeleteOutlined/>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>

          <!-- 空状态 -->
          <div v-if="!loading && filteredRoles.length === 0" class="empty-state">
            <UserOutlined class="empty-icon"/>
            <p class="empty-text">{{ searchKeyword ? '未找到匹配的角色' : '暂无角色数据' }}</p>
            <button
                v-if="!searchKeyword"
                @click="showCreateModal = true"
                class="btn-primary"
            >
              创建第一个角色
            </button>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="filteredRoles.length > 0" class="pagination-container">
          <a-pagination
              v-model:current="pagination.current"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              :show-total="(total) => `共 ${total} 个角色`"
              :show-size-changer="true"
              :page-size-options="['10', '20', '50', '100']"
              @change="loadRoles"
              @show-size-change="loadRoles"
          />
        </div>
      </div>
    </div>

    <!-- 创建/编辑角色弹窗 -->
    <a-modal
        v-model:open="showCreateModal"
        :title="editingRole ? '编辑角色' : '新增角色'"
        :width="600"
        :footer="null"
        @cancel="closeModal"
    >
      <div class="role-form">
        <div class="form-group">
          <label class="form-label">角色名称 *</label>
          <input
              v-model="roleForm.name"
              placeholder="请输入角色名称"
              class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label">角色简介</label>
          <textarea
              v-model="roleForm.introduction"
              placeholder="请输入角色简介"
              class="form-textarea"
              rows="3"
          ></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">语音代码</label>
            <input
                v-model="roleForm.voiceCode"
                placeholder="请输入语音代码"
                class="form-input"
            />
          </div>

          <div class="form-group">
            <label class="form-label">语音模型</label>
            <select v-model="roleForm.voiceModelName" class="form-select">
              <option value="">请选择语音模型</option>
              <option value="cosyvoice-v3">cosyvoice-v3</option>
              <option value="cosyvoice-v2">cosyvoice-v2</option>
              <option value="cosyvoice-v1">cosyvoice-v1</option>
            </select>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">语速 ({{ roleForm.speechRate }})</label>
            <input
                v-model.number="roleForm.speechRate"
                type="range"
                min="0.5"
                max="2.0"
                step="0.1"
                class="form-range"
            />
            <div class="range-labels">
              <span>0.5x</span>
              <span>2.0x</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">语调 ({{ roleForm.pitchRate }})</label>
            <input
                v-model.number="roleForm.pitchRate"
                type="range"
                min="0.5"
                max="2.0"
                step="0.1"
                class="form-range"
            />
            <div class="range-labels">
              <span>0.5x</span>
              <span>2.0x</span>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">语言</label>
          <select v-model="roleForm.language" class="form-select">
            <option value="">请选择语言</option>
            <option value="zh">中文</option>
            <option value="en">English</option>
          </select>
        </div>

        <div class="form-actions">
          <button @click="closeModal" class="btn-cancel">取消</button>
          <button @click="saveRole" class="btn-save" :disabled="!roleForm.name">
            {{ editingRole ? '更新' : '创建' }}
          </button>
        </div>
      </div>
    </a-modal>

    <!-- 删除确认弹窗 -->
    <a-modal
        v-model:open="showDeleteModal"
        title="确认删除"
        :width="400"
        @ok="confirmDelete"
        @cancel="showDeleteModal = false"
    >
      <div class="delete-confirm">
        <ExclamationCircleOutlined class="delete-icon"/>
        <p class="delete-text">确定要删除角色 "{{ deletingRole?.name }}" 吗？</p>
        <p class="delete-warning">此操作不可撤销，请谨慎操作。</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {message} from 'ant-design-vue'
import {
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  PlusOutlined,
  SearchOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import {createRole, deleteRole as deleteRoleApi, getRoles, updateRole} from '@/api/role'

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const roleList = ref([])
const showCreateModal = ref(false)
const showDeleteModal = ref(false)
const editingRole = ref(null)
const deletingRole = ref(null)

// 分页数据
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 角色表单数据
const roleForm = reactive({
  name: '',
  introduction: '',
  voiceCode: '',
  voiceModelName: '',
  language: '',
  speechRate: 1.3,
  pitchRate: 1.3
})

// 计算属性 - 过滤后的角色列表
const filteredRoles = computed(() => {
  if (!searchKeyword.value) return roleList.value
  return roleList.value.filter(role =>
      role.name?.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      role.introduction?.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 方法
const loadRoles = async () => {
  try {
    loading.value = true
    const response = await getRoles(pagination.current, pagination.size)
    if (response.success) {
      roleList.value = response.data || []
      pagination.total = response.total || 0
    } else {
      message.error(response.message || '加载角色列表失败')
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
    message.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadRoles()
}

const resetSearch = () => {
  searchKeyword.value = ''
  pagination.current = 1
  loadRoles()
}

const editRole = (role) => {
  editingRole.value = role
  Object.assign(roleForm, {
    name: role.name || '',
    introduction: role.introduction || '',
    voiceCode: role.voiceCode || '',
    voiceModelName: role.voiceModelName || '',
    language: role.language || '',
    speechRate: role.speechRate || 1.3,
    pitchRate: role.pitchRate || 1.3
  })
  showCreateModal.value = true
}

const deleteRole = (role) => {
  deletingRole.value = role
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (!deletingRole.value) return

  try {
    const response = await deleteRoleApi(deletingRole.value.id)
    if (response.success) {
      message.success('删除成功')
      showDeleteModal.value = false
      deletingRole.value = null
      loadRoles()
    } else {
      message.error(response.message || '删除失败')
    }
  } catch (error) {
    console.error('删除角色失败:', error)
    message.error('删除角色失败')
  }
}

const saveRole = async () => {
  if (!roleForm.name.trim()) {
    message.warning('请输入角色名称')
    return
  }

  try {
    const roleData = {...roleForm}
    let response

    if (editingRole.value) {
      roleData.id = editingRole.value.id
      response = await updateRole(roleData)
    } else {
      response = await createRole(roleData)
    }

    if (response.success) {
      message.success(editingRole.value ? '更新成功' : '创建成功')
      closeModal()
      loadRoles()
    } else {
      message.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('保存角色失败:', error)
    message.error('保存角色失败')
  }
}

const closeModal = () => {
  showCreateModal.value = false
  editingRole.value = null
  Object.assign(roleForm, {
    name: '',
    introduction: '',
    voiceCode: '',
    voiceModelName: '',
    language: '',
    speechRate: 1.3,
    pitchRate: 1.3
  })
}

const getAvatarColor = (voiceCode) => {
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4']
  const hash = voiceCode ? voiceCode.split('').reduce((a, b) => a + b.charCodeAt(0), 0) : 0
  return colors[hash % colors.length]
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 生命周期
onMounted(() => {
  loadRoles()
})

// 暴露方法给父组件
defineExpose({
  loadRoles
})
</script>

<style scoped>
/* 角色管理视图容器 */
.role-management-view {
  height: 100vh;
  background-color: #f8fafc;
  padding: 24px;
  overflow-y: auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 按钮样式 */
.btn-primary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
}

.btn-primary:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.4);
}

.btn-primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
}

.btn-icon {
  font-size: 16px;
}

/* 搜索区域 */
.search-section {
  margin-bottom: 24px;
}

.search-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.search-form {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input-group {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 16px;
}

.search-input {
  width: 100%;
  padding: 10px 12px 10px 40px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: border-color 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.btn-search {
  padding: 10px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-search:hover {
  background: #2563eb;
}

.btn-reset {
  padding: 10px 20px;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-reset:hover {
  background: #e2e8f0;
  color: #475569;
}

/* 表格区域 */
.table-section {
  margin-bottom: 24px;
}

.table-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.table-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.table-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total-count {
  font-size: 14px;
  color: #64748b;
}

.table-container {
  overflow-x: auto;
}

.role-table {
  width: 100%;
  border-collapse: collapse;
}

.role-table th {
  background: #f8fafc;
  padding: 16px 12px;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
}

.role-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.table-row:hover {
  background: #f8fafc;
}

/* 表格列样式 */
.col-avatar {
  width: 80px;
}

.col-name {
  width: 200px;
}

.col-intro {
  width: 300px;
}

.col-voice {
  width: 120px;
}

.col-language {
  width: 100px;
}

.col-time {
  width: 160px;
}

.col-actions {
  width: 100px;
}

/* 角色头像 */
.role-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.avatar-text {
  color: white;
}

/* 角色信息 */
.role-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.role-code {
  font-size: 12px;
  color: #64748b;
}

.role-intro {
  font-size: 14px;
  color: #475569;
  line-height: 1.4;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 语音信息样式 */
.voice-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.voice-params {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.param-item {
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 3px;
  display: inline-block;
  width: fit-content;
}

/* 标签样式 */
.tag {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.tag-blue {
  background: #dbeafe;
  color: #1e40af;
}

.tag-green {
  background: #dcfce7;
  color: #166534;
}

.tag-gray {
  background: #f1f5f9;
  color: #64748b;
}

/* 时间显示 */
.time-text {
  font-size: 14px;
  color: #64748b;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
}

.btn-action {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  font-size: 14px;
}

.btn-edit {
  background: #dbeafe;
  color: #1e40af;
}

.btn-edit:hover {
  background: #bfdbfe;
  color: #1e3a8a;
}

.btn-delete {
  background: #fee2e2;
  color: #dc2626;
}

.btn-delete:hover {
  background: #fecaca;
  color: #b91c1c;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 48px;
  color: #cbd5e1;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  margin-bottom: 24px;
}

/* 分页 */
.pagination-container {
  padding: 20px 24px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  justify-content: center;
}

/* 表单样式 */
.role-form {
  padding: 8px 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  transition: border-color 0.2s ease;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 滑块样式 */
.form-range {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: #e2e8f0;
  outline: none;
  -webkit-appearance: none;
  appearance: none;
  cursor: pointer;
}

.form-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.form-range::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.range-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.btn-cancel {
  padding: 10px 20px;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel:hover {
  background: #e2e8f0;
  color: #475569;
}

.btn-save {
  padding: 10px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-save:hover:not(:disabled) {
  background: #2563eb;
}

.btn-save:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

/* 删除确认弹窗 */
.delete-confirm {
  text-align: center;
  padding: 20px 0;
}

.delete-icon {
  font-size: 48px;
  color: #f59e0b;
  margin-bottom: 16px;
}

.delete-text {
  font-size: 16px;
  color: #374151;
  margin-bottom: 8px;
}

.delete-warning {
  font-size: 14px;
  color: #64748b;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .role-management-view {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input-group {
    max-width: none;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .role-table {
    font-size: 12px;
  }

  .role-table th,
  .role-table td {
    padding: 8px 6px;
  }

  .col-intro {
    max-width: 200px;
  }
}
</style>
