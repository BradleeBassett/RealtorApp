<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import brianaLogo from '../assets/brianalogo.png'
import brianaFlier from '../assets/BrianaFlier.jpg'
import { buildEmailHref } from '../utils/contact'

const user = ref(null)
const listings = ref([])
const listingFilter = ref('ALL')
const selectedAddress = ref('')
const addressFilter = ref('')
const listingSearchQuery = ref('')
const maximumPrice = ref(1000000)
const activePosition = ref(0)
const closedPosition = ref(0)
const viewportWidth = ref(typeof window === 'undefined' ? 1200 : window.innerWidth)
const demoImages = [
  'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1200&q=85',
  'https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=85',
  'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=85',
]
const demoListings = [
  ...Array.from({ length: 10 }, (_, index) => ({
    id: `demo-active-${index}`,
    address: `${100 + index} Caloosa Drive`,
    city: index % 2 ? 'Fort Myers' : 'LaBelle',
    state: 'FL',
    zipcode: '33935',
    price: 425000 + index * 27500,
    pictureUrl: demoImages[index % 3],
    status: 'ACTIVE',
  })),
  ...Array.from({ length: 10 }, (_, index) => ({
    id: `demo-closed-${index}`,
    address: `${200 + index} Orange Grove Lane`,
    city: index % 2 ? 'LaBelle' : 'Fort Myers',
    state: 'FL',
    zipcode: '33935',
    price: 390000 + index * 22000,
    pictureUrl: demoImages[(index + 1) % 3],
    status: 'CLOSED',
  })),
]
const isClosedListing = (listing) =>
  String(listing.status || '')
    .trim()
    .toUpperCase() === 'CLOSED'
const matchesAddress = (listing) => !addressFilter.value || listing.address === addressFilter.value
const matchesListingSearch = (listing) => {
  const query = listingSearchQuery.value.trim().toLowerCase()
  if (!query) return true
  return [listing.address, listing.city, listing.state, listing.zipcode]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
    .includes(query)
}
const matchesMaximumPrice = (listing) => Number(listing.price) <= maximumPrice.value
const activeListings = computed(() =>
  listings.value.filter(
    (listing) =>
      !isClosedListing(listing) &&
      matchesAddress(listing) &&
      matchesListingSearch(listing) &&
      matchesMaximumPrice(listing),
  ),
)
const closedListings = computed(() =>
  listings.value.filter((listing) => isClosedListing(listing) && matchesAddress(listing)),
)
const chunkListings = (collection, size = 3) =>
  Array.from({ length: Math.ceil(collection.length / size) }, (_, index) =>
    collection.slice(index * size, index * size + size),
  )
const cardsPerPage = computed(() =>
  viewportWidth.value <= 780 ? 1 : viewportWidth.value <= 1020 ? 2 : 3,
)
const activePages = computed(() => chunkListings(activeListings.value, cardsPerPage.value))
const closedPages = computed(() => chunkListings(closedListings.value, cardsPerPage.value))
const showActiveListings = computed(() => listingFilter.value !== 'CLOSED')
const showClosedListings = computed(() => listingFilter.value !== 'ACTIVE')
const availableAddresses = computed(() => [
  ...new Set(listings.value.map((listing) => listing.address).filter(Boolean)),
])
const activeCarouselStyle = computed(() => ({
  transform: `translateX(-${activePosition.value * 100}%)`,
}))
const closedCarouselStyle = computed(() => ({
  transform: `translateX(-${closedPosition.value * 100}%)`,
}))

const loadListings = async () => {
  try {
    const response = await fetch('/api/entries')
    if (response.ok) {
      const payload = await response.json()
      listings.value = payload.length ? payload : demoListings
    }
  } catch {
    listings.value = []
  }
}

const moveCarousel = (type, direction) => {
  const collection = type === 'active' ? activePages.value : closedPages.value
  const position = type === 'active' ? activePosition : closedPosition
  if (!collection.length) return
  const next = position.value + direction
  position.value = next < 0 ? collection.length - 1 : next >= collection.length ? 0 : next
}

watch([listingFilter, addressFilter, listingSearchQuery, maximumPrice], () => {
  activePosition.value = 0
  closedPosition.value = 0
})

const formatPrice = (price) =>
  Number(price || 0).toLocaleString('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  })

const searchListings = () => {
  addressFilter.value = selectedAddress.value
}

const clearListingSearch = () => {
  selectedAddress.value = ''
  addressFilter.value = ''
  listingSearchQuery.value = ''
  maximumPrice.value = 1000000
}

const isAdmin = computed(() => user.value?.role === 'ADMIN')
const displayName = computed(() => {
  const name = [user.value?.firstName, user.value?.lastName].filter(Boolean).join(' ')
  return name || user.value?.email || 'Account'
})
const emailHref = computed(() =>
  buildEmailHref({ userName: user.value ? displayName.value : undefined }),
)

const loadUser = () => {
  try {
    user.value = JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    user.value = null
    localStorage.removeItem('user')
  }
}

const logout = () => {
  localStorage.removeItem('user')
  user.value = null
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  loadUser()
  loadListings()
  window.addEventListener('resize', updateViewportWidth)
})

const updateViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportWidth)
})
</script>

<template>
  <main class="landing-page">
    <button
      class="return-top"
      type="button"
      aria-label="Return to top"
      title="Return to top"
      @click="scrollToTop"
    >
      ↑
    </button>
    <nav class="navbar" aria-label="Main navigation">
      <RouterLink class="brand" to="/">
        <img :src="brianaLogo" alt="Briana Chance REALTOR logo" />
      </RouterLink>

      <div class="nav-links nav-center">
        <a href="#listings">Listings</a>
        <a href="#about">About Briana</a>
        <a href="#contact">Contact</a>
        <RouterLink v-if="isAdmin" class="nav-action nav-action-muted" to="/admin"
          >Admin panel</RouterLink
        >
        <RouterLink v-if="isAdmin" class="nav-action nav-action-muted" to="/admin/listings"
          >Manage listings</RouterLink
        >
      </div>

      <div class="nav-account">
        <template v-if="user">
          <span class="signed-in">{{ displayName }}</span>
          <button class="nav-action" type="button" @click="logout">Logout</button>
        </template>
        <RouterLink v-else class="nav-action nav-login" to="/login">Login</RouterLink>
      </div>
    </nav>

    <section class="hero">
      <div class="hero-copy">
        <h1>Rooted in Southwest Florida. <em>Ready for what’s next.</em></h1>
        <p class="hero-text">
          Personal guidance for buying, selling, investing, acreage, and groves across LaBelle, Fort
          Myers, and the communities in between.
        </p>
        <div class="hero-actions">
          <a class="primary-button" href="#listings"
            >View current listings <span aria-hidden="true">↗</span></a
          >
          <a class="text-link" href="tel:8638439024">Call Briana</a>
        </div>
        <div class="service-strip">
          <span>Real Estate</span><span>Investments</span><span>Acreage</span><span>Groves</span>
        </div>
      </div>

      <div class="hero-image">
        <img :src="brianaFlier" alt="Briana Chance, REALTOR, with a Southwest Florida home" />
      </div>
    </section>

    <section id="listings" class="listings-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">The market, thoughtfully presented</p>
          <h2>Current active listings</h2>
        </div>
        <p>Available homes are updated as your search evolves.</p>
      </div>
      <div class="listing-filter" aria-label="Filter listings">
        <span>Show:</span>
        <button
          :class="{ selected: listingFilter === 'ALL' }"
          type="button"
          @click="listingFilter = 'ALL'"
        >
          All listings
        </button>
        <button
          :class="{ selected: listingFilter === 'ACTIVE' }"
          type="button"
          @click="listingFilter = 'ACTIVE'"
        >
          Active
        </button>
        <button
          :class="{ selected: listingFilter === 'CLOSED' }"
          type="button"
          @click="listingFilter = 'CLOSED'"
        >
          Just closed
        </button>
      </div>
      <form class="address-search" @submit.prevent="searchListings">
        <label for="listing-search"
          >Search active listings<input
            id="listing-search"
            v-model="listingSearchQuery"
            type="search"
            placeholder="Address, city, state, or ZIP code"
        /></label>
        <label for="address-search"
          >Look up an address<select id="address-search" v-model="selectedAddress">
            <option value="">Select an available address</option>
            <option v-for="address in availableAddresses" :key="address" :value="address">
              {{ address }}
            </option>
          </select></label
        >
        <label class="price-filter" for="maximum-price"
          >Maximum price <strong>{{ formatPrice(maximumPrice) }}</strong
          ><input
            id="maximum-price"
            v-model.number="maximumPrice"
            min="100000"
            max="1000000"
            step="25000"
            type="range"
          />
        </label>
        <div class="search-actions">
          <button type="submit">Search</button>
          <button
            class="clear-search"
            type="button"
            :disabled="!addressFilter && !listingSearchQuery && maximumPrice === 1000000"
            @click="clearListingSearch"
          >
            Clear
          </button>
        </div>
      </form>
      <div v-if="showActiveListings && activeListings.length" class="listing-carousel">
        <button
          class="carousel-arrow previous"
          type="button"
          aria-label="Previous active listing"
          @click="moveCarousel('active', -1)"
        >
          ‹
        </button>
        <div class="carousel-viewport">
          <div class="carousel-track" :style="activeCarouselStyle">
            <div v-for="(page, pageIndex) in activePages" :key="pageIndex" class="carousel-page">
              <RouterLink
                v-for="(listing, index) in page"
                :key="listing.id"
                class="home-card carousel-card"
                :to="`/listings/${listing.id}`"
              >
                <div :class="['card-image', `image-${((pageIndex * 3 + index) % 3) + 1}`]">
                  <img
                    v-if="listing.pictureUrl"
                    :key="listing.pictureUrl"
                    :src="listing.pictureUrl"
                    :alt="listing.address"
                  /><span class="listing-status active">Active</span>
                </div>
                <div class="card-content">
                  <span>{{ listing.city }}, {{ listing.state }}</span>
                  <h3>{{ listing.address }}</h3>
                  <p>{{ formatPrice(listing.price) }} · {{ listing.zipcode }}</p>
                </div>
              </RouterLink>
            </div>
          </div>
        </div>
        <button
          class="carousel-arrow next"
          type="button"
          aria-label="Next active listing"
          @click="moveCarousel('active', 1)"
        >
          ›
        </button>
      </div>
      <div v-else-if="showActiveListings" class="empty-listings">
        {{
          listingSearchQuery || addressFilter || maximumPrice !== 1000000
            ? 'No active listings match your search.'
            : 'New active listings are coming soon. Contact Briana to find your Florida place.'
        }}
      </div>
      <div v-if="false" class="home-grid">
        <article class="home-card home-card-large">
          <div class="card-image image-one"><span class="listing-status">Active</span></div>
          <div class="card-content">
            <span>LaBelle, FL</span>
            <h3>Southwest Florida living</h3>
            <p>Contact Briana for current availability</p>
          </div>
        </article>
        <article class="home-card">
          <div class="card-image image-two"><span class="listing-status">Active</span></div>
          <div class="card-content">
            <span>Fort Myers, FL</span>
            <h3>Find your Florida place</h3>
            <p>Ask about homes, land, and investments</p>
          </div>
        </article>
        <article class="home-card">
          <div class="card-image image-three"><span class="listing-status">Active</span></div>
          <div class="card-content">
            <span>Southwest Florida</span>
            <h3>Land with possibility</h3>
            <p>Real estate, acreage, and groves</p>
          </div>
        </article>
      </div>
    </section>

    <section v-if="showClosedListings" class="closed-section">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Just closed</p>
          <h2>Moves worth celebrating</h2>
        </div>
      </div>
      <div v-if="closedListings.length" class="listing-carousel closed-carousel">
        <button
          class="carousel-arrow previous"
          type="button"
          aria-label="Previous closed listing"
          @click="moveCarousel('closed', -1)"
        >
          ‹
        </button>
        <div class="carousel-viewport">
          <div class="carousel-track" :style="closedCarouselStyle">
            <div v-for="(page, pageIndex) in closedPages" :key="pageIndex" class="carousel-page">
              <RouterLink
                v-for="(listing, index) in page"
                :key="listing.id"
                class="home-card carousel-card"
                :to="`/listings/${listing.id}`"
              >
                <div :class="['card-image', `image-${((pageIndex * 3 + index) % 3) + 1}`]">
                  <img
                    v-if="listing.pictureUrl"
                    :key="listing.pictureUrl"
                    :src="listing.pictureUrl"
                    :alt="listing.address"
                  /><span class="listing-status closed">Just closed</span>
                </div>
                <div class="card-content">
                  <span>{{ listing.city }}, {{ listing.state }}</span>
                  <h3>{{ listing.address }}</h3>
                  <p>{{ formatPrice(listing.price) }} · {{ listing.zipcode }}</p>
                </div>
              </RouterLink>
            </div>
          </div>
        </div>
        <button
          class="carousel-arrow next"
          type="button"
          aria-label="Next closed listing"
          @click="moveCarousel('closed', 1)"
        >
          ›
        </button>
      </div>
      <div v-else class="closed-grid">
        <div>
          <strong>Closed with care</strong><span>Recent success stories will appear here.</span>
        </div>
        <div><strong>Local knowledge</strong><span>LaBelle, Fort Myers, and beyond.</span></div>
        <div><strong>Personal service</strong><span>Advice shaped around your goals.</span></div>
      </div>
    </section>

    <section id="about" class="about-section">
      <div class="about-copy">
        <p class="eyebrow">Meet your REALTOR</p>
        <h2>Briana Chance, REALTOR®, GRI, MRP</h2>
        <p>
          Briana brings local perspective, honest guidance, and personal service to every move.
          Whether you are buying your first home, selling land, investing, or planning your next
          chapter, she is here to help you move with confidence.
        </p>
        <p class="credentials">
          Florida Real Estate Sales Associate · SL3606334<br />Sweetwater Land Company, LLC
        </p>
      </div>
      <img :src="brianaLogo" alt="Briana Chance REALTOR logo" class="about-logo" />
    </section>

    <section id="contact" class="contact-section">
      <div>
        <p class="eyebrow">Let’s talk real estate</p>
        <h2>Ready when you are.</h2>
        <p>Questions about a listing, land, selling, or investing? Reach Briana directly.</p>
      </div>
      <div class="contact-actions">
        <a class="contact-button" href="tel:8638439024">Call 863-843-9024</a
        ><a class="contact-button contact-button-light" :href="emailHref">Email Briana</a>
        <div class="social-links">
          <a
            href="https://www.linkedin.com/in/briana-chance-969ab9274?utm_source=share_via&utm_content=profile&utm_medium=member_ios"
            target="_blank"
            rel="noreferrer"
            >LinkedIn</a
          ><a
            href="https://www.facebook.com/share/1AmhL4op2Y/?mibextid=wwXIfr"
            target="_blank"
            rel="noreferrer"
            >Facebook</a
          >
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.landing-page {
  min-height: 100vh;
  color: #273124;
  background: #f8f4ed;
  font-family: Georgia, 'Times New Roman', serif;
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
  background: #df481d;
  border: 0;
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(39, 49, 36, 0.2);
  font:
    1.35rem/1 Georgia,
    serif;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.return-top:hover,
.return-top:focus-visible {
  background: #273124;
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(39, 49, 36, 0.28);
}
.navbar {
  position: absolute;
  z-index: 2;
  top: 1.3rem;
  left: 50%;
  transform: translateX(-50%);
  width: min(1160px, calc(100% - 3rem));
  padding: 0.6rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  background: rgba(255, 252, 245, 0.94);
  border: 1px solid rgba(39, 49, 36, 0.15);
  border-radius: 999px;
  box-shadow: 0 12px 32px rgba(39, 49, 36, 0.1);
  backdrop-filter: blur(14px);
}
.brand img {
  display: block;
  width: 81px;
  height: auto;
}
.nav-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem 0.75rem;
  min-width: 0;
  font:
    700 0.75rem Arial,
    sans-serif;
}
.nav-center {
  flex: 1;
  flex-wrap: wrap;
  margin: 0 1rem;
}
.nav-account {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.55rem;
  flex: 0 0 auto;
  min-width: max-content;
  font:
    700 0.75rem Arial,
    sans-serif;
}
.nav-links a:not(.nav-action),
.signed-in {
  padding: 0.7rem 0.95rem;
  color: #273124;
  border: 1px solid transparent;
  border-radius: 999px;
  text-decoration: none;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.nav-links a:not(.nav-action):hover,
.nav-links a:not(.nav-action):focus-visible {
  color: #df481d;
  background: #fff8ef;
  border-color: rgba(223, 72, 29, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 5px 12px rgba(39, 49, 36, 0.08);
}
.nav-action {
  padding: 0.78rem 1.2rem;
  border: 0;
  border-radius: 999px;
  color: #fff !important;
  background: #e94718;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.nav-action:hover,
.nav-action:focus-visible {
  background: #c83b14;
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(223, 72, 29, 0.25);
}
.nav-login {
  margin-left: 0;
}
.nav-action-muted {
  color: #273124 !important;
  background: transparent;
  border: 1px solid rgba(39, 49, 36, 0.2);
}
.nav-action-muted:hover,
.nav-action-muted:focus-visible {
  color: #273124 !important;
  background: #fff8ef;
  border-color: rgba(39, 49, 36, 0.4);
  box-shadow: 0 5px 12px rgba(39, 49, 36, 0.08);
}
.signed-in {
  margin-left: auto;
  color: #68705f;
  background: #eef1e7;
  border-color: rgba(104, 112, 95, 0.15) !important;
}
.hero {
  min-height: 720px;
  display: grid;
  grid-template-columns: 47% 53%;
  padding: 9rem max(1.5rem, calc((100% - 1200px) / 2)) 5rem;
  background: #e9eee2;
}
.hero-copy {
  align-self: center;
  max-width: 600px;
  padding: 2rem 3rem 2rem 0;
}
.eyebrow {
  margin: 0 0 0.75rem;
  color: #df481d;
  font:
    700 0.72rem/1 Arial,
    sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
h1 {
  margin: 0;
  color: #273124;
  font-size: clamp(3.2rem, 5.5vw, 6rem);
  line-height: 0.93;
  letter-spacing: -0.04em;
}
h1 em {
  color: #df481d;
  font-weight: 400;
}
.hero-text {
  max-width: 500px;
  margin: 1.8rem 0;
  color: #606b59;
  font-size: 1.08rem;
  line-height: 1.6;
}
.hero-actions {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}
.primary-button,
.contact-button {
  padding: 0.95rem 1.2rem;
  color: #fff;
  background: #df481d;
  border-radius: 999px;
  font:
    700 0.8rem Arial,
    sans-serif;
  text-decoration: none;
}
.primary-button span {
  margin-left: 0.8rem;
  font-size: 1.1rem;
}
.text-link {
  color: #273124;
  font:
    700 0.8rem Arial,
    sans-serif;
  text-decoration: underline;
  text-underline-offset: 0.3rem;
}
.service-strip {
  display: flex;
  gap: 1rem;
  margin-top: 3.5rem;
  color: #68705f;
  font:
    700 0.68rem Arial,
    sans-serif;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}
.service-strip span + span::before {
  content: '/';
  margin-right: 1rem;
  color: #df481d;
}
.hero-image {
  min-height: 570px;
  overflow: hidden;
  background: #ede7dd;
  border-radius: 2px 100px 2px 100px;
}
.hero-image img {
  width: 100%;
  height: 100%;
  min-height: 570px;
  display: block;
  object-fit: cover;
  object-position: center;
}
.listings-section,
.closed-section,
.about-section,
.contact-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 7rem 1.5rem;
}
.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 2rem;
  margin-bottom: 2rem;
}
h2 {
  margin: 0;
  font-size: clamp(2.2rem, 4vw, 4.2rem);
  line-height: 1;
  letter-spacing: -0.035em;
}
.section-heading > p,
.contact-section p,
.about-copy > p {
  color: #6a766d;
  font:
    0.85rem/1.6 Arial,
    sans-serif;
}
.listing-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: -0.5rem 0 2rem;
  color: #68705f;
  font:
    700 0.78rem Arial,
    sans-serif;
}
.listing-filter button {
  padding: 0.65rem 0.9rem;
  color: #273124;
  background: #fffdf8;
  border: 1px solid #d8ded0;
  border-radius: 999px;
  font: inherit;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}
.listing-filter button:hover,
.listing-filter button:focus-visible {
  border-color: #df481d;
  color: #df481d;
  transform: translateY(-2px);
}
.listing-filter button.selected {
  color: #fff;
  background: #df481d;
  border-color: #df481d;
}
.address-search {
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin: 0 0 2.5rem;
  padding: 1rem;
  background: #fffdf8;
  border: 1px solid #d8ded0;
}
.address-search label {
  min-width: 0;
  flex: 1 1 280px;
  display: grid;
  gap: 0.35rem;
  color: #68705f;
  font:
    700 0.75rem Arial,
    sans-serif;
}

.address-search input,
.address-search select {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  padding: 0.7rem 0.8rem;
  color: #273124;
  background: #f8f4ed;
  border: 1px solid #cbd6cc;
  border-radius: 999px;
  font:
    0.85rem Arial,
    sans-serif;
}
.address-search .price-filter {
  flex-basis: 220px;
}
.price-filter strong {
  color: #273124;
  font-size: 0.85rem;
}
.address-search input[type='range'] {
  width: 100%;
  padding: 0;
  accent-color: #df481d;
}
.search-actions {
  display: flex;
  align-items: end;
  gap: 0.6rem;
}
.address-search button {
  padding: 0.7rem 1rem;
  color: #fff;
  background: #df481d;
  border: 1px solid #df481d;
  border-radius: 999px;
  font:
    700 0.75rem Arial,
    sans-serif;
  cursor: pointer;
}
.address-search button:hover:not(:disabled) {
  background: #c83b14;
}
.address-search .clear-search {
  color: #273124;
  background: transparent;
  border-color: #cbd6cc;
}
.address-search button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.home-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr;
  gap: 1.2rem;
}
.home-card {
  background: #fffdf8;
}
.listing-carousel {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 1rem;
}
.carousel-viewport {
  min-width: 0;
  overflow: hidden;
}
.carousel-track {
  display: flex;
  transition: transform 0.45s ease;
}
.carousel-page {
  flex: 0 0 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1.2rem;
}
.carousel-card {
  min-width: 0;
  color: inherit;
  text-decoration: none;
}
.carousel-arrow {
  position: relative;
  z-index: 2;
  width: 2.8rem;
  height: 2.8rem;
  display: grid;
  place-items: center;
  padding: 0;
  color: #fff;
  background: #df481d;
  border: 0;
  border-radius: 50%;
  font:
    2rem/1 Georgia,
    serif;
  cursor: pointer;
  touch-action: manipulation;
  transition:
    background-color 0.2s ease,
    transform 0.2s ease;
}
.carousel-arrow:hover,
.carousel-arrow:focus-visible {
  background: #273124;
  transform: scale(1.08);
}
.closed-carousel .carousel-arrow {
  background: #1877f2;
}
.closed-carousel .carousel-arrow:hover,
.closed-carousel .carousel-arrow:focus-visible {
  background: #0d5ec7;
}
.card-image {
  height: 250px;
  position: relative;
  background-position: center;
  background-size: cover;
}
.card-image img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}
.home-card-large .card-image {
  height: 340px;
}
.image-one {
  background-image: url('https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=900&q=85');
}
.image-two {
  background-image: url('https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=900&q=85');
}
.image-three {
  background-image: url('https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?auto=format&fit=crop&w=900&q=85');
}
.listing-status {
  position: absolute;
  top: 1rem;
  left: 1rem;
  padding: 0.45rem 0.65rem;
  color: #fff;
  font:
    700 0.65rem Arial,
    sans-serif;
  text-transform: uppercase;
}
.listing-status.active {
  background: #df481d;
}
.listing-status.closed {
  background: #1877f2;
}
.empty-listings {
  padding: 2rem;
  color: #6a766d;
  background: #fffdf8;
  font:
    1rem/1.6 Arial,
    sans-serif;
}
.card-content {
  padding: 1.35rem 1.4rem 1.55rem;
}
.card-content span {
  display: block;
  color: #68705f;
  font:
    700 0.85rem/1.4 Arial,
    sans-serif;
  letter-spacing: 0.02em;
}
.card-content h3 {
  margin: 0.55rem 0 0.45rem;
  color: #273124;
  font-size: 1.65rem;
  line-height: 1.1;
}
.card-content p {
  margin: 0;
  color: #4f5b4b;
  font:
    700 1rem/1.4 Arial,
    sans-serif;
}
.closed-section {
  background: #68734d;
  color: #fffdf8;
  max-width: none;
  padding-left: max(1.5rem, calc((100% - 1200px) / 2));
  padding-right: max(1.5rem, calc((100% - 1200px) / 2));
}
.closed-section .eyebrow {
  color: #f7c6a5;
}
.closed-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr;
  gap: 1.2rem;
}
.closed-grid > div:not(.home-card) {
  padding: 1.4rem 0;
  border-top: 1px solid rgba(255, 255, 255, 0.35);
}
.closed-grid strong,
.closed-grid span {
  display: block;
}
.closed-grid strong {
  font-size: 1.3rem;
}
.closed-grid span {
  margin-top: 0.4rem;
  color: #e5eadb;
  font:
    0.8rem Arial,
    sans-serif;
}
.about-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: center;
}
.about-copy {
  max-width: 600px;
}
.about-copy > p:not(.eyebrow) {
  font:
    1.08rem/1.7 Georgia,
    serif;
}
.credentials {
  color: #df481d !important;
  font:
    700 0.85rem/1.7 Arial,
    sans-serif !important;
}
.about-logo {
  width: min(100%, 480px);
  justify-self: end;
}
.contact-section {
  display: flex;
  justify-content: space-between;
  gap: 3rem;
  align-items: end;
  background: #273124;
  color: #fffdf8;
  max-width: none;
  padding-left: max(1.5rem, calc((100% - 1200px) / 2));
  padding-right: max(1.5rem, calc((100% - 1200px) / 2));
}
.contact-section h2 {
  color: #fffdf8;
}
.contact-section p {
  max-width: 430px;
  color: #cbd3c2;
}
.contact-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: end;
  gap: 0.7rem;
  max-width: 450px;
}
.contact-button-light {
  color: #273124;
  background: #fffdf8;
}
.social-links {
  width: 100%;
  display: flex;
  justify-content: end;
  gap: 0.65rem;
  margin-top: 0.7rem;
}

.social-links a {
  padding: 0.65rem 0.95rem;
  color: #fff;
  background: #0a66c2;
  border: 1px solid #0a66c2;
  border-radius: 999px;
  font:
    700 0.78rem Arial,
    sans-serif;
  text-decoration: none;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.social-links a + a {
  color: #fff;
  background: #1877f2;
  border-color: #1877f2;
}
.social-links a:hover,
.social-links a:focus-visible {
  color: #fff;
  background: #004182;
  border-color: #004182;
  transform: translateY(-2px);
  box-shadow: 0 7px 16px rgba(0, 0, 0, 0.18);
}
.social-links a + a:hover,
.social-links a + a:focus-visible {
  color: #fff;
  background: #0d5ec7;
  border-color: #0d5ec7;
}
@media (prefers-reduced-motion: reduce) {
  .nav-links a,
  .nav-action,
  .social-links a {
    transition: none;
  }
}
@media (max-width: 1020px) and (min-width: 781px) {
  .navbar {
    align-items: flex-start;
  }
  .nav-center {
    align-content: center;
  }
  .nav-links a:not(.nav-action),
  .nav-action,
  .signed-in {
    flex: 0 0 auto;
  }
}
@media (max-width: 1020px) and (min-width: 781px) {
  .carousel-page {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  
}
@media (max-width: 780px) {
  .navbar {
    top: 0.7rem;
    width: calc(100% - 1.5rem);
    max-width: calc(100% - 1.5rem);
    flex-wrap: wrap;
    overflow: hidden;
    border-radius: 24px;
  }
#listing-search{
  height: 50%; 
}
#address-search{
  height: 50%;
}
  .brand {
    margin-left: 15%;
  }
  .brand img {
    width: 73px;
  }
  .nav-center {
    order: 3;
    flex: 0 0 100%;
    width: 100%;
    max-width: 100%;
    margin: 0.35rem 0 0;
    justify-content: center;
    gap: 0.35rem;
  }
  .nav-account {
    order: 2;
    margin-left: auto;
  }
  .nav-links a:not(.nav-action),
  .nav-action-muted,
  .signed-in {
    display: inline-flex;
    flex: 0 0 auto;
  }
  .nav-links a:not(.nav-action),
  .signed-in {
    padding: 0.55rem 0.7rem;
  }
  .nav-action {
    flex: 0 0 auto;
    padding: 0.65rem 0.9rem;
  }
  .address-search {
    align-items: flex-start;
    flex-direction: column;
  }
  .address-search label {
    width: 100%;
    max-width: 18rem;
  }
  .search-actions {
    width: 100%;
  }
  .address-search input,
  .address-search select {
    width: 100%;
    min-width: 0;
    padding: 0.55rem 0.65rem;
    font-size: 0.8rem;
  }
  .search-actions button {
    flex: 1;
  }
  .listing-carousel {
    gap: 0.5rem;
  }
  .carousel-page {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  .carousel-page .carousel-card:not(:first-child) {
    display: none;
  }
  .carousel-arrow {
    width: 2.35rem;
    height: 2.35rem;
    font-size: 1.7rem;
  }
  .hero {
    display: block;
    min-height: auto;
    padding: 11rem 1.5rem 3rem;
  }
  .hero-copy {
    padding: 2rem 0 3rem;
  }
  .hero-image,
  .hero-image img {
    min-height: 440px;
  }
  .hero-image {
    border-radius: 2px 52px 2px 52px;
  }
  .service-strip {
    flex-wrap: wrap;
    margin-top: 2rem;
  }
  .service-strip span + span::before {
    margin-right: 0.5rem;
  }
  .section-heading,
  .contact-section {
    display: block;
  }
  .section-heading > p {
    margin-top: 1rem;
  }
  .home-grid,
  .closed-grid,
  .about-section {
    grid-template-columns: 1fr;
  }
  .home-card-large .card-image,
  .card-image {
    height: 280px;
  }
  .about-logo {
    justify-self: start;
    max-width: 360px;
  }
  .contact-actions,
  .social-links {
    justify-content: start;
    margin-top: 2rem;
  }
}
</style>
