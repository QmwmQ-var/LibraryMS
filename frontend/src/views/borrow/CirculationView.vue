<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import BookCover from '@/components/book/BookCover.vue'
import {
  borrowBook,
  getBorrowRecords,
  renewBook,
  returnBook,
} from '@/api/borrow'
import { getBooks } from '@/api/book'
import { getReader, getReaders } from '@/api/reader'
import { getOverview } from '@/api/stats'
import { useAppStore } from '@/stores/app'
import { BORROW_STATUS, READER_TYPES } from '@/utils/constants'
import { formatCurrency, initials, relativeDue } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const overview = ref({})
const readerQuery = ref('')
const readerResults = ref([])
const readerSearching = ref(false)
const selectedReader = ref(null)
const bookQuery = ref('')
const bookResults = ref([])
const bookLoading = ref(false)
const selectedBook = ref(null)
const borrowing = ref(false)
const borrowResult = ref(null)
const borrowResultVisible = ref(false)

const returnQuery = ref('')
const activeLoans = ref([])
const activeLoading = ref(false)
const returningId = ref(null)
const returnResult = ref(null)
const returnResultVisible = ref(false)

const records = ref([])
const recordsLoading = ref(false)
const recordsTotal = ref(0)
const recordFilters = reactive({
  keyword: '',
  status: '',
  dateRange: [],
  page: 1,
  size: 10,
})

const recentReaders = computed(() => {
  const seen = new Set()
  return activeLoans.value
    .filter((record) => {
      if (seen.has(record.readerId)) return false
      seen.add(record.readerId)
      return true
    })
    .slice(0, 4)
    .map((record) => ({
      id: record.readerId,
      name: record.readerName,
      studentNo: record.readerNo,
    }))
})

const selectedReaderConfig = computed(() =>
  selectedReader.value ? READER_TYPES[selectedReader.value.type] : null,
)

const dueDate = computed(() => {
  if (!selectedReaderConfig.value) return ''
  return dayjs().add(selectedReaderConfig.value.borrowDays, 'day').format('YYYY年M月D日')
})

const canConfirmBorrow = computed(() => {
  if (!selectedReader.value || !selectedBook.value) return false
  if (selectedReader.value.cardStatus !== 'ACTIVE') return false
  if (selectedReader.value.activeBorrowCount >= selectedReader.value.maxBorrow) return false
  if (selectedBook.value.availableCopies <= 0) return false
  return true
})

const borrowBlockedReason = computed(() => {
  if (!selectedReader.value) return '请先读取读者借阅证'
  if (selectedReader.value.cardStatus !== 'ACTIVE') return '借阅证状态异常，无法借阅'
  if (selectedReader.value.activeBorrowCount >= selectedReader.value.maxBorrow) {
    return '该读者已达借阅上限'
  }
  if (!selectedBook.value) return '请选择要借阅的图书'
  if (selectedBook.value.availableCopies <= 0) return '该图书暂无可借副本'
  return ''
})

const loadOverview = async () => {
  try {
    overview.value = await getOverview()
  } catch (error) {
    console.warn(error)
  }
}

const loadInitialBooks = async () => {
  bookLoading.value = true
  try {
    const result = await getBooks({ availability: 'AVAILABLE', sort: 'popular', page: 1, size: 6 })
    bookResults.value = result.records
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    bookLoading.value = false
  }
}

const loadActiveLoans = async (keyword = '') => {
  activeLoading.value = true
  try {
    const result = await getBorrowRecords({
      status: 'ACTIVE',
      keyword: keyword || undefined,
      page: 1,
      size: 8,
    })
    activeLoans.value = result.records
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    activeLoading.value = false
  }
}

const loadRecords = async () => {
  recordsLoading.value = true
  try {
    const [startDate, endDate] = recordFilters.dateRange || []
    const result = await getBorrowRecords({
      keyword: recordFilters.keyword || undefined,
      status: recordFilters.status || undefined,
      startDate: startDate ? dayjs(startDate).format('YYYY-MM-DD') : undefined,
      endDate: endDate ? dayjs(endDate).format('YYYY-MM-DD') : undefined,
      page: recordFilters.page,
      size: recordFilters.size,
    })
    records.value = result.records
    recordsTotal.value = result.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    recordsLoading.value = false
  }
}

onMounted(async () => {
  if (route.query.status) recordFilters.status = String(route.query.status)
  await Promise.all([
    loadOverview(),
    loadInitialBooks(),
    loadActiveLoans(),
    loadRecords(),
  ])
})

const searchReaders = async () => {
  const keyword = readerQuery.value.trim()
  if (!keyword) {
    ElMessage.info('请输入学号或姓名')
    return
  }
  readerSearching.value = true
  try {
    const result = await getReaders({ keyword, page: 1, size: 6 })
    readerResults.value = result.records
    if (!result.records.length) ElMessage.warning('未找到匹配的读者')
    else if (result.records.length === 1) selectReader(result.records[0])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    readerSearching.value = false
  }
}

const selectReader = async (reader) => {
  try {
    selectedReader.value =
      reader.activeBorrowCount !== undefined ? reader : await getReader(reader.id)
    readerResults.value = []
    readerQuery.value = reader.studentNo || reader.readerNo || ''
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const searchBooks = async () => {
  bookLoading.value = true
  try {
    const result = await getBooks({
      keyword: bookQuery.value.trim() || undefined,
      availability: 'AVAILABLE',
      page: 1,
      size: 6,
    })
    bookResults.value = result.records
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    bookLoading.value = false
  }
}

const selectBook = (book) => {
  selectedBook.value = book
}

const confirmBorrow = async () => {
  if (!canConfirmBorrow.value) {
    ElMessage.warning(borrowBlockedReason.value)
    return
  }
  try {
    await ElMessageBox.confirm(
      `为 ${selectedReader.value.name} 办理《${selectedBook.value.title}》借阅？应还日期 ${dueDate.value}。`,
      '确认借阅',
      { confirmButtonText: '确认办理', cancelButtonText: '取消', type: 'info' },
    )
    borrowing.value = true
    const result = await borrowBook({
      readerId: selectedReader.value.id,
      bookId: selectedBook.value.id,
    })
    borrowResult.value = result
    borrowResultVisible.value = true
    selectedBook.value = null
    bookQuery.value = ''
    await Promise.all([
      loadInitialBooks(),
      loadActiveLoans(returnQuery.value.trim()),
      loadOverview(),
      loadRecords(),
    ])
    selectedReader.value = await getReader(selectedReader.value.id)
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '借阅办理失败')
    }
  } finally {
    borrowing.value = false
  }
}

const searchActiveLoans = () => {
  loadActiveLoans(returnQuery.value.trim())
}

const doReturn = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确认为 ${record.readerName} 归还《${record.bookTitle}》吗？系统将自动计算逾期罚款。`,
      '归还图书',
      { confirmButtonText: '确认归还', cancelButtonText: '取消', type: 'warning' },
    )
    returningId.value = record.id
    const result = await returnBook(record.id)
    returnResult.value = result
    returnResultVisible.value = true
    await Promise.all([
      loadActiveLoans(returnQuery.value.trim()),
      loadOverview(),
      loadRecords(),
      loadInitialBooks(),
    ])
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '还书失败')
    }
  } finally {
    returningId.value = null
  }
}

const doRenew = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确认为 ${record.readerName} 续借《${record.bookTitle}》吗？每本图书最多续借 1 次。`,
      '续借图书',
      { confirmButtonText: '确认续借', cancelButtonText: '取消', type: 'info' },
    )
    await renewBook(record.id)
    ElMessage.success('续借成功，应还日期已延长')
    await Promise.all([loadActiveLoans(returnQuery.value.trim()), loadRecords()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '续借失败')
    }
  }
}

const resetRecordFilters = () => {
  Object.assign(recordFilters, {
    keyword: '',
    status: '',
    dateRange: [],
    page: 1,
  })
  loadRecords()
  if (route.query.status) router.replace({ query: {} })
}

const searchRecords = () => {
  recordFilters.page = 1
  loadRecords()
}

const onRecordsPage = (page) => {
  recordFilters.page = page
  loadRecords()
}

const statusTone = (status) => BORROW_STATUS[status]?.tone || 'gray'
const statusLabel = (status) => BORROW_STATUS[status]?.label || status

const summaryItems = computed(() => [
  { label: '今日借出', value: overview.value.todayBorrows || 0, suffix: '本' },
  { label: '当前在借', value: overview.value.activeLoans || 0, suffix: '本' },
  { label: '逾期未还', value: overview.value.overdueLoans || 0, suffix: '本', danger: true },
  { label: '待缴罚款', value: (overview.value.pendingFineAmount || 0).toFixed(2), suffix: '元' },
])
</script>

<template>
  <div class="app-page">
    <PageHeader
      eyebrow="Circulation"
      title="流通办理"
      description="借书、还书与续借一站式办理，每次操作都在同一事务中完成。"
    >
      <template #actions>
        <el-button plain @click="loadOverview(); loadActiveLoans(returnQuery); loadRecords()">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button type="primary" @click="readerQuery = '2023001'; searchReaders()">
          <el-icon><Aim /></el-icon>
          快速体验
        </el-button>
      </template>
    </PageHeader>

    <section class="desk-summary">
      <div class="desk-summary__intro">
        <div class="desk-summary__eyebrow">CIRCULATION DESK · {{ dayjs().format('YYYY.MM.DD') }}</div>
        <h2>今日流通服务台</h2>
        <p>请核对读者身份与图书信息后办理，操作将实时同步馆藏状态。</p>
      </div>
      <div class="desk-summary__stats">
        <div v-for="item in summaryItems" :key="item.label" class="desk-stat">
          <span>{{ item.label }}</span>
          <strong :class="{ 'is-danger': item.danger }">
            {{ item.value }}<small>{{ item.suffix }}</small>
          </strong>
        </div>
      </div>
      <span class="desk-summary__arc" />
    </section>

    <div class="desk-layout">
      <section class="surface-card borrow-desk">
        <div class="desk-head">
          <div>
            <div class="eyebrow">Borrow</div>
            <h2>借阅办理</h2>
            <p>依次读取借阅证、选择图书，然后确认借出。</p>
          </div>
          <div class="desk-steps">
            <span class="is-done"><i>1</i>读者</span>
            <em />
            <span :class="{ 'is-done': selectedBook }"><i>2</i>图书</span>
            <em />
            <span :class="{ 'is-done': canConfirmBorrow }"><i>3</i>确认</span>
          </div>
        </div>

        <div class="desk-columns">
          <div class="desk-panel">
            <div class="desk-panel__title">
              <span>01</span>
              <div>
                <strong>读取读者借阅证</strong>
                <small>输入学号或姓名，调取读者档案</small>
              </div>
            </div>

            <div class="desk-search">
              <el-input
                v-model="readerQuery"
                placeholder="学号 / 姓名"
                clearable
                @keyup.enter="searchReaders"
              >
                <template #prefix><el-icon><CreditCard /></el-icon></template>
              </el-input>
              <el-button type="primary" :loading="readerSearching" @click="searchReaders">
                读取
              </el-button>
            </div>

            <div v-if="readerResults.length" class="quick-results">
              <button
                v-for="reader in readerResults"
                :key="reader.id"
                class="quick-result"
                @click="selectReader(reader)"
              >
                <span class="quick-result__avatar">{{ initials(reader.name) }}</span>
                <div>
                  <strong>{{ reader.name }}</strong>
                  <span>{{ reader.studentNo }} · {{ reader.department }}</span>
                </div>
                <span class="pill" :class="reader.cardStatus === 'ACTIVE' ? 'pill--green' : 'pill--amber'">
                  {{ reader.cardStatus === 'ACTIVE' ? '正常' : '异常' }}
                </span>
              </button>
            </div>

            <div v-if="!readerQuery && !selectedReader && recentReaders.length" class="recent-readers">
              <span class="recent-readers__label">最近在借读者</span>
              <div>
                <button
                  v-for="reader in recentReaders"
                  :key="reader.id"
                  @click="selectReader(reader)"
                >
                  {{ reader.name }} <small>{{ reader.studentNo }}</small>
                </button>
              </div>
            </div>

            <div v-if="selectedReader" class="selected-reader">
              <div class="selected-reader__head">
                <span
                  class="selected-reader__avatar"
                  :style="{ background: READER_TYPES[selectedReader.type]?.color }"
                >
                  {{ initials(selectedReader.name) }}
                </span>
                <div>
                  <strong>{{ selectedReader.name }}</strong>
                  <span>{{ selectedReader.studentNo }} · {{ selectedReader.department }}</span>
                </div>
                <span
                  class="pill"
                  :class="selectedReader.cardStatus === 'ACTIVE' ? 'pill--green' : 'pill--red'"
                >
                  <span class="pill__dot" />
                  {{ selectedReader.cardStatus === 'ACTIVE' ? '借阅证正常' : '借阅证异常' }}
                </span>
              </div>
              <div class="selected-reader__grid">
                <div>
                  <span>读者类型</span>
                  <strong>{{ READER_TYPES[selectedReader.type]?.label }}</strong>
                </div>
                <div>
                  <span>已借 / 上限</span>
                  <strong>{{ selectedReader.activeBorrowCount }} / {{ selectedReader.maxBorrow }}</strong>
                </div>
                <div>
                  <span>可用额度</span>
                  <strong class="text-success">{{ selectedReader.maxBorrow - selectedReader.activeBorrowCount }} 本</strong>
                </div>
                <div>
                  <span>待缴罚款</span>
                  <strong :class="selectedReader.unpaidAmount ? 'text-danger' : ''">
                    {{ formatCurrency(selectedReader.unpaidAmount) }}
                  </strong>
                </div>
              </div>
              <div v-if="selectedReader.activeBorrowCount >= selectedReader.maxBorrow" class="desk-warning">
                <el-icon><WarningFilled /></el-icon>
                该读者已达借阅上限，请先归还部分图书。
              </div>
            </div>

            <div v-else class="desk-placeholder">
              <span class="desk-placeholder__icon"><el-icon><CreditCard /></el-icon></span>
              <strong>等待读取借阅证</strong>
              <p>可直接点击上方最近读者，或输入学号查询。</p>
            </div>
          </div>

          <div class="desk-divider" />

          <div class="desk-panel">
            <div class="desk-panel__title">
              <span>02</span>
              <div>
                <strong>选择借阅图书</strong>
                <small>仅展示当前有可借副本的图书</small>
              </div>
            </div>

            <div class="desk-search">
              <el-input
                v-model="bookQuery"
                placeholder="书名 / ISBN / 作者"
                clearable
                @keyup.enter="searchBooks"
              >
                <template #prefix><el-icon><Reading /></el-icon></template>
              </el-input>
              <el-button plain :loading="bookLoading" @click="searchBooks">检索</el-button>
            </div>

            <div v-loading="bookLoading" class="desk-book-list">
              <button
                v-for="book in bookResults"
                :key="book.id"
                class="desk-book"
                :class="{ 'is-selected': selectedBook?.id === book.id }"
                @click="selectBook(book)"
              >
                <div class="desk-book__cover"><BookCover :book="book" size="xs" flat /></div>
                <div class="desk-book__copy">
                  <strong>{{ book.title }}</strong>
                  <span>{{ book.author }} · {{ book.isbn }}</span>
                </div>
                <span class="desk-book__stock">可借 {{ book.availableCopies }}</span>
                <span class="desk-book__check">
                  <el-icon v-if="selectedBook?.id === book.id"><Check /></el-icon>
                </span>
              </button>
              <el-empty v-if="!bookResults.length && !bookLoading" description="没有可借图书" :image-size="55" />
            </div>
          </div>
        </div>

        <div class="checkout-bar" :class="{ 'is-ready': canConfirmBorrow }">
          <div class="checkout-bar__flow">
            <div>
              <span>读者</span>
              <strong>{{ selectedReader?.name || '待读取' }}</strong>
            </div>
            <el-icon><Right /></el-icon>
            <div>
              <span>图书</span>
              <strong>{{ selectedBook?.title || '待选择' }}</strong>
            </div>
            <el-icon><Right /></el-icon>
            <div>
              <span>应还日期</span>
              <strong>{{ canConfirmBorrow ? dueDate : '—' }}</strong>
            </div>
          </div>
          <div class="checkout-bar__action">
            <span v-if="borrowBlockedReason" class="checkout-bar__hint">{{ borrowBlockedReason }}</span>
            <el-button
              type="primary"
              :disabled="!canConfirmBorrow"
              :loading="borrowing"
              @click="confirmBorrow"
            >
              确认借阅
              <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
          </div>
        </div>
      </section>

      <section class="surface-card return-desk">
        <div class="desk-head desk-head--compact">
          <div>
            <div class="eyebrow">Return</div>
            <h2>还书通道</h2>
            <p>按书名、ISBN 或读者检索在借记录。</p>
          </div>
          <span class="return-desk__count">{{ activeLoans.length }} 笔在借</span>
        </div>

        <div class="return-search">
          <el-input
            v-model="returnQuery"
            placeholder="扫描 ISBN 或输入读者"
            clearable
            @keyup.enter="searchActiveLoans"
            @clear="searchActiveLoans"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button plain :loading="activeLoading" @click="searchActiveLoans">查询</el-button>
        </div>

        <div v-loading="activeLoading" class="return-list">
          <div v-for="record in activeLoans" :key="record.id" class="return-item">
            <div class="return-item__cover"><BookCover :book="record" size="xs" flat /></div>
            <div class="return-item__copy">
              <strong>{{ record.bookTitle }}</strong>
              <span>{{ record.readerName }} · {{ record.readerNo }}</span>
              <span class="return-item__due" :class="{ 'is-late': record.status === 'OVERDUE' }">
                {{ relativeDue(record.dueDate).label }}
              </span>
            </div>
            <el-button
              size="small"
              plain
              :loading="returningId === record.id"
              @click="doReturn(record)"
            >
              还书
            </el-button>
          </div>
          <el-empty
            v-if="!activeLoans.length && !activeLoading"
            description="未找到在借记录"
            :image-size="58"
          />
        </div>

        <div class="return-tip">
          <el-icon><InfoFilled /></el-icon>
          还书后系统自动更新库存；逾期将按每日 ¥0.50 生成罚款。
        </div>
      </section>
    </div>

    <section class="surface-card records-panel">
      <div class="section-heading">
        <div>
          <div class="eyebrow">Records</div>
          <h3>借阅台账</h3>
          <p>按读者、图书、状态或借阅日期查询全部记录</p>
        </div>
        <span class="records-total">共 {{ recordsTotal }} 条记录</span>
      </div>

      <div class="filter-bar records-filter">
        <el-input
          v-model="recordFilters.keyword"
          class="search-input"
          placeholder="读者姓名 / 学号 / 书名 / ISBN"
          clearable
          @keyup.enter="searchRecords"
          @clear="searchRecords"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="recordFilters.status" placeholder="全部状态" clearable @change="searchRecords">
          <el-option label="借阅中" value="BORROWED" />
          <el-option label="已逾期" value="OVERDUE" />
          <el-option label="已归还" value="RETURNED" />
          <el-option label="在借（全部）" value="ACTIVE" />
        </el-select>
        <el-date-picker
          v-model="recordFilters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="借阅开始"
          end-placeholder="借阅结束"
          value-format="YYYY-MM-DD"
          @change="searchRecords"
        />
        <el-button type="primary" plain @click="searchRecords">查询</el-button>
        <el-button text @click="resetRecordFilters">重置</el-button>
      </div>

      <el-table v-loading="recordsLoading" :data="records" row-key="id">
        <el-table-column label="图书" min-width="220">
          <template #default="{ row }">
            <div class="record-book">
              <div class="record-book__cover"><BookCover :book="row" size="xs" flat /></div>
              <div>
                <strong>{{ row.bookTitle }}</strong>
                <span>{{ row.bookIsbn }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="读者" width="150">
          <template #default="{ row }">
            <div class="record-reader">
              <strong>{{ row.readerName }}</strong>
              <span>{{ row.readerNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借出日期" width="112" prop="borrowDate" />
        <el-table-column label="应还日期" width="112">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.status === 'OVERDUE' }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归还日期" width="112">
          <template #default="{ row }">{{ row.returnDate || '—' }}</template>
        </el-table-column>
        <el-table-column label="续借" width="70">
          <template #default="{ row }">{{ row.renewCount }} / 1</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <span class="pill" :class="`pill--${statusTone(row.status)}`">
              <span class="pill__dot" />
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="126" fixed="right">
          <template #default="{ row }">
            <template v-if="['BORROWED', 'OVERDUE'].includes(row.status)">
              <el-button text size="small" @click="doReturn(row)">还书</el-button>
              <el-button
                text
                size="small"
                :disabled="row.status === 'OVERDUE' || row.renewCount >= 1"
                @click="doRenew(row)"
              >
                续借
              </el-button>
            </template>
            <span v-else class="record-done">已完成</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="recordsTotal > recordFilters.size"
        background
        layout="prev, pager, next, total"
        :current-page="recordFilters.page"
        :page-size="recordFilters.size"
        :total="recordsTotal"
        @current-change="onRecordsPage"
      />
    </section>

    <el-dialog v-model="borrowResultVisible" width="430px">
      <div v-if="borrowResult" class="flow-result">
        <div class="flow-result__icon is-success"><el-icon><Check /></el-icon></div>
        <div class="flow-result__eyebrow">BORROW COMPLETED</div>
        <h2>借阅办理成功</h2>
        <p>《{{ borrowResult.record.bookTitle }}》已借给 {{ borrowResult.record.readerName }}</p>
        <div class="flow-result__meta">
          <div><span>借阅日期</span><strong>{{ borrowResult.record.borrowDate }}</strong></div>
          <div><span>应还日期</span><strong>{{ borrowResult.record.dueDate }}</strong></div>
        </div>
        <el-button type="primary" class="w-full" @click="borrowResultVisible = false">
          继续办理
        </el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="returnResultVisible" width="430px">
      <div v-if="returnResult" class="flow-result">
        <div
          class="flow-result__icon"
          :class="returnResult.penalty ? 'is-warning' : 'is-success'"
        >
          <el-icon><Check v-if="!returnResult.penalty" /><WarningFilled v-else /></el-icon>
        </div>
        <div class="flow-result__eyebrow">RETURN COMPLETED</div>
        <h2>归还成功</h2>
        <p>《{{ returnResult.record.bookTitle }}》已归还并恢复馆藏库存。</p>
        <div v-if="returnResult.penalty" class="fine-alert">
          <span>本次逾期 {{ returnResult.penalty.overdueDays }} 天，产生罚款</span>
          <strong>{{ formatCurrency(returnResult.penalty.amount) }}</strong>
        </div>
        <div v-else class="fine-clear">
          <el-icon><CircleCheck /></el-icon>
          按时归还，本次没有产生罚款
        </div>
        <el-button type="primary" class="w-full" @click="returnResultVisible = false">
          完成
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.desk-summary {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 128px;
  padding: 22px 30px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f5ebde;
  background:
    radial-gradient(circle at 88% 16%, rgba(201, 146, 100, 0.2), transparent 30%),
    linear-gradient(120deg, #17443e, #0d2f30);
}

.desk-summary__eyebrow {
  color: rgba(204, 220, 210, 0.38);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.desk-summary__intro h2 {
  margin-top: 5px;
  color: #f7ede0;
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 700;
}

.desk-summary__intro p {
  margin-top: 4px;
  color: rgba(207, 223, 213, 0.45);
  font-size: 9px;
}

.desk-summary__stats {
  display: flex;
  gap: clamp(24px, 4vw, 50px);
  margin-left: auto;
  padding-right: 70px;
}

.desk-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.desk-stat span {
  color: rgba(196, 213, 203, 0.4);
  font-size: 8px;
}

.desk-stat strong {
  color: #f2e4d1;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.desk-stat strong.is-danger {
  color: #e7a99e;
}

.desk-stat small {
  margin-left: 2px;
  color: rgba(196, 213, 203, 0.34);
  font-family: var(--font-sans);
  font-size: 8px;
  font-weight: 400;
}

.desk-summary__arc {
  position: absolute;
  top: -80px;
  right: -40px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(225, 195, 163, 0.09);
  border-radius: 50%;
}

.desk-summary__arc::after {
  position: absolute;
  inset: 34px;
  border: 1px solid rgba(225, 195, 163, 0.06);
  border-radius: 50%;
  content: '';
}

.desk-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.62fr) minmax(300px, 0.72fr);
  gap: 16px;
}

.borrow-desk,
.return-desk {
  padding: 22px 23px;
}

.desk-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.desk-head .eyebrow {
  margin-bottom: 4px;
}

.desk-head h2 {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
}

.desk-head p {
  margin-top: 3px;
  color: var(--text-400);
  font-size: 9px;
}

.desk-steps {
  display: flex;
  align-items: center;
  gap: 5px;
  padding-top: 3px;
}

.desk-steps span {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-400);
  font-size: 7.5px;
}

.desk-steps span i {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: 1px solid var(--line-strong);
  border-radius: 50%;
  font-family: var(--font-mono);
  font-size: 7px;
  font-style: normal;
}

.desk-steps span.is-done {
  color: var(--jade-500);
}

.desk-steps span.is-done i {
  color: #fff;
  border-color: var(--jade-500);
  background: var(--jade-500);
}

.desk-steps em {
  width: 15px;
  height: 1px;
  background: var(--line);
}

.desk-columns {
  display: grid;
  grid-template-columns: 1fr 1px 1fr;
  gap: 20px;
}

.desk-divider {
  background: var(--line);
}

.desk-panel {
  min-width: 0;
}

.desk-panel__title {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 11px;
}

.desk-panel__title > span {
  color: var(--copper-500);
  font-family: var(--font-mono);
  font-size: 8px;
  padding-top: 2px;
}

.desk-panel__title > div {
  display: flex;
  flex-direction: column;
}

.desk-panel__title strong {
  color: var(--text-700);
  font-size: 10.5px;
  font-weight: 650;
}

.desk-panel__title small {
  color: var(--text-400);
  font-size: 8px;
}

.desk-search {
  display: flex;
  gap: 7px;
}

.desk-search .el-input {
  flex: 1;
}

.desk-search .el-button {
  flex-shrink: 0;
  padding-right: 13px;
  padding-left: 13px;
}

.quick-results {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 8px;
  padding: 4px;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.quick-result {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.quick-result:hover {
  background: var(--paper-100);
}

.quick-result__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  height: 25px;
  border-radius: 50%;
  color: #fff;
  background: var(--jade-500);
  font-family: var(--font-display);
  font-size: 8px;
}

.quick-result > div {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.quick-result strong {
  color: var(--text-700);
  font-size: 9.5px;
}

.quick-result > div span {
  color: var(--text-400);
  font-size: 7.5px;
}

.quick-result .pill {
  font-size: 7px;
}

.recent-readers {
  margin-top: 12px;
}

.recent-readers__label {
  color: var(--text-400);
  font-size: 8px;
}

.recent-readers > div {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 6px;
}

.recent-readers button {
  padding: 3px 8px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--text-500);
  background: var(--paper-50);
  font-size: 8px;
  cursor: pointer;
}

.recent-readers button:hover {
  color: var(--ink-700);
  border-color: var(--ink-200);
  background: var(--jade-100);
}

.recent-readers small {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
}

.selected-reader {
  margin-top: 11px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: linear-gradient(135deg, var(--paper-100), var(--paper-50));
}

.selected-reader__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selected-reader__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  font-family: var(--font-display);
  font-size: 10px;
}

.selected-reader__head > div {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.selected-reader__head strong {
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 13px;
}

.selected-reader__head > div span {
  color: var(--text-400);
  font-size: 7.5px;
}

.selected-reader__head .pill {
  font-size: 7px;
}

.selected-reader__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 11px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.selected-reader__grid > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.selected-reader__grid span {
  color: var(--text-400);
  font-size: 7px;
}

.selected-reader__grid strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 550;
  white-space: nowrap;
}

.desk-warning {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 9px;
  padding: 6px 8px;
  border-radius: 5px;
  color: var(--danger);
  background: var(--danger-bg);
  font-size: 7.5px;
}

.desk-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 132px;
  margin-top: 10px;
  border: 1px dashed var(--line);
  border-radius: 9px;
  text-align: center;
}

.desk-placeholder__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 33px;
  height: 33px;
  border-radius: 50%;
  color: var(--text-400);
  background: var(--paper-100);
  font-size: 14px;
}

.desk-placeholder strong {
  margin-top: 7px;
  color: var(--text-500);
  font-size: 9.5px;
  font-weight: 550;
}

.desk-placeholder p {
  margin-top: 2px;
  color: var(--text-400);
  font-size: 7.5px;
}

.desk-book-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 220px;
  min-height: 120px;
  margin-top: 10px;
  overflow-y: auto;
  padding-right: 2px;
}

.desk-book {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.desk-book:hover {
  background: var(--paper-100);
}

.desk-book.is-selected {
  border-color: var(--ink-200);
  background: var(--jade-100);
}

.desk-book__cover {
  width: 24px;
  flex-shrink: 0;
}

.desk-book__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.desk-book__copy strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desk-book__copy span {
  overflow: hidden;
  color: var(--text-400);
  font-size: 7px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desk-book__stock {
  color: var(--success);
  font-size: 7.5px;
  white-space: nowrap;
}

.desk-book__check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  border: 1px solid var(--line-strong);
  border-radius: 50%;
  color: #fff;
  font-size: 8px;
}

.desk-book.is-selected .desk-book__check {
  border-color: var(--jade-500);
  background: var(--jade-500);
}

.checkout-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--paper-100);
}

.checkout-bar.is-ready {
  border-color: rgba(46, 115, 100, 0.2);
  background: linear-gradient(90deg, var(--jade-100), var(--paper-100));
}

.checkout-bar__flow {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 14px;
}

.checkout-bar__flow > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.checkout-bar__flow span {
  color: var(--text-400);
  font-size: 7px;
}

.checkout-bar__flow strong {
  overflow: hidden;
  max-width: 150px;
  color: var(--text-700);
  font-size: 9.5px;
  font-weight: 550;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checkout-bar__flow > .el-icon {
  color: var(--line-strong);
  font-size: 10px;
}

.checkout-bar__action {
  display: flex;
  align-items: center;
  gap: 10px;
}

.checkout-bar__hint {
  color: var(--text-400);
  font-size: 7.5px;
}

.return-desk {
  display: flex;
  flex-direction: column;
}

.desk-head--compact {
  margin-bottom: 10px;
}

.return-desk__count {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.return-search {
  display: flex;
  gap: 7px;
}

.return-search .el-input {
  flex: 1;
}

.return-search .el-button {
  flex-shrink: 0;
}

.return-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  max-height: 261px;
  min-height: 140px;
  margin-top: 11px;
  overflow-y: auto;
}

.return-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 6px;
  border-bottom: 1px solid var(--line);
}

.return-item__cover {
  width: 24px;
  flex-shrink: 0;
}

.return-item__copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.return-item__copy strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 9.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.return-item__copy > span {
  color: var(--text-400);
  font-size: 7px;
}

.return-item__due {
  color: var(--success) !important;
}

.return-item__due.is-late {
  color: var(--danger) !important;
}

.return-item .el-button {
  height: 23px;
  padding: 0 9px;
  font-size: 8px;
}

.return-tip {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
  color: var(--text-400);
  font-size: 7.5px;
  line-height: 1.6;
}

.return-tip .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 10px;
}

.records-panel {
  padding: 21px 22px 16px;
}

.records-total {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.records-filter .search-input {
  flex: 1;
  width: auto;
  min-width: 220px;
}

.records-filter :deep(.el-date-editor) {
  width: 235px;
}

.record-book {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record-book__cover {
  width: 25px;
  flex-shrink: 0;
}

.record-book > div:last-child {
  display: flex;
  flex-direction: column;
}

.record-book strong {
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 10.5px;
  font-weight: 600;
}

.record-book span {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
}

.record-reader {
  display: flex;
  flex-direction: column;
}

.record-reader strong {
  color: var(--text-700);
  font-size: 9.5px;
}

.record-reader span {
  color: var(--text-400);
  font-size: 8px;
}

.record-done {
  color: var(--text-400);
  font-size: 8px;
}

.flow-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 5px 2px;
  text-align: center;
}

.flow-result__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  margin-bottom: 12px;
  border-radius: 50%;
  font-size: 24px;
}

.flow-result__icon.is-success {
  color: var(--success);
  background: var(--success-bg);
}

.flow-result__icon.is-warning {
  color: var(--warning);
  background: var(--warning-bg);
}

.flow-result__eyebrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.flow-result h2 {
  margin-top: 6px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 23px;
}

.flow-result > p {
  margin-top: 5px;
  color: var(--text-500);
  font-size: 9.5px;
}

.flow-result__meta {
  display: flex;
  width: 100%;
  gap: 1px;
  margin-top: 17px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--line);
}

.flow-result__meta > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  padding: 10px;
  background: var(--paper-50);
}

.flow-result__meta span {
  color: var(--text-400);
  font-size: 7.5px;
}

.flow-result__meta strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
}

.fine-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin: 13px 0 16px;
  padding: 10px 12px;
  border-radius: 7px;
  color: var(--warning);
  background: var(--warning-bg);
  font-size: 8.5px;
  text-align: left;
}

.fine-alert strong {
  font-family: var(--font-display);
  font-size: 15px;
}

.fine-clear {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 13px 0 16px;
  padding: 8px 12px;
  border-radius: 7px;
  color: var(--success);
  background: var(--success-bg);
  font-size: 8.5px;
}

.flow-result > .el-button {
  margin-top: 4px;
}

@media (max-width: 1280px) {
  .desk-layout {
    grid-template-columns: 1fr;
  }

  .return-desk {
    min-height: auto;
  }

  .return-list {
    max-height: 230px;
  }
}

@media (max-width: 1000px) {
  .desk-summary {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }

  .desk-summary__stats {
    margin-left: 0;
    padding-right: 0;
  }

  .desk-columns {
    grid-template-columns: 1fr;
  }

  .desk-divider {
    height: 1px;
  }

  .checkout-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .checkout-bar__action {
    width: 100%;
    justify-content: flex-end;
  }
}

@media (max-width: 680px) {
  .desk-summary {
    padding: 20px;
  }

  .desk-summary__stats {
    flex-wrap: wrap;
    gap: 20px;
  }

  .desk-steps {
    display: none;
  }

  .borrow-desk,
  .return-desk,
  .records-panel {
    padding: 17px;
  }

  .checkout-bar__flow {
    flex-wrap: wrap;
    gap: 9px;
  }

  .checkout-bar__flow > .el-icon {
    display: none;
  }

  .selected-reader__grid {
    grid-template-columns: 1fr 1fr;
  }

  .records-filter .search-input {
    flex-basis: 100%;
  }

  .records-filter :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
