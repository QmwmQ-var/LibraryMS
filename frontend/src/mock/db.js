import { seedDatabase } from './seed'

const STORAGE_KEY = 'libraryms_mock_db_v1'
let databaseCache = null

const canUseStorage = () => typeof window !== 'undefined' && window.localStorage

function readFromStorage() {
  if (!canUseStorage()) return null
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed?.version === 1 ? parsed : null
  } catch {
    return null
  }
}

export function getDatabase() {
  if (databaseCache) return databaseCache
  databaseCache = readFromStorage() || seedDatabase()
  persistDatabase()
  return databaseCache
}

export function persistDatabase() {
  if (!databaseCache || !canUseStorage()) return
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(databaseCache))
  } catch (error) {
    console.warn('[LibraryMS] 无法写入 localStorage，本次演示数据只在内存中保留。', error)
  }
}

export function resetDatabase() {
  databaseCache = seedDatabase()
  persistDatabase()
  return databaseCache
}

export function nextId(collection) {
  if (!collection?.length) return 1
  return Math.max(...collection.map((item) => Number(item.id) || 0)) + 1
}

export function resetMockStorage() {
  if (canUseStorage()) {
    window.localStorage.removeItem(STORAGE_KEY)
  }
  databaseCache = null
}

export { STORAGE_KEY }
