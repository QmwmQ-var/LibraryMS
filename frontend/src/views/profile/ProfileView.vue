<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { getOverview } from '@/api/stats'
import { READER_TYPES, ROLE_LABELS } from '@/utils/constants'
import { formatCurrency, initials } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const appStore = useAppStore()
const passwordVisible = ref(false)
const passwordRef = ref()
const passwordSubmitting = ref(false)
const adminOverview = ref({})
const preferences = reactive({
  dueReminder: localStorage.getItem('libraryms_pref_due') !== 'false',
  fineReminder: localStorage.getItem('libraryms_pref_fine') !== 'false',
  weeklyDigest: localStorage.getItem('libraryms_pref_digest') === 'true',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const reader = computed(() => auth.user?.reader || null)
const isAdmin = computed(() => auth.isAdmin)
const readerConfig = computed(() => (reader.value ? READER_TYPES[reader.value.type] : null))

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) callback(new Error('两次输入的新密码不一致'))
  else callback()
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6–32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

const stats = computed(() => {
  if (!reader.value) {
    return [
      { label: '今日借出', value: adminOverview.value.todayBorrows || 0, suffix: '本' },
      { label: '当前在借', value: adminOverview.value.activeLoans || 0, suffix: '本' },
      { label: '注册读者', value: adminOverview.value.readerCount || 0, suffix: '人' },
      { label: '待缴罚款', value: formatCurrency(adminOverview.value.pendingFineAmount), suffix: '' },
    ]
  }
  return [
    { label: '累计阅读', value: reader.value.totalBorrowCount || 0, suffix: '本' },
    { label: '当前在借', value: reader.value.activeBorrowCount || 0, suffix: '本' },
    { label: '可用额度', value: reader.value.availableQuota || 0, suffix: '本' },
    { label: '待缴罚款', value: formatCurrency(reader.value.unpaidAmount), suffix: '' },
  ]
})

onMounted(async () => {
  try {
    await auth.fetchProfile()
    if (isAdmin.value) adminOverview.value = await getOverview()
  } catch (error) {
    ElMessage.error(error.message)
  }
})

const openPassword = () => {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  passwordVisible.value = true
  passwordRef.value?.clearValidate()
}

const submitPassword = async () => {
  try {
    await passwordRef.value.validate()
  } catch {
    return
  }
  passwordSubmitting.value = true
  try {
    await auth.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请妥善保管新密码')
    passwordVisible.value = false
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    passwordSubmitting.value = false
  }
}

const savePreference = (key, value) => {
  preferences[key] = value
  const map = {
    dueReminder: 'libraryms_pref_due',
    fineReminder: 'libraryms_pref_fine',
    weeklyDigest: 'libraryms_pref_digest',
  }
  localStorage.setItem(map[key], String(value))
  ElMessage.success('通知偏好已保存')
}

const logout = async () => {
  await auth.logout()
  router.replace({ name: 'login' })
}

const cardNumber = computed(() => {
  if (reader.value) return reader.value.cardNo
  return `STAFF-${String(auth.user?.id || 1).padStart(4, '0')}`
})
</script>

<template>
  <div class="app-page">
    <PageHeader
      eyebrow="Account"
      title="个人中心"
      description="管理你的账号信息、阅读数据与安全设置。"
    >
      <template #actions>
        <el-button plain @click="openPassword">
          <el-icon><Lock /></el-icon>
          修改密码
        </el-button>
        <el-button type="primary" @click="router.push({ name: isAdmin ? 'circulation' : 'books' })">
          <el-icon><Reading /></el-icon>
          {{ isAdmin ? '进入工作台' : '继续阅读' }}
        </el-button>
      </template>
    </PageHeader>

    <div class="profile-layout">
      <div class="profile-left">
        <section class="profile-hero">
          <div class="profile-hero__grain" />
          <div class="profile-hero__top">
            <span>PROFILE</span>
            <span class="profile-hero__role">{{ ROLE_LABELS[auth.role] }}</span>
          </div>
          <div class="profile-hero__avatar" :style="{ background: auth.user?.avatarColor }">
            {{ initials(auth.displayName) }}
          </div>
          <h2>{{ auth.displayName }}</h2>
          <p>{{ auth.user?.title || (isAdmin ? '图书馆馆员' : '在校读者') }}</p>
          <div class="profile-hero__account">@{{ auth.user?.username }}</div>
          <div class="profile-hero__tags">
            <span v-if="reader">{{ readerConfig?.label }}</span>
            <span v-if="reader" :class="reader.cardStatus === 'ACTIVE' ? 'is-valid' : 'is-warning'">
              {{ reader.cardStatus === 'ACTIVE' ? '借阅证正常' : '借阅证异常' }}
            </span>
            <span v-if="isAdmin">馆藏管理权限</span>
          </div>
          <span class="profile-hero__arc" />
        </section>

        <section class="library-card" :class="{ 'is-staff': isAdmin }">
          <div class="library-card__head">
            <div>
              <span class="library-card__brand">知墨图书馆</span>
              <span class="library-card__en">ZHIMO UNIVERSITY LIBRARY</span>
            </div>
            <span class="library-card__seal">{{ isAdmin ? '馆员' : '借阅证' }}</span>
          </div>
          <div class="library-card__body">
            <span class="library-card__chip" />
            <div class="library-card__name">
              <strong>{{ auth.displayName }}</strong>
              <span>{{ cardNumber }}</span>
            </div>
          </div>
          <div class="library-card__bottom">
            <span>{{ reader ? reader.studentNo : auth.user?.username }}</span>
            <span>
              {{ reader ? `VALID THRU ${reader.cardExpiresAt?.slice(0, 7)}` : 'STAFF ACCESS' }}
            </span>
          </div>
          <span class="library-card__arc" />
        </section>

        <section class="contact-panel surface-card">
          <div class="contact-panel__head">
            <div class="eyebrow">Contact</div>
            <h3>联系方式</h3>
          </div>
          <div class="contact-row">
            <span><el-icon><Message /></el-icon></span>
            <div>
              <small>邮箱</small>
              <strong>{{ auth.user?.email || '未填写' }}</strong>
            </div>
          </div>
          <div class="contact-row">
            <span><el-icon><Phone /></el-icon></span>
            <div>
              <small>手机</small>
              <strong>{{ auth.user?.phone || '未填写' }}</strong>
            </div>
          </div>
          <div class="contact-row">
            <span><el-icon><OfficeBuilding /></el-icon></span>
            <div>
              <small>{{ isAdmin ? '所属部门' : '院系' }}</small>
              <strong>{{ reader?.department || (isAdmin ? '流通服务部' : '未填写') }}</strong>
            </div>
          </div>
          <p class="contact-note">
            如需修改姓名或联系方式，请携带有效证件前往流通服务台。
          </p>
        </section>
      </div>

      <div class="profile-right">
        <section class="surface-card stats-panel">
          <div class="panel-head">
            <div>
              <div class="eyebrow">{{ isAdmin ? 'Work Summary' : 'Reading Summary' }}</div>
              <h3>{{ isAdmin ? '工作概览' : '阅读概览' }}</h3>
            </div>
            <span>{{ dayjs().format('YYYY年M月D日') }}</span>
          </div>
          <div class="profile-stats">
            <div v-for="item in stats" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}<small>{{ item.suffix }}</small></strong>
            </div>
          </div>
          <div v-if="reader" class="reading-info">
            <div>
              <span>借阅额度</span>
              <strong>{{ reader.activeBorrowCount }} / {{ reader.maxBorrow }} 本</strong>
            </div>
            <div class="reading-info__bar">
              <span
                :style="{
                  width: `${Math.min((reader.activeBorrowCount / reader.maxBorrow) * 100, 100)}%`,
                }"
              />
            </div>
            <div>
              <span>借期</span>
              <strong>{{ readerConfig?.borrowDays }} 天</strong>
            </div>
          </div>
        </section>

        <section class="surface-card identity-panel">
          <div class="panel-head">
            <div>
              <div class="eyebrow">Identity</div>
              <h3>账号与身份</h3>
            </div>
            <span class="verified"><el-icon><CircleCheckFilled /></el-icon> 已认证</span>
          </div>
          <div class="identity-grid">
            <div>
              <span>登录账号</span>
              <strong>{{ auth.user?.username }}</strong>
            </div>
            <div>
              <span>角色</span>
              <strong>{{ ROLE_LABELS[auth.role] }}</strong>
            </div>
            <div>
              <span>{{ isAdmin ? '馆员编号' : '学号' }}</span>
              <strong>{{ reader?.studentNo || cardNumber }}</strong>
            </div>
            <div>
              <span>{{ isAdmin ? '所属部门' : '专业 / 年级' }}</span>
              <strong>{{ reader?.major || (isAdmin ? '流通服务部' : '—') }}</strong>
            </div>
            <div v-if="reader">
              <span>读者类型</span>
              <strong>{{ readerConfig?.label }} · {{ readerConfig?.maxBorrow }} 本 / {{ readerConfig?.borrowDays }} 天</strong>
            </div>
            <div v-if="reader">
              <span>借阅证有效期</span>
              <strong>{{ reader.cardExpiresAt }}</strong>
            </div>
            <div v-if="isAdmin">
              <span>最近登录 IP</span>
              <strong class="mono">127.0.0.1</strong>
            </div>
            <div>
              <span>账号状态</span>
              <strong class="text-success">正常使用</strong>
            </div>
          </div>
        </section>

        <div class="profile-row">
          <section class="surface-card security-panel">
            <div class="panel-head">
              <div>
                <div class="eyebrow">Security</div>
                <h3>安全设置</h3>
              </div>
            </div>
            <button class="setting-row" @click="openPassword">
              <span class="setting-row__icon"><el-icon><Lock /></el-icon></span>
              <div>
                <strong>登录密码</strong>
                <span>建议定期更换密码，长度不少于 6 位</span>
              </div>
              <el-icon class="setting-row__arrow"><ArrowRight /></el-icon>
            </button>
            <div class="setting-row is-static">
              <span class="setting-row__icon"><el-icon><Monitor /></el-icon></span>
              <div>
                <strong>当前登录设备</strong>
                <span>Windows · Chrome · 127.0.0.1</span>
              </div>
              <span class="setting-row__badge">当前设备</span>
            </div>
            <button class="setting-row setting-row--danger" @click="logout">
              <span class="setting-row__icon"><el-icon><SwitchButton /></el-icon></span>
              <div>
                <strong>退出当前账号</strong>
                <span>退出后需要重新验证身份</span>
              </div>
              <el-icon class="setting-row__arrow"><ArrowRight /></el-icon>
            </button>
          </section>

          <section class="surface-card preference-panel">
            <div class="panel-head">
              <div>
                <div class="eyebrow">Preferences</div>
                <h3>通知偏好</h3>
              </div>
              <span class="reserved-tag">界面预留</span>
            </div>
            <div class="pref-row">
              <div>
                <strong>到期提醒</strong>
                <span>图书到期前 3 天发送站内提醒</span>
              </div>
              <el-switch
                :model-value="preferences.dueReminder"
                @change="savePreference('dueReminder', $event)"
              />
            </div>
            <div class="pref-row">
              <div>
                <strong>罚款提醒</strong>
                <span>产生新罚款时即时通知</span>
              </div>
              <el-switch
                :model-value="preferences.fineReminder"
                @change="savePreference('fineReminder', $event)"
              />
            </div>
            <div class="pref-row">
              <div>
                <strong>每周阅读摘要</strong>
                <span>每周一发送借阅与推荐摘要</span>
              </div>
              <el-switch
                :model-value="preferences.weeklyDigest"
                @change="savePreference('weeklyDigest', $event)"
              />
            </div>
            <p class="pref-note">
              通知偏好在本地保存。邮件/短信通道为后期扩展接口，当前演示环境仅保存设置。
            </p>
          </section>
        </div>
      </div>
    </div>

    <el-dialog v-model="passwordVisible" width="440px" title="修改登录密码" destroy-on-close>
      <p class="dialog-lead">为了账号安全，新密码长度需为 6–32 位，且不能与原密码相同。</p>
      <el-form
        ref="passwordRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="6–32 位新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="submitPassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.65fr) minmax(0, 1.35fr);
  gap: 16px;
  align-items: start;
}

.profile-left,
.profile-right {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.profile-hero {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 23px 22px 26px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  color: #f4e9da;
  background:
    radial-gradient(circle at 85% 5%, rgba(201, 146, 100, 0.21), transparent 33%),
    linear-gradient(145deg, #1a4740, #0d2d2e);
  text-align: center;
}

.profile-hero__grain {
  position: absolute;
  inset: 0;
  opacity: 0.12;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.profile-hero__top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  color: rgba(204, 220, 210, 0.36);
  font-family: var(--font-mono);
  font-size: 7px;
  letter-spacing: 0.13em;
}

.profile-hero__role {
  padding: 2px 6px;
  border: 1px solid rgba(216, 161, 111, 0.26);
  border-radius: 3px;
  color: rgba(224, 178, 131, 0.7);
  font-family: var(--font-sans);
  font-size: 7px;
  letter-spacing: 0.04em;
}

.profile-hero__avatar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 74px;
  height: 74px;
  margin-top: 15px;
  border: 2px solid rgba(248, 237, 223, 0.24);
  border-radius: 50%;
  color: #fff9ee;
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 600;
  box-shadow: 0 0 0 6px rgba(255, 255, 255, 0.04);
}

.profile-hero h2 {
  position: relative;
  z-index: 1;
  margin-top: 12px;
  color: #f6ebdc;
  font-family: var(--font-display);
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.profile-hero > p {
  position: relative;
  z-index: 1;
  margin-top: 3px;
  color: rgba(204, 220, 210, 0.45);
  font-size: 8.5px;
}

.profile-hero__account {
  position: relative;
  z-index: 1;
  margin-top: 6px;
  color: #d4a87e;
  font-family: var(--font-mono);
  font-size: 8px;
}

.profile-hero__tags {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
  margin-top: 13px;
}

.profile-hero__tags span {
  padding: 2px 6px;
  border: 1px solid rgba(203, 220, 209, 0.11);
  border-radius: 999px;
  color: rgba(206, 222, 212, 0.5);
  font-size: 7px;
}

.profile-hero__tags span.is-valid {
  color: #aed7c3;
  border-color: rgba(126, 190, 159, 0.2);
}

.profile-hero__tags span.is-warning {
  color: #e6b0a7;
  border-color: rgba(230, 176, 167, 0.2);
}

.profile-hero__arc {
  position: absolute;
  right: -60px;
  bottom: -95px;
  width: 230px;
  height: 230px;
  border: 1px solid rgba(225, 195, 163, 0.09);
  border-radius: 50%;
}

.profile-hero__arc::after {
  position: absolute;
  inset: 30px;
  border: 1px solid rgba(225, 195, 163, 0.06);
  border-radius: 50%;
  content: '';
}

.library-card {
  position: relative;
  padding: 15px 17px 13px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 13px;
  background:
    radial-gradient(circle at 91% 3%, rgba(201, 146, 100, 0.12), transparent 38%),
    linear-gradient(140deg, #f5ebdc, #e9ddcb);
}

.library-card.is-staff {
  background:
    radial-gradient(circle at 91% 3%, rgba(201, 146, 100, 0.18), transparent 38%),
    linear-gradient(140deg, #ece0cd, #e1d0b8);
}

.library-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.library-card__head > div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.library-card__brand {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 0.1em;
}

.library-card__en {
  color: #9b8d78;
  font-family: var(--font-mono);
  font-size: 5.5px;
  letter-spacing: 0.14em;
}

.library-card__seal {
  padding: 3px;
  border: 1px solid rgba(141, 99, 65, 0.36);
  color: #8d6341;
  font-family: var(--font-display);
  font-size: 6px;
  letter-spacing: 0.06em;
  writing-mode: vertical-rl;
}

.library-card__body {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 13px;
}

.library-card__chip {
  width: 25px;
  height: 19px;
  border: 1px solid rgba(141, 99, 65, 0.24);
  border-radius: 4px;
  background: linear-gradient(135deg, #d7b987, #b89667);
  box-shadow: inset 0 0 0 2px rgba(244, 235, 220, 0.2);
}

.library-card__name {
  display: flex;
  flex-direction: column;
}

.library-card__name strong {
  color: #34443f;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 0.06em;
}

.library-card__name span {
  color: #8c7b64;
  font-family: var(--font-mono);
  font-size: 6.5px;
}

.library-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  color: #96856b;
  font-family: var(--font-mono);
  font-size: 6px;
  letter-spacing: 0.06em;
}

.library-card__arc {
  position: absolute;
  right: -28px;
  bottom: -42px;
  width: 105px;
  height: 105px;
  border: 1px solid rgba(141, 99, 65, 0.1);
  border-radius: 50%;
}

.contact-panel {
  padding: 18px 19px;
}

.contact-panel__head {
  margin-bottom: 11px;
}

.contact-panel__head .eyebrow {
  margin-bottom: 3px;
}

.contact-panel__head h3 {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 15px;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 0;
  border-bottom: 1px solid rgba(232, 223, 210, 0.55);
}

.contact-row > span {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  height: 25px;
  border-radius: 6px;
  color: var(--text-400);
  background: var(--paper-100);
  font-size: 11px;
}

.contact-row > div {
  display: flex;
  flex-direction: column;
}

.contact-row small {
  color: var(--text-400);
  font-size: 7px;
}

.contact-row strong {
  color: var(--text-600);
  font-size: 9px;
  font-weight: 500;
}

.contact-note {
  margin-top: 10px;
  color: var(--text-400);
  font-size: 7.5px;
  line-height: 1.6;
}

.stats-panel,
.identity-panel,
.security-panel,
.preference-panel {
  padding: 19px 21px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head .eyebrow {
  margin-bottom: 3px;
}

.panel-head h3 {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 17px;
}

.panel-head > span {
  color: var(--text-400);
  font-family: var(--font-mono);
  font-size: 7.5px;
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--line);
}

.profile-stats > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 13px;
  background: var(--paper-50);
}

.profile-stats span {
  color: var(--text-400);
  font-size: 8px;
}

.profile-stats strong {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
}

.profile-stats small {
  margin-left: 2px;
  color: var(--text-400);
  font-family: var(--font-sans);
  font-size: 7px;
  font-weight: 400;
}

.reading-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 13px;
}

.reading-info > div:first-child,
.reading-info > div:last-child {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex-shrink: 0;
}

.reading-info span {
  color: var(--text-400);
  font-size: 7px;
}

.reading-info strong {
  color: var(--text-700);
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 500;
}

.reading-info__bar {
  flex: 1;
  height: 4px;
  overflow: hidden;
  border-radius: 4px;
  background: var(--paper-200);
}

.reading-info__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--jade-400), var(--ink-500));
}

.verified {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--success) !important;
  font-family: var(--font-sans) !important;
  font-size: 8px !important;
}

.identity-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 13px 20px;
}

.identity-grid > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.identity-grid span {
  color: var(--text-400);
  font-size: 8px;
}

.identity-grid strong {
  color: var(--text-700);
  font-size: 9.5px;
  font-weight: 550;
}

.profile-row {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 14px;
}

.setting-row {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 9px 3px;
  border: 0;
  border-bottom: 1px solid rgba(232, 223, 210, 0.55);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.setting-row:last-child {
  border-bottom: 0;
}

.setting-row.is-static {
  cursor: default;
}

.setting-row__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  border-radius: 7px;
  color: var(--text-500);
  background: var(--paper-100);
  font-size: 12px;
}

.setting-row > div {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.setting-row strong {
  color: var(--text-700);
  font-size: 9px;
  font-weight: 600;
}

.setting-row > div span {
  color: var(--text-400);
  font-size: 7.5px;
}

.setting-row__arrow {
  color: var(--line-strong);
  font-size: 9px;
}

.setting-row__badge {
  color: var(--success);
  font-size: 7.5px;
}

.setting-row--danger:hover .setting-row__icon {
  color: var(--danger);
  background: var(--danger-bg);
}

.setting-row--danger:hover strong {
  color: var(--danger);
}

.reserved-tag {
  padding: 2px 5px;
  border: 1px solid var(--line);
  border-radius: 3px;
  color: var(--text-400) !important;
  font-family: var(--font-sans) !important;
  font-size: 6.5px !important;
}

.pref-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(232, 223, 210, 0.55);
}

.pref-row > div {
  display: flex;
  flex-direction: column;
}

.pref-row strong {
  color: var(--text-700);
  font-size: 9px;
  font-weight: 600;
}

.pref-row span {
  color: var(--text-400);
  font-size: 7.5px;
}

.pref-note {
  margin-top: 9px;
  color: var(--text-400);
  font-size: 7px;
  line-height: 1.6;
}

.dialog-lead {
  margin: -4px 0 16px;
  color: var(--text-400);
  font-size: 9.5px;
  line-height: 1.7;
}

@media (max-width: 1120px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .profile-left {
    display: grid;
    grid-template-columns: 1fr 1fr;
    align-items: start;
  }

  .profile-hero {
    grid-row: span 2;
  }
}

@media (max-width: 820px) {
  .profile-left {
    display: flex;
  }

  .profile-row {
    grid-template-columns: 1fr;
  }

  .identity-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .profile-stats {
    grid-template-columns: 1fr 1fr;
  }

  .profile-stats > div {
    padding: 10px;
  }

  .identity-grid {
    grid-template-columns: 1fr;
  }

  .stats-panel,
  .identity-panel,
  .security-panel,
  .preference-panel {
    padding: 16px;
  }
}
</style>
