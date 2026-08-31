import { useState, useEffect, useRef } from 'react'

const MAX_EVENTS = 20

function fmtTime(iso) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function topicLabel(topic) {
  if (!topic) return ''
  return topic.replace('order.events.', '').replace(/\./g, '_').toUpperCase()
}

const STATUS_STYLE = {
  PENDENTE:         { icon: '⏳', color: '#d69e2e' },
  APROVADO:         { icon: '✅', color: '#38a169' },
  CANCELADO:        { icon: '❌', color: '#e53e3e' },
  EM_PROCESSAMENTO: { icon: '⚙️', color: '#3182ce' },
  ENVIADO:          { icon: '🚚', color: '#805ad5' },
  ENTREGUE:         { icon: '📦', color: '#68d391' },
}

export default function EventsTab() {
  const [events, setEvents] = useState([])
  const [connected, setConnected] = useState(false)
  const [total, setTotal] = useState(0)

  useEffect(() => {
    const es = new EventSource('/api/v1/orders/kafka/stream')

    es.onopen = () => setConnected(true)
    es.onerror = () => setConnected(false)

    es.addEventListener('kafka', e => {
      const ev = JSON.parse(e.data)
      setEvents(prev => [ev, ...prev].slice(0, MAX_EVENTS))
      setTotal(prev => prev + 1)
    })

    return () => { es.close(); setConnected(false) }
  }, [])

  function clearEvents() {
    setEvents([])
    setTotal(0)
  }

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <p className="section-title" style={{ marginBottom: 0 }}>Eventos consumidos do Kafka</p>
          <span style={{
            display: 'inline-flex', alignItems: 'center', gap: '0.4rem',
            fontSize: '0.8rem', fontWeight: 600,
            color: connected ? '#38a169' : '#e53e3e',
          }}>
            <span style={{
              width: 8, height: 8, borderRadius: '50%',
              background: connected ? '#38a169' : '#e53e3e',
              boxShadow: connected ? '0 0 0 3px rgba(56,161,105,0.25)' : 'none',
              animation: connected ? 'pulse 2s infinite' : 'none',
              display: 'inline-block',
            }} />
            {connected ? 'Conectado' : 'Desconectado'}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span className="counter">{total} evento(s) recebido(s) · exibindo últimos {MAX_EVENTS}</span>
          {events.length > 0 && (
            <button className="btn btn-secondary" onClick={clearEvents}>Limpar</button>
          )}
        </div>
      </div>

      {events.length === 0 ? (
        <div className="empty-state" style={{ padding: '4rem 0' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.75rem' }}>📨</div>
          <div style={{ fontWeight: 600, marginBottom: '0.5rem' }}>Aguardando eventos do Kafka…</div>
          <div style={{ fontSize: '0.85rem', color: '#718096' }}>
            Crie um pedido ou altere um status — o evento aparecerá aqui após passar pelo Kafka.
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {events.map((ev, i) => {
            const s = STATUS_STYLE[ev.status] || { icon: '•', color: '#718096' }
            return (
              <div key={i} className="event-card" style={{ '--event-color': s.color }}>
                <div className="event-left">
                  <span className="event-icon">{s.icon}</span>
                  <div>
                    <div className="event-status" style={{ color: s.color }}>{ev.status || '—'}</div>
                    <div className="event-partner" style={{ fontFamily: 'monospace', fontSize: '0.72rem', color: '#4a5568' }}>
                      {topicLabel(ev.topic)}
                    </div>
                  </div>
                </div>
                <div className="event-center">
                  <span className="event-id" title={ev.orderId}>order: {ev.orderId?.slice(0, 8)}…</span>
                  <span className="event-id" title={ev.partnerId}>partner: {ev.partnerId?.slice(0, 8)}…</span>
                </div>
                <div className="event-time">{fmtTime(ev.receivedAt)}</div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
