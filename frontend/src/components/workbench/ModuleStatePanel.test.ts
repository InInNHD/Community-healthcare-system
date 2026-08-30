import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ModuleStatePanel from './ModuleStatePanel.vue'

describe('ModuleStatePanel', () => {
  it('announces loading, error and empty states and exposes retry as a native button', async () => {
    const wrapper = mount(ModuleStatePanel, { props: { state: 'loading' } })
    expect(wrapper.get('[role="status"]').text()).toContain('正在加载')

    await wrapper.setProps({ state: 'error', errorMessage: '网络连接失败' })
    expect(wrapper.get('[role="alert"]').text()).toContain('网络连接失败')
    expect(wrapper.get('button').attributes('type')).toBe('button')

    await wrapper.setProps({ state: 'empty', emptyMessage: '暂无待办' })
    expect(wrapper.get('[role="status"]').text()).toContain('暂无待办')
  })
})
