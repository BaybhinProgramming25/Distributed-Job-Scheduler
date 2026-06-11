import { useCallback, useEffect, useState } from 'react'
import { JOB_TEMPLATES } from './jobTemplates'
import { fetchJobs, createJob } from './api'
import JobCard from './JobCard'
import ScheduleModal from './ScheduleModal'
import JobList from './JobList'
import './App.css'

const FILTERS = [
  { key: 'all', label: 'All Jobs' },
  { key: 'recurring', label: 'Recurring' },
  { key: 'onetime', label: 'One-Time' },
]

const App = () => {
  const [userId, setUserId] = useState(() => localStorage.getItem('jobScheduler.userId') || 'user-1024')
  const [filter, setFilter] = useState('all')
  const [activeTemplate, setActiveTemplate] = useState(null)
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

  const visibleTemplates = JOB_TEMPLATES.filter((template) => {
    if (filter === 'recurring') return template.isRecurring
    if (filter === 'onetime') return !template.isRecurring
    return true
  })

  const handleSubmit = async (payload) => {
    setSubmitting(true)
    try {
      const created = await createJob(payload)
      setJobs((prev) => [created, ...prev])
      setToast(`"${payload.Name}" scheduled successfully`)
      setActiveTemplate(null)
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
            <p className="subtitle">Pick a job, schedule it, and watch it run across the cluster.</p>
          </div>
        </div>
        <label className="user-field">
          User ID
          <input value={userId} onChange={(event) => setUserId(event.target.value)} placeholder="e.g. user-1024" />
        </label>
      </header>

      {error && <div className="banner banner-error">{error}</div>}

      <main>
        <section className="panel">
          <div className="panel-header">
            <h2>Job Catalog</h2>
            <div className="tabs">
              {FILTERS.map((item) => (
                <button
                  key={item.key}
                  type="button"
                  className={`tab ${filter === item.key ? 'active' : ''}`}
                  onClick={() => setFilter(item.key)}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>
          <div className="card-grid">
            {visibleTemplates.map((template) => (
              <JobCard key={template.type} template={template} onSchedule={setActiveTemplate} />
            ))}
          </div>
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

      {activeTemplate && (
        <ScheduleModal
          template={activeTemplate}
          userId={userId}
          submitting={submitting}
          onClose={() => setActiveTemplate(null)}
          onSubmit={handleSubmit}
        />
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}

export default App
