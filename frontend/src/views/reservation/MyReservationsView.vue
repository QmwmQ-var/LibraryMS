<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import BookCover from '@/components/book/BookCover.vue'
import { cancelReservation, getMyReservations } from '@/api/reservation'
import { borrowBook } from '@/api/borrow'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { RESERVATION_STATUS } from '@/utils/constants'

const router = useRouter()
const auth = useAuthStore()
const appStore = useAppStore()

const loading = ref(true)
const records = ref([])
const total = ref(0)
const summary = ref({})
const actionId = ref(null)
const successVisible = ref(false)
const successData = ref(null)

const filters = reactive({
  status: 'ALL',
  keyword: '',
  page: 1,
  size: 8,
})

const tabs = computed(() => [
  { value: 'ALL', label: '全部预约' },
  { value: 'ACTIVE', label: '进行中' },
  { value: 'PENDING', label: '排队中' },
  { value: 'READY', label: '已到书' },
  { value: 'FULFILLED', label: '已借出' },
])

const summaryCards = computed(() => [
  { label: '排队中', value: summary.value.pendingCount || 0, tone: 'blue', icon: 'Clock' },
  { label: '已到书待领取', value: summary.value.readyCount || 0, tone: 'copper', icon: 'Bell' },
  { label: '累计借出', value: summary.value.fulfilledCount || 0, tone: 'jade', icon: 'CircleCheck' },
  { label: '全部预约', value: summary.value.totalCount || 0, tone: 'gray', icon: 'Calendar' },
])

const load = async () => {
  loading.value = true
  try {
    const result = await getMyReservations({
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

const switchTab = (status) => {
  filters.status = status
  filters.page = 1
  load()
}

const search = () => {
  filters.page = 1
  load()
}

const statusTone = (status) => RESERVATION_STATUS[status]?.tone || 'gray'
const statusLabel = (status) => RESERVATION_STATUS[status]?.label || status

const openBook = (reservation) => {
  router.push({ name: 'books', query: { bookId: reservation.bookId } })
}

const cancel = async (reservation) => {
  try {
    await ElMessageBox.confirm(
      `确认取消《${reservation.bookTitle}》的预约吗？取消后如需借阅需重新排队。`,
      '取消预约',
      { confirmButtonText: '取消预约', cancelButtonText: '保留', type: 'warning' },
    )
    actionId.value = `cancel-${reservation.id}`
    await cancelReservation(reservation.id)
    ElMessage.success('预约已取消')
    await load()
    await auth.fetchProfile()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '取消失败')
    }
  } finally {
    actionId.value = null
  }
}

const collect = async (reservation) => {
  try {
    await ElMessageBox.confirm(
      `《${reservation.bookTitle}》已为您留书，确认现在借阅吗？借期按 ${auth.reader?.typeLabel || '读者类型'} 计算。`,
      '到书领取',
      { confirmButtonText: '确认借阅', cancelButtonText: '稍后', type: 'info' },
    )
    actionId.value = `collect-${reservation.id}`
    const result = await borrowBook({ bookId: reservation.bookId })
    successData.value = result
    successVisible.value = true
    await load()
    await auth.fetchProfile()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '借阅失败')
    }
  } finally {
    actionId.value = null
  }
}

const goMyBorrows = () => {
  successVisible.value = false
  router.push({ name: 'my-borrows' })
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
      title="我的预约"
      description="在无书可借时排队等候，到书后将为你保留 48 小时。"
    >
      <template #actions>
        <el-button plain @click="router.push({ name: 'my-borrows' })">
          <el-icon><Collection /></el-icon>
          我的借阅
        </el-button>
        <el-button type="primary" @click="router.push({ name: 'books' })">
          <el-icon><Reading /></el-icon>
          浏览馆藏
        </el-button>
      </template>
    </PageHeader>

    <section class="reservation-banner">
      <div class="reservation-banner__copy">
        <div class="reservation-banner__eyebrow">MY RESERVATION SHELF</div>
        <h2>{{ auth.displayName }}，好书值得等一等。</h2>
        <p>
          <template v-if="summary.readyCount">
            你有 <strong>{{ summary.readyCount }}</strong> 本预约已到书，请在保留时间内领取。
          </template>
          <template v-else-if="summary.pendingCount">
            正在排队 <strong>{{ summary.pendingCount }}</strong> 本，到书后我们会第一时间提醒你。
          </template>
          <template v-else>当前没有进行中的预约，去馆藏里挑一本想读的书吧。</template>
        </p>
      </div>
      <div class="reservation-banner__stats">
        <div>
          <span>排队中</span>
          <strong>{{ summary.pendingCount || 0 }}</strong>
        </div>
        <div>
          <span>已到书</span>
          <strong class="is-ready">{{ summary.readyCount || 0 }}</strong>
        </div>
        <div>
          <span>累计借出</span>
          <strong>{{ summary.fulfilledCount || 0 }}</strong>
        </div>
      </div>
      <span class="reservation-banner__arc" />
      <span class="reservation-banner__stamp">WAIT<br />FOR<br />BOOKS</span>
    </section>

    <div class="summary-cards">
      <div
        v-for="item in summaryCards"
        :key="item.label"
        class="summary-card"
        :class="`is-${item.tone}`"
      >
        <span class="summary-card__icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </div>

    <section class="surface-card reservation-content">
      <div class="reservation-toolbar">
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
          placeholder="搜索书名或 ISBN"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div v-loading="loading" class="reservation-list">
        <article
          v-for="item in records"
          :key="item.id"
          class="reservation-card"
          :class="[`is-${statusTone(item.status)}`, { 'is-ready': item.status === 'READY' }]"
        >
          <div class="reservation-card__cover" @click="openBook(item)">
            <BookCover :book="item" size="sm" flat />
          </div>

          <div class="reservation-card__body">
            <div class="reservation-card__top">
              <span class="pill" :class="`pill--${statusTone(item.status)}`">
                <span class="pill__dot" />
                {{ statusLabel(item.status) }}
              </span>
              <span v-if="item.status === 'READY'" class="reservation-card__expire">
                还剩 {{ item.remainingHours }} 小时
              </span>
              <span v-else-if="item.status === 'PENDING'" class="reservation-card__position">
                第 {{ item.queuePosition }} 位
              </span>
              <span v-else class="reservation-card__time">
                {{ dayjs(item.closedAt || item.requestedAt).format('YYYY-MM-DD') }}
              </span>
            </div>

            <h3 @click="openBook(item)">{{ item.bookTitle }}</h3>
            <p>{{ item.bookAuthor }} · {{ item.categoryName }}</p>

            <div class="reservation-card__meta">
              <span>预约时间 {{ dayjs(item.requestedAt).format('MM-DD HH:mm') }}</span>
              <i />
              <span v-if="item.status === 'READY'">
                到书 {{ dayjs(item.readyAt).format('MM-DD HH:mm') }}
              </span>
              <span v-else-if="item.status === 'PENDING'">前方 {{ item.aheadCount }} 人</span>
              <span v-else-if="item.status === 'FULFILLED'">已生成借阅记录</span>
              <span v-else>{{ item.cancelReason || '预约已结束' }}</span>
            </div>

            <div v-if="item.status === 'READY'" class="reservation-card__ready">
              <span class="reservation-card__ready-icon"><el-icon><BellFilled /></el-icon></span>
              <div>
                <strong>已为你留书</strong>
                <span>请在 {{ dayjs(item.expireAt).format('M月D日 HH:mm') }} 前到馆借阅，逾期将顺延给下一位读者。</span>
              </div>
            </div>

            <div class="reservation-card__footer">
              <button class="text-link" @click="openBook(item)">
                查看馆藏
                <el-icon><ArrowRight /></el-icon>
              </button>
              <div class="reservation-card__actions">
                <template v-if="item.status === 'READY'">
                  <el-button
                    size="small"
                    text
                    :loading="actionId === `cancel-${item.id}`"
                    @click="cancel(item)"
                  >
                    取消预约
                  </el-button>
                  <el-button
                    size="small"
                    type="primary"
                    class="collect-btn"
                    :loading="actionId === `collect-${item.id}`"
                    @click="collect(item)"
                  >
                    到书领取
                  </el-button>
                </template>
                <template v-else-if="item.status === 'PENDING'">
                  <el-button
                    size="small"
                    plain
                    :loading="actionId === `cancel-${item.id}`"
                    @click="cancel(item)"
                  >
                    取消预约
                  </el-button>
                </template>
                <template v-else-if="item.status === 'FULFILLED'">
                  <el-button size="small" plain @click="goMyBorrows">查看借阅</el-button>
                </template>
              </div>
            </div>
          </div>
        </article>

        <div v-if="!records.length && !loading" class="reservation-empty">
          <span><el-icon><Calendar /></el-icon></span>
          <h3>{{ filters.keyword ? '没有匹配的预约记录' : '还没有预约记录' }}</h3>
          <p>
            {{ filters.keyword ? '试试其他书名或 ISBN。' : '当一本书暂时借完时，可以先排队预约。' }}
          </p>
          <el-button type="primary" @click="router.push({ name: 'books' })">浏览馆藏</el-button>
        </div>
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

    <el-dialog v-model="successVisible" width="420px">
      <div v-if="successData" class="collect-result">
        <div class="collect-result__icon"><el-icon><Check /></el-icon></div>
        <div class="collect-result__eyebrow">RESERVATION FULFILLED</div>
        <h2>到书领取成功</h2>
        <p>《{{ successData.record.bookTitle }}》已加入你的借阅书架。</p>
        <div class="collect-result__meta">
          <div>
            <span>借阅日期</span>
            <strong>{{ successData.record.borrowDate }}</strong>
          </div>
          <div>
            <span>应还日期</span>
            <strong>{{ successData.record.dueDate }}</strong>
          </div>
        </div>
        <div class="collect-result__actions">
          <el-button plain @click="successVisible = false">继续浏览</el-button>
          <el-button type="primary" @click="goMyBorrows">查看我的借阅</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.reservation-banner {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 146px;
  padding: 25px 32px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f5ebde;
  background:
    radial-gradient(circle at 82% 20%, rgba(201, 146, 100, 0.2), transparent 32%),
    linear-gradient(120deg, #18463f, #0e3030);
}

.reservation-banner__copy {
  position: relative;
  z-index: 2;
}

.reservation-banner__eyebrow {
  color: rgba(206, 220, 211, 0.38);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.14em;
}

.reservation-banner__copy h2 {
  margin-top: 7px;
  color: #f8efe2;
  font-family: var(--font-display);
  font-size: 25px;
  font-weight: 700;
}

.reservation-banner__copy p {
  margin-top: 6px;
  color: rgba(207, 223, 213, 0.47);
  font-size: 9.5px;
}

.reservation-banner__copy strong {
  color: #e4b583;
  font-family: var(--font-mono);
  font-size: 10px;
}

.reservation-banner__stats {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 38px;
  margin-left: auto;
  padding-right: 86px;
}

.reservation-banner__stats > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reservation-banner__stats span {
  color: rgba(195, 212, 202, 0.38);
  font-size: 8px;
}

.reservation-banner__stats strong {
  color: #f2e4d1;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.reservation-banner__stats strong.is-ready {
  color: #e3b281;
}

.reservation-banner__arc {
  position: absolute;
  top: -80px;
  right: -35px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(225, 195, 163, 0.09);
  border-radius: 50%;
}

.reservation-banner__arc::after {
  position: absolute;
  inset: 34px;
  border: 1px solid rgba(225, 195, 163, 0.06);
  border-radius: 50%;
  content: '';
}

.reservation-banner__stamp {
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

.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.86);
}

.summary-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  border-radius: 8px;
  color: var(--text-500);
  background: var(--paper-100);
  font-size: 14px;
}

.summary-card.is-blue .summary-card__icon {
  color: var(--info);
  background: var(--info-bg);
}

.summary-card.is-copper .summary-card__icon {
  color: var(--copper-600);
  background: var(--copper-100);
}

.summary-card.is-jade .summary-card__icon {
  color: var(--jade-500);
  background: var(--jade-100);
}

.summary-card > div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.summary-card > div span {
  color: var(--text-400);
  font-size: 8px;
}

.summary-card > div strong {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 20px;
  line-height: 1.2;
}

.reservation-content {
  padding: 20px 21px 16px;
}

.reservation-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
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
  width: 210px;
}

.reservation-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 13px;
  min-height: 260px;
  padding-top: 14px;
}

.reservation-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.86);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.reservation-card:hover {
  border-color: var(--line-strong);
  box-shadow: var(--shadow-sm);
}

.reservation-card.is-ready {
  border-color: rgba(181, 121, 75, 0.28);
  background:
    linear-gradient(135deg, rgba(246, 234, 221, 0.58), transparent 52%),
    rgba(253, 251, 247, 0.9);
}

.reservation-card__cover {
  width: 90px;
  flex-shrink: 0;
  cursor: pointer;
}

.reservation-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.reservation-card__top {
  display: flex;
  align-items: center;
  gap: 7px;
}

.reservation-card__top .pill {
  font-size: 7.5px;
}

.reservation-card__expire,
.reservation-card__position {
  margin-left: auto;
  color: var(--copper-600);
  font-family: var(--font-mono);
  font-size: 7.5px;
}

.reservation-card__time {
  margin-left: auto;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
}

.reservation-card h3 {
  overflow: hidden;
  margin-top: 8px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.reservation-card h3:hover {
  color: var(--copper-600);
}

.reservation-card > .reservation-card__body > p {
  overflow: hidden;
  margin-top: 2px;
  color: var(--text-400);
  font-size: 8.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reservation-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: var(--text-400);
  font-size: 7.5px;
}

.reservation-card__meta i {
  width: 1px;
  height: 7px;
  background: var(--line-strong);
}

.reservation-card__ready {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 9px;
  padding: 8px 9px;
  border: 1px solid rgba(181, 121, 75, 0.14);
  border-radius: 7px;
  background: rgba(255, 246, 234, 0.7);
}

.reservation-card__ready-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 50%;
  color: var(--copper-600);
  background: var(--copper-100);
  font-size: 11px;
}

.reservation-card__ready > div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.reservation-card__ready strong {
  color: var(--copper-600);
  font-size: 8.5px;
}

.reservation-card__ready span {
  color: var(--text-500);
  font-size: 7.5px;
  line-height: 1.5;
}

.reservation-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 9px;
}

.text-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  border: 0;
  color: var(--text-400);
  background: transparent;
  font-size: 8px;
  cursor: pointer;
}

.text-link:hover {
  color: var(--copper-600);
}

.text-link .el-icon {
  font-size: 7px;
}

.reservation-card__actions {
  display: flex;
  align-items: center;
  gap: 3px;
}

.reservation-card__actions .el-button {
  height: 24px;
  padding: 0 8px;
  font-size: 8px;
}

.collect-btn {
  background: var(--copper-600);
  border-color: var(--copper-600);
}

.collect-btn:hover {
  background: var(--copper-500);
  border-color: var(--copper-500);
}

.reservation-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  grid-column: span 2;
  padding: 55px 20px;
  text-align: center;
}

.reservation-empty > span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  color: var(--copper-600);
  background: var(--copper-100);
  font-size: 22px;
}

.reservation-empty h3 {
  margin-top: 11px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 17px;
  cursor: default;
}

.reservation-empty p {
  margin: 4px 0 14px;
  color: var(--text-400);
  font-size: 9px;
}

.collect-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 5px 2px;
  text-align: center;
}

.collect-result__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-bottom: 11px;
  border-radius: 50%;
  color: var(--success);
  background: var(--success-bg);
  font-size: 23px;
}

.collect-result__eyebrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.collect-result h2 {
  margin-top: 5px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 22px;
}

.collect-result > p {
  margin-top: 4px;
  color: var(--text-500);
  font-size: 9px;
}

.collect-result__meta {
  display: flex;
  width: 100%;
  gap: 1px;
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--line);
}

.collect-result__meta > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  padding: 10px;
  background: var(--paper-50);
}

.collect-result__meta span {
  color: var(--text-400);
  font-size: 7.5px;
}

.collect-result__meta strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
}

.collect-result__actions {
  display: flex;
  width: 100%;
  gap: 8px;
  margin-top: 18px;
}

.collect-result__actions .el-button {
  flex: 1;
}

@media (max-width: 1180px) {
  .reservation-list {
    grid-template-columns: 1fr;
  }

  .reservation-empty {
    grid-column: span 1;
  }
}

@media (max-width: 900px) {
  .reservation-banner {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
  }

  .reservation-banner__stats {
    margin-left: 0;
    padding-right: 0;
  }

  .reservation-banner__stamp {
    display: none;
  }

  .summary-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .reservation-toolbar {
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

  .reservation-content {
    padding: 17px;
  }
}

@media (max-width: 520px) {
  .reservation-banner {
    padding: 20px;
  }

  .reservation-banner__copy h2 {
    font-size: 20px;
  }

  .reservation-banner__stats {
    gap: 22px;
  }

  .summary-cards {
    gap: 8px;
  }

  .reservation-card__cover {
    width: 76px;
  }

  .reservation-card h3 {
    font-size: 15px;
  }

  .reservation-card__meta {
    display: none;
  }
}
</style>
