<p align="center">
  <img src="assets/banner.svg" alt="RockLogic banner" />
</p>

<h1 align="center">⛏️ RockLogic</h1>

<p align="center">
  <b>Gestión inteligente de operaciones mineras</b><br/>
  Plataforma completa para cargar datos de mina, validarlos, cruzarlos contra el catálogo de equipos y generar reportes profesionales — desde la nube o desde el escritorio.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 25"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=white" alt="React"/>
  <img src="https://img.shields.io/badge/JavaFX-25-007396?style=for-the-badge&logo=java&logoColor=white" alt="JavaFX"/>
  <img src="https://img.shields.io/badge/Bootstrap-Tailwind-38BDF8?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind"/>
</p>

---

## 🏭 ¿Qué es RockLogic?

**RockLogic** es un MVP de gestión de operaciones mineras con tres frentes conectados al mismo corazón:

| Interfaz | Tecnología | Descripción |
|---|---|---|
| 🌐 **Web** | React + Vite + Tailwind | Dashboard, carga de archivos, historial y gestión de usuarios |
| 🖥️ **Escritorio** | JavaFX | Cliente de escritorio conectado a la misma API |
| ⚙️ **API** | Spring Boot | Motor de validación, cruce de catálogo y reportes |

Cada empresa minera ve **solo sus propios datos** (multitenant) y cada operador trabaja con roles y permisos definidos.

---

## ✨ Funcionalidades estrella

- 📥 **Carga de datos** desde Excel (.xlsx) o CSV con validación campo a campo.
- 🔄 **Validación y transformación**: fechas, turnos, rangos de tonelaje y ley de cobre.
- 🗂️ **Cruce contra catálogo**: los equipos que no existen en tu inventario se marcan automáticamente.
- 📊 **KPIs por zona y turno**: total de tonelaje, ley promedio y agrupaciones Norte/Sur/Este/Oeste/Centro.
- 📄 **Reportes profesionales** en Excel y PDF descargables.
- 🕓 **Historial completo** con log de cada corrida y cada fila procesada.
- 📧 **Notificaciones por email** opcionales con el resumen del proceso.
- 👥 **Multi-usuario y multi-tenant** con roles ADMIN / OPERATOR.

---

## 🚀 Arranque rápido

> Requisitos: JDK 25 (Temurin), Node.js 24+, Maven 3.9.

### 1. Backend — API en `:8080`
```batch
scripts\run-backend.bat
```

### 2. Frontend — Web en `:5173`
```batch
scripts\run-frontend.bat
```
Abrir [http://localhost:5173](http://localhost:5173)

### 3. Escritorio
```batch
scripts\run-desktop.bat
```

### Sin scripts (modo desarrollador)
```bash
# API (desde backend/)
mvn spring-boot:run

# Web (desde frontend/)
npm install && npm run dev

# Escritorio (desde desktop/)
mvn javafx:run
```

---

## 🔑 Cuentas de prueba

| Email | Contraseña | Rol | Empresa |
|---|---|---|---|
| `admin@mineraandina.com` | `admin123` | **ADMIN** | Minera Andina SAC |
| `operador@mineraandina.com` | `oper123` | OPERATOR | Minera Andina SAC |
| `admin@minadelsur.com` | `admin123` | **ADMIN** | Mina del Sur Ltda |

---

## 📂 Formato de archivos

CSV o Excel con las columnas:

```
fecha; turno; zona; equipo; tonelaje; ley_cu; estado
```

Reglas de negocio aplicadas en el cruce:

| Campo | Regla |
|---|---|
| `fecha` | `12/09/2026` o `2026-09-12` |
| `turno` | `A`, `B` o `C` |
| `zona` | Norte, Sur, Este, Oeste, Centro |
| `equipo` | Debe existir en el catálogo de la empresa |
| `tonelaje` | > 0 y < 100000 (acepta coma o punto) |
| `ley_cu` | Entre 0 y 100 |

💡 Las filas inválidas **no rompen el proceso**: se marcan con el motivo y el resto se procesa normalmente.

---

## 🔌 Endpoints principales

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/auth/login` | Autenticación JWT |
| `POST` | `/api/uploads` | Subir CSV/Excel (multipart) |
| `POST` | `/api/uploads/{id}/run` | Ejecutar validación + cruce |
| `GET` | `/api/runs` | Historial de corridas |
| `GET` | `/api/runs/{id}/download/excel` | Reporte Excel |
| `GET` | `/api/runs/{id}/download/pdf` | Reporte PDF |
| `GET` | `/api/dashboard` | KPIs y resumen |
| `GET/POST` | `/api/admin/users` | Usuarios (ADMIN) |
| `GET/POST` | `/api/admin/equipments` | Catálogo de equipos |

---

## 🗂️ Estructura del proyecto

```
los menudos desarrollo/
├── backend/         # API REST Spring Boot (Java 25)
│   └── src/main/java/com/minera/mvp/
│       ├── controller/   # Endpoints REST
│       ├── service/      # Procesos, reportes, validación, seed
│       ├── security/     # JWT y configuración
│       ├── repo/         # Acceso a datos (H2)
│       └── model/        # Entidades
├── frontend/        # Web React + Vite + Tailwind
│   └── src/pages/   # Login, Dashboard, Upload, History, Users
├── desktop/         # Cliente JavaFX
│   └── src/main/java/com/minera/mvp/desktop/
└── scripts/         # run-backend / run-frontend / run-desktop
```

---

<p align="center">
  <sub>Hecho con ⛏️ por el equipo de Los Menudos Desarrollo · MVP RockLogic</sub>
</p>