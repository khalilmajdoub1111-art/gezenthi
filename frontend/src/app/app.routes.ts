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
      { path: 'hotel/:ref', loadComponent: () => import('./pages/hotel/hotel-page').then((m) => m.HotelPage), title: 'Hôtel — Gezenthi' },
      { path: 'reservation', loadComponent: () => import('./pages/booking/booking-page').then((m) => m.BookingPage), title: 'Réservation — Gezenthi' },
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
      { path: 'reservations', loadComponent: () => import('./admin/admin-bookings').then((m) => m.AdminBookings), title: 'Réservations — Admin Gezenthi' },
      { path: 'devis', loadComponent: () => import('./admin/admin-inquiries').then((m) => m.AdminInquiries), title: 'Devis — Admin Gezenthi' },
      { path: 'devis/:id', loadComponent: () => import('./admin/admin-inquiries').then((m) => m.AdminInquiryDetail), title: 'Devis — Admin Gezenthi' },
      { path: 'reservations/:id', loadComponent: () => import('./admin/admin-booking-detail').then((m) => m.AdminBookingDetail), title: 'Réservation — Admin Gezenthi' },
      { path: ':entity', loadComponent: () => import('./admin/entity-list').then((m) => m.EntityList), title: 'Offres — Admin Gezenthi' },
      { path: ':entity/new', loadComponent: () => import('./admin/entity-form').then((m) => m.EntityForm), title: 'Nouvelle offre — Admin Gezenthi' },
      { path: ':entity/:id', loadComponent: () => import('./admin/entity-form').then((m) => m.EntityForm), title: 'Modifier — Admin Gezenthi' },
    ],
  },
  { path: '**', redirectTo: '' },
];
