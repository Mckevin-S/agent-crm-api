import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? true : inject(Router).createUrlTree(['/connexion']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/accueil']) : true;
};

/** Pages réservées aux managers (la vérification réelle reste côté API). */
export const managerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isManager() ? true : inject(Router).createUrlTree(['/accueil']);
};
