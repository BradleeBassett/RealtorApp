<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAuthHeaders } from '../utils/auth'
import { buildEmailHref, getStoredUserName } from '../utils/contact'

const route = useRoute()
const listing = ref(null)
const error = ref('')
const saveMessage = ref('')
const selectedImage = ref('')
const isEditing = ref(false)
const editForm = ref(null)
const emailHref = computed(() =>
  buildEmailHref({ userName: getStoredUserName(), listing: listing.value }),
)
const isAdmin = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')?.role === 'ADMIN'
  } catch {
    return false
  }
})

const startEdit = () => {
  editForm.value = { ...listing.value, price: String(listing.value.price), description: listing.value.description || '' }
  saveMessage.value = ''
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editForm.value = null
}

const saveListing = async () => {
  error.value = ''
  saveMessage.value = ''
  try {
    const response = await fetch(`/api/entries/${listing.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify({ ...editForm.value, price: Number(editForm.value.price) }),
    })
    const updatedListing = await response.json().catch(() => ({}))
    if (!response.ok) throw new Error(updatedListing.message || 'Listing could not be updated.')
    listing.value = updatedListing
    selectedImage.value = updatedListing.pictureUrl || updatedListing.pictureUrls?.[0] || ''
    isEditing.value = false
    saveMessage.value = 'Listing updated successfully.'
  } catch (err) {
    error.value = err.message || 'Could not update listing.'
  }
}

onMounted(async () => {
  try {
    const response = await fetch(`/api/entries/${route.params.id}`)
    if (!response.ok) throw new Error('Listing could not be found.')
    listing.value = await response.json()
    selectedImage.value = listing.value.pictureUrl || listing.value.pictureUrls?.[0] || ''
  } catch (err) {
    error.value = err.message || 'Unable to load listing.'
  }
})
</script>

<template>
  <main class="detail-page">
    <RouterLink class="back-link" to="/">← Back to listings</RouterLink>
    <p v-if="error" class="error">{{ error }}</p>
    <section v-else-if="listing" class="detail-card">
      <div class="gallery">
        <div class="main-image">
          <img v-if="selectedImage" :src="selectedImage" :alt="listing.address" />
        </div>
        <div class="thumbs">
          <button
            v-for="picture in listing.pictureUrls?.length
              ? listing.pictureUrls
              : [listing.pictureUrl]"
            :key="picture"
            type="button"
            :class="{ selected: selectedImage === picture }"
            @click="selectedImage = picture"
          >
            <img :src="picture" :alt="listing.address" />
          </button>
        </div>
      </div>
      <div v-if="!isEditing" class="detail-copy">
        <span :class="['status', listing.status === 'CLOSED' ? 'closed' : 'active']">{{
          listing.status === 'CLOSED' ? 'Just closed' : 'Active'
        }}</span>
        <p class="eyebrow">{{ listing.city }}, {{ listing.state }} {{ listing.zipcode }}</p>
        <h1>{{ listing.address }}</h1>
        <p class="price">
          {{
            Number(listing.price).toLocaleString('en-US', {
              style: 'currency',
              currency: 'USD',
              maximumFractionDigits: 0,
            })
          }}
        </p>
        <p>{{ listing.description || 'Explore the property gallery and contact Briana for additional details, availability, or a private showing.' }}</p>
        <div class="contact-actions">
          <a class="contact" href="tel:8638439024">Call Briana</a>
          <a class="contact contact-email" :href="emailHref">Email Briana</a>
          <button v-if="isAdmin" class="edit-listing" type="button" @click="startEdit">Edit listing</button>
        </div>
      </div>
      <form v-else class="detail-edit-form" @submit.prevent="saveListing">
        <label>Address<input v-model="editForm.address" required type="text" /></label>
        <div class="edit-location">
          <label>City<input v-model="editForm.city" required type="text" /></label>
          <label>State<input v-model="editForm.state" required maxlength="2" type="text" /></label>
          <label>ZIP Code<input v-model="editForm.zipcode" required type="text" /></label>
        </div>
        <label>Price<input v-model="editForm.price" required min="0" step="0.01" type="number" /></label>
        <label>Status<select v-model="editForm.status"><option value="ACTIVE">Active</option><option value="CLOSED">Just closed</option></select></label>
        <label>Description<textarea v-model="editForm.description" required rows="6" placeholder="Describe this property"></textarea></label>
        <div class="edit-actions"><button type="submit">Save changes</button><button class="cancel-edit" type="button" @click="cancelEdit">Cancel</button></div>
      </form>
    </section>
    <p v-if="saveMessage" class="success">{{ saveMessage }}</p>
  </main>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding: 2rem;
  color: #273124;
  background: #f8f4ed;
  font-family: Georgia, 'Times New Roman', serif;
}
.back-link {
  display: inline-block;
  margin-bottom: 2rem;
  color: #273124;
  font:
    700 0.8rem Arial,
    sans-serif;
  text-decoration: none;
}
.detail-card {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 3rem;
  padding: 2rem;
  background: #fffdf8;
}
.main-image {
  height: 520px;
  background: #e9eee2;
}
.main-image img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}
.thumbs {
  display: flex;
  gap: 0.6rem;
  margin-top: 0.7rem;
  overflow-x: auto;
}
.thumbs button {
  flex: 0 0 82px;
  padding: 0;
  border: 2px solid transparent;
  background: none;
  cursor: pointer;
}
.thumbs button.selected {
  border-color: #df481d;
}
.thumbs img {
  width: 78px;
  height: 62px;
  display: block;
  object-fit: cover;
}
.detail-copy {
  align-self: center;
}
.eyebrow {
  color: #68705f;
  font:
    700 0.75rem Arial,
    sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
h1 {
  margin: 0.8rem 0;
  font-size: clamp(2.5rem, 5vw, 4.5rem);
  line-height: 0.95;
}
.price {
  color: #df481d;
  font:
    700 1.5rem Arial,
    sans-serif;
}
.detail-copy > p:not(.eyebrow):not(.price) {
  color: #6a766d;
  font:
    1rem/1.7 Arial,
    sans-serif;
}
.status {
  display: inline-block;
  padding: 0.45rem 0.7rem;
  color: white;
  font:
    700 0.7rem Arial,
    sans-serif;
  text-transform: uppercase;
}
.status.active {
  background: #df481d;
}
.status.closed {
  background: #1877f2;
}
.contact-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  margin-top: 1rem;
}
.contact {
  display: inline-block;
  padding: 0.9rem 1.1rem;
  color: white;
  background: #df481d;
  border-radius: 999px;
  font:
    700 0.8rem Arial,
    sans-serif;
  text-decoration: none;
}
.contact-email {
  color: #273124;
  background: #e9eee2;
}
.edit-listing, .detail-edit-form button { border: 0; cursor: pointer; }
.edit-listing { padding: .9rem 1.1rem; color: #273124; background: #e9eee2; border-radius: 999px; font: 700 .8rem Arial, sans-serif; }
.detail-edit-form { display: grid; gap: 1rem; align-self: center; font: 700 .8rem Arial, sans-serif; color: #273124; }
.detail-edit-form label { display: grid; gap: .35rem; }
.detail-edit-form input, .detail-edit-form select, .detail-edit-form textarea { box-sizing: border-box; width: 100%; padding: .7rem .8rem; border: 1px solid #cbd6cc; color: #273124; background: #f8f4ed; font: 1rem Arial, sans-serif; }
.detail-edit-form textarea { resize: vertical; }
.edit-location { display: grid; grid-template-columns: 1fr .45fr .75fr; gap: .7rem; }
.edit-actions { display: flex; gap: .7rem; }
.edit-actions button { padding: .85rem 1rem; color: #fff; background: #df481d; font: 700 .8rem Arial, sans-serif; }
.edit-actions .cancel-edit { color: #273124; background: #e9eee2; }
.success { max-width: 1100px; margin: 1rem auto 0; color: #166534; font: 700 1rem Arial, sans-serif; }
.error {
  color: #a63e2a;
  font:
    700 1rem Arial,
    sans-serif;
}
@media (max-width: 780px) {
  .detail-page {
    padding: 1rem;
  }
  .detail-card {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 1rem;
  }
  .main-image {
    height: 350px;
  }
  .thumbs {
    flex-wrap: wrap;
    overflow-x: visible;
  }
  .thumbs button {
    flex-basis: 82px;
  }
  .edit-location { grid-template-columns: 1fr; }
}
</style>
