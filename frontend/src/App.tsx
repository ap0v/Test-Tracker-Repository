import { useState } from 'react'
import LoginPage from './pages/LoginPage'
import TestCasePage from './pages/TestCasePage'
import './App.css'

function App() {
  // TODO: Restore the session via /api/auth/me on page load.
  const [isLoggedIn, setIsLoggedIn] = useState(false)

  return isLoggedIn
    ? <TestCasePage />
    : <LoginPage onLogin={() => setIsLoggedIn(true)} />
}

export default App
