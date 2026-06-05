import { useState } from 'react'
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
import { useQuery } from '@tanstack/react-query'
import './App.css'
import {
  type FormField,
  getEmployees,
  getFormDefinition,
  getFormDefinitions,
  getOrganizations,
  getPositions,
} from './api'

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

function previewFieldType(field: FormField) {
  if (field.dataType === 'TEXTAREA') {
    return <textarea className="preview-input" placeholder={field.placeholder ?? ''} readOnly />
  }

  const inputType =
    field.dataType === 'NUMBER'
      ? 'number'
      : field.dataType === 'DATE'
        ? 'date'
        : field.dataType === 'MONTH'
          ? 'month'
          : 'text'

  return <input className="preview-input" placeholder={field.placeholder ?? ''} readOnly type={inputType} />
}

function App() {
  const [selectedFormCode, setSelectedFormCode] = useState<string>()
  const employeesQuery = useQuery({
    queryKey: ['employees'],
    queryFn: getEmployees,
  })
  const organizationsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: getOrganizations,
  })
  const positionsQuery = useQuery({
    queryKey: ['positions'],
    queryFn: getPositions,
  })
  const formDefinitionsQuery = useQuery({
    queryKey: ['formDefinitions'],
    queryFn: getFormDefinitions,
  })

  const employees = employeesQuery.data ?? []
  const organizations = organizationsQuery.data ?? []
  const positions = positionsQuery.data ?? []
  const formDefinitions = formDefinitionsQuery.data ?? []
  const activeFormCode = selectedFormCode ?? formDefinitions[0]?.formCode
  const selectedFormQuery = useQuery({
    queryKey: ['formDefinition', activeFormCode],
    queryFn: () => getFormDefinition(activeFormCode ?? ''),
    enabled: Boolean(activeFormCode),
  })
  const isMasterDataLoading =
    employeesQuery.isLoading || organizationsQuery.isLoading || positionsQuery.isLoading
  const hasMasterDataError =
    employeesQuery.isError || organizationsQuery.isError || positionsQuery.isError

  const masterDataSummary = hasMasterDataError
    ? '取得エラー'
    : isMasterDataLoading
      ? '-'
      : employees.length
  const employeeCountLabel = employeesQuery.isError
    ? '取得エラー'
    : employeesQuery.isLoading
      ? '読込中'
      : `${employees.length}件`
  const organizationCountLabel = organizationsQuery.isError
    ? '取得エラー'
    : organizationsQuery.isLoading
      ? '読込中'
      : `${organizations.length}件`
  const positionCountLabel = positionsQuery.isError
    ? '取得エラー'
    : positionsQuery.isLoading
      ? '読込中'
      : `${positions.length}件`
  const formDefinitionCountLabel = formDefinitionsQuery.isError
    ? '取得エラー'
    : formDefinitionsQuery.isLoading
      ? '読込中'
      : `${formDefinitions.length}件`
  const selectedForm = selectedFormQuery.data

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
            <span>社員マスタ</span>
            <strong>{masterDataSummary}</strong>
            <small>{hasMasterDataError ? '取得エラー' : '有効社員'}</small>
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

        <section className="definition-grid" aria-label="申請書定義">
          <article className="panel definition-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">申請書定義</p>
                <h2>申請フォーム設定</h2>
              </div>
              <span className="count-label">{formDefinitionCountLabel}</span>
            </div>

            <div className="definition-layout">
              <div className="definition-list" aria-label="申請書定義一覧">
                {formDefinitionsQuery.isError ? (
                  <div className="compact-row compact-row-muted">申請書定義を取得できません</div>
                ) : (
                  formDefinitions.map((formDefinition) => (
                    <button
                      className={
                        activeFormCode === formDefinition.formCode
                          ? 'definition-option active'
                          : 'definition-option'
                      }
                      key={formDefinition.formCode}
                      onClick={() => setSelectedFormCode(formDefinition.formCode)}
                      type="button"
                    >
                      <strong>{formDefinition.formName}</strong>
                      <span>{formDefinition.formCode}</span>
                      <small>{formDefinition.workflowName}</small>
                      <em>{formDefinition.fieldCount}項目</em>
                    </button>
                  ))
                )}
              </div>

              <div className="definition-detail">
                {selectedFormQuery.isError ? (
                  <div className="compact-row compact-row-muted">選択中の申請書定義を取得できません</div>
                ) : (
                  <>
                    <div className="definition-title">
                      <div>
                        <span>{selectedForm?.formCode ?? '---'}</span>
                        <h3>{selectedForm?.formName ?? '申請書を選択'}</h3>
                      </div>
                      <small>{selectedForm?.workflowName ?? 'ワークフロー未選択'}</small>
                    </div>

                    <div className="field-list">
                      {(selectedForm?.fields ?? []).map((field) => (
                        <div className="field-row" key={field.fieldKey}>
                          <strong>{field.label}</strong>
                          <span>{field.dataType}</span>
                          <span>{field.required ? '必須' : '任意'}</span>
                        </div>
                      ))}
                    </div>

                    <div className="form-preview-grid" aria-label="申請フォームプレビュー">
                      {(selectedForm?.fields ?? []).slice(0, 4).map((field) => (
                        <label className="preview-field" key={field.fieldKey}>
                          <span>
                            {field.label}
                            {field.required ? <em>必須</em> : null}
                          </span>
                          {previewFieldType(field)}
                        </label>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </div>
          </article>
        </section>

        <section className="master-grid" aria-label="マスタデータ">
          <article className="panel master-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">社員マスタ</p>
                <h2>承認に関わる社員</h2>
              </div>
              <span className="count-label">{employeeCountLabel}</span>
            </div>
            <div className="compact-list">
              {employeesQuery.isError ? (
                <div className="compact-row compact-row-muted">社員マスタを取得できません</div>
              ) : (
                employees.slice(0, 5).map((employee) => (
                  <div className="compact-row" key={employee.employeeCode}>
                    <strong>{employee.name}</strong>
                    <span>{employee.organizationName}</span>
                    <span>{employee.positionName}</span>
                  </div>
                ))
              )}
            </div>
          </article>

          <article className="panel master-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">組織マスタ</p>
                <h2>組織階層</h2>
              </div>
              <span className="count-label">{organizationCountLabel}</span>
            </div>
            <div className="compact-list">
              {organizationsQuery.isError ? (
                <div className="compact-row compact-row-muted">組織マスタを取得できません</div>
              ) : (
                organizations.slice(0, 5).map((organization) => (
                  <div className="compact-row" key={organization.organizationCode}>
                    <strong>{organization.name}</strong>
                    <span>{organization.organizationCode}</span>
                    <span>{organization.parentOrganizationCode ?? '最上位'}</span>
                  </div>
                ))
              )}
            </div>
          </article>

          <article className="panel master-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">役職マスタ</p>
                <h2>承認ランク</h2>
              </div>
              <span className="count-label">{positionCountLabel}</span>
            </div>
            <div className="compact-list">
              {positionsQuery.isError ? (
                <div className="compact-row compact-row-muted">役職マスタを取得できません</div>
              ) : (
                positions.slice(0, 5).map((position) => (
                  <div className="compact-row" key={position.positionCode}>
                    <strong>{position.name}</strong>
                    <span>{position.positionCode}</span>
                    <span>rank {position.approvalRank}</span>
                  </div>
                ))
              )}
            </div>
          </article>
        </section>
      </section>
    </main>
  )
}

export default App
