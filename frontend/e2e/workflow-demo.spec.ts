import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

const applicantAccount = {
  username: 'demo1@growtea.co.jp',
  password: 'demo1001',
  employeeName: '山田 太郎',
}

const approverAccount = {
  username: 'demo5@growtea.co.jp',
  password: 'demo1005',
  employeeName: '岩瀬 大樹',
}

async function assertBackendAvailable(request: APIRequestContext) {
  try {
    const response = await request.get('/api/health')
    if (response.ok()) {
      return
    }
    throw new Error(`health check returned HTTP ${response.status()}`)
  } catch (error) {
    throw new Error('Backend must be running at http://localhost:8080 before running E2E tests', { cause: error })
  }
}

async function login(page: Page, account: typeof applicantAccount) {
  await page.goto('/')
  await page.getByRole('button', { name: new RegExp(account.employeeName) }).click()
  await page.getByRole('button', { name: 'ログイン' }).click()
  await expect(page.getByLabel('現在のユーザー')).toContainText(account.employeeName)
}

async function openSection(page: Page, sectionName: string) {
  await page.getByRole('navigation', { name: 'メインナビゲーション' }).getByRole('button', { name: sectionName }).click()
}

test('applicant submits a request and approver approves it with history visible', async ({ page, request }) => {
  await assertBackendAvailable(request)

  const title = `E2E承認確認 ${Date.now()}`

  await login(page, applicantAccount)

  await openSection(page, '申請者フロー')
  const newApplication = page.getByLabel('新規申請')
  await newApplication.getByLabel('申請書').selectOption('TRAVEL')
  await newApplication.getByLabel('件名').fill(title)
  await newApplication.getByLabel(/出張先/).fill('大阪支社')
  await newApplication.getByLabel(/開始日/).fill('2026-06-15')
  await newApplication.getByLabel(/終了日/).fill('2026-06-16')
  await newApplication.getByLabel(/目的/).fill('E2E テスト')
  await newApplication.getByRole('button', { name: '下書き保存' }).click()

  await expect(page.getByRole('button', { name: new RegExp(title) })).toBeVisible()
  await page.getByRole('button', { name: new RegExp(title) }).click()
  await page.getByRole('button', { name: '申請する' }).click()
  await expect(page.getByText('申請を提出しました')).toBeVisible()
  await expect(page.getByText('申請中').first()).toBeVisible()

  await page.getByLabel('ログアウト').click()
  await login(page, approverAccount)

  await openSection(page, '承認者フロー')
  const approvalTasks = page.getByLabel('承認タスク')
  await expect(approvalTasks.getByText(title)).toBeVisible()
  await approvalTasks.getByText(title).click()
  await approvalTasks.getByTitle('承認').first().click()

  await expect(page.getByText('承認済み').first()).toBeVisible()
  await expect(page.getByText('承認しました')).toBeVisible()
  await expect(page.getByText('承認').first()).toBeVisible()
})
