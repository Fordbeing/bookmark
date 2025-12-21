<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
        <div class="modal-container">
          <div class="modal-header">
            <div class="modal-icon" :class="iconClass">
              <span>{{ iconText }}</span>
            </div>
            <h3 class="modal-title">{{ title }}</h3>
          </div>
          <div class="modal-body">
            <p class="modal-message">{{ message }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="handleCancel">{{ cancelText }}</button>
            <button class="btn" :class="confirmBtnClass" @click="handleConfirm">{{ confirmText }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  message: { type: String, default: '确定要执行此操作吗？' },
  type: { type: String, default: 'warning' }, // warning, danger, info
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' }
})

const emit = defineEmits(['confirm', 'cancel', 'update:visible'])

const iconClass = computed(() => `icon-${props.type}`)
const iconText = computed(() => {
  const icons = { warning: '⚠️', danger: '❌', info: 'ℹ️', success: '✅' }
  return icons[props.type] || '⚠️'
})
const confirmBtnClass = computed(() => {
  const classes = { warning: 'btn-warning', danger: 'btn-danger', info: 'btn-primary', success: 'btn-success' }
  return classes[props.type] || 'btn-primary'
})

function handleConfirm() {
  emit('confirm')
  emit('update:visible', false)
}

function handleCancel() {
  emit('cancel')
  emit('update:visible', false)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.modal-container {
  background: var(--bg-secondary, #1e1e2e);
  border-radius: 16px;
  padding: 24px;
  min-width: 320px;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  border: 1px solid var(--border-color, rgba(255,255,255,0.1));
}

.modal-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.modal-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.icon-warning { background: rgba(245, 158, 11, 0.2); }
.icon-danger { background: rgba(239, 68, 68, 0.2); }
.icon-info { background: rgba(59, 130, 246, 0.2); }
.icon-success { background: rgba(34, 197, 94, 0.2); }

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.modal-body {
  text-align: center;
  margin-bottom: 24px;
}

.modal-message {
  color: #d1d5db;
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
}

.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-footer .btn {
  min-width: 100px;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
}

.btn-warning {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
}
.btn-warning:hover { background: linear-gradient(135deg, #d97706, #b45309); }

.btn-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
}
.btn-danger:hover { background: linear-gradient(135deg, #dc2626, #b91c1c); }

/* Transition */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-active .modal-container,
.modal-fade-leave-active .modal-container {
  transition: transform 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
.modal-fade-enter-from .modal-container,
.modal-fade-leave-to .modal-container {
  transform: scale(0.9);
}
</style>
