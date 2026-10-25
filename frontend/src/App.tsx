import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useNavigate} from 'react-router'
import { checkSession } from './api/auth'
import LoginPage from './pages/LoginPage/LoginPage'
import RegisterPage from './pages/RegisterPage'
import TestCasePage from './pages/TestCasePage'
import './App.css'

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean | null>(null)
  const [sessionError, setSessionError] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    const controller = new AbortController()

    checkSession(controller.signal)
      .then(authenticated => {
        if (!controller.signal.aborted) setIsLoggedIn(authenticated)
      })
      .catch(error => {
        if (!controller.signal.aborted) {
          setSessionError(error instanceof Error ? error.message : 'Unable to check your session. Please try again.')
        }
      })

    return () => controller.abort()
  }, [])

  if (sessionError) {
    return (
      <main className="auth-page">
        <div className="auth-panel">
          <p role="alert">{sessionError}</p>
          <button className="button button--primary" onClick={() => window.location.reload()}>
            Try again
          </button>
        </div>
      </main>
    )
  }

  if (isLoggedIn === null) {
    return (
      <main className="auth-page">
        <p role="status">Checking your session...</p>
      </main>
    )
  }

  return (
    <Routes>
      <Route
        path="/"
        element={
          isLoggedIn
            ? <TestCasePage onLogout={() => setIsLoggedIn(false)}/>
            : <LoginPage onLogin={() => setIsLoggedIn(true)} />
        }
      />

      <Route
        path="/register"
        element={
          <RegisterPage
            onRegister={() =>
              navigate('/', {
                replace: true,
                state: { registrationComplete: true },
              })
            }
          />
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
