const brianaEmail = 'briana@sweetwaterlandco.com'
const fallbackName = '[Your name]'

export const getStoredUserName = (storage = globalThis.localStorage) => {
  try {
    const user = JSON.parse(storage?.getItem('user') || 'null')
    return (
      [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.email || fallbackName
    )
  } catch {
    return fallbackName
  }
}

export const buildEmailHref = ({ userName = fallbackName, listing = null } = {}) => {
  const property = listing
    ? [listing.address, listing.city, listing.state, listing.zipcode].filter(Boolean).join(', ')
    : 'your available properties'
  const subject = listing ? `Interest in ${listing.address}` : 'Real estate inquiry'
  const body = `Hi Briana,\n\nMy name is ${userName}, and I am interested in ${listing ? `the property at ${property}` : `learning more about ${property}`}.\n\nPlease contact me with more details.\n\nThank you,\n${userName}`

  return `mailto:${brianaEmail}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`
}
