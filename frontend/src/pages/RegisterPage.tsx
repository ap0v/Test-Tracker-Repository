import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { Link } from 'react-router'
import { register } from '../api/auth'

interface RegisterProps {
  onRegister: () => void
}

function RegisterPage({ onRegister }: RegisterProps) { 
  const [accountName, setAccountName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    console.log("Registering.")

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

    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setIsSubmitting(true)

    try {
      await register({ accountName: accountName.trim(), email: email.trim(), password })
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Unable to sign in. Please try again.')
      return
    } finally {
      setIsSubmitting(false)
    }

    onRegister()
  }

  return (
    <main>
      <h1>Register</h1>
      <p>Create an account to access your test cases.</p>

      <form onSubmit={handleSubmit} aria-busy={isSubmitting} noValidate>
        <div>
          <label htmlFor="register-account-name">Account Name</label>
          <input
            id="register-account-name"
            name="accountName"
            type="text"
            autoComplete="username"
            value={accountName}
            onChange={(event) => setAccountName(event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>

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
          <label htmlFor="register-password">Password</label>
          <input
            id="register-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>

        <div>
          <label htmlFor="register-confirm-password">Confirm Password</label>
          <input
            id="register-confirm-password"
            name="confirmPassword"
            type="password"
            autoComplete="current-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            disabled={isSubmitting}
            required
          />
        </div>

        {error && <p role="alert">{error}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Registering…' : 'Register'}
        </button>
      </form>
      <p>
        Already have an account? <Link to="/">Sign in</Link>
      </p>
    </main>
  )
}

export default RegisterPage
