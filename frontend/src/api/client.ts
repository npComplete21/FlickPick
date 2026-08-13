export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request(url: string, init?: RequestInit): Promise<Response> {
  const res = await fetch(url, init)
  if (!res.ok) {
    throw new ApiError(res.status, `${init?.method ?? 'GET'} ${url} failed with ${res.status}`)
  }
  return res
}

export function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  return request(url, init).then((res) => res.json() as Promise<T>)
}

export function requestVoid(url: string, init?: RequestInit): Promise<void> {
  return request(url, init).then(() => undefined)
}

export function authHeaders(token: string): HeadersInit {
  return { Authorization: `Bearer ${token}` }
}
