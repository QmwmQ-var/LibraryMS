<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import BookCard from '@/components/book/BookCard.vue'
import BookCover from '@/components/book/BookCover.vue'
import {
  createBook,
  getBook,
  getBooks,
  getCategories,
  removeBook,
  updateBook,
} from '@/api/book'
import { borrowBook } from '@/api/borrow'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { formatCurrency } from '@/utils/format'
import { READER_TYPES } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const appStore = useAppStore()

const books = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(true)
const viewMode = ref('card')
const detailVisible = ref(false)
const selectedBook = ref(null)
const formVisible = ref(false)
const formRef = ref()
const submitting = ref(false)
const deletingId = ref(null)
const borrowLoading = ref(false)
const borrowResult = ref(null)
const borrowResultVisible = ref(false)
const isAdmin = computed(() => auth.isAdmin)
const isManagement = computed(() => route.meta.management === true)

const filters = reactive({
  keyword: '',
  categoryId: null,
  availability: '',
  sort: 'recommended',
  page: 1,
  size: 9,
})

const emptyForm = () => ({
  id: null,
  title: '',
  author: '',
  isbn: '',
  categoryId: null,
  publisher: '',
  totalCopies: 1,
  publishYear: dayjs().year(),
  price: 0,
  location: '',
  description: '',
})

const form = reactive(emptyForm())

const categoryOptions = computed(() =>
  categories.value.map((item) => ({ value: item.id, label: item.name })),
)

const totalAvailableCopies = computed(() =>
  books.value.reduce((sum, book) => sum + book.availableCopies, 0),
)

const rules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  isbn: [
    { required: true, message: '请输入 ISBN', trigger: 'blur' },
    {
      pattern: /^(97[89])?\d{9}[\dX]$/i,
      message: 'ISBN 应为 10 位或 13 位数字',
      trigger: 'blur',
    },
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  totalCopies: [{ required: true, message: '请设置馆藏总量', trigger: 'change' }],
}

const loadCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const loadBooks = async () => {
  loading.value = true
  try {
    const result = await getBooks({
      keyword: filters.keyword || undefined,
      categoryId: filters.categoryId || undefined,
      availability: filters.availability || undefined,
      sort: filters.sort,
      page: filters.page,
      size: filters.size,
    })
    books.value = result.records
    total.value = result.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const syncFromRoute = () => {
  if (typeof route.query.keyword === 'string') filters.keyword = route.query.keyword
  if (route.query.categoryId) filters.categoryId = Number(route.query.categoryId)
  if (typeof route.query.availability === 'string') {
    filters.availability = route.query.availability
  }
}

const openBookFromQuery = async () => {
  if (!route.query.bookId) return
  try {
    const book = await getBook(route.query.bookId)
    openDetail(book)
  } catch {
    // 无效的书目 ID 不打断页面。
  }
}

onMounted(async () => {
  syncFromRoute()
  await Promise.all([loadCategories(), loadBooks()])
  await openBookFromQuery()
})

watch(
  () => route.query,
  async (query) => {
    if (query.bookId) await openBookFromQuery()
    if (query.keyword !== undefined || query.availability !== undefined) {
      syncFromRoute()
      filters.page = 1
      await loadBooks()
    }
  },
  { deep: true },
)

const search = () => {
  filters.page = 1
  loadBooks()
}

const resetFilters = () => {
  Object.assign(filters, {
    keyword: '',
    categoryId: null,
    availability: '',
    sort: 'recommended',
    page: 1,
  })
  loadBooks()
}

const onPageChange = (page) => {
  filters.page = page
  loadBooks()
}

const openDetail = (book) => {
  selectedBook.value = book
  detailVisible.value = true
}

const openCreate = () => {
  Object.assign(form, emptyForm())
  formVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = (book) => {
  Object.assign(form, emptyForm(), {
    id: book.id,
    title: book.title,
    author: book.author,
    isbn: book.isbn,
    categoryId: book.categoryId,
    publisher: book.publisher,
    totalCopies: book.totalCopies,
    publishYear: book.publishYear,
    price: book.price,
    location: book.location,
    description: book.description,
  })
  formVisible.value = true
  detailVisible.value = false
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    const isEditing = Boolean(payload.id)
    if (isEditing) await updateBook(payload.id, payload)
    else await createBook(payload)
    ElMessage.success(isEditing ? '图书信息已更新' : '新图书已入库')
    formVisible.value = false
    if (!isEditing) {
      // 新书默认追加在列表末页，创建后直接以书名定位，避免馆员找不到刚录入的图书。
      filters.keyword = payload.title
      filters.categoryId = null
      filters.availability = ''
      filters.page = 1
    }
    await Promise.all([loadBooks(), loadCategories()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

const confirmRemove = async (book) => {
  try {
    await ElMessageBox.confirm(
      `确认删除《${book.title}》吗？若仍有未归还副本，系统将阻止删除。`,
      '删除图书',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
    deletingId.value = book.id
    await removeBook(book.id)
    ElMessage.success('图书已删除')
    detailVisible.value = false
    if (books.value.length === 1 && filters.page > 1) filters.page -= 1
    await Promise.all([loadBooks(), loadCategories()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败')
    }
  } finally {
    deletingId.value = null
  }
}

const confirmBorrow = async (book) => {
  if (!auth.reader) {
    ElMessage.warning('仅读者账号可以自助借阅')
    return
  }
  if (auth.reader.cardStatus !== 'ACTIVE') {
    ElMessage.warning('当前借阅证状态异常，请联系图书馆管理员')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认借阅《${book.title}》？借期 ${auth.reader.borrowDays} 天，应还日期为 ${dayjs()
        .add(auth.reader.borrowDays, 'day')
        .format('YYYY年M月D日')}。`,
      '确认借阅',
      { confirmButtonText: '确认借阅', cancelButtonText: '再想想', type: 'info' },
    )
    borrowLoading.value = true
    const result = await borrowBook({ bookId: book.id })
    borrowResult.value = result
    borrowResultVisible.value = true
    detailVisible.value = false
    await loadBooks()
    await auth.fetchProfile()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '借阅失败')
    }
  } finally {
    borrowLoading.value = false
  }
}

const goMyBorrows = () => {
  borrowResultVisible.value = false
  router.push({ name: 'my-borrows' })
}

const borrowLimitLabel = computed(() => {
  const config = READER_TYPES[auth.reader?.type]
  return config ? `每位${config.label}最多可借 ${config.maxBorrow} 本` : '按读者类型限制借阅数量'
})
</script>

<template>
  <div class="app-page">
    <PageHeader
      eyebrow="Collection"
      :title="isManagement ? '图书管理' : '馆藏检索'"
      :description="
        isManagement
          ? '维护馆藏信息、馆藏数量与图书分类，掌握每一本书的去向。'
          : '在 12.8 万册馆藏中，找到下一本值得读的书。'
      "
    >
      <template #meta>
        <div class="collection-meta">
          <span><strong>{{ total }}</strong> 种图书</span>
          <i />
          <span>本页可借 <strong>{{ totalAvailableCopies }}</strong> 册</span>
          <i />
          <span>{{ borrowLimitLabel }}</span>
        </div>
      </template>
      <template #actions>
        <div class="view-switch">
          <button :class="{ 'is-active': viewMode === 'card' }" @click="viewMode = 'card'">
            <el-icon><Grid /></el-icon>
          </button>
          <button :class="{ 'is-active': viewMode === 'table' }" @click="viewMode = 'table'">
            <el-icon><Expand /></el-icon>
          </button>
        </div>
        <el-button v-if="isAdmin" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增图书
        </el-button>
        <el-button v-else plain @click="router.push({ name: 'my-borrows' })">
          <el-icon><Collection /></el-icon>
          我的借阅
        </el-button>
      </template>
    </PageHeader>

    <section class="filter-bar collection-filter">
      <el-input
        v-model="filters.keyword"
        class="search-input"
        placeholder="搜索书名、作者、ISBN 或出版社"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>

      <el-select v-model="filters.categoryId" placeholder="全部分类" clearable @change="search">
        <el-option
          v-for="category in categoryOptions"
          :key="category.value"
          :label="category.label"
          :value="category.value"
        />
      </el-select>

      <el-select v-model="filters.availability" placeholder="借阅状态" clearable @change="search">
        <el-option label="当前可借" value="AVAILABLE" />
        <el-option label="暂无库存" value="UNAVAILABLE" />
      </el-select>

      <el-select v-model="filters.sort" placeholder="排序方式" @change="search">
        <el-option label="综合排序" value="recommended" />
        <el-option label="借阅热度" value="popular" />
        <el-option label="评分优先" value="rating" />
        <el-option label="最新出版" value="newest" />
        <el-option label="书名排序" value="title" />
      </el-select>

      <el-button type="primary" plain @click="search">
        <el-icon><Search /></el-icon>
        检索
      </el-button>
      <el-button text @click="resetFilters">重置</el-button>
    </section>

    <div v-loading="loading" class="collection-content">
      <template v-if="viewMode === 'card'">
        <div v-if="books.length" class="books-grid">
          <BookCard
            v-for="book in books"
            :key="book.id"
            :book="book"
            :mode="isAdmin ? 'manage' : 'browse'"
            :borrow-disabled="!auth.reader || auth.reader.cardStatus !== 'ACTIVE'"
            @detail="openDetail"
            @borrow="confirmBorrow"
            @edit="openEdit"
            @remove="confirmRemove"
          />
        </div>
        <div v-else class="empty-state surface-card">
          <div class="empty-state__icon"><el-icon><Search /></el-icon></div>
          <h3>没有找到匹配的图书</h3>
          <p>试试更换关键词，或清除分类与库存筛选条件。</p>
          <el-button plain @click="resetFilters">重置筛选</el-button>
        </div>
      </template>

      <div v-else class="table-panel surface-card surface-card--flat">
        <el-table :data="books" row-key="id" @row-click="openDetail">
          <el-table-column label="图书" min-width="265">
            <template #default="{ row }">
              <div class="table-book">
                <div class="table-book__cover"><BookCover :book="row" size="xs" flat /></div>
                <div>
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.author }} · {{ row.publisher }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="100">
            <template #default="{ row }">
              <span class="table-category">
                <i :style="{ background: row.categoryColor }" />{{ row.categoryName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="isbn" label="ISBN" width="142" />
          <el-table-column label="馆藏" width="108">
            <template #default="{ row }">
              <div class="table-stock">
                <strong :class="{ 'is-low': row.availableCopies <= 1 }">
                  {{ row.availableCopies }}
                </strong>
                <span>/ {{ row.totalCopies }} 可借</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="location" label="馆藏位置" width="104" />
          <el-table-column v-if="isAdmin" label="操作" width="138" fixed="right">
            <template #default="{ row }">
              <el-button text size="small" @click.stop="openEdit(row)">编辑</el-button>
              <el-button text size="small" class="danger-text" @click.stop="confirmRemove(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
          <el-table-column v-else label="操作" width="94" fixed="right">
            <template #default="{ row }">
              <el-button
                text
                size="small"
                :disabled="row.availableCopies <= 0"
                @click.stop="confirmBorrow(row)"
              >
                借阅
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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

    <el-drawer
      v-model="detailVisible"
      :with-header="false"
      size="min(540px, 92vw)"
      class="book-detail-drawer"
    >
      <div v-if="selectedBook" class="book-detail">
        <div class="book-detail__bar">
          <div class="book-detail__eyebrow">馆藏详情 · COLLECTION</div>
          <button class="icon-button" @click="detailVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <div class="book-detail__hero">
          <div class="book-detail__cover">
            <BookCover :book="selectedBook" size="lg" />
          </div>
          <div class="book-detail__intro">
            <span class="pill pill--copper">{{ selectedBook.categoryName }}</span>
            <h2>{{ selectedBook.title }}</h2>
            <p class="book-detail__author">{{ selectedBook.author }}</p>
            <div class="book-detail__rating">
              <el-icon><StarFilled /></el-icon>
              <strong>{{ selectedBook.rating }}</strong>
              <span>馆藏评分</span>
              <i />
              <span>累计借阅 {{ selectedBook.activeLoans + 12 }} 次</span>
            </div>
            <div class="book-detail__stock">
              <div>
                <strong>{{ selectedBook.availableCopies }}</strong>
                <span>本可借</span>
              </div>
              <div class="book-detail__stock-bar">
                <span
                  :style="{
                    width: `${(selectedBook.availableCopies / Math.max(selectedBook.totalCopies, 1)) * 100}%`,
                  }"
                />
              </div>
              <small>馆藏 {{ selectedBook.totalCopies }} 册</small>
            </div>
          </div>
        </div>

        <div class="book-detail__meta-grid">
          <div>
            <span class="meta-label">ISBN</span>
            <span class="meta-value mono">{{ selectedBook.isbn }}</span>
          </div>
          <div>
            <span class="meta-label">出版社</span>
            <span class="meta-value">{{ selectedBook.publisher }}</span>
          </div>
          <div>
            <span class="meta-label">出版年份</span>
            <span class="meta-value">{{ selectedBook.publishYear }}</span>
          </div>
          <div>
            <span class="meta-label">馆藏位置</span>
            <span class="meta-value">{{ selectedBook.location || '待分配' }}</span>
          </div>
          <div>
            <span class="meta-label">定价</span>
            <span class="meta-value">{{ formatCurrency(selectedBook.price) }}</span>
          </div>
          <div>
            <span class="meta-label">在借副本</span>
            <span class="meta-value">{{ selectedBook.activeLoans }} 本</span>
          </div>
        </div>

        <div class="book-detail__section">
          <div class="book-detail__section-title">内容简介</div>
          <p>{{ selectedBook.description || '暂无简介。' }}</p>
        </div>

        <div class="book-detail__section">
          <div class="book-detail__section-title">借阅须知</div>
          <ul class="borrow-rules">
            <li><span>01</span>本科生借期 30 天，研究生 60 天，教师 90 天。</li>
            <li><span>02</span>每本图书可续借 1 次，逾期后无法续借。</li>
            <li><span>03</span>逾期罚款按每日 ¥0.50 计算，归还时一次性生成。</li>
          </ul>
        </div>

        <div class="book-detail__footer">
          <template v-if="isAdmin">
            <el-button @click="detailVisible = false">关闭</el-button>
            <el-button type="danger" plain :loading="deletingId === selectedBook.id" @click="confirmRemove(selectedBook)">
              删除
            </el-button>
            <el-button type="primary" @click="openEdit(selectedBook)">
              <el-icon><EditPen /></el-icon>
              编辑图书
            </el-button>
          </template>
          <template v-else>
            <div class="book-detail__hint">
              <el-icon><InfoFilled /></el-icon>
              借阅后请按期归还
            </div>
            <el-button
              type="primary"
              :disabled="selectedBook.availableCopies <= 0"
              :loading="borrowLoading"
              @click="confirmBorrow(selectedBook)"
            >
              {{ selectedBook.availableCopies > 0 ? '立即借阅' : '暂无库存' }}
            </el-button>
          </template>
        </div>
      </div>
    </el-drawer>

    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑图书' : '新增图书'"
      width="700px"
      destroy-on-close
      class="book-form-dialog"
    >
      <p class="dialog-lead">
        {{ form.id ? '修改馆藏信息后，库存数量将根据在借副本自动重新计算。' : '录入一本新书，完善的书目信息能让读者更快找到它。' }}
      </p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="书名" prop="title" class="span-2">
            <el-input v-model="form.title" placeholder="请输入图书书名" maxlength="80" />
          </el-form-item>
          <el-form-item label="作者" prop="author">
            <el-input v-model="form.author" placeholder="作者或编者" maxlength="60" />
          </el-form-item>
          <el-form-item label="ISBN" prop="isbn">
            <el-input v-model="form.isbn" placeholder="13 位 ISBN，如 9787544291170" maxlength="17" />
          </el-form-item>
          <el-form-item label="图书分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" class="w-full">
              <el-option
                v-for="category in categoryOptions"
                :key="category.value"
                :label="category.label"
                :value="category.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="出版社">
            <el-input v-model="form.publisher" placeholder="出版社名称" maxlength="60" />
          </el-form-item>
          <el-form-item label="馆藏总量" prop="totalCopies">
            <el-input-number v-model="form.totalCopies" :min="1" :max="999" class="w-full" />
          </el-form-item>
          <el-form-item label="出版年份">
            <el-input-number
              v-model="form.publishYear"
              :min="1900"
              :max="dayjs().year()"
              class="w-full"
              :controls="false"
            />
          </el-form-item>
          <el-form-item label="定价（元）">
            <el-input-number v-model="form.price" :min="0" :precision="2" class="w-full" />
          </el-form-item>
          <el-form-item label="馆藏位置">
            <el-input v-model="form.location" placeholder="如 A-01-03" maxlength="30" />
          </el-form-item>
          <el-form-item label="内容简介" class="span-2">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="用一两句话介绍这本书…"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ form.id ? '保存修改' : '确认入库' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="borrowResultVisible" width="420px" class="borrow-result-dialog">
      <div v-if="borrowResult" class="borrow-result">
        <div class="borrow-result__seal"><el-icon><Check /></el-icon></div>
        <div class="borrow-result__eyebrow">BORROWED SUCCESSFULLY</div>
        <h2>借阅成功</h2>
        <p class="borrow-result__book">《{{ borrowResult.record.bookTitle }}》</p>
        <div class="borrow-result__meta">
          <div>
            <span>借阅日期</span>
            <strong>{{ borrowResult.record.borrowDate }}</strong>
          </div>
          <div>
            <span>应还日期</span>
            <strong>{{ borrowResult.record.dueDate }}</strong>
          </div>
        </div>
        <p class="borrow-result__notice">
          请于应还日期前归还，逾期将按每日 ¥0.50 生成罚款。
        </p>
        <div class="borrow-result__actions">
          <el-button plain @click="borrowResultVisible = false">继续浏览</el-button>
          <el-button type="primary" @click="goMyBorrows">查看我的借阅</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.collection-meta {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 10px;
  color: var(--text-400);
  font-size: 9.5px;
}

.collection-meta strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
}

.collection-meta i {
  width: 1px;
  height: 9px;
  background: var(--line-strong);
}

.view-switch {
  display: flex;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(253, 251, 247, 0.65);
}

.view-switch button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 29px;
  height: 27px;
  border: 0;
  border-radius: 5px;
  color: var(--text-400);
  background: transparent;
  font-size: 13px;
  cursor: pointer;
}

.view-switch button.is-active {
  color: var(--ink-700);
  background: var(--paper-50);
  box-shadow: 0 1px 4px rgba(31, 47, 42, 0.08);
}

.collection-filter {
  gap: 10px;
}

.collection-filter .search-input {
  flex: 1;
  width: auto;
  min-width: 220px;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.collection-content {
  min-height: 280px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 330px;
  padding: 40px;
  text-align: center;
}

.empty-state__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  color: var(--text-400);
  background: var(--paper-100);
  font-size: 24px;
}

.empty-state h3 {
  margin-top: 14px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 18px;
}

.empty-state p {
  margin: 5px 0 16px;
  color: var(--text-400);
  font-size: 11px;
}

.table-panel {
  overflow: hidden;
}

.table-book {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-book__cover {
  width: 30px;
  flex-shrink: 0;
}

.table-book > div:last-child {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.table-book strong {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-book span {
  overflow: hidden;
  margin-top: 1px;
  color: var(--text-400);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-category {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-500);
  font-size: 10px;
}

.table-category i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.table-stock {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.table-stock strong {
  color: var(--ink-700);
  font-family: var(--font-mono);
  font-size: 12px;
}

.table-stock strong.is-low {
  color: var(--warning);
}

.table-stock span {
  color: var(--text-400);
  font-size: 8px;
}

.danger-text {
  color: var(--danger);
}

.book-detail {
  padding-bottom: 20px;
}

.book-detail__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}

.book-detail__eyebrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.13em;
}

.book-detail__hero {
  display: flex;
  gap: 24px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
}

.book-detail__cover {
  width: 152px;
  flex-shrink: 0;
}

.book-detail__intro {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding-top: 4px;
}

.book-detail__intro .pill {
  align-self: flex-start;
}

.book-detail__intro h2 {
  margin-top: 11px;
  color: var(--ink-900);
  font-family: var(--font-display);
  font-size: 27px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.35;
}

.book-detail__author {
  margin-top: 4px;
  color: var(--text-500);
  font-size: 11px;
}

.book-detail__rating {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 13px;
  color: var(--text-400);
  font-size: 8.5px;
}

.book-detail__rating .el-icon {
  color: var(--copper-500);
  font-size: 10px;
}

.book-detail__rating strong {
  color: var(--copper-600);
  font-family: var(--font-mono);
  font-size: 9px;
}

.book-detail__rating i {
  width: 1px;
  height: 9px;
  margin: 0 5px;
  background: var(--line);
}

.book-detail__stock {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 15px;
}

.book-detail__stock > div:first-child {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.book-detail__stock strong {
  color: var(--ink-700);
  font-family: var(--font-display);
  font-size: 23px;
  line-height: 1;
}

.book-detail__stock span {
  color: var(--text-400);
  font-size: 8px;
}

.book-detail__stock-bar {
  flex: 1;
  height: 4px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.book-detail__stock-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--jade-400), var(--ink-500));
}

.book-detail__stock small {
  color: var(--text-400);
  font-size: 8px;
  white-space: nowrap;
}

.book-detail__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 13px 24px;
  margin-top: 20px;
  padding: 17px 19px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: var(--paper-100);
}

.book-detail__meta-grid > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.book-detail__section {
  margin-top: 24px;
}

.book-detail__section-title {
  margin-bottom: 8px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
}

.book-detail__section > p {
  color: var(--text-500);
  font-size: 10.5px;
  line-height: 1.85;
}

.borrow-rules {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.borrow-rules li {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  color: var(--text-500);
  font-size: 10px;
  line-height: 1.65;
}

.borrow-rules li span {
  color: var(--copper-500);
  font-family: var(--font-mono);
  font-size: 8px;
}

.book-detail__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 30px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.book-detail__hint {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 5px;
  color: var(--text-400);
  font-size: 9px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-grid .span-2 {
  grid-column: span 2;
}

.form-grid :deep(.el-form-item) {
  margin-bottom: 16px;
}

.dialog-lead {
  margin: -4px 0 16px;
  color: var(--text-400);
  font-size: 10px;
  line-height: 1.7;
}

.borrow-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 4px 2px;
  text-align: center;
}

.borrow-result__seal {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 55px;
  height: 55px;
  margin-bottom: 13px;
  border: 1px solid rgba(46, 115, 100, 0.24);
  border-radius: 50%;
  color: var(--jade-500);
  background: var(--jade-100);
  font-size: 25px;
}

.borrow-result__eyebrow {
  color: var(--copper-600);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.15em;
}

.borrow-result h2 {
  margin-top: 6px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 24px;
}

.borrow-result__book {
  margin-top: 6px;
  color: var(--text-500);
  font-family: var(--font-display);
  font-size: 12px;
}

.borrow-result__meta {
  display: flex;
  width: 100%;
  gap: 1px;
  margin-top: 18px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: var(--line);
}

.borrow-result__meta > div {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 3px;
  padding: 11px;
  background: var(--paper-50);
}

.borrow-result__meta span {
  color: var(--text-400);
  font-size: 8px;
}

.borrow-result__meta strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 500;
}

.borrow-result__notice {
  margin-top: 12px;
  color: var(--text-400);
  font-size: 8.5px;
}

.borrow-result__actions {
  display: flex;
  width: 100%;
  gap: 9px;
  margin-top: 20px;
}

.borrow-result__actions .el-button {
  flex: 1;
}

@media (max-width: 1360px) {
  .books-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .books-grid {
    grid-template-columns: 1fr;
  }

  .collection-filter .search-input {
    flex-basis: 100%;
  }
}

@media (max-width: 680px) {
  .view-switch {
    display: none;
  }

  .collection-meta {
    flex-wrap: wrap;
  }

  .book-detail__hero {
    flex-direction: column;
  }

  .book-detail__cover {
    width: 130px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid .span-2 {
    grid-column: span 1;
  }

  .book-detail__meta-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
