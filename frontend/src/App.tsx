import { useState } from 'react'
import './App.css'
import LoginScreen from './screens/LoginScreen'
import CompareScreen from './screens/CompareScreen'
import RankingsScreen from './screens/RankingsScreen'
import ImportScreen from './screens/ImportScreen'
import HealthScreen from './screens/HealthScreen'

const TOKEN_STORAGE_KEY = 'flickpick_token'

type View = 'compare' | 'rankings' | 'import' | 'health'

function App() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_STORAGE_KEY),
  )
  const [view, setView] = useState<View>('compare')

  const handleAuthenticated = (newToken: string) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, newToken)
    setToken(newToken)
    setView('compare')
  }

  const handleLogout = () => {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
  }

  if (!token) {
    return <LoginScreen onAuthenticated={handleAuthenticated} />
  }

  return (
    <div className="app-shell">
      <nav className="app-nav">
        <span className="app-nav-brand">FlickPick</span>
        <button className={view === 'compare' ? 'active' : ''} onClick={() => setView('compare')}>
          Compare
        </button>
        <button className={view === 'rankings' ? 'active' : ''} onClick={() => setView('rankings')}>
          Rankings
        </button>
        <button className={view === 'import' ? 'active' : ''} onClick={() => setView('import')}>
          Import
        </button>
        <button className={view === 'health' ? 'active' : ''} onClick={() => setView('health')}>
          Health
        </button>
        <button className="logout" onClick={handleLogout}>
          Log out
        </button>
      </nav>
      {view === 'compare' && <CompareScreen token={token} onUnauthorized={handleLogout} />}
      {view === 'rankings' && <RankingsScreen token={token} onUnauthorized={handleLogout} />}
      {view === 'import' && <ImportScreen token={token} onUnauthorized={handleLogout} />}
      {view === 'health' && <HealthScreen />}
    </div>
  )
}

export default App
