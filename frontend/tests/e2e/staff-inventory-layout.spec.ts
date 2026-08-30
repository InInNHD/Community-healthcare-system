import { expect, test } from '@playwright/test'

test('inventory status badge keeps its text inside the medicine card', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('healthcare_user', JSON.stringify({
      username: 'doctor',
      displayName: '演示医生',
      roles: ['DOCTOR'],
      portal: 'staff',
      subjectId: 1,
      mustChangePassword: false,
    }))
  })

  await page.route('**/api/staff/**', async (route) => {
    const pathname = new URL(route.request().url()).pathname
    if (pathname === '/api/staff/summary') {
      await route.fulfill({ json: {
        appointmentsToday: 0,
        pendingAppointments: 0,
        completedToday: 0,
        managedPatients: 0,
        chronicCases: 0,
        lowStockMedicines: 1,
      } })
      return
    }
    if (pathname === '/api/staff/medicine-alerts') {
      await route.fulfill({ json: [{
        id: 1,
        name: '苯磺酸氨氯地平片',
        category: '心血管用药',
        specification: '5mg×28片',
        stock: 24,
        minimumStock: 30,
      }] })
      return
    }
    await route.fulfill({ json: [] })
  })

  await page.goto('/staff')
  await page.getByRole('button', { name: /库存预警/ }).click()

  const card = page.locator('.medicine-card').filter({ hasText: '苯磺酸氨氯地平片' })
  const badge = card.locator('.el-tag').filter({ hasText: '库存偏低' })
  await expect(badge).toBeVisible()

  const layout = await badge.evaluate((element) => ({
    clientWidth: element.clientWidth,
    scrollWidth: element.scrollWidth,
    whiteSpace: getComputedStyle(element).whiteSpace,
  }))
  expect(layout.whiteSpace).toBe('nowrap')
  expect(layout.scrollWidth).toBeLessThanOrEqual(layout.clientWidth)

  const [cardBox, badgeBox] = await Promise.all([card.boundingBox(), badge.boundingBox()])
  expect(cardBox).not.toBeNull()
  expect(badgeBox).not.toBeNull()
  expect(badgeBox!.x + badgeBox!.width).toBeLessThanOrEqual(cardBox!.x + cardBox!.width)
})
