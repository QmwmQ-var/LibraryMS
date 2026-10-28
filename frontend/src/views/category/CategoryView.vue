<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import {
  createCategory,
  getCategories,
  removeCategory,
  updateCategory,
} from '@/api/book'

const categories = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()

const palette = ['#2e7364', '#9b6238', '#4d6b80', '#756083', '#8a6b3b', '#995c51', '#55745b', '#876d4b']

const form = reactive({
  id: null,
  name: '',
  code: '',
  color: '#2e7364',
  description: '',
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入分类代码', trigger: 'blur' },
    {
      pattern: /^[A-Za-z]{1,6}$/,
      message: '分类代码为 1–6 位英文字母',
      trigger: 'blur',
    },
  ],
}

const totalBooks = computed(() => categories.value.reduce((sum, item) => sum + item.bookCount, 0))
const totalBorrows = computed(() =>
  categories.value.reduce((sum, item) => sum + item.borrowCount, 0),
)
const maxBorrowCount = computed(() =>
  Math.max(...categories.value.map((item) => item.borrowCount), 1),
)

const load = async () => {
  loading.value = true
  try {
    categories.value = await getCategories()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    code: '',
    color: palette[categories.value.length % palette.length],
    description: '',
  })
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const openEdit = (category) => {
  Object.assign(form, {
    id: category.id,
    name: category.name,
    code: category.code,
    color: category.color,
    description: category.description,
  })
  dialogVisible.value = true
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
    if (payload.id) await updateCategory(payload.id, payload)
    else await createCategory(payload)
    ElMessage.success(payload.id ? '分类已更新' : '分类创建成功')
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

const confirmRemove = async (category) => {
  try {
    await ElMessageBox.confirm(
      `确认删除「${category.name}」分类吗？分类下仍有图书时无法删除。`,
      '删除分类',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
    await removeCategory(category.id)
    ElMessage.success('分类已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}
</script>

<template>
  <div v-loading="loading" class="app-page">
    <PageHeader
      eyebrow="Taxonomy"
      title="分类管理"
      description="用清晰的分类结构组织馆藏，让读者从熟悉的领域开始探索。"
    >
      <template #actions>
        <div class="category-summary">
          <span><strong>{{ categories.length }}</strong> 个分类</span>
          <i />
          <span><strong>{{ totalBooks }}</strong> 种图书</span>
        </div>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
      </template>
    </PageHeader>

    <section class="category-banner">
      <div class="category-banner__copy">
        <div class="eyebrow">Knowledge Map</div>
        <h2>知识的版图，从分类开始。</h2>
        <p>八大分类覆盖人文、科技与社会科学，让每一本书都有清晰的归属。</p>
      </div>
      <div class="category-banner__stats">
        <div>
          <strong>{{ totalBooks }}</strong>
          <span>馆藏品种</span>
        </div>
        <div>
          <strong>{{ totalBorrows }}</strong>
          <span>累计借阅</span>
        </div>
        <div>
          <strong>{{ categories.length }}</strong>
          <span>学科门类</span>
        </div>
      </div>
      <span class="category-banner__stamp">CATALOG<br />SYSTEM</span>
      <span class="category-banner__arc" />
    </section>

    <div class="category-grid">
      <article
        v-for="(category, index) in categories"
        :key="category.id"
        class="category-card"
        :style="{ '--accent': category.color }"
      >
        <div class="category-card__top">
          <span class="category-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="category-card__code">{{ category.code }}</span>
          <el-dropdown trigger="click">
            <button class="category-card__more">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openEdit(category)">
                  <el-icon><EditPen /></el-icon> 编辑分类
                </el-dropdown-item>
                <el-dropdown-item divided @click="confirmRemove(category)">
                  <el-icon><Delete /></el-icon> 删除分类
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="category-card__icon">
          <span>{{ category.name.slice(0, 1) }}</span>
        </div>

        <h3>{{ category.name }}</h3>
        <p class="category-card__desc">{{ category.description || '暂无分类描述' }}</p>

        <div class="category-card__metrics">
          <div>
            <strong>{{ category.bookCount }}</strong>
            <span>种图书</span>
          </div>
          <div>
            <strong>{{ category.copyCount }}</strong>
            <span>册馆藏</span>
          </div>
          <div>
            <strong>{{ category.borrowCount }}</strong>
            <span>次借阅</span>
          </div>
        </div>

        <div class="category-card__progress">
          <span
            :style="{ width: `${Math.max((category.borrowCount / maxBorrowCount) * 100, 3)}%` }"
          />
        </div>
        <div class="category-card__foot">
          <span>借阅热度</span>
          <span>{{ totalBorrows ? Math.round((category.borrowCount / totalBorrows) * 100) : 0 }}%</span>
        </div>
      </article>

      <button class="category-add" @click="openCreate">
        <span class="category-add__icon"><el-icon><Plus /></el-icon></span>
        <strong>新增分类</strong>
        <p>为新的知识领域建立归属</p>
      </button>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑分类' : '新增分类'"
      width="520px"
      destroy-on-close
    >
      <p class="dialog-lead">合理的分类名称与代码，能帮助馆员快速整理和检索馆藏。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-row">
          <el-form-item label="分类名称" prop="name">
            <el-input v-model="form.name" placeholder="如：计算机" maxlength="20" />
          </el-form-item>
          <el-form-item label="分类代码" prop="code">
            <el-input v-model="form.code" placeholder="如：TP" maxlength="6" />
          </el-form-item>
        </div>
        <el-form-item label="主题色">
          <div class="color-picker">
            <button
              v-for="color in palette"
              :key="color"
              type="button"
              class="color-swatch"
              :class="{ 'is-active': form.color === color }"
              :style="{ background: color }"
              @click="form.color = color"
            >
              <el-icon v-if="form.color === color"><Check /></el-icon>
            </button>
          </div>
        </el-form-item>
        <el-form-item label="分类描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="100"
            show-word-limit
            placeholder="一句话介绍这个分类的馆藏范围"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          {{ form.id ? '保存修改' : '创建分类' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-summary {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 5px;
  color: var(--text-400);
  font-size: 9px;
}

.category-summary strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
}

.category-summary i {
  width: 1px;
  height: 9px;
  background: var(--line-strong);
}

.category-banner {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 138px;
  padding: 25px 30px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f5ebde;
  background:
    radial-gradient(circle at 82% 22%, rgba(201, 146, 100, 0.19), transparent 32%),
    linear-gradient(120deg, #18463f, #0e3030);
}

.category-banner__copy {
  position: relative;
  z-index: 2;
}

.category-banner__copy .eyebrow {
  margin-bottom: 5px;
  color: #d6a779;
}

.category-banner__copy .eyebrow::before {
  background: #c7915d;
}

.category-banner__copy h2 {
  color: #f8efe2;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.category-banner__copy p {
  margin-top: 6px;
  color: rgba(215, 228, 219, 0.48);
  font-size: 10px;
}

.category-banner__stats {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 40px;
  margin-left: auto;
  padding-right: 92px;
}

.category-banner__stats > div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.category-banner__stats strong {
  color: #f2e4d1;
  font-family: var(--font-display);
  font-size: 25px;
  line-height: 1.2;
}

.category-banner__stats span {
  color: rgba(197, 215, 204, 0.4);
  font-size: 8px;
}

.category-banner__stamp {
  position: absolute;
  top: 20px;
  right: 23px;
  padding: 6px;
  border: 1px solid rgba(214, 167, 121, 0.32);
  color: rgba(214, 167, 121, 0.55);
  font-family: var(--font-mono);
  font-size: 6px;
  letter-spacing: 0.12em;
  line-height: 1.7;
  text-align: center;
}

.category-banner__arc {
  position: absolute;
  right: -50px;
  bottom: -130px;
  width: 280px;
  height: 280px;
  border: 1px solid rgba(228, 201, 170, 0.08);
  border-radius: 50%;
}

.category-banner__arc::after {
  position: absolute;
  inset: 38px;
  border: 1px solid rgba(228, 201, 170, 0.06);
  border-radius: 50%;
  content: '';
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.category-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 255px;
  padding: 18px 19px 15px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--accent) 4%, transparent), transparent 42%),
    rgba(253, 251, 247, 0.88);
  box-shadow: var(--shadow-xs);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.category-card::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--accent), transparent);
  content: '';
}

.category-card:hover {
  border-color: color-mix(in srgb, var(--accent) 25%, var(--line));
  box-shadow: 0 16px 34px rgba(35, 48, 42, 0.08);
  transform: translateY(-3px);
}

.category-card__top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-card__index {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.category-card__code {
  padding: 1px 5px;
  border: 1px solid color-mix(in srgb, var(--accent) 25%, var(--line));
  border-radius: 3px;
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 7px;
  font-weight: 600;
}

.category-card__more {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-left: auto;
  border: 0;
  border-radius: 5px;
  color: var(--text-400);
  background: transparent;
  cursor: pointer;
}

.category-card__more:hover {
  color: var(--text-700);
  background: var(--paper-100);
}

.category-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  margin-top: 15px;
  border-radius: 10px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 9%, transparent);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.category-card h3 {
  margin-top: 10px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
}

.category-card__desc {
  overflow: hidden;
  margin-top: 3px;
  color: var(--text-400);
  font-size: 9px;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-card__metrics {
  display: flex;
  gap: 22px;
  margin-top: 14px;
  padding-top: 13px;
  border-top: 1px dashed var(--line);
}

.category-card__metrics > div {
  display: flex;
  flex-direction: column;
}

.category-card__metrics strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 550;
}

.category-card__metrics span {
  color: var(--text-400);
  font-size: 7.5px;
}

.category-card__progress {
  height: 3px;
  margin-top: auto;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.category-card__progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent);
}

.category-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 5px;
  color: var(--text-400);
  font-size: 7.5px;
}

.category-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 255px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-md);
  color: var(--text-400);
  background: rgba(253, 251, 247, 0.35);
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease;
}

.category-add:hover {
  border-color: var(--jade-400);
  color: var(--jade-500);
  background: rgba(229, 240, 235, 0.35);
}

.category-add__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid currentColor;
  border-radius: 50%;
  font-size: 17px;
  opacity: 0.7;
}

.category-add strong {
  margin-top: 11px;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
}

.category-add p {
  margin-top: 3px;
  font-size: 8px;
  opacity: 0.7;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.dialog-lead {
  margin: -4px 0 16px;
  color: var(--text-400);
  font-size: 10px;
  line-height: 1.7;
}

.color-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.color-swatch {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 2px solid transparent;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  cursor: pointer;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.04);
}

.color-swatch.is-active {
  border-color: var(--paper-50);
  box-shadow: 0 0 0 2px currentColor;
}

@media (max-width: 1180px) {
  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .category-banner {
    align-items: flex-start;
    flex-direction: column;
    gap: 20px;
  }

  .category-banner__stats {
    margin-left: 0;
    padding-right: 0;
  }

  .category-banner__stamp {
    display: none;
  }
}

@media (max-width: 620px) {
  .category-grid {
    grid-template-columns: 1fr;
  }

  .category-summary {
    display: none;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
