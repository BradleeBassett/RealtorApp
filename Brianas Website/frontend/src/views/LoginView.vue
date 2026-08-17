<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { saveAuthSession } from '../utils/auth'

const router = useRouter()
const authMode = ref('login')
const form = ref({ firstName: '', lastName: '', phoneNumber: '', email: '', password: '' })
const error = ref('')
const success = ref('')

const submit = async () => {
  error.value = ''
  success.value = ''

  try {
    const endpoint = authMode.value === 'login' ? '/api/auth/login' : '/api/auth/register'
    const payload =
      authMode.value === 'login'
        ? { email: form.value.email, password: form.value.password }
        : { ...form.value, role: 'USER' }

    const response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    const data = await response.json().catch(() => ({}))

    if (!response.ok) throw new Error(data.message || 'Authentication failed.')

    if (authMode.value === 'register') {
      success.value = 'Account created. Check your email for a verification link before logging in.'
      authMode.value = 'login'
      form.value = { firstName: '', lastName: '', phoneNumber: '', email: '', password: '' }
      return
    }

    saveAuthSession(data)
    router.push('/')
  } catch (err) {
    error.value = err.message || 'Something went wrong.'
  }
}
</script>

<template>
  <main class="auth-page">
    <RouterLink class="back-link" to="/">← Back to Sweetwater Land Company</RouterLink>
    <section class="auth-card">
      <div class="auth-header">
        <p class="eyebrow">Welcome back</p>
        <h1>{{ authMode === 'login' ? 'Login' : 'Create your account' }}</h1>
        <p>Save homes, follow your search, and pick up where you left off.</p>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <div v-if="authMode === 'register'" class="row">
          <label>First Name<input v-model="form.firstName" type="text" required /></label>
          <label>Last Name<input v-model="form.lastName" type="text" required /></label>
        </div>
        <label v-if="authMode === 'register'"
          >Phone Number
          <input v-model="form.phoneNumber" type="tel" autocomplete="tel" required />
        </label>
        <label>Email<input v-model="form.email" type="email" required /></label>
        <label>Password<input v-model="form.password" type="password" required /></label>
        <button type="submit">{{ authMode === 'login' ? 'Login' : 'Create Account' }}</button>
      </form>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>
      <button
        class="link-btn"
        type="button"
        @click="authMode = authMode === 'login' ? 'register' : 'login'"
      >
        {{ authMode === 'login' ? 'Need an account? Register' : 'Already have an account? Login' }}
      </button>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  position: relative;
  padding: 2rem;
  color: #20332d;
  background: #dfe7df;
  font-family: Georgia, 'Times New Roman', serif;
}
.back-link {
  position: absolute;
  top: 2rem;
  left: 2rem;
  padding: 0.7rem 1rem;
  color: #20332d;
  background: rgba(255, 253, 248, 0.72);
  border: 1px solid rgba(32, 51, 45, 0.18);
  border-radius: 999px;
  box-shadow: 0 8px 20px rgba(32, 51, 45, 0.08);
  font:
    700 0.8rem Arial,
    sans-serif;
  text-decoration: none;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.back-link:hover,
.back-link:focus-visible {
  color: #f7f4ed;
  background: #c65d3b;
  border-color: #c65d3b;
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(198, 93, 59, 0.24);
}
.auth-card {
  width: min(500px, 100%);
  padding: 2.5rem;
  background: #fffdf8;
  box-shadow: 0 24px 70px rgba(32, 51, 45, 0.12);
}
.auth-header {
  margin-bottom: 1.8rem;
}
.eyebrow {
  margin: 0 0 0.7rem;
  color: #c65d3b;
  font:
    700 0.72rem/1 Arial,
    sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
h1 {
  margin: 0 0 0.7rem;
  font-size: 3rem;
  line-height: 0.95;
  letter-spacing: -0.04em;
}
.auth-header > p:last-child {
  color: #6a766d;
  line-height: 1.5;
}
.auth-form {
  display: grid;
  gap: 1rem;
}
.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
label {
  display: grid;
  gap: 0.4rem;
  color: #52635a;
  font:
    700 0.75rem Arial,
    sans-serif;
}
input {
  width: 100%;
  padding: 0.85rem;
  border: 1px solid #cbd6cc;
  border-radius: 0;
  color: #20332d;
  background: #f7f4ed;
  font:
    1rem Georgia,
    serif;
}
button {
  width: 100%;
  padding: 0.9rem 1rem;
  border: 0;
  border-radius: 999px;
  color: #f7f4ed;
  background: #20332d;
  font:
    700 0.8rem Arial,
    sans-serif;
  cursor: pointer;
}
button:hover {
  background: #c65d3b;
}
.link-btn {
  margin-top: 1rem;
  color: #20332d;
  background: transparent;
  border: 1px solid #cbd6cc;
}
.error {
  color: #a63e2a;
  font:
    700 0.85rem Arial,
    sans-serif;
}
.success {
  color: #32724b;
  font:
    700 0.85rem Arial,
    sans-serif;
}
@media (max-width: 560px) {
  .auth-page {
    padding: 1.5rem;
  }
  .back-link {
    top: 1.2rem;
    left: 1.5rem;
  }
  .auth-card {
    padding: 1.7rem;
  }
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
