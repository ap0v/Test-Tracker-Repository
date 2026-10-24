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
      event.currentTarget.querySelector<HTMLInputElement>('#register-confirm-password')?.focus()
      return
    }

    setIsSubmitting(true)

    try {
      await register({ accountName: accountName.trim(), email: email.trim(), password })
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Unable to create your account. Please try again.')
      return
    } finally {
      setIsSubmitting(false)
    }

    onRegister()
  }

  return (
    <main className="auth-page">
      <section className="auth-panel" aria-labelledby="register-heading">
        <header className="page-introduction">
          <h1 id="register-heading">Create an account.</h1>
          <p>Keep your test cases organized, all in one place.</p>
        </header>

        <form className="form-stack" onSubmit={handleSubmit} aria-busy={isSubmitting} noValidate>
          <div className="form-field">
            <label htmlFor="register-account-name">Account name</label>
            <input
              id="register-account-name"
              name="accountName"
              type="text"
              autoComplete="nickname"
              placeholder="Your name or team"
              value={accountName}
              onChange={(event) => setAccountName(event.target.value)}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="register-email">Email address</label>
            <input
              id="register-email"
              name="email"
              type="email"
              autoComplete="username"
              placeholder="you@company.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="register-password">Password</label>
            <input
              id="register-password"
              name="password"
              type="password"
              autoComplete="new-password"
              placeholder="Create a password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="register-confirm-password">Confirm password</label>
            <input
              id="register-confirm-password"
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              placeholder="Enter your password again"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              disabled={isSubmitting}
              required
            />
          </div>

          {error && <p className="feedback feedback--error" role="alert">{error}</p>}

          <button className="button button--primary auth-submit" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account…' : 'Create account'}
            {!isSubmitting && (
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M5 12h14m-6-6 6 6-6 6" />
              </svg>
            )}
          </button>
        </form>
        <p className="auth-footer">
          Already have an account? <Link to="/">Sign in</Link>
        </p>
      </section>
    </main>
  )
}

export default RegisterPage
