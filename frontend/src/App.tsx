import {
  BadgeCheck,
  Bell,
  ClipboardList,
  FileSpreadsheet,
  GitBranch,
  LayoutDashboard,
  Search,
  Settings,
  Users,
} from 'lucide-react'
import './App.css'

const applications = [
  {
    title: '東京出張申請',
    form: '出張申請',
    applicant: '山田 太郎',
    status: '承認中',
    updated: '2026-06-04',
  },
  {
    title: '業務用PC購入稟議',
    form: '稟議',
    applicant: '久米 幸子',
    status: '確認待ち',
    updated: '2026-06-03',
  },
  {
    title: '月次勤務表',
    form: '勤務表',
    applicant: '柳田 雅之',
    status: '承認済み',
    updated: '2026-06-01',
  },
]

const navItems = [
  { label: 'ダッシュボード', icon: LayoutDashboard, active: true },
  { label: '各種申請', icon: ClipboardList },
  { label: '承認タスク', icon: BadgeCheck },
  { label: 'ワークフロー定義', icon: GitBranch },
  { label: '社員', icon: Users },
  { label: '申請書定義', icon: FileSpreadsheet },
  { label: '設定', icon: Settings },
]

function App() {
  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">W</div>
          <div>
            <strong>ワークフローデモ</strong>
            <span>モダン承認ワークフロー</span>
          </div>
        </div>

        <nav className="nav-list" aria-label="Main navigation">
          {navItems.map((item) => (
            <button className={item.active ? 'nav-item active' : 'nav-item'} key={item.label}>
              <item.icon size={18} aria-hidden="true" />
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">ワークフロー運用</p>
            <h1>ワークフロー運用ダッシュボード</h1>
          </div>
          <div className="topbar-actions">
            <label className="search">
              <Search size={17} aria-hidden="true" />
              <input aria-label="申請検索" placeholder="申請を検索" />
            </label>
            <button className="icon-button" aria-label="Notifications">
              <Bell size={18} aria-hidden="true" />
            </button>
          </div>
        </header>

        <section className="summary-grid" aria-label="Workflow summary">
          <article>
            <span>確認待ち</span>
            <strong>8</strong>
            <small>本日期限 3件</small>
          </article>
          <article>
            <span>申請中</span>
            <strong>14</strong>
            <small>6種類の申請書</small>
          </article>
          <article>
            <span>公開中の承認フロー</span>
            <strong>6</strong>
            <small>今月更新 2件</small>
          </article>
        </section>

        <section className="content-grid">
          <article className="panel applications-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">申請キュー</p>
                <h2>最近の申請</h2>
              </div>
              <button className="primary-button">新規申請</button>
            </div>

            <div className="table">
              <div className="table-row table-head">
                <span>件名</span>
                <span>申請書</span>
                <span>申請者</span>
                <span>ステータス</span>
                <span>更新日</span>
              </div>
              {applications.map((application) => (
                <div className="table-row" key={application.title}>
                  <strong>{application.title}</strong>
                  <span>{application.form}</span>
                  <span>{application.applicant}</span>
                  <span className="status-pill">{application.status}</span>
                  <span>{application.updated}</span>
                </div>
              ))}
            </div>
          </article>

          <article className="panel flow-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">ワークフロー定義</p>
                <h2>部門承認ルート</h2>
              </div>
            </div>
            <div className="flow-preview" aria-label="ワークフローのプレビュー">
              <div className="flow-node start">申請者</div>
              <div className="flow-line" />
              <div className="flow-node">課長</div>
              <div className="flow-line" />
              <div className="flow-node final">部長</div>
            </div>
          </article>
        </section>
      </section>
    </main>
  )
}

export default App
