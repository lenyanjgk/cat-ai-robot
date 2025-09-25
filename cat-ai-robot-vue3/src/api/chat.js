/**
 * 聊天相关API接口
 */

const BASE_URL = 'http://localhost:8081'

/**
 * 新建对话
 * @param {string} message - 用户消息
 * @param {number} roleId - 角色ID
 * @returns {Promise}
 */
export const newChat = async (message, roleId) => {
    const response = await fetch(`${BASE_URL}/chat/new`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({message, roleId})
    })

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
}

/**
 * 获取历史对话列表
 * @param {number} current - 当前页码
 * @param {number} size - 每页数量
 * @returns {Promise}
 */
export const getChatHistory = async (current = 1, size = 20) => {
    const response = await fetch(`${BASE_URL}/chat/list`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({current, size})
    })

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
}

/**
 * 获取对话历史消息
 * @param {string} chatId - 对话ID
 * @param {number} current - 当前页码
 * @param {number} size - 每页数量
 * @returns {Promise}
 */
export const getChatMessages = async (chatId, current = 1, size = 50) => {
    const response = await fetch(`${BASE_URL}/chat/message/list`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({chatId, current, size})
    })

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
}

/**
 * 重命名对话
 * @param {number} id - 对话ID
 * @param {string} summary - 新的摘要
 * @returns {Promise}
 */
export const renameChat = async (id, summary) => {
    const response = await fetch(`${BASE_URL}/chat/summary/rename`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({id, summary})
    })

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
}

/**
 * 删除对话
 * @param {string} uuid - 对话UUID
 * @returns {Promise}
 */
export const deleteChat = async (uuid) => {
    const response = await fetch(`${BASE_URL}/chat/delete`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({uuid})
    })

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
}

/**
 * 发送流式聊天消息
 * @param {Object} params - 聊天参数
 * @param {string} params.message - 用户消息
 * @param {string} params.chatId - 对话ID
 * @param {string} params.modelName - 模型名称
 * @param {number} params.temperature - 温度值
 * @param {boolean} params.networkSearch - 是否联网搜索
 * @returns {Promise} - 返回流式读取器
 */
export const sendChatMessage = async (params) => {
    const {message, chatId, modelName = 'qwen-plus', temperature = 0.7, networkSearch = false} = params

    console.log('发送聊天请求:', params)

    const response = await fetch(`${BASE_URL}/chat/completion`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache'
        },
        body: JSON.stringify({
            message,
            chatId,
            modelName,
            temperature,
            networkSearch
        })
    })

    console.log('响应状态:', response.status, response.statusText)
    console.log('响应头:', [...response.headers.entries()])

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')

    return {
        reader,
        decoder,
        response
    }
}

/**
 * 根据对话ID获取角色信息
 * @param {string} chatId - 对话ID
 * @returns {Promise}
 */
export const getChatRoleInfo = async (chatId) => {
    const response = await fetch(`${BASE_URL}/chat/role/info?chatId=${chatId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
    }

    return await response.json()
}