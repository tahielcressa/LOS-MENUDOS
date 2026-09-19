import { useEffect, useState } from 'react'
import { Loader2, Download, FileSpreadsheet, FileText } from 'lucide-react'
import api from '../api'
import { Badge } from './Dashboard'

export default function History() {
  const [runs, setRuns] = useState(null)
  const [expanded, setExpanded] = useState(null)

  const load = () => {
    api
      .get('/runs')
      .then((res) => setRuns(res.data))
      .catch(() => setRuns([]))
  }

  useEffect(load, [])

  const download = async (run, type) => {
    const res = await api.get(`/runs/${run.id}/download/${type}`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = type === 'excel' ? run.excelFileName : run.pdfFileName
    a.click()
    URL.revokeObjectURL(url)
  }

  if (!runs) {
    return (
      <div className="flex items-center gap-3 text-slate-500">
        <Loader2 size={20} className="animate-spin" /> Cargando historial...
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Historial de ejecuciones</h1>
        <p className="mt-1 text-sm text-slate-500">
          Cada proceso guarda su resultado, KPIs, reportes generados y log de validación.
        </p>
      </div>

      {runs.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center text-sm text-slate-400">
          Todavía no hay procesos ejecutados en esta empresa.
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/60">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-400">
              <tr>
                <th className="px-4 py-3 font-semibold">Archivo</th>
                <th className="px-4 py-3 font-semibold">Fecha</th>
                <th className="px-4 py-3 font-semibold">Estado</th>
                <th className="px-4 py-3 font-semibold">Válidas / Inválidas</th>
                <th className="px-4 py-3 font-semibold">Tonelaje</th>
                <th className="px-4 py-3 font-semibold">Ley Cu</th>
                <th className="px-4 py-3 font-semibold">Reportes</th>
                <th className="px-4 py-3 font-semibold">Usuario</th>
              </tr>
            </thead>
            <tbody>
              {runs.map((r) => (
                <Row
                  key={r.id}
                  run={r}
                  expanded={expanded === r.id}
                  onToggle={() => setExpanded(expanded === r.id ? null : r.id)}
                  onDownload={download}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function Row({ run, expanded, onToggle, onDownload }) {
  return (
    <>
      <tr className="border-b border-slate-100 hover:bg-slate-50">
        <td className="px-4 py-3">
          <button onClick={onToggle} className="text-left font-medium text-slate-800 hover:text-amber-600">
            {run.originalName}
          </button>
        </td>
        <td className="px-4 py-3 text-slate-500">
          {run.finishedAt ? new Date(run.finishedAt).toLocaleString('es-AR') : '—'}
        </td>
        <td className="px-4 py-3">
          <Badge status={run.status} />
        </td>
        <td className="px-4 py-3 text-slate-600">
          <span className="font-semibold text-emerald-600">{run.validRows}</span>
          {' / '}
          <span className="font-semibold text-red-600">{run.invalidRows}</span>
        </td>
        <td className="px-4 py-3 text-slate-600">{run.totalTonnage ?? '—'}</td>
        <td className="px-4 py-3 text-slate-600">{run.avgGrade ?? '—'}</td>
        <td className="px-4 py-3">
          {run.status === 'OK' && (
            <div className="flex gap-2">
              <button
                onClick={() => onDownload(run, 'excel')}
                className="flex items-center gap-1 rounded-md bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-600 hover:bg-emerald-100"
              >
                <FileSpreadsheet size={14} /> Excel
              </button>
              <button
                onClick={() => onDownload(run, 'pdf')}
                className="flex items-center gap-1 rounded-md bg-red-50 px-2 py-1 text-xs font-semibold text-red-600 hover:bg-red-100"
              >
                <FileText size={14} /> PDF
              </button>
            </div>
          )}
        </td>
        <td className="px-4 py-3 text-slate-500">{run.executedByName}</td>
      </tr>
      {expanded && (
        <tr className="border-b border-slate-100 bg-slate-50/70">
          <td colSpan={8} className="px-4 py-4">
            <div className="mb-3 flex flex-wrap gap-4 text-xs text-slate-500">
              <span>
                Tipo: <b className="text-slate-700">{run.runType}</b>
              </span>
              <span>
                Duración: <b className="text-slate-700">{(run.durationMs / 1000).toFixed(1)} s</b>
              </span>
              <span>
                Ejecutado: <b className="text-slate-700">{run.executedByName}</b>
              </span>
            </div>
            <pre className="whitespace-pre-wrap rounded-lg bg-slate-900 p-4 font-mono text-xs leading-relaxed text-slate-300">
              {run.logs || 'Sin log'}
            </pre>
          </td>
        </tr>
      )}
    </>
  )
}