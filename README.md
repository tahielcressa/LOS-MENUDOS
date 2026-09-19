# MineOps — Sistema de gestión de operaciones mineras

Sistema local tipo **MVP** para **cargar, validar, transformar y cruzar datos de operación minera** (provenientes de planillas Excel o CSV), generar **KPIs**, guardar el **historial** de cada corrida y producir **reportes descargables** (Excel y PDF). Incluye una visa de **dashboard**, administración de **usuarios y catálogo de equipos** por empresa, y una **app de escritorio**.

> Está desarrollado para una **empresa o varias empresas a la vez** (multi-tenant): cada empresa solo ve y procesa sus propios datos.

---

## Tecnologías y estructura del proyecto

```
LOS-MENUDOS/
├── backend/      API REST Spring Boot (Java 25) — puerto 8080
├── frontend/     Web React + Vite + Tailwind — puerto 5173
├── desktop/      Cliente de escritorio JavaFX (usa la misma API)
├── sample-data/  Archivos de ejemplo (CSV y Excel)
└── scripts/      Scripts .bat para arrancar todo en Windows
```

| Componente | Tecnología | Puerto |
|---|---|---|
| **backend** | Spring Boot 3.5.16 · Java 25 · Spring Security (JWT) · Spring Data JPA · H2 (archivo) · Apache POI (Excel) · OpenPDF (PDF) · Spring Mail | `8080` |
| **frontend** | React 18 · Vite · Tailwind · Recharts (gráficos) · axios | `5173` (proxy a 8080) |
| **desktop** | JavaFX (cliente liviano conectado a la API) | — |

- La base de datos H2 se guarda en `backend/db/minera.mv.db` (persistente entre reinicios).
- Los archivos subidos quedan en `outputs/uploads/` y los reportes en `outputs/runs/{id}/`.
- Al primer arranque, el backend **crea empresas, usuarios y el catálogo de equipos** de demostración y **genera los archivos de ejemplo** en `sample-data/`.

---

## Cómo levantar todo (en orden)

Requisitos:

- **JDK 25 (Temurin)** en `C:\Program Files\Eclipse Adoptium`
- **Node.js 24+**
- **Maven 3.9.16** en `C:\Users\Usuario\dev-tools\apache-maven-3.9.16`

Pasos:

1. `scripts\run-backend.bat` — inicia la API en segundo plano (se abre minimizada).
2. `scripts\run-frontend.bat` — inicia la web. Abrir **http://localhost:5173**.
3. `scripts\run-desktop.bat` — abre la app de escritorio.

> Nota: el backend también puede iniciarse con `mvn spring-boot:run` dentro de `backend/`.

---

## Cuentas de prueba

| Email | Contraseña | Rol | Empresa |
|---|---|---|---|
| admin@mineraandina.com | admin123 | ADMIN | Minera Andina SAC |
| operador@mineraandina.com | oper123 | OPERATOR | Minera Andina SAC |
| admin@minadelsur.com | admin123 | ADMIN | Mina del Sur Ltda |

Roles disponibles:

- **ADMIN**: acceso total, incluye la sección *Usuarios* (crear usuarios, ver/crear equipos del catálogo).
- **OPERATOR**: puede cargar archivos, ejecutar procesos, ver dashboard e historial.

---

## Explicación de cada función / módulo

### 1. Autenticación y seguridad (JWT)
- `POST /api/auth/login` valida email + contraseña (BCrypt) y devuelve un **token JWT** de 24 h.
- `POST /api/auth/register` permite **crear una nueva empresa** (código de empresa + nombre + país) y su primer usuario ADMIN.
- Todos los endpoints salvo `/api/auth/**` exigen token (`Authorization: Bearer <token>`).
- El token incluye `companyId` y `role`: cada petición opera **siempre sobre la empresa del usuario** (no se puede leer ni procesar datos de otra empresa).

### 2. Carga de archivos (Excel / CSV)
Ver sección [Carga de datos (Excel/CSV)](#carga-de-datos-excelcsv) más abajo.

- `POST /api/uploads` recibe el archivo, lo guarda en disco (`outputs/uploads/`) y registra la subida con estado **RECIBIDO**.
- `GET /api/uploads` lista las subidas de la empresa.

### 3. Proceso: validar + transformar + cruzar + KPI
`POST /api/uploads/{id}/run` orquesta el flujo completo:

1. **Leer el archivo** (`FileParserService`) → respeta CSV con `;` o `,` (auto-detectado, respetando comillas) y Excel `.xlsx`/`.xls` (primera hoja). Usa la primera fila como encabezados.
2. **Validar y transformar cada fila** (`ValidationAndTransformService`) → acepta múltiples **alias de columna**, normaliza valores y aplica las reglas de negocio (ver abajo).
3. **Cruzar con el catálogo de equipos** de la empresa → si el código de equipo no existe, la fila queda inválida con el motivo.
4. **Calcular KPIs** (`ProcessService`): filas válidas/inválidas, tonelaje total, ley media de cobre, tonelaje por zona, por turno y filas por estado.
5. **Guardar historial**: cada fila validada se persiste en `result_rows` con su resultado y errores (`ResultRow`). La corrida (`ProcessRun`) queda en estado **OK** o **ERROR** con su log completo.
6. **Generar reportes** `Excel` y `PDF` en `outputs/runs/{id}/`.
7. **Notificar por email** (opcional, ver módulo 9).
8. Actualizar la subida a estado **PROCESADO** o **ERROR**.

> El proceso es **tolerante a errores**: una fila que no cumple las reglas **no detiene el proceso**; se marca como inválida con el motivo y el resto se procesa normalmente.

### 4. Dashboard
`GET /api/dashboard` resume para la empresa:

- Tarjetas: procesos ejecutados, filas válidas, filas inválidas, tonelaje total, ley media de cobre.
- Gráfico de **tonelaje por zona** y de **filas por estado** (EN_PROCESO / DETENIDO).
- Tabla de **últimos 5 procesos** con acceso al historial completo.
- (En la web se calcula sobre las últimas 500 filas; en el procesamiento se calcula sobre la corrida completa.)

### 5. Historial de corridas y descarga de reportes
- `GET /api/runs` — histórico completo de la empresa (con KPIs, log y nombres de archivo).
- `GET /api/runs/{id}` — detalle de una corrida.
- `GET /api/runs/{id}/download/excel|pdf` — descarga los reportes generados.

### 6. Administración (solo ADMIN)
- `GET /api/admin/company` — datos de la empresa (código, nombre, país) y cantidad de usuarios/equipos.
- `GET|POST /api/admin/users` — listar y crear usuarios (ADMIN u OPERATOR).
- `GET|POST /api/admin/equipments` — listar y crear equipos del catálogo (código, nombre, área, tipo).

> El **catálogo de equipos** es crítico: es la fuente contra la que se cruzan las filas al validar (`camión`, `cargador`, `perforadora`, etc.).

### 7. Reportes exportables
- **Excel** (`.xlsx`): hoja *Resultados* con el detalle fila por fila (Fila, Fecha, Turno, Zona, Equipo, Nombre, Área, Tipo, Estado, Tonelaje, LeyCu, Validación, Errores) + hoja *KPIs* con el resumen.
- **PDF**: resumen ejecutivo con tarjetas de KPIs y tabla de las filas válidas.

### 8. Notificación por email
- `MailService` envía al usuario ejecutor un correo con el resumen y los reportes adjuntos.
- Si `app.mail.enabled=false` (configuración por defecto) **no se envía nada real**: el "correo" se muestra en la consola del backend como simulado. Para activar SMTP real se completan `spring.mail.*` en `backend/src/main/resources/application.properties`.

### 9. Datos de demostración (seed)
- Al arrancar sin datos, crea 2 empresas, 3 usuarios y un catálogo de equipos (Perú y Chile).
- Genera `sample-data/muestra_operacion.csv` y `sample-data/muestra_operacion.xlsx` si no existen.

### 10. Cliente de escritorio (JavaFX)
App standalone con login, dashboard, carga de archivos y descarga de reportes directo a `~/Downloads/MineOps_reportes`. Incluye un botón para comprobar si el servidor está arriba.

---

## Carga de datos (Excel/CSV)

Para que una carga funcione, **el archivo debe cumplir lo siguiente**:

### Formato
- Extensiones aceptadas: **`.csv`**, **`.xlsx`** y **`.xls`**.
- Tamaño máximo: **25 MB por archivo** (solicitud completa 30 MB).
- En Excel se lee la **primera hoja**; la primera fila se toma como **encabezados**.
- En CSV el separador puede ser `;` o `,` (se **detecta automáticamente** según la primera línea y respeta comillas dobles `"` para valores con separador interno).
- Las filas completamente vacías se ignoran.

### Columnas esperadas

```
fecha; turno; zona; equipo; tonelaje; ley_cu; estado
```

El sistema acepta **sinónimos** de nombre de columna (da igual mayúsculas/minúsculas, guiones bajos o espacios — `ley_cu`, `ley cu`, `LEY_CU` son lo mismo):

| Campo | Sinónimos aceptados |
|---|---|
| fecha | `fecha`, `date`, `fecha_oper`, `dia` |
| turno | `turno`, `shift` |
| zona | `zona`, `area`, `sector`, `block` |
| equipo | `equipo`, `equipment`, `equipo_codigo`, `codigo_equipo`, `maquina` |
| tonelaje | `tonelaje`, `ton`, `tonnage`, `tms`, `peso` |
| ley_cu | `ley_cu`, `ley`, `ley_cobre`, `cu`, `grade`, `grado` |
| estado | `estado`, `status`, `condicion` |

### Valores y normalización automática

| Campo | Valores aceptados | Se guarda como |
|---|---|---|
| **fecha** | `12/09/2026`, `2026-09-12`, `12-09-2026`, `2026/09/12`, `12.09.2026`, `d/M/yyyy` | `LocalDate` |
| **turno** | `A`, `1`, `DIA`, `MAÑANA`, `MANANA`, `TURNO A` | `A` |
| | `B`, `2`, `TARDE`, `TURNO B` | `B` |
| | `C`, `3`, `NOCHE`, `TURNO C` | `C` |
| **estado** | `OK`, `OPERATIVO`, `OPERANDO`, `PRODUCCION`, `EN_PROCESO`, `EN PROCESO`, `ACTIVO` | `EN_PROCESO` |
| | `DETENIDO`, `DETENIDA`, `MANTENCION`, `MANTENIMIENTO`, `INACTIVO`, `STOP` | `DETENIDO` |
| **tonelaje** | Acepta coma o punto decimal, separadores de miles y espacios: `1234,5`, `1.234,56`, `1234.56`, `1 234,5` | `BigDecimal` |
| **ley_cu** | Ídem tonelaje, con `,` o `.` como decimal | `BigDecimal` |
| **zona / equipo** | Texto libre (se pasa a mayúsculas y se recorta) | `String` |

### Reglas de validación (qué debe cumplir cada fila)

| Campo | Regla |
|---|---|
| fecha | Debe parsearse con alguno de los formatos soportados |
| turno | Debe ser `A`, `B` o `C` (o sus sinónimos) |
| zona | No debe estar vacía |
| estado | Debe ser operativo/en proceso o detenido (o sus sinónimos) |
| tonelaje | **Mayor a 0 y menor a 100000** |
| ley_cu | **Entre 0 y 100** (inclusive) |
| equipo | **Debe existir en el catálogo de la empresa** (código exacto, sin importar mayúsculas) |

### Qué pasa con las filas que no cumplen

No rompen el proceso. Cada fila inválida se guarda con todos sus **motivos de error** y el resto se procesa; en la subida se resumen los **primeros 20 errores distintos**. Ejemplo de errores reales del sistema:

- `Fecha invalida '14/09/2026X'`
- `Turno invalido (usar A, B o C)`
- `Equipo 'ZZ-999' no existe en el catalogo de la empresa`
- `Tonelaje invalido '0' (mayor a 0 y menor a 100000)`

### Archivos de ejemplo

Podés probar con `sample-data/muestra_operacion.csv` o `sample-data/muestra_operacion.xlsx`: contienen filas válidas **y** filas inválidas a propósito (turno `X`, equipo inexistente, tonelaje `0`) para ver cómo el sistema separa lo válido de lo inválido.

---

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Login con email y contraseña (devuelve JWT) |
| POST | `/api/auth/register` | Crear empresa + usuario ADMIN |
| POST | `/api/uploads` | Subir CSV/Excel (multipart `file`) |
| GET | `/api/uploads` | Listar subidas de la empresa |
| POST | `/api/uploads/{id}/run` | Ejecutar validación + transformación + cruce |
| GET | `/api/runs` | Historial de corridas |
| GET | `/api/runs/{id}` | Detalle de una corrida |
| GET | `/api/runs/{id}/download/excel` | Descargar Excel |
| GET | `/api/runs/{id}/download/pdf` | Descargar PDF |
| GET | `/api/dashboard` | KPIs y resumen |
| GET | `/api/admin/company` | Datos de la empresa (solo ADMIN) |
| GET/POST | `/api/admin/users` | Usuarios (solo ADMIN) |
| GET/POST | `/api/admin/equipments` | Catálogo de equipos (solo ADMIN) |

---

## Configuración relevante

Archivo: `backend/src/main/resources/application.properties`

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto de la API |
| `spring.datasource.url` | `jdbc:h2:file:./db/minera` | Base H2 persistente |
| `spring.servlet.multipart.max-file-size` | `25MB` | Tamaño máximo de archivo subido |
| `app.storage.output` | `./outputs` | Carpeta de subidas y reportes |
| `app.storage.samples` | `../sample-data` | Carpeta de archivos de ejemplo |
| `app.mail.enabled` | `false` | `false` = correo simulado en consola |
| `spring.mail.host/port/username/password` | — | SMTP (para activar el correo real) |
| `app.jwt.secret` | clave de 48 bytes | Secreto de firma del token |
| `app.jwt.expiration-hours` | `24` | Vigencia del token |

> ⚠️ Los datos reales vienen de **planillas operativas**; este MVP está pensado para un ambiente local de prueba. Para producción conviene reemplazar los valores por defecto (ver *Mejoras*).

---

## Cómo se puede mejorar

- **Vista previa antes de procesar**: mostrar en pantalla las filas inválidas y permitir corregir el archivo antes de lanzar la corrida (hoy el resultado se ve recién al final + log).
- **Procesamiento asíncrono**: para archivos grandes, ejecutar las corridas en segundo plano (cola/tareas) con estado en vivo, en vez de bloquear la petición.
- **Paginación**: en histórico, subidas y filas detalle (hoy el dashboard limita a 500 filas y la web no pagina).
- **Plantilla descargable** del formato esperado (con validación asistida y hojas de ejemplo).
- **Detección de duplicados**: impedir volver a procesar el mismo archivo (hash) y consolidar corridas.
- **Metales y unidades múltiples** (Cu, Au, Mo; toneladas métricas/SPT), zonas y turnos configurados por empresa en vez de fijos.
- **Mejores tests**: unitarios (parser, validación, cálculo de KPIs) y de integración (End-to-End del flujo de carga).
- **Seguridad**: mover el secreto JWT y credenciales SMTP a variables de entorno, bloquear intentos de login fallidos, y no commitear claves.
- **PostgreSQL/MySQL**: migrar fuera de H2 para producción real (concurrencia, backups, usuarios).
- **Auditoría**: hash/MD5 de los archivos originales y trazabilidad total de cada corrida.
- **Notificaciones in-app** además del correo, y reportes programados (diario/semanal).

---

## Qué se puede incorporar a futuro

- **Despliegue en la nube**: contenedores Docker, `docker-compose`, CI/CD y base de datos gestionada.
- **Autenticación avanzada**: OAuth2 / SSO / 2FA, recuperación de contraseña.
- **Dashboard avanzado**: series temporales (producción diaria, evolución de ley), filtros por fecha/zona/equipo, comparativas entre turnos.
- **Integración con telemetría/equipos reales** (IoT): que la carga deje de ser manual y llegue automáticamente desde los equipos.
- **API pública documentada** (OpenAPI/Swagger) para otros sistemas (ESB, contabilidad, ERP minero).
- **Mapas**: geolocalización de zonas y equipos sobre el terreno.
- **Productividad y costos**: toneladas por hora, disponibilidad/utilización de equipos, costos unitarios.
- **Gestión de mantenimiento**: planificación de detenciones, stock de insumos y órdenes de trabajo vinculadas al estado de los equipos.
- **Predicción con machine learning**: estimación de producción y ley a partir del histórico.
- **Multilingüe y multimoneda**, permisos granulares (supervisor/planta), y app móvil.