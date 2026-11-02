import { Component } from '@angular/core';
import { environment } from '../../environments/environment';
import { AppLayoutService } from './service/app.layout.service';
@Component({
  selector: 'app-sidebar',
  templateUrl: './app.sidebar.component.html',
  styles: [':host { display: contents; }'],
})
export class AppSidebarComponent {
  institution = environment.institution;
  constructor(public layout: AppLayoutService) {}
}
