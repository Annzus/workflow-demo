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

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path)

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }

  return response.json() as Promise<T>
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
