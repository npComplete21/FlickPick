import { useState } from 'react'
import { login, signup } from '../api/auth'
import { ApiError } from '../api/client'

interface LoginScreenProps {
  onAuthenticated: (token: string) => void
}

type Mode = 'login' | 'signup'

function errorMessage(mode: Mode, error: unknown): string {
  if (error instanceof ApiError) {
    if (mode === 'signup' && error.status === 409) return 'That email is already registered.'
    if (mode === 'login' && error.status === 401) return 'Invalid email or password.'
    if (error.status === 400) return 'Please enter a valid email and an 8+ character password.'
  }
  return 'Something went wrong. Please try again.'
}

export default function LoginScreen({ onAuthenticated }: LoginScreenProps) {
  const [mode, setMode] = useState<Mode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    const action = mode === 'login' ? login : signup
    action(email, password)
      .then(onAuthenticated)
      .catch((err: unknown) => setError(errorMessage(mode, err)))
      .finally(() => setSubmitting(false))
  }

  return (
    <main className="auth-page">
      <h1>FlickPick</h1>
      <h2>{mode === 'login' ? 'Log in' : 'Sign up'}</h2>
      <form onSubmit={handleSubmit} className="auth-form">
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required
          />
        </label>
        {error && <p className="auth-error">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Sign up'}
        </button>
      </form>
      <button
        type="button"
        className="auth-toggle"
        onClick={() => {
          setMode(mode === 'login' ? 'signup' : 'login')
          setError(null)
        }}
      >
        {mode === 'login' ? "Don't have an account? Sign up" : 'Already have an account? Log in'}
      </button>
    </main>
  )
}
