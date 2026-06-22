import { useCallback, useEffect, useState } from 'react'
import { generateRandomMinuteCron } from './jobTemplates'
import { fetchJobs, createJob } from './api'
import JobList from './JobList'
import './App.css'

const App = () => {
  const [userId, setUserId] = useState(() => localStorage.getItem('jobScheduler.userId') || 'user-1024')
  const [jobs, setJobs] = useState([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')

  useEffect(() => {
    localStorage.setItem('jobScheduler.userId', userId)
  }, [userId])

  const loadJobs = useCallback(async () => {
    try {
      const data = await fetchJobs()
      setJobs(data)
      setError('')
    } catch {
      setError('Could not reach the job service at localhost:7000. Is it running?')
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch + polling
    loadJobs()
    const interval = setInterval(loadJobs, 8000)
    return () => clearInterval(interval)
  }, [loadJobs])

  useEffect(() => {
    if (!toast) return
    const timeout = setTimeout(() => setToast(''), 3000)
    return () => clearTimeout(timeout)
  }, [toast])

  const handleSendQuickJob = async () => {
    setSubmitting(true)
    try {
      const schedule = generateRandomMinuteCron()
      const created = await createJob({
        UserId: userId.trim() || 'anonymous',
        Name: 'Quick Job',
        Type: 'QUICK_JOB',
        isRecurring: true,
        Schedule: schedule,
      })
      setJobs((prev) => [created, ...prev])
      setToast(`Quick job scheduled (${schedule})`)
      setError('')
    } catch {
      setError('Failed to schedule job. Is the job service running on localhost:7000?')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand-icon">⚡</span>
          <div>
            <h1>Distributed Job Scheduler</h1>
            <p className="subtitle">Send a quick job and watch it run across the cluster.</p>
          </div>
        </div>
        <label className="user-field">
          User ID
          <input value={userId} onChange={(event) => setUserId(event.target.value)} placeholder="e.g. user-1024" />
        </label>
      </header>

      {error && <div className="banner banner-error">{error}</div>}

      <main>
        <section className="panel quick-send-panel">
          <h2>Send a Quick Job</h2>
          <p>Click the button to schedule a quick job with a randomly generated interval (every 1-30 minutes).</p>
          <button type="button" className="btn btn-primary" onClick={handleSendQuickJob} disabled={submitting}>
            {submitting ? 'Sending…' : 'Send Quick Job'}
          </button>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2>Scheduled Jobs</h2>
            <button type="button" className="btn btn-ghost" onClick={loadJobs}>
              ↻ Refresh
            </button>
          </div>
          <JobList jobs={jobs} />
        </section>
      </main>

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}

export default App
