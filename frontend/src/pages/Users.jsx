import { useEffect, useState } from 'react'
import { Users as UsersIcon, Truck, Building2, Loader2, Plus, Shield, User } from 'lucide-react'
import api from '../api'
import { Badge } from './Dashboard'

export default function Users() {
  const [tab, setTab] = useState('users')
  const [users, setUsers] = useState([])
  const [equipments, setEquipments] = useState([])
  const [company, setCompany] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    Promise.all([
      api.get('/admin/users'),
      api.get('/admin/equipments'),
      api.get('/admin/company'),
    ])
      .then(([u, e, c]) => {
        setUsers(u.data)
        setEquipments(e.data)
        setCompany(c.data)
      })
      .catch((err) => setError(err.response?.data?.message || 'Error al cargar'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  if (loading) {
    return (
      <div className="flex items-center gap-3 text-slate-500">
        <Loader2 size={20} className="animate-spin" /> Cargando administración...
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Administración</h1>
        <p className="mt-1 text-sm text-slate-500">Usuarios, roles y catálogo de equipos de la empresa.</p>
      </div>

      {company && (
        <div className="flex items-center gap-3 rounded-2xl bg-slate-900 p-5 text-white">
          <Building2 size={22} className="text-amber-500" />
          <div>
            <p className="font-bold">{company.name}</p>
            <p className="text-sm text-slate-400">
              {company.code} · {company.country} · {company.users} usuarios · {company.equipments} equipos
            </p>
          </div>
        </div>
      )}

      {error && <div className="rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</div>}

      <div className="flex gap-2">
        {[
          { id: 'users', label: 'Usuarios', icon: UsersIcon },
          { id: 'equipment', label: 'Catálogo de equipos', icon: Truck },
        ].map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition ${
              tab === t.id
                ? 'bg-slate-900 text-white'
                : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-100'
            }`}
          >
            <t.icon size={15} /> {t.label}
          </button>
        ))}
      </div>

      {tab === 'users' ? (
        <UsersTab users={users} onCreated={load} />
      ) : (
        <EquipmentTab equipments={equipments} onCreated={load} />
      )}
    </div>
  )
}

function RoleBadge({ role }) {
  return role === 'ADMIN' ? (
    <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-700">
      <Shield size={12} /> ADMIN
    </span>
  ) : (
    <span className="inline-flex items-center gap-1 rounded-full bg-sky-100 px-2.5 py-0.5 text-xs font-semibold text-sky-700">
      <User size={12} /> OPERADOR
    </span>
  )
}

function UsersTab({ users, onCreated }) {
  const [form, setForm] = useState({ fullName: '', email: '', password: '', role: 'OPERATOR' })
  const [msg, setMsg] = useState('')

  const create = async (e) => {
    e.preventDefault()
    setMsg('')
    try {
      await api.post('/admin/users', form)
      setForm({ fullName: '', email: '', password: '', role: 'OPERATOR' })
      setMsg('Usuario creado')
      onCreated()
    } catch (err) {
      setMsg(err.response?.data?.message || 'Error')
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
        <h3 className="mb-1 text-sm font-bold text-slate-900">Usuarios de la empresa</h3>
        <p className="mb-4 text-xs text-slate-400">ADMIN gestiona todo; OPERADOR carga y procesa datos.</p>
        <div className="space-y-3">
          {users.map((u) => (
            <div key={u.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
              <div>
                <p className="font-semibold text-slate-800">{u.fullName}</p>
                <p className="text-xs text-slate-500">{u.email}</p>
              </div>
              <RoleBadge role={u.role} />
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
        <h3 className="mb-1 text-sm font-bold text-slate-900">Nuevo usuario</h3>
        <p className="mb-4 text-xs text-slate-400">Se crea dentro de tu empresa (multi-tenant).</p>
        <form onSubmit={create} className="space-y-3">
          <input
            required
            placeholder="Nombre completo"
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500 focus:ring-2 focus:ring-amber-200"
          />
          <input
            required
            type="email"
            placeholder="Email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500 focus:ring-2 focus:ring-amber-200"
          />
          <input
            required
            type="password"
            placeholder="Contraseña"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500 focus:ring-2 focus:ring-amber-200"
          />
          <select
            value={form.role}
            onChange={(e) => setForm({ ...form, role: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500"
          >
            <option value="OPERATOR">Operador</option>
            <option value="ADMIN">Administrador</option>
          </select>
          <button className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-bold text-white hover:bg-slate-800">
            <Plus size={15} /> Crear usuario
          </button>
          {msg && <p className="text-xs text-slate-500">{msg}</p>}
        </form>
      </div>
    </div>
  )
}

function EquipmentTab({ equipments, onCreated }) {
  const [form, setForm] = useState({ code: '', name: '', area: '', type: '' })
  const [msg, setMsg] = useState('')

  const create = async (e) => {
    e.preventDefault()
    setMsg('')
    try {
      await api.post('/admin/equipments', form)
      setForm({ code: '', name: '', area: '', type: '' })
      setMsg('Equipo agregado')
      onCreated()
    } catch (err) {
      setMsg(err.response?.data?.message || 'Error')
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/60">
        <div className="p-6">
          <h3 className="text-sm font-bold text-slate-900">Catálogo de equipos</h3>
          <p className="mt-1 text-xs text-slate-400">
            El proceso cruza cada fila del archivo contra este catálogo.
          </p>
        </div>
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-400">
            <tr>
              <th className="px-4 py-2 font-semibold">Código</th>
              <th className="px-4 py-2 font-semibold">Nombre</th>
              <th className="px-4 py-2 font-semibold">Área</th>
              <th className="px-4 py-2 font-semibold">Tipo</th>
            </tr>
          </thead>
          <tbody>
            {equipments.map((eq) => (
              <tr key={eq.id} className="border-t border-slate-100">
                <td className="px-4 py-2 font-mono text-xs font-bold text-amber-600">{eq.code}</td>
                <td className="px-4 py-2 text-slate-700">{eq.name}</td>
                <td className="px-4 py-2 text-slate-500">{eq.area}</td>
                <td className="px-4 py-2 text-slate-500">{eq.type}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
        <h3 className="mb-1 text-sm font-bold text-slate-900">Nuevo equipo</h3>
        <p className="mb-4 text-xs text-slate-400">Debe coincidir con el código usado en tus archivos.</p>
        <form onSubmit={create} className="space-y-3">
          <input
            required
            placeholder="Código (ej: CAM-04)"
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500 focus:ring-2 focus:ring-amber-200"
          />
          <input
            required
            placeholder="Nombre (ej: Camión 785C-04)"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500 focus:ring-2 focus:ring-amber-200"
          />
          <input
            placeholder="Área (ej: CHACRA, SUR, NORESTE)"
            value={form.area}
            onChange={(e) => setForm({ ...form, area: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500 focus:ring-2 focus:ring-amber-200"
          />
          <select
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value })}
            className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm outline-none focus:border-amber-500"
          >
            <option value="">Tipo de equipo...</option>
            <option value="CAMION">Camión</option>
            <option value="CARGADOR">Cargador</option>
            <option value="PERFORADORA">Perforadora</option>
            <option value="OTRO">Otro</option>
          </select>
          <button className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-bold text-white hover:bg-slate-800">
            <Plus size={15} /> Agregar equipo
          </button>
          {msg && <p className="text-xs text-slate-500">{msg}</p>}
        </form>
      </div>
    </div>
  )
}