// Hard cap: random schedules generated for the quick-send button never
// exceed a 30 minute interval, and never go coarser than minutes (no
// hourly/monthly/yearly schedules).
const MAX_RANDOM_INTERVAL_MINUTES = 3

// Generates a cron string of the form "*/N * * * *", where N is a random
// integer between 1 and MAX_RANDOM_INTERVAL_MINUTES (inclusive).
export const generateRandomMinuteCron = () => {
  const minutes = Math.floor(Math.random() * MAX_RANDOM_INTERVAL_MINUTES) + 1
  return `*/${minutes} * * * *`
}
