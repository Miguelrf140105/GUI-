import { PasswordRecoveryComponent } from './login/password-recovery.component';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AppLayoutComponent } from './layout/app.layout.component';
import { LoginComponent } from './login/login.component';
import { AuthGuard } from './servicios/usuarios/login/auth.guard';
import { QrRouteComponent } from './compartido/qr-route/qr-route.component';
const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'recuperar-contrasena', component: PasswordRecoveryComponent },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./modulos/dashboard/dashboard.module').then((module) => module.DashboardModule),
      },
      {
        path: 'inventario',
        loadChildren: () =>
          import('./modulos/inventario/inventario.module').then(
            (module) => module.InventarioModule
          ),
      },
      {
        path: 'movimientos',
        loadChildren: () =>
          import('./modulos/movimientos/movimientos.module').then(
            (module) => module.MovimientosModule
          ),
      },
      {
        path: 'ubicaciones',
        loadChildren: () =>
          import('./modulos/ubicaciones/ubicaciones.module').then(
            (module) => module.UbicacionesModule
          ),
      },
      {
        path: 'mantenimiento',
        loadChildren: () =>
          import('./modulos/mantenimiento/mantenimiento.module').then(
            (module) => module.MantenimientoModule
          ),
      },
      {
        path: 'proveedores',
        loadChildren: () =>
          import('./modulos/proveedores/proveedores.module').then(
            (module) => module.ProveedoresModule
          ),
      },
      {
        path: 'ingresos',
        loadChildren: () =>
          import('./modulos/ingresos/ingresos.module').then((module) => module.IngresosModule),
      },
      {
        path: 'egresos',
        loadChildren: () => import('./modulos/egresos/egresos.module').then((m) => m.EgresosModule),
      },
      {
        path: 'documentos',
        loadChildren: () =>
          import('./modulos/documentos/documentos.module').then(
            (module) => module.DocumentosModule
          ),
      },
      {
        path: 'reportes',
        loadChildren: () =>
          import('./modulos/reportes/reportes.module').then((module) => module.ReportesModule),
      },
      {
        path: 'usuarios',
        loadChildren: () =>
          import('./modulos/usuarios/usuarios.module').then((module) => module.UsuariosModule),
      },
      {
        path: 'roles',
        loadChildren: () =>
          import('./modulos/roles/roles.module').then((module) => module.RolesModule),
      },
      {
        path: 'configuracion',
        loadChildren: () =>
          import('./modulos/configuracion/configuracion.module').then(
            (module) => module.ConfiguracionModule
          ),
      },
      {
        path: 'auditoria',
        loadChildren: () =>
          import('./modulos/auditoria/auditoria.module').then((module) => module.AuditoriaModule),
      },
      { path: 'a/:token', component: QrRouteComponent },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'enabled' })],
  exports: [RouterModule],
})
export class AppRoutingModule {}
