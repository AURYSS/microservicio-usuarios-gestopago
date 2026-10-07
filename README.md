# Microservicio de Usuarios y Onboarding de Clientes

Este microservicio se encarga de la gestión integral del ciclo de vida de los clientes, desde su onboarding inicial (registro de datos personales, fiscales y domicilio) hasta la creación automática de su cuenta financiera y credenciales de acceso seguro.

## Entregables Cumplidos (Rúbrica)

### 1. Diseño de Base de Datos y Optimizaciones
Se aplicaron las mejores prácticas en el diseño relacional y la elección de tipos de datos para asegurar el uso eficiente de memoria e integridad referencial.

#### Diagrama Entidad-Relación (ERD)
```mermaid
erDiagram
    CLIENTES ||--|| DOMICILIOS : "tiene 1"
    CLIENTES ||--o{ CUENTAS : "tiene 1 o muchas"
    CLIENTES ||--|| USUARIOS : "tiene 1"

    CLIENTES {
        BIGINT id PK
        VARCHAR(50) nombre
        VARCHAR(50) apellido_paterno
        VARCHAR(50) apellido_materno
        DATE fecha_nacimiento
        VARCHAR(18) curp UK
        VARCHAR(13) rfc UK
        VARCHAR(100) correo UK
        VARCHAR(10) telefono_movil
        NUMERIC(15_2) ingreso_mensual
        BOOLEAN activo
    }

    DOMICILIOS {
        BIGINT id PK
        VARCHAR(100) calle
        VARCHAR(20) numero_exterior
        VARCHAR(100) colonia
        VARCHAR(5) codigo_postal
        BIGINT cliente_id FK
    }

    CUENTAS {
        BIGINT id PK
        UUID numero_cuenta UK
        NUMERIC(15_2) saldo
        BOOLEAN activa
        BIGINT cliente_id FK
    }

    USUARIOS {
        BIGINT id PK
        VARCHAR(100) correo UK
        VARCHAR(255) password
        VARCHAR(20) role
        BOOLEAN activo
        BIGINT cliente_id FK
    }
```

#### Modelo de Datos:
* **`clientes`**: Almacena información personal, laboral y de contacto. Uso de `VARCHAR` con límites estrictos (`CURP` a 18, `RFC` a 13) para optimización de almacenamiento. Los campos únicos (`CURP`, `RFC`, `correo`) tienen índices automáticos para agilizar las búsquedas.
* **`cuentas`**: Usa `UUID` para generar identificadores de cuenta matemáticamente únicos y seguros (previniendo enumeración). El saldo se maneja con `Double`/`NUMERIC(15_2)` para operaciones financieras.
* **`usuarios`**: Almacena credenciales de acceso y el Rol (`ADMIN` o `CLIENTE`). La contraseña nunca se guarda en texto plano (cifrada con BCrypt).

### 2. Validaciones Implementadas (Reglas de Negocio)
Se implementó un sistema dual de validación (Nivel Entity/DTO y Nivel Servicio):
- **Validaciones de Formato:** Uso de expresiones regulares (`@Pattern`) para validar el CURP, el RFC, el correo y contraseñas fuertes.
- **Validaciones Transaccionales (Pagos y Recargas):** Al pagar o recargar, el sistema valida que el **monto sea mayor a 0** (previniendo hackeos lógicos con números negativos). También verifica saldo insuficiente al pagar.
- **Validación de Cuentas Inactivas:** Si un cliente se da de baja, el sistema arroja `CuentaInactivaException` e impide más transacciones (403 Forbidden).
- **Baja Lógica:** Se implementó el borrado lógico; en lugar de ejecutar un `DELETE` físico, se inhabilita el Cliente, su Usuario y su Cuenta bancaria en cascada.

### 3. Excepciones Personalizadas
- `ClienteYaRegistradoException` (409 Conflict)
- `CurpDuplicadaException`, `RfcDuplicadoException`, `CorreoDuplicadoException` (409 Conflict)
- `ContrasenaInvalidaException`, Spring Validation (400 Bad Request)
- `CuentaInactivaException` (403 Forbidden)
- `ClienteNoEncontradoException` (404 Not Found)

---

## 🚀 Endpoints de la API REST y Roles de Seguridad

El microservicio expone los endpoints protegidos mediante token JWT. Existen políticas de autorización estrictas:

### Autenticación
* `POST /api/auth/login`: (Abierto) Autentica a un usuario y devuelve el token JWT.

### Clientes (Autoservicio y Gestión)
* `POST /api/clientes`: (Abierto) Registra un nuevo cliente (crea su cuenta y usuario automáticamente).
* `GET /api/me/perfil`: (`CLIENTE`) Extrae del Token JWT la identidad del usuario y retorna sus datos protegidos.
* `POST /api/me/reconocimiento-facial`: (`CLIENTE`) Endpoint de innovación con `MultipartFile` para simulación de verificación de identidad.
* `DELETE /api/clientes/{id}`: (`ADMIN`, `CLIENTE`) Realiza la **baja lógica** del cliente y desactiva su usuario y cuentas.

### Consultas Administrativas (Solo `ADMIN`)
* `GET /api/clientes`: Lista todos los clientes.
* `GET /api/clientes/activos`: Lista solo clientes dados de alta.
* `GET /api/clientes/correo/{correo}` / `rfc/{rfc}`: Búsquedas indexadas específicas.

### Transacciones Financieras (Cuentas)
* `POST /api/cuentas/{numero}/recargar`: Abona dinero validando montos > 0 y estatus de cuenta.
* `POST /api/cuentas/{numero}/pagar`: Resta dinero validando montos > 0, fondos suficientes y estatus.
* `GET /api/cuentas/{numero}/historial`: Consulta el historial financiero.

---

## 💾 Script de Base de Datos (DDL)
Aunque Spring Boot (Hibernate) genera automáticamente las tablas gracias a la propiedad `ddl-auto=update`, aquí se adjunta el script DDL equivalente para la creación manual de la base de datos optimizada:

```sql
CREATE DATABASE gestopago_usuarios;

\c gestopago_usuarios;

CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    curp VARCHAR(18) UNIQUE NOT NULL,
    rfc VARCHAR(13) UNIQUE NOT NULL,
    sexo VARCHAR(20),
    nacionalidad VARCHAR(50),
    estado_civil VARCHAR(50),
    correo VARCHAR(100) UNIQUE NOT NULL,
    telefono_movil VARCHAR(10) NOT NULL,
    telefono_alternativo VARCHAR(10),
    ocupacion VARCHAR(100),
    empresa VARCHAR(100),
    ingreso_mensual NUMERIC(15,2) CHECK (ingreso_mensual > 0),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE domicilios (
    id BIGSERIAL PRIMARY KEY,
    calle VARCHAR(100) NOT NULL,
    numero_exterior VARCHAR(20) NOT NULL,
    numero_interior VARCHAR(20),
    colonia VARCHAR(100) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    estado VARCHAR(100) NOT NULL,
    codigo_postal VARCHAR(5) NOT NULL,
    pais VARCHAR(50) NOT NULL,
    cliente_id BIGINT UNIQUE REFERENCES clientes(id)
);

CREATE TABLE cuentas (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta UUID UNIQUE NOT NULL,
    saldo NUMERIC(15,2) NOT NULL CHECK (saldo >= 0),
    activa BOOLEAN DEFAULT TRUE,
    cliente_id BIGINT REFERENCES clientes(id)
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    cliente_id BIGINT UNIQUE REFERENCES clientes(id),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimización de consultas frecuentes
CREATE INDEX idx_cliente_curp ON clientes(curp);
CREATE INDEX idx_cliente_rfc ON clientes(rfc);
CREATE INDEX idx_cuenta_numero ON cuentas(numero_cuenta);
```

---

## 🛠️ Instrucciones de Ejecución y Pruebas
1. Ejecutar el proyecto: `./mvnw spring-boot:run`
2. Abrir **Swagger OpenAPI**: `http://localhost:8080/swagger-ui/index.html`
3. Crear un cliente -> Iniciar sesión -> Copiar Token JWT -> Pegarlo en el botón verde **Authorize**.
