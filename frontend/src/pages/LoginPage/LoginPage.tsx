import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { Link, useLocation } from 'react-router'
import { login } from '../../api/auth'
import styles from './LoginPage.module.css'

interface LoginPageProps {
  onLogin: () => void
}

function LoginPage({ onLogin }: LoginPageProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const location = useLocation()
  const registrationComplete =
    location.state?.registrationComplete === true

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
    <main className={styles.page}>
      <section className={styles.panel} aria-labelledby="login-heading">
        <header className={styles.introduction}>
          <h1 id="login-heading">Welcome back.</h1>
          <p>Good to see you. Let’s get you signed in.</p>
        </header>

        {registrationComplete && (
          <p className={styles.success} role="status">
            Account created successfully. Sign in with your email and password.
          </p>
        )}

        <form className={styles.form} onSubmit={handleSubmit} aria-busy={isSubmitting} noValidate>
          <div className={styles.field}>
            <label htmlFor="login-email">Email address</label>
            <input
              id="login-email"
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

          <div className={styles.field}>
            <label htmlFor="login-password">Password</label>
            <div className={styles.passwordField}>
              <input
                id="login-password"
                name="password"
                type={isPasswordVisible ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••••"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                disabled={isSubmitting}
                required
              />
              <button
                className={styles.passwordToggle}
                type="button"
                aria-label={isPasswordVisible ? 'Hide password' : 'Show password'}
                aria-controls="login-password"
                aria-pressed={isPasswordVisible}
                onClick={() => setIsPasswordVisible((visible) => !visible)}
                disabled={isSubmitting}
              >
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
                  <circle cx="12" cy="12" r="3" />
                  {isPasswordVisible && <path d="m3 3 18 18" />}
                </svg>
              </button>
            </div>
          </div>

          <div className={styles.options} aria-describedby="login-options-note">
            <label className={styles.remember} title="Remember me is not available yet.">
              <input type="checkbox" name="remember" disabled />
              <span>Remember for 30 days</span>
            </label>
            <span title="Password reset is not available yet.">
              <button className={styles.forgotPassword} type="button" disabled>
                Forgot password?
              </button>
            </span>
          </div>
          <p id="login-options-note" className={styles.visuallyHidden}>
            Remember me and password reset are not available yet.
          </p>

          {error && <p className={styles.error} role="alert">{error}</p>}

          <button className={styles.submit} type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in…' : 'Sign in'}
            {!isSubmitting && (
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M5 12h14m-6-6 6 6-6 6" />
              </svg>
            )}
          </button>
        </form>

        <p className={styles.createAccount}>
          New here? <Link to="/register">Create an account</Link>
        </p>
      </section>
    </main>
  )
}

export default LoginPage
