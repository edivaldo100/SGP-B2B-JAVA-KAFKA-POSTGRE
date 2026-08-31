import { useState, useEffect, useRef, useCallback } from 'react'

const STATUS_OPTIONS = ['', 'PENDENTE', 'APROVADO', 'CANCELADO', 'PROCESSANDO']

function fmt(val) {
  if (val == null) return '—'
  return val
}

function fmtDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
}

function fmtBRL(val) {
  if (val == null) return '—'
  return Number(val).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export default function OrdersTab() {
  const [partners, setPartners] = useState([])
  const [orders, setOrders] = useState([])
  const [newOrderIds, setNewOrderIds] = useState(new Set())
  const [selectedPartner, setSelectedPartner] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [nameFilter, setNameFilter] = useState('')
  const [loading, setLoading] = useState(false)
  const sseRef = useRef(null)

  // Carrega parceiros na montagem
  useEffect(() => {
    fetch('/api/v1/partners')
      .then(r => r.json())
      .then(setPartners)
      .catch(console.error)
  }, [])

  const fetchOrders = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (selectedPartner) params.set('partnerId', selectedPartner)
    if (nameFilter.trim()) params.set('name', nameFilter.trim())
    if (statusFilter) params.set('status', statusFilter)
    const qs = params.toString()
    fetch(`/api/v1/orders${qs ? '?' + qs : ''}`)
      .then(r => r.json())
      .then(data => { setOrders(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [selectedPartner, nameFilter, statusFilter])

  // Busca inicial e ao mudar filtros
  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

  // SSE para tempo real
  useEffect(() => {
    const es = new EventSource('/api/v1/orders/stream')
    sseRef.current = es

    es.addEventListener('order', e => {
      const order = JSON.parse(e.data)
      setOrders(prev => {
        // Atualiza se já existe, senão adiciona no topo
        const exists = prev.find(o => o.id === order.id)
        if (exists) return prev.map(o => o.id === order.id ? order : o)
        setNewOrderIds(ids => new Set([...ids, order.id]))
        setTimeout(() => setNewOrderIds(ids => { const n = new Set(ids); n.delete(order.id); return n }), 2000)
        return [order, ...prev]
      })
    })

    return () => es.close()
  }, [])

  function handlePartnerClick(partner) {
    setSelectedPartner(prev => prev === partner.id ? null : partner.id)
    setNameFilter('')
  }

  return (
    <div>
      <p className="section-title">Parceiros</p>
      <div className="partners-grid">
        {partners.map(p => (
          <div
            key={p.id}
            className={`partner-card ${selectedPartner === p.id ? 'selected' : ''}`}
            onClick={() => handlePartnerClick(p)}
          >
            <div className="partner-id">#{p.id} · {p.partnerUuid?.slice(0, 8)}…</div>
            <div className="partner-name">{p.name}</div>
          </div>
        ))}
      </div>

      <div className="flex-between">
        <p className="section-title" style={{ marginBottom: 0 }}>Pedidos</p>
        <span className="counter">{orders.length} registro(s)</span>
      </div>

      <div className="filters">
        <input
          placeholder="Filtrar por nome do parceiro"
          value={nameFilter}
          onChange={e => { setNameFilter(e.target.value); setSelectedPartner(null) }}
        />
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
          {STATUS_OPTIONS.map(s => (
            <option key={s} value={s}>{s || 'Todos os status'}</option>
          ))}
        </select>
        <button className="btn btn-primary" onClick={fetchOrders}>Buscar</button>
        {(selectedPartner || statusFilter || nameFilter) && (
          <button className="btn btn-secondary" onClick={() => {
            setSelectedPartner(null); setStatusFilter(''); setNameFilter('')
          }}>Limpar</button>
        )}
      </div>

      <div className="table-wrapper">
        <div className="scroll-table">
          {loading ? (
            <div className="empty-state">Carregando…</div>
          ) : orders.length === 0 ? (
            <div className="empty-state">Nenhum pedido encontrado</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Pedido ID</th>
                  <th>Parceiro</th>
                  <th>Status</th>
                  <th>Total</th>
                  <th>Criado em</th>
                  <th>Atualizado em</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o, i) => (
                  <tr key={o.id} className={newOrderIds.has(o.id) ? 'new-row' : ''}>
                    <td style={{ color: '#718096' }}>{i + 1}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{o.id?.slice(0, 8)}…</td>
                    <td>
                      <span style={{ color: '#63b3ed', fontWeight: 600 }}>
                        {fmt(o.partnerName)}
                      </span>
                      {o.partnerSequentialId && (
                        <span style={{ color: '#718096', fontSize: '0.75rem', marginLeft: '0.4rem' }}>
                          #{o.partnerSequentialId}
                        </span>
                      )}
                    </td>
                    <td>
                      <span className={`status-badge status-${o.status}`}>{o.status}</span>
                    </td>
                    <td>{fmtBRL(o.totalAmount)}</td>
                    <td>{fmtDate(o.createdAt)}</td>
                    <td>{fmtDate(o.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
