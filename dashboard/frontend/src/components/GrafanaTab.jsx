export default function GrafanaTab() {
  return (
    <iframe
      className="grafana-frame"
      src="/grafana/d/ffwswyewfdse8b/k6-load-testing-results?orgId=1&refresh=5s&kiosk=tv"
      title="Grafana K6 Metrics"
    />
  )
}
