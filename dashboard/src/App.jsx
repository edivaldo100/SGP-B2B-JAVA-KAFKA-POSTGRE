import { useState } from 'react'
import OrdersTab from './components/OrdersTab'
import GrafanaTab from './components/GrafanaTab'

export default function App() {
  const [activeTab, setActiveTab] = useState('orders')

  return (
    <div className="app">
      <div className="header">
        <h1>SGP · Dashboard</h1>
        <span className="badge live">● LIVE</span>
      </div>
      <div className="tabs">
        <button
          className={`tab ${activeTab === 'orders' ? 'active' : ''}`}
          onClick={() => setActiveTab('orders')}
        >
          Histórico de Pedidos
        </button>
        <button
          className={`tab ${activeTab === 'grafana' ? 'active' : ''}`}
          onClick={() => setActiveTab('grafana')}
        >
          Métricas (Grafana)
        </button>
      </div>
      <div className="content">
        {activeTab === 'orders' && <OrdersTab />}
        {activeTab === 'grafana' && <GrafanaTab />}
      </div>
    </div>
  )
}
