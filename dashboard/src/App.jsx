import { useState } from 'react'
import OrdersTab from './components/OrdersTab'
import GrafanaTab from './components/GrafanaTab'
import EventsTab from './components/EventsTab'
import ManagementTab from './components/ManagementTab'

export default function App() {
  const [activeTab, setActiveTab] = useState('orders')

  const tabs = [
    { key: 'orders', label: 'Histórico de Pedidos' },
    { key: 'events', label: 'Eventos Kafka' },
    { key: 'management', label: 'Gerenciamento' },
    { key: 'grafana', label: 'Métricas (Grafana)' },
  ]

  return (
    <div className="app">
      <div className="header">
        <h1>SGP · Dashboard</h1>
        <span className="badge live">● LIVE</span>
      </div>
      <div className="tabs">
        {tabs.map(t => (
          <button
            key={t.key}
            className={`tab ${activeTab === t.key ? 'active' : ''}`}
            onClick={() => setActiveTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div className="content">
        {activeTab === 'orders' && <OrdersTab />}
        {activeTab === 'events' && <EventsTab />}
        {activeTab === 'management' && <ManagementTab />}
        {activeTab === 'grafana' && <GrafanaTab />}
      </div>
    </div>
  )
}
