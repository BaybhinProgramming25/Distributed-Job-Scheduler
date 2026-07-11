#!/usr/bin/env node
// Load test for the distributed job scheduler.
//
// Usage:
//   node run.js --jobs=200 --concurrency=20 [--label=1-worker]
//
// What it does:
//   1. Fires `jobs` job-creation requests at job_service (POST /job), `concurrency` at a time,
//      each scheduled to run ~5s from now so they land in the queue almost immediately.
//   2. Records per-request latency for the ingestion API itself.
//   3. Polls job_service + RabbitMQ's management API until every created job has a terminal
//      history entry (success/failed) or a timeout is hit.
//   4. Prints ingestion latency percentiles, end-to-end completion latency percentiles,
//      throughput (jobs/min completed), retry count, and queue-depth samples over time.

const JOB_SERVICE = process.env.JOB_SERVICE_URL || 'http://localhost:7000'
const RABBITMQ_API = process.env.RABBITMQ_API_URL || 'http://localhost:15672'
const RABBITMQ_AUTH = { username: 'guest', password: 'guest' }
const QUEUE_NAME = 'job.queue'

function parseArgs() {
  const args = { jobs: 100, concurrency: 10, label: '', pollMs: 2000, timeoutMs: 15 * 60 * 1000, leadSeconds: 90 }
  for (const arg of process.argv.slice(2)) {
    const m = arg.match(/^--([\w-]+)=(.*)$/)
    if (!m) continue
    const [, key, value] = m
    args[key] = /^\d+$/.test(value) ? Number(value) : value
  }
  return args
}

function percentile(sortedValues, p) {
  if (sortedValues.length === 0) return NaN
  const idx = Math.min(sortedValues.length - 1, Math.ceil((p / 100) * sortedValues.length) - 1)
  return sortedValues[Math.max(0, idx)]
}

function summarize(label, valuesMs) {
  const sorted = [...valuesMs].sort((a, b) => a - b)
  const fmt = (ms) => (Number.isNaN(ms) ? 'n/a' : `${(ms / 1000).toFixed(2)}s`)
  console.log(
    `${label}: n=${sorted.length} min=${fmt(sorted[0])} p50=${fmt(percentile(sorted, 50))} ` +
    `p95=${fmt(percentile(sorted, 95))} p99=${fmt(percentile(sorted, 99))} max=${fmt(sorted[sorted.length - 1])}`
  )
}

async function createJob(schedule) {
  const started = performance.now()
  const res = await fetch(`${JOB_SERVICE}/job`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ Name: 'loadtest', Type: 'LOADTEST_JOB', isRecurring: false, Schedule: schedule }),
  })
  const latencyMs = performance.now() - started
  if (!res.ok) throw new Error(`create job failed: ${res.status} ${await res.text()}`)
  return latencyMs
}

// Every job in this run shares the same 1-minute cron slot so we can identify "our" jobs
// by nextRun without needing job_service to return the created id. Cron granularity is
// per-minute and cron-utils' nextExecution() only fires for times strictly after "now" at
// request-processing time, so the target minute must stay in the future for the whole batch
// (not just at script start) - hence a generous lead time rather than a few seconds.
function cronForSecondsFromNow(seconds) {
  const target = new Date(Date.now() + seconds * 1000)
  return `${target.getUTCMinutes()} ${target.getUTCHours()} * * *`
}

async function runBatch(count, concurrency, schedule) {
  const latencies = []
  let inFlight = 0
  let nextIndex = 0
  let failures = 0

  await new Promise((resolve) => {
    const launchNext = () => {
      if (nextIndex >= count && inFlight === 0) return resolve()
      while (inFlight < concurrency && nextIndex < count) {
        nextIndex++
        inFlight++
        createJob(schedule)
          .then((ms) => latencies.push(ms))
          .catch(() => failures++)
          .finally(() => {
            inFlight--
            if (nextIndex >= count && inFlight === 0) resolve()
            else launchNext()
          })
      }
    }
    launchNext()
  })

  return { latencies, failures }
}

async function fetchJobs() {
  const res = await fetch(`${JOB_SERVICE}/job`)
  if (!res.ok) throw new Error(`list jobs failed: ${res.status}`)
  return res.json()
}

async function fetchHistory(id) {
  const res = await fetch(`${JOB_SERVICE}/job/${id}/history`)
  if (!res.ok) throw new Error(`history failed for ${id}: ${res.status}`)
  return res.json()
}

async function fetchQueueDepth() {
  const authHeader = 'Basic ' + Buffer.from(`${RABBITMQ_AUTH.username}:${RABBITMQ_AUTH.password}`).toString('base64')
  const res = await fetch(`${RABBITMQ_API}/api/queues/%2f/${QUEUE_NAME}`, { headers: { Authorization: authHeader } })
  if (!res.ok) return null
  const data = await res.json()
  return { messagesReady: data.messages_ready, unacked: data.messages_unacknowledged, consumers: data.consumers }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function main() {
  const { jobs: jobCount, concurrency, label, pollMs, timeoutMs, leadSeconds } = parseArgs()
  const runLabel = label ? `[${label}] ` : ''

  console.log(`${runLabel}Creating ${jobCount} jobs at concurrency ${concurrency}...`)
  const schedule = cronForSecondsFromNow(leadSeconds)
  const batchStarted = Date.now()
  const { latencies, failures } = await runBatch(jobCount, concurrency, schedule)
  const batchWallMs = Date.now() - batchStarted

  console.log(`${runLabel}Ingestion done in ${(batchWallMs / 1000).toFixed(2)}s (${failures} failures)`)
  summarize(`${runLabel}Ingestion latency`, latencies)
  console.log(`${runLabel}Ingestion throughput: ${(jobCount / (batchWallMs / 1000)).toFixed(2)} jobs/s`)

  // Identify which jobs belong to this run by matching schedule + recent nextRun.
  const allJobs = await fetchJobs()
  const ourJobs = allJobs.filter((j) => j.schedule === schedule)
  console.log(`${runLabel}Tracking ${ourJobs.length} jobs through completion (timeout ${timeoutMs / 1000}s)...`)

  const createdAt = Date.now()
  const pending = new Map(ourJobs.map((j) => [j.id, true]))
  const completionLatencies = []
  const retryCounts = []
  const queueSamples = []
  const deadline = Date.now() + timeoutMs

  while (pending.size > 0 && Date.now() < deadline) {
    const depth = await fetchQueueDepth()
    if (depth) queueSamples.push({ t: Date.now() - createdAt, ...depth })

    // One bulk list call finds which jobs just went terminal; history is only fetched
    // per-job for those (not for every still-pending job every cycle) - keeps this cheap
    // regardless of how many jobs are in flight.
    const snapshot = await fetchJobs()
    const statusById = new Map(snapshot.map((j) => [j.id, j.status]))
    const justTerminal = [...pending.keys()].filter((id) => {
      const status = statusById.get(id)
      return status === 'success' || status === 'failed'
    })

    for (const id of justTerminal) {
      const history = await fetchHistory(id)
      const terminal = history.find((h) => h.jobStatus === 'success' || h.jobStatus === 'failed')
      if (terminal) {
        const finishedMs = new Date(terminal.jobFinished).getTime() - createdAt
        completionLatencies.push(finishedMs)
        retryCounts.push(history.length - 1)
        pending.delete(id)
      }
    }
    if (pending.size > 0) await sleep(pollMs)
  }

  if (pending.size > 0) {
    console.log(`${runLabel}TIMED OUT waiting for ${pending.size} job(s) to finish.`)
  }

  const totalWallMs = Date.now() - createdAt
  console.log(`${runLabel}All tracked jobs settled in ${(totalWallMs / 1000).toFixed(2)}s`)
  summarize(`${runLabel}End-to-end completion latency (create -> finish)`, completionLatencies)
  console.log(
    `${runLabel}Completion throughput: ${(completionLatencies.length / (totalWallMs / 1000 / 60)).toFixed(2)} jobs/min`
  )
  const totalRetries = retryCounts.reduce((a, b) => a + Math.max(0, b), 0)
  console.log(`${runLabel}Jobs with at least one retry: ${retryCounts.filter((r) => r > 0).length}/${retryCounts.length} (total retries: ${totalRetries})`)

  if (queueSamples.length > 0) {
    const peakReady = Math.max(...queueSamples.map((s) => s.messagesReady))
    console.log(`${runLabel}Peak queue backlog (messages_ready): ${peakReady}`)
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
