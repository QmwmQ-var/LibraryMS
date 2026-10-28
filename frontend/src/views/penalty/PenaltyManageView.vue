<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import { getPenalties, payPenalty } from '@/api/penalty'
import { useAppStore } from '@/stores/app'
import { FINE_RATE } from '@/utils/constants'
import { formatCurrency, formatDateTime, initials } from '@/utils/format'

const appStore = useAppStore()
const penalties = ref([])
const total = ref(0)
const summary = ref({})
const loading = ref(true)
const payingId = ref(null)
const paidItem = ref(null)
const resultVisible = ref(false)

const filters = reactive({
  keyword: '',
  status: 'ALL',
  page: 1,
  size: 10,
})

const stats = computed(() => [
  {
    label: '待收金额',
    value: formatCurrency(summary.value.pendingAmount),
    note: `${summary.value.pendingCount || 0} 笔待缴`,
    tone: 'amber',
    icon: 'Wallet',
  },
  {
    label: '本月已收',
    value: formatCurrency(summary.value.paidThisMonth),
    note: `${summary.value.paidThisMonthCount || 0} 笔已缴纳`,
    tone: 'jade',
    icon: 'TrendCharts',
  },
  {
    label: '累计罚款',
    value: formatCurrency(summary.value.collectedTotal),
    note: '历史已收金额',
    tone: 'blue',
    icon: 'Money',
  },
  {
    label: '每日费率',
    value: `¥${FINE_RATE.toFixed(2)}`,
    note: '系统配置项 · 可按需调整',
    tone: 'copper',
    icon: 'Setting',
  },
])

const load = async () => {
  loading.value = true
  try {
    const result = await getPenalties({
      keyword: filters.keyword || undefined,
      status: filters.status,
      page: filters.page,
      size: filters.size,
    })
    penalties.value = result.records
    total.value = result.total
    summary.value = result.summary || {}
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const search = () => {
  filters.page = 1
  load()
}

const reset = () => {
  Object.assign(filters, { keyword: '', status: 'ALL', page: 1 })
  load()
}

const confirmPay = async (penalty) => {
  try {
    await ElMessageBox.confirm(
      `确认收取 ${penalty.readerName} 的罚款 ${formatCurrency(penalty.amount)}？确认后将标记为已缴纳。`,
      '确认缴纳',
      { confirmButtonText: '确认收款', cancelButtonText: '取消', type: 'warning' },
    )
    payingId.value = penalty.id
    const result = await payPenalty(penalty.id)
    paidItem.value = result
    resultVisible.value = true
    await load()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '操作失败')
    }
  } finally {
    payingId.value = null
  }
}

const exportCsv = () => {
  if (!penalties.value.length) {
    ElMessage.info('当前没有可导出的罚款记录')
    return
  }
  const header = ['读者', '学号', '图书', '逾期天数', '金额', '状态', '生成时间', '缴纳时间']
  const lines = penalties.value.map((item) => [
    item.readerName,
    item.readerNo,
    item.bookTitle,
    item.overdueDays,
    item.amount.toFixed(2),
    item.status === 'PAID' ? '已缴纳' : '待缴纳',
    item.createdAt ? formatDateTime(item.createdAt) : '',
    item.paidAt ? formatDateTime(item.paidAt) : '',
  ])
  const csv = [header, ...lines]
    .map((row) => row.map((cell) => `"${String(cell ?? '').replaceAll('"', '""')}"`).join(','))
    .join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `图书馆罚款明细_${dayjs().format('YYYYMMDD_HHmm')}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('罚款明细已导出')
}

const onPageChange = (page) => {
  filters.page = page
  load()
}
</script>

<template>
  <div class="app-page">
    <PageHeader
      eyebrow="Fines"
      title="罚款管理"
      description="核对逾期罚款、确认缴纳并查看历史收款记录。"
    >
      <template #actions>
        <el-button plain @click="exportCsv">
          <el-icon><Download /></el-icon>
          导出明细
        </el-button>
        <el-button type="primary" @click="load">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </PageHeader>

    <div class="fine-stats">
      <div
        v-for="item in stats"
        :key="item.label"
        class="fine-stat"
        :class="`is-${item.tone}`"
      >
        <div class="fine-stat__top">
          <span>{{ item.label }}</span>
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <strong>{{ item.value }}</strong>
        <p>{{ item.note }}</p>
      </div>
    </div>

    <section class="surface-card penalty-table-card">
      <div class="penalty-table-head">
        <div>
          <div class="eyebrow">Fine Ledger</div>
          <h3>罚款台账</h3>
          <p>逾期天数 × 每日费率生成，读者归还时自动入账</p>
        </div>
        <div class="penalty-rule">
          <span>收费标准</span>
          <strong>¥{{ FINE_RATE.toFixed(2) }} / 天</strong>
        </div>
      </div>

      <div class="filter-bar penalty-filter">
        <el-input
          v-model="filters.keyword"
          class="search-input"
          placeholder="搜索读者、学号、书名或 ISBN"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.status" placeholder="全部状态" @change="search">
          <el-option label="全部状态" value="ALL" />
          <el-option label="待缴纳" value="UNPAID" />
          <el-option label="已缴纳" value="PAID" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="penalty-count">共 {{ total }} 笔</span>
      </div>

      <el-table v-loading="loading" :data="penalties" row-key="id">
        <el-table-column label="读者" width="160">
          <template #default="{ row }">
            <div class="penalty-reader">
              <span>{{ initials(row.readerName) }}</span>
              <div>
                <strong>{{ row.readerName }}</strong>
                <small>{{ row.readerNo }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="图书" min-width="205">
          <template #default="{ row }">
            <div class="penalty-book">
              <strong>{{ row.bookTitle }}</strong>
              <span>{{ row.bookIsbn }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="逾期" width="92">
          <template #default="{ row }">
            <span class="overdue-days">{{ row.overdueDays }} 天</span>
          </template>
        </el-table-column>
        <el-table-column label="罚款金额" width="105">
          <template #default="{ row }">
            <strong class="penalty-amount">{{ formatCurrency(row.amount) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <span class="pill" :class="row.status === 'PAID' ? 'pill--green' : 'pill--amber'">
              <span class="pill__dot" />
              {{ row.status === 'PAID' ? '已缴纳' : '待缴纳' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="145">
          <template #default="{ row }">
            <span class="table-time">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缴纳时间" width="145">
          <template #default="{ row }">
            <span class="table-time">{{ row.paidAt ? formatDateTime(row.paidAt) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'UNPAID'"
              size="small"
              type="primary"
              plain
              :loading="payingId === row.id"
              @click="confirmPay(row)"
            >
              确认缴纳
            </el-button>
            <span v-else class="table-done"><el-icon><Check /></el-icon>已结清</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > filters.size"
        background
        layout="prev, pager, next, total"
        :current-page="filters.page"
        :page-size="filters.size"
        :total="total"
        @current-change="onPageChange"
      />
    </section>

    <el-dialog v-model="resultVisible" width="390px">
      <div v-if="paidItem" class="fine-result">
        <div class="fine-result__icon"><el-icon><Check /></el-icon></div>
        <div class="fine-result__eyebrow">PAYMENT CONFIRMED</div>
        <h2>收款成功</h2>
        <p>{{ paidItem.readerName }} · 《{{ paidItem.bookTitle }}》</p>
        <div class="fine-result__amount">{{ formatCurrency(paidItem.amount) }}</div>
        <div class="fine-result__meta">
          <span>缴纳时间</span>
          <strong>{{ formatDateTime(paidItem.paidAt) }}</strong>
        </div>
        <el-button type="primary" class="w-full" @click="resultVisible = false">完成</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.fine-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.fine-stat {
  position: relative;
  padding: 15px 17px 13px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.87);
}

.fine-stat__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.fine-stat__top span {
  color: var(--text-500);
  font-size: 9px;
}

.fine-stat__top .el-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  color: var(--text-400);
  background: var(--paper-100);
  font-size: 12px;
}

.fine-stat.is-amber .fine-stat__top .el-icon {
  color: var(--warning);
  background: var(--warning-bg);
}

.fine-stat.is-jade .fine-stat__top .el-icon {
  color: var(--jade-500);
  background: var(--jade-100);
}

.fine-stat.is-blue .fine-stat__top .el-icon {
  color: var(--info);
  background: var(--info-bg);
}

.fine-stat.is-copper .fine-stat__top .el-icon {
  color: var(--copper-600);
  background: var(--copper-100);
}

.fine-stat > strong {
  display: block;
  margin-top: 9px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 700;
  line-height: 1.1;
}

.fine-stat > p {
  margin-top: 4px;
  color: var(--text-400);
  font-size: 7.5px;
}

.fine-stat::after {
  position: absolute;
  right: -24px;
  bottom: -38px;
  width: 85px;
  height: 85px;
  border: 1px solid rgba(46, 115, 100, 0.05);
  border-radius: 50%;
  content: '';
}

.penalty-table-card {
  padding: 21px 21px 16px;
  overflow: hidden;
}

.penalty-table-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 15px;
}

.penalty-table-head .eyebrow {
  margin-bottom: 4px;
}

.penalty-table-head h3 {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 19px;
}

.penalty-table-head p {
  margin-top: 3px;
  color: var(--text-400);
  font-size: 8.5px;
}

.penalty-rule {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  padding: 7px 11px;
  border: 1px dashed var(--line-strong);
  border-radius: 7px;
  background: var(--paper-100);
}

.penalty-rule span {
  color: var(--text-400);
  font-size: 7px;
}

.penalty-rule strong {
  color: var(--copper-600);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 500;
}

.penalty-filter .search-input {
  flex: 1;
  width: auto;
  min-width: 220px;
}

.penalty-count {
  margin-left: auto;
  color: var(--text-400);
  font-size: 8.5px;
}

.penalty-reader {
  display: flex;
  align-items: center;
  gap: 8px;
}

.penalty-reader > span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  background: var(--ink-500);
  font-family: var(--font-display);
  font-size: 8px;
}

.penalty-reader > div {
  display: flex;
  flex-direction: column;
}

.penalty-reader strong {
  color: var(--text-700);
  font-size: 9.5px;
  font-weight: 600;
}

.penalty-reader small {
  color: var(--text-400);
  font-size: 7.5px;
}

.penalty-book {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.penalty-book strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 10.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.penalty-book span {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
}

.overdue-days {
  color: var(--danger);
  font-family: var(--font-mono);
  font-size: 9px;
}

.penalty-amount {
  color: var(--danger);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
}

.table-time {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.table-done {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--success);
  font-size: 8px;
}

.table-done .el-icon {
  font-size: 9px;
}

.fine-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 5px 2px;
  text-align: center;
}

.fine-result__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  margin-bottom: 11px;
  border-radius: 50%;
  color: var(--success);
  background: var(--success-bg);
  font-size: 22px;
}

.fine-result__eyebrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.fine-result h2 {
  margin-top: 5px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 22px;
}

.fine-result > p {
  margin-top: 4px;
  color: var(--text-500);
  font-size: 9px;
}

.fine-result__amount {
  margin: 12px 0;
  color: var(--success);
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 600;
}

.fine-result__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  margin-bottom: 15px;
  padding: 8px 10px;
  border-radius: 7px;
  background: var(--paper-100);
}

.fine-result__meta span {
  color: var(--text-400);
  font-size: 7px;
}

.fine-result__meta strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 500;
}

@media (max-width: 1000px) {
  .fine-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .fine-stats {
    gap: 8px;
  }

  .penalty-table-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .penalty-rule {
    align-items: flex-start;
  }

  .penalty-count {
    display: none;
  }

  .penalty-table-card {
    padding: 17px;
  }
}
</style>
