<script setup lang="ts">
import { FlaskConical, Save } from '@lucide/vue'
import {
  NButton,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NSelect,
  NSwitch
} from 'naive-ui'

import type { LlmMarkdownConfigFormState, LlmConfigCapabilityHints } from '@/utils/llmMarkdownConfigRules'
import type { LlmMarkdownApiType } from '@/types/llmMarkdownConfig'

/**
 * LLM Markdown 配置抽屉，承载创建和编辑表单。
 *
 * @author lvdaxianerplus
 * @date 2026-06-19
 */
const props = defineProps<{
  visible: boolean
  title: string
  form: LlmMarkdownConfigFormState
  saving: boolean
  testing: boolean
  canSubmit: boolean
  capabilityHints: LlmConfigCapabilityHints
}>()

const emit = defineEmits<{
  close: []
  save: []
  test: []
}>()

const apiTypeOptions = [
  { label: 'OpenAI compatible', value: 'openai' satisfies LlmMarkdownApiType },
  { label: 'Anthropic', value: 'anthropic' satisfies LlmMarkdownApiType }
]
</script>

<template>
  <NDrawer :show="visible" :width="540" placement="right" @update:show="emit('close')">
    <NDrawerContent :title="title" closable>
      <p class="llm-config-drawer__hint">
        用户只需输入完整接口地址。
      </p>

      <NForm class="llm-config-drawer__form" label-placement="top">
        <NFormItem label="配置名称">
          <NInput v-model:value="form.name" placeholder="例如：主配置" />
        </NFormItem>
        <NFormItem label="协议">
          <NSelect v-model:value="form.apiType" :options="apiTypeOptions" />
        </NFormItem>
        <NFormItem label="完整地址">
          <NInput v-model:value="form.url" :placeholder="capabilityHints.urlPlaceholder" />
        </NFormItem>
        <NFormItem label="模型名称">
          <NInput v-model:value="form.model" placeholder="markdown-model" />
        </NFormItem>
        <NFormItem label="API Key 环境变量名">
          <NInput
            v-model:value="form.credentialEnvVar"
            placeholder="例如：MINIMAX_API_KEY"
          />
        </NFormItem>
        <NFormItem label="最大上下文 Token 数">
          <NInputNumber
            v-model:value="form.maxContextTokens"
            :min="1000"
            :precision="0"
            placeholder="例如：16000"
          />
        </NFormItem>
        <NFormItem label="最大并发数">
          <NInputNumber
            v-model:value="form.maxConcurrency"
            :min="1"
            :precision="0"
            placeholder="例如：1"
          />
        </NFormItem>
        <NFormItem label="请求间隔（毫秒）">
          <NInputNumber
            v-model:value="form.requestIntervalMillis"
            :min="0"
            :precision="0"
            placeholder="例如：1000"
          />
        </NFormItem>
        <NFormItem label="优先级">
          <NInputNumber v-model:value="form.priority" :min="0" :precision="0" />
        </NFormItem>

        <div class="llm-config-drawer__toggles">
          <label><NSwitch v-model:value="form.enabled" /> 启用</label>
          <label><NSwitch v-model:value="form.defaultConfig" /> 设为默认</label>
        </div>
      </NForm>

      <template #footer>
        <div class="llm-config-drawer__footer">
          <span>
            凭证：{{ form.credentialEnvVar || '未填写环境变量名' }}
            / {{ form.credentialConfigured ? '已配置' : '未配置' }}
          </span>
          <NButton
            secondary
            :loading="testing"
            :disabled="!capabilityHints.canTest"
            @click="emit('test')"
          >
            <template #icon>
              <NIcon :component="FlaskConical" />
            </template>
            测试配置
          </NButton>
          <NButton
            type="primary"
            :loading="saving"
            :disabled="!canSubmit"
            @click="emit('save')"
          >
            <template #icon>
              <NIcon :component="Save" />
            </template>
            保存
          </NButton>
        </div>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.llm-config-drawer__hint {
  margin: 0 0 14px;
  color: var(--ink-muted);
  font-size: 12px;
}

.llm-config-drawer__form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.llm-config-drawer__toggles,
.llm-config-drawer__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.llm-config-drawer__toggles {
  justify-content: flex-start;
}

.llm-config-drawer__toggles label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 650;
}

.llm-config-drawer__footer {
  color: var(--ink-muted);
  font-size: 12px;
}
</style>
