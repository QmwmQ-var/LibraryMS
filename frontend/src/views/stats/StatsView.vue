<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import BookCover from '@/components/book/BookCover.vue'
import { getOverview, getRanking, getOverdueStats, getTrends } from '@/api/stats'
import { READER_TYPES } from '@/utils/constants'
import { formatCurrency, initials } from '@/utils/format'

const loading = ref(true)
const overview = ref({})
const trends = ref({ monthly: { labels: [], borrows: [], returns: [] }, categories: [] })
const ranking = ref({ books: [], readers: [] })
const overdue = ref({
  summary: {},
  trend: { labels: [], counts: [], amounts: [] },
  buckets: [],
  records: [],
})
const range = ref('180')

const ranges = [
  { value: '30', label: '近 30 天' },
  { value: '90', label: '近 90 天' },
  { value: '180', label: '近半年' },
  { value: '365', label: '近一年' },
  { value: 'all', label: '全部' },
]

const dateParams = computed(() => {
  if (range.value === 'all') return {}
  return {
    startDate: dayjs().subtract(Number(range.value), 'day').format('YYYY-MM-DD'),
    endDate: dayjs().format('YYYY-MM-DD'),
  }
})

const rangeLabel = computed(
  () => ranges.find((item) => item.value === range.value)?.label || '全部',
)

const load = async () => {
  loading.value = true
  try {
    const [overviewData, trendData, rankingData, overdueData] = await Promise.all([
      getOverview(dateParams.value),
      getTrends(),
      getRanking({ limit: 8, ...dateParams.value }),
      getOverdueStats(),
    ])
    overview.value = overviewData
    trends.value = trendData
    ranking.value = rankingData
    overdue.value = overdueData
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const changeRange = () => load()

const monthlyOption = computed(() => ({
  color: ['#2e7364', '#bd8a58'],
  grid: { top: 30, right: 14, bottom: 25, left: 36 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fdfbf7',
    borderColor: '#e8dfd2',
    borderWidth: 1,
    textStyle: { color: '#40514e', fontSize: 11 },
    axisPointer: { lineStyle: { color: '#d9cdbb', type: 'dashed' } },
  },
  legend: {
    top: 0,
    right: 0,
    itemWidth: 10,
    itemHeight: 2,
    textStyle: { color: '#72827e', fontSize: 9 },
    data: ['借出', '归还'],
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trends.value.monthly.labels,
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
      smooth: 0.4,
      showSymbol: false,
      lineStyle: { width: 2 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(46,115,100,0.2)' },
            { offset: 1, color: 'rgba(46,115,100,0.01)' },
          ],
        },
      },
      data: trends.value.monthly.borrows,
    },
    {
      name: '归还',
      type: 'line',
      smooth: 0.4,
      showSymbol: false,
      lineStyle: { width: 1.4, type: 'dashed' },
      data: trends.value.monthly.returns,
    },
  ],
}))

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
      radius: ['58%', '82%'],
      center: ['50%', '50%'],
      itemStyle: { borderColor: '#fdfbf7', borderWidth: 4, borderRadius: 4 },
      label: { show: false },
      emphasis: { scale: true, scaleSize: 4 },
      data: trends.value.categories
        .filter((item) => item.value > 0)
        .map((item) => ({
          name: item.name,
          value: item.value,
          itemStyle: { color: item.color },
        })),
    },
  ],
}))

const categoryTotal = computed(() =>
  trends.value.categories.reduce((sum, item) => sum + item.value, 0),
)

const overdueOption = computed(() => ({
  color: ['#c98b5b', '#b55b52'],
  grid: { top: 30, right: 38, bottom: 24, left: 34 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fdfbf7',
    borderColor: '#e8dfd2',
    borderWidth: 1,
    textStyle: { color: '#40514e', fontSize: 11 },
  },
  legend: {
    top: 0,
    right: 0,
    itemWidth: 9,
    itemHeight: 7,
    textStyle: { color: '#72827e', fontSize: 8.5 },
    data: ['罚款笔数', '罚款金额'],
  },
  xAxis: {
    type: 'category',
    data: overdue.value.trend.labels,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#94a09d', fontSize: 9, margin: 10 },
  },
  yAxis: [
    {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eee6da', type: 'dashed' } },
      axisLabel: { color: '#94a09d', fontSize: 8 },
    },
    {
      type: 'value',
      splitLine: { show: false },
      axisLabel: { color: '#94a09d', fontSize: 8, formatter: '¥{value}' },
    },
  ],
  series: [
    {
      name: '罚款笔数',
      type: 'bar',
      barWidth: 13,
      itemStyle: { borderRadius: [3, 3, 0, 0] },
      data: overdue.value.trend.counts,
    },
    {
      name: '罚款金额',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbolSize: 5,
      lineStyle: { width: 1.6 },
      data: overdue.value.trend.amounts,
    },
  ],
}))

const overdueBucketsMax = computed(() =>
  Math.max(...overdue.value.buckets.map((item) => item.value), 1),
)

const exportReport = () => {
  const rows = [
    ['知墨图书馆 · 统计报表'],
    ['统计周期', rangeLabel.value],
    ['导出时间', dayjs().format('YYYY-MM-DD HH:mm')],
    [],
    ['核心指标'],
    ['馆藏品种', overview.value.totalTitles],
    ['注册读者', overview.value.readerCount],
    ['当前在借', overview.value.activeLoans],
    ['逾期未还', overview.value.overdueLoans],
    ['统计期借阅量', overview.value.rangeBorrowCount],
    ['待缴罚款（元）', overview.value.pendingFineAmount],
    [],
    ['借阅排行榜'],
    ['排名', '书名', '作者', '分类', '借阅次数'],
    ...ranking.value.books.map((book, index) => [
      index + 1,
      book.title,
      book.author,
      book.categoryName,
      book.borrowCount,
    ]),
    [],
    ['逾期明细'],
    ['读者', '学号', '图书', '逾期天数', '预估罚款（元）'],
    ...overdue.value.records.map((record) => [
      record.readerName,
      record.readerNo,
      record.bookTitle,
      record.overdueDays,
      (record.overdueDays * 0.5).toFixed(2),
    ]),
  ]
  const csv = rows
    .map((row) => row.map((cell) => `"${String(cell ?? '').replaceAll('"', '""')}"`).join(','))
    .join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `图书馆统计报表_${dayjs().format('YYYYMMDD_HHmm')}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('统计报表已导出')
}

const readerTypeColor = (type) => READER_TYPES[type]?.color || '#72827e'
const readerTypeLabel = (type) => READER_TYPES[type]?.label || '读者'
</script>

<template>
  <div v-loading="loading" class="app-page stats-page">
    <PageHeader
      eyebrow="Insights"
      title="统计报表"
      description="从借阅数据中观察馆藏流转与阅读趋势，为馆藏建设提供依据。"
    >
      <template #actions>
        <el-radio-group v-model="range" size="small" @change="changeRange">
          <el-radio-button v-for="item in ranges" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button plain @click="exportReport">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </template>
    </PageHeader>

    <div class="stats-grid">
      <StatCard
        label="统计期借阅量"
        :value="overview.rangeBorrowCount || 0"
        suffix="次"
        icon="TrendCharts"
        tone="jade"
        :note="rangeLabel"
      />
      <StatCard
        label="注册读者"
        :value="overview.readerCount || 0"
        suffix="人"
        icon="User"
        tone="blue"
        :note="`本月活跃 ${overview.activeMonthReaders || 0} 人`"
      />
      <StatCard
        label="当前在借"
        :value="overview.activeLoans || 0"
        suffix="本"
        icon="Collection"
        tone="copper"
        :note="`馆藏可借 ${overview.availableCopies || 0} 册`"
      />
      <StatCard
        label="逾期未还"
        :value="overview.overdueLoans || 0"
        suffix="本"
        icon="Warning"
        tone="danger"
        :note="`待缴罚款 ${formatCurrency(overview.pendingFineAmount)}`"
      />
    </div>

    <div class="stats-grid-2 stats-grid-2--main">
      <section class="surface-card chart-card">
        <div class="section-heading">
          <div>
            <div class="eyebrow">Monthly Trend</div>
            <h3>借阅趋势</h3>
            <p>近 8 个月借出与归还变化</p>
          </div>
          <span class="chart-note">单位：次</span>
        </div>
        <BaseChart :option="monthlyOption" height="286px" />
      </section>

      <section class="surface-card chart-card">
        <div class="section-heading">
          <div>
            <div class="eyebrow">Category Mix</div>
            <h3>分类借阅构成</h3>
            <p>各类馆藏的累计借阅占比</p>
          </div>
          <span class="chart-note">{{ categoryTotal }} 次</span>
        </div>
        <BaseChart :option="categoryOption" height="196px" />
        <div class="category-legend">
          <div v-for="category in trends.categories" :key="category.id">
            <span :style="{ background: category.color }" />
            <p>{{ category.name }}</p>
            <strong>{{ category.value }}</strong>
          </div>
        </div>
      </section>
    </div>

    <div class="stats-grid-2">
      <section class="surface-card chart-card">
        <div class="section-heading">
          <div>
            <div class="eyebrow">Overdue Analysis</div>
            <h3>逾期与罚款趋势</h3>
            <p>近 6 个月生成罚款的笔数与金额</p>
          </div>
          <span class="chart-note">
            逾期未还 <strong>{{ overdue.summary.count || 0 }}</strong> 笔
          </span>
        </div>
        <BaseChart :option="overdueOption" height="250px" />
        <div class="bucket-list">
          <div v-for="bucket in overdue.buckets" :key="bucket.label" class="bucket-item">
            <div class="bucket-item__label">
              <span :style="{ background: bucket.color }" />
              {{ bucket.label }}
            </div>
            <div class="bucket-item__bar">
              <span
                :style="{
                  width: `${Math.max((bucket.value / overdueBucketsMax) * 100, bucket.value ? 8 : 0)}%`,
                  background: bucket.color,
                }"
              />
            </div>
            <strong>{{ bucket.value }}</strong>
          </div>
        </div>
      </section>

      <section class="surface-card chart-card reader-rank-card">
        <div class="section-heading">
          <div>
            <div class="eyebrow">Active Readers</div>
            <h3>读者借阅排行</h3>
            <p>累计借阅量最高的读者</p>
          </div>
        </div>
        <div class="reader-ranking">
          <div
            v-for="(reader, index) in ranking.readers.slice(0, 6)"
            :key="reader.id"
            class="reader-rank"
          >
            <span class="reader-rank__index" :class="{ 'is-top': index < 3 }">
              {{ String(index + 1).padStart(2, '0') }}
            </span>
            <span
              class="reader-rank__avatar"
              :style="{ background: readerTypeColor(reader.type) }"
            >
              {{ initials(reader.name) }}
            </span>
            <div class="reader-rank__copy">
              <strong>{{ reader.name }}</strong>
              <span>{{ readerTypeLabel(reader.type) }} · 当前在借 {{ reader.activeCount }} 本</span>
            </div>
            <div class="reader-rank__bar">
              <span
                :style="{
                  width: `${(reader.borrowCount / Math.max(ranking.readers[0]?.borrowCount || 1, 1)) * 100}%`,
                }"
              />
            </div>
            <strong class="reader-rank__count">{{ reader.borrowCount }} 次</strong>
          </div>
        </div>
      </section>
    </div>

    <div class="stats-grid-2 stats-grid-2--bottom">
      <section class="surface-card chart-card">
        <div class="section-heading">
          <div>
            <div class="eyebrow">Top Collection</div>
            <h3>馆藏借阅排行榜</h3>
            <p>{{ rangeLabel }}最受欢迎的图书</p>
          </div>
        </div>
        <div class="book-ranking">
          <div
            v-for="(book, index) in ranking.books"
            :key="book.id"
            class="book-rank"
          >
            <span class="book-rank__index" :class="{ 'is-top': index < 3 }">
              {{ String(index + 1).padStart(2, '0') }}
            </span>
            <div class="book-rank__cover"><BookCover :book="book" size="xs" flat /></div>
            <div class="book-rank__copy">
              <strong>{{ book.title }}</strong>
              <span>{{ book.author }} · {{ book.categoryName }}</span>
            </div>
            <div class="book-rank__bar">
              <span
                :style="{
                  width: `${(book.borrowCount / Math.max(ranking.books[0]?.borrowCount || 1, 1)) * 100}%`,
                  background: book.categoryColor,
                }"
              />
            </div>
            <strong class="book-rank__count">{{ book.borrowCount }}<small>次</small></strong>
          </div>
        </div>
      </section>

      <section class="surface-card chart-card overdue-card">
        <div class="section-heading">
          <div>
            <div class="eyebrow">Overdue Records</div>
            <h3>当前逾期明细</h3>
            <p>按逾期天数由高到低排序</p>
          </div>
          <span class="chart-note">
            最长 <strong>{{ overdue.summary.maxDays || 0 }}</strong> 天
          </span>
        </div>

        <div v-if="overdue.records.length" class="overdue-list">
          <div v-for="record in overdue.records.slice(0, 6)" :key="record.id" class="overdue-item">
            <div class="overdue-item__line">
              <span class="overdue-item__dot" />
              <div>
                <strong>《{{ record.bookTitle }}》</strong>
                <span>{{ record.readerName }} · {{ record.readerNo }}</span>
              </div>
              <span class="overdue-item__days">{{ record.overdueDays }} 天</span>
            </div>
            <div class="overdue-item__meta">
              <span>应还 {{ record.dueDate }}</span>
              <strong>预估 {{ formatCurrency(record.overdueDays * 0.5) }}</strong>
            </div>
          </div>
        </div>
        <div v-else class="overdue-empty">
          <el-icon><CircleCheckFilled /></el-icon>
          <strong>当前没有逾期记录</strong>
          <span>馆藏流转状态良好</span>
        </div>

        <div v-if="overdue.summary.count" class="overdue-footer">
          <span>逾期率 {{ overdue.summary.overdueRate }}%</span>
          <span>待缴 {{ formatCurrency(overdue.summary.pendingAmount) }}</span>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.stats-page {
  gap: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stats-grid-2 {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 16px;
}

.stats-grid-2--main {
  grid-template-columns: minmax(0, 1.5fr) minmax(330px, 0.7fr);
}

.stats-grid-2--bottom {
  grid-template-columns: minmax(0, 1.28fr) minmax(330px, 0.72fr);
}

.chart-card {
  padding: 20px 21px;
}

.chart-card .section-heading {
  margin-bottom: 10px;
}

.chart-card .section-heading h3 {
  font-size: 17px;
}

.chart-note {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.chart-note strong {
  color: var(--copper-600);
  font-size: 10px;
}

.category-legend {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px 14px;
  margin-top: 6px;
}

.category-legend > div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.category-legend span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.category-legend p {
  flex: 1;
  color: var(--text-500);
  font-size: 8.5px;
}

.category-legend strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 500;
}

.bucket-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 5px;
  padding-top: 11px;
  border-top: 1px dashed var(--line);
}

.bucket-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bucket-item__label {
  display: flex;
  align-items: center;
  gap: 5px;
  width: 65px;
  color: var(--text-500);
  font-size: 8px;
}

.bucket-item__label span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.bucket-item__bar {
  flex: 1;
  height: 4px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.bucket-item__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.bucket-item > strong {
  width: 20px;
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 8px;
  text-align: right;
}

.reader-ranking {
  display: flex;
  flex-direction: column;
}

.reader-rank {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 1px;
  border-bottom: 1px solid rgba(232, 223, 210, 0.55);
}

.reader-rank:last-child {
  border-bottom: 0;
}

.reader-rank__index {
  width: 17px;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.reader-rank__index.is-top {
  color: var(--copper-600);
  font-weight: 600;
}

.reader-rank__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  border-radius: 50%;
  color: #fff;
  font-family: var(--font-display);
  font-size: 8px;
}

.reader-rank__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.reader-rank__copy strong {
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 600;
}

.reader-rank__copy span {
  color: var(--text-400);
  font-size: 7px;
}

.reader-rank__bar {
  width: 56px;
  height: 3px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.reader-rank__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--jade-500);
}

.reader-rank__count {
  width: 32px;
  color: var(--text-500);
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 500;
  text-align: right;
}

.book-ranking {
  display: flex;
  flex-direction: column;
}

.book-rank {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 1px;
  border-bottom: 1px solid rgba(232, 223, 210, 0.55);
}

.book-rank:last-child {
  border-bottom: 0;
}

.book-rank__index {
  width: 17px;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.book-rank__index.is-top {
  color: var(--copper-600);
  font-weight: 600;
}

.book-rank__cover {
  width: 23px;
  flex-shrink: 0;
}

.book-rank__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.book-rank__copy strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-rank__copy span {
  overflow: hidden;
  color: var(--text-400);
  font-size: 7px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-rank__bar {
  width: 88px;
  height: 3px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.book-rank__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.book-rank__count {
  width: 30px;
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
  text-align: right;
}

.book-rank__count small {
  margin-left: 1px;
  color: var(--text-400);
  font-size: 6px;
  font-weight: 400;
}

.overdue-list {
  display: flex;
  flex-direction: column;
}

.overdue-item {
  padding: 8px 2px;
  border-bottom: 1px solid rgba(232, 223, 210, 0.55);
}

.overdue-item__line {
  display: flex;
  align-items: center;
  gap: 7px;
}

.overdue-item__dot {
  width: 5px;
  height: 5px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--danger);
}

.overdue-item__line > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.overdue-item__line strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 9.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overdue-item__line > div span {
  color: var(--text-400);
  font-size: 7px;
}

.overdue-item__days {
  padding: 2px 6px;
  border-radius: 3px;
  color: var(--danger);
  background: var(--danger-bg);
  font-family: var(--font-mono);
  font-size: 7.5px;
}

.overdue-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  padding-left: 12px;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 6.5px;
}

.overdue-item__meta strong {
  color: var(--warning);
  font-weight: 500;
}

.overdue-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 10px;
  padding-top: 9px;
  border-top: 1px dashed var(--line);
  color: var(--text-400);
  font-size: 7.5px;
}

.overdue-footer span + span::before {
  margin-right: 16px;
  color: var(--line-strong);
  content: '·';
}

.overdue-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 30px;
  color: var(--success);
}

.overdue-empty .el-icon {
  font-size: 26px;
}

.overdue-empty strong {
  color: var(--text-700);
  font-size: 10px;
}

.overdue-empty span {
  color: var(--text-400);
  font-size: 8px;
}

@media (max-width: 1180px) {
  .stats-grid-2,
  .stats-grid-2--main,
  .stats-grid-2--bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .stats-grid {
    gap: 8px;
  }

  .chart-card {
    padding: 17px;
  }

  .book-rank__bar {
    display: none;
  }

  .category-legend {
    grid-template-columns: 1fr;
  }
}
</style>
