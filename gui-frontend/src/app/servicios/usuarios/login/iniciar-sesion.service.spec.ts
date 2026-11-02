import { of } from 'rxjs';
import { InicioSesionService } from './iniciar-sesion.service';

describe('Inicio de sesión después de expirar', () => {
  it('descarta el CSRF anterior antes de enviar las credenciales', async () => {
    const api = jasmine.createSpyObj('api', ['resetCsrf', 'post', 'token', 'get']);
    api.post.and.callFake(() => {
      expect(api.resetCsrf).toHaveBeenCalledTimes(1);
      return of(null);
    });
    api.token.and.returnValue(of({ token: 'nuevo' }));
    api.get.and.returnValue(of({ permissions: [301] }));
    const auth = new InicioSesionService(api);
    await auth.login('usuario', 'contraseña');
    expect(api.resetCsrf).toHaveBeenCalledTimes(2);
    expect(auth.can(301)).toBeTrue();
  });
});
