<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { getMyPenalties, payPenalty } from '@/api/penalty'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { FINE_RATE } from '@/utils/constants'
import { formatCurrency, formatDateTime } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const appStore = useAppStore()

const penalties = ref([])
const total = ref(0)
const summary = ref({})
const loading = ref(true)
const payingId = ref(null)
const payResultVisible = ref(false)
const paidItem = ref(null)

const filters = reactive({
  status: 'ALL',
  page: 1,
  size: 6,
})

const tabs = computed(() => [
  { value: 'ALL', label: '全部记录' },
  { value: 'UNPAID', label: '待缴纳' },
  { value: 'PAID', label: '已缴纳' },
])

const load = async () => {
  loading.value = true
  try {
    const result = await getMyPenalties({
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

const switchTab = (value) => {
  filters.status = value
  filters.page = 1
  load()
}

const confirmPay = async (penalty) => {
  try {
    await ElMessageBox.confirm(
      `确认缴纳《${penalty.bookTitle}》的逾期罚款 ${formatCurrency(penalty.amount)}？`,
      '缴纳罚款',
      { confirmButtonText: '确认缴纳', cancelButtonText: '稍后处理', type: 'info' },
    )
    payingId.value = penalty.id
    const result = await payPenalty(penalty.id)
    paidItem.value = result
    payResultVisible.value = true
    await load()
    await auth.fetchProfile()
    appStore.fetchNotifications()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '缴纳失败')
    }
  } finally {
    payingId.value = null
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
      eyebrow="Fines"
      title="我的罚款"
      description="查看逾期罚款明细，及时缴纳以保持借阅证正常使用。"
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

    <section class="fine-hero" :class="{ 'is-clear': !summary.pendingCount }">
      <div class="fine-hero__copy">
        <div class="fine-hero__eyebrow">OUTSTANDING FINES</div>
        <div class="fine-hero__amount">
          <small>¥</small>
          {{ Number(summary.pendingAmount || 0).toFixed(2) }}
        </div>
        <p>
          <template v-if="summary.pendingCount">
            共有 {{ summary.pendingCount }} 笔待缴罚款，请在归还后及时处理。
          </template>
          <template v-else>当前没有待缴罚款，你的借阅信用状态良好。</template>
        </p>
      </div>

      <div class="fine-hero__stats">
        <div>
          <span>本月已缴</span>
          <strong>{{ formatCurrency(summary.paidThisMonth) }}</strong>
        </div>
        <div>
          <span>历史缴纳</span>
          <strong>{{ formatCurrency(summary.collectedTotal) }}</strong>
        </div>
        <div>
          <span>每日费率</span>
          <strong>¥{{ FINE_RATE.toFixed(2) }}<small>/天</small></strong>
        </div>
      </div>

      <div class="fine-hero__seal">
        <el-icon v-if="!summary.pendingCount"><CircleCheckFilled /></el-icon>
        <el-icon v-else><Wallet /></el-icon>
      </div>
      <span class="fine-hero__arc" />
    </section>

    <div class="fine-layout">
      <section class="surface-card fine-list-card">
        <div class="fine-tabs">
          <div class="fine-tabs__list">
            <button
              v-for="tab in tabs"
              :key="tab.value"
              :class="{ 'is-active': filters.status === tab.value }"
              @click="switchTab(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
          <span>共 {{ total }} 笔罚款记录</span>
        </div>

        <div v-loading="loading" class="fine-list">
          <article
            v-for="penalty in penalties"
            :key="penalty.id"
            class="fine-item"
            :class="{ 'is-paid': penalty.status === 'PAID' }"
          >
            <div class="fine-item__status">
              <span class="fine-item__icon">
                <el-icon>
                  <CircleCheck v-if="penalty.status === 'PAID'" />
                  <WarningFilled v-else />
                </el-icon>
              </span>
              <span
                class="pill"
                :class="penalty.status === 'PAID' ? 'pill--green' : 'pill--amber'"
              >
                {{ penalty.status === 'PAID' ? '已缴纳' : '待缴纳' }}
              </span>
            </div>

            <div class="fine-item__main">
              <h3>《{{ penalty.bookTitle }}》</h3>
              <p>逾期 {{ penalty.overdueDays }} 天 · 每日 ¥{{ FINE_RATE.toFixed(2) }}</p>
              <div class="fine-item__meta">
                <span>借阅 {{ penalty.borrowDate }}</span>
                <i />
                <span>应还 {{ penalty.dueDate }}</span>
                <i />
                <span>归还 {{ penalty.returnDate }}</span>
              </div>
            </div>

            <div class="fine-item__amount">
              <span>罚款金额</span>
              <strong>{{ formatCurrency(penalty.amount) }}</strong>
              <small v-if="penalty.status === 'PAID'">{{ formatDateTime(penalty.paidAt) }}</small>
            </div>

            <div class="fine-item__action">
              <el-button
                v-if="penalty.status === 'UNPAID'"
                type="primary"
                size="small"
                :loading="payingId === penalty.id"
                @click="confirmPay(penalty)"
              >
                缴纳
              </el-button>
              <span v-else class="fine-item__done">
                <el-icon><Check /></el-icon>
                已完成
              </span>
            </div>
          </article>

          <div v-if="!penalties.length && !loading" class="fine-empty">
            <span><el-icon><Wallet /></el-icon></span>
            <h3>{{ filters.status === 'UNPAID' ? '没有待缴罚款' : '暂无罚款记录' }}</h3>
            <p>保持按时归还，就能始终保持良好的借阅信用。</p>
          </div>
        </div>

        <el-pagination
          v-if="total > filters.size"
          background
          layout="prev, pager, next"
          :current-page="filters.page"
          :page-size="filters.size"
          :total="total"
          @current-change="onPageChange"
        />
      </section>

      <aside class="fine-side">
        <section class="rule-card">
          <div class="rule-card__head">
            <span class="rule-card__icon"><el-icon><Reading /></el-icon></span>
            <div>
              <h3>罚款规则</h3>
              <p>FINE POLICY</p>
            </div>
          </div>
          <div class="rule-formula">
            <span>逾期天数</span>
            <i>×</i>
            <span>每日费率</span>
            <i>=</i>
            <strong>罚款金额</strong>
          </div>
          <ul>
            <li>
              <span>01</span>
              逾期罚款按每日 ¥{{ FINE_RATE.toFixed(2) }} 计算，归还时一次性生成。
            </li>
            <li>
              <span>02</span>
              罚款未缴清不影响本次还书，但建议及时缴纳以保持良好信用。
            </li>
            <li>
              <span>03</span>
              如对罚款金额有疑问，请携带借阅证前往一层流通服务台核对。
            </li>
          </ul>
        </section>

        <section class="service-card">
          <span class="service-card__seal">知墨</span>
          <h3>需要帮助？</h3>
          <p>流通服务台位于主馆一层大厅东侧。</p>
          <div class="service-card__hours">
            <span>服务时间</span>
            <strong>08:00 — 21:00</strong>
          </div>
          <div class="service-card__contact">
            <el-icon><Phone /></el-icon>
            021-6488 2100
          </div>
        </section>
      </aside>
    </div>

    <el-dialog v-model="payResultVisible" width="390px">
      <div v-if="paidItem" class="pay-result">
        <div class="pay-result__icon"><el-icon><Check /></el-icon></div>
        <div class="pay-result__eyebrow">PAYMENT COMPLETED</div>
        <h2>缴纳成功</h2>
        <p>《{{ paidItem.bookTitle }}》的逾期罚款已结清</p>
        <div class="pay-result__amount">{{ formatCurrency(paidItem.amount) }}</div>
        <el-button type="primary" class="w-full" @click="payResultVisible = false">完成</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.fine-hero {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 154px;
  padding: 25px 32px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f5ebde;
  background:
    radial-gradient(circle at 82% 18%, rgba(201, 146, 100, 0.2), transparent 32%),
    linear-gradient(120deg, #1b443f, #0e2d2f);
}

.fine-hero.is-clear {
  background:
    radial-gradient(circle at 82% 18%, rgba(118, 168, 148, 0.17), transparent 32%),
    linear-gradient(120deg, #19463e, #0d302e);
}

.fine-hero__eyebrow {
  color: rgba(206, 220, 211, 0.38);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.14em;
}

.fine-hero__amount {
  margin-top: 5px;
  color: #f4e5d2;
  font-family: var(--font-display);
  font-size: 45px;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.1;
}

.fine-hero.is-clear .fine-hero__amount {
  color: #cfe3d7;
}

.fine-hero__amount small {
  margin-right: 4px;
  color: #d8a16f;
  font-family: var(--font-sans);
  font-size: 16px;
  font-weight: 400;
}

.fine-hero__copy p {
  margin-top: 5px;
  color: rgba(207, 223, 213, 0.47);
  font-size: 9px;
}

.fine-hero__stats {
  position: relative;
  z-index: 2;
  display: flex;
  gap: clamp(26px, 4vw, 52px);
  margin-left: auto;
  padding-right: 78px;
}

.fine-hero__stats > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.fine-hero__stats span {
  color: rgba(195, 212, 202, 0.38);
  font-size: 8px;
}

.fine-hero__stats strong {
  color: #f1e3d0;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
}

.fine-hero__stats small {
  margin-left: 1px;
  color: rgba(195, 212, 202, 0.35);
  font-family: var(--font-sans);
  font-size: 7px;
  font-weight: 400;
}

.fine-hero__seal {
  position: absolute;
  right: 28px;
  bottom: 23px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 47px;
  height: 47px;
  border: 1px solid rgba(214, 167, 121, 0.28);
  border-radius: 50%;
  color: rgba(214, 167, 121, 0.6);
  font-size: 20px;
}

.fine-hero__arc {
  position: absolute;
  top: -95px;
  right: -60px;
  width: 290px;
  height: 290px;
  border: 1px solid rgba(225, 195, 163, 0.08);
  border-radius: 50%;
}

.fine-hero__arc::after {
  position: absolute;
  inset: 38px;
  border: 1px solid rgba(225, 195, 163, 0.055);
  border-radius: 50%;
  content: '';
}

.fine-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.65fr);
  gap: 16px;
}

.fine-list-card {
  padding: 20px 21px 16px;
}

.fine-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.fine-tabs__list {
  display: flex;
  gap: 3px;
}

.fine-tabs__list button {
  padding: 6px 11px;
  border: 0;
  border-radius: 6px;
  color: var(--text-500);
  background: transparent;
  font-size: 9.5px;
  cursor: pointer;
}

.fine-tabs__list button.is-active {
  color: var(--ink-700);
  background: var(--jade-100);
  font-weight: 600;
}

.fine-tabs > span {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
}

.fine-list {
  display: flex;
  flex-direction: column;
  min-height: 250px;
}

.fine-item {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--line);
}

.fine-item:last-child {
  border-bottom: 0;
}

.fine-item.is-paid {
  opacity: 0.74;
}

.fine-item__status {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  width: 66px;
  flex-shrink: 0;
}

.fine-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  border-radius: 7px;
  color: var(--warning);
  background: var(--warning-bg);
  font-size: 12px;
}

.is-paid .fine-item__icon {
  color: var(--success);
  background: var(--success-bg);
}

.fine-item__status .pill {
  padding: 2px 6px;
  font-size: 7px;
}

.fine-item__main {
  flex: 1;
  min-width: 0;
}

.fine-item__main h3 {
  overflow: hidden;
  color: var(--text-700);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fine-item__main p {
  margin-top: 1px;
  color: var(--text-400);
  font-size: 8px;
}

.fine-item__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
}

.fine-item__meta i {
  width: 1px;
  height: 7px;
  background: var(--line-strong);
}

.fine-item__amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
  flex-shrink: 0;
  min-width: 76px;
}

.fine-item__amount span {
  color: var(--text-400);
  font-size: 7px;
}

.fine-item__amount strong {
  color: var(--danger);
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
}

.is-paid .fine-item__amount strong {
  color: var(--text-500);
}

.fine-item__amount small {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 6.5px;
}

.fine-item__action {
  width: 54px;
  flex-shrink: 0;
  text-align: right;
}

.fine-item__action .el-button {
  height: 26px;
  padding: 0 11px;
  font-size: 8.5px;
}

.fine-item__done {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--success);
  font-size: 7.5px;
}

.fine-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 46px 15px;
  text-align: center;
}

.fine-empty > span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  color: var(--jade-500);
  background: var(--jade-100);
  font-size: 20px;
}

.fine-empty h3 {
  margin-top: 10px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 16px;
}

.fine-empty p {
  margin-top: 3px;
  color: var(--text-400);
  font-size: 9px;
}

.fine-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rule-card {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.86);
}

.rule-card__head {
  display: flex;
  align-items: center;
  gap: 9px;
}

.rule-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 31px;
  height: 31px;
  border-radius: 8px;
  color: var(--copper-600);
  background: var(--copper-100);
  font-size: 14px;
}

.rule-card__head h3 {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
}

.rule-card__head p {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 6px;
  letter-spacing: 0.1em;
}

.rule-formula {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-top: 16px;
  padding: 10px 11px;
  border: 1px dashed var(--line-strong);
  border-radius: 7px;
  background: var(--paper-100);
}

.rule-formula span {
  color: var(--text-500);
  font-size: 7.5px;
}

.rule-formula i {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 8px;
  font-style: normal;
}

.rule-formula strong {
  color: var(--copper-600);
  font-size: 7.5px;
  font-weight: 600;
}

.rule-card ul {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin-top: 15px;
}

.rule-card li {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  color: var(--text-500);
  font-size: 8.5px;
  line-height: 1.7;
}

.rule-card li span {
  flex-shrink: 0;
  color: var(--copper-500);
  font-family: var(--font-mono);
  font-size: 7px;
}

.service-card {
  position: relative;
  padding: 19px 20px;
  overflow: hidden;
  border-radius: var(--radius-md);
  color: #f3e8d9;
  background:
    radial-gradient(circle at 92% 0%, rgba(201, 146, 100, 0.2), transparent 40%),
    linear-gradient(140deg, #1c4841, #0f2f2f);
}

.service-card__seal {
  position: absolute;
  top: 17px;
  right: 17px;
  width: 25px;
  height: 25px;
  padding: 4px;
  border: 1px solid rgba(216, 161, 111, 0.45);
  color: #d8a16f;
  font-family: var(--font-display);
  font-size: 7px;
  line-height: 1.25;
  text-align: center;
  writing-mode: vertical-rl;
}

.service-card h3 {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
}

.service-card > p {
  margin-top: 4px;
  color: rgba(203, 219, 209, 0.45);
  font-size: 8px;
}

.service-card__hours {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 14px;
  padding-top: 11px;
  border-top: 1px solid rgba(219, 234, 224, 0.1);
}

.service-card__hours span {
  color: rgba(196, 213, 203, 0.36);
  font-size: 7px;
}

.service-card__hours strong {
  color: #e8d4bd;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
}

.service-card__contact {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 9px;
  color: #d4a97d;
  font-family: var(--font-mono);
  font-size: 8px;
}

.pay-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 5px 2px;
  text-align: center;
}

.pay-result__icon {
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

.pay-result__eyebrow {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.pay-result h2 {
  margin-top: 5px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 22px;
}

.pay-result > p {
  margin-top: 4px;
  color: var(--text-500);
  font-size: 9px;
}

.pay-result__amount {
  margin: 14px 0 16px;
  color: var(--success);
  font-family: var(--font-display);
  font-size: 29px;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .fine-layout {
    grid-template-columns: 1fr;
  }

  .fine-side {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 800px) {
  .fine-hero {
    align-items: flex-start;
    flex-direction: column;
    gap: 17px;
  }

  .fine-hero__stats {
    margin-left: 0;
    padding-right: 0;
  }

  .fine-hero__seal {
    display: none;
  }

  .fine-item__meta {
    display: none;
  }
}

@media (max-width: 620px) {
  .fine-side {
    grid-template-columns: 1fr;
  }

  .fine-hero {
    padding: 20px;
  }

  .fine-hero__stats {
    flex-wrap: wrap;
    gap: 20px;
  }

  .fine-hero__amount {
    font-size: 37px;
  }

  .fine-tabs > span {
    display: none;
  }

  .fine-item {
    gap: 8px;
  }

  .fine-item__status {
    width: auto;
  }

  .fine-item__main p {
    display: none;
  }

  .fine-item__amount {
    min-width: 60px;
  }

  .fine-item__action {
    width: auto;
  }
}
</style>
