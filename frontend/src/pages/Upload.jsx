import { useRef, useState } from 'react'
import {
  FileSpreadsheet,
  FileUp,
  Loader2,
  CheckCircle2,
  XCircle,
  Download,
  Rocket,
  Table2,
  CircleAlert,
} from 'lucide-react'
import api from '../api'
import { Badge } from './Dashboard'

const EXPECTED = ['fecha', 'turno', 'zona', 'equipo', 'tonelaje', 'ley_cu', 'estado']

export default function Upload() {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const [upload, setUpload] = useState(null)
  const [run, setRun] = useState(null)
  const [fileName, setFileName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleFile = async (file) => {
    if (!file) return
    setError('')
    setRun(null)
    setLoading(true)
    setFileName(file.name)
    try {
      const form = new FormData()
      form.append('file', file)
      const { data } = await api.post('/uploads', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setUpload(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Error al subir el archivo')
    } finally {
      setLoading(false)
    }
  }

  const process = async () => {
    setError('')
    setLoading(true)
    try {
      const { data } = await api.post(`/uploads/${upload.id}/run`)
      setRun(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Error al procesar el archivo')
    } finally {
      setLoading(false)
    }
  }

  const download = async (type) => {
    const res = await api.get(`/runs/${run.id}/download/${type}`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = type === 'excel' ? run.excelFileName : run.pdfFileName
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Cargar datos de operación</h1>
        <p className="mt-1 text-sm text-slate-500">
          Sube un Excel (.xlsx) o CSV. El sistema valida, transforma y cruza con el catálogo de
          equipos automáticamente.
        </p>
      </div>

      <div
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          handleFile(e.dataTransfer.files[0])
        }}
        onClick={() => inputRef.current?.click()}
        className={`flex cursor-pointer flex-col items-center justify-center gap-4 rounded-2xl border-2 border-dashed p-12 text-center transition-colors ${
          dragging
            ? 'border-amber-500 bg-amber-50'
            : 'border-slate-300 bg-white hover:border-amber-400 hover:bg-amber-50/40'
        }`}
      >
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-500/15">
          {loading ? (
            <Loader2 size={30} className="animate-spin text-amber-600" />
          ) : (
            <FileSpreadsheet size={30} className="text-amber-600" />
          )}
        </div>
        <div>
          <p className="text-lg font-bold text-slate-900">
            {loading ? 'Subiendo archivo...' : 'Arrastra tu archivo aquí'}
          </p>
          <p className="mt-1 text-sm text-slate-500">
            o haz clic para seleccionarlo — CSV o Excel (.xlsx / .xls)
          </p>
        </div>
        <input
          ref={inputRef}
          type="file"
          hidden
          accept=".csv,.xlsx,.xls"
          onChange={(e) => handleFile(e.target.files[0])}
        />
      </div>

      <div className="rounded-2xl bg-slate-900 p-5 text-slate-300">
        <div className="mb-3 flex items-center gap-2 text-sm font-bold text-white">
          <Table2 size={16} className="text-amber-500" />
          Formato esperado
        </div>
        <div className="flex flex-wrap gap-2">
          {EXPECTED.map((c) => (
            <code
              key={c}
              className="rounded-lg bg-slate-800 px-2.5 py-1 text-xs font-semibold text-amber-400"
            >
              {c}
            </code>
          ))}
        </div>
        <p className="mt-3 flex items-start gap-2 text-xs text-slate-400">
          <CircleAlert size={14} className="mt-0.5 shrink-0" />
          El equipo debe existir en el catálogo (Administración → Usuarios y equipos). Ejemplos de
          fecha: 12/09/2026 · Turno A/B/C · tonelaje con punto o coma decimal.
        </p>
      </div>

      {error && <div className="rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</div>}

      {upload && (
        <div className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Archivo subido</p>
              <p className="text-lg font-bold text-slate-900">{upload.originalName}</p>
              <p className="mt-0.5 text-sm text-slate-500">
                {(upload.sizeBytes / 1024).toFixed(1)} KB ·{' '}
                {new Date(upload.createdAt).toLocaleString('es-AR')}
              </p>
            </div>
            <Badge status={upload.status} />
          </div>

          {!run ? (
            <button
              onClick={process}
              disabled={loading}
              className="mt-6 inline-flex items-center gap-2 rounded-lg bg-amber-500 px-5 py-2.5 text-sm font-bold text-slate-900 transition hover:bg-amber-400 disabled:opacity-60"
            >
              {loading ? <Loader2 size={16} className="animate-spin" /> : <Rocket size={16} />}
              {loading ? 'Procesando...' : 'Ejecutar proceso (validar + transformar + cruzar)'}
            </button>
          ) : (
            <ResultCard run={run} onDownload={download} />
          )}
        </div>
      )}
    </div>
  )
}

function ResultCard({ run, onDownload }) {
  if (run.status === 'ERROR') {
    return (
      <div className="mt-5 flex items-center gap-3 rounded-xl bg-red-50 p-4 text-red-700">
        <XCircle size={20} />
        <div>
          <p className="font-semibold">El proceso falló</p>
          <p className="text-sm text-red-600">{run.logs?.split('\n').at(-1)}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mt-5 space-y-5">
      <div className="flex items-center gap-3">
        <CheckCircle2 size={20} className="text-emerald-600" />
        <p className="font-semibold text-emerald-700">
          Proceso completado en {(run.durationMs / 1000).toFixed(1)} s
        </p>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        {[
          { label: 'Filas totales', value: run.totalRows },
          { label: 'Válidas', value: run.validRows },
          { label: 'Inválidas', value: run.invalidRows },
          { label: 'Tonelaje (t)', value: run.totalTonnage },
          { label: 'Ley media Cu (%)', value: run.avgGrade },
          { label: 'Duración', value: `${(run.durationMs / 1000).toFixed(1)} s` },
        ].map((s) => (
          <div key={s.label} className="rounded-xl bg-slate-50 p-4">
            <p className="text-xl font-extrabold text-slate-900">{s.value}</p>
            <p className="text-xs font-medium text-slate-500">{s.label}</p>
          </div>
        ))}
      </div>

      <div className="flex flex-wrap gap-3">
        <button
          onClick={() => onDownload('excel')}
          className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-emerald-500"
        >
          <Download size={15} /> Excel detallado
        </button>
        <button
          onClick={() => onDownload('pdf')}
          className="inline-flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-red-500"
        >
          <Download size={15} /> Reporte PDF
        </button>
      </div>

      {run.logs && (
        <details className="rounded-xl bg-slate-50 p-4">
          <summary className="cursor-pointer text-sm font-semibold text-slate-700">
            Ver log de ejecución
          </summary>
          <pre className="mt-3 whitespace-pre-wrap font-mono text-xs leading-relaxed text-slate-600">
            {run.logs}
          </pre>
        </details>
      )}
    </div>
  )
}