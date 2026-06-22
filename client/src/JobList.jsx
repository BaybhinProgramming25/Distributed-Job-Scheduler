import { JOB_TEMPLATES } from './jobTemplates'

const iconForType = (type) => JOB_TEMPLATES.find((template) => template.type === type)?.icon ?? '⚙️'

const formatDate = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

const statusClass = (status) => {
  switch ((status ?? '').toLowerCase()) {
    case 'scheduled':
      return 'status-scheduled'
    case 'running':
      return 'status-running'
    case 'completed':
      return 'status-completed'
    case 'failed':
      return 'status-failed'
    default:
      return 'status-default'
  }
}

// Backend Job records may serialize the `isRecurring` boolean as either
// "isRecurring" or "recurring" depending on the Jackson version, so accept both.
const isRecurring = (job) => Boolean(job.isRecurring ?? job.recurring)

const JobList = ({ jobs }) => {
  if (jobs.length === 0) {
    return <p className="empty-state">No jobs yet — click "Send Quick Job" above.</p>
  }

  const sorted = [...jobs].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

  return (
    <div className="job-table">
      <div className="job-table-row job-table-head">
        <span>Job</span>
        <span>Type</span>
        <span>Schedule</span>
        <span>Status</span>
        <span>Created</span>
        <span>Retries</span>
      </div>
      {sorted.map((job) => (
        <div className="job-table-row" key={job.Id ?? `${job.Name}-${job.createdAt}`}>
          <span className="job-table-name">
            <span className="job-table-icon">{iconForType(job.Type)}</span>
            <span>
              <strong>{job.Name}</strong>
              <small>{job.UserId}</small>
            </span>
          </span>
          <span>
            <span className={`badge ${isRecurring(job) ? 'badge-recurring' : 'badge-onetime'}`}>
              {isRecurring(job) ? 'Recurring' : 'One-time'}
            </span>
          </span>
          <span className="schedule-cell">{job.Schedule}</span>
          <span>
            <span className={`status-pill ${statusClass(job.Status)}`}>{job.Status}</span>
          </span>
          <span>{formatDate(job.createdAt)}</span>
          <span>{job.retryCount}/{job.maxRetries}</span>
        </div>
      ))}
    </div>
  )
}

export default JobList
