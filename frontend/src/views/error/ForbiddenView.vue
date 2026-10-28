<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
</script>

<template>
  <div class="forbidden">
    <div class="forbidden__art">
      <span class="forbidden__code">403</span>
      <span class="forbidden__ring" />
    </div>
    <div class="forbidden__copy">
      <div class="eyebrow">Access Restricted</div>
      <h1>这一页，需要另一把钥匙。</h1>
      <p>
        当前账号「{{ auth.displayName }}」没有访问此页面的权限。
        请切换到图书管理员账号，或返回你的工作空间。
      </p>
      <div class="forbidden__actions">
        <el-button type="primary" @click="router.push({ name: 'dashboard' })">
          返回首页
        </el-button>
        <el-button plain @click="router.back()">回到上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 190px);
  gap: 55px;
  max-width: 920px;
  margin: 0 auto;
}

.forbidden__art {
  position: relative;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 220px;
  height: 220px;
}

.forbidden__code {
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 78px;
  font-weight: 700;
  letter-spacing: -0.04em;
}

.forbidden__ring {
  position: absolute;
  inset: 0;
  border: 1px solid var(--line-strong);
  border-radius: 50%;
}

.forbidden__ring::after {
  position: absolute;
  inset: 25px;
  border: 1px dashed var(--line);
  border-radius: 50%;
  content: '';
}

.forbidden__copy {
  max-width: 430px;
}

.forbidden__copy h1 {
  color: var(--ink-900);
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.forbidden__copy p {
  margin-top: 10px;
  color: var(--text-500);
  font-size: 11px;
  line-height: 1.9;
}

.forbidden__actions {
  display: flex;
  gap: 9px;
  margin-top: 20px;
}

@media (max-width: 720px) {
  .forbidden {
    flex-direction: column;
    gap: 22px;
    padding: 40px 0;
    text-align: center;
  }

  .forbidden__art {
    width: 160px;
    height: 160px;
  }

  .forbidden__code {
    font-size: 58px;
  }

  .forbidden__actions {
    justify-content: center;
  }
}
</style>
