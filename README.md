# MineOps — MVP de gestión de operaciones mineras

Sistema local de carga y procesamiento de datos de operación, con reportes. Incluye:

- **backend/** — API REST Spring Boot (Java 25), puerto **8080**
- **frontend/** — Aplicación web React + Vite + Tailwind, puerto **5173**
- **desktop/** — Cliente de escritorio JavaFX conectado a la misma API
- **sample-data/** — Archivos de ejemplo (CSV y Excel) para probar

## Cómo levantar todo (en orden)

1. `scripts\run-backend.bat` — inicia la API en segundo plano (se ve minimizado).
2. `scripts\run-frontend.bat` — inicia la web. Abrir **http://localhost:5173**.
3. `scripts\run-desktop.bat` — abre la app de escritorio.

Requisitos: JDK 25 (Temurin) en `C:\Program Files\Eclipse Adoptium`, Node.js 24+,
Maven 3.9.16 en `C:\Users\Usuario\dev-tools\apache-maven-3.9.16`.

## Cuentas de prueba

| Email | Contraseña | Rol | Empresa |
|---|---|---|---|
| admin@mineraandina.com | admin123 | ADMIN | Minera Andina SAC |
| operador@mineraandina.com | oper123 | OPERATOR | Minera Andina SAC |
| admin@minadelsur.com | admin123 | ADMIN | Mina del Sur Ltda |

Cada empresa solo ve sus propios datos (multi-tenant).

## Formato de archivos

Columnas esperadas (CSV o Excel):

```
fecha; turno; zona; equipo; tonelaje; ley_cu; estado
```

Reglas:
- fecha: `12/09/2026` o `2026-09-12`
- turno: `A`, `B` o `C`
- zona: texto libre (Norte, Sur, Este, Oeste, Centro)
- equipo: debe existir en el catálogo de la empresa
- tonelaje: > 0 y < 100000 (acepta coma o punto decimal)
- ley_cu: entre 0 y 100

Las filas que no cumplen no rompen el proceso: se marcan como **inválidas** con el
motivo y el resto se procesa normalmente. El sistema cruza el equipo contra el
catálogo, agrupa por zona (Norte/Sur/Este/Oeste/Centro) y guarda KPIs por turno.

## Qué hace cada corrida

- Valida, transforma y cruza los datos (reglas + catálogo).
- Calcula tonelaje total, ley promedio y KPIs por zona/turno.
- Genera **reporte Excel** (.xlsx) y **PDF** descargables.
- Guarda el historial con log completo de cada fila.
- Opcional: envía correo de resumen (configurar SMTP en `backend/src/main/resources/application.properties`).

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | /api/auth/login | Login con email y contraseña |
| POST | /api/uploads | Subir CSV/Excel (multipart `file`) |
| POST | /api/uploads/{id}/run | Ejecutar proceso de validación/cruce |
| GET | /api/runs | Historial de corridas |
| GET | /api/runs/{id}/download/excel | Descargar Excel |
| GET | /api/runs/{id}/download/pdf | Descargar PDF |
| GET | /api/dashboard | KPIs y resumen |
| GET/POST | /api/admin/users | Usuarios (solo ADMIN) |
| GET/POST | /api/admin/equipments | Catálogo de equipos |