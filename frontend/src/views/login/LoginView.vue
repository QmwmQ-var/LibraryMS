<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref()
const remember = ref(true)
const activeDemo = ref('ADMIN')
const errorMessage = ref('')
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const demoAccounts = [
  {
    role: 'ADMIN',
    label: '图书管理员',
    name: '顾清和 · 流通服务部',
    username: 'admin',
    password: 'admin123',
    icon: 'Tools',
    hint: '馆藏、读者、流通与统计',
  },
  {
    role: 'READER',
    label: '在校读者',
    name: '林知夏 · 软件工程',
    username: '2023001',
    password: 'reader123',
    icon: 'Reading',
    hint: '检索、借阅、续借与罚款',
  },
]

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

onMounted(() => {
  const saved = localStorage.getItem('libraryms_remembered_account')
  if (saved) {
    form.username = saved
    form.password = saved === 'admin' ? 'admin123' : 'reader123'
    activeDemo.value = saved === 'admin' ? 'ADMIN' : 'READER'
  }
})

const fillDemo = (account) => {
  activeDemo.value = account.role
  form.username = account.username
  form.password = account.password
  errorMessage.value = ''
  formRef.value?.clearValidate()
}

const submit = async () => {
  errorMessage.value = ''
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const result = await auth.login({ ...form })
    if (remember.value) {
      localStorage.setItem('libraryms_remembered_account', form.username)
    } else {
      localStorage.removeItem('libraryms_remembered_account')
    }
    ElMessage.success(`欢迎回来，${result.user.name}`)
    const redirect = String(route.query.redirect || '')
    if (redirect.startsWith('/') && !redirect.startsWith('/login')) {
      router.replace(redirect)
    } else {
      router.replace({ name: 'dashboard' })
    }
  } catch (error) {
    errorMessage.value = error.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const resetDemo = async () => {
  try {
    await ElMessageBox.confirm(
      '将清除本地演示数据并恢复到初始状态，你新增或修改的内容会丢失。',
      '重置演示数据',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    Object.keys(localStorage)
      .filter((key) => key.startsWith('libraryms_'))
      .forEach((key) => localStorage.removeItem(key))
    window.location.reload()
  } catch {
    // 用户取消
  }
}

const forgotPassword = () => {
  ElMessage.info('演示环境可直接使用下方账号登录；正式环境请联系图书馆管理员重置密码。')
}
</script>

<template>
  <div class="login-page">
    <section class="login-hero">
      <div class="hero-noise" />
      <div class="hero-arch" />
      <div class="hero-rings">
        <span />
        <span />
        <span />
      </div>

      <div class="hero-top">
        <div class="hero-brand">
          <div class="hero-brand__seal">知</div>
          <div class="hero-brand__copy">
            <strong>知墨图书馆</strong>
            <span>ZHIMO UNIVERSITY LIBRARY</span>
          </div>
        </div>
        <span class="hero-top__edition">EST. 1952 · LIBRARYMS</span>
      </div>

      <div class="hero-body">
        <div class="hero-kicker">
          <span class="hero-kicker__line" />
          高校图书馆 · 智慧流通平台
        </div>
        <h1 class="hero-title">
          让每一本书，<br />
          找到它的<span>下一位读者</span>。
        </h1>
        <p class="hero-description">
          从馆藏检索到借阅归还，从读者服务到数据洞察。
          <br />
          用更从容的方式，连接知识与人。
        </p>

        <div class="hero-quote">
          <span class="hero-quote__mark">“</span>
          <p>图书馆是大学的心脏，而每一次借阅，都是知识与人的一次相遇。</p>
          <span class="hero-quote__source">—— 知墨图书馆 · 馆员手记</span>
        </div>
      </div>

      <div class="hero-art" aria-hidden="true">
        <div class="floating-card">
          <div class="floating-card__icon"><el-icon><Reading /></el-icon></div>
          <div>
            <span>本月新上架</span>
            <strong>126 <small>种</small></strong>
          </div>
        </div>

        <div class="book-stack">
          <div class="stack-book stack-book--back">
            <span>A</span>
            <i>THE ART OF READING</i>
          </div>
          <div class="stack-book stack-book--middle">
            <span>墨</span>
            <i>ZHIMO LIBRARY</i>
          </div>
          <div class="stack-book stack-book--front">
            <div class="stack-book__label">馆藏</div>
            <strong>阅读<br />是一座<br />随身携带的<br />避难所</strong>
            <span class="stack-book__seal">知墨</span>
          </div>
          <div class="stack-shadow" />
        </div>
      </div>

      <div class="hero-footer">
        <div class="hero-stat">
          <strong>12.8<span>万</span></strong>
          <p>馆藏纸质图书</p>
        </div>
        <div class="hero-stat">
          <strong>26,400</strong>
          <p>在校注册读者</p>
        </div>
        <div class="hero-stat">
          <strong>08:00<span>—</span>22:30</strong>
          <p>主馆开放时间</p>
        </div>
        <span class="hero-footer__version">v1.0 · 2026</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="panel-grain" />

      <div class="login-box">
        <div class="mobile-brand">
          <div class="hero-brand__seal">知</div>
          <div class="hero-brand__copy">
            <strong>知墨图书馆</strong>
            <span>ZHIMO LIBRARY</span>
          </div>
        </div>

        <div class="login-heading">
          <div class="login-heading__eyebrow">
            <span />
            WELCOME BACK
          </div>
          <h2>欢迎回来</h2>
          <p>登录你的图书馆账号，继续今天的阅读与管理工作。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="login-form"
          @submit.prevent="submit"
        >
          <el-form-item label="登录账号" prop="username">
            <el-input
              v-model.trim="form.username"
              placeholder="管理员账号 / 读者学号"
              autocomplete="username"
              @keyup.enter="submit"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入登录密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="submit"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <button type="button" class="text-link" @click="forgotPassword">忘记密码？</button>
          </div>

          <transition name="fade">
            <div v-if="errorMessage" class="login-error">
              <el-icon><WarningFilled /></el-icon>
              <span>{{ errorMessage }}</span>
            </div>
          </transition>

          <el-button
            type="primary"
            class="login-submit"
            :loading="loading"
            native-type="submit"
            @click="submit"
          >
            <span v-if="!loading">进入图书馆</span>
            <span v-else>正在验证身份…</span>
            <el-icon v-if="!loading" class="login-submit__arrow"><Right /></el-icon>
          </el-button>
        </el-form>

        <div class="demo-section">
          <div class="demo-section__head">
            <span>快速体验</span>
            <i />
            <small>点击卡片自动填入演示账号</small>
          </div>
          <div class="demo-grid">
            <button
              v-for="account in demoAccounts"
              :key="account.role"
              class="demo-card"
              :class="{ 'is-active': activeDemo === account.role }"
              @click="fillDemo(account)"
            >
              <div class="demo-card__top">
                <span class="demo-card__icon">
                  <el-icon><component :is="account.icon" /></el-icon>
                </span>
                <span class="demo-card__role">{{ account.label }}</span>
                <el-icon v-if="activeDemo === account.role" class="demo-card__check">
                  <CircleCheckFilled />
                </el-icon>
              </div>
              <strong>{{ account.name }}</strong>
              <p>{{ account.hint }}</p>
              <div class="demo-card__account">
                <span>{{ account.username }}</span>
                <i>/</i>
                <span>{{ account.password }}</span>
              </div>
            </button>
          </div>
        </div>

        <div class="login-note">
          <el-icon><Lock /></el-icon>
          <span>密码采用加密存储 · 登录行为将记录时间与 IP</span>
        </div>

        <div class="login-panel__footer">
          <span>© 2026 知墨图书馆</span>
          <button @click="resetDemo">重置演示数据</button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(460px, 0.92fr);
  min-height: 100vh;
  background: var(--paper-100);
}

.login-hero {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 38px 56px 32px;
  overflow: hidden;
  color: #f5ebde;
  background:
    radial-gradient(circle at 78% 24%, rgba(201, 146, 100, 0.16), transparent 32%),
    radial-gradient(circle at 16% 84%, rgba(84, 143, 126, 0.24), transparent 38%),
    linear-gradient(148deg, #17453f 0%, #0f3030 46%, #0b2729 100%);
}

.hero-noise {
  position: absolute;
  inset: 0;
  opacity: 0.14;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 220 220' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.35'/%3E%3C/svg%3E");
  pointer-events: none;
}

.hero-arch {
  position: absolute;
  top: 14%;
  right: -8%;
  width: 68%;
  aspect-ratio: 0.72;
  border: 1px solid rgba(224, 202, 173, 0.12);
  border-radius: 50% 50% 0 0 / 42% 42% 0 0;
  pointer-events: none;
}

.hero-arch::before,
.hero-arch::after {
  position: absolute;
  inset: 8%;
  border: 1px solid rgba(224, 202, 173, 0.08);
  border-radius: inherit;
  content: '';
}

.hero-arch::after {
  inset: 18%;
}

.hero-rings {
  position: absolute;
  top: 38%;
  right: 10%;
  width: 340px;
  height: 340px;
  pointer-events: none;
}

.hero-rings span {
  position: absolute;
  border: 1px solid rgba(222, 193, 159, 0.1);
  border-radius: 50%;
}

.hero-rings span:nth-child(1) {
  inset: 0;
}

.hero-rings span:nth-child(2) {
  inset: 17%;
}

.hero-rings span:nth-child(3) {
  inset: 34%;
}

.hero-top {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-brand__seal {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(226, 196, 162, 0.45);
  border-radius: 11px;
  color: #f7ecdc;
  background: rgba(255, 255, 255, 0.035);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  position: relative;
}

.hero-brand__seal::after {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 10px;
  height: 1px;
  background: rgba(226, 196, 162, 0.6);
  content: '';
}

.hero-brand__copy {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.hero-brand__copy strong {
  color: #f8efe3;
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 0.1em;
}

.hero-brand__copy span {
  margin-top: 3px;
  color: rgba(190, 210, 201, 0.46);
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.18em;
}

.hero-top__edition {
  color: rgba(194, 213, 204, 0.42);
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.16em;
}

.hero-body {
  position: relative;
  z-index: 2;
  width: min(570px, 74%);
  margin-top: clamp(48px, 9vh, 110px);
}

.hero-kicker {
  display: flex;
  align-items: center;
  gap: 11px;
  color: #d7a674;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.hero-kicker__line {
  width: 28px;
  height: 1px;
  background: #c7915d;
}

.hero-title {
  margin-top: 24px;
  color: #f8efe2;
  font-family: var(--font-display);
  font-size: clamp(42px, 4.2vw, 66px);
  font-weight: 700;
  letter-spacing: 0.015em;
  line-height: 1.28;
}

.hero-title span {
  position: relative;
  color: #dfb080;
  white-space: nowrap;
}

.hero-title span::after {
  position: absolute;
  right: 3px;
  bottom: 3px;
  left: 2px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(223, 176, 128, 0.5), transparent);
  content: '';
}

.hero-description {
  margin-top: 22px;
  color: rgba(220, 231, 224, 0.57);
  font-size: 13px;
  letter-spacing: 0.03em;
  line-height: 2;
}

.hero-quote {
  position: relative;
  width: min(400px, 86%);
  margin-top: clamp(30px, 5vh, 58px);
  padding: 18px 22px 16px 20px;
  border-left: 1px solid rgba(200, 145, 93, 0.6);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.055), transparent);
}

.hero-quote__mark {
  position: absolute;
  top: -2px;
  left: 12px;
  color: rgba(200, 145, 93, 0.32);
  font-family: var(--font-display);
  font-size: 38px;
  line-height: 1;
}

.hero-quote p {
  position: relative;
  color: rgba(233, 238, 230, 0.7);
  font-family: var(--font-display);
  font-size: 12.5px;
  letter-spacing: 0.04em;
  line-height: 1.9;
}

.hero-quote__source {
  display: block;
  margin-top: 7px;
  color: rgba(198, 214, 204, 0.36);
  font-size: 9px;
  letter-spacing: 0.08em;
}

.hero-art {
  position: absolute;
  right: 6.5%;
  bottom: 16%;
  z-index: 3;
  width: 240px;
  height: 300px;
  pointer-events: none;
}

.floating-card {
  position: absolute;
  top: 1%;
  right: -12%;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 13px;
  border: 1px solid rgba(231, 211, 185, 0.16);
  border-radius: 10px;
  background: rgba(19, 56, 51, 0.62);
  box-shadow: 0 18px 40px rgba(3, 18, 17, 0.24);
  backdrop-filter: blur(14px);
  animation: float 6s ease-in-out infinite;
}

.floating-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  color: #e2b183;
  background: rgba(216, 161, 111, 0.12);
  font-size: 14px;
}

.floating-card > div:last-child {
  display: flex;
  flex-direction: column;
}

.floating-card span {
  color: rgba(202, 220, 210, 0.46);
  font-size: 8px;
}

.floating-card strong {
  color: #f4e8d8;
  font-family: var(--font-display);
  font-size: 17px;
  line-height: 1.2;
}

.floating-card small {
  font-size: 8px;
  font-weight: 400;
}

.book-stack {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 195px;
  height: 265px;
  transform: rotate(-4deg);
}

.stack-book {
  position: absolute;
  border-radius: 3px 7px 7px 3px;
  box-shadow: 0 24px 48px rgba(3, 17, 16, 0.32);
}

.stack-book--back {
  top: 8px;
  right: 14px;
  width: 132px;
  height: 215px;
  background: linear-gradient(145deg, #b78a60, #8d6341);
  transform: rotate(13deg);
}

.stack-book--back::after {
  position: absolute;
  inset: 10px;
  border: 1px solid rgba(255, 247, 234, 0.22);
  border-radius: 2px;
  content: '';
}

.stack-book--back span {
  position: absolute;
  top: 23px;
  left: 20px;
  color: rgba(255, 246, 232, 0.75);
  font-family: var(--font-display);
  font-size: 44px;
}

.stack-book--back i {
  position: absolute;
  bottom: 22px;
  left: 20px;
  color: rgba(255, 246, 232, 0.45);
  font-family: var(--font-mono);
  font-size: 6px;
  font-style: normal;
  letter-spacing: 0.18em;
}

.stack-book--middle {
  top: 22px;
  left: 5px;
  width: 128px;
  height: 225px;
  background:
    linear-gradient(90deg, rgba(0, 0, 0, 0.16) 0 5px, transparent 5px),
    linear-gradient(150deg, #3e7c6d, #245b52);
  transform: rotate(-9deg);
}

.stack-book--middle span {
  position: absolute;
  top: 30px;
  left: 25px;
  color: rgba(248, 239, 225, 0.8);
  font-family: var(--font-display);
  font-size: 36px;
}

.stack-book--middle i {
  position: absolute;
  bottom: 24px;
  left: 25px;
  color: rgba(240, 230, 214, 0.45);
  font-family: var(--font-mono);
  font-size: 6px;
  font-style: normal;
  letter-spacing: 0.14em;
}

.stack-book--front {
  right: 28px;
  bottom: 0;
  display: flex;
  flex-direction: column;
  width: 152px;
  height: 232px;
  padding: 20px 20px;
  background:
    linear-gradient(90deg, rgba(0, 0, 0, 0.2) 0 7px, transparent 7px),
    linear-gradient(150deg, #f2e7d6, #e5d4be);
  color: #27453f;
}

.stack-book--front::after {
  position: absolute;
  inset: 11px;
  border: 1px solid rgba(80, 83, 58, 0.23);
  border-radius: 2px;
  content: '';
}

.stack-book__label {
  position: relative;
  z-index: 1;
  align-self: flex-start;
  padding: 2px 7px;
  border: 1px solid rgba(141, 99, 65, 0.45);
  color: #8d6341;
  font-family: var(--font-display);
  font-size: 8px;
  letter-spacing: 0.18em;
}

.stack-book--front strong {
  position: relative;
  z-index: 1;
  margin-top: 24px;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.1em;
  line-height: 1.55;
}

.stack-book__seal {
  position: absolute;
  right: 17px;
  bottom: 19px;
  z-index: 1;
  padding: 5px;
  border: 1px solid rgba(153, 82, 59, 0.42);
  color: #99523b;
  font-family: var(--font-display);
  font-size: 8px;
  letter-spacing: 0.08em;
  writing-mode: vertical-rl;
}

.stack-shadow {
  position: absolute;
  right: 3px;
  bottom: -16px;
  width: 205px;
  height: 28px;
  border-radius: 50%;
  background: rgba(3, 16, 15, 0.4);
  filter: blur(15px);
}

.hero-footer {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  gap: clamp(24px, 4vw, 52px);
  margin-top: auto;
  padding-top: 30px;
  border-top: 1px solid rgba(218, 232, 223, 0.09);
}

.hero-stat {
  display: flex;
  flex-direction: column;
}

.hero-stat strong {
  color: rgba(246, 237, 226, 0.92);
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.hero-stat strong span {
  margin: 0 1px;
  color: rgba(215, 166, 116, 0.8);
  font-size: 11px;
}

.hero-stat p {
  margin-top: 2px;
  color: rgba(190, 207, 198, 0.4);
  font-size: 9px;
  letter-spacing: 0.06em;
}

.hero-footer__version {
  margin-left: auto;
  padding-bottom: 5px;
  color: rgba(190, 207, 198, 0.27);
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.1em;
}

.login-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px 48px;
  overflow: hidden;
  background:
    radial-gradient(circle at 95% 0%, rgba(191, 150, 102, 0.09), transparent 30%),
    linear-gradient(160deg, #fcf9f3 0%, #f7f1e7 100%);
}

.panel-grain {
  position: absolute;
  inset: 0;
  opacity: 0.25;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.75' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.18'/%3E%3C/svg%3E");
  pointer-events: none;
}

.login-box {
  position: relative;
  z-index: 2;
  width: min(424px, 100%);
  animation: panel-in 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.mobile-brand {
  display: none;
}

.login-heading__eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--copper-600);
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.login-heading__eyebrow span {
  width: 20px;
  height: 1px;
  background: var(--copper-500);
}

.login-heading h2 {
  margin-top: 14px;
  color: var(--ink-900);
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.login-heading p {
  margin-top: 9px;
  color: var(--text-500);
  font-size: 12px;
  letter-spacing: 0.02em;
}

.login-form {
  margin-top: 30px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  height: 22px;
  margin-bottom: 7px;
  color: var(--text-700);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.login-form :deep(.el-input__wrapper) {
  height: 49px;
  border-radius: 10px;
  background: rgba(255, 253, 249, 0.8);
  box-shadow: 0 0 0 1px #e5d9c9 inset;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #d4c3ad inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fffdf9;
  box-shadow: 0 0 0 1.5px var(--jade-400) inset !important;
}

.login-form :deep(.el-input__prefix) {
  margin-right: 4px;
  color: #a18d76;
}

.login-form :deep(.el-input__inner) {
  color: var(--text-900);
  font-size: 13px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: -6px;
  margin-bottom: 18px;
}

.form-options :deep(.el-checkbox__label) {
  color: var(--text-500);
  font-size: 11px;
}

.form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  border-color: var(--ink-600);
  background-color: var(--ink-600);
}

.text-link {
  border: 0;
  color: var(--copper-600);
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.text-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.login-error {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: -4px 0 14px;
  padding: 9px 11px;
  border: 1px solid rgba(181, 91, 82, 0.16);
  border-radius: 8px;
  color: var(--danger);
  background: var(--danger-bg);
  font-size: 11px;
  line-height: 1.5;
}

.login-submit {
  display: flex;
  width: 100%;
  height: 49px;
  border-radius: 10px;
  color: #faf3e8;
  background: linear-gradient(135deg, #1c5149, #123b3a);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
  box-shadow: 0 12px 26px rgba(17, 53, 49, 0.18);
}

.login-submit:hover {
  background: linear-gradient(135deg, #236056, #164442);
  box-shadow: 0 14px 30px rgba(17, 53, 49, 0.24);
}

.login-submit__arrow {
  margin-left: 7px;
  color: #d9ac7d;
  font-size: 13px;
}

.demo-section {
  margin-top: 28px;
}

.demo-section__head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 11px;
}

.demo-section__head > span {
  color: var(--text-500);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.demo-section__head i {
  flex: 1;
  height: 1px;
  background: var(--line);
}

.demo-section__head small {
  color: var(--text-400);
  font-size: 9px;
}

.demo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.demo-card {
  display: flex;
  flex-direction: column;
  padding: 12px 13px 11px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: rgba(253, 250, 245, 0.68);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.demo-card:hover {
  border-color: var(--copper-200);
  background: #fffcf7;
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(53, 50, 37, 0.06);
}

.demo-card.is-active {
  border-color: rgba(46, 115, 100, 0.35);
  background: linear-gradient(145deg, rgba(232, 242, 237, 0.8), rgba(253, 250, 245, 0.9));
  box-shadow: 0 0 0 1px rgba(46, 115, 100, 0.07);
}

.demo-card__top {
  display: flex;
  align-items: center;
  gap: 7px;
}

.demo-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  color: var(--ink-600);
  background: var(--jade-100);
  font-size: 12px;
}

.demo-card__role {
  color: var(--text-500);
  font-size: 9px;
  font-weight: 600;
}

.demo-card__check {
  margin-left: auto;
  color: var(--jade-500);
  font-size: 12px;
}

.demo-card > strong {
  margin-top: 9px;
  color: var(--text-700);
  font-size: 10.5px;
  font-weight: 650;
  white-space: nowrap;
}

.demo-card > p {
  margin-top: 2px;
  color: var(--text-400);
  font-size: 8.5px;
  line-height: 1.5;
}

.demo-card__account {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  padding-top: 7px;
  border-top: 1px dashed var(--line);
  color: var(--copper-600);
  font-family: var(--font-mono);
  font-size: 8px;
}

.demo-card__account i {
  color: var(--line-strong);
  font-style: normal;
}

.login-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 20px;
  color: var(--text-400);
  font-size: 8.5px;
  letter-spacing: 0.02em;
}

.login-note .el-icon {
  color: #9ea993;
  font-size: 10px;
}

.login-panel__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 26px;
  padding-top: 15px;
  border-top: 1px solid rgba(226, 215, 200, 0.75);
  color: var(--text-400);
  font-size: 8.5px;
}

.login-panel__footer button {
  border: 0;
  color: var(--text-400);
  background: transparent;
  font-size: 8.5px;
  cursor: pointer;
}

.login-panel__footer button:hover {
  color: var(--copper-600);
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0) rotate(-1deg);
  }
  50% {
    transform: translateY(-9px) rotate(1deg);
  }
}

@keyframes panel-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1180px) {
  .login-hero {
    padding: 34px 38px 30px;
  }

  .login-panel {
    padding: 40px 34px;
  }

  .hero-body {
    width: 82%;
  }

  .hero-art {
    right: 3.5%;
    bottom: 19%;
    transform: scale(0.84);
    transform-origin: right bottom;
  }

  .hero-title {
    font-size: clamp(38px, 4.4vw, 54px);
  }
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    display: none;
  }

  .login-panel {
    min-height: 100vh;
    padding: 36px 24px;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 11px;
    margin-bottom: 34px;
  }

  .mobile-brand .hero-brand__seal {
    color: #f7ecdc;
    background: var(--ink-700);
  }

  .mobile-brand .hero-brand__copy strong {
    color: var(--ink-800);
  }

  .mobile-brand .hero-brand__copy span {
    color: var(--text-400);
  }

  .login-box {
    width: min(420px, 100%);
  }
}

@media (max-width: 520px) {
  .login-panel {
    align-items: flex-start;
    padding: 28px 18px;
  }

  .login-heading h2 {
    font-size: 31px;
  }

  .demo-grid {
    grid-template-columns: 1fr;
  }

  .login-panel__footer {
    margin-top: 20px;
  }
}
</style>
