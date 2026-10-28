<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '280px' },
  loading: { type: Boolean, default: false },
})

const chartEl = ref(null)
let chart = null
let observer = null

const render = () => {
  if (!chart) return
  chart.setOption(props.option, true)
}

onMounted(async () => {
  await nextTick()
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value, null, { renderer: 'canvas' })
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(chartEl.value)
})

watch(
  () => props.option,
  () => render(),
  { deep: true },
)

watch(
  () => props.loading,
  (loading) => {
    if (!chart) return
    if (loading) chart.showLoading('default', { color: '#2e7364', textColor: '#72827e' })
    else chart.hideLoading()
  },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="chartEl" class="base-chart" :style="{ height }" />
</template>

<style scoped>
.base-chart {
  width: 100%;
}
</style>
