import { type FormEvent, useEffect, useState } from 'react'
import {
  BadgeCheck,
  Bell,
  Check,
  ClipboardList,
  FileText,
  FileSpreadsheet,
  GitBranch,
  LayoutDashboard,
  Search,
  Send,
  Settings,
  Users,
  X,
} from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import './App.css'
import {
  approveApprovalTask,
  createDraftApplication,
  type FormField,
  getApplication,
  getApplicationHistory,
  getApplications,
  getEmployees,
  getFormDefinition,
  getFormDefinitions,
  getOrganizations,
  getPendingApprovalTasks,
  getPositions,
  rejectApprovalTask,
  submitApplication,
} from './api'

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

function editorInputType(field: FormField) {
  if (field.dataType === 'NUMBER') {
    return 'number'
  }
  if (field.dataType === 'DATE') {
    return 'date'
  }
  if (field.dataType === 'MONTH') {
    return 'month'
  }
  return 'text'
}

const dateTimeFormatter = new Intl.DateTimeFormat('ja-JP', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

function formatApplicationDate(value: string) {
  return dateTimeFormatter.format(new Date(value))
}

function statusLabel(status: string) {
  if (status === 'DRAFT') {
    return '下書き'
  }
  if (status === 'SUBMITTED') {
    return '申請中'
  }
  if (status === 'APPROVED') {
    return '承認済み'
  }
  if (status === 'REJECTED') {
    return '差戻し'
  }
  return status
}

function historyActionLabel(action: string) {
  if (action === 'SUBMIT') {
    return '提出'
  }
  if (action === 'APPROVE') {
    return '承認'
  }
  if (action === 'REJECT') {
    return '否認'
  }
  return action
}

function App() {
  const queryClient = useQueryClient()
  const [selectedFormCode, setSelectedFormCode] = useState<string>()
  const [applicationTitle, setApplicationTitle] = useState('')
  const [draftValues, setDraftValues] = useState<Record<string, string>>({})
  const [selectedApplicationId, setSelectedApplicationId] = useState<string>()
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
  const applicationsQuery = useQuery({
    queryKey: ['applications'],
    queryFn: getApplications,
  })
  const approvalTasksQuery = useQuery({
    queryKey: ['approvalTasks'],
    queryFn: getPendingApprovalTasks,
  })

  const employees = employeesQuery.data ?? []
  const organizations = organizationsQuery.data ?? []
  const positions = positionsQuery.data ?? []
  const formDefinitions = formDefinitionsQuery.data ?? []
  const workflowApplications = applicationsQuery.data ?? []
  const approvalTasks = approvalTasksQuery.data ?? []
  const activeFormCode = selectedFormCode ?? formDefinitions[0]?.formCode
  const activeApplicationId = selectedApplicationId ?? workflowApplications[0]?.id
  const selectedFormQuery = useQuery({
    queryKey: ['formDefinition', activeFormCode],
    queryFn: () => getFormDefinition(activeFormCode ?? ''),
    enabled: Boolean(activeFormCode),
  })
  const selectedApplicationQuery = useQuery({
    queryKey: ['application', activeApplicationId],
    queryFn: () => getApplication(activeApplicationId ?? ''),
    enabled: Boolean(activeApplicationId),
  })
  const applicationHistoryQuery = useQuery({
    queryKey: ['applicationHistory', activeApplicationId],
    queryFn: () => getApplicationHistory(activeApplicationId ?? ''),
    enabled: Boolean(activeApplicationId),
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
  const applicationCountLabel = applicationsQuery.isError
    ? '取得エラー'
    : applicationsQuery.isLoading
      ? '読込中'
      : `${workflowApplications.length}件`
  const approvalTaskCountLabel = approvalTasksQuery.isError
    ? '取得エラー'
    : approvalTasksQuery.isLoading
      ? '読込中'
      : `${approvalTasks.length}件`
  const selectedForm = selectedFormQuery.data
  const selectedApplication = selectedApplicationQuery.data
  const applicationHistory = applicationHistoryQuery.data ?? []
  const createDraftMutation = useMutation({
    mutationFn: createDraftApplication,
    onSuccess: (savedApplication) => {
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      setSelectedApplicationId(savedApplication.id)
    },
  })
  const submitApplicationMutation = useMutation({
    mutationFn: submitApplication,
    onSuccess: (submittedApplication) => {
      queryClient.setQueryData(['application', submittedApplication.id], submittedApplication)
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      void queryClient.invalidateQueries({ queryKey: ['approvalTasks'] })
      void queryClient.invalidateQueries({ queryKey: ['applicationHistory', submittedApplication.id] })
    },
  })
  const approveTaskMutation = useMutation({
    mutationFn: approveApprovalTask,
    onSuccess: (result) => {
      setSelectedApplicationId(result.applicationId)
      void queryClient.invalidateQueries({ queryKey: ['approvalTasks'] })
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      void queryClient.invalidateQueries({ queryKey: ['application', result.applicationId] })
      void queryClient.invalidateQueries({ queryKey: ['applicationHistory', result.applicationId] })
    },
  })
  const rejectTaskMutation = useMutation({
    mutationFn: rejectApprovalTask,
    onSuccess: (result) => {
      setSelectedApplicationId(result.applicationId)
      void queryClient.invalidateQueries({ queryKey: ['approvalTasks'] })
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      void queryClient.invalidateQueries({ queryKey: ['application', result.applicationId] })
      void queryClient.invalidateQueries({ queryKey: ['applicationHistory', result.applicationId] })
    },
  })

  useEffect(() => {
    setApplicationTitle('')
    setDraftValues({})
    createDraftMutation.reset()
  }, [activeFormCode])

  useEffect(() => {
    submitApplicationMutation.reset()
    approveTaskMutation.reset()
    rejectTaskMutation.reset()
  }, [activeApplicationId])

  function updateDraftValue(fieldKey: string, value: string) {
    setDraftValues((currentValues) => ({
      ...currentValues,
      [fieldKey]: value,
    }))
  }

  function submitDraftApplication(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!activeFormCode || !applicationTitle.trim()) {
      return
    }

    createDraftMutation.mutate({
      formCode: activeFormCode,
      title: applicationTitle,
      values: draftValues,
    })
  }

  function submitSelectedApplication() {
    if (!selectedApplication || selectedApplication.status !== 'DRAFT') {
      return
    }

    submitApplicationMutation.mutate(selectedApplication.id)
  }

  function approveTask(taskId: string) {
    approveTaskMutation.mutate(taskId)
  }

  function rejectTask(taskId: string) {
    rejectTaskMutation.mutate(taskId)
  }

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
            <strong>{approvalTasksQuery.isLoading ? '-' : approvalTasks.length}</strong>
            <small>{approvalTasksQuery.isError ? '取得エラー' : '承認タスク'}</small>
          </article>
          <article>
            <span>申請中</span>
            <strong>{applicationsQuery.isLoading ? '-' : workflowApplications.length}</strong>
            <small>{applicationsQuery.isError ? '取得エラー' : '保存済み申請'}</small>
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
              <span className="count-label">{applicationCountLabel}</span>
            </div>

            <div className="table">
              <div className="table-row table-head">
                <span>件名</span>
                <span>申請書</span>
                <span>申請者</span>
                <span>ステータス</span>
                <span>更新日</span>
              </div>
              {applicationsQuery.isError ? (
                <div className="table-empty">申請一覧を取得できません</div>
              ) : applicationsQuery.isLoading ? (
                <div className="table-empty">申請一覧を読込中</div>
              ) : workflowApplications.length === 0 ? (
                <div className="table-empty">まだ保存された申請はありません</div>
              ) : (
                workflowApplications.map((application) => (
                  <button
                    className={
                      activeApplicationId === application.id
                        ? 'table-row application-row active'
                        : 'table-row application-row'
                    }
                    key={application.id}
                    onClick={() => setSelectedApplicationId(application.id)}
                    type="button"
                  >
                    <strong>{application.title}</strong>
                    <span>{application.formName}</span>
                    <span>{application.applicantName}</span>
                    <span className="status-pill">{statusLabel(application.status)}</span>
                    <span>{formatApplicationDate(application.createdAt)}</span>
                  </button>
                ))
              )}
            </div>
          </article>

          <article className="panel application-detail-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">申請詳細</p>
                <h2>入力内容</h2>
              </div>
              <FileText size={20} aria-hidden="true" />
            </div>
            {selectedApplicationQuery.isError ? (
              <div className="compact-row compact-row-muted">申請詳細を取得できません</div>
            ) : selectedApplicationQuery.isLoading && activeApplicationId ? (
              <div className="compact-row">申請詳細を読込中</div>
            ) : selectedApplication ? (
              <div className="application-detail">
                <div className="application-detail-title">
                  <span>{selectedApplication.applicationNumber}</span>
                  <strong>{selectedApplication.title}</strong>
                  <small>
                    {selectedApplication.formName} / {selectedApplication.applicantName}
                  </small>
                </div>
                <div className="detail-meta-grid">
                  <span>{statusLabel(selectedApplication.status)}</span>
                  <span>{formatApplicationDate(selectedApplication.createdAt)}</span>
                </div>
                <div className="detail-value-list">
                  {selectedApplication.values.map((fieldValue) => (
                    <div className="detail-value-row" key={fieldValue.fieldKey}>
                      <span>{fieldValue.label}</span>
                      <strong>{fieldValue.value || '-'}</strong>
                    </div>
                  ))}
                </div>
                <div className="detail-action-bar">
                  {submitApplicationMutation.isError ? (
                    <span className="draft-message error">申請を提出できません</span>
                  ) : null}
                  {submitApplicationMutation.isSuccess ? (
                    <span className="draft-message">申請を提出しました</span>
                  ) : null}
                  {selectedApplication.status === 'DRAFT' ? (
                    <button
                      className="primary-button detail-submit-button"
                      disabled={submitApplicationMutation.isPending}
                      onClick={submitSelectedApplication}
                      type="button"
                    >
                      <Send size={15} aria-hidden="true" />
                      {submitApplicationMutation.isPending ? '提出中' : '申請する'}
                    </button>
                  ) : (
                    <span className="detail-state-note">提出済み</span>
                  )}
                </div>
                <div className="history-block">
                  <div className="history-header">
                    <strong>承認履歴</strong>
                    <span>{applicationHistory.length}件</span>
                  </div>
                  {applicationHistoryQuery.isError ? (
                    <div className="compact-row compact-row-muted">承認履歴を取得できません</div>
                  ) : applicationHistoryQuery.isLoading ? (
                    <div className="compact-row">承認履歴を読込中</div>
                  ) : applicationHistory.length === 0 ? (
                    <div className="compact-row">まだ履歴はありません</div>
                  ) : (
                    <div className="history-list">
                      {applicationHistory.map((history) => (
                        <div className="history-row" key={history.id}>
                          <span>{historyActionLabel(history.action)}</span>
                          <strong>{history.actorName}</strong>
                          <small>{history.comment || '-'}</small>
                          <time>{formatApplicationDate(history.createdAt)}</time>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="compact-row">申請を選択してください</div>
            )}
          </article>
        </section>

        <section className="approval-grid" aria-label="承認タスク">
          <article className="panel approval-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">承認タスク</p>
                <h2>確認待ちの申請</h2>
              </div>
              <span className="count-label">{approvalTaskCountLabel}</span>
            </div>

            {approvalTasksQuery.isError ? (
              <div className="compact-row compact-row-muted">承認タスクを取得できません</div>
            ) : approvalTasksQuery.isLoading ? (
              <div className="compact-row">承認タスクを読込中</div>
            ) : approvalTasks.length === 0 ? (
              <div className="compact-row">現在の承認待ちはありません</div>
            ) : (
              <div className="approval-task-list">
                {approvalTasks.map((task) => {
                  const isActing =
                    (approveTaskMutation.isPending && approveTaskMutation.variables === task.id) ||
                    (rejectTaskMutation.isPending && rejectTaskMutation.variables === task.id)
                  return (
                    <div className="approval-task-row" key={task.id}>
                      <button
                        className="approval-task-main"
                        onClick={() => setSelectedApplicationId(task.applicationId)}
                        type="button"
                      >
                        <span>{task.stepName}</span>
                        <strong>{task.title}</strong>
                        <small>
                          {task.formName} / 期限 {task.dueDate ?? '-'}
                        </small>
                      </button>
                      <div className="approval-task-actions">
                        <button
                          className="icon-action approve"
                          disabled={isActing}
                          onClick={() => approveTask(task.id)}
                          title="承認"
                          type="button"
                        >
                          <Check size={16} aria-hidden="true" />
                        </button>
                        <button
                          className="icon-action reject"
                          disabled={isActing}
                          onClick={() => rejectTask(task.id)}
                          title="否認"
                          type="button"
                        >
                          <X size={16} aria-hidden="true" />
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </article>
        </section>

        <section className="draft-grid" aria-label="新規申請">
          <article className="panel draft-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">新規申請</p>
                <h2>申請フォーム入力</h2>
              </div>
              <span className="count-label">DRAFT</span>
            </div>

            <form className="draft-layout" onSubmit={submitDraftApplication}>
              <div className="draft-meta">
                <label className="draft-field">
                  <span>申請書</span>
                  <select
                    value={activeFormCode ?? ''}
                    onChange={(event) => setSelectedFormCode(event.target.value)}
                  >
                    {formDefinitions.map((formDefinition) => (
                      <option key={formDefinition.formCode} value={formDefinition.formCode}>
                        {formDefinition.formName}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="draft-field">
                  <span>件名</span>
                  <input
                    required
                    placeholder="例：東京本社への出張申請"
                    value={applicationTitle}
                    onChange={(event) => setApplicationTitle(event.target.value)}
                  />
                </label>

                <div className="draft-status-box">
                  <strong>{selectedForm?.workflowName ?? 'ワークフロー未選択'}</strong>
                  <span>申請者：山田 太郎</span>
                  <span>保存後ステータス：DRAFT</span>
                </div>
              </div>

              <div className="draft-form">
                {(selectedForm?.fields ?? []).map((field) => (
                  <label className="draft-field" key={field.fieldKey}>
                    <span>
                      {field.label}
                      {field.required ? <em>必須</em> : null}
                    </span>
                    {field.dataType === 'TEXTAREA' ? (
                      <textarea
                        placeholder={field.placeholder ?? ''}
                        required={field.required}
                        value={draftValues[field.fieldKey] ?? ''}
                        onChange={(event) => updateDraftValue(field.fieldKey, event.target.value)}
                      />
                    ) : (
                      <input
                        placeholder={field.placeholder ?? ''}
                        required={field.required}
                        type={editorInputType(field)}
                        value={draftValues[field.fieldKey] ?? ''}
                        onChange={(event) => updateDraftValue(field.fieldKey, event.target.value)}
                      />
                    )}
                  </label>
                ))}
              </div>

              <div className="draft-actions">
                {createDraftMutation.isError ? (
                  <span className="draft-message error">申請を保存できません</span>
                ) : null}
                {createDraftMutation.data ? (
                  <span className="draft-message">
                    {createDraftMutation.data.applicationNumber} を保存しました
                  </span>
                ) : null}
                <button className="primary-button" disabled={!activeFormCode || createDraftMutation.isPending}>
                  {createDraftMutation.isPending ? '保存中' : '下書き保存'}
                </button>
              </div>
            </form>
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
