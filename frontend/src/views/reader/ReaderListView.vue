<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import {
  createReader,
  getReader,
  getReaders,
  removeReader,
  updateCardStatus,
  updateReader,
} from '@/api/reader'
import {
  CARD_STATUS,
  CARD_STATUS_OPTIONS,
  READER_TYPES,
  READER_TYPE_OPTIONS,
  ROLE_LABELS,
} from '@/utils/constants'
import { formatCurrency, initials } from '@/utils/format'

const readers = ref([])
const total = ref(0)
const summary = ref({})
const loading = ref(true)
const formVisible = ref(false)
const formRef = ref()
const submitting = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedReader = ref(null)

const filters = reactive({
  keyword: '',
  type: '',
  cardStatus: '',
  page: 1,
  size: 10,
})

const emptyForm = () => ({
  id: null,
  studentNo: '',
  name: '',
  gender: '女',
  type: 'UNDERGRADUATE',
  cardStatus: 'ACTIVE',
  department: '',
  major: '',
  email: '',
  phone: '',
})

const form = reactive(emptyForm())

const rules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{6,12}$/, message: '学号应为 6–12 位数字', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  type: [{ required: true, message: '请选择读者类型', trigger: 'change' }],
  cardStatus: [{ required: true, message: '请选择借阅证状态', trigger: 'change' }],
  email: [
    {
      type: 'email',
      message: '邮箱格式不正确',
      trigger: ['blur', 'change'],
    },
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur',
    },
  ],
}

const cardStatusPill = (status) => {
  const tone = { ACTIVE: 'green', LOST: 'amber', CANCELLED: 'gray' }[status] || 'gray'
  return `pill pill--${tone}`
}

const typeColor = (type) => READER_TYPES[type]?.color || '#72827e'
const typeLabel = (type) => READER_TYPES[type]?.label || '未知'
const cardStatusLabel = (status) => CARD_STATUS[status]?.label || '未知'

const load = async () => {
  loading.value = true
  try {
    const result = await getReaders({
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      cardStatus: filters.cardStatus || undefined,
      page: filters.page,
      size: filters.size,
    })
    readers.value = result.records
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

const resetFilters = () => {
  Object.assign(filters, { keyword: '', type: '', cardStatus: '', page: 1 })
  load()
}

const openCreate = () => {
  Object.assign(form, emptyForm())
  formVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = (reader) => {
  Object.assign(form, emptyForm(), {
    id: reader.id,
    studentNo: reader.studentNo,
    name: reader.name,
    gender: reader.gender,
    type: reader.type,
    cardStatus: reader.cardStatus,
    department: reader.department,
    major: reader.major,
    email: reader.email,
    phone: reader.phone,
  })
  formVisible.value = true
  drawerVisible.value = false
  formRef.value?.clearValidate()
}

const submit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (payload.id) {
      await updateReader(payload.id, payload)
      ElMessage.success('读者信息已更新')
    } else {
      await createReader(payload)
      ElMessage.success('读者新增成功，登录初始密码为 reader123')
    }
    formVisible.value = false
    await load()
    if (drawerVisible.value && payload.id) await openDetail({ id: payload.id })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

const confirmRemove = async (reader) => {
  try {
    await ElMessageBox.confirm(
      `确认删除读者「${reader.name}」吗？若存在未归还图书，系统将阻止删除。`,
      '删除读者',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
    )
    await removeReader(reader.id)
    ElMessage.success('读者已删除')
    drawerVisible.value = false
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const openDetail = async (reader) => {
  drawerVisible.value = true
  detailLoading.value = true
  try {
    selectedReader.value = await getReader(reader.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    detailLoading.value = false
  }
}

const changeCardStatus = async (status) => {
  try {
    await updateCardStatus(selectedReader.value.id, status)
    ElMessage.success(`借阅证状态已更新为「${cardStatusLabel(status)}」`)
    await openDetail({ id: selectedReader.value.id })
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const onPageChange = (page) => {
  filters.page = page
  load()
}

const summaryCards = computed(() => [
  { label: '注册读者', value: summary.value.total || 0, suffix: '人', tone: 'jade' },
  { label: '有效借阅证', value: summary.value.active || 0, suffix: '张', tone: 'blue' },
  { label: '当前在借', value: summary.value.activeLoans || 0, suffix: '本', tone: 'copper' },
  { label: '逾期未还', value: summary.value.overdue || 0, suffix: '本', tone: 'danger' },
])
</script>

<template>
  <div class="app-page">
    <PageHeader
      eyebrow="Readers"
      title="读者管理"
      description="维护读者档案与借阅证状态，让每一位读者都能顺畅使用图书馆。"
    >
      <template #actions>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增读者
        </el-button>
      </template>
    </PageHeader>

    <div class="reader-summary">
      <div
        v-for="item in summaryCards"
        :key="item.label"
        class="reader-summary__item"
        :class="`is-${item.tone}`"
      >
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}<small>{{ item.suffix }}</small></strong>
        </div>
        <span class="reader-summary__mark" />
      </div>
    </div>

    <section class="filter-bar">
      <el-input
        v-model="filters.keyword"
        class="search-input"
        placeholder="搜索姓名、学号、院系或手机号"
        clearable
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.type" placeholder="读者类型" clearable @change="search">
        <el-option
          v-for="option in READER_TYPE_OPTIONS"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-select v-model="filters.cardStatus" placeholder="借阅证状态" clearable @change="search">
        <el-option
          v-for="option in CARD_STATUS_OPTIONS"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-button type="primary" plain @click="search">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button text @click="resetFilters">重置</el-button>
      <span class="filter-count">共 {{ total }} 位读者</span>
    </section>

    <section class="surface-card reader-table-card">
      <el-table v-loading="loading" :data="readers" row-key="id" @row-click="openDetail">
        <el-table-column label="读者" min-width="230">
          <template #default="{ row }">
            <div class="reader-cell">
              <span
                class="reader-cell__avatar"
                :style="{ background: typeColor(row.type) }"
              >
                {{ initials(row.name) }}
              </span>
              <div class="reader-cell__copy">
                <strong>{{ row.name }}</strong>
                <span>{{ row.studentNo }} · {{ row.gender }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="读者类型" width="105">
          <template #default="{ row }">
            <span class="type-badge" :style="{ '--type-color': typeColor(row.type) }">
              {{ typeLabel(row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="院系 / 专业" min-width="180">
          <template #default="{ row }">
            <div class="reader-dept">
              <span>{{ row.department || '未填写院系' }}</span>
              <small>{{ row.major || '—' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅状态" width="155">
          <template #default="{ row }">
            <div class="quota-cell">
              <div class="quota-cell__text">
                <strong>{{ row.activeBorrowCount }}</strong>
                <span>/ {{ row.maxBorrow }}</span>
                <em v-if="row.overdueCount">逾期 {{ row.overdueCount }}</em>
              </div>
              <div class="quota-cell__bar">
                <span
                  :style="{
                    width: `${Math.min((row.activeBorrowCount / row.maxBorrow) * 100, 100)}%`,
                  }"
                  :class="{ 'is-full': row.activeBorrowCount >= row.maxBorrow }"
                />
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅证" width="100">
          <template #default="{ row }">
            <span :class="cardStatusPill(row.cardStatus)">
              <span class="pill__dot" />
              {{ cardStatusLabel(row.cardStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="待缴罚款" width="100">
          <template #default="{ row }">
            <span :class="row.unpaidAmount ? 'unpaid' : 'paid'">
              {{ row.unpaidAmount ? formatCurrency(row.unpaidAmount) : '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="132" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click.stop="openDetail(row)">详情</el-button>
            <el-button text size="small" @click.stop="openEdit(row)">编辑</el-button>
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

    <el-dialog
      v-model="formVisible"
      :title="form.id ? '编辑读者' : '新增读者'"
      width="660px"
      destroy-on-close
    >
      <p class="dialog-lead">
        {{ form.id ? '修改读者档案信息，学号作为登录账号不可变更。' : '新增读者后将自动创建登录账号，初始密码为 reader123。' }}
      </p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="学号" prop="studentNo">
            <el-input
              v-model="form.studentNo"
              :disabled="Boolean(form.id)"
              placeholder="6–12 位数字学号"
              maxlength="12"
            />
          </el-form-item>
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="读者真实姓名" maxlength="20" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="form.gender" class="w-full">
              <el-option label="女" value="女" />
              <el-option label="男" value="男" />
              <el-option label="未设置" value="未设置" />
            </el-select>
          </el-form-item>
          <el-form-item label="读者类型" prop="type">
            <el-select v-model="form.type" class="w-full">
              <el-option
                v-for="option in READER_TYPE_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="所属院系">
            <el-input v-model="form.department" placeholder="如：计算机与电子信息学院" maxlength="40" />
          </el-form-item>
          <el-form-item label="专业 / 年级">
            <el-input v-model="form.major" placeholder="如：软件工程 2301" maxlength="40" />
          </el-form-item>
          <el-form-item label="联系邮箱" prop="email">
            <el-input v-model="form.email" placeholder="name@campus.edu.cn" maxlength="60" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="借阅证状态" prop="cardStatus">
            <el-select v-model="form.cardStatus" class="w-full">
              <el-option
                v-for="option in CARD_STATUS_OPTIONS"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <div class="type-preview">
            <span
              class="type-preview__icon"
              :style="{ background: typeColor(form.type) }"
            >
              {{ initials(form.name || '新') }}
            </span>
            <div>
              <strong>{{ form.name || '新读者' }}</strong>
              <span>
                {{ typeLabel(form.type) }} · 可借
                {{ READER_TYPES[form.type]?.maxBorrow }} 本 /
                {{ READER_TYPES[form.type]?.borrowDays }} 天
              </span>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ form.id ? '保存修改' : '创建读者' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" :with-header="false" size="min(520px, 92vw)">
      <div v-loading="detailLoading" class="reader-detail">
        <template v-if="selectedReader">
          <div class="reader-detail__bar">
            <div class="reader-detail__eyebrow">读者档案 · READER PROFILE</div>
            <button class="icon-button" @click="drawerVisible = false">
              <el-icon><Close /></el-icon>
            </button>
          </div>

          <div class="library-card">
            <div class="library-card__head">
              <div>
                <span class="library-card__brand">知墨图书馆</span>
                <span class="library-card__en">ZHIMO UNIVERSITY LIBRARY</span>
              </div>
              <span class="library-card__seal">借阅证</span>
            </div>
            <div class="library-card__body">
              <span class="library-card__avatar" :style="{ background: typeColor(selectedReader.type) }">
                {{ initials(selectedReader.name) }}
              </span>
              <div class="library-card__name">
                <strong>{{ selectedReader.name }}</strong>
                <span>{{ selectedReader.studentNo }} · {{ typeLabel(selectedReader.type) }}</span>
              </div>
              <span class="library-card__status" :class="{ 'is-invalid': selectedReader.cardStatus !== 'ACTIVE' }">
                {{ cardStatusLabel(selectedReader.cardStatus) }}
              </span>
            </div>
            <div class="library-card__bottom">
              <span>{{ selectedReader.cardNo }}</span>
              <span>VALID THRU {{ selectedReader.cardExpiresAt?.slice(0, 7) }}</span>
            </div>
            <span class="library-card__arc" />
          </div>

          <div class="reader-detail__info">
            <div>
              <span class="meta-label">院系</span>
              <span class="meta-value">{{ selectedReader.department || '—' }}</span>
            </div>
            <div>
              <span class="meta-label">专业 / 年级</span>
              <span class="meta-value">{{ selectedReader.major || '—' }}</span>
            </div>
            <div>
              <span class="meta-label">联系电话</span>
              <span class="meta-value">{{ selectedReader.phone || '—' }}</span>
            </div>
            <div>
              <span class="meta-label">邮箱</span>
              <span class="meta-value">{{ selectedReader.email || '—' }}</span>
            </div>
            <div>
              <span class="meta-label">借阅额度</span>
              <span class="meta-value">
                {{ selectedReader.activeBorrowCount }} / {{ selectedReader.maxBorrow }} 本
              </span>
            </div>
            <div>
              <span class="meta-label">借阅证有效期</span>
              <span class="meta-value">{{ selectedReader.cardExpiresAt }}</span>
            </div>
          </div>

          <div class="reader-detail__section">
            <div class="reader-detail__section-head">
              <h3>当前在借</h3>
              <span>{{ selectedReader.activeBorrows?.length || 0 }} 本</span>
            </div>
            <div v-if="selectedReader.activeBorrows?.length" class="detail-loans">
              <div
                v-for="loan in selectedReader.activeBorrows"
                :key="loan.id"
                class="detail-loan"
              >
                <span class="detail-loan__dot" :class="{ 'is-late': loan.status === 'OVERDUE' }" />
                <div>
                  <strong>《{{ loan.bookTitle }}》</strong>
                  <span>借于 {{ loan.borrowDate }} · 应还 {{ loan.dueDate }}</span>
                </div>
                <span
                  class="pill"
                  :class="loan.status === 'OVERDUE' ? 'pill--red' : 'pill--green'"
                >
                  {{ loan.status === 'OVERDUE' ? `逾期 ${loan.overdueDays} 天` : '借阅中' }}
                </span>
              </div>
            </div>
            <p v-else class="detail-empty">当前没有在借图书</p>
          </div>

          <div class="reader-detail__section">
            <div class="reader-detail__section-head">
              <h3>罚款记录</h3>
              <span>{{ formatCurrency(selectedReader.unpaidAmount) }} 待缴</span>
            </div>
            <div v-if="selectedReader.penalties?.length" class="detail-fines">
              <div v-for="fine in selectedReader.penalties.slice(0, 4)" :key="fine.id" class="detail-fine">
                <span class="detail-fine__icon">
                  <el-icon><Wallet /></el-icon>
                </span>
                <div>
                  <strong>《{{ fine.bookTitle }}》逾期 {{ fine.overdueDays }} 天</strong>
                  <span>{{ fine.createdAt?.slice(0, 10) }}</span>
                </div>
                <div class="detail-fine__amount">
                  <strong>{{ formatCurrency(fine.amount) }}</strong>
                  <span :class="fine.status === 'PAID' ? 'is-paid' : 'is-unpaid'">
                    {{ fine.status === 'PAID' ? '已缴纳' : '待缴纳' }}
                  </span>
                </div>
              </div>
            </div>
            <p v-else class="detail-empty">没有罚款记录</p>
          </div>

          <div class="reader-detail__footer">
            <el-dropdown trigger="click" @command="changeCardStatus">
              <el-button plain>
                <el-icon><CreditCard /></el-icon>
                借阅证状态
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="status in ['ACTIVE', 'LOST', 'CANCELLED']"
                    :key="status"
                    :command="status"
                  >
                    {{ cardStatusLabel(status) }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="danger" plain @click="confirmRemove(selectedReader)">删除</el-button>
            <el-button type="primary" @click="openEdit(selectedReader)">编辑档案</el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.reader-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.reader-summary__item {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  min-height: 84px;
  padding: 15px 17px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.84);
}

.reader-summary__item > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reader-summary__item span {
  color: var(--text-500);
  font-size: 9px;
}

.reader-summary__item strong {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 25px;
  line-height: 1;
}

.reader-summary__item small {
  margin-left: 3px;
  color: var(--text-400);
  font-family: var(--font-sans);
  font-size: 8px;
  font-weight: 400;
}

.reader-summary__item.is-danger strong {
  color: var(--danger);
}

.reader-summary__mark {
  width: 30px;
  height: 30px;
  border: 1px solid var(--line);
  border-radius: 50%;
  opacity: 0.7;
}

.reader-summary__item.is-jade .reader-summary__mark {
  border-color: rgba(46, 115, 100, 0.18);
  background: var(--jade-100);
}

.reader-summary__item.is-blue .reader-summary__mark {
  border-color: rgba(80, 122, 142, 0.18);
  background: var(--info-bg);
}

.reader-summary__item.is-copper .reader-summary__mark {
  border-color: rgba(181, 121, 75, 0.18);
  background: var(--copper-100);
}

.reader-summary__item.is-danger .reader-summary__mark {
  border-color: rgba(181, 91, 82, 0.18);
  background: var(--danger-bg);
}

.filter-count {
  margin-left: auto;
  color: var(--text-400);
  font-size: 9px;
}

.reader-table-card {
  padding: 7px 16px 16px;
  overflow: hidden;
}

.reader-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reader-cell__avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 35px;
  height: 35px;
  border-radius: 50%;
  color: #fff9ef;
  font-family: var(--font-display);
  font-size: 11px;
  font-weight: 600;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.15);
}

.reader-cell__copy {
  display: flex;
  flex-direction: column;
}

.reader-cell__copy strong {
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 650;
}

.reader-cell__copy span {
  color: var(--text-400);
  font-size: 8.5px;
}

.type-badge {
  display: inline-flex;
  padding: 2px 8px;
  border: 1px solid color-mix(in srgb, var(--type-color) 22%, transparent);
  border-radius: 999px;
  color: var(--type-color);
  background: color-mix(in srgb, var(--type-color) 7%, transparent);
  font-size: 8.5px;
  font-weight: 600;
}

.reader-dept {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.reader-dept span {
  color: var(--text-700);
  font-size: 10px;
}

.reader-dept small {
  color: var(--text-400);
  font-size: 8.5px;
}

.quota-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quota-cell__text {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.quota-cell__text strong {
  color: var(--ink-700);
  font-family: var(--font-mono);
  font-size: 12px;
}

.quota-cell__text span {
  color: var(--text-400);
  font-size: 8px;
}

.quota-cell__text em {
  margin-left: 4px;
  padding: 0 4px;
  border-radius: 3px;
  color: var(--danger);
  background: var(--danger-bg);
  font-size: 7px;
  font-style: normal;
}

.quota-cell__bar {
  width: 76px;
  height: 3px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.quota-cell__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--jade-500);
}

.quota-cell__bar span.is-full {
  background: var(--danger);
}

.unpaid {
  color: var(--danger);
  font-family: var(--font-mono);
  font-size: 10px;
}

.paid {
  color: var(--text-400);
  font-size: 10px;
}

.dialog-lead {
  margin: -4px 0 16px;
  color: var(--text-400);
  font-size: 10px;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 15px;
}

.form-grid :deep(.el-form-item) {
  margin-bottom: 15px;
}

.type-preview {
  display: flex;
  align-items: center;
  gap: 9px;
  grid-column: span 2;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: var(--paper-100);
}

.type-preview__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  border-radius: 50%;
  color: #fff;
  font-family: var(--font-display);
  font-size: 10px;
}

.type-preview > div {
  display: flex;
  flex-direction: column;
}

.type-preview strong {
  color: var(--text-700);
  font-size: 10px;
}

.type-preview span {
  color: var(--text-400);
  font-size: 8.5px;
}

.reader-detail {
  min-height: 300px;
  padding-bottom: 16px;
}

.reader-detail__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.reader-detail__eyebrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.12em;
}

.library-card {
  position: relative;
  padding: 18px 20px 15px;
  overflow: hidden;
  border-radius: 14px;
  color: #f4e9da;
  background:
    radial-gradient(circle at 88% 0%, rgba(201, 146, 100, 0.22), transparent 40%),
    linear-gradient(135deg, #1b4841, #0d2c2e);
  box-shadow: 0 15px 30px rgba(18, 48, 43, 0.16);
}

.library-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.library-card__head > div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.library-card__brand {
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.1em;
}

.library-card__en {
  color: rgba(205, 220, 211, 0.38);
  font-family: var(--font-mono);
  font-size: 6px;
  letter-spacing: 0.15em;
}

.library-card__seal {
  padding: 4px;
  border: 1px solid rgba(216, 161, 111, 0.45);
  color: #d8a16f;
  font-family: var(--font-display);
  font-size: 7px;
  letter-spacing: 0.08em;
  writing-mode: vertical-rl;
}

.library-card__body {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 19px;
}

.library-card__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 37px;
  height: 37px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  color: #fff8ee;
  font-family: var(--font-display);
  font-size: 11px;
}

.library-card__name {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.library-card__name strong {
  font-family: var(--font-display);
  font-size: 19px;
  letter-spacing: 0.08em;
}

.library-card__name span {
  color: rgba(204, 219, 210, 0.48);
  font-size: 8px;
}

.library-card__status {
  padding: 2px 7px;
  border: 1px solid rgba(155, 208, 185, 0.24);
  border-radius: 3px;
  color: #a7d4bd;
  font-family: var(--font-mono);
  font-size: 6.5px;
  letter-spacing: 0.08em;
}

.library-card__status.is-invalid {
  color: #e5a69c;
  border-color: rgba(229, 166, 156, 0.24);
}

.library-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  color: rgba(204, 219, 210, 0.36);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.08em;
}

.library-card__arc {
  position: absolute;
  right: -25px;
  bottom: -50px;
  width: 130px;
  height: 130px;
  border: 1px solid rgba(222, 188, 151, 0.1);
  border-radius: 50%;
}

.library-card__arc::after {
  position: absolute;
  inset: 17px;
  border: 1px solid rgba(222, 188, 151, 0.08);
  border-radius: 50%;
  content: '';
}

.reader-detail__info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 13px 20px;
  margin-top: 20px;
  padding: 16px 17px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--paper-100);
}

.reader-detail__info > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.reader-detail__section {
  margin-top: 22px;
}

.reader-detail__section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 9px;
}

.reader-detail__section-head h3 {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
}

.reader-detail__section-head span {
  color: var(--text-400);
  font-size: 8.5px;
}

.detail-loans,
.detail-fines {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-loan {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--paper-50);
}

.detail-loan__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--jade-500);
}

.detail-loan__dot.is-late {
  background: var(--danger);
}

.detail-loan > div {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.detail-loan strong {
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 10.5px;
  font-weight: 600;
}

.detail-loan > div span {
  color: var(--text-400);
  font-size: 8px;
}

.detail-loan .pill {
  font-size: 7px;
}

.detail-empty {
  padding: 14px;
  border: 1px dashed var(--line);
  border-radius: 8px;
  color: var(--text-400);
  font-size: 9px;
  text-align: center;
}

.detail-fine {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.detail-fine__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  color: var(--warning);
  background: var(--warning-bg);
  font-size: 12px;
}

.detail-fine > div:nth-child(2) {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.detail-fine > div:nth-child(2) strong {
  color: var(--text-700);
  font-size: 9.5px;
  font-weight: 550;
}

.detail-fine > div:nth-child(2) span {
  color: var(--text-400);
  font-size: 8px;
}

.detail-fine__amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}

.detail-fine__amount strong {
  color: var(--danger);
  font-family: var(--font-mono);
  font-size: 10px;
}

.detail-fine__amount span {
  font-size: 7.5px;
}

.detail-fine__amount .is-paid {
  color: var(--success);
}

.detail-fine__amount .is-unpaid {
  color: var(--warning);
}

.reader-detail__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 26px;
  padding-top: 17px;
  border-top: 1px solid var(--line);
}

@media (max-width: 1000px) {
  .reader-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-count {
    display: none;
  }
}

@media (max-width: 620px) {
  .reader-summary {
    gap: 8px;
  }

  .reader-summary__item {
    min-height: 74px;
    padding: 12px;
  }

  .reader-summary__item strong {
    font-size: 21px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .type-preview {
    grid-column: span 1;
  }

  .reader-detail__info {
    grid-template-columns: 1fr;
  }
}
</style>
