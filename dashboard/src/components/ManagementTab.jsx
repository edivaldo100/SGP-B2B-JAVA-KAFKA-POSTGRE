import { useState, useEffect, useCallback } from 'react'

const ORDER_STATUS_OPTIONS = ['PENDENTE', 'APROVADO', 'EM_PROCESSAMENTO', 'ENVIADO', 'ENTREGUE', 'CANCELADO']

function fmtBRL(val) {
  if (val == null) return '—'
  return Number(val).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function fmtDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
}

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: '2rem' }}>
      <p className="section-title">{title}</p>
      {children}
    </div>
  )
}

// ─── Partners ───────────────────────────────────────────────────────────────

function PartnersSection() {
  const [partners, setPartners] = useState([])
  const [name, setName] = useState('')
  const [creditLimit, setCreditLimit] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const load = useCallback(() => {
    fetch('/api/v1/partners')
      .then(r => r.json())
      .then(setPartners)
      .catch(() => {})
  }, [])

  useEffect(() => { load() }, [load])

  async function handleCreate(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setLoading(true)
    const body = { name: name.trim() }
    if (creditLimit.trim()) body.creditLimit = parseFloat(creditLimit)
    try {
      const r = await fetch('/api/v1/partners', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })
      if (!r.ok) {
        const err = await r.json().catch(() => ({}))
        setError(err.detail || `Erro ${r.status}`)
      } else {
        setSuccess('Parceiro cadastrado com sucesso.')
        setName('')
        setCreditLimit('')
        load()
      }
    } catch {
      setError('Erro de conexão.')
    } finally {
      setLoading(false)
    }
  }

  async function handleDelete(id) {
    if (!window.confirm(`Remover parceiro #${id}?`)) return
    try {
      const r = await fetch(`/api/v1/partners/${id}`, { method: 'DELETE' })
      if (!r.ok && r.status !== 204) {
        const err = await r.json().catch(() => ({}))
        setError(err.detail || `Erro ${r.status}`)
      } else {
        load()
      }
    } catch {
      setError('Erro de conexão.')
    }
  }

  return (
    <Section title="Parceiros">
      <form onSubmit={handleCreate} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
        <input
          placeholder="Nome do parceiro *"
          value={name}
          onChange={e => setName(e.target.value)}
          required
          style={inputStyle}
        />
        <input
          placeholder="Limite de crédito (opcional)"
          type="number"
          min="0.01"
          step="0.01"
          value={creditLimit}
          onChange={e => setCreditLimit(e.target.value)}
          style={{ ...inputStyle, width: '220px' }}
        />
        <button className="btn btn-primary" type="submit" disabled={loading}>
          {loading ? 'Salvando…' : '+ Cadastrar'}
        </button>
      </form>
      {error && <p style={{ color: '#fc8181', fontSize: '0.85rem', marginBottom: '0.75rem' }}>{error}</p>}
      {success && <p style={{ color: '#9ae6b4', fontSize: '0.85rem', marginBottom: '0.75rem' }}>{success}</p>}
      <div className="table-wrapper">
        <div className="scroll-table">
          {partners.length === 0 ? (
            <div className="empty-state">Nenhum parceiro cadastrado</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>UUID</th>
                  <th>Nome</th>
                  <th>Cadastrado em</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {partners.map(p => (
                  <tr key={p.id}>
                    <td style={{ color: '#718096' }}>{p.id}</td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.78rem', color: '#718096' }}>{p.partnerUuid?.slice(0, 8)}…</td>
                    <td style={{ fontWeight: 600 }}>{p.name}</td>
                    <td>{fmtDate(p.createdAt)}</td>
                    <td>
                      <button
                        className="btn btn-secondary"
                        style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem', color: '#fc8181' }}
                        onClick={() => handleDelete(p.id)}
                      >
                        Remover
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </Section>
  )
}

// ─── Orders ─────────────────────────────────────────────────────────────────

function OrdersSection() {
  const [partners, setPartners] = useState([])
  const [orders, setOrders] = useState([])
  const [filterPartner, setFilterPartner] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [loading, setLoading] = useState(false)
  const [statusMap, setStatusMap] = useState({})
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    fetch('/api/v1/partners').then(r => r.json()).then(setPartners).catch(() => {})
  }, [])

  const fetchOrders = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (filterPartner) params.set('partnerId', filterPartner)
    if (filterStatus) params.set('status', filterStatus)
    fetch(`/api/v1/orders${params.toString() ? '?' + params : ''}`)
      .then(r => r.json())
      .then(data => { setOrders(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [filterPartner, filterStatus])

  useEffect(() => { fetchOrders() }, [fetchOrders])

  async function handleUpdateStatus(orderId) {
    const newStatus = statusMap[orderId]
    if (!newStatus) return
    setError('')
    setSuccess('')
    try {
      const r = await fetch(`/api/v1/orders/${orderId}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus }),
      })
      if (!r.ok) {
        const err = await r.json().catch(() => ({}))
        setError(err.detail || `Erro ${r.status}`)
      } else {
        setSuccess(`Status atualizado para ${newStatus}.`)
        fetchOrders()
      }
    } catch {
      setError('Erro de conexão.')
    }
  }

  async function handleCancel(orderId) {
    if (!window.confirm('Cancelar este pedido?')) return
    setError('')
    setSuccess('')
    try {
      const r = await fetch(`/api/v1/orders/${orderId}`, { method: 'DELETE' })
      if (!r.ok) {
        const err = await r.json().catch(() => ({}))
        setError(err.detail || `Erro ${r.status}`)
      } else {
        setSuccess('Pedido cancelado.')
        fetchOrders()
      }
    } catch {
      setError('Erro de conexão.')
    }
  }

  return (
    <Section title="Pedidos">
      <div className="filters" style={{ marginBottom: '0.75rem' }}>
        <select value={filterPartner} onChange={e => setFilterPartner(e.target.value)} style={selectStyle}>
          <option value="">Todos os parceiros</option>
          {partners.map(p => <option key={p.id} value={p.id}>{p.name} (#{p.id})</option>)}
        </select>
        <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)} style={selectStyle}>
          <option value="">Todos os status</option>
          {ORDER_STATUS_OPTIONS.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
        <button className="btn btn-primary" onClick={fetchOrders}>Buscar</button>
        {(filterPartner || filterStatus) && (
          <button className="btn btn-secondary" onClick={() => { setFilterPartner(''); setFilterStatus('') }}>Limpar</button>
        )}
      </div>
      {error && <p style={{ color: '#fc8181', fontSize: '0.85rem', marginBottom: '0.75rem' }}>{error}</p>}
      {success && <p style={{ color: '#9ae6b4', fontSize: '0.85rem', marginBottom: '0.75rem' }}>{success}</p>}
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
                  <th>Pedido ID</th>
                  <th>Parceiro</th>
                  <th>Status atual</th>
                  <th>Total</th>
                  <th>Criado em</th>
                  <th>Alterar status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {orders.map(o => (
                  <tr key={o.id}>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>{o.id?.slice(0, 8)}…</td>
                    <td style={{ color: '#63b3ed', fontWeight: 600 }}>{o.partnerName}</td>
                    <td><span className={`status-badge status-${o.status}`}>{o.status}</span></td>
                    <td>{fmtBRL(o.totalAmount)}</td>
                    <td>{fmtDate(o.createdAt)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                        <select
                          style={{ ...selectStyle, fontSize: '0.78rem', padding: '0.3rem 0.5rem' }}
                          value={statusMap[o.id] || ''}
                          onChange={e => setStatusMap(prev => ({ ...prev, [o.id]: e.target.value }))}
                        >
                          <option value="">Selecionar…</option>
                          {ORDER_STATUS_OPTIONS.map(s => <option key={s} value={s}>{s}</option>)}
                        </select>
                        <button
                          className="btn btn-primary"
                          style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem' }}
                          onClick={() => handleUpdateStatus(o.id)}
                          disabled={!statusMap[o.id]}
                        >
                          Atualizar
                        </button>
                      </div>
                    </td>
                    <td>
                      {o.status !== 'CANCELADO' && (
                        <button
                          className="btn btn-secondary"
                          style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem', color: '#fc8181' }}
                          onClick={() => handleCancel(o.id)}
                        >
                          Cancelar
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </Section>
  )
}

// ─── Styles ──────────────────────────────────────────────────────────────────

const inputStyle = {
  background: '#1a1d2e',
  border: '1px solid #2d3748',
  color: '#e2e8f0',
  padding: '0.5rem 0.75rem',
  borderRadius: '6px',
  fontSize: '0.875rem',
  outline: 'none',
  flex: 1,
  minWidth: '180px',
}

const selectStyle = {
  background: '#1a1d2e',
  border: '1px solid #2d3748',
  color: '#e2e8f0',
  padding: '0.5rem 0.75rem',
  borderRadius: '6px',
  fontSize: '0.875rem',
  outline: 'none',
}

// ─── Tab ─────────────────────────────────────────────────────────────────────

export default function ManagementTab() {
  return (
    <div>
      <PartnersSection />
      <OrdersSection />
    </div>
  )
}
