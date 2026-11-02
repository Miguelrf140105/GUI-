# GUI backend

Java 17, Spring Boot 3.3.3 y Maven. Abrir **pom.xml** en IntelliJ, importar Maven y usar JDK 17.

## Compilar

Maven → Lifecycle → **clean**, después **package**. Equivalente desde esta carpeta:

```powershell
.\mvnw.cmd clean package
```

Genera `target/gui-api-0.1.0.war` únicamente con el backend. No lee recursos del frontend. Las pruebas Java se ejecutan al empaquetar.

## Ejecutar

Seleccionar **GUI Local** y pulsar ▶. La API escucha en http://localhost:8080/. El frontend se inicia por separado desde VS Code y se abre en http://localhost:4200/.

`.run/GUI Local.run.xml` incluye Tomcat `provided` y las rutas a `../.runtime` para la autenticación Windows. `application.properties` recibe las variables `GUI_DB_*`.

Las fuentes están en `src/main/java/pe/edu/gui`; las pruebas, en `src/test/java`; el contrato SQL, en `database`. No reinstalar las bases existentes.

Ver [ejecución](../docs/EJECUCION.md) y [SQL Server](../docs/SQLSERVER2014.md).

## Edición del nombre de un activo

La migración database/25-editar-nombre-activo.sql añade un nombre individual opcional y el procedimiento de edición. Requiere permisos 301 y 302 y alcance sobre el activo. Solo acepta name y expectedName; no cambia código, QR, modelo, ubicación ni Kardex. El cambio se audita y detecta nombres modificados por otra sesión. Ejecutar esta migración después de las anteriores en instalaciones nuevas; ya está aplicada en GUI_BP local.


## Movimientos por lote

Aplicar database/26-movimientos-lote.sql después de las migraciones anteriores (ya aplicada en GUI_BP local). POST /api/movements/batch usa permiso 402 y CSRF, entre 1 y 100 assetIds únicos, cabecera común e idempotencyKey. MovimientoService valida las unidades y coordina una sola transacción de Spring; MovimientoDAO reserva la huella del lote mediante SP y reutiliza el procedimiento existente para crear una solicitud y Kardex independientes por activo. Un fallo revierte movimientos, Kardex, auditoría y reserva. Autorizar, despachar y recibir siguen siendo etapas individuales. Las pruebas SQL opcionales MovementBatchSqlTest se habilitan con gui.sql.integration=true y revierten todas sus escrituras.


## Alta automática de ubicaciones

La migración database/27-ubicaciones-codigo-automatico.sql (aplicada localmente) genera códigos UBI únicos con secuencia SQL Server 2014, ignora códigos suministrados para nuevas ubicaciones y conserva el código al editar. Si no se asigna un responsable al crear, hereda el de la ubicación superior. Rechaza nombres duplicados bajo el mismo padre y conserva permisos, alcance, controles de ciclos y auditoría del procedimiento de ubicaciones. Las pruebas LocationAutoSqlTest habilitadas con gui.sql.integration=true revierten todas las altas de prueba.



### Eliminación segura de ubicaciones (migración 28)

Aplicar `database/28-ubicaciones-eliminacion-segura.sql` después de la migración 27.
El permiso **503: Ubicaciones / Eliminar** es independiente de 502 (crear/editar).
Se asigna inicialmente al rol Administrador (recuperación); los otros roles se configuran en Roles y permisos.
El alcance del usuario se valida en SQL. El botón se muestra en todos los niveles del árbol solo con permiso 503.
La baja lógica (`ESTADO_REGISTRO=2`) conserva código, referencias e historial y genera auditoría 503.
Se rechaza cuando existen hijos no eliminados, activos asignados, movimientos pendientes o alcances de usuarios.
No se realiza borrado en cascada. Los registros eliminados dejan de aparecer en los alcances disponibles.
Contrato: `POST /api/locations/{id}/delete` → controlador → servicio transaccional → DAO → `INV.sp_Gestionar`, acción 503.
Reiniciar GUI Local tras compilar el backend para cargar el nuevo endpoint.
`LocationDeleteSqlTest` comprueba los cuatro niveles, permisos, alcance y dependencias dentro de transacciones revertidas.


### Edición y eliminación exclusivas del Administrador (migración 29)

Esta regla sustituye la delegación de bajas descrita en la migración 28. Solo usuarios activos con el rol **Administrador (ID 1)** activo pueden editar o eliminar registros. El nombre de un rol personalizado no concede privilegios administrativos.
El permiso 504 permite a la interfaz distinguir edición de creación sin cambiar los contratos existentes. Los permisos 503, 504, 902 y 1002 no tienen efecto para usuarios sin el rol Administrador, aunque estén asignados a otro rol. La identidad publica solo permisos efectivos.
SQL valida la edición de ubicaciones, productos/modelos, proveedores y nombres individuales; la gestión de usuarios y roles es exclusiva de Administrador. El alcance y los permisos propios de cada módulo siguen siendo obligatorios. La creación y las etapas operativas de movimientos conservan sus permisos específicos.
La migración no modifica ni elimina usuarios, asignaciones, activos o ubicaciones existentes. Los cambios SQL tienen efecto inmediato; recargar Angular actualiza la sesión.


### Listado de Usuarios en SQL Server 2014 (migración 30)

`database/30-opciones-xml-listado-usuarios.sql` recompila la definición vigente de `INV.sp_Listar` con `ANSI_NULLS` y `QUOTED_IDENTIFIER` activos. El agregado XML de nombres de roles requiere esas opciones persistidas; si faltan, el módulo Usuarios devolvía un error de lectura. No modifica consultas ni registros. `UsersListSqlTest` verifica lectura real de usuarios/roles y ausencia del hash de contraseña en la proyección.


### Contadores del dashboard (migración 31)

Movimientos del mes cuenta solicitudes de `INV.MOVIMIENTO` creadas en el mes UTC, dentro del alcance del usuario. Los ingresos iniciales y cada etapa adicional del Kardex no incrementan ese contador. La gráfica «Actividad del inventario» conserva todos los eventos del Kardex y los identifica expresamente, incluidos los ingresos. No se altera el historial. `DashboardMovementSqlTest` verifica el contador, la serie y que autorizar una solicitud no la cuente dos veces; los registros del caso se revierten.

### Borrado lógico de proveedores (migración 32)

Aplicar `database/32-proveedores-borrado-logico.sql` después de las migraciones 29–31. El permiso **1303: Proveedores / Eliminar** es exclusivo del rol Administrador (ID 1), incluso si se asigna por error a otro perfil. Se mantiene la arquitectura controlador → servicio transaccional → DAO → `INV.sp_Gestionar`.

`POST /api/institutions/{id}/delete` cambia únicamente `INV.TERCERO.ESTADO_REGISTRO` a 2 y genera auditoría 1303. Conserva la fila, identificación, ingresos, documentos, movimientos y relaciones históricas. El listado normal excluye eliminados; los catálogos para nuevas operaciones solo admiten activos. La búsqueda exacta por identificación sigue informando del registro eliminado para impedir duplicar su RUC. No puede reactivarse enviando un formulario antiguo ni reutilizarse en nuevos documentos, ingresos o solicitudes. Los permisos exclusivos se muestran bloqueados en Roles y permisos.

No existe borrado físico ni cascada. La confirmación de Angular explica estas reglas y permite cancelar sin escribir. `ProviderDeleteSqlTest` verifica los tres perfiles, CSRF, conservación de vínculos y bloqueos, usando exclusivamente fixtures que se revierten. Reiniciar GUI Local después de compilar para cargar el endpoint.
### Código visible y unidades de ingresos (migración 33)

`database/33-ingresos-codigo-y-unidades.sql` añade `INV.fnCodigoIngreso`: presenta `ID_OPERACION` como ING0001, ING0002… y amplía el ancho cuando supera cuatro cifras. No cambia PK, FK, códigos patrimoniales ni QR. El correlativo no se libera ni se reutiliza al eliminar; puede contener saltos legítimos.

La vista 3 de `INV.sp_IngresoDetalle` y el campo `units` del contrato de detalle muestran los activos individuales y su código original, serie, ubicación actual y estado, dentro del alcance autorizado. `IntakeCodeSqlTest` comprueba códigos largos, unidades y concordancia de contadores mediante lecturas. Esta migración es de lectura; la anulación lógica del ingreso y sus unidades se implementa en las migraciones 34 y 35.
### Anulación lógica integral de ingresos (migraciones 34 y 35)

El permiso **1403** es exclusivo de usuarios activos con el rol Administrador (ID 1), además de consultar ingresos (1401) y tener alcance sobre todas sus unidades. El botón de Ingresos abre la advertencia y la lista completa de productos y códigos antes de confirmar. `POST /api/intakes/{id}/delete` pasa por controlador → servicio transaccional → DAO → `INV.sp_Gestionar`, acción 1403.

Se actualiza **ESTADO_REGISTRO=2 en las mismas filas** de `INV.OPERACION` y sus `INV.ACTIVO`. No se elimina físicamente ninguna fila, no se renumeran IDs ni códigos, no se borran relaciones ni documentos y los modelos compartidos permanecen. Cada unidad recibe un nuevo evento inmutable de Kardex tipo 8 (Anulación de ingreso), conservando todos los eventos anteriores. El tipo 8 es interno y no puede solicitarse como movimiento normal.

Si cualquier unidad tiene un movimiento (incluso pendiente o cancelado), eventos posteriores, mantenimiento o estado incompatible, se rechaza toda la operación con nombre y código del activo. SQL vuelve a comprobarlo bajo bloqueos al confirmar; ninguna unidad se anula parcialmente. También se bloquean unidades pendientes de registrar, falta de alcance, doble anulación y reintentos de registro de una recepción anulada. Las vistas vigentes, reportes y contadores excluyen las filas en estado 2; la consulta de historial y detalle conserva su acceso autorizado.

`CODIGO_INGRESO` conserva el código de negocio ING0001, ING0002… separado de `ID_OPERACION`. `INV.CORRELATIVO_INGRESO` asigna el siguiente código dentro de la transacción. No se libera al anular y no depende de los saltos del IDENTITY. No se implementa reactivación automática ni reutilización de códigos.

`IntakeCancelSqlTest` usa recepciones y unidades desechables dentro de transacciones revertidas y compara huellas de las tablas antes/después. Comprueba perfiles, CSRF, preservación de documentos/IDs/QR/Kardex, bloqueo por actividad posterior, fallo tras escrituras, reintentos y correlativo sin reutilización. No se anulan ingresos reales durante las pruebas.
## Egresos definitivos

Aplicar la migración 36 después de 25–35. El módulo 150 consulta salidas confirmadas y conserva el movimiento y Kardex individual. Ver [guía de Egresos](../docs/EGRESOS.md).

## Fotos de productos

Migración 37 posterior a 36. WebP principal y miniatura en GUI_BF, con administración exclusiva del permiso 303. Ver [fotos de productos](../docs/FOTOS-PRODUCTOS.md).
