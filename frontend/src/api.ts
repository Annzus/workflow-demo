export type Employee = {
  employeeCode: string
  name: string
  organizationName: string
  positionName: string
}

export type Organization = {
  organizationCode: string
  name: string
  parentOrganizationCode: string | null
  validFrom: string
  validTo: string
}

export type Position = {
  positionCode: string
  name: string
  approvalRank: number
}

export type FormDefinitionSummary = {
  formCode: string
  formName: string
  workflowCode: string
  workflowName: string
  fieldCount: number
}

export type FormField = {
  fieldKey: string
  label: string
  dataType: string
  required: boolean
  placeholder: string | null
  initialValueType: string
  displayOrder: number
}

export type FormDefinitionDetail = {
  formCode: string
  formName: string
  workflowCode: string
  workflowName: string
  fields: FormField[]
}

export type WorkflowDefinitionSummary = {
  workflowCode: string
  workflowName: string
  activeVersion: number
  nodeCount: number
}

export type WorkflowNode = {
  nodeKey: string
  nodeName: string
  nodeType: string
  approverType: string | null
  positionCode: string | null
  employeeCode: string | null
  displayOrder: number
  xPosition: number
  yPosition: number
}

export type WorkflowEdge = {
  sourceNodeKey: string
  targetNodeKey: string
  conditionExpression: string | null
  displayOrder: number
}

export type WorkflowDefinitionDetail = WorkflowDefinitionSummary & {
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

export type SaveFormDefinitionRequest = {
  formCode: string
  formName: string
  workflowCode: string
  fields: FormField[]
}

export type SaveWorkflowDraftRequest = {
  workflowName: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

export type WorkflowDraftResponse = {
  workflowCode: string
  workflowName: string
  versionNumber: number
  published: boolean
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

export type CreateDraftApplicationRequest = {
  formCode: string
  title: string
  values: Record<string, string>
}

export type DraftApplication = {
  id: string
  applicationNumber: string
  title: string
  status: string
  applicantName: string
  formName: string
  fieldValueCount: number
}

export type ApplicationSummary = {
  id: string
  applicationNumber: string
  title: string
  status: string
  applicantName: string
  formName: string
  createdAt: string
  submittedAt: string | null
}

export type ApplicationFieldValue = {
  fieldKey: string
  label: string
  dataType: string
  value: string
  displayOrder: number
}

export type ApprovalRouteStep = {
  stepKey: string
  stepName: string
  actorName: string
  roleName: string
  status: string
  completedAt: string | null
}

export type ApplicationDetail = ApplicationSummary & {
  values: ApplicationFieldValue[]
  approvalRoute: ApprovalRouteStep[]
}

export type ApplicationHistory = {
  id: string
  actorName: string
  action: string
  comment: string
  createdAt: string
}

export type ApplicationAttachment = {
  id: string
  originalFilename: string
  contentType: string
  sizeBytes: number
  uploadedByName: string
  uploadedAt: string
}

export type ApprovalTask = {
  id: string
  applicationId: string
  applicationNumber: string
  title: string
  formName: string
  approverName: string
  stepName: string
  status: string
  dueDate: string | null
  createdAt: string
}

export type ApprovalTaskActionResponse = {
  taskId: string
  applicationId: string
  applicationStatus: string
  taskStatus: string
  history: ApplicationHistory
}

export type CurrentUser = {
  username: string
  employeeCode: string
  name: string
  organizationName: string
  positionName: string
}

const AUTH_STORAGE_KEY = 'workflowDemoAuthorization'

export const DEMO_ACCOUNTS = [
  {
    label: '申請者',
    username: 'demo1@example.local',
    password: 'demo1001',
    employeeName: '山田 太郎',
  },
  {
    label: '承認者',
    username: 'demo5@example.local',
    password: 'demo1005',
    employeeName: '岩瀬 大樹',
  },
] as const

export const DEMO_USERNAME = DEMO_ACCOUNTS[0].username
export const DEMO_PASSWORD = DEMO_ACCOUNTS[0].password

export function buildBasicAuthorization(username: string, password: string) {
  return `Basic ${btoa(`${username}:${password}`)}`
}

export function getSavedAuthorization() {
  if (typeof window === 'undefined') {
    return null
  }
  return window.localStorage.getItem(AUTH_STORAGE_KEY)
}

export function saveAuthorization(authorization: string) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, authorization)
}

export function clearAuthorization() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}

function currentAuthorization() {
  return getSavedAuthorization() ?? ''
}

async function getJson<T>(path: string, options: { authorization?: string } = {}): Promise<T> {
  const response = await fetch(path, {
    headers: {
      ...(options.authorization ? { Authorization: options.authorization } : {}),
    },
  })

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }

  return response.json() as Promise<T>
}

async function postJson<TResponse, TBody>(
  path: string,
  body: TBody,
  options: { authorization?: string } = {},
): Promise<TResponse> {
  const response = await fetch(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(options.authorization ? { Authorization: options.authorization } : {}),
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }

  return response.json() as Promise<TResponse>
}

export function getEmployees() {
  return getJson<Employee[]>('/api/master-data/employees')
}

export function getOrganizations() {
  return getJson<Organization[]>('/api/master-data/organizations')
}

export function getPositions() {
  return getJson<Position[]>('/api/master-data/positions')
}

export function getFormDefinitions() {
  return getJson<FormDefinitionSummary[]>('/api/form-definitions')
}

export function getFormDefinition(formCode: string) {
  return getJson<FormDefinitionDetail>(`/api/form-definitions/${formCode}`)
}

export function getWorkflowDefinitions() {
  return getJson<WorkflowDefinitionSummary[]>('/api/workflow-definitions')
}

export function getWorkflowDefinition(workflowCode: string) {
  return getJson<WorkflowDefinitionDetail>(`/api/workflow-definitions/${workflowCode}`)
}

export function saveFormDefinition(request: SaveFormDefinitionRequest) {
  return postJson<FormDefinitionDetail, SaveFormDefinitionRequest>('/api/form-definitions', request, {
    authorization: currentAuthorization(),
  })
}

export function saveWorkflowDraft(workflowCode: string, request: SaveWorkflowDraftRequest) {
  return postJson<WorkflowDraftResponse, SaveWorkflowDraftRequest>(
    `/api/workflow-definitions/${workflowCode}/draft`,
    request,
    {
      authorization: currentAuthorization(),
    },
  )
}

export function publishWorkflowDraft(workflowCode: string) {
  return postJson<WorkflowDraftResponse, Record<string, never>>(
    `/api/workflow-definitions/${workflowCode}/publish`,
    {},
    {
      authorization: currentAuthorization(),
    },
  )
}

export function getMe(authorization = currentAuthorization()) {
  return getJson<CurrentUser>('/api/me', {
    authorization,
  })
}

export function getApplications() {
  return getJson<ApplicationSummary[]>('/api/applications', {
    authorization: currentAuthorization(),
  })
}

export function getApplication(id: string) {
  return getJson<ApplicationDetail>(`/api/applications/${id}`, {
    authorization: currentAuthorization(),
  })
}

export function getApplicationHistory(id: string) {
  return getJson<ApplicationHistory[]>(`/api/applications/${id}/history`, {
    authorization: currentAuthorization(),
  })
}

export function getApplicationAttachments(id: string) {
  return getJson<ApplicationAttachment[]>(`/api/applications/${id}/attachments`, {
    authorization: currentAuthorization(),
  })
}

export function getPendingApprovalTasks() {
  return getJson<ApprovalTask[]>('/api/approval-tasks/pending', {
    authorization: currentAuthorization(),
  })
}

export function createDraftApplication(request: CreateDraftApplicationRequest) {
  return postJson<DraftApplication, CreateDraftApplicationRequest>('/api/applications/drafts', request, {
    authorization: currentAuthorization(),
  })
}

export async function uploadApplicationAttachment(id: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const authorization = currentAuthorization()

  const response = await fetch(`/api/applications/${id}/attachments`, {
    method: 'POST',
    headers: {
      ...(authorization ? { Authorization: authorization } : {}),
    },
    body: formData,
  })

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }

  return response.json() as Promise<ApplicationAttachment>
}

export function submitApplication(id: string) {
  return postJson<ApplicationDetail, Record<string, never>>(`/api/applications/${id}/submit`, {}, {
    authorization: currentAuthorization(),
  })
}

export function approveApprovalTask(id: string) {
  return postJson<ApprovalTaskActionResponse, { comment: string }>(
    `/api/approval-tasks/${id}/approve`,
    { comment: '承認しました' },
    { authorization: currentAuthorization() },
  )
}

export function rejectApprovalTask(id: string) {
  return postJson<ApprovalTaskActionResponse, { comment: string }>(
    `/api/approval-tasks/${id}/reject`,
    { comment: '差戻し確認' },
    { authorization: currentAuthorization() },
  )
}
