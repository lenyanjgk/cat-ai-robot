<template>
  <div class="markdown-container">
    <div v-html="renderedContent">
    </div>
  </div>
</template>

<script setup>
import {nextTick, ref, watch} from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import markdownItHighlightJs from 'markdown-it-highlightjs'
import {message} from 'ant-design-vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const renderedContent = ref('')

// 初始化 MarkdownIt
const md = new MarkdownIt({
  html: true,
  xhtmlOut: true,
  linkify: true,
  typographer: true,
  breaks: true,
  langPrefix: 'language-',
})

// 使用代码高亮插件
md.use(markdownItHighlightJs, {
  hljs,
  auto: true,
  code: true
})

// 保存默认的代码块渲染规则
const defaultRender = md.renderer.rules.fence || function (tokens, idx, options, env, renderer) {
  return renderer.renderToken(tokens, idx, options)
}

// 重写代码块渲染规则，添加复制功能
md.renderer.rules.fence = function (tokens, idx, options, env, renderer) {
  const token = tokens[idx]
  const info = token.info ? md.utils.unescapeAll(token.info).trim() : ''
  let langName = ''

  if (info) {
    const langCode = info.split(/\s+/g)[0]
    langName = langCode.toLowerCase()
  }

  const originalContent = defaultRender(tokens, idx, options, env, renderer)
  let finalContent = `<div class="code-block-wrapper">
      <div class="code-header">
      `

  if (langName) {
    finalContent += `<div class="code-language-label">${langName}</div>`
  }

  const codeContent = token.content
  const codeId = `code-${Math.random().toString(36).substr(2, 9)}`

  return finalContent += `
        <button class="copy-code-btn" onclick="copyCode('${codeId}')">  
          <svg t="1750068080826" class="copy-icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1529" 
          width="15" height="15"><path d="M761.088 715.3152a38.7072 38.7072 0 0 1 0-77.4144 37.4272 37.4272 0 0 0 37.4272-37.4272V265.0112a37.4272 37.4272 0 0 0-37.4272-37.4272H425.6256a37.4272 37.4272 0 0 0-37.4272 37.4272 38.7072 38.7072 0 1 1-77.4144 0 115.0976 115.0976 0 0 1 114.8416-114.8416h335.4624a115.0976 115.0976 0 0 1 114.8416 114.8416v335.4624a115.0976 115.0976 0 0 1-114.8416 114.8416z" p-id="1530"></path><path d="M589.4656 883.0976H268.1856a121.1392 121.1392 0 0 1-121.2928-121.2928v-322.56a121.1392 121.1392 0 0 1 121.2928-121.344h321.28a121.1392 121.1392 0 0 1 121.2928 121.2928v322.56c1.28 67.1232-54.1696 121.344-121.2928 121.344zM268.1856 395.3152a43.52 43.52 0 0 0-43.8784 43.8784v322.56a43.52 43.52 0 0 0 43.8784 43.8784h321.28a43.52 43.52 0 0 0 43.8784-43.8784v-322.56a43.52 43.52 0 0 0-43.8784-43.8784z" p-id="1531"></path></svg>
          <span class="copy-text">复制</span>
        </button>
      </div>
      <div class="code-content" id="${codeId}" data-code="${encodeURIComponent(codeContent)}">
        ${originalContent}
      </div>
    </div>`
}

// 初始化复制功能
const setupCopyFunction = () => {
  if (!window.copyCode) {
    window.copyCode = async (codeId) => {
      try {
        const codeElement = document.getElementById(codeId)
        if (!codeElement) return

        const codeContent = decodeURIComponent(codeElement.getAttribute('data-code'))
        await navigator.clipboard.writeText(codeContent)

        const btn = codeElement.parentElement.querySelector('.copy-code-btn')
        if (btn) {
          const originalIcon = btn.querySelector('.copy-icon').innerHTML
          btn.querySelector('.copy-icon').innerHTML = `<path d="M912 190h-69.9c-9.8 0-19.1 4.5-25.1 12.2L404.7 724.5 207 474c-6.1-7.7-15.3-12.2-25.1-12.2H112c-6.7 0-10.4 7.7-6.3 12.9L357.1 864c12.6 16.1 35.5 16.1 48.1 0L918.3 202.9c4.1-5.2 0.4-12.9-6.3-12.9z" p-id="4582"></path>`
          btn.classList.add('copied')
          message.success('复制成功')

          setTimeout(() => {
            btn.querySelector('.copy-icon').innerHTML = originalIcon
            btn.classList.remove('copied')
          }, 1000)
        }
      } catch (err) {
        console.error('复制失败:', err)
      }
    }
  }
}

// 监听内容变化，实时渲染
watch(() => props.content, (newVal) => {
  if (newVal) {
    const html = md.render(newVal)
    renderedContent.value = html

    nextTick(() => {
      setupCopyFunction()
    })
  }
}, {immediate: true})
</script>

<style scoped>
.markdown-container {
  width: 100%;
  line-height: 24px;
  color: rgb(64 64 64);
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
}

/* 第一个段落上边距为0 */
:deep(.markdown-container > p:first-child),
:deep(p:first-child) {
  margin-top: 0;
}

/* 标题样式 */
:deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
  font-weight: 600;
  margin: calc(1.143 * 16px) 0 calc(1.143 * 12px) 0;
}

:deep(h1) {
  font-size: 1.5em;
  margin-top: 1.2em;
  margin-bottom: 0.7em;
  line-height: 1.5;
}

:deep(h2) {
  font-size: 1.3em;
  margin-top: 1.1em;
  margin-bottom: 0.6em;
  line-height: 1.5;
}

:deep(h3) {
  font-size: calc(1.143 * 16px);
  line-height: 1.5;
}

:deep(p) {
  line-height: 1.7;
  margin: calc(1.143 * 12px) 0;
  font-size: calc(1.143 * 14px);
}

:deep(ul) {
  list-style: disc; /* 实心圆点 */
  margin-top: 0.6em;
  margin-bottom: 0.9em;
  padding-left: 2em;
}

:deep(ol) {
  list-style: decimal;
  margin-top: 0.6em;
  margin-bottom: 0.9em;
  padding-left: 2em;
}

/* 列表项样式 */
:deep(li) {
  margin-bottom: 0.5em;
  line-height: 1.7;
}

/* 列表标记样式 */
:deep(ol li::marker) {
  line-height: calc(1.143 * 25px);
  color: rgb(139 139 139);
}

:deep(ul li::marker) {
  color: rgb(139 139 139);
}

/* 嵌套列表 */
:deep(ul ul) {
  list-style: circle;
  margin-top: 0.3em;
  margin-bottom: 0.3em;
}

:deep(ul ul ul) {
  list-style: square;
}

/* 代码块容器 */
:deep(.code-block-wrapper) {
  margin: 1em 0;
  border-radius: 14px;
  overflow: hidden;
  background-color: #f6f8fa;
}

/* 代码块头部 */
:deep(.code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f5f5f5;
  padding: 8px 12px;
}

/* 语言标签 */
:deep(.code-language-label) {
  color: rgb(82 82 82);
  margin-left: 8px;
  font-size: 12px;
  line-height: 18px;
}

/* 代码高亮 */
:deep(.hljs) {
  background: transparent !important;
  padding: 0 !important;
}

/* 复制按钮 */
:deep(.copy-code-btn) {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border-radius: 12px;
  padding: 0 8px;
  color: #586069;
  font-size: 12px;
  height: 28px;
  cursor: pointer;
  transition: all 0.2s ease;
}

:deep(.copy-code-btn.copied .copy-icon) {
  fill: #22c55e;
}

:deep(.copy-code-btn:hover) {
  background-color: rgb(0 0 0 / 4%);
}

:deep(.copy-icon) {
  fill: currentColor;
  flex-shrink: 0;
}

:deep(.copy-text) {
  white-space: nowrap;
}

:deep(pre) {
  background-color: #fafafa;
  padding: 1em;
  border-radius: 5px;
  overflow-x: auto;
  max-width: 100%;
  white-space: pre;
  word-wrap: normal;
}

/* 内联代码 */
:deep(:not(pre) > code) {
  font-size: .875em;
  font-weight: 600;
  background-color: #ececec;
  border-radius: 4px;
  padding: .15rem .3rem;
  margin: 0 .2rem;
}

/* 代码块内代码 */
:deep(pre > code) {
  font-size: .875em;
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-weight: normal;
  color: #333;
  display: block;
  width: 100%;
}

:deep(a) {
  color: #4d6bfe;
  text-decoration: none;
}

:deep(a:hover) {
  text-decoration: underline;
}

:deep(blockquote) {
  border-left: 4px solid #e5e5e5;
  padding-left: 1em;
  margin: 1em 0;
  color: #666;
}

:deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  font-size: 0.95em;
}

:deep(th), :deep(td) {
  border: 1px solid #e5e5e5;
  padding: 0.6em;
  text-align: left;
}

:deep(th) {
  background-color: #f5f5f5;
}

:deep(hr) {
  background-color: rgb(229 229 229);
  margin: 1.5em 0;
  height: 1px;
  border: none;
}

/* 元素间距调整 */
:deep(h1 + p),
:deep(h2 + p),
:deep(h3 + p) {
  margin-top: 0.5em;
}

:deep(p + ul),
:deep(p + ol) {
  margin-top: 0.5em;
}

:deep(ul + p),
:deep(ol + p) {
  margin-top: 0.7em;
}
</style>