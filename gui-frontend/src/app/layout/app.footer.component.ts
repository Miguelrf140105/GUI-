import { Component } from '@angular/core';
import { environment } from '../../environments/environment';
@Component({
  selector: 'app-footer',
  templateUrl: './app.footer.component.html',
  styles: [':host { display: contents; }'],
})
export class AppFooterComponent {
  institution = environment.institution;
}
