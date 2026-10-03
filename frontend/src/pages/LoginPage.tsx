import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { login } from '../api/auth'

interface LoginPageProps {
  onLogin: () => void
}

function LoginPage({ onLogin }: LoginPageProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    console.log("Signing in.")

    event.preventDefault()
    if (isSubmitting) return

    setError(null)

    const invalidField = event.currentTarget.querySelector<HTMLInputElement>('input:invalid')
    if (invalidField) {
      setError(invalidField.validity.valueMissing
        ? `Please enter your ${invalidField.name}.`
        : invalidField.validationMessage)
      invalidField.focus()
      return
    }

    setIsSubmitting(true)

    try {
      await login({ email: email.trim(), password })
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Unable to sign in. Please try again.')
      return
    } finally {
      setIsSubmitting(false)
    }

    onLogin()
  }

  return (
    <main>
      <h1>Sign in</h1>
      <p>Sign in to access your test cases.</p>

      <form onSubmit={handleSubmit} aria-busy={isSubmitting} noValidate>
        <div>
          <label htmlFor="login-email">Email</label>
          <input
            id="login-email"
            name="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>

        <div>
          <label htmlFor="login-password">Password</label>
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>

        {error && <p role="alert">{error}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </main>
  )
}

export default LoginPage
