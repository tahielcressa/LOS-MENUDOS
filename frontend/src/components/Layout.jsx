import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'

import {
  LayoutDashboard,
  FileUp,
  History,
  Users as UsersIcon,
  LogOut,
  Mountain,
} from 'lucide-react'

const nav = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/cargar', label: 'Cargar datos', icon: FileUp },
  { to: '/historial', label: 'Historial', icon: History },
  { to: '/usuarios', label: 'Usuarios y equipos', icon: UsersIcon, admin: true },
]

function NavItems() {
  const { user } = useAuth()
  return nav
    .filter((n) => !n.admin || user?.role === 'ADMIN')
    .map((n) => (
      <NavLink
        key={n.to}
        to={n.to}
        end={n.end}
        className={({ isActive }) =>
          `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
            isActive
              ? 'bg-amber-500 text-slate-900'
              : 'text-slate-300 hover:bg-slate-800 hover:text-white'
          }`
        }
      >
        <n.icon size={18} />
        {n.label}
      </NavLink>
    ))
}

export default function Layout() {
  const { user, logout } = useAuth()
  const initial = user?.fullName?.charAt(0)?.toUpperCase() || 'U'

  return (
    <div className="flex h-screen bg-slate-100">
      <aside className="flex w-64 flex-col bg-slate-900 text-white">
        <div className="flex items-center gap-3 border-b border-slate-800 px-5 py-5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500">
            <Mountain size={22} className="text-slate-900" />
          </div>
          <div>
            <p className="text-lg font-extrabold tracking-tight">MineOps</p>
            <p className="text-xs text-slate-400">Gestión de operaciones mineras</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto scrollbar-thin p-4">
          <NavItems />
        </nav>

        <div className="border-t border-slate-800 p-4">
          <div className="mb-3 rounded-lg bg-slate-800 p-3">
            <p className="text-[11px] uppercase tracking-wide text-slate-400">Empresa</p>
            <p className="truncate text-sm font-semibold text-white">{user?.companyName}</p>
            <span className="mt-1 inline-block rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] font-semibold text-emerald-400">
              Multi-tenant
            </span>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-amber-500 text-sm font-bold text-slate-900">
              {initial}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold">{user?.fullName}</p>
              <p className="truncate text-xs text-slate-400">{user?.role?.toLowerCase()}</p>
            </div>
            <button
              onClick={logout}
              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-800 hover:text-white"
              title="Cerrar sesión"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto scrollbar-thin">
        <div className="mx-auto max-w-6xl p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}