import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PlatformWorkbenchView from './PlatformWorkbenchView.vue'

const { startEncounter, saveEncounterDraft, signEncounter } = vi.hoisted(() => ({
  startEncounter: vi.fn(),
  saveEncounterDraft: vi.fn(),
  signEncounter: vi.fn(),
}))

vi.mock('../auth', () => ({
  auth: { user: { roles: ['DOCTOR'] } },
}))

vi.mock('../api/platform', () => ({
  platformApi: {
    startEncounter,
    saveEncounterDraft,
    signEncounter,
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), warning: vi.fn(), info: vi.fn() },
}))

describe('PlatformWorkbenchView encounter workflow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    startEncounter.mockResolvedValue({ data: { id: 77, version: 3, status: 'DRAFT' } })
    saveEncounterDraft.mockResolvedValue({ data: { id: 77, version: 4, status: 'DRAFT' } })
    signEncounter.mockResolvedValue({ data: { id: 77, version: 5, status: 'SIGNED' } })
  })

  it('starts from an appointment and then saves and signs the returned encounter', async () => {
    const wrapper = mount(PlatformWorkbenchView, {
      props: { portal: 'staff' },
      global: { stubs: { RouterLink: true } },
    })
    await flushPromises()

    await wrapper.get('input[type="number"]').setValue(41)
    await wrapper.get('textarea').setValue('头晕，血压 146/92mmHg')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(startEncounter).toHaveBeenCalledWith(41)
    expect(saveEncounterDraft).toHaveBeenCalledWith(77, {
      chiefComplaint: '头晕，血压 146/92mmHg',
      diagnosisCodes: [],
    })

    await wrapper.get('.secondary-action').trigger('click')
    await flushPromises()
    expect(signEncounter).toHaveBeenCalledWith(77)
  })
})
