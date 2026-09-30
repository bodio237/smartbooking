import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  {
    path: 'home',
    loadComponent: () =>
      import('./pages/home/home').then(m => m.Home)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login').then(m => m.Login)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register').then(m => m.Register)
  },
  {
    path: 'bookings',
    loadComponent: () =>
      import('./pages/bookings/bookings').then(m => m.Bookings)
  },
  {
    path: 'resources',
    loadComponent: () =>
      import('./pages/resources/resources').then(m => m.Resources)
  },
  {
  path: 'resources/:id',
  loadComponent: () =>
    import('./pages/resource-detail/resource-detail').then(m => m.ResourceDetail)
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./pages/admin/admin').then(m => m.Admin)
  },
  {
    path: '**',
    redirectTo: 'home'
  }
];