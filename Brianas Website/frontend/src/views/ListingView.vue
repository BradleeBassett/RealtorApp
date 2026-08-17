<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAuthHeaders } from '../utils/auth'

const router = useRouter()
const listings = ref([])
const error = ref('')
const message = ref('')
const form = ref({
  address: '',
  city: '',
  state: 'FL',
  zipcode: '',
  price: '',
  description: '',
  pictureUrl: '',
  pictureUrls: [],
  status: 'ACTIVE',
})
const editingId = ref(null)
const editForm = ref({
  address: '',
  city: '',
  state: 'FL',
  zipcode: '',
  price: '',
  description: '',
  pictureUrl: '',
  pictureUrls: [],
  status: 'ACTIVE',
})
const pictureReading = ref(false)
const searchQuery = ref('')
const statusFilter = ref('ALL')
const selectedIds = ref([])
const isReturnTopVisible = ref(false)
const maxAdditionalPictures = 15

const filteredListings = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return listings.value.filter((listing) => {
    const searchable = [listing.address, listing.city, listing.state, listing.zipcode]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    const matchesQuery = !query || searchable.includes(query)
    const matchesStatus =
      statusFilter.value === 'ALL' || (listing.status || 'ACTIVE') === statusFilter.value
    return matchesQuery && matchesStatus
  })
})

const clearFilters = () => {
  searchQuery.value = ''
  statusFilter.value = 'ALL'
}

const updateReturnTopVisibility = () => {
  isReturnTopVisible.value = window.scrollY > 220
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const toggleSelected = (id) => {
  const exists = selectedIds.value.includes(id)
  selectedIds.value = exists
    ? selectedIds.value.filter((selectedId) => selectedId !== id)
    : [...selectedIds.value, id]
}

const deleteSelectedListings = async () => {
  const ids = [...selectedIds.value]
  if (!ids.length) return

  if (!window.confirm(`Delete ${ids.length} selected listing${ids.length === 1 ? '' : 's'}?`)) {
    return
  }

  try {
    await Promise.all(
      ids.map((id) =>
        fetch(`/api/entries/${id}`, {
          method: 'DELETE',
          headers: getAuthHeaders(),
        }),
      ),
    )
    selectedIds.value = []
    await loadListings()
  } catch (err) {
    error.value = err.message || 'Could not delete selected listings.'
  }
}

const readPictures = async (files) => {
  if (files.some((file) => !file.type.startsWith('image/'))) {
    throw new Error('Please choose an image file.')
  }

  if (files.some((file) => file.size > 5 * 1024 * 1024)) {
    throw new Error('Please choose an image smaller than 5 MB.')
  }

  return Promise.all(
    files.map(
      (file) =>
        new Promise((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = () => resolve(reader.result)
          reader.onerror = reject
          reader.readAsDataURL(file)
        }),
    ),
  )
}

const handleMainPictureChange = async (event) => {
  const [file] = [...(event.target.files || [])]
  if (!file) return

  pictureReading.value = true
  try {
    const [picture] = await readPictures([file])
    const previousMainPicture = form.value.pictureUrl
    form.value.pictureUrl = picture
    form.value.pictureUrls = [
      picture,
      ...form.value.pictureUrls.filter((url) => url !== previousMainPicture),
    ]
  } catch (err) {
    error.value = err.message || 'The image could not be read. Please choose it again.'
    event.target.value = ''
  } finally {
    pictureReading.value = false
  }
}

const handleAdditionalPicturesChange = async (event) => {
  const files = [...(event.target.files || [])]
  if (!files.length) return

  const additionalPictures = form.value.pictureUrls.filter((url) => url !== form.value.pictureUrl)
  if (additionalPictures.length + files.length > maxAdditionalPictures) {
    error.value = `You can add up to ${maxAdditionalPictures} additional pictures.`
    event.target.value = ''
    return
  }

  pictureReading.value = true
  try {
    const pictures = await readPictures(files)
    form.value.pictureUrls = [form.value.pictureUrl, ...additionalPictures, ...pictures].filter(
      Boolean,
    )
  } catch (err) {
    error.value = err.message || 'The images could not be read. Please choose them again.'
    event.target.value = ''
  } finally {
    pictureReading.value = false
  }
}

const handlePictureChange = async (event, target = 'create') => {
  const targetForm = target === 'edit' ? editForm : form
  const files = [...(event.target.files || [])]
  if (!files.length) {
    return
  }

  pictureReading.value = true
  try {
    const pictures = await readPictures(files)
    targetForm.value.pictureUrls = pictures
    targetForm.value.pictureUrl = pictures[0]
  } catch (err) {
    error.value = err.message || 'The images could not be read. Please choose them again.'
    event.target.value = ''
  } finally {
    pictureReading.value = false
  }
}

const startEdit = (listing) => {
  editingId.value = listing.id
  editForm.value = { ...listing, price: String(listing.price) }
  error.value = ''
}

const cancelEdit = () => {
  editingId.value = null
}

const updateListing = async (id) => {
  error.value = ''
  message.value = ''

  try {
    const response = await fetch(`/api/entries/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify({ ...editForm.value, price: Number(editForm.value.price) }),
    })
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(payload.message || 'Listing could not be updated.')
    editingId.value = null
    message.value = 'Listing updated successfully.'
    await loadListings()
  } catch (err) {
    error.value = err.message || 'Could not update listing.'
  }
}

const loadListings = async () => {
  const response = await fetch('/api/entries')
  if (!response.ok) throw new Error('Could not load listings.')
  listings.value = await response.json()
}

const submitListing = async () => {
  error.value = ''
  message.value = ''

  try {
    const response = await fetch('/api/entries', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify({ ...form.value, price: Number(form.value.price) }),
    })
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(payload.message || 'Listing could not be created.')

    form.value = {
      address: '',
      city: '',
      state: 'FL',
      zipcode: '',
      price: '',
      description: '',
      pictureUrl: '',
      pictureUrls: [],
      status: 'ACTIVE',
    }
    message.value = 'Listing added successfully.'
    await loadListings()
  } catch (err) {
    error.value = err.message || 'Something went wrong.'
  }
}

const deleteListing = async (listing) => {
  if (!window.confirm(`Delete the listing at ${listing.address}?`)) return

  try {
    const response = await fetch(`/api/entries/${listing.id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    })
    if (!response.ok) throw new Error('Listing could not be deleted.')
    selectedIds.value = selectedIds.value.filter((id) => id !== listing.id)
    await loadListings()
  } catch (err) {
    error.value = err.message || 'Could not delete listing.'
  }
}

onMounted(async () => {
  updateReturnTopVisibility()
  window.addEventListener('scroll', updateReturnTopVisibility)

  try {
    await loadListings()
  } catch (err) {
    error.value = err.message
  }
})
</script>

<template>
  <main class="listing-page">
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
        <h1>Listing Management</h1>
      </div>
      <div class="topbar-actions">
        <RouterLink class="home-button" to="/">Home</RouterLink>
        <RouterLink class="home-button" to="/admin">Admin panel</RouterLink>
      </div>
    </header>

    <section class="card">
      <p class="eyebrow">Add a property</p>
      <h2>Create listing</h2>
      <form class="listing-form" @submit.prevent="submitListing">
        <label>Property address<input v-model="form.address" required type="text" /></label>
        <label>City<input v-model="form.city" required type="text" /></label>
        <label>State<input v-model="form.state" required maxlength="2" type="text" /></label>
        <label>ZIP code<input v-model="form.zipcode" required type="text" /></label>
        <label
          >Listing price<input v-model="form.price" required min="0" step="0.01" type="number"
        /></label>
        <label>Property description<textarea v-model="form.description" required rows="5"></textarea></label>
        <label
          >Main listing photo<input
            required
            type="file"
            accept="image/*"
            @change="handleMainPictureChange"
        /></label>
        <label
          >Additional home photos<input
            type="file"
            accept="image/*"
            multiple
            @change="handleAdditionalPicturesChange"
        /></label>
        <span v-if="form.pictureUrls.length" class="picture-count"
          >{{ form.pictureUrl ? form.pictureUrls.length - 1 : form.pictureUrls.length }} additional
          picture{{
            (form.pictureUrl ? form.pictureUrls.length - 1 : form.pictureUrls.length) === 1
              ? ''
              : 's'
          }}
          selected. Up to {{ maxAdditionalPictures }} additional pictures are allowed.</span
        >
        <div
          v-if="form.pictureUrls.length"
          class="create-previews"
          aria-label="Selected listing pictures"
        >
          <div v-if="form.pictureUrl" class="create-preview create-preview-cover">
            <img :src="form.pictureUrl" alt="Selected landing picture" /><span
              >Landing picture</span
            >
          </div>
          <div
            v-for="(picture, index) in form.pictureUrls.slice(form.pictureUrl ? 1 : 0)"
            :key="picture"
            class="create-preview"
          >
            <img :src="picture" :alt="`Selected additional picture ${index + 1}`" /><span
              >Picture {{ index + 1 }}</span
            >
          </div>
        </div>
        <label
          >Status<select v-model="form.status">
            <option value="ACTIVE">Current active</option>
            <option value="CLOSED">Just closed</option>
          </select></label
        >
        <button type="submit">Add listing</button>
      </form>
      <p v-if="message" class="success">{{ message }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>

    <section class="card">
      <h2>Saved listings</h2>
      <div class="listing-filters">
        <label class="search-filter" for="listing-search"
          >Search listings<input
            id="listing-search"
            v-model="searchQuery"
            type="search"
            placeholder="Address, city, or ZIP code"
        /></label>
        <label for="listing-status-filter"
          >Listing status<select id="listing-status-filter" v-model="statusFilter">
            <option value="ALL">All listings</option>
            <option value="ACTIVE">Active</option>
            <option value="CLOSED">Just closed</option>
          </select></label
        >
        <button
          class="clear-filter"
          type="button"
          :disabled="!searchQuery && statusFilter === 'ALL'"
          @click="clearFilters"
        >
          Clear
        </button>
        <div class="bulk-actions">
          <button
            v-if="selectedIds.length"
            class="danger"
            data-testid="delete-selected"
            type="button"
            @click="deleteSelectedListings"
          >
            Delete selected ({{ selectedIds.length }})
          </button>
        </div>
        <span class="filter-count"
          >{{ filteredListings.length }} of {{ listings.length }} listings</span
        >
      </div>
      <ul v-if="filteredListings.length" class="listing-list">
        <li v-for="listing in filteredListings" :key="listing.id">
          <label class="listing-checkbox-wrap">
            <input
              class="listing-select"
              type="checkbox"
              :checked="selectedIds.includes(listing.id)"
              @change="toggleSelected(listing.id)"
            />
          </label>
          <template v-if="editingId === listing.id">
            <form class="edit-listing-form" @submit.prevent="updateListing(listing.id)">
              <label>Property address<input v-model="editForm.address" required type="text" /></label>
              <label>City<input v-model="editForm.city" required type="text" /></label>
              <label
                >State<input v-model="editForm.state" required maxlength="2" type="text"
              /></label>
              <label>ZIP code<input v-model="editForm.zipcode" required type="text" /></label>
              <label
                >Listing price<input v-model="editForm.price" required min="0" step="0.01" type="number"
              /></label>
              <label>Property description<textarea v-model="editForm.description" required rows="5"></textarea></label>
              <label
                >Replace photos<input
                  type="file"
                  accept="image/*"
                  multiple
                  @change="handlePictureChange($event, 'edit')"
              /></label>
              <div v-if="editForm.pictureUrls?.length" class="edit-previews">
                <img
                  v-for="picture in editForm.pictureUrls"
                  :key="picture"
                  class="edit-preview"
                  :src="picture"
                  alt="Selected replacement preview"
                />
              </div>
              <label
                >Listing status<select v-model="editForm.status">
                  <option value="ACTIVE">Current active</option>
                  <option value="CLOSED">Just closed</option>
                </select></label
              >
              <div class="edit-actions">
                <button type="submit" :disabled="pictureReading">
                  {{ pictureReading ? 'Reading...' : 'Save' }}</button
                ><button class="cancel" type="button" @click="cancelEdit">Cancel</button>
              </div>
            </form>
          </template>
          <template v-else>
            <img
              v-if="listing.pictureUrl"
              :key="listing.pictureUrl"
              :src="listing.pictureUrl"
              :alt="listing.address"
            />
            <div>
              <strong>{{ listing.address }}</strong
              ><span>{{ listing.city }}, {{ listing.state }} {{ listing.zipcode }}</span
              ><span
                >{{ listing.status === 'CLOSED' ? 'Just closed' : 'Active' }} · ${{
                  Number(listing.price).toLocaleString()
                }}</span
              >
            </div>
            <button class="edit" type="button" @click="startEdit(listing)">Edit</button>
            <button class="danger" type="button" @click="deleteListing(listing)">Delete</button>
          </template>
        </li>
      </ul>
      <p v-else class="empty">
        {{
          listings.length ? 'No listings match these filters.' : 'No listings have been added yet.'
        }}
      </p>
    </section>
  </main>
</template>

<style scoped>
.listing-page {
  min-height: 100vh;
  display: grid;
  gap: 1.5rem;
  padding: 2rem;
  background: linear-gradient(180deg, #eef7ee 0%, #edf4f1 100%);
  color: #1f2d24;
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
  background: #5c7f5c;
  border: 0;
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(56, 82, 59, 0.22);
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
  background: #365b3b;
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(56, 82, 59, 0.28);
}
.topbar,
.card {
  width: min(960px, 100%);
  margin: 0 auto;
}
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.topbar-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.eyebrow {
  margin: 0 0 0.35rem;
  color: #3b82f6;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}
h1,
h2 {
  margin: 0;
  font-family: Georgia, 'Times New Roman', serif;
  letter-spacing: 0.01em;
}
h1 {
  font-size: 2.2rem;
}
h2 {
  margin-bottom: 1.2rem;
  font-size: 1.8rem;
}
.card {
  box-sizing: border-box;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(114, 144, 120, 0.2);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(62, 94, 70, 0.08);
}
.listing-filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px auto 1fr;
  align-items: end;
  gap: 0.75rem;
  margin: 1rem 0;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f9fafb;
}
.listing-filters label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.8rem;
}
.listing-filters input,
.listing-filters select {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  background: white;
}
.clear-filter {
  color: #374151;
  background: white;
  border: 1px solid #d1d5db;
}
.clear-filter:hover:not(:disabled) {
  background: #e5e7eb;
}
.clear-filter:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.bulk-actions {
  display: flex;
  align-items: end;
}
.bulk-actions button {
  width: 100%;
}
.filter-count {
  justify-self: end;
  padding-bottom: 0.75rem;
  color: #6b7280;
  font-size: 0.8rem;
}
.listing-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}
label {
  display: grid;
  gap: 0.45rem;
  color: #374151;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
input,
select {
  width: 100%;
  box-sizing: border-box;
  padding: 0.75rem 0.9rem;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font: inherit;
  font-size: 0.98rem;
  text-transform: none;
}
button,
.home-button {
  padding: 0.8rem 1rem;
  border: 0;
  border-radius: 10px;
  color: white;
  background: #5d7a67;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(93, 122, 103, 0.18);
}
button:hover,
.home-button:hover {
  background: #4d6857;
}
.listing-form button {
  align-self: end;
  background: #5b7c5f;
}
.listing-form button:hover {
  background: #45654b;
}
.picture-count {
  color: #166534;
  font-size: 0.8rem;
}
.create-previews {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f9fafb;
}
.create-preview {
  display: grid;
  gap: 0.35rem;
  color: #6b7280;
  font-size: 0.72rem;
  font-weight: 600;
}
.create-preview img {
  width: 92px;
  height: 72px;
  object-fit: cover;
  border: 2px solid #d1d5db;
  border-radius: 8px;
}
.create-preview-cover img {
  width: 124px;
  height: 92px;
  border-color: #2563eb;
}
.listing-list {
  display: grid;
  gap: 0.75rem;
  padding: 0;
  margin: 0;
  list-style: none;
}
.listing-list li {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 0.8rem;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
  transition: background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}
.listing-list li:has(.edit-listing-form) {
  background: #eef2f7;
  border-color: #a9bdd8;
  box-shadow: 0 0 0 2px rgba(120, 146, 184, 0.14), inset 0 0 0 1px rgba(98, 118, 145, 0.12);
}
.listing-checkbox-wrap {
  display: inline-flex;
  align-items: flex-start;
  justify-content: center;
  padding: 0.25rem 0 0;
  flex-shrink: 0;
}
.listing-select {
  width: 1.1rem;
  height: 1.1rem;
  accent-color: #2563eb;
  cursor: pointer;
}
.listing-list img {
  width: 76px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}
.listing-list > li > div {
  display: grid;
  gap: 0.25rem;
  flex: 1;
}
.listing-list span,
.empty {
  color: #6b7280;
  font-size: 0.85rem;
}
.edit {
  background: #4b5563;
}
.edit:hover {
  background: #374151;
}
.edit-listing-form {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
  flex: 1;
}
.edit-previews {
  grid-column: 1 / -1;
  min-width: 0;
  max-width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  overflow: hidden;
}
.edit-preview {
  flex: 0 0 100px;
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}
.edit-actions {
  display: flex;
  align-items: end;
  gap: 0.5rem;
}
.edit-actions button {
  flex: 1;
}
.cancel {
  color: #374151;
  background: #e5e7eb;
}
.cancel:hover {
  background: #d1d5db;
}
.danger {
  background: #dc2626;
}
.danger:hover {
  background: #b91c1c;
}
.error {
  color: #b91c1c;
  font-weight: 600;
}
.success {
  color: #166534;
  font-weight: 600;
}
@media (max-width: 700px) {
  .listing-page {
    padding: 1rem;
  }
  .topbar {
    align-items: stretch;
    flex-direction: column;
  }
  .listing-filters {
    grid-template-columns: 1fr 1fr;
  }
  .listing-filters .search-filter {
    grid-column: 1 / -1;
  }
  .filter-count {
    justify-self: start;
  }
  .listing-form,
  .edit-listing-form {
    grid-template-columns: 1fr;
  }
  .listing-list li {
    align-items: stretch;
    flex-direction: column;
  }
  .listing-list img {
    width: 100%;
    height: 150px;
  }
  .edit-actions {
    align-items: stretch;
  }
}
</style>
