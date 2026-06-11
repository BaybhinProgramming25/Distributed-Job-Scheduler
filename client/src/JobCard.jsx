const JobCard = ({ template, onSchedule }) => {
  return (
    <div className="job-card">
      <div className="job-card-icon">{template.icon}</div>
      <div className="job-card-body">
        <div className="job-card-header">
          <h3>{template.name}</h3>
          <span className={`badge ${template.isRecurring ? 'badge-recurring' : 'badge-onetime'}`}>
            {template.isRecurring ? 'Recurring' : 'One-time'}
          </span>
        </div>
        <p>{template.description}</p>
      </div>
      <button type="button" className="btn btn-primary" onClick={() => onSchedule(template)}>
        Schedule
      </button>
    </div>
  )
}

export default JobCard
