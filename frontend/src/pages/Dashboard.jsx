import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
  CartesianGrid,
} from 'recharts'
import {
  Activity,
  CheckCircle2,
  XCircle,
  Truck,
  Percent,
  Loader2,
  ArrowRight,
} from 'lucide-react'
import api from '../api'

const COLORS = ['#f59e0b', '#0ea5e9', '#10b981', '#8b5cf6', '#ef4444']

const fmt = (n) => (n == null ? '—' : Number(n).toLocaleString('es-AR'))

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .get('/dashboard')
      .then((res) => setData(res.data))
      .catch((err) => setError(err.response?.data?.message || 'Error al cargar el dashboard'))
  }, [])

  if (error) {
    return <div className="rounded-xl bg-red-50 p-6 text-sm text-red-700">{error}</div>
  }
  if (!data) {
    return (
      <div className="flex items-center gap-3 text-slate-500">
        <Loader2 size={20} className="animate-spin" /> Cargando dashboard...
      </div>
    )
  }

  const cards = [
    { label: 'Procesos ejecutados', value: data.totalRuns, icon: Activity, color: 'bg-sky-500' },
    { label: 'Filas válidas', value: fmt(data.validRows), icon: CheckCircle2, color: 'bg-emerald-500' },
    { label: 'Filas inválidas', value: fmt(data.invalidRows), icon: XCircle, color: 'bg-red-500' },
    { label: 'Tonelaje total (t)', value: fmt(data.totalTonnage), icon: Truck, color: 'bg-amber-500' },
    { label: 'Ley media de cobre (%)', value: fmt(data.avgGrade), icon: Percent, color: 'bg-violet-500' },
  ]

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Dashboard de operación</h1>
        <p className="mt-1 text-sm text-slate-500">
          Resumen de los últimos procesos de carga y cruce de datos de tu empresa.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-5">
        {cards.map((c) => (
          <div key={c.label} className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200/60">
            <div className={`mb-3 inline-flex h-10 w-10 items-center justify-center rounded-xl ${c.color} text-white`}>
              <c.icon size={20} />
            </div>
            <p className="text-2xl font-extrabold text-slate-900">{c.value}</p>
            <p className="mt-0.5 text-xs font-medium text-slate-500">{c.label}</p>
          </div>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
          <h3 className="mb-1 text-sm font-bold text-slate-900">Tonelaje por zona</h3>
          <p className="mb-4 text-xs text-slate-400">En toneladas, según las filas válidas</p>
          {data.perZone.length === 0 ? (
            <Empty />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data.perZone}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="value" fill="#f59e0b" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
          <h3 className="mb-1 text-sm font-bold text-slate-900">Filas por estado</h3>
          <p className="mb-4 text-xs text-slate-400">Distribución de equipos EN_PROCESO / DETENIDO</p>
          {data.perEstado.length === 0 ? (
            <Empty />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={data.perEstado}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={60}
                  outerRadius={95}
                  paddingAngle={3}
                  label={({ name, value }) => `${name} (${value})`}
                >
                  {data.perEstado.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
        <div className="mb-1 flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-900">Últimos procesos</h3>
          <Link
            to="/historial"
            className="flex items-center gap-1 text-xs font-semibold text-amber-600 hover:text-amber-700"
          >
            Ver historial completo <ArrowRight size={14} />
          </Link>
        </div>
        <p className="mb-4 text-xs text-slate-400">Últimas 5 ejecuciones de tu empresa</p>

        {data.recentRuns.length === 0 ? (
          <Empty />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-400">
                  <th className="py-2 pr-4 font-semibold">Archivo</th>
                  <th className="py-2 pr-4 font-semibold">Estado</th>
                  <th className="py-2 pr-4 font-semibold">Válidas</th>
                  <th className="py-2 pr-4 font-semibold">Inválidas</th>
                  <th className="py-2 pr-4 font-semibold">Tonelaje</th>
                  <th className="py-2 font-semibold">Usuario</th>
                </tr>
              </thead>
              <tbody>
                {data.recentRuns.map((r) => (
                  <tr key={r.id} className="border-b border-slate-100 hover:bg-slate-50">
                    <td className="max-w-[200px] truncate py-2 pr-4 font-medium text-slate-800">
                      {r.originalName}
                    </td>
                    <td className="py-2 pr-4">
                      <Badge status={r.status} />
                    </td>
                    <td className="py-2 pr-4 text-slate-600">{r.validRows}</td>
                    <td className="py-2 pr-4 text-slate-600">{r.invalidRows}</td>
                    <td className="py-2 pr-4 text-slate-600">{r.totalTonnage ?? '—'}</td>
                    <td className="py-2 text-slate-500">{r.executedByName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

function Empty() {
  return (
    <div className="flex h-[260px] items-center justify-center rounded-xl border border-dashed border-slate-200 text-sm text-slate-400">
      Sin datos. Sube un archivo en "Cargar datos".
    </div>
  )
}

export function Badge({ status }) {
  const map = {
    OK: 'bg-emerald-100 text-emerald-700',
    ERROR: 'bg-red-100 text-red-700',
    EJECUTANDO: 'bg-sky-100 text-sky-700',
    RECIBIDO: 'bg-slate-100 text-slate-600',
    PROCESADO: 'bg-emerald-100 text-emerald-700',
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${map[status] || 'bg-slate-100 text-slate-600'}`}>
      {status}
    </span>
  )
}