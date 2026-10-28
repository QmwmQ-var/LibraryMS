<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '—' },
  suffix: { type: String, default: '' },
  icon: { type: String, default: 'DataLine' },
  tone: { type: String, default: 'jade' },
  note: { type: String, default: '' },
  trend: { type: [String, Number], default: null },
  trendLabel: { type: String, default: '较上月' },
  loading: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false },
})
</script>

<template>
  <div
    class="stat-card"
    :class="[`tone-${tone}`, { 'is-clickable': clickable }]"
    :tabindex="clickable ? 0 : undefined"
  >
    <div class="stat-card__top">
      <span class="stat-card__label">{{ label }}</span>
      <span class="stat-card__icon">
        <el-icon :size="17"><component :is="icon" /></el-icon>
      </span>
    </div>

    <div v-if="loading" class="stat-card__skeleton">
      <span />
    </div>
    <div v-else class="stat-card__value">
      {{ value }}<small v-if="suffix">{{ suffix }}</small>
    </div>

    <div class="stat-card__bottom">
      <span v-if="trend !== null" class="stat-card__trend" :class="{ 'is-down': Number(trend) < 0 }">
        <el-icon>
          <Top v-if="Number(trend) >= 0" />
          <Bottom v-else />
        </el-icon>
        {{ Math.abs(Number(trend)) }}%
      </span>
      <span v-if="trend !== null" class="stat-card__trend-label">{{ trendLabel }}</span>
      <span v-if="note" class="stat-card__note">{{ note }}</span>
    </div>

    <span class="stat-card__ornament" />
  </div>
</template>

<style scoped>
.stat-card {
  position: relative;
  min-width: 0;
  padding: 17px 18px 15px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.85);
  box-shadow: var(--shadow-xs);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.stat-card:hover {
  border-color: var(--line-strong);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}

.stat-card.is-clickable {
  cursor: pointer;
}

.stat-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stat-card__label {
  color: var(--text-500);
  font-size: 11px;
  font-weight: 550;
  letter-spacing: 0.03em;
}

.stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  border-radius: 9px;
  color: var(--jade-500);
  background: var(--jade-100);
}

.tone-copper .stat-card__icon {
  color: var(--copper-600);
  background: var(--copper-100);
}

.tone-amber .stat-card__icon {
  color: var(--warning);
  background: var(--warning-bg);
}

.tone-danger .stat-card__icon {
  color: var(--danger);
  background: var(--danger-bg);
}

.tone-blue .stat-card__icon {
  color: var(--info);
  background: var(--info-bg);
}

.stat-card__value {
  margin-top: 12px;
  color: var(--ink-900);
  font-family: var(--font-display);
  font-size: 31px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.1;
  white-space: nowrap;
}

.stat-card__value small {
  margin-left: 4px;
  color: var(--text-400);
  font-family: var(--font-sans);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.stat-card__bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-height: 18px;
  margin-top: 8px;
}

.stat-card__trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--success);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 600;
}

.stat-card__trend.is-down {
  color: var(--danger);
}

.stat-card__trend-label,
.stat-card__note {
  color: var(--text-400);
  font-size: 9px;
}

.stat-card__ornament {
  position: absolute;
  right: -18px;
  bottom: -34px;
  width: 82px;
  height: 82px;
  border: 1px solid rgba(46, 115, 100, 0.06);
  border-radius: 50%;
  pointer-events: none;
}

.tone-copper .stat-card__ornament {
  border-color: rgba(181, 121, 75, 0.08);
}

.stat-card__skeleton {
  display: flex;
  align-items: center;
  height: 42px;
  margin-top: 8px;
}

.stat-card__skeleton span {
  width: 74px;
  height: 24px;
  border-radius: 5px;
  background: linear-gradient(90deg, #f1e9dc, #faf6ef, #f1e9dc);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
