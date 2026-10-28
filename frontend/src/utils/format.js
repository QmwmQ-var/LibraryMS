import dayjs from 'dayjs'

export const formatDate = (value, template = 'YYYY-MM-DD') =>
  value ? dayjs(value).format(template) : '—'

export const formatDateTime = (value) => formatDate(value, 'YYYY-MM-DD HH:mm')

export const formatCurrency = (value) =>
  `¥${Number(value || 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`

export const daysBetween = (from, to) => dayjs(to).startOf('day').diff(dayjs(from).startOf('day'), 'day')

export const daysUntil = (date) => daysBetween(dayjs(), date)

export const relativeDue = (dueDate) => {
  const diff = daysUntil(dueDate)
  if (diff < 0) return { label: `已逾期 ${Math.abs(diff)} 天`, tone: 'red' }
  if (diff === 0) return { label: '今日到期', tone: 'amber' }
  if (diff <= 3) return { label: `${diff} 天后到期`, tone: 'amber' }
  return { label: `${diff} 天后到期`, tone: 'green' }
}

export const initials = (name = '') => {
  const clean = String(name).trim()
  if (!clean) return '?'
  // 中文姓名取最后一至两字，英文取首字母组合。
  if (/[\u4e00-\u9fa5]/.test(clean)) {
    return clean.length <= 2 ? clean : clean.slice(-2)
  }
  return clean
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

export const todayLabel = (value = dayjs()) => value.format('YYYY年M月D日 dddd')

export const greeting = () => {
  const hour = dayjs().hour()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}
