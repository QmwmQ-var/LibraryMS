<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const destination = computed(() => (auth.isAuthenticated ? '/dashboard' : '/login'))
</script>

<template>
  <div class="not-found">
    <div class="not-found__noise" />
    <div class="not-found__content">
      <div class="not-found__brand" @click="router.push(destination)">
        <span class="not-found__seal">知</span>
        <div>
          <strong>知墨图书馆</strong>
          <span>ZHIMO UNIVERSITY LIBRARY</span>
        </div>
      </div>

      <div class="not-found__main">
        <div class="not-found__code">404<span>.</span></div>
        <div class="not-found__copy">
          <div class="eyebrow">Page Not Found</div>
          <h1>这本书，似乎不在馆藏里。</h1>
          <p>你访问的页面可能已被移动或删除。不妨回到书架，重新找到阅读的方向。</p>
          <div class="not-found__actions">
            <el-button type="primary" @click="router.push(destination)">
              {{ auth.isAuthenticated ? '返回工作台' : '返回登录' }}
            </el-button>
            <el-button v-if="auth.isAuthenticated" plain @click="router.push('/books')">
              浏览馆藏
            </el-button>
          </div>
        </div>
      </div>

      <div class="not-found__footer">
        <span>© 2026 知墨图书馆</span>
        <span>ERROR 404 · LOST IN THE STACKS</span>
      </div>

      <span class="not-found__arc" />
      <span class="not-found__arc not-found__arc--two" />
    </div>
  </div>
</template>

<style scoped>
.not-found {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: #f4eadb;
  background:
    radial-gradient(circle at 76% 24%, rgba(201, 146, 100, 0.16), transparent 32%),
    linear-gradient(145deg, #17433d, #0c2b2d 65%, #092426);
}

.not-found__noise {
  position: absolute;
  inset: 0;
  opacity: 0.1;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.not-found__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  max-width: 1180px;
  margin: 0 auto;
  padding: 34px 48px 26px;
}

.not-found__brand {
  display: flex;
  align-items: center;
  gap: 11px;
  align-self: flex-start;
  cursor: pointer;
}

.not-found__seal {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 39px;
  height: 39px;
  border: 1px solid rgba(226, 196, 162, 0.42);
  border-radius: 10px;
  color: #f7ecdc;
  font-family: var(--font-display);
  font-size: 19px;
}

.not-found__brand > div {
  display: flex;
  flex-direction: column;
}

.not-found__brand strong {
  color: #f7ede0;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.09em;
}

.not-found__brand div span {
  color: rgba(190, 210, 201, 0.42);
  font-family: var(--font-mono);
  font-size: 6px;
  letter-spacing: 0.16em;
}

.not-found__main {
  display: flex;
  flex: 1;
  align-items: center;
  gap: clamp(30px, 7vw, 90px);
}

.not-found__code {
  color: rgba(243, 232, 215, 0.94);
  font-family: var(--font-display);
  font-size: clamp(140px, 20vw, 240px);
  font-weight: 700;
  letter-spacing: -0.06em;
  line-height: 0.9;
}

.not-found__code span {
  color: #c7915d;
}

.not-found__copy {
  max-width: 450px;
}

.not-found__copy .eyebrow {
  color: #d6a779;
}

.not-found__copy .eyebrow::before {
  background: #c7915d;
}

.not-found__copy h1 {
  color: #f8efe2;
  font-family: var(--font-display);
  font-size: clamp(27px, 3vw, 38px);
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.4;
}

.not-found__copy p {
  margin-top: 11px;
  color: rgba(211, 225, 216, 0.48);
  font-size: 11px;
  line-height: 1.9;
}

.not-found__actions {
  display: flex;
  gap: 9px;
  margin-top: 22px;
}

.not-found__actions :deep(.el-button--primary) {
  color: #1a4039;
  border-color: #e6d4bd;
  background: #e6d4bd;
}

.not-found__actions :deep(.el-button--primary:hover) {
  border-color: #f2e4d1;
  background: #f2e4d1;
}

.not-found__actions :deep(.el-button.is-plain) {
  color: #dbc3a9;
  border-color: rgba(219, 195, 169, 0.26);
  background: transparent;
}

.not-found__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 18px;
  border-top: 1px solid rgba(218, 232, 223, 0.09);
  color: rgba(190, 207, 198, 0.27);
  font-size: 8px;
  letter-spacing: 0.06em;
}

.not-found__footer span:last-child {
  font-family: var(--font-mono);
  font-size: 6.5px;
  letter-spacing: 0.12em;
}

.not-found__arc {
  position: absolute;
  top: -80px;
  right: -120px;
  width: 460px;
  height: 460px;
  border: 1px solid rgba(224, 198, 169, 0.07);
  border-radius: 50%;
}

.not-found__arc--two {
  top: auto;
  right: auto;
  bottom: -260px;
  left: -140px;
  width: 520px;
  height: 520px;
  border-color: rgba(224, 198, 169, 0.05);
}

@media (max-width: 800px) {
  .not-found__content {
    padding: 25px 22px 20px;
  }

  .not-found__main {
    align-items: flex-start;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
  }

  .not-found__code {
    font-size: 120px;
  }

  .not-found__copy h1 {
    font-size: 25px;
  }

  .not-found__footer {
    margin-top: 26px;
  }
}
</style>
