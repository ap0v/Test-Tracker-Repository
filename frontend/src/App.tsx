import { useState } from 'react'
import { Navigate, Route, Routes, useNavigate} from 'react-router'
import LoginPage from './pages/LoginPage/LoginPage'
import RegisterPage from './pages/RegisterPage'
import TestCasePage from './pages/TestCasePage'
import './App.css'

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const navigate = useNavigate()

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
