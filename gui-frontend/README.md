# GUI frontend

Angular 13.3.12, TypeScript 4.6.4 y PrimeNG. Abrir **esta carpeta** en VS Code.

## Iniciar

```powershell
npm start
```

Abrir **http://localhost:4200/**. Angular recompila al guardar. La API debe estar iniciada desde IntelliJ en 8080; `proxy.conf.json` redirige `/api` al backend.

F5 con **GUI Angular** ejecuta previamente **GUI Angular: iniciar** y abre Edge. Si ya existe un servidor en 4200, detenerlo antes de iniciar otra tarea.

## Compilar y probar

```powershell
npm run build
npm test
```

El build genera **dist/gui** y no modifica el backend. También se puede usar **Ctrl+Shift+B** o **GUI Angular: compilar**. **GUI Angular: pruebas** ejecuta las pruebas.

Los ajustes de terminal y tareas anteponen `../.runtime/node16` al PATH para usar Node 16/npm 8. Abrir una terminal nueva después de cargar la carpeta. Para reinstalar dependencias, usar `npm ci`; mantener `package-lock.json`.

Las fuentes están en `src/app`. `dist`, `.angular` y `node_modules` son generados. Las tipografías se sirven localmente desde Angular. El WAR Java no incluye esta aplicación.

## Exportar a Excel

El botón Exportar a Excel descarga un XLSX real con ExcelJS, generado localmente en el navegador. Exporta exclusivamente las columnas visibles y las filas filtradas de la página actual; no solicita páginas adicionales ni incluye Acciones. Los códigos, series, RUC y teléfonos se escriben como texto. Las cantidades y conteos son enteros; las fechas son celdas de fecha y los nulos se dejan vacíos. La auditoría conserva la hora UTC. Los identificadores textuales largos se conservan como texto para evitar pérdida de precisión de Excel. El botón se desactiva si no hay resultados o mientras se genera el archivo.


## Buscador de productos por acción

El componente de selección comparte búsqueda por producto, código, marca o serie y filtros por tipo y ubicación. La impresión permite varias unidades; consulta, edición de nombre y mantenimiento eligen un solo activo físico por código. Movimientos admite hasta 100 unidades por lote, acumuladas por búsqueda o QR sin duplicados. El registro de bienes usa referencias de modelo del catálogo del tipo elegido y conserva cantidad, series, destino y comprobantes pendientes. La selección solo prepara la acción: no guarda operaciones automáticamente. Las validaciones y permisos de las operaciones siguen en los servicios y procedimientos existentes del backend. Los datos se cargan mediante los servicios Angular, respetando el alcance autorizado; el frontend no accede a SQL Server.


