/**
 * API 配置文件
 * 统一管理所有 API 相关配置
 */

// 开发环境配置
const config = {
  // API 基础地址 - 支持环境变量
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',
  
  // 请求超时时间（毫秒）
  TIMEOUT: import.meta.env.VITE_API_TIMEOUT || 30000,
  
  // 默认请求头
  DEFAULT_HEADERS: {
    'Content-Type': 'application/json',
  },
  
  // 流式请求头
  STREAM_HEADERS: {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    'Cache-Control': 'no-cache'
  }
}

export default config

// 导出常用配置
export const { BASE_URL, TIMEOUT, DEFAULT_HEADERS, STREAM_HEADERS } = config
