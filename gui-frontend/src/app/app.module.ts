import { PasswordRecoveryComponent } from './login/password-recovery.component';
import { NgModule, LOCALE_ID } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { PrimeNGConfig } from 'primeng/api';
import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { AppRoutingModule } from './app-routing.module';
import { AppLayoutModule } from './layout/app.layout.module';
import { SharedModule } from './compartido/shared.module';
import { esTranslation } from './i18n/es';
registerLocaleData(localeEs, 'es-PE');
@NgModule({
  declarations: [AppComponent, LoginComponent, PasswordRecoveryComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    SharedModule,
    AppLayoutModule,
    AppRoutingModule,
  ],
  providers: [{ provide: LOCALE_ID, useValue: 'es-PE' }],
  bootstrap: [AppComponent],
})
export class AppModule {
  constructor(config: PrimeNGConfig) {
    config.setTranslation(esTranslation);
    config.ripple = true;
  }
}
