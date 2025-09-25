import { BASE_URL, DEFAULT_HEADERS } from '@/config/api.js'

/**
 * 分页查询角色列表
 * @param {number} current - 当前页码
 * @param {number} size - 每页大小
 * @returns {Promise} API响应
 */
export const getRoles = async (current = 1, size = 20) => {
    try {
        const response = await fetch(`${BASE_URL}/role/page`, {
            method: 'POST',
            headers: DEFAULT_HEADERS,
            body: JSON.stringify({current, size})
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        return data
    } catch (error) {
        console.error('获取角色列表失败:', error)
        return {
            success: false,
            message: '获取角色列表失败',
            data: null
        }
    }
}

/**
 * 根据ID查询角色详情
 * @param {number} id - 角色ID
 * @returns {Promise} API响应
 */
export const getRoleById = async (id) => {
    try {
        const response = await fetch(`${BASE_URL}/role/get?id=${id}`, {
            method: 'POST',
            headers: DEFAULT_HEADERS
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        return data
    } catch (error) {
        console.error('获取角色详情失败:', error)
        return {
            success: false,
            message: '获取角色详情失败',
            data: null
        }
    }
}

/**
 * 创建新角色
 * @param {Object} roleData - 角色数据
 * @returns {Promise} API响应
 */
export const createRole = async (roleData) => {
    try {
        const response = await fetch(`${BASE_URL}/role/create`, {
            method: 'POST',
            headers: DEFAULT_HEADERS,
            body: JSON.stringify(roleData)
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        return data
    } catch (error) {
        console.error('创建角色失败:', error)
        return {
            success: false,
            message: '创建角色失败',
            data: null
        }
    }
}

/**
 * 更新角色信息
 * @param {Object} roleData - 角色数据（包含ID）
 * @returns {Promise} API响应
 */
export const updateRole = async (roleData) => {
    try {
        const response = await fetch(`${BASE_URL}/role/update`, {
            method: 'POST',
            headers: DEFAULT_HEADERS,
            body: JSON.stringify(roleData)
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        return data
    } catch (error) {
        console.error('更新角色失败:', error)
        return {
            success: false,
            message: '更新角色失败',
            data: null
        }
    }
}

/**
 * 删除角色
 * @param {number} id - 角色ID
 * @returns {Promise} API响应
 */
export const deleteRole = async (id) => {
    try {
        const response = await fetch(`${BASE_URL}/role/delete?id=${id}`, {
            method: 'POST',
            headers: DEFAULT_HEADERS
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        return data
    } catch (error) {
        console.error('删除角色失败:', error)
        return {
            success: false,
            message: '删除角色失败',
            data: null
        }
    }
}
