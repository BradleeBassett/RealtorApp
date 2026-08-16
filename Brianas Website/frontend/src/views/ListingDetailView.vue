<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { buildEmailHref, getStoredUserName } from '../utils/contact'

const route = useRoute()
const listing = ref(null)
const error = ref('')
const selectedImage = ref('')
const emailHref = computed(() => buildEmailHref({ userName: getStoredUserName(), listing: listing.value }))

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
        <div class="main-image"><img v-if="selectedImage" :src="selectedImage" :alt="listing.address" /></div>
        <div class="thumbs"><button v-for="picture in (listing.pictureUrls?.length ? listing.pictureUrls : [listing.pictureUrl])" :key="picture" type="button" :class="{ selected: selectedImage === picture }" @click="selectedImage = picture"><img :src="picture" :alt="listing.address" /></button></div>
      </div>
      <div class="detail-copy">
        <span :class="['status', listing.status === 'CLOSED' ? 'closed' : 'active']">{{ listing.status === 'CLOSED' ? 'Just closed' : 'Active' }}</span>
        <p class="eyebrow">{{ listing.city }}, {{ listing.state }} {{ listing.zipcode }}</p>
        <h1>{{ listing.address }}</h1>
        <p class="price">{{ Number(listing.price).toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }) }}</p>
        <p>Explore the property gallery and contact Briana for additional details, availability, or a private showing.</p>
        <div class="contact-actions">
          <a class="contact" href="tel:8638439024">Call Briana</a>
          <a class="contact contact-email" :href="emailHref">Email Briana</a>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.detail-page { min-height: 100vh; padding: 2rem; color: #273124; background: #f8f4ed; font-family: Georgia, 'Times New Roman', serif; }
.back-link { display: inline-block; margin-bottom: 2rem; color: #273124; font: 700 .8rem Arial, sans-serif; text-decoration: none; }
.detail-card { max-width: 1100px; margin: 0 auto; display: grid; grid-template-columns: 1.2fr .8fr; gap: 3rem; padding: 2rem; background: #fffdf8; }
.main-image { height: 520px; background: #e9eee2; }
.main-image img { width: 100%; height: 100%; display: block; object-fit: cover; }
.thumbs { display: flex; gap: .6rem; margin-top: .7rem; overflow-x: auto; }
.thumbs button { flex: 0 0 82px; padding: 0; border: 2px solid transparent; background: none; cursor: pointer; }
.thumbs button.selected { border-color: #df481d; }
.thumbs img { width: 78px; height: 62px; display: block; object-fit: cover; }
.detail-copy { align-self: center; }
.eyebrow { color: #68705f; font: 700 .75rem Arial, sans-serif; letter-spacing: .08em; text-transform: uppercase; }
h1 { margin: .8rem 0; font-size: clamp(2.5rem, 5vw, 4.5rem); line-height: .95; }
.price { color: #df481d; font: 700 1.5rem Arial, sans-serif; }
.detail-copy > p:not(.eyebrow):not(.price) { color: #6a766d; font: 1rem/1.7 Arial, sans-serif; }
.status { display: inline-block; padding: .45rem .7rem; color: white; font: 700 .7rem Arial, sans-serif; text-transform: uppercase; }
.status.active { background: #df481d; }
.status.closed { background: #1877f2; }
.contact-actions { display: flex; flex-wrap: wrap; gap: .7rem; margin-top: 1rem; }
.contact { display: inline-block; padding: .9rem 1.1rem; color: white; background: #df481d; border-radius: 999px; font: 700 .8rem Arial, sans-serif; text-decoration: none; }
.contact-email { color: #273124; background: #e9eee2; }
.error { color: #a63e2a; font: 700 1rem Arial, sans-serif; }
@media (max-width: 780px) { .detail-page { padding: 1rem; } .detail-card { grid-template-columns: 1fr; gap: 1.5rem; padding: 1rem; } .main-image { height: 350px; } }
</style>
