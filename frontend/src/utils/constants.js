export const ROLES = {
  ADMIN: 'ADMIN',
  READER: 'READER',
}

export const ROLE_LABELS = {
  ADMIN: '图书管理员',
  READER: '读者',
}

export const READER_TYPES = {
  UNDERGRADUATE: {
    label: '本科生',
    shortLabel: '本科',
    maxBorrow: 5,
    borrowDays: 30,
    color: '#4f7f74',
  },
  GRADUATE: {
    label: '研究生',
    shortLabel: '研究生',
    maxBorrow: 10,
    borrowDays: 60,
    color: '#8e6a45',
  },
  TEACHER: {
    label: '教师',
    shortLabel: '教师',
    maxBorrow: 15,
    borrowDays: 90,
    color: '#7a5d76',
  },
}

export const READER_TYPE_OPTIONS = Object.entries(READER_TYPES).map(([value, config]) => ({
  value,
  label: `${config.label}（${config.maxBorrow} 本 / ${config.borrowDays} 天）`,
}))

export const CARD_STATUS = {
  ACTIVE: { label: '正常', tone: 'green' },
  LOST: { label: '挂失', tone: 'amber' },
  CANCELLED: { label: '注销', tone: 'gray' },
}

export const CARD_STATUS_OPTIONS = Object.entries(CARD_STATUS).map(([value, config]) => ({
  value,
  label: config.label,
}))

export const BORROW_STATUS = {
  BORROWED: { label: '借阅中', tone: 'blue' },
  RETURNED: { label: '已归还', tone: 'green' },
  OVERDUE: { label: '已逾期', tone: 'red' },
}

export const BORROW_STATUS_OPTIONS = Object.entries(BORROW_STATUS).map(([value, config]) => ({
  value,
  label: config.label,
}))

export const PENALTY_STATUS = {
  UNPAID: { label: '待缴纳', tone: 'amber' },
  PAID: { label: '已缴纳', tone: 'green' },
}

export const PENALTY_STATUS_OPTIONS = Object.entries(PENALTY_STATUS).map(([value, config]) => ({
  value,
  label: config.label,
}))

export const RESERVATION_STATUS = {
  PENDING: { label: '排队中', tone: 'blue' },
  READY: { label: '已到书', tone: 'amber' },
  FULFILLED: { label: '已借出', tone: 'green' },
  CANCELLED: { label: '已取消', tone: 'gray' },
  EXPIRED: { label: '已过期', tone: 'red' },
}

export const RESERVATION_STATUS_OPTIONS = Object.entries(RESERVATION_STATUS).map(
  ([value, config]) => ({ value, label: config.label }),
)

export const RESERVATION_ACTIVE_STATUSES = ['PENDING', 'READY']
export const RESERVATION_HOLD_HOURS = 48
export const MAX_ACTIVE_RESERVATIONS = 5

export const FINE_RATE = 0.5
export const MAX_RENEW_COUNT = 1

export const BOOK_COVER_PALETTES = [
  { bg: '#1f4e48', ink: '#f4eadb', accent: '#c6915c' },
  { bg: '#9b5b45', ink: '#fbf1e5', accent: '#e7c49d' },
  { bg: '#38546b', ink: '#f3eee5', accent: '#bb9666' },
  { bg: '#6e5a76', ink: '#f8efe7', accent: '#d7b58b' },
  { bg: '#8c7245', ink: '#fbf5e9', accent: '#e3cda8' },
  { bg: '#3d5c50', ink: '#f3ecdf', accent: '#c89c70' },
  { bg: '#7f3f3f', ink: '#f9ede2', accent: '#d8af83' },
  { bg: '#305d64', ink: '#f0eee4', accent: '#d1a46f' },
]

export const CATEGORY_TONES = ['#2e7364', '#9b6238', '#4d6b80', '#756083', '#8a6b3b', '#995c51']
