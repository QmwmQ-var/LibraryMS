<script setup>
import { computed } from 'vue'
import { BOOK_COVER_PALETTES } from '@/utils/constants'

const props = defineProps({
  book: { type: Object, required: true },
  size: { type: String, default: 'md' },
  flat: { type: Boolean, default: false },
})

const palette = computed(() => {
  const index = (Number(props.book.id) || 1) % BOOK_COVER_PALETTES.length
  let base = BOOK_COVER_PALETTES[index]
  if (props.book.categoryColor) {
    base = {
      ...base,
      accent: props.book.categoryColor,
    }
  }
  return base
})

const categoryLabel = computed(() => props.book.categoryName || '馆藏')
const displayTitle = computed(() => props.book.title || props.book.bookTitle || '未命名图书')
const displayAuthor = computed(() => props.book.author || props.book.bookAuthor || '')
</script>

<template>
  <div
    class="book-cover"
    :class="[`size-${size}`, { 'is-flat': flat }]"
    :style="{
      '--cover-bg': palette.bg,
      '--cover-ink': palette.ink,
      '--cover-accent': palette.accent,
    }"
  >
    <span class="book-cover__spine" />
    <span class="book-cover__grain" />

    <div class="book-cover__top">
      <span class="book-cover__category">{{ categoryLabel }}</span>
      <span class="book-cover__seal">知墨</span>
    </div>

    <div class="book-cover__middle">
      <div class="book-cover__rule" />
      <h4 class="book-cover__title">{{ displayTitle }}</h4>
      <p class="book-cover__author">{{ displayAuthor }}</p>
    </div>

    <div class="book-cover__bottom">
      <span class="book-cover__library">ZHIMO LIBRARY</span>
      <span class="book-cover__code">NO. {{ String(book.id || 0).padStart(3, '0') }}</span>
    </div>

    <span class="book-cover__ornament" />
  </div>
</template>

<style scoped>
.book-cover {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  aspect-ratio: 0.7;
  padding: 16px 14px 12px 19px;
  overflow: hidden;
  border-radius: 3px 7px 7px 3px;
  color: var(--cover-ink);
  background:
    radial-gradient(circle at 65% 22%, rgba(255, 255, 255, 0.08), transparent 30%),
    linear-gradient(145deg, var(--cover-bg), color-mix(in srgb, var(--cover-bg) 82%, #051716));
  box-shadow:
    0 10px 24px rgba(25, 39, 34, 0.2),
    inset -3px 0 7px rgba(0, 0, 0, 0.13),
    inset 1px 0 0 rgba(255, 255, 255, 0.1);
}

.book-cover.is-flat {
  box-shadow: 0 8px 20px rgba(25, 39, 34, 0.14);
}

.book-cover__spine {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 7px;
  background: linear-gradient(
    90deg,
    rgba(0, 0, 0, 0.3),
    rgba(255, 255, 255, 0.09) 52%,
    rgba(0, 0, 0, 0.19)
  );
}

.book-cover__grain {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 160 160' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  pointer-events: none;
}

.book-cover__top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 6px;
}

.book-cover__category {
  padding: 1px 5px;
  border: 1px solid color-mix(in srgb, var(--cover-accent) 55%, transparent);
  border-radius: 1px;
  color: color-mix(in srgb, var(--cover-accent) 90%, white);
  font-family: var(--font-display);
  font-size: 7px;
  letter-spacing: 0.12em;
  white-space: nowrap;
}

.book-cover__seal {
  width: 21px;
  height: 21px;
  padding: 3px;
  border: 1px solid color-mix(in srgb, var(--cover-accent) 48%, transparent);
  color: color-mix(in srgb, var(--cover-accent) 88%, white);
  font-family: var(--font-display);
  font-size: 7px;
  line-height: 1.35;
  text-align: center;
  writing-mode: vertical-rl;
}

.book-cover__middle {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  padding: 7px 2px 9px;
}

.book-cover__rule {
  width: 24px;
  height: 1px;
  margin-bottom: 9px;
  background: var(--cover-accent);
}

.book-cover__title {
  display: -webkit-box;
  overflow: hidden;
  color: var(--cover-ink);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 1.32;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.book-cover__author {
  overflow: hidden;
  margin-top: 7px;
  color: color-mix(in srgb, var(--cover-ink) 68%, transparent);
  font-family: var(--font-display);
  font-size: 8px;
  letter-spacing: 0.03em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-cover__bottom {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 3px;
  padding-top: 8px;
  border-top: 1px solid color-mix(in srgb, var(--cover-ink) 13%, transparent);
}

.book-cover__library {
  color: color-mix(in srgb, var(--cover-ink) 40%, transparent);
  font-family: var(--font-mono);
  font-size: 5.5px;
  letter-spacing: 0.12em;
}

.book-cover__code {
  color: color-mix(in srgb, var(--cover-accent) 70%, transparent);
  font-family: var(--font-mono);
  font-size: 5.5px;
  letter-spacing: 0.04em;
}

.book-cover__ornament {
  position: absolute;
  right: -19px;
  bottom: -23px;
  width: 82px;
  height: 82px;
  border: 1px solid color-mix(in srgb, var(--cover-accent) 16%, transparent);
  border-radius: 50%;
  pointer-events: none;
}

.book-cover__ornament::after {
  position: absolute;
  inset: 12px;
  border: 1px solid color-mix(in srgb, var(--cover-accent) 11%, transparent);
  border-radius: 50%;
  content: '';
}

.size-sm {
  padding: 11px 10px 8px 14px;
  border-radius: 2px 5px 5px 2px;
}

.size-sm .book-cover__title {
  font-size: 14px;
}

.size-sm .book-cover__author {
  margin-top: 4px;
  font-size: 6.5px;
}

.size-sm .book-cover__middle {
  padding: 5px 1px 6px;
}

.size-sm .book-cover__rule {
  width: 18px;
  margin-bottom: 5px;
}

.size-sm .book-cover__seal {
  width: 16px;
  height: 16px;
  font-size: 5.5px;
}

.size-sm .book-cover__category {
  font-size: 5.5px;
}

.size-sm .book-cover__bottom {
  padding-top: 5px;
}

.size-sm .book-cover__library,
.size-sm .book-cover__code {
  font-size: 4.5px;
}

.size-lg {
  padding: 25px 22px 17px 30px;
  border-radius: 4px 10px 10px 4px;
}

.size-lg .book-cover__title {
  font-size: 29px;
}

.size-lg .book-cover__author {
  margin-top: 11px;
  font-size: 11px;
}

.size-lg .book-cover__category {
  font-size: 9px;
}

.size-lg .book-cover__seal {
  width: 28px;
  height: 28px;
  font-size: 9px;
}

.size-lg .book-cover__library,
.size-lg .book-cover__code {
  font-size: 7px;
}

.size-xs {
  padding: 7px 7px 5px 10px;
}

.size-xs .book-cover__title {
  font-size: 11px;
}

.size-xs .book-cover__author,
.size-xs .book-cover__bottom,
.size-xs .book-cover__rule,
.size-xs .book-cover__category {
  display: none;
}

.size-xs .book-cover__seal {
  width: 14px;
  height: 14px;
  padding: 1px;
  font-size: 5px;
}
</style>
