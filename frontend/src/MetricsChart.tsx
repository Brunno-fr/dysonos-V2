import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

type Sample = { time: string; cpu: number; memory: number }

export default function MetricsChart({ samples, reducedMotion, tall = false }: { samples: Sample[]; reducedMotion: boolean; tall?: boolean }) {
  return <div className={`chart-wrap${tall ? ' tall' : ''}`}><ResponsiveContainer width="100%" height="100%"><AreaChart data={samples} margin={{ top: 12, right: 8, bottom: 0, left: -18 }}>
    <defs><linearGradient id={tall ? 'reportCpuFill' : 'cpuFill'} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#18d9ff" stopOpacity={0.24} /><stop offset="95%" stopColor="#18d9ff" stopOpacity={0} /></linearGradient><linearGradient id={tall ? 'reportMemFill' : 'memFill'} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#a855f7" stopOpacity={0.2} /><stop offset="95%" stopColor="#a855f7" stopOpacity={0} /></linearGradient></defs>
    <CartesianGrid stroke="rgba(115,151,221,.12)" vertical={false} /><XAxis dataKey="time" tick={{ fill: '#7585a5', fontSize: 10 }} axisLine={false} tickLine={false} minTickGap={24} /><YAxis domain={[0, 100]} tick={{ fill: '#7585a5', fontSize: 10 }} axisLine={false} tickLine={false} tickFormatter={v => `${v}%`} /><Tooltip contentStyle={{ background: '#0a1020', border: '1px solid #283454', borderRadius: 8, color: '#eaf2ff' }} />
    <Area type="monotone" dataKey="cpu" name="CPU média" stroke="#16d9ff" strokeWidth={2} fill={`url(#${tall ? 'reportCpuFill' : 'cpuFill'})`} isAnimationActive={!reducedMotion} /><Area type="monotone" dataKey="memory" name="Memória média" stroke="#b052ff" strokeWidth={2} fill={`url(#${tall ? 'reportMemFill' : 'memFill'})`} isAnimationActive={!reducedMotion} />
  </AreaChart></ResponsiveContainer></div>
}
