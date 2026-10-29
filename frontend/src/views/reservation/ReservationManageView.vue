<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import { cancelReservation, getReservations } from '@/api/reservation'
import { useAppStore } from '@/stores/app'
import { RESERVATION_STATUS } from '@/utils/constants'
import { initials } from '@/utils/format'

const appStore = useAppStore()

const loading = ref(true)
const records = ref([])
const total = ref(0)
const summary = ref({})
const actionId = ref(null)

const filters = reactive({
  status: 'ALL',
  keyword: '',
  page: 1,
  size: 10,
})

const tabs = [
  { value: 'ALL', label: '全部预约' },
  { value: 'ACTIVE', label: '进行中' },
  { value: 'PENDING', label: '排队中' },
  { value: 'READY', label: '已到书' },
  { value: 'FULFILLED', label: '已借出' },
]

const summaryCards = computed(() => [
  {
    label: '排队中',
    value: summary.value.pendingCount || 0,
    note: '等待可借副本',
    icon: 'Clock',
    tone: 'blue',
  },
  {
    label: '已到书',
    value: summary.value.readyCount || 0,
    note: '已留书待领取',
    icon: 'Bell',
    tone: 'copper',
  },
  {
    label: '已借出',
    value: summary.value.fulfilledCount || 0,
    note: '预约已履约',
    icon: 'CircleCheck',
    tone: 'jade',
  },
  {
    label: '累计预约',
    value: summary.value.totalCount || 0,
    note: `已取消 ${summary.value.cancelledCount || 0} · 过期 ${summary.value.expiredCount || 0}`,
    icon: 'Calendar',
    tone: 'gray',
  },
])

const load = async () => {
  loading.value = true
  try {
    const result = await getReservations({
      status: filters.status,
      keyword: filters.keyword || undefined,
      page: filters.page,
      size: filters.size,
    })
    records.value = result.records
    total.value = result.total
    summary.value = result.summary || {}
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const statusTone = (status) => RESERVATION_STATUS[status]?.tone || 'gray'
const statusLabel = (status) => RESERVATION_STATUS[status]?.label || status

const switchTab = (status) => {
  filters.status = status
  filters.page = 1
  load()
}

const search = () => {
  filters.page = 1
  load()
}

const cancel = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确认为 ${item.readerName} 取消《${item.bookTitle}》的预约吗？若该预约已留书，副本将自动顺延给下一位读者。`,
      '取消预约',
      { confirmButtonText: '确认取消', cancelButtonText: '保留', type: 'warning' },
    )
    actionId.value = item.id
    await cancelReservation(item.id)
    ElMessage.success('预约已取消')
    await load()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '取消失败')
    }
  } finally {
    actionId.value = null
  }
}

const onPageChange = (page) => {
  filters.page = page
  load()
}
</script>

<template>
  <div class="app-page">
    <PageHeader
      eyebrow="Reservations"
      title="预约管理"
      description="管理等候队列、到书留书与领取状态，避免好书在架上空等。"
    >
      <template #actions>
        <el-button plain :loading="loading" @click="load">
          <el-icon><Refresh /></el-icon>
          刷新队列
        </el-button>
        <el-button type="primary" @click="$router.push({ name: 'circulation' })">
          <el-icon><Switch /></el-icon>
          前往流通台
        </el-button>
      </template>
    </PageHeader>

    <section class="reservation-hero">
      <div class="reservation-hero__copy">
        <div class="reservation-hero__eyebrow">RESERVATION DESK · QUEUE CONTROL</div>
        <h2>让等待，也有秩序。</h2>
        <p>
          当前 <strong>{{ summary.pendingCount || 0 }}</strong> 位读者在排队，
          <strong>{{ summary.readyCount || 0 }}</strong> 本图书已到书留书。
          到书保留 48 小时，逾期自动释放并顺延下一位。
        </p>
      </div>
      <div class="reservation-hero__queue">
        <span class="reservation-hero__queue-label">当前等候</span>
        <div class="reservation-hero__queue-value">
          <strong>{{ summary.pendingCount || 0 }}</strong>
          <span>人</span>
        </div>
        <div class="reservation-hero__queue-bar">
          <span :style="{ width: `${Math.min((summary.pendingCount || 0) / 10, 1) * 100}%` }" />
        </div>
      </div>
      <span class="reservation-hero__arc" />
      <span class="reservation-hero__stamp">HOLD<br />48H</span>
    </section>

    <div class="queue-summary">
      <div
        v-for="item in summaryCards"
        :key="item.label"
        class="queue-summary__item"
        :class="`is-${item.tone}`"
      >
        <span class="queue-summary__icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.note }}</small>
        </div>
      </div>
    </div>

    <section class="surface-card reservation-table-card">
      <div class="reservation-table-head">
        <div class="reservation-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="{ 'is-active': filters.status === tab.value }"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
        <el-input
          v-model="filters.keyword"
          class="reservation-search"
          placeholder="搜索读者、学号、书名或 ISBN"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <el-table v-loading="loading" :data="records" row-key="id">
        <el-table-column label="读者" width="160">
          <template #default="{ row }">
            <div class="reservation-reader">
              <span>{{ initials(row.readerName) }}</span>
              <div>
                <strong>{{ row.readerName }}</strong>
                <small>{{ row.readerNo }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预约图书" min-width="220">
          <template #default="{ row }">
            <div class="reservation-book">
              <strong>{{ row.bookTitle }}</strong>
              <span>{{ row.bookIsbn }} · {{ row.categoryName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" width="140">
          <template #default="{ row }">
            <span class="table-time">{{ dayjs(row.requestedAt).format('MM-DD HH:mm') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="队列 / 留书" width="160">
          <template #default="{ row }">
            <div v-if="row.status === 'READY'" class="ready-cell">
              <strong>还剩 {{ row.remainingHours }} 小时</strong>
              <span>{{ dayjs(row.expireAt).format('MM-DD HH:mm') }} 过期</span>
            </div>
            <div v-else-if="row.status === 'PENDING'" class="queue-cell">
              <strong>第 {{ row.queuePosition }} 位</strong>
              <span>前方 {{ row.aheadCount }} 人</span>
            </div>
            <span v-else class="table-time">
              {{ row.closedAt ? dayjs(row.closedAt).format('MM-DD HH:mm') : '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <span class="pill" :class="`pill--${statusTone(row.status)}`">
              <span class="pill__dot" />
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="['PENDING', 'READY'].includes(row.status)"
              text
              size="small"
              :loading="actionId === row.id"
              @click="cancel(row)"
            >
              取消预约
            </el-button>
            <span v-else class="table-done">已结束</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!records.length && !loading" class="reservation-empty">
        <el-icon><Calendar /></el-icon>
        <strong>当前没有预约记录</strong>
        <span>读者可在无可借副本的图书详情页发起预约。</span>
      </div>

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

    <div class="queue-note">
      <span class="queue-note__icon"><el-icon><InfoFilled /></el-icon></span>
      <div>
        <strong>留书与顺延规则</strong>
        <p>
          还书时若有读者排队，系统会自动把副本留给她/他并发送通知；保留 48 小时后自动过期。
          读者领取时，预约状态变为“已借出”；管理员也可在此取消预约，留书会顺延到下一位。
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reservation-hero {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 154px;
  padding: 26px 32px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f5ebde;
  background:
    radial-gradient(circle at 82% 18%, rgba(201, 146, 100, 0.2), transparent 32%),
    linear-gradient(120deg, #1b443f, #0e2d2f);
}

.reservation-hero__copy {
  position: relative;
  z-index: 2;
  max-width: 640px;
}

.reservation-hero__eyebrow {
  color: rgba(206, 220, 211, 0.38);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.14em;
}

.reservation-hero h2 {
  margin-top: 7px;
  color: #f8efe2;
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 700;
}

.reservation-hero p {
  margin-top: 6px;
  color: rgba(207, 223, 213, 0.48);
  font-size: 9.5px;
  line-height: 1.7;
}

.reservation-hero p strong {
  color: #e4b583;
  font-family: var(--font-mono);
  font-size: 10px;
}

.reservation-hero__queue {
  position: relative;
  z-index: 2;
  width: 135px;
  margin-left: auto;
  padding-right: 72px;
  flex-shrink: 0;
}

.reservation-hero__queue-label {
  color: rgba(195, 212, 202, 0.38);
  font-size: 8px;
}

.reservation-hero__queue-value {
  display: flex;
  align-items: baseline;
  gap: 3px;
  margin-top: 2px;
}

.reservation-hero__queue-value strong {
  color: #f3e6d4;
  font-family: var(--font-display);
  font-size: 37px;
  font-weight: 600;
  line-height: 1.1;
}

.reservation-hero__queue-value span {
  color: rgba(195, 212, 202, 0.4);
  font-size: 8px;
}

.reservation-hero__queue-bar {
  height: 3px;
  margin-top: 7px;
  overflow: hidden;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
}

.reservation-hero__queue-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #d5a372;
}

.reservation-hero__arc {
  position: absolute;
  top: -95px;
  right: -45px;
  width: 290px;
  height: 290px;
  border: 1px solid rgba(225, 195, 163, 0.09);
  border-radius: 50%;
}

.reservation-hero__arc::after {
  position: absolute;
  inset: 38px;
  border: 1px solid rgba(225, 195, 163, 0.06);
  border-radius: 50%;
  content: '';
}

.reservation-hero__stamp {
  position: absolute;
  right: 22px;
  bottom: 18px;
  padding: 5px;
  border: 1px solid rgba(214, 167, 121, 0.3);
  color: rgba(214, 167, 121, 0.5);
  font-family: var(--font-mono);
  font-size: 6px;
  letter-spacing: 0.12em;
  line-height: 1.6;
  text-align: center;
}

.queue-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.queue-summary__item {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.87);
}

.queue-summary__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 7px;
  color: var(--text-500);
  background: var(--paper-100);
  font-size: 13px;
}

.queue-summary__item.is-blue .queue-summary__icon {
  color: var(--info);
  background: var(--info-bg);
}

.queue-summary__item.is-copper .queue-summary__icon {
  color: var(--copper-600);
  background: var(--copper-100);
}

.queue-summary__item.is-jade .queue-summary__icon {
  color: var(--jade-500);
  background: var(--jade-100);
}

.queue-summary__item > div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.queue-summary__item > div > span {
  color: var(--text-400);
  font-size: 8px;
}

.queue-summary__item > div > strong {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 21px;
  line-height: 1.2;
}

.queue-summary__item > div > small {
  color: var(--text-400);
  font-size: 7px;
}

.reservation-table-card {
  padding: 20px 21px 16px;
  overflow: hidden;
}

.reservation-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 13px;
  border-bottom: 1px solid var(--line);
}

.reservation-tabs {
  display: flex;
  gap: 3px;
}

.reservation-tabs button {
  padding: 6px 11px;
  border: 0;
  border-radius: 6px;
  color: var(--text-500);
  background: transparent;
  font-size: 9.5px;
  cursor: pointer;
}

.reservation-tabs button.is-active {
  color: var(--ink-700);
  background: var(--jade-100);
  font-weight: 600;
}

.reservation-search {
  width: 245px;
}

.reservation-reader {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reservation-reader > span {
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

.reservation-reader > div {
  display: flex;
  flex-direction: column;
}

.reservation-reader strong {
  color: var(--text-700);
  font-size: 9.5px;
  font-weight: 600;
}

.reservation-reader small {
  color: var(--text-400);
  font-size: 7.5px;
}

.reservation-book {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.reservation-book strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 10.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reservation-book span {
  color: var(--text-400);
  font-size: 7.5px;
}

.table-time {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.ready-cell,
.queue-cell {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.ready-cell strong {
  color: var(--copper-600);
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 600;
}

.queue-cell strong {
  color: var(--info);
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 600;
}

.ready-cell span,
.queue-cell span {
  color: var(--text-400);
  font-size: 7.5px;
}

.table-done {
  color: var(--text-400);
  font-size: 8px;
}

.reservation-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 45px 15px;
  color: var(--text-400);
  text-align: center;
}

.reservation-empty .el-icon {
  margin-bottom: 4px;
  color: var(--copper-500);
  font-size: 26px;
}

.reservation-empty strong {
  color: var(--text-700);
  font-size: 10px;
}

.reservation-empty span {
  font-size: 8px;
}

.queue-note {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.5);
}

.queue-note__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  flex-shrink: 0;
  border-radius: 7px;
  color: var(--copper-600);
  background: var(--copper-100);
  font-size: 13px;
}

.queue-note > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.queue-note strong {
  color: var(--text-700);
  font-size: 9.5px;
}

.queue-note p {
  color: var(--text-400);
  font-size: 8px;
  line-height: 1.7;
}

@media (max-width: 1000px) {
  .queue-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 850px) {
  .reservation-hero {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .reservation-hero__queue {
    margin-left: 0;
    padding-right: 0;
  }

  .reservation-hero__stamp {
    display: none;
  }

  .reservation-table-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .reservation-tabs {
    width: 100%;
    overflow-x: auto;
  }

  .reservation-tabs button {
    flex-shrink: 0;
  }

  .reservation-search {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .reservation-hero {
    padding: 20px;
  }

  .reservation-hero h2 {
    font-size: 21px;
  }

  .queue-summary {
    gap: 8px;
  }

  .reservation-table-card {
    padding: 16px;
  }
}
</style>
