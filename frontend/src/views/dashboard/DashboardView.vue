<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import BookCover from '@/components/book/BookCover.vue'
import { getDashboard } from '@/api/stats'
import { renewBook, returnBook } from '@/api/borrow'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { formatCurrency, initials, relativeDue, todayLabel } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const appStore = useAppStore()
const loading = ref(true)
const data = ref(null)
const actionLoading = ref(null)

const isAdmin = computed(() => auth.isAdmin)
const overview = computed(() => data.value?.overview || {})
const activeLoans = computed(() => data.value?.activeLoans || [])

const load = async () => {
  loading.value = true
  try {
    data.value = await getDashboard()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const trendOption = computed(() => {
  const trend = data.value?.trend || { labels: [], borrows: [], returns: [] }
  return {
    color: ['#2e7364', '#bd8a58'],
    grid: { top: 28, right: 12, bottom: 24, left: 34 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fdfbf7',
      borderColor: '#e8dfd2',
      borderWidth: 1,
      padding: [9, 12],
      textStyle: { color: '#40514e', fontSize: 11 },
      axisPointer: { type: 'line', lineStyle: { color: '#d9cdbb', type: 'dashed' } },
    },
    legend: { show: false },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.labels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#94a09d', fontSize: 9, margin: 12 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eee6da', type: 'dashed' } },
      axisLabel: { color: '#94a09d', fontSize: 9 },
    },
    series: [
      {
        name: '借出',
        type: 'line',
        smooth: 0.42,
        showSymbol: false,
        symbolSize: 6,
        lineStyle: { width: 2 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(46,115,100,0.18)' },
              { offset: 1, color: 'rgba(46,115,100,0.01)' },
            ],
          },
        },
        data: trend.borrows,
      },
      {
        name: '归还',
        type: 'line',
        smooth: 0.42,
        showSymbol: false,
        lineStyle: { width: 1.4, type: 'dashed' },
        data: trend.returns,
      },
    ],
  }
})

const categoryOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    backgroundColor: '#fdfbf7',
    borderColor: '#e8dfd2',
    borderWidth: 1,
    textStyle: { color: '#40514e', fontSize: 11 },
    formatter: '{b}<br/>{c} 次借阅 · {d}%',
  },
  series: [
    {
      type: 'pie',
      radius: ['57%', '81%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fdfbf7', borderWidth: 4, borderRadius: 4 },
      label: { show: false },
      emphasis: {
        scale: true,
        scaleSize: 4,
        itemStyle: { shadowBlur: 14, shadowColor: 'rgba(32,45,43,0.12)' },
      },
      data: (data.value?.categoryDistribution || []).map((item) => ({
        name: item.name,
        value: item.value,
        itemStyle: { color: item.color },
      })),
    },
  ],
}))

const categoryTotal = computed(() =>
  (data.value?.categoryDistribution || []).reduce((sum, item) => sum + item.value, 0),
)

const quota = computed(() => {
  const total = overview.value.maxBorrow || 1
  const left = overview.value.availableQuota || 0
  return Math.round((left / total) * 100)
})

const pendingTasks = computed(() => {
  if (!data.value) return []
  const tasks = []
  const overdueCount = activeLoans.value.filter((item) => item.status === 'OVERDUE').length
  if (overdueCount) {
    tasks.push({
      title: `${overdueCount} 笔借阅已逾期`,
      desc: '联系读者尽快归还，避免罚款累积',
      icon: 'WarningFilled',
      tone: 'danger',
      path: '/circulation?status=OVERDUE',
    })
  }
  if (overview.value.pendingFineCount) {
    tasks.push({
      title: `${overview.value.pendingFineCount} 笔罚款待收`,
      desc: `待收金额 ${formatCurrency(overview.value.pendingFineAmount)}`,
      icon: 'Wallet',
      tone: 'amber',
      path: '/penalties-manage',
    })
  }
  if (data.value.dueSoon?.length) {
    tasks.push({
      title: `${data.value.dueSoon.length} 本图书三日内到期`,
      desc: '可提醒读者续借或准备归还上架',
      icon: 'Clock',
      tone: 'blue',
      path: '/circulation',
    })
  }
  if (overview.value.lowStockCount) {
    tasks.push({
      title: `${overview.value.lowStockCount} 种图书可借量不足`,
      desc: '建议检查馆藏并安排补采',
      icon: 'Reading',
      tone: 'copper',
      path: '/books-manage?availability=UNAVAILABLE',
    })
  }
  return tasks
})

const loanProgress = (record) => {
  const start = dayjs(record.borrowDate)
  const end = dayjs(record.dueDate)
  const total = Math.max(end.diff(start, 'day'), 1)
  const elapsed = Math.max(dayjs().diff(start, 'day'), 0)
  return Math.min(Math.round((elapsed / total) * 100), 100)
}

const dueTone = (record) => {
  if (record.status === 'OVERDUE') return 'red'
  const diff = dayjs(record.dueDate).startOf('day').diff(dayjs().startOf('day'), 'day')
  if (diff <= 3) return 'amber'
  return 'green'
}

const go = (path) => router.push(path)
const openBook = (book) =>
  router.push({ name: isAdmin.value ? 'books-manage' : 'books', query: { bookId: book.id } })

const quickReturn = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确认归还《${record.bookTitle}》吗？系统将自动判断是否产生逾期罚款。`,
      '归还图书',
      { confirmButtonText: '确认归还', cancelButtonText: '取消', type: 'warning' },
    )
    actionLoading.value = `return-${record.id}`
    const result = await returnBook(record.id)
    ElMessage.success(result?.record ? '归还成功' : '归还成功')
    await load()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作失败')
  } finally {
    actionLoading.value = null
  }
}

const quickRenew = async (record) => {
  try {
    actionLoading.value = `renew-${record.id}`
    await renewBook(record.id)
    ElMessage.success('续借成功，应还日期已延长')
    await load()
    appStore.fetchNotifications()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    actionLoading.value = null
  }
}
</script>

<template>
  <div v-loading="loading" class="app-page dashboard-page">
    <PageHeader
      eyebrow="Overview"
      :title="isAdmin ? '馆藏运营总览' : '我的阅读空间'"
      :description="
        isAdmin
          ? '掌握图书馆今日流通、馆藏结构与待处理事项。'
          : '查看在借图书、归还期限与为你准备的阅读推荐。'
      "
    >
      <template #actions>
        <el-button v-if="isAdmin" plain @click="go('/stats')">
          <el-icon><TrendCharts /></el-icon>
          查看报表
        </el-button>
        <el-button type="primary" @click="go(isAdmin ? '/circulation' : '/books')">
          <el-icon><Switch v-if="isAdmin" /><Reading v-else /></el-icon>
          {{ isAdmin ? '办理流通' : '去借一本书' }}
        </el-button>
      </template>
    </PageHeader>

    <section class="welcome-panel">
      <div class="welcome-panel__grain" />
      <div class="welcome-panel__content">
        <div class="welcome-date">{{ todayLabel() }}</div>
        <h2>
          {{ isAdmin ? '今日馆务，井然有序。' : `${auth.displayName}，今天想读点什么？` }}
        </h2>
        <p v-if="isAdmin">
          今日已借出 <strong>{{ overview.todayBorrows || 0 }}</strong> 本，
          <template v-if="activeLoans.filter((item) => item.status === 'OVERDUE').length">
            有 <strong class="is-warn">{{ activeLoans.filter((item) => item.status === 'OVERDUE').length }}</strong>
            笔借阅需要跟进。
          </template>
          <template v-else>流通状态良好，继续保持。</template>
        </p>
        <p v-else>
          已借阅 <strong>{{ overview.activeLoans || 0 }}</strong> 本，还可借
          <strong>{{ overview.availableQuota || 0 }}</strong> 本。
          <template v-if="overview.dueSoon">有 {{ overview.dueSoon }} 本即将到期，记得续借。</template>
          <template v-else>愿你在书页之间，找到今日的答案。</template>
        </p>

        <div class="welcome-actions">
          <button class="welcome-link" @click="go(isAdmin ? '/books-manage' : '/my-borrows')">
            {{ isAdmin ? '管理馆藏' : '查看我的借阅' }}
            <el-icon><ArrowRight /></el-icon>
          </button>
          <span class="welcome-sep" />
          <button class="welcome-link" @click="go(isAdmin ? '/penalties-manage' : '/penalties')">
            {{ isAdmin ? '处理罚款' : '我的罚款' }}
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>

      <div v-if="isAdmin" class="welcome-metrics">
        <div class="welcome-metric">
          <span>馆藏可借</span>
          <strong>{{ overview.availableCopies || 0 }}<small>册</small></strong>
        </div>
        <div class="welcome-metric">
          <span>本月活跃读者</span>
          <strong>{{ overview.activeMonthReaders || 0 }}<small>人</small></strong>
        </div>
        <div class="welcome-metric">
          <span>本月借出</span>
          <strong>{{ overview.monthBorrows || 0 }}<small>次</small></strong>
        </div>
      </div>

      <div v-else class="quota-ring">
        <div class="quota-ring__outer" :style="{ '--quota': `${quota * 3.6}deg` }">
          <div class="quota-ring__inner">
            <strong>{{ overview.availableQuota || 0 }}</strong>
            <span>/ {{ overview.maxBorrow || 0 }} 可借</span>
          </div>
        </div>
        <div class="quota-ring__caption">
          <span>借阅额度</span>
          <small>本科 5 · 研究生 10 · 教师 15</small>
        </div>
      </div>

      <div class="welcome-panel__ornament">
        <span />
        <span />
        <span />
      </div>
    </section>

    <div class="stats-grid">
      <template v-if="isAdmin">
        <StatCard
          label="馆藏品种"
          :value="overview.totalTitles || 0"
          suffix="种"
          icon="Reading"
          tone="jade"
          :note="`共 ${overview.totalCopies || 0} 册馆藏`"
          :loading="loading"
        />
        <StatCard
          label="当前在借"
          :value="overview.activeLoans || 0"
          suffix="本"
          icon="Collection"
          tone="copper"
          :note="`今日借出 ${overview.todayBorrows || 0} 本`"
          :loading="loading"
        />
        <StatCard
          label="注册读者"
          :value="overview.readerCount || 0"
          suffix="人"
          icon="User"
          tone="blue"
          :note="`本月活跃 ${overview.activeMonthReaders || 0} 人`"
          :loading="loading"
        />
        <StatCard
          label="待缴罚款"
          :value="Number(overview.pendingFineAmount || 0).toFixed(2)"
          suffix="元"
          icon="Wallet"
          tone="amber"
          :note="`${overview.pendingFineCount || 0} 笔待处理`"
          :loading="loading"
        />
      </template>
      <template v-else>
        <StatCard
          label="可借额度"
          :value="overview.availableQuota || 0"
          :suffix="' / ' + (overview.maxBorrow || 0) + ' 本'"
          icon="Reading"
          tone="jade"
          :note="`借阅证状态正常`"
          :loading="loading"
        />
        <StatCard
          label="当前在借"
          :value="overview.activeLoans || 0"
          suffix="本"
          icon="Collection"
          tone="copper"
          :note="`其中 ${overview.dueSoon || 0} 本即将到期`"
          :loading="loading"
        />
        <StatCard
          label="累计阅读"
          :value="overview.totalRead || 0"
          suffix="本"
          icon="Notebook"
          tone="blue"
          :note="overview.favoriteCategory !== '暂无' ? `偏爱 ${overview.favoriteCategory}` : '开始你的第一本书'"
          :loading="loading"
        />
        <StatCard
          label="待缴罚款"
          :value="Number(overview.unpaidAmount || 0).toFixed(2)"
          suffix="元"
          icon="Wallet"
          tone="amber"
          :note="`${overview.unpaidCount || 0} 笔待缴纳`"
          :loading="loading"
        />
      </template>
    </div>

    <template v-if="isAdmin">
      <div class="dashboard-grid dashboard-grid--charts">
        <section class="surface-card section-card trend-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">Circulation</div>
              <h3>借阅与归还趋势</h3>
              <p>近 8 个月馆藏流通变化</p>
            </div>
            <div class="chart-legend">
              <span><i class="is-jade" />借出</span>
              <span><i class="is-copper" />归还</span>
            </div>
          </div>
          <BaseChart :option="trendOption" height="255px" />
        </section>

        <section class="surface-card section-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">Categories</div>
              <h3>馆藏借阅构成</h3>
              <p>按图书分类统计的累计借阅量</p>
            </div>
            <span class="mini-total">{{ categoryTotal }} 次</span>
          </div>
          <div class="category-layout">
            <BaseChart :option="categoryOption" height="188px" class="category-chart" />
            <div class="category-legend">
              <div
                v-for="category in (data?.categoryDistribution || []).slice(0, 5)"
                :key="category.id"
                class="category-legend__row"
              >
                <span class="category-legend__dot" :style="{ background: category.color }" />
                <span class="category-legend__name">{{ category.name }}</span>
                <strong>{{ category.value }}</strong>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="dashboard-grid dashboard-grid--split">
        <section class="surface-card section-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">On Loan</div>
              <h3>在借与临期提醒</h3>
              <p>按应还日期排序的当前借阅</p>
            </div>
            <el-button text class="section-link" @click="go('/circulation')">
              全部记录 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div v-if="activeLoans.length" class="loan-table">
            <button
              v-for="record in activeLoans.slice(0, 5)"
              :key="record.id"
              class="loan-row"
              @click="go('/circulation')"
            >
              <div class="loan-row__cover">
                <BookCover :book="record" size="xs" flat />
              </div>
              <div class="loan-row__main">
                <strong>{{ record.bookTitle }}</strong>
                <span>{{ record.readerName }} · {{ record.readerNo }}</span>
              </div>
              <span class="loan-row__borrow">借于 {{ record.borrowDate.slice(5) }}</span>
              <span class="pill" :class="`pill--${dueTone(record)}`">
                <span class="pill__dot" />
                {{ relativeDue(record.dueDate).label }}
              </span>
            </button>
          </div>
          <el-empty v-else description="暂无在借记录" :image-size="70" />

          <div class="loan-foot">
            <span>逾期记录 {{ overview.overdueLoans || 0 }} 笔</span>
            <span>临期 {{ data?.dueSoon?.length || 0 }} 本</span>
            <span>在借总量 {{ overview.activeLoans || 0 }} 本</span>
          </div>
        </section>

        <section class="surface-card section-card tasks-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">To Do</div>
              <h3>今日待办</h3>
              <p>需要馆员关注的事项</p>
            </div>
            <span class="task-count">{{ pendingTasks.length }}</span>
          </div>

          <div v-if="pendingTasks.length" class="task-list">
            <button
              v-for="task in pendingTasks"
              :key="task.title"
              class="task-item"
              @click="go(task.path)"
            >
              <span class="task-item__icon" :class="`is-${task.tone}`">
                <el-icon><component :is="task.icon" /></el-icon>
              </span>
              <span class="task-item__copy">
                <strong>{{ task.title }}</strong>
                <span>{{ task.desc }}</span>
              </span>
              <el-icon class="task-item__arrow"><ArrowRight /></el-icon>
            </button>
          </div>
          <div v-else class="all-clear">
            <el-icon><CircleCheckFilled /></el-icon>
            <strong>今日事项已清空</strong>
            <span>流通、罚款与馆藏状态良好</span>
          </div>

          <div class="announcement-block">
            <div class="announcement-block__head">馆内公告</div>
            <div
              v-for="notice in (data?.announcements || []).slice(0, 2)"
              :key="notice.id"
              class="announcement"
            >
              <span>{{ notice.tag }}</span>
              <p>{{ notice.title }}</p>
            </div>
          </div>
        </section>
      </div>

      <div class="dashboard-grid dashboard-grid--split">
        <section class="surface-card section-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">Popular</div>
              <h3>热门借阅</h3>
              <p>最受读者欢迎的馆藏</p>
            </div>
            <el-button text class="section-link" @click="go('/stats')">
              借阅排行 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div class="popular-list">
            <button
              v-for="(book, index) in data?.popularBooks || []"
              :key="book.id"
              class="popular-row"
              @click="openBook(book)"
            >
              <span class="popular-row__rank" :class="{ 'is-top': index < 3 }">
                {{ String(index + 1).padStart(2, '0') }}
              </span>
              <div class="popular-row__cover">
                <BookCover :book="book" size="xs" flat />
              </div>
              <div class="popular-row__copy">
                <strong>{{ book.title }}</strong>
                <span>{{ book.author }} · {{ book.categoryName }}</span>
              </div>
              <div class="popular-row__bar">
                <span
                  :style="{
                    width: `${(book.borrowCount / Math.max(data.popularBooks[0]?.borrowCount || 1, 1)) * 100}%`,
                  }"
                />
              </div>
              <span class="popular-row__count">{{ book.borrowCount }} 次</span>
            </button>
          </div>
        </section>

        <section class="surface-card section-card activity-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">Recent</div>
              <h3>最近动态</h3>
              <p>来自流通服务台的实时记录</p>
            </div>
          </div>

          <div class="activity-list">
            <div
              v-for="activity in data?.activities || []"
              :key="activity.id"
              class="activity-item"
            >
              <span class="activity-item__dot" :class="`is-${activity.tone}`" />
              <div class="activity-item__copy">
                <p>{{ activity.title }}</p>
                <span>{{ activity.description }}</span>
              </div>
              <time>{{ dayjs(activity.time).format('MM-DD HH:mm') }}</time>
            </div>
          </div>
        </section>
      </div>
    </template>

    <template v-else>
      <div class="dashboard-grid dashboard-grid--reader">
        <section class="surface-card section-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">Currently Reading</div>
              <h3>正在阅读</h3>
              <p>按期归还，让好书继续流转</p>
            </div>
            <el-button text class="section-link" @click="go('/my-borrows')">
              全部借阅 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div v-if="activeLoans.length" class="reader-loans">
            <article v-for="record in activeLoans" :key="record.id" class="reader-loan">
              <div class="reader-loan__cover">
                <BookCover :book="record" size="sm" flat />
              </div>
              <div class="reader-loan__body">
                <div class="reader-loan__top">
                  <span class="pill" :class="`pill--${dueTone(record)}`">
                    <span class="pill__dot" />
                    {{ relativeDue(record.dueDate).label }}
                  </span>
                  <span class="reader-loan__renew">续借 {{ record.renewCount }}/1</span>
                </div>
                <h4>{{ record.bookTitle }}</h4>
                <p>{{ record.bookAuthor }}</p>
                <div class="loan-progress">
                  <span :style="{ width: `${loanProgress(record)}%` }" :class="{ 'is-late': record.status === 'OVERDUE' }" />
                </div>
                <div class="reader-loan__meta">
                  <span>借阅 {{ record.borrowDate }}</span>
                  <span>应还 {{ record.dueDate }}</span>
                </div>
                <div class="reader-loan__actions">
                  <el-button
                    size="small"
                    plain
                    :disabled="record.status === 'OVERDUE' || record.renewCount >= 1"
                    :loading="actionLoading === `renew-${record.id}`"
                    @click="quickRenew(record)"
                  >
                    续借
                  </el-button>
                  <el-button
                    size="small"
                    type="primary"
                    :loading="actionLoading === `return-${record.id}`"
                    @click="quickReturn(record)"
                  >
                    归还
                  </el-button>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="reader-empty">
            <div class="reader-empty__icon"><el-icon><Reading /></el-icon></div>
            <strong>书架暂时空着</strong>
            <p>馆藏里有 12.8 万册好书，去挑一本带回今天吧。</p>
            <el-button type="primary" @click="go('/books')">浏览馆藏</el-button>
          </div>
        </section>

        <div class="reader-side">
          <section class="reader-card">
            <div class="reader-card__grain" />
            <div class="reader-card__head">
              <span>READER CARD</span>
              <span class="reader-card__seal">知墨</span>
            </div>
            <div class="reader-card__name">
              <strong>{{ auth.displayName }}</strong>
              <span>{{ auth.reader?.typeLabel }} · {{ auth.user?.username }}</span>
            </div>
            <div class="reader-card__bottom">
              <span>{{ auth.reader?.cardNo }}</span>
              <span class="reader-card__status">VALID</span>
            </div>
            <span class="reader-card__arc" />
          </section>

          <section class="surface-card section-card fine-panel">
            <div class="fine-panel__head">
              <div>
                <div class="eyebrow">Fines</div>
                <h3>罚款与提醒</h3>
              </div>
              <span class="fine-panel__amount" :class="{ 'is-clear': !overview.unpaidAmount }">
                {{ formatCurrency(overview.unpaidAmount) }}
              </span>
            </div>
            <p v-if="overview.unpaidCount" class="fine-panel__desc">
              有 {{ overview.unpaidCount }} 笔逾期罚款待缴纳，可在罚款管理中查看明细。
            </p>
            <p v-else class="fine-panel__desc">当前没有待缴罚款，请继续保持按时归还。</p>
            <el-button
              size="small"
              :type="overview.unpaidCount ? 'primary' : 'default'"
              plain
              @click="go('/penalties')"
            >
              {{ overview.unpaidCount ? '去处理罚款' : '查看罚款记录' }}
            </el-button>
          </section>
        </div>
      </div>

      <div class="dashboard-grid dashboard-grid--split reader-bottom">
        <section class="surface-card section-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">For You</div>
              <h3>为你推荐</h3>
              <p>根据馆藏热度与你的阅读偏好挑选</p>
            </div>
            <el-button text class="section-link" @click="go('/books')">
              更多馆藏 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div class="recommend-grid">
            <button
              v-for="book in data?.recommendations || []"
              :key="book.id"
              class="recommend-card"
              @click="openBook(book)"
            >
              <div class="recommend-card__cover">
                <BookCover :book="book" size="sm" flat />
              </div>
              <div class="recommend-card__copy">
                <span>{{ book.categoryName }}</span>
                <strong>{{ book.title }}</strong>
                <p>{{ book.author }}</p>
                <small>可借 {{ book.availableCopies }} 本</small>
              </div>
            </button>
          </div>
        </section>

        <section class="surface-card section-card activity-card">
          <div class="section-heading">
            <div>
              <div class="eyebrow">History</div>
              <h3>阅读足迹</h3>
              <p>你与这些书相遇过</p>
            </div>
            <el-button text class="section-link" @click="go('/my-borrows')">
              全部记录 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div class="activity-list reader-history">
            <div
              v-for="record in data?.recentHistory || []"
              :key="record.id"
              class="activity-item"
            >
              <span
                class="activity-item__dot"
                :class="record.status === 'RETURNED' ? 'is-green' : 'is-copper'"
              />
              <div class="activity-item__copy">
                <p>《{{ record.bookTitle }}》</p>
                <span>
                  {{ record.status === 'RETURNED' ? '已归还' : '阅读中' }} ·
                  {{ record.borrowDate }} 借出
                </span>
              </div>
              <time>{{ record.categoryName }}</time>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  gap: 20px;
}

.welcome-panel {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 178px;
  padding: 28px 34px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f5ebde;
  background:
    radial-gradient(circle at 80% 10%, rgba(201, 146, 100, 0.18), transparent 32%),
    radial-gradient(circle at 15% 120%, rgba(87, 148, 130, 0.24), transparent 42%),
    linear-gradient(120deg, #153e39 0%, #0f3030 56%, #0b292b 100%);
  box-shadow: 0 20px 44px rgba(18, 48, 43, 0.15);
}

.welcome-panel__grain {
  position: absolute;
  inset: 0;
  opacity: 0.11;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  pointer-events: none;
}

.welcome-panel__content {
  position: relative;
  z-index: 2;
  max-width: 58%;
}

.welcome-date {
  color: rgba(205, 219, 210, 0.44);
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.12em;
}

.welcome-panel h2 {
  margin-top: 9px;
  color: #f8efe2;
  font-family: var(--font-display);
  font-size: 27px;
  font-weight: 700;
  letter-spacing: 0.025em;
}

.welcome-panel p {
  margin-top: 8px;
  color: rgba(218, 231, 223, 0.56);
  font-size: 11.5px;
  line-height: 1.7;
}

.welcome-panel p strong {
  color: #e4b583;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
}

.welcome-panel p strong.is-warn {
  color: #e8a294;
}

.welcome-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
}

.welcome-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0;
  border: 0;
  color: #d9b085;
  background: transparent;
  font-size: 10.5px;
  cursor: pointer;
  transition: color 0.18s ease;
}

.welcome-link:hover {
  color: #f1d2b0;
}

.welcome-link .el-icon {
  font-size: 9px;
}

.welcome-sep {
  width: 1px;
  height: 11px;
  background: rgba(226, 203, 177, 0.18);
}

.welcome-metrics {
  position: relative;
  z-index: 2;
  display: flex;
  gap: clamp(20px, 3vw, 46px);
  margin-left: auto;
  padding-right: 82px;
}

.welcome-metric {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.welcome-metric span {
  color: rgba(198, 214, 205, 0.42);
  font-size: 8.5px;
  letter-spacing: 0.04em;
}

.welcome-metric strong {
  color: #f3e6d4;
  font-family: var(--font-display);
  font-size: 25px;
  font-weight: 600;
  line-height: 1.2;
}

.welcome-metric small {
  margin-left: 2px;
  color: rgba(205, 219, 210, 0.38);
  font-family: var(--font-sans);
  font-size: 8px;
  font-weight: 400;
}

.welcome-panel__ornament {
  position: absolute;
  top: 50%;
  right: -65px;
  width: 320px;
  height: 320px;
  border: 1px solid rgba(228, 201, 170, 0.08);
  border-radius: 50%;
  transform: translateY(-50%);
}

.welcome-panel__ornament span {
  position: absolute;
  border: 1px solid rgba(228, 201, 170, 0.065);
  border-radius: 50%;
}

.welcome-panel__ornament span:nth-child(1) {
  inset: 38px;
}

.welcome-panel__ornament span:nth-child(2) {
  inset: 82px;
}

.welcome-panel__ornament span:nth-child(3) {
  inset: 128px;
  border-color: rgba(201, 146, 100, 0.14);
}

.quota-ring {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: auto;
  margin-right: 40px;
}

.quota-ring__outer {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 106px;
  height: 106px;
  border-radius: 50%;
  background: conic-gradient(
    #d5a372 0 var(--quota),
    rgba(255, 255, 255, 0.08) var(--quota) 360deg
  );
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.quota-ring__inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 87px;
  height: 87px;
  border-radius: 50%;
  background: #133a36;
}

.quota-ring__inner strong {
  color: #f8ead7;
  font-family: var(--font-display);
  font-size: 33px;
  line-height: 1.1;
}

.quota-ring__inner span {
  color: rgba(200, 216, 207, 0.4);
  font-size: 8px;
}

.quota-ring__caption {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quota-ring__caption span {
  color: rgba(239, 228, 213, 0.82);
  font-family: var(--font-display);
  font-size: 14px;
}

.quota-ring__caption small {
  max-width: 120px;
  color: rgba(195, 212, 202, 0.38);
  font-size: 8px;
  line-height: 1.6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-grid {
  display: grid;
  gap: 16px;
}

.dashboard-grid--charts {
  grid-template-columns: minmax(0, 1.45fr) minmax(340px, 0.85fr);
}

.dashboard-grid--split {
  grid-template-columns: minmax(0, 1.38fr) minmax(320px, 0.82fr);
}

.dashboard-grid--reader {
  grid-template-columns: minmax(0, 1.42fr) minmax(310px, 0.78fr);
}

.section-card {
  padding: 21px 22px;
}

.section-heading {
  margin-bottom: 14px;
}

.section-heading .eyebrow {
  margin-bottom: 5px;
}

.section-heading h3 {
  font-size: 18px;
}

.section-link {
  padding: 4px 0;
  color: var(--copper-600);
  font-size: 10px;
}

.section-link:hover {
  color: var(--copper-500);
}

.section-link .el-icon {
  margin-left: 2px;
  font-size: 9px;
}

.chart-legend {
  display: flex;
  gap: 14px;
  color: var(--text-400);
  font-size: 9px;
}

.chart-legend span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.chart-legend i {
  width: 14px;
  height: 2px;
  border-radius: 2px;
}

.chart-legend i.is-jade {
  background: #2e7364;
}

.chart-legend i.is-copper {
  background: #bd8a58;
  opacity: 0.8;
}

.mini-total {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 9px;
}

.category-layout {
  display: flex;
  flex-direction: column;
}

.category-chart {
  margin-top: -8px;
}

.category-legend {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 2px;
}

.category-legend__row {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--text-500);
  font-size: 9px;
}

.category-legend__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.category-legend__name {
  flex: 1;
}

.category-legend__row strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
}

.loan-table {
  display: flex;
  flex-direction: column;
}

.loan-row {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 8px 5px;
  border: 0;
  border-bottom: 1px solid rgba(232, 223, 210, 0.62);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.loan-row:hover {
  background: var(--paper-100);
}

.loan-row__cover {
  width: 29px;
  flex-shrink: 0;
}

.loan-row__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.loan-row__main strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loan-row__main span {
  margin-top: 1px;
  color: var(--text-400);
  font-size: 8.5px;
}

.loan-row__borrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.loan-row .pill {
  min-width: 82px;
  justify-content: center;
  font-size: 8px;
}

.loan-foot {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  color: var(--text-400);
  font-size: 8px;
}

.loan-foot span + span::before {
  margin-right: 16px;
  color: var(--line-strong);
  content: '·';
}

.task-count {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: var(--copper-600);
  background: var(--copper-100);
  font-family: var(--font-mono);
  font-size: 10px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 7px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.18s ease;
}

.task-item:hover {
  background: var(--paper-100);
}

.task-item__icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  font-size: 13px;
}

.task-item__icon.is-danger {
  color: var(--danger);
  background: var(--danger-bg);
}

.task-item__icon.is-amber {
  color: var(--warning);
  background: var(--warning-bg);
}

.task-item__icon.is-blue {
  color: var(--info);
  background: var(--info-bg);
}

.task-item__icon.is-copper {
  color: var(--copper-600);
  background: var(--copper-100);
}

.task-item__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.task-item__copy strong {
  color: var(--text-700);
  font-size: 10.5px;
  font-weight: 650;
}

.task-item__copy span {
  overflow: hidden;
  margin-top: 1px;
  color: var(--text-400);
  font-size: 8.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-item__arrow {
  color: var(--line-strong);
  font-size: 9px;
}

.all-clear {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 20px 0 18px;
  color: var(--success);
}

.all-clear .el-icon {
  margin-bottom: 2px;
  font-size: 26px;
}

.all-clear strong {
  color: var(--text-700);
  font-size: 11px;
}

.all-clear span {
  color: var(--text-400);
  font-size: 8.5px;
}

.announcement-block {
  margin-top: 12px;
  padding-top: 11px;
  border-top: 1px dashed var(--line);
}

.announcement-block__head {
  margin-bottom: 5px;
  color: var(--text-400);
  font-size: 8px;
  font-weight: 650;
  letter-spacing: 0.1em;
}

.announcement {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 4px 0;
}

.announcement span {
  padding: 1px 5px;
  border: 1px solid var(--line);
  border-radius: 3px;
  color: var(--text-400);
  font-size: 7px;
  white-space: nowrap;
}

.announcement p {
  overflow: hidden;
  color: var(--text-500);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popular-list {
  display: flex;
  flex-direction: column;
}

.popular-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 4px;
  border: 0;
  border-bottom: 1px solid rgba(232, 223, 210, 0.55);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.popular-row:hover .popular-row__copy strong {
  color: var(--copper-600);
}

.popular-row__rank {
  width: 18px;
  flex-shrink: 0;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.popular-row__rank.is-top {
  color: var(--copper-600);
  font-weight: 600;
}

.popular-row__cover {
  width: 25px;
  flex-shrink: 0;
}

.popular-row__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.popular-row__copy strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}

.popular-row__copy span {
  overflow: hidden;
  color: var(--text-400);
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popular-row__bar {
  width: 64px;
  height: 3px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.popular-row__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--jade-400), var(--ink-500));
}

.popular-row__count {
  width: 35px;
  color: var(--text-500);
  font-family: var(--font-mono);
  font-size: 8px;
  text-align: right;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.activity-item {
  position: relative;
  display: flex;
  gap: 10px;
  padding: 8px 2px 8px 5px;
}

.activity-item__dot {
  width: 5px;
  height: 5px;
  flex-shrink: 0;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--copper-400);
  box-shadow: 0 0 0 3px rgba(181, 121, 75, 0.09);
}

.activity-item__dot.is-green {
  background: var(--success);
  box-shadow: 0 0 0 3px rgba(62, 130, 101, 0.09);
}

.activity-item__dot.is-copper {
  background: var(--copper-400);
}

.activity-item__copy {
  flex: 1;
  min-width: 0;
}

.activity-item__copy p {
  overflow: hidden;
  color: var(--text-700);
  font-size: 10px;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-item__copy span {
  color: var(--text-400);
  font-size: 8.5px;
}

.activity-item time {
  flex-shrink: 0;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7.5px;
}

.reader-loans {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 11px;
}

.reader-loan {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: rgba(255, 253, 248, 0.7);
}

.reader-loan__cover {
  width: 66px;
  flex-shrink: 0;
}

.reader-loan__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.reader-loan__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 5px;
}

.reader-loan__top .pill {
  font-size: 7.5px;
}

.reader-loan__renew {
  color: var(--text-400);
  font-size: 7.5px;
}

.reader-loan__body h4 {
  overflow: hidden;
  margin-top: 6px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reader-loan__body > p {
  color: var(--text-400);
  font-size: 8.5px;
}

.loan-progress {
  height: 3px;
  margin-top: 8px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.loan-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--jade-500);
}

.loan-progress span.is-late {
  background: var(--danger);
}

.reader-loan__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 3px 8px;
  margin-top: 5px;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
}

.reader-loan__actions {
  display: flex;
  gap: 6px;
  margin-top: auto;
  padding-top: 9px;
}

.reader-loan__actions .el-button {
  height: 23px;
  padding: 0 9px;
  font-size: 8.5px;
}

.reader-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36px 12px;
  text-align: center;
}

.reader-empty__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  margin-bottom: 10px;
  border-radius: 50%;
  color: var(--jade-500);
  background: var(--jade-100);
  font-size: 22px;
}

.reader-empty strong {
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 15px;
}

.reader-empty p {
  margin: 4px 0 13px;
  color: var(--text-400);
  font-size: 10px;
}

.reader-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reader-card {
  position: relative;
  min-height: 168px;
  padding: 21px 22px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f6eadb;
  background:
    radial-gradient(circle at 90% 0%, rgba(201, 146, 100, 0.25), transparent 38%),
    linear-gradient(140deg, #194740, #0d2d2f);
  box-shadow: 0 16px 34px rgba(18, 48, 43, 0.16);
}

.reader-card__grain {
  position: absolute;
  inset: 0;
  opacity: 0.12;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.75' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.reader-card__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  color: rgba(204, 220, 210, 0.42);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.reader-card__seal {
  width: 25px;
  height: 25px;
  padding: 4px;
  border: 1px solid rgba(217, 169, 121, 0.5);
  color: #d9a979;
  font-family: var(--font-display);
  font-size: 8px;
  line-height: 1.25;
  text-align: center;
  writing-mode: vertical-rl;
}

.reader-card__name {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  margin-top: 21px;
}

.reader-card__name strong {
  font-family: var(--font-display);
  font-size: 24px;
  letter-spacing: 0.08em;
}

.reader-card__name span {
  margin-top: 4px;
  color: rgba(201, 218, 208, 0.5);
  font-size: 9px;
}

.reader-card__bottom {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 15px;
  color: rgba(211, 225, 216, 0.46);
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.08em;
}

.reader-card__status {
  color: #d9b085;
  font-size: 7px;
}

.reader-card__arc {
  position: absolute;
  right: -42px;
  bottom: -66px;
  width: 190px;
  height: 190px;
  border: 1px solid rgba(222, 188, 151, 0.1);
  border-radius: 50%;
}

.reader-card__arc::after {
  position: absolute;
  inset: 24px;
  border: 1px solid rgba(222, 188, 151, 0.08);
  border-radius: 50%;
  content: '';
}

.fine-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.fine-panel__head h3 {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--ink-800);
}

.fine-panel__amount {
  color: var(--warning);
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 700;
}

.fine-panel__amount.is-clear {
  color: var(--success);
}

.fine-panel__desc {
  margin: 7px 0 12px;
  color: var(--text-400);
  font-size: 9.5px;
  line-height: 1.65;
}

.fine-panel {
  flex: 1;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.recommend-card {
  display: flex;
  gap: 9px;
  padding: 9px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease;
}

.recommend-card:hover {
  border-color: var(--copper-200);
  background: var(--paper-100);
}

.recommend-card__cover {
  width: 48px;
  flex-shrink: 0;
}

.recommend-card__copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.recommend-card__copy > span {
  color: var(--text-400);
  font-size: 7px;
}

.recommend-card__copy strong {
  overflow: hidden;
  margin-top: 3px;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-card__copy p {
  overflow: hidden;
  margin-top: 1px;
  color: var(--text-400);
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-card__copy small {
  margin-top: 7px;
  color: var(--success);
  font-size: 7.5px;
}

.reader-history .activity-item time {
  color: var(--text-400);
}

@media (max-width: 1300px) {
  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .welcome-metrics {
    padding-right: 40px;
  }
}

@media (max-width: 1180px) {
  .dashboard-grid--charts,
  .dashboard-grid--split,
  .dashboard-grid--reader {
    grid-template-columns: 1fr;
  }

  .reader-side {
    display: grid;
    grid-template-columns: minmax(0, 1.2fr) minmax(240px, 0.8fr);
  }
}

@media (max-width: 860px) {
  .welcome-panel {
    align-items: flex-start;
    flex-direction: column;
    padding: 24px;
  }

  .welcome-panel__content {
    max-width: 100%;
  }

  .welcome-metrics,
  .quota-ring {
    margin: 20px 0 0;
    padding: 0;
  }

  .welcome-metrics {
    gap: 28px;
  }

  .welcome-panel__ornament {
    right: -150px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .reader-loans {
    grid-template-columns: 1fr;
  }

  .reader-side {
    display: flex;
  }
}

@media (max-width: 560px) {
  .welcome-panel h2 {
    font-size: 22px;
  }

  .welcome-metrics {
    flex-wrap: wrap;
  }

  .welcome-metric strong {
    font-size: 21px;
  }

  .stats-grid {
    gap: 9px;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .section-card {
    padding: 17px;
  }

  .loan-row__borrow,
  .popular-row__bar {
    display: none;
  }
}
</style>
