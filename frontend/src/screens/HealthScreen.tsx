import { useEffect, useState } from 'react'

interface ServiceCheck {
  name: string
  url: string
}

type Status = 'checking' | 'up' | 'down'

const SERVICES: ServiceCheck[] = [
  { name: 'User Service', url: 'http://localhost:8081/actuator/health' },
  { name: 'Import Service', url: 'http://localhost:8082/actuator/health' },
  { name: 'Ranking Service', url: 'http://localhost:8083/actuator/health' },
]

export default function HealthScreen() {
  const [statuses, setStatuses] = useState<Record<string, Status>>(
    Object.fromEntries(SERVICES.map((s) => [s.name, 'checking'])),
  )

  useEffect(() => {
    SERVICES.forEach((service) => {
      fetch(service.url)
        .then((res) => {
          setStatuses((prev) => ({ ...prev, [service.name]: res.ok ? 'up' : 'down' }))
        })
        .catch(() => {
          setStatuses((prev) => ({ ...prev, [service.name]: 'down' }))
        })
    })
  }, [])

  return (
    <main className="health-page">
      <h2>Service Health</h2>
      <ul className="service-list">
        {SERVICES.map((service) => (
          <li key={service.name} className={`status-${statuses[service.name]}`}>
            <span className="dot" />
            {service.name}
            <span className="status-label">{statuses[service.name]}</span>
          </li>
        ))}
      </ul>
    </main>
  )
}
