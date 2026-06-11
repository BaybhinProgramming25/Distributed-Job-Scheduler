// Catalog of jobs a user can schedule. Every job is either recurring
// (runs on a cron schedule) or one-time (runs once at a chosen time).
export const JOB_TEMPLATES = [
  {
    type: 'DATABASE_BACKUP',
    name: 'Database Backup',
    description: 'Dump the primary database to durable storage.',
    isRecurring: true,
    defaultSchedule: '0 2 * * *',
  },
  {
    type: 'LOG_CLEANUP',
    name: 'Log Rotation & Cleanup',
    description: 'Archive old logs and free up disk space.',
    isRecurring: true,
    defaultSchedule: '0 0 * * 0',
  },
  {
    type: 'INVENTORY_SYNC',
    name: 'Inventory Sync',
    description: 'Sync stock counts with the warehouse system.',
    isRecurring: true,
    defaultSchedule: '0 * * * *',
  },
  {
    type: 'HEALTH_CHECK',
    name: 'Health Check Ping',
    description: 'Ping critical services and report uptime.',
    isRecurring: true,
    defaultSchedule: '*/5 * * * *',
  },
  {
    type: 'EMAIL_REPORT',
    name: 'Weekly Report Email',
    description: 'Compile and send the weekly metrics digest.',
    isRecurring: true,
    defaultSchedule: '0 9 * * 1',
  },
  {
    type: 'GENERATE_INVOICE',
    name: 'Generate Invoice',
    description: 'Create a PDF invoice for a customer order.',
    isRecurring: false,
  },
  {
    type: 'SEND_NOTIFICATION',
    name: 'Send Welcome Email',
    description: 'Send a one-time welcome email to a new user.',
    isRecurring: false,
  },
  {
    type: 'DATA_EXPORT',
    name: 'Data Export',
    description: 'Export account data to a downloadable archive.',
    isRecurring: false,
  },
  {
    type: 'CACHE_PURGE',
    name: 'Cache Purge',
    description: 'Flush stale entries from the cache layer.',
    isRecurring: false,
  },
]

// Friendly cron presets offered for recurring jobs
export const CRON_PRESETS = [
  { label: 'Every 5 minutes', value: '*/5 * * * *' },
  { label: 'Every hour', value: '0 * * * *' },
  { label: 'Daily at 2:00 AM', value: '0 2 * * *' },
  { label: 'Weekly (Sun midnight)', value: '0 0 * * 0' },
  { label: 'Weekly (Mon 9:00 AM)', value: '0 9 * * 1' },
  { label: 'Custom…', value: 'custom' },
]
