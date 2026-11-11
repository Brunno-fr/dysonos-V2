export type NodeStatus = 'ONLINE' | 'OVERLOADED'
export interface Network { id: string; name: string; zoneIds: string[]; createdAt: string }
export interface Zone { id: string; name: string; networkId: string | null; nodeIds: string[]; createdAt: string }
export interface Node { id: string; name: string; cpuUsage: number; memoryUsage: number; status: NodeStatus; zoneId: string | null; createdAt: string }
export interface NetworkMetrics { networkId: string; name: string; zoneCount: number; nodeCount: number; overloadedNodeCount: number; averageCpuUsage: number; averageMemoryUsage: number }
export interface ZoneMetrics { zoneId: string; name: string; networkId: string | null; nodeCount: number; overloadedNodeCount: number; averageCpuUsage: number; averageMemoryUsage: number }
export interface Plan { zoneId: string; feasible: boolean; requestedCpu: number; requestedMemory: number; availableCpu: number; availableMemory: number; allocations: { nodeId: string; nodeName: string; allocatedCpu: number; allocatedMemory: number; projectedCpuUsage: number; projectedMemoryUsage: number }[] }
export interface Workload { id: string; name: string; zoneId: string; cpuDemand: number; memoryDemand: number; status: 'ACTIVE' | 'STOPPED'; createdAt: string; allocations: { nodeId: string; nodeName: string; cpuAllocated: number; memoryAllocated: number }[] }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...init,
    headers: { ...(init?.body ? { 'Content-Type': 'application/json' } : {}), ...init?.headers },
  })
  if (!response.ok) {
    const body = await response.text()
    let detail = body
    try { detail = JSON.parse(body).message ?? body } catch { /* keep backend text */ }
    throw new Error(detail || `Erro HTTP ${response.status}`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

const json = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) })
export const api = {
  health: () => request<{ status: string }>('/health'),
  networks: () => request<Network[]>('/networks'),
  networkMetrics: (id: string) => request<NetworkMetrics>(`/networks/${id}/metrics`),
  zones: () => request<Zone[]>('/zones'),
  zoneMetrics: (id: string) => request<ZoneMetrics>(`/zones/${id}/metrics`),
  nodes: () => request<Node[]>('/nodes'),
  createNode: (data: { name: string; cpuUsage: number; memoryUsage: number; zoneId: string | null }) => request<Node>('/nodes', json(data)),
  deleteNode: (id: string) => request<void>(`/nodes/${id}`, { method: 'DELETE' }),
  updateNode: (id: string, data: { cpuUsage: number; memoryUsage: number }) => request<Node>(`/nodes/${id}/usage`, { method: 'PUT', body: JSON.stringify(data), headers: { 'Content-Type': 'application/json' } }),
  plan: (zoneId: string, data: { cpuDemand: number; memoryDemand: number }) => request<Plan>(`/zones/${zoneId}/distribution-plan`, json(data)),
  workloads: (zoneId: string) => request<Workload[]>(`/zones/${zoneId}/workloads`),
  createWorkload: (zoneId: string, data: { name: string; cpuDemand: number; memoryDemand: number }) => request<Workload>(`/zones/${zoneId}/workloads`, json(data)),
  stopWorkload: (id: string) => request<Workload>(`/workloads/${id}`, { method: 'DELETE' }),
}
