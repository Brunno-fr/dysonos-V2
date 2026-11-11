import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import { Activity, AlertTriangle, ArrowRight, ArrowUpRight, Boxes, BrainCircuit, ChartNoAxesCombined, Check, ChevronDown, CircleHelp, Cpu, Database, Expand, Gauge, Globe2, Layers3, LockKeyhole, Menu, Network as NetworkIcon, Plus, RefreshCw, Search, Server, Settings2, Shield, SlidersHorizontal, Sparkles, Trash2, X, Zap } from 'lucide-react'
import { api, type Network, type NetworkMetrics, type Node, type Plan, type Workload, type Zone, type ZoneMetrics } from './api'
const Globe = lazy(() => import('./Globe'))
const MetricsChart = lazy(() => import('./MetricsChart'))

type Page = 'dashboard' | 'infra' | 'simulation' | 'expansion' | 'reports' | 'security' | 'settings'
type Sample = { time: string; cpu: number; memory: number }
type EventItem = { id: number; title: string; detail: string; at: string; tone: 'cyan' | 'purple' | 'amber' }
const nav: { id: Page; label: string; icon: typeof Activity }[] = [
  { id: 'dashboard', label: 'Dashboard', icon: Gauge }, { id: 'infra', label: 'Infraestrutura', icon: Boxes },
  { id: 'simulation', label: 'Simulação', icon: BrainCircuit }, { id: 'expansion', label: 'Expansão', icon: Zap },
  { id: 'reports', label: 'Relatórios', icon: ChartNoAxesCombined }, { id: 'security', label: 'Segurança', icon: Shield },
]

function fmt(value: number) { return `${Number(value || 0).toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%` }
function timeLabel() { return new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) }

export default function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const [mobileNav, setMobileNav] = useState(false)
  const [networks, setNetworks] = useState<Network[]>([])
  const [zones, setZones] = useState<Zone[]>([])
  const [nodes, setNodes] = useState<Node[]>([])
  const [networkMetrics, setNetworkMetrics] = useState<NetworkMetrics[]>([])
  const [zoneMetrics, setZoneMetrics] = useState<ZoneMetrics[]>([])
  const [workloads, setWorkloads] = useState<Workload[]>([])
  const [samples, setSamples] = useState<Sample[]>([])
  const [events, setEvents] = useState<EventItem[]>([])
  const [selectedZoneId, setSelectedZoneId] = useState('')
  const [health, setHealth] = useState<'online' | 'offline' | 'loading'>('loading')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [modal, setModal] = useState<'node' | 'workload' | null>(null)
  const [menu, setMenu] = useState(false)
  const [query, setQuery] = useState('')
  const [pollSeconds, setPollSeconds] = useState(8)
  const [reducedMotion, setReducedMotion] = useState(false)
  const [parallax, setParallax] = useState({ x: 0, y: 0, scroll: 0 })
  const [nodeForm, setNodeForm] = useState({ name: '', cpuUsage: 8, memoryUsage: 14, zoneId: '' })
  const [workloadForm, setWorkloadForm] = useState({ name: '', cpuDemand: 12, memoryDemand: 10 })
  const [plan, setPlan] = useState<Plan | null>(null)
  const [activeTab, setActiveTab] = useState<'nodes' | 'zones' | 'workloads'>('nodes')

  const selectedZone = zones.find(zone => zone.id === selectedZoneId) ?? zones[0]
  const cpuAverage = nodes.length ? nodes.reduce((sum, node) => sum + node.cpuUsage, 0) / nodes.length : 0
  const memoryAverage = nodes.length ? nodes.reduce((sum, node) => sum + node.memoryUsage, 0) / nodes.length : 0
  const onlineNodes = nodes.filter(node => node.status === 'ONLINE').length
  const overloadedNodes = nodes.filter(node => node.cpuUsage >= 80 || node.memoryUsage >= 80).length
  const filteredNodes = nodes.filter(node => `${node.name} ${zones.find(zone => zone.id === node.zoneId)?.name ?? ''}`.toLowerCase().includes(query.toLowerCase()))
  const activeWorkloads = workloads.filter(workload => workload.status === 'ACTIVE')

  const addEvent = useCallback((title: string, detail: string, tone: EventItem['tone'] = 'cyan') => {
    setEvents(previous => [{ id: Date.now() + Math.random(), title, detail, at: timeLabel(), tone }, ...previous].slice(0, 12))
  }, [])

  const refresh = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    setError('')
    try {
      const [healthResult, networkResult, zoneResult, nodeResult] = await Promise.all([
        api.health(), api.networks(), api.zones(), api.nodes(),
      ])
      setHealth(healthResult.status === 'UP' ? 'online' : 'offline')
      setNetworks(networkResult); setZones(zoneResult); setNodes(nodeResult)
      const [networkStats, zoneStats] = await Promise.all([
        Promise.all(networkResult.map(network => api.networkMetrics(network.id))),
        Promise.all(zoneResult.map(zone => api.zoneMetrics(zone.id))),
      ])
      setNetworkMetrics(networkStats); setZoneMetrics(zoneStats)
      const nextZone = zoneResult.find(zone => zone.id === selectedZoneId) ?? zoneResult[0]
      if (nextZone) {
        setSelectedZoneId(nextZone.id)
        setWorkloads(await api.workloads(nextZone.id))
      } else setWorkloads([])
      if (nodeResult.length) {
        const cpu = nodeResult.reduce((sum, node) => sum + node.cpuUsage, 0) / nodeResult.length
        const memory = nodeResult.reduce((sum, node) => sum + node.memoryUsage, 0) / nodeResult.length
        setSamples(previous => [...previous, { time: timeLabel(), cpu, memory }].slice(-18))
      }
    } catch (cause) {
      setHealth('offline')
      setError(cause instanceof Error ? cause.message : 'Falha ao comunicar com a API.')
    } finally { setLoading(false) }
  }, [selectedZoneId])

  useEffect(() => { void refresh() }, []) // initial API load
  useEffect(() => {
    const timer = window.setInterval(() => void refresh(true), pollSeconds * 1000)
    return () => window.clearInterval(timer)
  }, [refresh, pollSeconds])
  useEffect(() => {
    const media = window.matchMedia('(prefers-reduced-motion: reduce)')
    const update = () => setReducedMotion(media.matches)
    update(); media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])
  useEffect(() => {
    if (reducedMotion) return
    const onMove = (event: MouseEvent) => setParallax(p => ({ ...p, x: (event.clientX / window.innerWidth - 0.5) * 11, y: (event.clientY / window.innerHeight - 0.5) * 9 }))
    const onScroll = () => setParallax(p => ({ ...p, scroll: Math.min(window.scrollY * 0.035, 14) }))
    window.addEventListener('mousemove', onMove); window.addEventListener('scroll', onScroll, { passive: true })
    return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('scroll', onScroll) }
  }, [reducedMotion])

  async function createNode(event: React.FormEvent) {
    event.preventDefault(); setBusy(true)
    try {
      const created = await api.createNode({ ...nodeForm, zoneId: nodeForm.zoneId || null })
      addEvent('Nó adicionado', `${created.name} • ${created.status}`)
      setModal(null); setNodeForm({ name: '', cpuUsage: 8, memoryUsage: 14, zoneId: '' }); await refresh(true)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível criar o nó.') }
    finally { setBusy(false) }
  }
  async function removeNode(node: Node) {
    if (!window.confirm(`Remover o nó “${node.name}”?`)) return
    setBusy(true)
    try { await api.deleteNode(node.id); addEvent('Nó removido', node.name, 'amber'); await refresh(true) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível remover o nó.') }
    finally { setBusy(false) }
  }
  async function runPlan(event: React.FormEvent) {
    event.preventDefault(); if (!selectedZone) return
    setBusy(true)
    try {
      const result = await api.plan(selectedZone.id, { cpuDemand: workloadForm.cpuDemand, memoryDemand: workloadForm.memoryDemand })
      setPlan(result); addEvent(result.feasible ? 'Plano calculado' : 'Capacidade insuficiente', `${result.requestedCpu} CPU / ${result.requestedMemory} memória`, result.feasible ? 'purple' : 'amber')
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Falha ao calcular o plano.') }
    finally { setBusy(false) }
  }
  async function createWorkload(event: React.FormEvent) {
    event.preventDefault(); if (!selectedZone) return
    setBusy(true)
    try {
      const created = await api.createWorkload(selectedZone.id, workloadForm)
      setPlan(null); setModal(null); addEvent('Demanda alocada', `${created.name} • ${created.allocations.length} alocação(ões)`, 'purple'); await refresh(true)
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível criar a demanda.') }
    finally { setBusy(false) }
  }
  async function stopWorkload(workload: Workload) {
    setBusy(true)
    try { const stopped = await api.stopWorkload(workload.id); addEvent('Demanda encerrada', stopped.name, 'amber'); await refresh(true) }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Não foi possível encerrar a demanda.') }
    finally { setBusy(false) }
  }
  function choosePage(id: Page) { setPage(id); setMobileNav(false); setMenu(false) }

  const title = nav.find(item => item.id === page)?.label ?? 'Configurações'
  const pageStyle = { '--px': `${parallax.x}px`, '--py': `${parallax.y - parallax.scroll}px` } as React.CSSProperties
  const topNetwork = networkMetrics[0]

  return <div className="app-shell" style={pageStyle}>
    <div className="ambient ambient-one" /><div className="ambient ambient-two" />
    <aside className={`sidebar ${mobileNav ? 'sidebar-open' : ''}`} aria-label="Navegação principal">
      <div className="brand-lockup">
        <div className="brand-orb"><Globe2 size={38} strokeWidth={1.3} /><span /></div>
        <div className="brand-name">DYSONOS <b>V2</b></div>
        <small>Autonomous Network Infrastructure</small>
      </div>
      <nav className="nav-list">{nav.map(item => <button className={`nav-link ${page === item.id ? 'active' : ''}`} key={item.id} onClick={() => choosePage(item.id)} aria-current={page === item.id ? 'page' : undefined}>
        <item.icon size={18} /><span>{item.label}</span>{item.id === 'infra' && overloadedNodes > 0 && <i className="nav-badge">{overloadedNodes}</i>}
      </button>)}</nav>
      <button className={`nav-link settings-link ${page === 'settings' ? 'active' : ''}`} onClick={() => choosePage('settings')}><Settings2 size={18} /><span>Configurações</span></button>
      <div className="sidebar-spacer" />
      <div className="system-status"><span className={`status-dot ${health}`} /><div><b>Sistema {health === 'online' ? 'Online' : health === 'loading' ? 'Conectando' : 'Offline'}</b><small>{health === 'online' ? 'API Spring Boot conectada' : health === 'loading' ? 'Verificando serviço' : 'API indisponível'}</small></div></div>
      <div className="side-footer"><span>DYSONOS V2</span><span>•</span><span>MONITOR</span></div>
    </aside>
    {mobileNav && <button className="scrim" aria-label="Fechar menu" onClick={() => setMobileNav(false)} />}
    <main className="main-area">
      <header className="topbar">
        <button className="mobile-menu icon-button" aria-label="Abrir navegação" onClick={() => setMobileNav(true)}><Menu size={19} /></button>
        <div className="breadcrumb"><span>DYSONOS</span><b>/</b><strong>{title}</strong></div>
        <label className="search-box"><Search size={16} /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Buscar nós, zonas ou recursos…" aria-label="Buscar nós e zonas" /><kbd>⌘ K</kbd></label>
        <div className="top-actions">
          <button className="icon-button refresh-button" onClick={() => void refresh()} aria-label="Atualizar dados" title="Atualizar dados"><RefreshCw size={16} className={loading ? 'spin' : ''} /></button>
          <span className={`live-pill ${health}`}><i />{health === 'online' ? 'AO VIVO' : health === 'loading' ? 'CONECTANDO' : 'SEM CONEXÃO'}</span>
          <button className="icon-button" onClick={() => document.documentElement.requestFullscreen?.()} aria-label="Alternar tela cheia"><Expand size={16} /></button>
          <div className="profile-wrap"><button className="profile-button" onClick={() => setMenu(!menu)} aria-expanded={menu} aria-label="Abrir opções"><span>DV</span><ChevronDown size={14} /></button>{menu && <div className="profile-menu"><b>DYSONOS V2</b><button onClick={() => choosePage('settings')}>Preferências</button><button onClick={() => void refresh()}>Atualizar conexão</button></div>}</div>
        </div>
      </header>
      <div className="page-content">
        {error && <div role="alert" className="error-banner"><AlertTriangle size={17} /><div><b>Não foi possível atualizar os dados</b><span>{error}</span></div><button className="icon-button" onClick={() => setError('')} aria-label="Fechar aviso"><X size={16} /></button></div>}
        {page === 'dashboard' && <>
          <section className="welcome-row"><div><div className="eyebrow"><span className="eyebrow-line" /> CENTRO DE OPERAÇÕES DISTRIBUÍDAS</div><h1>Infraestrutura <em>em órbita.</em></h1><p>Visão operacional da sua rede, atualizada diretamente pela API DYSONOS.</p></div><button className="primary-button" onClick={() => setModal('node')}><Plus size={16} /> Adicionar nó</button></section>
          <section className="kpi-grid" aria-label="Indicadores da infraestrutura">
            <Kpi icon={NetworkIcon} label="Redes" value={loading ? '—' : `${networks.length}`} sub={topNetwork?.name ?? 'Dados da API'} color="cyan" />
            <Kpi icon={Layers3} label="Zonas ativas" value={loading ? '—' : `${zones.length}`} sub={`${zoneMetrics.reduce((s, z) => s + z.nodeCount, 0)} nós distribuídos`} color="purple" />
            <Kpi icon={Server} label="Nós online" value={loading ? '—' : `${onlineNodes} / ${nodes.length}`} sub={`${overloadedNodes} acima de 80% em CPU ou memória`} color={overloadedNodes ? 'amber' : 'cyan'} />
            <Kpi icon={Gauge} label="Uso médio" value={nodes.length ? fmt(cpuAverage) : '—'} sub={`Memória ${nodes.length ? fmt(memoryAverage) : '—'}`} color="green" />
          </section>
          {overloadedNodes > 0 && <div className="alert-banner"><AlertTriangle size={18} /><div><b>{overloadedNodes} nó(s) com utilização elevada</b><span>CPU ou memória atingiu 80%. Verifique os recursos na infraestrutura.</span></div><button onClick={() => choosePage('infra')}>Ver nós <ArrowRight size={14} /></button></div>}
          <section className="overview-grid">
            <article className="panel globe-panel">
              <div className="panel-heading"><div><span className="panel-kicker">MAPA DA INFRAESTRUTURA</span><h2>Topologia da rede</h2></div><span className="data-tag"><span className="status-dot online" /> {zones.length} ZONAS</span></div>
              <div className="world-stage"><Suspense fallback={<div className="chart-empty">Carregando topologia 3D…</div>}><Globe zones={zones} nodes={nodes} reducedMotion={reducedMotion} /></Suspense>
                {zones.slice(0, 3).map((zone, i) => <div key={zone.id} className={`zone-float zone-float-${i + 1}`}><span className="status-dot online" /><div><b>{zone.name}</b><small>{nodes.filter(node => node.zoneId === zone.id).length} nós</small></div></div>)}
                {!zones.length && <div className="empty-overlay"><Globe2 size={22} /><span>Cadastre uma zona para visualizar a topologia.</span></div>}
              </div>
              <div className="globe-footer"><span><i className="legend-dot cyan-dot" /> Nó online</span><span><i className="legend-dot purple-dot" /> Zona</span><span className="footnote">Visualização 3D interativa • arraste para orbitar</span></div>
            </article>
            <div className="right-stack">
              <article className="panel monitor-panel"><div className="panel-heading compact"><div><span className="panel-kicker">TELEMETRIA DA API</span><h2>Utilização atual</h2></div><Activity size={17} className="accent-icon" /></div>
                <MetricRow icon={Cpu} label="CPU média" value={nodes.length ? fmt(cpuAverage) : '—'} pct={cpuAverage} color="cyan" />
                <MetricRow icon={Database} label="Memória média" value={nodes.length ? fmt(memoryAverage) : '—'} pct={memoryAverage} color="purple" />
                <MetricRow icon={AlertTriangle} label="Nós sobrecarregados" value={`${overloadedNodes}`} pct={nodes.length ? overloadedNodes / nodes.length * 100 : 0} color={overloadedNodes ? 'amber' : 'green'} />
                <div className="monitor-note">Métricas agregadas dos nós cadastrados.</div>
              </article>
              <article className="panel event-panel"><div className="panel-heading compact"><div><span className="panel-kicker">AUDITORIA LOCAL</span><h2>Atividade nesta sessão</h2></div><span className="session-tag">NÃO PERSISTENTE</span></div>
                {events.length ? <div className="event-list">{events.slice(0, 4).map(item => <div className="event-row" key={item.id}><i className={`event-mark ${item.tone}`} /><div><b>{item.title}</b><small>{item.detail}</small></div><time>{item.at}</time></div>)}</div> : <div className="empty-small">Ações realizadas nesta sessão aparecerão aqui. A API ainda não oferece histórico de eventos.</div>}
              </article>
            </div>
          </section>
          <section className="bottom-grid">
            <article className="panel chart-panel"><div className="panel-heading"><div><span className="panel-kicker">AMOSTRAS DA SESSÃO</span><h2>Utilização ao longo do tempo</h2></div><div className="chart-legend"><span><i className="legend-dot cyan-dot" /> CPU</span><span><i className="legend-dot purple-dot" /> Memória</span></div></div>
              {samples.length > 1 ? <Suspense fallback={<div className="chart-empty">Carregando gráfico…</div>}><MetricsChart samples={samples} reducedMotion={reducedMotion} /></Suspense> : <div className="chart-empty"><Activity size={20} /><span>{nodes.length ? 'Coletando amostras reais da API…' : 'Cadastre nós para iniciar a coleta de métricas.'}</span></div>}
              <div className="chart-caption"><span><i className="status-dot online" /> Atualização a cada {pollSeconds}s</span><span>O histórico fica apenas nesta sessão do navegador.</span></div>
            </article>
            <article className="panel capacity-panel"><div className="panel-heading"><div><span className="panel-kicker">CAPACIDADE DO CLUSTER</span><h2>Resumo por rede</h2></div><Boxes size={17} className="accent-icon" /></div>
              {networkMetrics.length ? <div className="capacity-list">{networkMetrics.map(network => <div className="capacity-item" key={network.networkId}><div className="capacity-title"><b>{network.name}</b><span>{network.nodeCount} nós • {network.zoneCount} zonas</span></div><div className="capacity-bar"><i style={{ width: `${Math.max(network.averageCpuUsage, network.averageMemoryUsage)}%` }} /></div><div className="capacity-meta"><span>CPU {fmt(network.averageCpuUsage)}</span><span>MEM {fmt(network.averageMemoryUsage)}</span></div></div>)}</div> : <div className="empty-small">Nenhuma rede retornada pela API.</div>}
              <button className="text-button" onClick={() => choosePage('infra')}>Explorar infraestrutura <ArrowRight size={14} /></button>
            </article>
          </section>
          <section className="panel resource-panel"><div className="panel-heading"><div><span className="panel-kicker">RECURSOS DA ZONA</span><h2>{selectedZone?.name ?? 'Demandas de trabalho'}</h2></div><div className="inline-actions"><select aria-label="Selecionar zona" value={selectedZone?.id ?? ''} onChange={e => setSelectedZoneId(e.target.value)} disabled={!zones.length}><option value="">Selecione uma zona</option>{zones.map(zone => <option key={zone.id} value={zone.id}>{zone.name}</option>)}</select><button className="primary-button small" onClick={() => { setPlan(null); setModal('workload') }} disabled={!selectedZone}><Plus size={15} /> Nova demanda</button></div></div>
            {activeWorkloads.length ? <div className="workload-strip">{activeWorkloads.slice(0, 4).map(workload => <div className="workload-chip" key={workload.id}><span className="status-dot online" /><div><b>{workload.name}</b><small>{workload.cpuDemand} CPU • {workload.memoryDemand} memória</small></div><button className="icon-button stop-button" onClick={() => void stopWorkload(workload)} disabled={busy} aria-label={`Encerrar ${workload.name}`} title="Encerrar demanda"><X size={15} /></button></div>)}</div> : <div className="empty-small">{selectedZone ? 'Nenhuma demanda ativa nesta zona. Crie uma para alocar recursos reais.' : 'Cadastre ou selecione uma zona para consultar demandas.'}</div>}
          </section>
        </>}
        {page === 'infra' && <section className="page-section">
          <PageHeading kicker="RECURSOS CONECTADOS À API" title="Infraestrutura" description="Redes, zonas e nós existentes no backend. As ações desta tela alteram os dados persistidos." action={<button className="primary-button" onClick={() => setModal('node')}><Plus size={16} /> Adicionar nó</button>} />
          <div className="panel table-panel"><div className="table-toolbar"><div className="tab-list">{(['nodes', 'zones', 'workloads'] as const).map(tab => <button className={activeTab === tab ? 'selected' : ''} onClick={() => setActiveTab(tab)} key={tab}>{tab === 'nodes' ? `Nós (${nodes.length})` : tab === 'zones' ? `Zonas (${zones.length})` : `Demandas (${workloads.length})`}</button>)}</div><button className="icon-button" onClick={() => void refresh()} aria-label="Atualizar lista"><RefreshCw size={16} /></button></div>
            {activeTab === 'nodes' && <div className="table-scroll"><table><thead><tr><th>Nó</th><th>Zona</th><th>Status</th><th>CPU</th><th>Memória</th><th>Ação</th></tr></thead><tbody>{filteredNodes.map(node => <tr key={node.id}><td><span className="node-name"><Server size={16} />{node.name}</span></td><td>{zones.find(zone => zone.id === node.zoneId)?.name ?? 'Sem zona'}</td><td><StatusPill status={node.status} /></td><td><Usage value={node.cpuUsage} /></td><td><Usage value={node.memoryUsage} /></td><td><button className="icon-button danger-button" onClick={() => void removeNode(node)} disabled={busy} aria-label={`Remover nó ${node.name}`}><Trash2 size={15} /></button></td></tr>)}</tbody></table>{!filteredNodes.length && <div className="empty-table">{nodes.length ? 'Nenhum nó corresponde à busca.' : 'A API não retornou nós. Adicione seu primeiro nó.'}</div>}</div>}
            {activeTab === 'zones' && <div className="table-scroll"><table><thead><tr><th>Zona</th><th>Rede</th><th>Nós</th><th>CPU média</th><th>Memória média</th></tr></thead><tbody>{zones.map(zone => { const metrics = zoneMetrics.find(item => item.zoneId === zone.id); return <tr key={zone.id}><td><span className="node-name"><Layers3 size={16} />{zone.name}</span></td><td>{networks.find(net => net.id === zone.networkId)?.name ?? 'Sem rede'}</td><td>{metrics?.nodeCount ?? 0}</td><td><Usage value={metrics?.averageCpuUsage ?? 0} /></td><td><Usage value={metrics?.averageMemoryUsage ?? 0} /></td></tr> })}</tbody></table>{!zones.length && <div className="empty-table">Nenhuma zona foi cadastrada no backend.</div>}</div>}
            {activeTab === 'workloads' && <div className="table-scroll"><table><thead><tr><th>Demanda</th><th>Status</th><th>CPU</th><th>Memória</th><th>Alocada em</th><th>Ação</th></tr></thead><tbody>{workloads.map(item => <tr key={item.id}><td><span className="node-name"><Activity size={16} />{item.name}</span></td><td><StatusPill status={item.status} /></td><td>{item.cpuDemand}</td><td>{item.memoryDemand}</td><td>{item.allocations.map(a => a.nodeName).join(', ') || '—'}</td><td>{item.status === 'ACTIVE' && <button className="icon-button danger-button" onClick={() => void stopWorkload(item)} aria-label={`Encerrar demanda ${item.name}`}><X size={15} /></button>}</td></tr>)}</tbody></table>{!workloads.length && <div className="empty-table">Nenhuma demanda registrada para a zona selecionada.</div>}</div>}
          </div>
          {networks.length > 0 && <div className="network-card-grid">{networkMetrics.map(metric => <article className="panel mini-network" key={metric.networkId}><NetworkIcon size={18} className="accent-icon" /><b>{metric.name}</b><span>{metric.zoneCount} zonas • {metric.nodeCount} nós</span><div className="mini-stats"><span>CPU {fmt(metric.averageCpuUsage)}</span><span>MEM {fmt(metric.averageMemoryUsage)}</span></div></article>)}</div>}
        </section>}
        {page === 'simulation' && <section className="page-section"><PageHeading kicker="PLANEJAMENTO DE CAPACIDADE" title="Simulação de demanda" description="O backend calcula se CPU e memória solicitadas cabem na zona e retorna as alocações previstas. A simulação não cria a demanda até você confirmar." />
          <div className="simulation-layout"><form className="panel form-panel" onSubmit={runPlan}><span className="panel-kicker">PARÂMETROS DO PLANO</span><h2>Defina a carga desejada</h2><label>Zona de destino<select required value={selectedZone?.id ?? ''} onChange={e => setSelectedZoneId(e.target.value)}><option value="">Selecione uma zona</option>{zones.map(zone => <option key={zone.id} value={zone.id}>{zone.name}</option>)}</select></label><label>CPU solicitada (%)<input type="number" min="0" step="0.1" required value={workloadForm.cpuDemand} onChange={e => setWorkloadForm(p => ({ ...p, cpuDemand: Number(e.target.value) }))} /></label><label>Memória solicitada (%)<input type="number" min="0" step="0.1" required value={workloadForm.memoryDemand} onChange={e => setWorkloadForm(p => ({ ...p, memoryDemand: Number(e.target.value) }))} /></label><button className="primary-button" disabled={busy || !selectedZone}><Sparkles size={16} /> {busy ? 'Calculando…' : 'Calcular distribuição'}</button></form>
            <article className="panel result-panel"><div className="panel-heading"><div><span className="panel-kicker">RESPOSTA REAL DO BACKEND</span><h2>Plano de distribuição</h2></div><BrainCircuit className="accent-icon" /></div>{plan ? <><div className={`feasibility ${plan.feasible ? 'feasible' : 'not-feasible'}`}><i>{plan.feasible ? <Check size={18} /> : <AlertTriangle size={18} />}</i><div><b>{plan.feasible ? 'Plano viável' : 'Capacidade insuficiente'}</b><span>Retornado pelo serviço de distribuição da zona</span></div></div><div className="plan-stats"><PlanStat label="CPU solicitada" value={fmt(plan.requestedCpu)} /><PlanStat label="Memória solicitada" value={fmt(plan.requestedMemory)} /><PlanStat label="CPU disponível" value={fmt(plan.availableCpu)} /><PlanStat label="Memória disponível" value={fmt(plan.availableMemory)} /></div><div className="allocation-list"><b>Alocações previstas</b>{plan.allocations.map(row => <div className="allocation-row" key={row.nodeId}><span><Server size={15} />{row.nodeName}</span><small>CPU +{fmt(row.allocatedCpu)} • MEM +{fmt(row.allocatedMemory)}</small><small>Projetado: CPU {fmt(row.projectedCpuUsage)} / MEM {fmt(row.projectedMemoryUsage)}</small></div>)}</div>{plan.feasible && <button className="primary-button" onClick={() => { setWorkloadForm(p => ({ ...p, name: p.name || 'workload-simulada' })); setModal('workload') }}><Plus size={16} /> Criar demanda persistente</button>}</> : <div className="empty-result"><BrainCircuit size={28} /><p>Preencha os parâmetros para consultar o serviço de distribuição.</p></div>}</article></div>
        </section>}
        {page === 'expansion' && <section className="page-section"><PageHeading kicker="ANÁLISE LOCAL • NÃO PERSISTE ALTERAÇÕES" title="Planejamento de expansão" description="O backend atual não possui rota de expansão automática. Esta tela estima um cenário local com base nos nós e métricas retornados pela API." />
          <div className="expansion-grid"><article className="panel expansion-card"><div className="panel-heading"><div><span className="panel-kicker">CENÁRIO OBSERVADO</span><h2>Pressão de recursos</h2></div><Zap className="accent-icon" /></div><div className="pressure-stats"><div><span>CPU média</span><b>{nodes.length ? fmt(cpuAverage) : '—'}</b><small>{cpuAverage >= 80 ? 'Alta' : 'Dentro do limite configurável'}</small></div><div><span>MEMÓRIA média</span><b>{nodes.length ? fmt(memoryAverage) : '—'}</b><small>{memoryAverage >= 80 ? 'Alta' : 'Dentro do limite configurável'}</small></div></div><p className="explain-note">Os valores são métricas atuais dos nós. Não existe criação automática de servidores no backend.</p></article><article className="panel expansion-card"><div className="panel-heading"><div><span className="panel-kicker">PROJEÇÃO DEMONSTRATIVA</span><h2>Adicione uma carga hipotética</h2></div><SlidersHorizontal className="accent-icon" /></div><p className="explain-note">A projeção abaixo é calculada nesta interface e não altera a infraestrutura.</p><label className="range-label">Demanda hipotética: <b>{workloadForm.cpuDemand}% CPU</b><input type="range" min="0" max="100" value={workloadForm.cpuDemand} onChange={e => setWorkloadForm(p => ({ ...p, cpuDemand: Number(e.target.value) }))} /></label><label className="range-label">Memória hipotética: <b>{workloadForm.memoryDemand}%</b><input type="range" min="0" max="100" value={workloadForm.memoryDemand} onChange={e => setWorkloadForm(p => ({ ...p, memoryDemand: Number(e.target.value) }))} /></label><button className="secondary-button" onClick={() => choosePage('simulation')}>Validar no backend <ArrowRight size={14} /></button></article></div>
          <div className="pending-integrations"><CircleHelp size={17} /><div><b>Expansão automática pendente</b><span>Será necessário implementar no backend um modelo de capacidade e uma rota de expansão antes de automatizar a criação de nós.</span></div></div></section>}
        {page === 'reports' && <section className="page-section"><PageHeading kicker="OBSERVABILIDADE" title="Relatórios" description="Resumo baseado nos dados atuais e amostras coletadas enquanto esta aba permanece aberta." />
          <div className="report-grid"><article className="panel report-chart"><div className="panel-heading"><div><span className="panel-kicker">COLETA DA SESSÃO</span><h2>CPU e memória</h2></div><span className="data-tag">{samples.length} AMOSTRAS</span></div>{samples.length > 1 ? <Suspense fallback={<div className="empty-table">Carregando gráfico…</div>}><MetricsChart samples={samples} reducedMotion={reducedMotion} tall /></Suspense> : <div className="empty-table">Ainda não há amostras suficientes. A coleta começa com nós retornados pela API.</div>}</article><article className="panel report-stats"><span className="panel-kicker">FOTOGRAFIA ATUAL</span><h2>Estado da infraestrutura</h2><PlanStat label="Redes" value={`${networks.length}`} /><PlanStat label="Zonas" value={`${zones.length}`} /><PlanStat label="Nós online" value={`${onlineNodes} / ${nodes.length}`} /><PlanStat label="Alertas atuais" value={`${overloadedNodes}`} /></article></div>
          <div className="pending-integrations"><Database size={17} /><div><b>Retenção histórica não disponível</b><span>As amostras são mantidas apenas em memória no navegador; a API ainda não possui endpoint para armazenar séries temporais.</span></div></div></section>}
        {page === 'security' && <section className="page-section"><PageHeading kicker="CENTRO DE SEGURANÇA" title="Segurança" description="Esta seção ainda não está conectada a serviços ou endpoints de segurança." />
          <article className="panel security-placeholder"><div className="security-icon"><LockKeyhole size={27} /></div><span className="pending-pill">PENDENTE DE INTEGRAÇÃO</span><h2>Sem dados de segurança na API</h2><p>O DYSONOS backend atual não expõe autenticação de operadores, auditoria persistente, vulnerabilidades ou alertas de segurança. Nenhum estado de segurança será inventado nesta tela.</p><div className="security-checks"><span><X size={14} /> Sem endpoint de autenticação</span><span><X size={14} /> Sem trilha de auditoria persistente</span><span><X size={14} /> Sem scanner de vulnerabilidades</span></div></article></section>}
        {page === 'settings' && <section className="page-section"><PageHeading kicker="PREFERÊNCIAS LOCAIS" title="Configurações" description="Controles aplicados somente a esta interface e ao seu navegador." />
          <article className="panel settings-panel"><div className="setting-row"><div><b>Intervalo de atualização</b><span>Frequência de consulta da API nesta página.</span></div><select value={pollSeconds} onChange={e => setPollSeconds(Number(e.target.value))}><option value={5}>5 segundos</option><option value={8}>8 segundos</option><option value={15}>15 segundos</option><option value={30}>30 segundos</option></select></div><div className="setting-row"><div><b>Movimento reduzido</b><span>Preferência do sistema: {reducedMotion ? 'ativada' : 'desativada'}. Animações respeitam esta configuração.</span></div><span className="setting-value">{reducedMotion ? 'REDUZIDO' : 'PADRÃO DO SISTEMA'}</span></div><div className="setting-row"><div><b>API conectada</b><span>O frontend utiliza o proxy do Vite para enviar requisições a /api.</span></div><span className={`live-pill ${health}`}><i />{health.toUpperCase()}</span></div><div className="setting-row"><div><b>Registros de atividade</b><span>Eventos gerados pelas ações desta sessão, sem persistência no servidor.</span></div><span className="setting-value">{events.length} EVENTOS</span></div></article></section>}
        <footer className="page-footer"><span>DYSONOS V2 <i>•</i> PAINEL DE OPERAÇÕES</span><span>Dados fornecidos pela API DYSONOS</span></footer>
      </div>
    </main>
    {modal && <div className="modal-backdrop" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) setModal(null) }}><section className="dialog panel" role="dialog" aria-modal="true" aria-labelledby="dialog-title"><div className="panel-heading"><div><span className="panel-kicker">{modal === 'node' ? 'NÓ DE INFRAESTRUTURA' : 'DEMANDA DE TRABALHO'}</span><h2 id="dialog-title">{modal === 'node' ? 'Adicionar nó' : 'Criar demanda'}</h2></div><button className="icon-button" onClick={() => setModal(null)} aria-label="Fechar"><X size={18} /></button></div>
      {modal === 'node' ? <form className="dialog-form" onSubmit={createNode}><label>Nome do nó<input autoFocus required maxLength={120} value={nodeForm.name} onChange={e => setNodeForm(p => ({ ...p, name: e.target.value }))} placeholder="ex.: edge-03" /></label><label>Zona<select value={nodeForm.zoneId} onChange={e => setNodeForm(p => ({ ...p, zoneId: e.target.value }))}><option value="">Sem zona</option>{zones.map(zone => <option key={zone.id} value={zone.id}>{zone.name}</option>)}</select></label><div className="two-fields"><label>CPU atual (%)<input type="number" min="0" max="100" step="0.1" value={nodeForm.cpuUsage} onChange={e => setNodeForm(p => ({ ...p, cpuUsage: Number(e.target.value) }))} /></label><label>Memória atual (%)<input type="number" min="0" max="100" step="0.1" value={nodeForm.memoryUsage} onChange={e => setNodeForm(p => ({ ...p, memoryUsage: Number(e.target.value) }))} /></label></div><p className="form-hint">O backend aceita CPU e memória de 0 a 100. Status inicial definido pelo servidor.</p><button className="primary-button full-width" disabled={busy}><Plus size={16} /> {busy ? 'Salvando…' : 'Criar nó na API'}</button></form> : <form className="dialog-form" onSubmit={createWorkload}><label>Nome da demanda<input autoFocus required maxLength={120} value={workloadForm.name} onChange={e => setWorkloadForm(p => ({ ...p, name: e.target.value }))} placeholder="ex.: api-service" /></label><label>Zona de destino<select required value={selectedZone?.id ?? ''} onChange={e => setSelectedZoneId(e.target.value)}><option value="">Selecione uma zona</option>{zones.map(zone => <option key={zone.id} value={zone.id}>{zone.name}</option>)}</select></label><div className="two-fields"><label>CPU solicitada<input type="number" min="0" step="0.1" required value={workloadForm.cpuDemand} onChange={e => setWorkloadForm(p => ({ ...p, cpuDemand: Number(e.target.value) }))} /></label><label>Memória solicitada<input type="number" min="0" step="0.1" required value={workloadForm.memoryDemand} onChange={e => setWorkloadForm(p => ({ ...p, memoryDemand: Number(e.target.value) }))} /></label></div><p className="form-hint">A criação persiste a demanda e suas alocações no PostgreSQL. A zona precisa ter capacidade disponível.</p><button className="secondary-button full-width" type="button" onClick={runPlan as unknown as React.MouseEventHandler} disabled={busy || !selectedZone}>Verificar capacidade primeiro</button>{plan && <div className={`plan-inline ${plan.feasible ? 'good' : 'bad'}`}>{plan.feasible ? 'Plano viável' : 'Capacidade insuficiente'} • CPU livre {fmt(plan.availableCpu)} • MEM livre {fmt(plan.availableMemory)}</div>}<button className="primary-button full-width" disabled={busy || !selectedZone}><Zap size={16} /> {busy ? 'Criando…' : 'Criar e alocar demanda'}</button></form>}
    </section></div>}
  </div>
}

function Kpi({ icon: Icon, label, value, sub, color }: { icon: typeof Activity; label: string; value: string; sub: string; color: string }) { return <article className={`panel kpi-card ${color}`}><div className="kpi-top"><span className="kpi-icon"><Icon size={19} /></span><span className="kpi-label">{label}</span><ArrowUpRight size={15} className="kpi-trend" /></div><b className="kpi-value">{value}</b><span className="kpi-sub">{sub}</span><div className="kpi-glow" /></article> }
function MetricRow({ icon: Icon, label, value, pct, color }: { icon: typeof Activity; label: string; value: string; pct: number; color: string }) { return <div className="metric-row"><span className={`metric-icon ${color}`}><Icon size={15} /></span><span className="metric-label">{label}</span><div className="metric-track"><i className={color} style={{ width: `${Math.min(100, pct)}%` }} /></div><b>{value}</b></div> }
function PageHeading({ kicker, title, description, action }: { kicker: string; title: string; description: string; action?: React.ReactNode }) { return <div className="page-heading"><div><div className="eyebrow"><span className="eyebrow-line" /> {kicker}</div><h1>{title}</h1><p>{description}</p></div>{action}</div> }
function StatusPill({ status }: { status: string }) { const good = status === 'ONLINE' || status === 'ACTIVE'; const warning = status === 'OVERLOADED'; return <span className={`status-pill ${good ? 'good' : warning ? 'warning' : 'muted'}`}><i />{status}</span> }
function Usage({ value }: { value: number }) { const color = value >= 80 ? 'amber' : value >= 60 ? 'purple' : 'cyan'; return <span className="usage-cell"><span className="usage-track"><i className={color} style={{ width: `${Math.min(100, value)}%` }} /></span><b className={value >= 80 ? 'warning-text' : ''}>{fmt(value)}</b></span> }
function PlanStat({ label, value }: { label: string; value: string }) { return <div className="plan-stat"><span>{label}</span><b>{value}</b></div> }
