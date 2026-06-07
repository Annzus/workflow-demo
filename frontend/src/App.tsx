import { type FormEvent, useState } from 'react'
import {
  applyNodeChanges,
  Background,
  Controls,
  ReactFlow,
  type Edge as FlowEdge,
  type Node as FlowNode,
  type NodeChange,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import {
  BadgeCheck,
  Bell,
  Check,
  ClipboardList,
  FileUp,
  FileText,
  FileSpreadsheet,
  GitBranch,
  LayoutDashboard,
  LogIn,
  LogOut,
  Search,
  Send,
  Settings,
  UserCircle,
  Users,
  X,
} from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import './App.css'
import {
  approveApprovalTask,
  buildBasicAuthorization,
  clearAuthorization,
  createDraftApplication,
  DEMO_ACCOUNTS,
  DEMO_PASSWORD,
  DEMO_USERNAME,
  type FormField,
  getApplication,
  getApplicationAttachments,
  getApplicationHistory,
  getApplications,
  getEmployees,
  getFormDefinition,
  getFormDefinitions,
  getMe,
  getOrganizations,
  getPendingApprovalTasks,
  getPositions,
  getSavedAuthorization,
  getWorkflowDefinition,
  getWorkflowDefinitions,
  type WorkflowEdge,
  type WorkflowNode,
  publishWorkflowDraft,
  rejectApprovalTask,
  saveAuthorization,
  saveFormDefinition,
  saveWorkflowDraft,
  submitApplication,
  uploadApplicationAttachment,
} from './api'

type SectionId = 'dashboard' | 'applications' | 'approvals' | 'workflows' | 'employees' | 'forms' | 'settings'

const navItems: Array<{ id: SectionId; label: string; icon: typeof LayoutDashboard }> = [
  { id: 'dashboard', label: 'ダッシュボード', icon: LayoutDashboard },
  { id: 'applications', label: '申請者フロー', icon: ClipboardList },
  { id: 'approvals', label: '承認者フロー', icon: BadgeCheck },
  { id: 'workflows', label: '承認ルート設定', icon: GitBranch },
  { id: 'forms', label: '申請書定義', icon: FileSpreadsheet },
  { id: 'employees', label: '社員マスタ', icon: Users },
  { id: 'settings', label: 'デモ情報', icon: Settings },
]

const sectionMeta: Record<SectionId, { eyebrow: string; title: string }> = {
  dashboard: {
    eyebrow: 'ワークフロー運用',
    title: '運用ダッシュボード',
  },
  applications: {
    eyebrow: '申請者フロー',
    title: '申請作成・提出',
  },
  approvals: {
    eyebrow: '承認者フロー',
    title: '承認タスク・履歴',
  },
  workflows: {
    eyebrow: '管理者設定',
    title: '承認ルート設定',
  },
  employees: {
    eyebrow: '管理者設定',
    title: '社員・組織・役職マスタ',
  },
  forms: {
    eyebrow: '管理者設定',
    title: '申請書定義',
  },
  settings: {
    eyebrow: 'ポートフォリオ',
    title: 'デモ環境と確認ポイント',
  },
}

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

function createDefaultField(displayOrder: number): FormField {
  return {
    fieldKey: `FIELD_${displayOrder}`,
    label: '新規項目',
    dataType: 'TEXT',
    required: false,
    placeholder: '',
    initialValueType: 'MANUAL',
    displayOrder,
  }
}

function createDefaultWorkflowNode(displayOrder: number): WorkflowNode {
  return {
    nodeKey: `NODE_${displayOrder}`,
    nodeName: '承認ノード',
    nodeType: 'APPROVAL',
    approverType: 'FIXED_EMPLOYEE',
    positionCode: null,
    employeeCode: '1005',
    displayOrder,
    xPosition: 160 + displayOrder * 12,
    yPosition: 120,
  }
}

function fieldDataTypeLabel(dataType: string) {
  if (dataType === 'TEXT') {
    return '一行テキスト'
  }
  if (dataType === 'TEXTAREA') {
    return '複数行テキスト'
  }
  if (dataType === 'NUMBER') {
    return '数値'
  }
  if (dataType === 'DATE') {
    return '日付'
  }
  if (dataType === 'MONTH') {
    return '年月'
  }
  return dataType
}

function workflowNodeTypeLabel(nodeType: string) {
  if (nodeType === 'APPLICANT') {
    return '申請者'
  }
  if (nodeType === 'APPROVAL') {
    return '承認'
  }
  if (nodeType === 'BRANCH') {
    return '条件分岐'
  }
  if (nodeType === 'END') {
    return '完了'
  }
  return nodeType
}

function approverTypeLabel(approverType: string | null) {
  if (!approverType) {
    return '承認者なし'
  }
  if (approverType === 'FIXED_EMPLOYEE') {
    return '社員を指定'
  }
  if (approverType === 'POSITION') {
    return '役職を指定'
  }
  return approverType
}

function workflowNodesToFlowNodes(nodes: WorkflowNode[]): FlowNode[] {
  return nodes.map((node) => ({
    id: node.nodeKey,
    position: { x: node.xPosition, y: node.yPosition },
    data: {
      label: `${node.nodeName} / ${workflowNodeTypeLabel(node.nodeType)}`,
    },
    type: 'default',
  }))
}

function workflowEdgesToFlowEdges(edges: WorkflowEdge[]): FlowEdge[] {
  return edges.map((edge) => ({
    id: `${edge.sourceNodeKey}-${edge.targetNodeKey}-${edge.displayOrder}`,
    source: edge.sourceNodeKey,
    target: edge.targetNodeKey,
    label: edge.conditionExpression ?? undefined,
  }))
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

function formatFileSize(sizeBytes: number) {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`
  }
  if (sizeBytes < 1024 * 1024) {
    return `${(sizeBytes / 1024).toFixed(1)} KB`
  }
  return `${(sizeBytes / 1024 / 1024).toFixed(1)} MB`
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

function routeStatusLabel(status: string) {
  if (status === 'COMPLETED') {
    return '完了'
  }
  if (status === 'CURRENT') {
    return '対応中'
  }
  if (status === 'REJECTED') {
    return '差戻し'
  }
  if (status === 'WAITING') {
    return '待機'
  }
  return status
}

function App() {
  const queryClient = useQueryClient()
  const [activeSection, setActiveSection] = useState<SectionId>('dashboard')
  const [authorization, setAuthorization] = useState<string | null>(() => getSavedAuthorization())
  const [loginUsername, setLoginUsername] = useState<string>(DEMO_USERNAME)
  const [loginPassword, setLoginPassword] = useState<string>(DEMO_PASSWORD)
  const [selectedFormCode, setSelectedFormCode] = useState<string>()
  const [selectedWorkflowCode, setSelectedWorkflowCode] = useState<string>()
  const [formEditorOverride, setFormEditor] = useState<{
    formCode: string
    formName: string
    workflowCode: string
    fields: FormField[]
  } | null>(null)
  const [workflowEditorOverride, setWorkflowEditor] = useState<{
    workflowCode: string
    workflowName: string
    nodes: WorkflowNode[]
    edges: WorkflowEdge[]
  } | null>(null)
  const [applicationTitle, setApplicationTitle] = useState('')
  const [draftValues, setDraftValues] = useState<Record<string, string>>({})
  const [selectedApplicationId, setSelectedApplicationId] = useState<string>()
  const [attachmentFile, setAttachmentFile] = useState<File | null>(null)
  const [attachmentInputVersion, setAttachmentInputVersion] = useState(0)
  const currentUserQuery = useQuery({
    queryKey: ['me', authorization],
    queryFn: () => getMe(authorization ?? ''),
    enabled: Boolean(authorization),
    retry: false,
  })
  const currentUser = currentUserQuery.data
  const isAuthenticated = Boolean(authorization && currentUser)
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
  const workflowDefinitionsQuery = useQuery({
    queryKey: ['workflowDefinitions'],
    queryFn: getWorkflowDefinitions,
  })
  const applicationsQuery = useQuery({
    queryKey: ['applications'],
    queryFn: getApplications,
    enabled: isAuthenticated,
  })
  const approvalTasksQuery = useQuery({
    queryKey: ['approvalTasks'],
    queryFn: getPendingApprovalTasks,
    enabled: isAuthenticated,
  })

  const employees = employeesQuery.data ?? []
  const organizations = organizationsQuery.data ?? []
  const positions = positionsQuery.data ?? []
  const formDefinitions = formDefinitionsQuery.data ?? []
  const workflowDefinitions = workflowDefinitionsQuery.data ?? []
  const workflowApplications = applicationsQuery.data ?? []
  const approvalTasks = approvalTasksQuery.data ?? []
  const activeFormCode = selectedFormCode ?? formDefinitions[0]?.formCode
  const activeFormSummary = formDefinitions.find((formDefinition) => formDefinition.formCode === activeFormCode)
  const activeWorkflowCode = selectedWorkflowCode ?? activeFormSummary?.workflowCode ?? workflowDefinitions[0]?.workflowCode
  const activeApplicationId = selectedApplicationId ?? workflowApplications[0]?.id
  const selectedFormQuery = useQuery({
    queryKey: ['formDefinition', activeFormCode],
    queryFn: () => getFormDefinition(activeFormCode ?? ''),
    enabled: Boolean(activeFormCode),
  })
  const selectedWorkflowQuery = useQuery({
    queryKey: ['workflowDefinition', activeWorkflowCode],
    queryFn: () => getWorkflowDefinition(activeWorkflowCode ?? ''),
    enabled: Boolean(activeWorkflowCode),
  })
  const selectedApplicationQuery = useQuery({
    queryKey: ['application', activeApplicationId],
    queryFn: () => getApplication(activeApplicationId ?? ''),
    enabled: isAuthenticated && Boolean(activeApplicationId),
  })
  const applicationHistoryQuery = useQuery({
    queryKey: ['applicationHistory', activeApplicationId],
    queryFn: () => getApplicationHistory(activeApplicationId ?? ''),
    enabled: isAuthenticated && Boolean(activeApplicationId),
  })
  const applicationAttachmentsQuery = useQuery({
    queryKey: ['applicationAttachments', activeApplicationId],
    queryFn: () => getApplicationAttachments(activeApplicationId ?? ''),
    enabled: isAuthenticated && Boolean(activeApplicationId),
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
  const workflowDefinitionCountLabel = workflowDefinitionsQuery.isError
    ? '取得エラー'
    : workflowDefinitionsQuery.isLoading
      ? '読込中'
      : `${workflowDefinitions.length}件`
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
  const selectedWorkflow = selectedWorkflowQuery.data
  const formEditor = formEditorOverride ?? {
    formCode: selectedForm?.formCode ?? '',
    formName: selectedForm?.formName ?? '',
    workflowCode: selectedForm?.workflowCode ?? workflowDefinitions[0]?.workflowCode ?? '',
    fields: selectedForm?.fields ?? [],
  }
  const workflowEditor = workflowEditorOverride ?? {
    workflowCode: activeWorkflowCode ?? '',
    workflowName: selectedWorkflow?.workflowName ?? '',
    nodes: selectedWorkflow?.nodes ?? [],
    edges: selectedWorkflow?.edges ?? [],
  }
  const selectedApplication = selectedApplicationQuery.data
  const applicationHistory = applicationHistoryQuery.data ?? []
  const applicationAttachments = applicationAttachmentsQuery.data ?? []
  const selectedApprovalRoute = selectedApplication?.approvalRoute ?? []
  const workflowFlowNodes = workflowNodesToFlowNodes(workflowEditor.nodes)
  const workflowFlowEdges = workflowEdgesToFlowEdges(workflowEditor.edges)
  const activeSectionMeta = sectionMeta[activeSection]
  const showApplicationWorkspace =
    activeSection === 'dashboard' || activeSection === 'applications' || activeSection === 'approvals'
  const loginMutation = useMutation({
    mutationFn: async (credentials: { username: string; password: string }) => {
      const nextAuthorization = buildBasicAuthorization(credentials.username, credentials.password)
      const user = await getMe(nextAuthorization)
      return { authorization: nextAuthorization, user }
    },
    onSuccess: (result) => {
      saveAuthorization(result.authorization)
      setAuthorization(result.authorization)
      queryClient.setQueryData(['me', result.authorization], result.user)
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      void queryClient.invalidateQueries({ queryKey: ['approvalTasks'] })
    },
  })
  const saveFormDefinitionMutation = useMutation({
    mutationFn: saveFormDefinition,
    onSuccess: (savedFormDefinition) => {
      setSelectedFormCode(savedFormDefinition.formCode)
      queryClient.setQueryData(['formDefinition', savedFormDefinition.formCode], savedFormDefinition)
      void queryClient.invalidateQueries({ queryKey: ['formDefinitions'] })
    },
  })
  const saveWorkflowDraftMutation = useMutation({
    mutationFn: () =>
      saveWorkflowDraft(workflowEditor.workflowCode, {
        workflowName: workflowEditor.workflowName,
        nodes: workflowEditor.nodes,
        edges: workflowEditor.edges,
      }),
    onSuccess: (savedWorkflowDraft) => {
      setSelectedWorkflowCode(savedWorkflowDraft.workflowCode)
      setWorkflowEditor({
        workflowCode: savedWorkflowDraft.workflowCode,
        workflowName: savedWorkflowDraft.workflowName,
        nodes: savedWorkflowDraft.nodes,
        edges: savedWorkflowDraft.edges,
      })
    },
  })
  const publishWorkflowDraftMutation = useMutation({
    mutationFn: async () => {
      await saveWorkflowDraft(workflowEditor.workflowCode, {
        workflowName: workflowEditor.workflowName,
        nodes: workflowEditor.nodes,
        edges: workflowEditor.edges,
      })
      return publishWorkflowDraft(workflowEditor.workflowCode)
    },
    onSuccess: (publishedWorkflow) => {
      setSelectedWorkflowCode(publishedWorkflow.workflowCode)
      setWorkflowEditor({
        workflowCode: publishedWorkflow.workflowCode,
        workflowName: publishedWorkflow.workflowName,
        nodes: publishedWorkflow.nodes,
        edges: publishedWorkflow.edges,
      })
      void queryClient.invalidateQueries({ queryKey: ['workflowDefinitions'] })
      void queryClient.invalidateQueries({ queryKey: ['workflowDefinition', publishedWorkflow.workflowCode] })
      void queryClient.invalidateQueries({ queryKey: ['formDefinitions'] })
    },
  })
  const createDraftMutation = useMutation({
    mutationFn: createDraftApplication,
    onSuccess: (savedApplication) => {
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      selectApplication(savedApplication.id)
    },
  })
  const uploadAttachmentMutation = useMutation({
    mutationFn: ({ applicationId, file }: { applicationId: string; file: File }) =>
      uploadApplicationAttachment(applicationId, file),
    onSuccess: (_, variables) => {
      setAttachmentFile(null)
      setAttachmentInputVersion((version) => version + 1)
      void queryClient.invalidateQueries({ queryKey: ['applicationAttachments', variables.applicationId] })
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
      selectApplication(result.applicationId)
      void queryClient.invalidateQueries({ queryKey: ['approvalTasks'] })
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      void queryClient.invalidateQueries({ queryKey: ['application', result.applicationId] })
      void queryClient.invalidateQueries({ queryKey: ['applicationHistory', result.applicationId] })
    },
  })
  const rejectTaskMutation = useMutation({
    mutationFn: rejectApprovalTask,
    onSuccess: (result) => {
      selectApplication(result.applicationId)
      void queryClient.invalidateQueries({ queryKey: ['approvalTasks'] })
      void queryClient.invalidateQueries({ queryKey: ['applications'] })
      void queryClient.invalidateQueries({ queryKey: ['application', result.applicationId] })
      void queryClient.invalidateQueries({ queryKey: ['applicationHistory', result.applicationId] })
    },
  })

  function updateDraftValue(fieldKey: string, value: string) {
    setDraftValues((currentValues) => ({
      ...currentValues,
      [fieldKey]: value,
    }))
  }

  function updateFormField(index: number, field: Partial<FormField>) {
    setFormEditor((currentEditor) => {
      const baseEditor = currentEditor ?? formEditor
      return {
        ...baseEditor,
        fields: baseEditor.fields.map((currentField, currentIndex) =>
        currentIndex === index ? { ...currentField, ...field } : currentField,
        ),
      }
    })
  }

  function addFormField() {
    setFormEditor((currentEditor) => {
      const baseEditor = currentEditor ?? formEditor
      return {
        ...baseEditor,
        fields: [...baseEditor.fields, createDefaultField((baseEditor.fields.length + 1) * 10)],
      }
    })
  }

  function removeFormField(index: number) {
    setFormEditor((currentEditor) => {
      const baseEditor = currentEditor ?? formEditor
      return {
        ...baseEditor,
        fields: baseEditor.fields.filter((_, currentIndex) => currentIndex !== index),
      }
    })
  }

  function submitFormDefinition(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!formEditor.formCode.trim() || !formEditor.formName.trim() || !formEditor.workflowCode.trim()) {
      return
    }

    saveFormDefinitionMutation.mutate(formEditor)
  }

  function updateWorkflowNode(index: number, node: Partial<WorkflowNode>) {
    setWorkflowEditor((currentEditor) => {
      const baseEditor = currentEditor ?? workflowEditor
      return {
        ...baseEditor,
        nodes: baseEditor.nodes.map((currentNode, currentIndex) =>
        currentIndex === index ? { ...currentNode, ...node } : currentNode,
        ),
      }
    })
  }

  function updateWorkflowEdge(index: number, edge: Partial<WorkflowEdge>) {
    setWorkflowEditor((currentEditor) => {
      const baseEditor = currentEditor ?? workflowEditor
      return {
        ...baseEditor,
        edges: baseEditor.edges.map((currentEdge, currentIndex) =>
        currentIndex === index ? { ...currentEdge, ...edge } : currentEdge,
        ),
      }
    })
  }

  function addWorkflowNode() {
    setWorkflowEditor((currentEditor) => {
      const baseEditor = currentEditor ?? workflowEditor
      return {
        ...baseEditor,
        nodes: [...baseEditor.nodes, createDefaultWorkflowNode((baseEditor.nodes.length + 1) * 10)],
      }
    })
  }

  function removeWorkflowNode(index: number) {
    setWorkflowEditor((currentEditor) => {
      const baseEditor = currentEditor ?? workflowEditor
      const removedNodeKey = baseEditor.nodes[index]?.nodeKey
      return {
        ...baseEditor,
        nodes: baseEditor.nodes.filter((_, currentIndex) => currentIndex !== index),
        edges: baseEditor.edges.filter(
          (edge) => edge.sourceNodeKey !== removedNodeKey && edge.targetNodeKey !== removedNodeKey,
        ),
      }
    })
  }

  function addWorkflowEdge() {
    setWorkflowEditor((currentEditor) => {
      const baseEditor = currentEditor ?? workflowEditor
      return {
        ...baseEditor,
        edges: [
          ...baseEditor.edges,
        {
          sourceNodeKey: baseEditor.nodes[0]?.nodeKey ?? '',
          targetNodeKey: baseEditor.nodes[1]?.nodeKey ?? '',
          conditionExpression: null,
          displayOrder: (baseEditor.edges.length + 1) * 10,
        },
      ],
      }
    })
  }

  function onWorkflowNodesChange(changes: NodeChange[]) {
    const nextFlowNodes = applyNodeChanges(changes, workflowFlowNodes)
    setWorkflowEditor((currentEditor) => ({
      ...(currentEditor ?? workflowEditor),
      nodes: (currentEditor ?? workflowEditor).nodes.map((node) => {
        const flowNode = nextFlowNodes.find((nextNode) => nextNode.id === node.nodeKey)
        return flowNode
          ? {
              ...node,
              xPosition: Math.round(flowNode.position.x),
              yPosition: Math.round(flowNode.position.y),
            }
          : node
      }),
    }))
  }

  function submitWorkflowDraft(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!workflowEditor.workflowCode.trim() || !workflowEditor.workflowName.trim()) {
      return
    }

    saveWorkflowDraftMutation.mutate()
  }

  function publishWorkflowDraftAction() {
    if (!workflowEditor.workflowCode.trim()) {
      return
    }

    publishWorkflowDraftMutation.mutate()
  }

  function selectForm(formCode: string) {
    setSelectedFormCode(formCode)
    setFormEditor(null)
    setApplicationTitle('')
    setDraftValues({})
    createDraftMutation.reset()
  }

  function selectWorkflow(workflowCode: string) {
    setSelectedWorkflowCode(workflowCode)
    setWorkflowEditor(null)
  }

  function selectApplication(applicationId: string) {
    setSelectedApplicationId(applicationId)
    setAttachmentFile(null)
    setAttachmentInputVersion((version) => version + 1)
    uploadAttachmentMutation.reset()
    submitApplicationMutation.reset()
    approveTaskMutation.reset()
    rejectTaskMutation.reset()
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

  function submitAttachment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!activeApplicationId || !attachmentFile) {
      return
    }

    uploadAttachmentMutation.mutate({
      applicationId: activeApplicationId,
      file: attachmentFile,
    })
  }

  function submitLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!loginUsername.trim() || !loginPassword) {
      return
    }

    loginMutation.mutate({
      username: loginUsername.trim(),
      password: loginPassword,
    })
  }

  function logout() {
    clearAuthorization()
    setAuthorization(null)
    setSelectedApplicationId(undefined)
    setAttachmentFile(null)
    setAttachmentInputVersion((version) => version + 1)
    loginMutation.reset()
    queryClient.clear()
  }

  if (!authorization || currentUserQuery.isError) {
    return (
      <main className="login-shell">
        <section className="login-panel" aria-label="ログイン">
          <div className="login-brand">
            <div className="brand-mark">W</div>
            <div>
              <p className="eyebrow">社内ワークフロー</p>
              <h1>ログイン</h1>
            </div>
          </div>

          <form className="login-form" onSubmit={submitLogin}>
            <label className="draft-field">
              <span>メールアドレス</span>
              <input
                autoComplete="username"
                required
                type="email"
                value={loginUsername}
                onChange={(event) => setLoginUsername(event.target.value)}
              />
            </label>
            <label className="draft-field">
              <span>パスワード</span>
              <input
                autoComplete="current-password"
                required
                type="password"
                value={loginPassword}
                onChange={(event) => setLoginPassword(event.target.value)}
              />
            </label>

            {loginMutation.isError || currentUserQuery.isError ? (
              <span className="draft-message error">ログイン情報を確認してください</span>
            ) : null}

            <button className="primary-button login-button" disabled={loginMutation.isPending}>
              <LogIn size={16} aria-hidden="true" />
              {loginMutation.isPending ? '確認中' : 'ログイン'}
            </button>
          </form>

          <div className="login-demo-card">
            <strong>デモアカウント</strong>
            {DEMO_ACCOUNTS.map((account) => (
              <button
                className="login-demo-account"
                key={account.username}
                onClick={() => {
                  setLoginUsername(account.username)
                  setLoginPassword(account.password)
                  loginMutation.reset()
                }}
                type="button"
              >
                <span>{account.label}</span>
                <strong>{account.employeeName}</strong>
                <small>{account.username} / {account.password}</small>
              </button>
            ))}
          </div>
        </section>
      </main>
    )
  }

  if (currentUserQuery.isLoading) {
    return (
      <main className="login-shell">
        <section className="login-panel login-loading" aria-label="認証確認">
          <div className="brand-mark">W</div>
          <strong>認証状態を確認中</strong>
        </section>
      </main>
    )
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

        <nav className="nav-list" aria-label="メインナビゲーション">
          {navItems.map((item) => (
            <button
              className={activeSection === item.id ? 'nav-item active' : 'nav-item'}
              key={item.id}
              onClick={() => setActiveSection(item.id)}
              type="button"
            >
              <item.icon size={18} aria-hidden="true" />
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">{activeSectionMeta.eyebrow}</p>
            <h1>{activeSectionMeta.title}</h1>
          </div>
          <div className="topbar-actions">
            <div className="user-chip" aria-label="現在のユーザー">
              <UserCircle size={18} aria-hidden="true" />
              <div>
                <strong>{currentUser?.name}</strong>
                <span>{currentUser?.organizationName} / {currentUser?.positionName}</span>
              </div>
            </div>
            <label className="search">
              <Search size={17} aria-hidden="true" />
              <input aria-label="申請検索" placeholder="申請を検索" />
            </label>
            <button className="icon-button" aria-label="通知">
              <Bell size={18} aria-hidden="true" />
            </button>
            <button className="icon-button" aria-label="ログアウト" onClick={logout} title="ログアウト">
              <LogOut size={18} aria-hidden="true" />
            </button>
          </div>
        </header>

        {activeSection === 'dashboard' ? (
        <>
        <section className="summary-grid" aria-label="ワークフロー概要">
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

        <section className="section-guide-grid" aria-label="主要フロー">
          <button className="guide-card" onClick={() => setActiveSection('applications')} type="button">
            <span>申請者フロー</span>
            <strong>下書き作成、添付、提出までを確認</strong>
            <small>動的フォームと承認ルートの紐づきを見せる画面</small>
          </button>
          <button className="guide-card" onClick={() => setActiveSection('approvals')} type="button">
            <span>承認者フロー</span>
            <strong>確認待ちタスクから承認履歴までを確認</strong>
            <small>承認・否認後の状態変化と履歴を追える画面</small>
          </button>
          <button className="guide-card" onClick={() => setActiveSection('workflows')} type="button">
            <span>管理者設定</span>
            <strong>申請書定義と承認ルートを設定</strong>
            <small>React Flow で業務ルートを編集し公開できる画面</small>
          </button>
        </section>
        </>
        ) : null}

        {showApplicationWorkspace ? (
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
                  onClick={() => selectApplication(application.id)}
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
                <div className="approval-route-block">
                  <div className="history-header">
                    <strong>承認ルート</strong>
                    <span>{selectedApprovalRoute.length}ステップ</span>
                  </div>
                  <div className="approval-route-list">
                    {selectedApprovalRoute.map((step) => (
                      <div className={`approval-route-step ${step.status.toLowerCase()}`} key={step.stepKey}>
                        <span className="route-marker" aria-hidden="true" />
                        <div className="route-main">
                          <strong>{step.stepName}</strong>
                          <span>
                            {step.actorName || step.roleName}
                            {step.actorName && step.roleName ? ` / ${step.roleName}` : ''}
                          </span>
                        </div>
                        <div className="route-status">
                          <em>{routeStatusLabel(step.status)}</em>
                          <small>{step.completedAt ? formatApplicationDate(step.completedAt) : '-'}</small>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="attachment-block">
                  <div className="history-header">
                    <strong>添付ファイル</strong>
                    <span>{applicationAttachments.length}件</span>
                  </div>
                  <form className="attachment-upload" onSubmit={submitAttachment}>
                    <label className="attachment-picker">
                      <FileUp size={16} aria-hidden="true" />
                      <span>{attachmentFile?.name ?? 'ファイルを選択'}</span>
                      <input
                        key={attachmentInputVersion}
                        aria-label="添付ファイル"
                        type="file"
                        onChange={(event) => setAttachmentFile(event.target.files?.[0] ?? null)}
                      />
                    </label>
                    <button
                      className="primary-button attachment-submit"
                      disabled={!attachmentFile || uploadAttachmentMutation.isPending}
                      type="submit"
                    >
                      {uploadAttachmentMutation.isPending ? 'アップロード中' : '追加'}
                    </button>
                  </form>
                  {uploadAttachmentMutation.isError ? (
                    <span className="draft-message error">添付ファイルを保存できません</span>
                  ) : null}
                  {uploadAttachmentMutation.isSuccess ? (
                    <span className="draft-message">添付ファイルを保存しました</span>
                  ) : null}
                  {applicationAttachmentsQuery.isError ? (
                    <div className="compact-row compact-row-muted">添付ファイルを取得できません</div>
                  ) : applicationAttachmentsQuery.isLoading ? (
                    <div className="compact-row">添付ファイルを読込中</div>
                  ) : applicationAttachments.length === 0 ? (
                    <div className="compact-row">添付ファイルはありません</div>
                  ) : (
                    <div className="attachment-list">
                      {applicationAttachments.map((attachment) => (
                        <div className="attachment-row" key={attachment.id}>
                          <FileText size={16} aria-hidden="true" />
                          <div>
                            <strong>{attachment.originalFilename}</strong>
                            <span>
                              {attachment.contentType} / {formatFileSize(attachment.sizeBytes)}
                            </span>
                          </div>
                          <small>
                            {attachment.uploadedByName} / {formatApplicationDate(attachment.uploadedAt)}
                          </small>
                        </div>
                      ))}
                    </div>
                  )}
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
        ) : null}

        {activeSection === 'approvals' ? (
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
                        onClick={() => selectApplication(task.applicationId)}
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
        ) : null}

        {activeSection === 'applications' ? (
        <section className="draft-grid" aria-label="新規申請">
          <article className="panel draft-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">新規申請</p>
                <h2>申請フォーム入力</h2>
              </div>
              <span className="count-label">下書き</span>
            </div>

            <form className="draft-layout" onSubmit={submitDraftApplication}>
              <div className="draft-meta">
                <label className="draft-field">
                  <span>申請書</span>
                  <select
                    value={activeFormCode ?? ''}
                    onChange={(event) => selectForm(event.target.value)}
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
                  <span>申請者：{currentUser?.name ?? '-'}</span>
                  <span>保存後ステータス：下書き</span>
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
        ) : null}

        {activeSection === 'workflows' ? (
        <section className="workflow-definition-grid" aria-label="ワークフロー定義">
          <article className="panel workflow-definition-panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">ワークフロー定義</p>
                <h2>承認ルート設定</h2>
              </div>
              <span className="count-label">{workflowDefinitionCountLabel}</span>
            </div>

            <div className="workflow-definition-layout">
              <div className="definition-list" aria-label="ワークフロー定義一覧">
                {workflowDefinitionsQuery.isError ? (
                  <div className="compact-row compact-row-muted">ワークフロー定義を取得できません</div>
                ) : (
                  workflowDefinitions.map((workflowDefinition) => (
                    <button
                      className={
                        activeWorkflowCode === workflowDefinition.workflowCode
                          ? 'definition-option active'
                          : 'definition-option'
                      }
                      key={workflowDefinition.workflowCode}
                      onClick={() => selectWorkflow(workflowDefinition.workflowCode)}
                      type="button"
                    >
                      <strong>{workflowDefinition.workflowName}</strong>
                      <span>{workflowDefinition.workflowCode}</span>
                      <small>公開版 {workflowDefinition.activeVersion}</small>
                      <em>{workflowDefinition.nodeCount}ノード</em>
                    </button>
                  ))
                )}
              </div>

              <div className="workflow-definition-detail">
                {selectedWorkflowQuery.isError ? (
                  <div className="compact-row compact-row-muted">選択中のワークフローを取得できません</div>
                ) : selectedWorkflowQuery.isLoading ? (
                  <div className="compact-row">ワークフローを読込中</div>
                ) : selectedWorkflow ? (
                  <>
                    <div className="definition-title">
                      <div>
                        <span>{selectedWorkflow.workflowCode}</span>
                        <h3>{selectedWorkflow.workflowName}</h3>
                      </div>
                      <small>公開版 {selectedWorkflow.activeVersion}</small>
                    </div>

                    <div className="workflow-node-list">
                      {selectedWorkflow.nodes.map((node) => (
                        <div className={`workflow-node-row ${node.nodeType.toLowerCase()}`} key={node.nodeKey}>
                          <span>{node.displayOrder}</span>
                          <strong>{node.nodeName}</strong>
                          <small>{workflowNodeTypeLabel(node.nodeType)}</small>
                          <em>
                            {node.employeeCode
                              ? `社員 ${node.employeeCode}`
                              : node.positionCode
                                ? `役職 ${node.positionCode}`
                                : approverTypeLabel(node.approverType)}
                          </em>
                        </div>
                      ))}
                    </div>

                    <div className="workflow-edge-list">
                      {selectedWorkflow.edges.map((edge) => (
                        <span key={`${edge.sourceNodeKey}-${edge.targetNodeKey}`}>
                          {edge.sourceNodeKey} から {edge.targetNodeKey}
                        </span>
                      ))}
                    </div>

                    <form className="definition-editor" onSubmit={submitWorkflowDraft}>
                      <div className="editor-toolbar">
                        <label className="draft-field">
                          <span>ワークフロー名</span>
                          <input
                            value={workflowEditor.workflowName}
                            onChange={(event) =>
                              setWorkflowEditor((currentEditor) => {
                                const baseEditor = currentEditor ?? workflowEditor
                                return {
                                  ...baseEditor,
                                  workflowName: event.target.value,
                                }
                              })
                            }
                          />
                        </label>
                        <div className="editor-actions">
                          {saveWorkflowDraftMutation.isError ? (
                            <span className="draft-message error">下書きを保存できません</span>
                          ) : null}
                          {saveWorkflowDraftMutation.data ? (
                            <span className="draft-message">下書き版 {saveWorkflowDraftMutation.data.versionNumber} を保存しました</span>
                          ) : null}
                          {publishWorkflowDraftMutation.isError ? (
                            <span className="draft-message error">公開できません</span>
                          ) : null}
                          {publishWorkflowDraftMutation.data ? (
                            <span className="draft-message">公開版 {publishWorkflowDraftMutation.data.versionNumber} を公開しました</span>
                          ) : null}
                          <button
                            className="secondary-button"
                            disabled={saveWorkflowDraftMutation.isPending}
                            type="submit"
                          >
                            {saveWorkflowDraftMutation.isPending ? '保存中' : '下書き保存'}
                          </button>
                          <button
                            className="primary-button"
                            disabled={saveWorkflowDraftMutation.isPending || publishWorkflowDraftMutation.isPending}
                            onClick={publishWorkflowDraftAction}
                            type="button"
                          >
                            {publishWorkflowDraftMutation.isPending ? '保存・公開中' : '公開'}
                          </button>
                        </div>
                      </div>

                      <div className="workflow-canvas" aria-label="ワークフロー編集キャンバス">
                        <ReactFlow
                          edges={workflowFlowEdges}
                          fitView
                          nodes={workflowFlowNodes}
                          onNodesChange={onWorkflowNodesChange}
                        >
                          <Background />
                          <Controls />
                        </ReactFlow>
                      </div>

                      <div className="editor-section-title">
                        <strong>ノード</strong>
                        <button className="icon-text-button" onClick={addWorkflowNode} type="button">
                          追加
                        </button>
                      </div>
                      <div className="workflow-editor-table">
                        {workflowEditor.nodes.map((node, index) => (
                          <div className="workflow-editor-row workflow-editor-card" key={`${node.nodeKey}-${index}`}>
                            <div className="node-badge">{index + 1}</div>
                            <label className="workflow-editor-field code-field">
                              <span>ステップID</span>
                              <input
                                value={node.nodeKey}
                                onChange={(event) => updateWorkflowNode(index, { nodeKey: event.target.value })}
                              />
                            </label>
                            <label className="workflow-editor-field wide-field">
                              <span>ステップ名</span>
                              <input
                                value={node.nodeName}
                                onChange={(event) => updateWorkflowNode(index, { nodeName: event.target.value })}
                              />
                            </label>
                            <label className="workflow-editor-field">
                              <span>種類</span>
                              <select
                                value={node.nodeType}
                                onChange={(event) => updateWorkflowNode(index, { nodeType: event.target.value })}
                              >
                                <option value="APPLICANT">申請者</option>
                                <option value="APPROVAL">承認</option>
                                <option value="BRANCH">条件分岐</option>
                                <option value="END">完了</option>
                              </select>
                            </label>
                            <label className="workflow-editor-field">
                              <span>承認者</span>
                              <select
                                value={node.approverType ?? ''}
                                onChange={(event) =>
                                  updateWorkflowNode(index, { approverType: event.target.value || null })
                                }
                              >
                                <option value="">承認者なし</option>
                                <option value="FIXED_EMPLOYEE">社員を指定</option>
                                <option value="POSITION">役職を指定</option>
                              </select>
                            </label>
                            <label className="workflow-editor-field compact-field">
                              <span>役職コード</span>
                              <input
                                placeholder="例：MGR"
                                value={node.positionCode ?? ''}
                                onChange={(event) =>
                                  updateWorkflowNode(index, { positionCode: event.target.value || null })
                                }
                              />
                            </label>
                            <label className="workflow-editor-field compact-field">
                              <span>社員コード</span>
                              <input
                                placeholder="例：1005"
                                value={node.employeeCode ?? ''}
                                onChange={(event) =>
                                  updateWorkflowNode(index, { employeeCode: event.target.value || null })
                                }
                              />
                            </label>
                            <button
                              className="icon-button"
                              onClick={() => removeWorkflowNode(index)}
                              title="ステップを削除"
                              type="button"
                            >
                              <X size={14} />
                            </button>
                          </div>
                        ))}
                      </div>

                      <div className="editor-section-title">
                        <strong>ルート接続</strong>
                        <button className="icon-text-button" onClick={addWorkflowEdge} type="button">
                          追加
                        </button>
                      </div>
                      <div className="workflow-editor-table">
                        {workflowEditor.edges.map((edge, index) => (
                          <div className="workflow-editor-row edge workflow-edge-card" key={`${edge.sourceNodeKey}-${edge.targetNodeKey}-${index}`}>
                            <label className="workflow-editor-field">
                              <span>開始ステップ</span>
                              <select
                                value={edge.sourceNodeKey}
                                onChange={(event) => updateWorkflowEdge(index, { sourceNodeKey: event.target.value })}
                              >
                                {workflowEditor.nodes.map((node) => (
                                  <option key={node.nodeKey} value={node.nodeKey}>
                                    {node.nodeName}
                                  </option>
                                ))}
                              </select>
                            </label>
                            <label className="workflow-editor-field">
                              <span>次のステップ</span>
                              <select
                                value={edge.targetNodeKey}
                                onChange={(event) => updateWorkflowEdge(index, { targetNodeKey: event.target.value })}
                              >
                                {workflowEditor.nodes.map((node) => (
                                  <option key={node.nodeKey} value={node.nodeKey}>
                                    {node.nodeName}
                                  </option>
                                ))}
                              </select>
                            </label>
                            <label className="workflow-editor-field">
                              <span>条件</span>
                              <input
                                placeholder="通常は空欄"
                                value={edge.conditionExpression ?? ''}
                                onChange={(event) =>
                                  updateWorkflowEdge(index, { conditionExpression: event.target.value || null })
                                }
                              />
                            </label>
                          </div>
                        ))}
                      </div>
                    </form>
                  </>
                ) : (
                  <div className="compact-row">ワークフローを選択してください</div>
                )}
              </div>
            </div>
          </article>
        </section>
        ) : null}

        {activeSection === 'forms' ? (
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
                      onClick={() => selectForm(formDefinition.formCode)}
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
                          <span>{fieldDataTypeLabel(field.dataType)}</span>
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

                    <form className="definition-editor" onSubmit={submitFormDefinition}>
                      <div className="editor-toolbar">
                        <label className="draft-field">
                          <span>申請書名</span>
                          <input
                            value={formEditor.formName}
                            onChange={(event) =>
                              setFormEditor((currentEditor) => {
                                const baseEditor = currentEditor ?? formEditor
                                return {
                                  ...baseEditor,
                                  formName: event.target.value,
                                }
                              })
                            }
                          />
                        </label>
                        <label className="draft-field">
                          <span>ワークフロー</span>
                          <select
                            value={formEditor.workflowCode}
                            onChange={(event) =>
                              setFormEditor((currentEditor) => {
                                const baseEditor = currentEditor ?? formEditor
                                return {
                                  ...baseEditor,
                                  workflowCode: event.target.value,
                                }
                              })
                            }
                          >
                            {workflowDefinitions.map((workflowDefinition) => (
                              <option key={workflowDefinition.workflowCode} value={workflowDefinition.workflowCode}>
                                {workflowDefinition.workflowName}
                              </option>
                            ))}
                          </select>
                        </label>
                        <div className="editor-actions">
                          {saveFormDefinitionMutation.isError ? (
                            <span className="draft-message error">申請書定義を保存できません</span>
                          ) : null}
                          {saveFormDefinitionMutation.data ? (
                            <span className="draft-message">申請書定義を保存しました</span>
                          ) : null}
                          <button className="primary-button" disabled={saveFormDefinitionMutation.isPending}>
                            {saveFormDefinitionMutation.isPending ? '保存中' : '定義保存'}
                          </button>
                        </div>
                      </div>

                      <div className="editor-section-title">
                        <strong>項目定義</strong>
                        <button className="icon-text-button" onClick={addFormField} type="button">
                          追加
                        </button>
                      </div>
                      <div className="form-field-editor-list">
                        {formEditor.fields.map((field, index) => (
                          <div className="form-field-editor-row" key={`${field.fieldKey}-${index}`}>
                            <input
                              value={field.fieldKey}
                              onChange={(event) => updateFormField(index, { fieldKey: event.target.value })}
                            />
                            <input
                              value={field.label}
                              onChange={(event) => updateFormField(index, { label: event.target.value })}
                            />
                            <select
                              value={field.dataType}
                              onChange={(event) => updateFormField(index, { dataType: event.target.value })}
                            >
                              <option value="TEXT">一行テキスト</option>
                              <option value="TEXTAREA">複数行テキスト</option>
                              <option value="NUMBER">数値</option>
                              <option value="DATE">日付</option>
                              <option value="MONTH">年月</option>
                            </select>
                            <label className="editor-check">
                              <input
                                checked={field.required}
                                onChange={(event) => updateFormField(index, { required: event.target.checked })}
                                type="checkbox"
                              />
                              必須
                            </label>
                            <input
                              value={field.placeholder ?? ''}
                              onChange={(event) => updateFormField(index, { placeholder: event.target.value })}
                            />
                            <button className="icon-button" onClick={() => removeFormField(index)} type="button">
                              <X size={14} />
                            </button>
                          </div>
                        ))}
                      </div>
                    </form>
                  </>
                )}
              </div>
            </div>
          </article>
        </section>
        ) : null}

        {activeSection === 'employees' ? (
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
                    <span>承認ランク {position.approvalRank}</span>
                  </div>
                ))
              )}
            </div>
          </article>
        </section>
        ) : null}

        {activeSection === 'settings' ? (
          <section className="settings-grid" aria-label="デモ情報">
            <article className="panel">
              <div className="panel-header">
                <div>
                  <p className="eyebrow">確認ポイント</p>
                  <h2>ポートフォリオで見せる範囲</h2>
                </div>
                <Settings size={20} aria-hidden="true" />
              </div>
              <div className="compact-list">
                <div className="compact-row">
                  <strong>申請者</strong>
                  <span>demo1@example.local / demo1001</span>
                </div>
                <div className="compact-row">
                  <strong>承認者</strong>
                  <span>demo5@example.local / demo1005</span>
                </div>
                <div className="compact-row">
                  <strong>API ドキュメント</strong>
                  <span>Swagger UI と OpenAPI JSON を localhost:8080 で確認できます</span>
                </div>
              </div>
            </article>
          </section>
        ) : null}
      </section>
    </main>
  )
}

export default App
