import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ListingView from '../ListingView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
}))

describe('ListingView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn()
    window.confirm = vi.fn(() => true)
  })

  it('lets admins select multiple listings and delete them together', async () => {
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [
          { id: 1, address: '111 Test St', city: 'Fort Myers', state: 'FL', zipcode: '33901', price: 250000, status: 'ACTIVE', pictureUrl: 'https://example.com/1.jpg' },
          { id: 2, address: '222 Test St', city: 'Naples', state: 'FL', zipcode: '34102', price: 300000, status: 'ACTIVE', pictureUrl: 'https://example.com/2.jpg' },
        ],
      })
      .mockResolvedValueOnce({ ok: true })
      .mockResolvedValueOnce({ ok: true })

    const wrapper = mount(ListingView, {
      global: {
        stubs: ['RouterLink'],
      },
    })

    await Promise.resolve()
    await wrapper.vm.$nextTick()
    await Promise.resolve()

    const checkboxes = wrapper.findAll('.listing-select')
    expect(checkboxes).toHaveLength(2)

    await checkboxes[0].setChecked(true)
    await checkboxes[1].setChecked(true)

    const deleteSelected = wrapper.find('[data-testid="delete-selected"]')
    expect(deleteSelected.exists()).toBe(true)

    await deleteSelected.trigger('click')

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/entries/1',
      expect.objectContaining({ method: 'DELETE' }),
    )
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/entries/2',
      expect.objectContaining({ method: 'DELETE' }),
    )
  })
})
