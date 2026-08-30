import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

test('login portal is keyboard and screen-reader identifiable @a11y', async ({ page }) => {
  await page.goto('/login?portal=resident')

  await expect(page.getByRole('button', { name: /居民端/ })).toBeVisible()
  await expect(page.getByRole('textbox', { name: '用户名' })).toBeVisible()
  await expect(page.getByLabel('密码')).toBeVisible()

  const result = await new AxeBuilder({ page }).analyze()
  const seriousViolations = result.violations.filter(item => item.impact === 'critical' || item.impact === 'serious')
  expect(seriousViolations).toEqual([])
})
