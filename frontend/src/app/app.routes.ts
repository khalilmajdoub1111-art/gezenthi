import { Routes } from '@angular/router';
import { adminGuard } from './core/services/auth.service';
import { PublicLayout } from './layout/public-layout';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: '', loadComponent: () => import('./pages/home/home').then((m) => m.Home), title: 'Gezenthi Travel Agency' },
      { path: 'recherche', loadComponent: () => import('./pages/search/search-results').then((m) => m.SearchResults), title: 'Recherche d’hôtels — Gezenthi' },
    ],
  },
  {
    path: 'admin/login',
    loadComponent: () => import('./admin/admin-login').then((m) => m.AdminLogin),
    title: 'Connexion — Admin Gezenthi',
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./admin/admin-shell').then((m) => m.AdminShell),
    children: [
      { path: '', loadComponent: () => import('./admin/admin-dashboard').then((m) => m.AdminDashboard), title: 'Tableau de bord — Admin Gezenthi' },
      { path: 'fournisseurs', loadComponent: () => import('./admin/admin-suppliers').then((m) => m.AdminSuppliers), title: 'Fournisseurs — Admin Gezenthi' },
      { path: ':entity', loadComponent: () => import('./admin/entity-list').then((m) => m.EntityList), title: 'Offres — Admin Gezenthi' },
      { path: ':entity/new', loadComponent: () => import('./admin/entity-form').then((m) => m.EntityForm), title: 'Nouvelle offre — Admin Gezenthi' },
      { path: ':entity/:id', loadComponent: () => import('./admin/entity-form').then((m) => m.EntityForm), title: 'Modifier — Admin Gezenthi' },
    ],
  },
  { path: '**', redirectTo: '' },
];
