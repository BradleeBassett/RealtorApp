const tokenKey = 'authToken'
const userKey = 'user'

export const getAuthHeaders = () => {
  const token = localStorage.getItem(tokenKey)
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export const saveAuthSession = ({ token, user }) => {
  localStorage.setItem(tokenKey, token)
  localStorage.setItem(userKey, JSON.stringify(user))
}

export const clearAuthSession = () => {
  localStorage.removeItem(tokenKey)
  localStorage.removeItem(userKey)
}