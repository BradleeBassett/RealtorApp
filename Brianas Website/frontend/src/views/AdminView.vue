<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuthSession, getAuthHeaders } from '../utils/auth'

const router = useRouter()
const users = ref([])
const form = ref({
  firstName: '',
  lastName: '',
  phoneNumber: '',
  email: '',
  description: '',
  password: '',
  role: 'USER',
})
const error = ref('')
const status = ref({
  backend: 'checking',
  database: 'checking',
})
const statusTimer = ref(null)
const editingUserId = ref(null)
const editForm = ref({ firstName: '', lastName: '', phoneNumber: '', email: '', description: '', role: 'USER' })
const savingUserId = ref(null)
const saveMessage = ref('')
const searchQuery = ref('')
const roleFilter = ref('ALL')
const isReturnTopVisible = ref(false)

const API_URL = '/api/users'
const STATUS_URL = '/api/health/status'

const filteredUsers = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  return users.value.filter((user) => {
    const matchesQuery =
      !query ||
      [user.firstName, user.lastName, user.phoneNumber, user.email]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(query)
    const matchesRole = roleFilter.value === 'ALL' || (user.role || 'USER') === roleFilter.value
    return matchesQuery && matchesRole
  })
})

const clearFilters = () => {
  searchQuery.value = ''
  roleFilter.value = 'ALL'
}

const updateReturnTopVisibility = () => {
  isReturnTopVisible.value = window.scrollY > 220
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const setStatus = (payload) => {
  status.value = {
    backend: payload?.backend === 'connected' ? 'connected' : 'disconnected',
    database: payload?.database === 'connected' ? 'connected' : 'disconnected',
  }
}

const loadStatus = async () => {
  try {
    const response = await fetch(STATUS_URL)
    if (!response.ok) {
      const payload = await response.json().catch(() => ({}))
      setStatus(payload)
      return
    }

    const payload = await response.json()
    setStatus(payload)
  } catch (err) {
    status.value = {
      backend: 'disconnected',
      database: 'disconnected',
    }
  }
}

const loadUsers = async () => {
  try {
    const response = await fetch(API_URL, { headers: getAuthHeaders() })
    if (!response.ok) {
      throw new Error('Unable to fetch users')
    }
    users.value = await response.json()
  } catch (err) {
    error.value = err.message || 'Could not load users.'
  }
}

const submitUser = async () => {
  error.value = ''

  try {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(form.value),
    })

    if (!response.ok) {
      let message = 'User could not be created.'
      try {
        const payload = await response.json()
        message = payload.message || payload.error || message
      } catch {
        // ignore non-json errors
      }
      throw new Error(message)
    }

    form.value = { firstName: '', lastName: '', phoneNumber: '', email: '', description: '', password: '', role: 'USER' }
    await loadUsers()
  } catch (err) {
    error.value = err.message || 'Something went wrong.'
  }
}

const deleteUser = async (id) => {
  const user = users.value.find((item) => item.id === id)
  const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'this user'

  if (!window.confirm(`Delete ${name}? This action cannot be undone.`)) {
    return
  }

  error.value = ''
  try {
    const response = await fetch(`${API_URL}/${id}`, { method: 'DELETE', headers: getAuthHeaders() })
    if (!response.ok) {
      throw new Error('User could not be deleted.')
    }
    await loadUsers()
  } catch (err) {
    error.value = err.message || 'Failed to delete user.'
  }
}

const startEdit = (user) => {
  editingUserId.value = user.id
  editForm.value = {
    firstName: user.firstName,
    lastName: user.lastName,
    phoneNumber: user.phoneNumber || '',
    email: user.email,
    description: user.description || '',
    role: user.role || 'USER',
  }
}

const cancelEdit = () => {
  editingUserId.value = null
}

const updateUser = async (id) => {
  error.value = ''
  saveMessage.value = ''

  const user = users.value.find((item) => item.id === id)
  const currentRole = user?.role || 'USER'
  const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || 'this user'

  if (
    editForm.value.role !== currentRole &&
    !window.confirm(`Change ${name}'s role from ${currentRole} to ${editForm.value.role}?`)
  ) {
    return
  }

  savingUserId.value = id

  try {
    const response = await fetch(`${API_URL}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(editForm.value),
    })

    if (!response.ok) {
      const payload = await response.json().catch(() => ({}))
      throw new Error(payload.message || 'User could not be updated.')
    }

    editingUserId.value = null
    saveMessage.value = 'User updated successfully.'
    await loadUsers()
  } catch (err) {
    error.value = err.message || 'Could not update user.'
  } finally {
    savingUserId.value = null
  }
}

const logout = () => {
  clearAuthSession()
  router.push('/')
}

const startStatusPolling = () => {
  loadStatus()
  statusTimer.value = setInterval(loadStatus, 5000)
}

onMounted(() => {
  updateReturnTopVisibility()
  window.addEventListener('scroll', updateReturnTopVisibility)

  let savedUser
  try {
    savedUser = JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    savedUser = null
  }

  if (savedUser?.role !== 'ADMIN') {
    router.push('/')
    return
  }

  loadUsers()
  startStatusPolling()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateReturnTopVisibility)
  if (statusTimer.value) {
    clearInterval(statusTimer.value)
  }
})
</script>

<template>
  <main class="page">
    <button
      :class="['return-top', { visible: isReturnTopVisible }]"
      type="button"
      aria-label="Return to top"
      title="Return to top"
      @click="scrollToTop"
    >
      ↑
    </button>
    <header class="topbar">
      <div>
        <p class="eyebrow">Admin Portal</p>
        <h1>System Management</h1>
      </div>
      <div class="topbar-actions">
        <RouterLink class="home-button" to="/">Home</RouterLink>
        <RouterLink class="home-button" to="/admin/listings">Manage listings</RouterLink>
        <button class="logout" type="button" @click="logout">Logout</button>
      </div>
    </header>

    <section class="card status-card">
      <div class="status-header">
        <h2>System Status</h2>
      </div>

      <div class="status-grid">
        <div class="status-item">
          <span class="status-label">Backend</span>
          <span :class="['status-pill', status.backend]">
            {{
              status.backend === 'connected'
                ? 'Connected'
                : status.backend === 'checking'
                  ? 'Checking...'
                  : 'Disconnected'
            }}
          </span>
        </div>

        <div class="status-item">
          <span class="status-label">Database</span>
          <span :class="['status-pill', status.database]">
            {{
              status.database === 'connected'
                ? 'Connected'
                : status.database === 'checking'
                  ? 'Checking...'
                  : 'Disconnected'
            }}
          </span>
        </div>
      </div>
    </section>

    <section class="card">
      <h2>User Management</h2>

      <form class="user-form" @submit.prevent="submitUser">
        <div class="field-group">
          <label for="firstName">First Name</label>
          <input id="firstName" v-model="form.firstName" type="text" required />
        </div>

        <div class="field-group">
          <label for="lastName">Last Name</label>
          <input id="lastName" v-model="form.lastName" type="text" required />
        </div>

        <div class="field-group">
          <label for="email">Email</label>
          <input id="email" v-model="form.email" type="email" required />
        </div>

        <div class="field-group">
          <label for="phoneNumber">Phone Number</label>
          <input id="phoneNumber" v-model="form.phoneNumber" type="tel" autocomplete="tel" required />
        </div>

        <div class="field-group">
          <label for="description">Notes</label>
          <textarea id="description" v-model="form.description" rows="3"></textarea>
        </div>

        <div class="field-group">
          <label for="password">Password</label>
          <input id="password" v-model="form.password" type="password" minlength="8" required />
        </div>

        <div class="field-group">
          <label for="role">Role</label>
          <select id="role" v-model="form.role" required>
            <option value="ADMIN">Admin</option>
            <option value="MANAGER">Manager</option>
            <option value="AGENT">Agent</option>
            <option value="USER">User</option>
          </select>
        </div>

        <button type="submit">Create User</button>
      </form>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="saveMessage" class="success">{{ saveMessage }}</p>
    </section>

    <section class="card">
      <h2>Current Users</h2>

      <div class="user-filters">
        <label class="search-field" for="user-search">
          Search accounts
          <input id="user-search" v-model="searchQuery" type="search" placeholder="Name or email" />
        </label>

        <label class="role-filter" for="role-filter">
          Role
          <select id="role-filter" v-model="roleFilter">
            <option value="ALL">All roles</option>
            <option value="ADMIN">Admin</option>
            <option value="MANAGER">Manager</option>
            <option value="AGENT">Agent</option>
            <option value="USER">User</option>
          </select>
        </label>

        <button
          class="clear-filters"
          type="button"
          :disabled="!searchQuery && roleFilter === 'ALL'"
          @click="clearFilters"
        >
          Clear
        </button>
        <span class="result-count">{{ filteredUsers.length }} of {{ users.length }} accounts</span>
      </div>

      <ul v-if="filteredUsers.length" class="user-list">
        <li v-for="user in filteredUsers" :key="user.id" class="user-item">
          <form
            v-if="editingUserId === user.id"
            class="edit-form"
            @submit.prevent="updateUser(user.id)"
          >
            <label>First name<input v-model="editForm.firstName" type="text" required /></label>
            <label>Last name<input v-model="editForm.lastName" type="text" required /></label>
            <label>Phone number<input v-model="editForm.phoneNumber" type="tel" required /></label>
            <label>Email<input v-model="editForm.email" type="email" required /></label>
            <label class="edit-notes">Notes<textarea v-model="editForm.description" rows="2"></textarea></label>
            <label>Role<select v-model="editForm.role" required><option value="ADMIN">Admin</option><option value="MANAGER">Manager</option><option value="AGENT">Agent</option><option value="USER">User</option></select></label>
            <button class="save" type="submit" :disabled="savingUserId === user.id">
              <span v-if="savingUserId === user.id" class="spinner" aria-hidden="true"></span>
              {{ savingUserId === user.id ? 'Saving...' : 'Save' }}
            </button>
            <button
              class="cancel"
              type="button"
              :disabled="savingUserId === user.id"
              @click="cancelEdit"
            >
              Cancel
            </button>
          </form>
          <div v-else>
            <strong>{{ user.firstName }} {{ user.lastName }}</strong>
            <span>{{ user.email }}</span>
            <span>{{ user.phoneNumber || 'No phone number' }}</span>
            <span class="user-description"><strong>Notes:</strong> {{ user.description || 'No notes' }}</span>
            <span class="role-badge">{{ user.role || 'USER' }}</span>
          </div>
          <div v-if="editingUserId !== user.id" class="user-actions">
            <button class="edit" type="button" @click="startEdit(user)">Edit</button>
            <button class="danger" type="button" @click="deleteUser(user.id)">Delete</button>
          </div>
        </li>
      </ul>

      <p v-else class="empty">
        {{ users.length ? 'No users match these filters.' : 'No users yet.' }}
      </p>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  gap: 1.5rem;
  padding: 2rem;
  background: linear-gradient(180deg, #eef6ff 0%, #edf4fb 100%);
  font-family: Arial, sans-serif;
}

.return-top {
  position: fixed;
  z-index: 4;
  left: 1.25rem;
  bottom: 1.5rem;
  width: 2.7rem;
  height: 2.7rem;
  display: grid;
  place-items: center;
  padding: 0;
  color: #fffdf8;
  background: #2563eb;
  border: 0;
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.24);
  font: 1.35rem/1 Georgia, serif;
  cursor: pointer;
  opacity: 0;
  visibility: hidden;
  transform: translateY(10px);
  transition: opacity 0.2s ease, visibility 0.2s ease, background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.return-top.visible {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.return-top:hover,
.return-top:focus-visible {
  background: #1d4ed8;
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.28);
}

.topbar {
  width: min(960px, 100%);
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.topbar-actions,
.user-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.home-button {
  padding: 0.8rem 1.1rem;
  border-radius: 10px;
  color: #1d4f91;
  background: linear-gradient(180deg, #edf5ff 0%, #dfeeff 100%);
  border: 1px solid #bfd8ff;
  font-weight: 700;
  text-decoration: none;
  box-shadow: 0 4px 12px rgba(96, 123, 167, 0.12);
}
.home-button:hover {
  background: linear-gradient(180deg, #dfeeff 0%, #d1e6ff 100%);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.7rem;
  color: #3b82f6;
  font-weight: 700;
  margin: 0 0 0.25rem;
}

h1,
h2 {
  margin: 0;
  color: #111827;
}

.card {
  width: min(960px, 100%);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(174, 205, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.08);
  padding: 2rem;
  margin: 0 auto;
}

.status-card {
  display: grid;
  gap: 1rem;
}

.status-grid {
  display: grid;
  gap: 0.9rem;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f9fafb;
}

.status-label {
  font-weight: 700;
  color: #374151;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  padding: 0.45rem 0.8rem;
  border-radius: 999px;
  font-weight: 700;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.status-pill.connected {
  background: #dcfce7;
  color: #166534;
}

.status-pill.disconnected {
  background: #fee2e2;
  color: #991b1b;
}

.status-pill.checking {
  background: #fef3c7;
  color: #92400e;
}

.user-form {
  display: grid;
  gap: 1rem;
}

.field-group {
  display: grid;
  gap: 0.4rem;
}

label {
  font-weight: 600;
  color: #374151;
}

input,
select,
textarea {
  padding: 0.75rem 0.9rem;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 1rem;
}

button {
  border: none;
  border-radius: 10px;
  padding: 0.8rem 1.1rem;
  background: #2563eb;
  color: white;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.14);
}

button:hover {
  background: #1d4ed8;
}

.logout {
  background: #111827;
}

.logout:hover {
  background: #1f2937;
}

.danger {
  background: #dc2626;
}

.danger:hover {
  background: #b91c1c;
}

.edit {
  background: #4b5563;
}

.edit:hover {
  background: #374151;
}

.edit-form {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto auto;
  gap: 0.6rem;
  align-items: center;
}

.edit-form input,
.edit-form select,
.edit-form textarea {
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
  padding: 0.65rem 0.7rem;
  font-size: 0.9rem;
}

.edit-form label {
  min-width: 0;
  display: grid;
  gap: 0.35rem;
  font-size: 0.75rem;
}

.edit-form button {
  padding: 0.65rem 0.8rem;
  white-space: nowrap;
}

.edit-form .edit-notes {
  grid-column: 1 / -1;
}

.edit-form textarea {
  min-height: 4.5rem;
  resize: vertical;
}

.save {
  background: #166534;
}

.save:hover {
  background: #14532d;
}

.save:disabled,
.cancel:disabled {
  cursor: wait;
  opacity: 0.7;
}

.spinner {
  display: inline-block;
  width: 0.8rem;
  height: 0.8rem;
  margin-right: 0.35rem;
  vertical-align: -0.1rem;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.cancel {
  background: #6b7280;
}

.cancel:hover {
  background: #4b5563;
}

.user-filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: end;
  gap: 1rem;
  margin-top: 1rem;
  padding: 1rem;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
}

.user-filters label {
  display: grid;
  gap: 0.4rem;
}

.user-filters input,
.user-filters select,
.clear-filters {
  width: 100%;
  box-sizing: border-box;
}

.result-count {
  align-self: center;
  color: #475569;
  font-size: 0.9rem;
  text-align: center;
}

.user-list {
  list-style: none;
  padding: 0;
  margin: 1rem 0 0;
  display: grid;
  gap: 0.75rem;
}

.user-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 0.9rem 1rem;
  background: #ffffff;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.user-item:has(.edit-form) {
  background: #eef4ff;
  border-color: #bfd2f7;
  box-shadow: 0 0 0 2px rgba(129, 152, 215, 0.12), inset 0 0 0 1px rgba(112, 134, 180, 0.1);
}

.user-item div {
  display: grid;
}

.user-item span {
  color: #6b7280;
}

.user-description {
  max-width: 42rem;
  margin-top: 0.25rem;
  white-space: pre-wrap;
}

.error {
  color: #b91c1c;
  font-weight: 600;
  margin-top: 1rem;
}

.empty {
  color: #6b7280;
  margin-top: 1rem;
}

.success {
  color: #166534;
  font-weight: 600;
  margin-top: 1rem;
}

@media (max-width: 760px) {
  .topbar,
  .user-item {
    align-items: stretch;
    flex-direction: column;
  }

  .topbar-actions,
  .user-actions {
    justify-content: flex-end;
  }

  .edit-form {
    grid-template-columns: 1fr 1fr;
  }

  .user-filters {
    grid-template-columns: 1fr;
  }
}
</style>
