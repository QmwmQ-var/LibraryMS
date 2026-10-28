<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import BookCover from '@/components/book/BookCover.vue'
import { getMyBorrows, renewBook, returnBook } from '@/api/borrow'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { BORROW_STATUS, FINE_RATE } from '@/utils/constants'
import { formatCurrency, relativeDue } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const appStore = useAppStore()

const records = ref([])
const total = ref(0)
const loading = ref(true)
const summary = ref({})
const actionId = ref(null)
const resultType = ref('return')
const resultVisible = ref(false)
const actionResult = ref(null)

const filters = reactive({
  status: 'ALL',
  keyword: '',
  page: 1,
  size: 8,
})

const tabs = computed(() => [
  { value: 'ALL', label: '全部借阅', count: summary.value.total || 0 },
  { value: 'BORROWED', label: '借阅中', count: summary.value.active || 0 },
  { value: 'OVERDUE', label: '已逾期', count: summary.value.overdue || 0 },
  { value: 'RETURNED', label: '已归还', count: summary.value.returned || 0 },
])

const reader = computed(() => auth.reader || {})
const quota = computed(() => {
  const max = reader.value.maxBorrow || 1
  return Math.max(0, Math.round(((reader.value.availableQuota ?? 0) / max) * 100))
})

const load = async () => {
  loading.value = true
  try {
    const result = await getMyBorrows({
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

const switchTab = (value) => {
  filters.status = value
  filters.page = 1
  load()
}

const search = () => {
  filters.page = 1
  load()
}

const statusTone = (status) => BORROW_STATUS[status]?.tone || 'gray'
const statusLabel = (status) => BORROW_STATUS[status]?.label || status

const estimatedFine = (record) => {
  if (record.status !== 'OVERDUE') return 0
  return Math.max(record.overdueDays, 0) * FINE_RATE
}

const progress = (record) => {
  const start = dayjs(record.borrowDate)
  const end = dayjs(record.dueDate)
  const totalDays = Math.max(end.diff(start, 'day'), 1)
  const used = Math.max(dayjs().diff(start, 'day'), 0)
  return Math.min(Math.round((used / totalDays) * 100), 100)
}

const confirmReturn = async (record) => {
  const fine = estimatedFine(record)
  try {
    await ElMessageBox.confirm(
      fine
        ? `《${record.bookTitle}》已逾期 ${record.overdueDays} 天，归还后将产生 ${formatCurrency(fine)} 罚款。确认归还吗？`
        : `确认归还《${record.bookTitle}》吗？`,
      '归还图书',
      { confirmButtonText: '确认归还', cancelButtonText: '取消', type: 'warning' },
    )
    actionId.value = `return-${record.id}`
    const result = await returnBook(record.id)
    actionResult.value = result
    resultType.value = 'return'
    resultVisible.value = true
    await load()
    await auth.fetchProfile()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '归还失败')
    }
  } finally {
    actionId.value = null
  }
}

const confirmRenew = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确认续借《${record.bookTitle}》？借期将延长 ${reader.value.borrowDays} 天，每本图书最多续借 1 次。`,
      '续借图书',
      { confirmButtonText: '确认续借', cancelButtonText: '取消', type: 'info' },
    )
    actionId.value = `renew-${record.id}`
    const result = await renewBook(record.id)
    actionResult.value = result
    resultType.value = 'renew'
    resultVisible.value = true
    await load()
    await auth.fetchProfile()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '续借失败')
    }
  } finally {
    actionId.value = null
  }
}

const openBook = (record) => {
  router.push({ name: 'books', query: { bookId: record.bookId } })
}

const onPageChange = (page) => {
  filters.page = page
  load()
}
</script>

<template>
  <div class="app-page">
    <PageHeader
      eyebrow="My Reading"
      title="我的借阅"
      description="查看借阅中的图书、归还期限与阅读历史。"
    >
      <template #actions>
        <el-button plain @click="router.push({ name: 'penalties' })">
          <el-icon><Wallet /></el-icon>
          我的罚款
        </el-button>
        <el-button type="primary" @click="router.push({ name: 'books' })">
          <el-icon><Reading /></el-icon>
          继续借书
        </el-button>
      </template>
    </PageHeader>

    <section class="reading-banner">
      <div class="reading-banner__copy">
        <div class="reading-banner__eyebrow">MY READING JOURNEY</div>
        <h2>{{ auth.displayName }}，你的阅读正在进行。</h2>
        <p>
          当前借阅 <strong>{{ summary.active || 0 }}</strong> 本，
          累计完成 <strong>{{ summary.returned || 0 }}</strong> 本阅读。
          <template v-if="summary.overdue">有 {{ summary.overdue }} 本已逾期，请尽快处理。</template>
          <template v-else>所有图书状态良好，请记得按期归还。</template>
        </p>
      </div>

      <div class="reading-quota">
        <div class="reading-quota__ring" :style="{ '--quota': `${quota * 3.6}deg` }">
          <span>
            <strong>{{ reader.availableQuota ?? '—' }}</strong>
            <small>/ {{ reader.maxBorrow ?? '—' }} 可借</small>
          </span>
        </div>
        <div class="reading-quota__copy">
          <span>借阅额度</span>
          <p>{{ reader.typeLabel }} · 借期 {{ reader.borrowDays }} 天</p>
        </div>
      </div>

      <span class="reading-banner__arc" />
      <span class="reading-banner__stamp">READ<br />MORE</span>
    </section>

    <div class="borrow-tabs">
      <div class="borrow-tabs__list">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="{ 'is-active': filters.status === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
          <span>{{ tab.count }}</span>
        </button>
      </div>
      <el-input
        v-model="filters.keyword"
        class="borrow-search"
        placeholder="搜索书名或 ISBN"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <div v-loading="loading" class="my-borrow-content">
      <div v-if="records.length" class="my-loans">
        <article
          v-for="record in records"
          :key="record.id"
          class="my-loan"
          :class="{ 'is-returned': record.status === 'RETURNED' }"
        >
          <div class="my-loan__cover" @click="openBook(record)">
            <BookCover :book="record" size="sm" flat />
          </div>

          <div class="my-loan__body">
            <div class="my-loan__top">
              <span class="pill" :class="`pill--${statusTone(record.status)}`">
                <span class="pill__dot" />
                {{ statusLabel(record.status) }}
              </span>
              <span v-if="record.status === 'OVERDUE'" class="my-loan__fine">
                预计罚款 {{ formatCurrency(estimatedFine(record)) }}
              </span>
              <span v-else-if="record.status === 'BORROWED'" class="my-loan__due">
                {{ relativeDue(record.dueDate).label }}
              </span>
              <span v-else class="my-loan__due">已归还 {{ record.returnDate }}</span>
            </div>

            <h3 class="my-loan__title" @click="openBook(record)">{{ record.bookTitle }}</h3>
            <p class="my-loan__author">
              {{ record.bookAuthor }} · {{ record.categoryName }}
            </p>

            <div class="my-loan__meta">
              <div>
                <span>借阅日期</span>
                <strong>{{ record.borrowDate }}</strong>
              </div>
              <i />
              <div>
                <span>应还日期</span>
                <strong :class="{ 'is-late': record.status === 'OVERDUE' }">
                  {{ record.dueDate }}
                </strong>
              </div>
              <i />
              <div>
                <span>续借次数</span>
                <strong>{{ record.renewCount }} / 1</strong>
              </div>
            </div>

            <div v-if="['BORROWED', 'OVERDUE'].includes(record.status)" class="my-loan__progress">
              <span
                :style="{ width: `${progress(record)}%` }"
                :class="{ 'is-late': record.status === 'OVERDUE' }"
              />
            </div>

            <div class="my-loan__footer">
              <span class="my-loan__isbn">{{ record.bookIsbn }}</span>
              <div class="my-loan__actions">
                <el-button size="small" text @click="openBook(record)">查看馆藏</el-button>
                <template v-if="['BORROWED', 'OVERDUE'].includes(record.status)">
                  <el-button
                    size="small"
                    plain
                    :disabled="record.status === 'OVERDUE' || record.renewCount >= 1"
                    :loading="actionId === `renew-${record.id}`"
                    @click="confirmRenew(record)"
                  >
                    续借
                  </el-button>
                  <el-button
                    size="small"
                    type="primary"
                    :loading="actionId === `return-${record.id}`"
                    @click="confirmReturn(record)"
                  >
                    归还
                  </el-button>
                </template>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="my-borrow-empty surface-card">
        <div class="my-borrow-empty__icon"><el-icon><Collection /></el-icon></div>
        <h3>{{ filters.status === 'ALL' ? '还没有借阅记录' : '该分类下暂无记录' }}</h3>
        <p>
          {{ filters.status === 'ALL' ? '馆藏中有许多值得一读的书，去发现你的下一本吧。' : '切换其他状态，或更换搜索关键词。' }}
        </p>
        <el-button type="primary" @click="router.push({ name: 'books' })">浏览馆藏</el-button>
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
    </div>

    <el-dialog v-model="resultVisible" width="410px">
      <div v-if="actionResult" class="action-result">
        <div class="action-result__icon">
          <el-icon><Check /></el-icon>
        </div>
        <div class="action-result__eyebrow">
          {{ resultType === 'return' ? 'RETURN COMPLETED' : 'RENEW COMPLETED' }}
        </div>
        <h2>{{ resultType === 'return' ? '归还成功' : '续借成功' }}</h2>
        <p>《{{ actionResult.record ? actionResult.record.bookTitle : actionResult.bookTitle }}》</p>
        <div class="action-result__meta">
          <div>
            <span>{{ resultType === 'return' ? '归还日期' : '新的应还日期' }}</span>
            <strong>
              {{ resultType === 'return' ? actionResult.record.returnDate : actionResult.dueDate }}
            </strong>
          </div>
          <div v-if="resultType === 'return'">
            <span>逾期罚款</span>
            <strong :class="{ 'is-danger': actionResult.penalty }">
              {{ actionResult.penalty ? formatCurrency(actionResult.penalty.amount) : '无' }}
            </strong>
          </div>
          <div v-else>
            <span>续借次数</span>
            <strong>{{ actionResult.renewCount }} / 1</strong>
          </div>
        </div>
        <p v-if="resultType === 'return' && actionResult.penalty" class="action-result__notice">
          罚款已生成，请在「我的罚款」中查看并缴纳。
        </p>
        <el-button type="primary" class="w-full" @click="resultVisible = false">知道了</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.reading-banner {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 145px;
  padding: 25px 32px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f5ebde;
  background:
    radial-gradient(circle at 82% 20%, rgba(201, 146, 100, 0.2), transparent 32%),
    linear-gradient(120deg, #18463f, #0e3030);
}

.reading-banner__eyebrow {
  color: rgba(206, 220, 211, 0.38);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.14em;
}

.reading-banner__copy h2 {
  margin-top: 7px;
  color: #f8efe2;
  font-family: var(--font-display);
  font-size: 25px;
  font-weight: 700;
}

.reading-banner__copy p {
  margin-top: 6px;
  color: rgba(207, 223, 213, 0.47);
  font-size: 9.5px;
}

.reading-banner__copy strong {
  color: #e4b583;
  font-family: var(--font-mono);
  font-size: 10px;
}

.reading-quota {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  margin-right: 55px;
}

.reading-quota__ring {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 87px;
  height: 87px;
  border-radius: 50%;
  background: conic-gradient(
    #d5a372 0 var(--quota),
    rgba(255, 255, 255, 0.08) var(--quota) 360deg
  );
}

.reading-quota__ring > span {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 71px;
  height: 71px;
  border-radius: 50%;
  background: #133a36;
}

.reading-quota__ring strong {
  color: #f8ead7;
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1.1;
}

.reading-quota__ring small {
  color: rgba(200, 216, 207, 0.4);
  font-size: 6.5px;
}

.reading-quota__copy span {
  color: rgba(239, 228, 213, 0.8);
  font-family: var(--font-display);
  font-size: 13px;
}

.reading-quota__copy p {
  margin-top: 3px;
  color: rgba(195, 212, 202, 0.37);
  font-size: 7.5px;
}

.reading-banner__arc {
  position: absolute;
  top: -80px;
  right: -35px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(225, 195, 163, 0.09);
  border-radius: 50%;
}

.reading-banner__arc::after {
  position: absolute;
  inset: 34px;
  border: 1px solid rgba(225, 195, 163, 0.06);
  border-radius: 50%;
  content: '';
}

.reading-banner__stamp {
  position: absolute;
  right: 22px;
  bottom: 19px;
  padding: 5px;
  border: 1px solid rgba(214, 167, 121, 0.3);
  color: rgba(214, 167, 121, 0.5);
  font-family: var(--font-mono);
  font-size: 6px;
  letter-spacing: 0.12em;
  line-height: 1.6;
  text-align: center;
}

.borrow-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line);
}

.borrow-tabs__list {
  display: flex;
  gap: 5px;
}

.borrow-tabs__list button {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  border: 0;
  border-radius: 7px;
  color: var(--text-500);
  background: transparent;
  font-size: 10.5px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.borrow-tabs__list button span {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  color: var(--text-400);
  background: var(--paper-200);
  font-family: var(--font-mono);
  font-size: 7px;
}

.borrow-tabs__list button:hover {
  color: var(--ink-700);
}

.borrow-tabs__list button.is-active {
  color: var(--ink-700);
  background: var(--jade-100);
  font-weight: 600;
}

.borrow-tabs__list button.is-active span {
  color: #f7ede0;
  background: var(--ink-600);
}

.borrow-search {
  width: 220px;
}

.my-borrow-content {
  min-height: 280px;
}

.my-loans {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 13px;
}

.my-loan {
  display: flex;
  gap: 15px;
  padding: 15px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.87);
  box-shadow: var(--shadow-xs);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.my-loan:hover {
  border-color: var(--line-strong);
  box-shadow: var(--shadow-sm);
}

.my-loan.is-returned {
  opacity: 0.82;
}

.my-loan__cover {
  width: 96px;
  flex-shrink: 0;
  padding-top: 2px;
  cursor: pointer;
}

.my-loan__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.my-loan__top {
  display: flex;
  align-items: center;
  gap: 7px;
}

.my-loan__top .pill {
  font-size: 7.5px;
}

.my-loan__due,
.my-loan__fine {
  margin-left: auto;
  color: var(--text-400);
  font-size: 8px;
}

.my-loan__fine {
  color: var(--danger);
}

.my-loan__title {
  overflow: hidden;
  margin-top: 8px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.my-loan__title:hover {
  color: var(--copper-600);
}

.my-loan__author {
  overflow: hidden;
  margin-top: 2px;
  color: var(--text-400);
  font-size: 8.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-loan__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 11px;
}

.my-loan__meta > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.my-loan__meta span {
  color: var(--text-400);
  font-size: 7px;
}

.my-loan__meta strong {
  color: var(--text-600);
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 500;
}

.my-loan__meta strong.is-late {
  color: var(--danger);
}

.my-loan__meta i {
  width: 1px;
  height: 17px;
  background: var(--line);
}

.my-loan__progress {
  height: 3px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.my-loan__progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--jade-500);
}

.my-loan__progress span.is-late {
  background: var(--danger);
}

.my-loan__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 9px;
}

.my-loan__isbn {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
}

.my-loan__actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.my-loan__actions .el-button {
  height: 23px;
  padding: 0 7px;
  font-size: 8px;
}

.my-borrow-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 55px 20px;
  text-align: center;
}

.my-borrow-empty__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 55px;
  height: 55px;
  border-radius: 50%;
  color: var(--jade-500);
  background: var(--jade-100);
  font-size: 23px;
}

.my-borrow-empty h3 {
  margin-top: 12px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 18px;
}

.my-borrow-empty p {
  margin: 4px 0 15px;
  color: var(--text-400);
  font-size: 9.5px;
}

.action-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 4px 2px;
  text-align: center;
}

.action-result__icon {
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

.action-result__eyebrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.action-result h2 {
  margin-top: 5px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 22px;
}

.action-result > p {
  margin-top: 4px;
  color: var(--text-500);
  font-family: var(--font-display);
  font-size: 10px;
}

.action-result__meta {
  display: flex;
  width: 100%;
  gap: 1px;
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--line);
}

.action-result__meta > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  padding: 10px;
  background: var(--paper-50);
}

.action-result__meta span {
  color: var(--text-400);
  font-size: 7.5px;
}

.action-result__meta strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
}

.action-result__meta strong.is-danger {
  color: var(--danger);
}

.action-result__notice {
  margin: 10px 0 12px !important;
  padding: 7px 11px;
  border-radius: 6px;
  color: var(--warning) !important;
  background: var(--warning-bg);
  font-family: var(--font-sans) !important;
  font-size: 8px !important;
}

@media (max-width: 1200px) {
  .my-loans {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .reading-banner {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .reading-quota {
    margin: 0;
  }

  .reading-banner__stamp {
    display: none;
  }

  .borrow-tabs {
    align-items: flex-start;
    flex-direction: column;
  }

  .borrow-tabs__list {
    width: 100%;
    overflow-x: auto;
  }

  .borrow-tabs__list button {
    flex-shrink: 0;
  }

  .borrow-search {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .my-loan__cover {
    width: 78px;
  }

  .my-loan__title {
    font-size: 16px;
  }

  .my-loan__meta {
    gap: 7px;
  }

  .reading-banner {
    padding: 20px;
  }

  .reading-banner__copy h2 {
    font-size: 20px;
  }
}
</style>
