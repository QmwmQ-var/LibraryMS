export const menuGroups = [
  {
    title: '总览',
    items: [{ name: 'dashboard', label: '首页', icon: 'HomeFilled', roles: ['ADMIN', 'READER'] }],
  },
  {
    title: '阅读服务',
    items: [
      { name: 'books', label: '馆藏检索', icon: 'Reading', roles: ['READER'] },
      { name: 'my-borrows', label: '我的借阅', icon: 'Collection', roles: ['READER'] },
      { name: 'my-reservations', label: '我的预约', icon: 'Calendar', roles: ['READER'] },
      { name: 'penalties', label: '我的罚款', icon: 'Wallet', roles: ['READER'] },
    ],
  },
  {
    title: '馆藏与读者',
    items: [
      { name: 'books-manage', label: '图书管理', icon: 'Reading', roles: ['ADMIN'] },
      { name: 'categories', label: '分类管理', icon: 'Files', roles: ['ADMIN'] },
      { name: 'readers', label: '读者管理', icon: 'User', roles: ['ADMIN'] },
    ],
  },
  {
    title: '流通与数据',
    items: [
      { name: 'circulation', label: '流通办理', icon: 'Switch', roles: ['ADMIN'] },
      { name: 'reservations-manage', label: '预约管理', icon: 'Calendar', roles: ['ADMIN'] },
      { name: 'penalties-manage', label: '罚款管理', icon: 'Wallet', roles: ['ADMIN'] },
      { name: 'stats', label: '统计报表', icon: 'TrendCharts', roles: ['ADMIN'] },
    ],
  },
  {
    title: '账户',
    items: [{ name: 'profile', label: '个人中心', icon: 'Setting', roles: ['ADMIN', 'READER'] }],
  },
]

export const flatMenuItems = menuGroups.flatMap((group) => group.items)
