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

export type CreateDraftApplicationRequest = {
  formCode: string
  title: string
  values: Record<string, string>
}

export type DraftApplication = {
  applicationNumber: string
  title: string
  status: string
  applicantName: string
  formName: string
  fieldValueCount: number
}

const DEMO_AUTHORIZATION_HEADER = `Basic ${btoa('demo1@growtea.co.jp:demo1001')}`

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path)

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

export function createDraftApplication(request: CreateDraftApplicationRequest) {
  return postJson<DraftApplication, CreateDraftApplicationRequest>('/api/applications/drafts', request, {
    authorization: DEMO_AUTHORIZATION_HEADER,
  })
}
