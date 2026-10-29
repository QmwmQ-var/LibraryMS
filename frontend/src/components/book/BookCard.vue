<script setup>
import BookCover from './BookCover.vue'

defineProps({
  book: { type: Object, required: true },
  mode: { type: String, default: 'browse' },
  borrowDisabled: { type: Boolean, default: false },
  reservation: { type: Object, default: null },
})

const emit = defineEmits(['detail', 'borrow', 'reserve', 'edit', 'remove'])
</script>

<template>
  <article class="book-card" @click="emit('detail', book)">
    <div class="book-card__cover">
      <BookCover :book="book" size="md" />
      <span
        class="book-card__availability"
        :class="{
          'is-empty': book.availableCopies <= 0,
          'is-ready': reservation?.status === 'READY',
        }"
      >
        <span />
        {{
          reservation?.status === 'READY'
            ? '预约到书'
            : book.availableCopies > 0
              ? '可借'
              : '借完'
        }}
      </span>
    </div>

    <div class="book-card__body">
      <div class="book-card__topline">
        <span class="book-card__category">
          <i :style="{ background: book.categoryColor }" />
          {{ book.categoryName }}
        </span>
        <span class="book-card__rating">
          <el-icon><StarFilled /></el-icon>
          {{ Number(book.rating || 0).toFixed(1) }}
        </span>
      </div>

      <h3 class="book-card__title" :title="book.title">{{ book.title }}</h3>
      <p class="book-card__author">{{ book.author }}</p>
      <p class="book-card__description">{{ book.description }}</p>

      <div class="book-card__meta">
        <span class="mono">{{ book.isbn }}</span>
        <span>{{ book.location }}</span>
      </div>

      <div class="book-card__footer">
        <div class="book-card__count">
          <strong>{{ book.availableCopies }}</strong>
          <span>/ {{ book.totalCopies }} 可借</span>
        </div>

        <div class="book-card__actions" @click.stop>
          <template v-if="mode === 'manage'">
            <el-tooltip content="编辑图书" placement="top">
              <button class="mini-action" @click="emit('edit', book)">
                <el-icon><EditPen /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="删除图书" placement="top">
              <button class="mini-action mini-action--danger" @click="emit('remove', book)">
                <el-icon><Delete /></el-icon>
              </button>
            </el-tooltip>
          </template>
          <template v-else>
            <button class="detail-link" @click="emit('detail', book)">详情</button>
            <template v-if="reservation?.status === 'READY'">
              <el-button
                type="primary"
                size="small"
                class="borrow-btn is-ready"
                :disabled="borrowDisabled"
                @click="emit('borrow', book)"
              >
                到书领取
              </el-button>
            </template>
            <template v-else-if="reservation?.status === 'PENDING'">
              <span class="reserved-hint">
                <el-icon><Clock /></el-icon>
                已预约 · 第 {{ reservation.queuePosition || 1 }} 位
              </span>
            </template>
            <template v-else-if="book.availableCopies > 0">
              <el-button
                type="primary"
                size="small"
                class="borrow-btn"
                :disabled="borrowDisabled"
                @click="emit('borrow', book)"
              >
                借阅
              </el-button>
            </template>
            <template v-else>
              <el-button
                type="primary"
                plain
                size="small"
                class="borrow-btn"
                :disabled="borrowDisabled"
                @click="emit('reserve', book)"
              >
                预约
              </el-button>
            </template>
          </template>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.book-card {
  position: relative;
  display: flex;
  gap: 16px;
  min-height: 218px;
  padding: 15px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: rgba(253, 251, 247, 0.86);
  box-shadow: var(--shadow-xs);
  cursor: pointer;
  transition:
    transform 0.24s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.book-card:hover {
  z-index: 2;
  border-color: var(--line-strong);
  box-shadow: 0 18px 38px rgba(35, 48, 42, 0.09);
  transform: translateY(-3px);
}

.book-card__cover {
  position: relative;
  flex-shrink: 0;
  width: 128px;
  padding: 2px 4px 4px 2px;
}

.book-card__availability {
  position: absolute;
  top: 10px;
  right: -3px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 7px;
  border-radius: 4px 0 0 4px;
  color: var(--success);
  background: rgba(245, 252, 248, 0.91);
  box-shadow: 0 3px 9px rgba(20, 50, 40, 0.12);
  font-size: 8px;
  font-weight: 650;
  letter-spacing: 0.05em;
  backdrop-filter: blur(6px);
}

.book-card__availability span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
}

.book-card__availability.is-empty {
  color: var(--text-400);
}

.book-card__availability.is-ready {
  color: var(--copper-600);
  background: rgba(255, 248, 238, 0.94);
}

.book-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  padding: 3px 2px 1px 0;
}

.book-card__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.book-card__category {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-500);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.book-card__category i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.book-card__rating {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--copper-500);
  font-family: var(--font-mono);
  font-size: 8.5px;
}

.book-card__title {
  overflow: hidden;
  margin-top: 8px;
  color: var(--ink-800);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-card__author {
  overflow: hidden;
  margin-top: 3px;
  color: var(--text-500);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-card__description {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 9px;
  color: var(--text-400);
  font-size: 10px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.book-card__meta {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 8px;
  color: var(--text-400);
  font-size: 8px;
}

.book-card__meta span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-card__meta span + span::before {
  margin-right: 9px;
  color: var(--line-strong);
  content: '·';
}

.book-card__footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}

.book-card__count {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.book-card__count strong {
  color: var(--ink-700);
  font-family: var(--font-display);
  font-size: 18px;
  line-height: 1;
}

.book-card__count span {
  color: var(--text-400);
  font-size: 8px;
}

.book-card__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mini-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  border: 1px solid var(--line);
  border-radius: 7px;
  color: var(--text-500);
  background: var(--paper-50);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.mini-action:hover {
  color: var(--ink-700);
  border-color: var(--ink-200);
  background: var(--jade-100);
}

.mini-action--danger:hover {
  color: var(--danger);
  border-color: rgba(181, 91, 82, 0.2);
  background: var(--danger-bg);
}

.detail-link {
  padding: 4px 2px;
  border: 0;
  color: var(--text-500);
  background: transparent;
  font-size: 9.5px;
  cursor: pointer;
}

.detail-link:hover {
  color: var(--ink-700);
}

.borrow-btn {
  height: 27px;
  padding: 0 12px;
  border-radius: 7px;
  font-size: 10px;
}

.borrow-btn.is-ready {
  color: #fffaf2;
  background: var(--copper-600);
  border-color: var(--copper-600);
  box-shadow: 0 5px 12px rgba(155, 98, 56, 0.18);
}

.reserved-hint {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--copper-600);
  font-size: 8px;
  white-space: nowrap;
}

@media (max-width: 480px) {
  .book-card {
    gap: 12px;
  }

  .book-card__cover {
    width: 108px;
  }

  .book-card__description {
    -webkit-line-clamp: 1;
  }
}
</style>
