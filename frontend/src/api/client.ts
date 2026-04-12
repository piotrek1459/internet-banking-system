import { mockServer } from './mockServer'

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token')
  const headers = new Headers(options.headers)

  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  return mockServer.request<T>(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body,
  })
}

export const api = {
  get: <T,>(url: string) => request<T>(url),
  post: <T,>(url: string, body: unknown) =>
    request<T>(url, { method: 'POST', body: JSON.stringify(body) }),
}
