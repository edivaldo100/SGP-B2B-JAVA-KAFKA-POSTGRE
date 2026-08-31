import { useState, useEffect, useRef } from 'react'

function fmtTime(iso) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function fmtBRL(val) {
  return Number(val).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

const STATUS_ICON = {
  PENDENTE:         { icon: '⏳', color: '#d69e2e' },
  APROVADO:         { icon: '✅', color: '#38a169' },
  CANCELADO:        { icon: '❌', color: '#e53e3e' },
  EM_PROCESSAMENTO: { icon: '⚙️', color: '#3182ce' },
  ENVIADO:          { icon: '🚚', color: '#805ad5' },
  ENTREGUE:         { icon: '📦', color: '#2d3748' },
}

export default function EventsTab() {
  const [events, setEvents] = useState([])
  const [connected, setConnected] = useState(false)
  const [total, setTotal] = useState(0)
  const listRef = useRef(null)
  const autoScrollRef = useRef(true)

  useEffect(() => {
    const es = new EventSource('/api/v1/orders/stream')

    es.onopen = () => setConnected(true)
    es.onerror = () => setConnected(false)

    es.addEventListener('order', e => {
      const order = JSON.parse(e.data)
      const evento = {
        key: `${order.id}-${Date.now()}`,
        receivedAt: new Date().toISOString(),
        order,
      }
      setEvents(prev => [evento, ...prev].slice(0, 200))
      setTotal(prev => prev + 1)
    })

    return () => {
      es.close()
      setConnected(false)
    }
  }, [])

  // Auto-scroll para o topo quando chega novo evento
  useEffect(() => {
    if (autoScrollRef.current && listRef.current) {
      listRef.current.scrollTop = 0
    }
  }, [events])

  function clearEvents() {
    setEvents([])
    setTotal(0)
  }

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <p className="section-title" style={{ marginBottom: 0 }}>Eventos SSE em tempo real</p>
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
          <span className="counter">{total} evento(s) recebido(s)</span>
          {events.length > 0 && (
            <button className="btn btn-secondary" onClick={clearEvents}>Limpar</button>
          )}
        </div>
      </div>

      {events.length === 0 ? (
        <div className="empty-state" style={{ padding: '4rem 0' }}>
          <div style={{ fontSize: '2rem', marginBottom: '0.75rem' }}>📡</div>
          <div style={{ fontWeight: 600, marginBottom: '0.5rem' }}>Aguardando eventos…</div>
          <div style={{ fontSize: '0.85rem', color: '#718096' }}>
            Crie um pedido ou altere um status para ver os eventos chegarem aqui.
          </div>
        </div>
      ) : (
        <div ref={listRef} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: 'calc(100vh - 240px)', overflowY: 'auto', paddingRight: '0.25rem' }}>
          {events.map(ev => {
            const s = STATUS_ICON[ev.order.status] || { icon: '•', color: '#718096' }
            return (
              <div key={ev.key} className="event-card" style={{ '--event-color': s.color }}>
                <div className="event-left">
                  <span className="event-icon">{s.icon}</span>
                  <div>
                    <div className="event-status" style={{ color: s.color }}>{ev.order.status}</div>
                    <div className="event-partner">{ev.order.partnerName || '—'}</div>
                  </div>
                </div>
                <div className="event-center">
                  <span className="event-id" title={ev.order.id}>{ev.order.id?.slice(0, 8)}…</span>
                  <span className="event-amount">{fmtBRL(ev.order.totalAmount)}</span>
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
