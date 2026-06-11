import { useState } from 'react'
import { CRON_PRESETS } from './jobTemplates'

const toLocalDateTimeValue = (date) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const ScheduleModal = ({ template, userId, submitting, onClose, onSubmit }) => {
  const [name, setName] = useState(template.name)
  const [cronPreset, setCronPreset] = useState(template.defaultSchedule ?? CRON_PRESETS[0].value)
  const [customCron, setCustomCron] = useState('')
  const [runAt, setRunAt] = useState(() => toLocalDateTimeValue(new Date(Date.now() + 60 * 60 * 1000)))

  const handleSubmit = (event) => {
    event.preventDefault()

    const schedule = template.isRecurring
      ? (cronPreset === 'custom' ? customCron.trim() : cronPreset)
      : new Date(runAt).toISOString()

    onSubmit({
      UserId: userId.trim() || 'anonymous',
      Name: name.trim(),
      Type: template.type,
      isRecurring: template.isRecurring,
      Schedule: schedule,
    })
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(event) => event.stopPropagation()}>
        <button type="button" className="modal-close" onClick={onClose} aria-label="Close">
          ×
        </button>
        <div className="modal-icon">{template.icon}</div>
        <h2>{template.name}</h2>
        <p className="modal-desc">{template.description}</p>

        <div className="badge-row">
          <span className="badge">{template.type}</span>
          <span className={`badge ${template.isRecurring ? 'badge-recurring' : 'badge-onetime'}`}>
            {template.isRecurring ? 'Recurring' : 'One-time'}
          </span>
        </div>

        <form onSubmit={handleSubmit}>
          <label>
            Job Name
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>

          {template.isRecurring ? (
            <>
              <label>
                Frequency
                <select value={cronPreset} onChange={(event) => setCronPreset(event.target.value)}>
                  {CRON_PRESETS.map((preset) => (
                    <option key={preset.value} value={preset.value}>
                      {preset.label}
                    </option>
                  ))}
                </select>
              </label>
              {cronPreset === 'custom' && (
                <label>
                  Cron Expression
                  <input
                    value={customCron}
                    onChange={(event) => setCustomCron(event.target.value)}
                    placeholder="*/15 * * * *"
                    required
                  />
                </label>
              )}
            </>
          ) : (
            <label>
              Run At
              <input
                type="datetime-local"
                value={runAt}
                onChange={(event) => setRunAt(event.target.value)}
                required
              />
            </label>
          )}

          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Scheduling…' : 'Schedule Job'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default ScheduleModal
