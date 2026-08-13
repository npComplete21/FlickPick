import { requestJson } from './client'
import { USER_SERVICE_URL } from './config'

interface AuthResponse {
  token: string
}

function jsonPost(path: string, body: unknown): Promise<AuthResponse> {
  return requestJson<AuthResponse>(`${USER_SERVICE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export function signup(email: string, password: string): Promise<string> {
  return jsonPost('/api/auth/signup', { email, password }).then((res) => res.token)
}

export function login(email: string, password: string): Promise<string> {
  return jsonPost('/api/auth/login', { email, password }).then((res) => res.token)
}
