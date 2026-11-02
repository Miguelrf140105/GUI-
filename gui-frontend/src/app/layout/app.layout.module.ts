import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../compartido/shared.module';
import { AppLayoutComponent } from './app.layout.component';
import { AppSidebarComponent } from './app.sidebar.component';
import { AppMenuComponent } from './app.menu.component';
import { AppTopbarComponent } from './app.topbar.component';
import { AppFooterComponent } from './app.footer.component';
@NgModule({
  declarations: [
    AppLayoutComponent,
    AppSidebarComponent,
    AppMenuComponent,
    AppTopbarComponent,
    AppFooterComponent,
  ],
  imports: [CommonModule, RouterModule, SharedModule],
})
export class AppLayoutModule {}
