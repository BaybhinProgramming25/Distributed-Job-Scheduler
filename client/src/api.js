import axios from 'axios'

export const api = axios.create({ withCredentials: true })

export const fetchJobs = () => api.get('/job').then((res) => res.data)
export const createJob = (job) => api.post('/job', job).then((res) => res.data)
