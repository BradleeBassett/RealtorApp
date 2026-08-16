import { describe, expect, it } from 'vitest'
import { buildEmailHref, getStoredUserName } from '../contact'

describe('contact email drafts', () => {
  it('creates a property-specific draft with the stored user name', () => {
    const listing = { address: '123 Main Street', city: 'LaBelle', state: 'FL', zipcode: '33935' }
    const href = buildEmailHref({ userName: 'Jordan Smith', listing })

    expect(href.startsWith('mailto:briana@sweetwaterlandco.com?')).toBe(true)
    expect(decodeURIComponent(href)).toContain('Interest in 123 Main Street')
    expect(decodeURIComponent(href)).toContain('My name is Jordan Smith')
    expect(decodeURIComponent(href)).toContain('123 Main Street, LaBelle, FL, 33935')
  })

  it('creates a general draft and falls back to a name placeholder', () => {
    const storage = { getItem: () => null }
    const userName = getStoredUserName(storage)
    const href = buildEmailHref({ userName })

    expect(userName).toBe('[Your name]')
    expect(decodeURIComponent(href)).toContain('Real estate inquiry')
    expect(decodeURIComponent(href)).toContain('My name is [Your name]')
    expect(decodeURIComponent(href)).toContain('learning more about your available properties')
  })
})
