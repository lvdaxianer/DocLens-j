<script setup lang="ts">
import { onMounted } from 'vue'
import { RefreshCcw, Save, ShieldCheck } from '@lucide/vue'
import { NAlert, NButton, NForm, NFormItem, NIcon, NInputNumber, useMessage } from 'naive-ui'

import { useOcrGovernanceConfig } from '@/composables/useOcrGovernanceConfig'
import { formatDateTime } from '@/utils/formatters'

const message = useMessage()
const governanceConfig = useOcrGovernanceConfig(message)

onMounted(governanceConfig.loadConfig)
</script>

<template>
  <section class="panel ocr-governance-panel">
    <div class="panel__header ocr-governance-panel__header">
      <div>
        <h2 class="panel__title">OCR 全局治理</h2>
        <span class="panel__hint">最后刷新：{{ formatDateTime(governanceConfig.lastLoadedAt.value) }}</span>
      </div>
      <div class="ocr-governance-panel__actions">
        <NButton size="small" :loading="governanceConfig.isLoading.value" @click="governanceConfig.loadConfig">
          <template #icon>
            <NIcon :component="RefreshCcw" />
          </template>
          刷新
        </NButton>
      </div>
    </div>

    <NAlert
      v-if="governanceConfig.errorMessage.value"
      class="ocr-governance-panel__alert"
      type="error"
      :title="governanceConfig.errorMessage.value"
    />
    <NAlert class="ocr-governance-panel__alert" type="info" title="保存后将影响后续健康检查、熔断窗口和手动连接恢复次数">
      周期探测与手动连接都会读取当前最新治理配置，不需要重启服务。
    </NAlert>

    <NForm class="ocr-governance-panel__form" label-placement="top">
      <NFormItem label="连续失败摘除阈值">
        <NInputNumber v-model:value="governanceConfig.form.failureThreshold" :min="1" :precision="0" />
      </NFormItem>
      <NFormItem label="周期探测间隔（秒）">
        <NInputNumber v-model:value="governanceConfig.form.probeIntervalSeconds" :min="1" :precision="0" />
      </NFormItem>
      <NFormItem label="熔断打开时长（秒）">
        <NInputNumber v-model:value="governanceConfig.form.circuitOpenSeconds" :min="1" :precision="0" />
      </NFormItem>
      <NFormItem label="恢复成功阈值">
        <NInputNumber v-model:value="governanceConfig.form.recoverySuccessThreshold" :min="1" :precision="0" />
      </NFormItem>
      <NFormItem label="手动恢复尝试次数">
        <NInputNumber v-model:value="governanceConfig.form.manualRecoveryAttempts" :min="1" :precision="0" />
      </NFormItem>
    </NForm>

    <div class="ocr-governance-panel__footer">
      <span>建议在低峰期调整探测间隔和熔断窗口，避免频繁震荡。</span>
      <NButton
        type="primary"
        :loading="governanceConfig.isSaving.value"
        :disabled="!governanceConfig.canSubmit.value"
        @click="governanceConfig.saveConfig"
      >
        <template #icon>
          <NIcon :component="Save" />
        </template>
        保存治理配置
      </NButton>
      <NButton secondary @click="governanceConfig.loadConfig">
        <template #icon>
          <NIcon :component="ShieldCheck" />
        </template>
        读取当前生效值
      </NButton>
    </div>
  </section>
</template>

<style scoped>
.ocr-governance-panel {
  overflow: hidden;
}

.ocr-governance-panel__header {
  align-items: flex-start;
}

.ocr-governance-panel__actions,
.ocr-governance-panel__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.ocr-governance-panel__alert {
  margin-bottom: 12px;
}

.ocr-governance-panel__form {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(5, minmax(160px, 1fr));
  gap: 12px;
}

.ocr-governance-panel__footer {
  margin-top: 4px;
}

.ocr-governance-panel__footer span {
  min-width: 0;
  color: var(--ink-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}

@media (max-width: 1280px) {
  .ocr-governance-panel__form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 780px) {
  .ocr-governance-panel__form {
    grid-template-columns: 1fr;
  }

  .ocr-governance-panel__header,
  .ocr-governance-panel__actions,
  .ocr-governance-panel__footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
