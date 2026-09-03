import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 8_000,
  headers: {
    Accept: 'application/json',
  },
})
