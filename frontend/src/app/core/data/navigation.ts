import { NavItem } from '../models/travel.models';

/**
 * Main services bar. Content mirrors the categories the client asked for;
 * hrefs point to in-page anchors until the dedicated pages exist.
 */
export const MAIN_NAV: NavItem[] = [
  {
    label: 'Séjour en Tunisie',
    href: '#hotels-tunisie',
    groups: [
      { title: 'Côte & mer', links: ['Hammamet', 'Nabeul', 'Korba', 'Sousse', 'Monastir', 'Mahdia', 'Djerba'].map(toLink) },
      { title: 'Nord & capitale', links: ['Tunis', 'Korbous', 'Tabarka', 'Ain Draham'].map(toLink) },
      { title: 'Sud & centre', links: ['Tozeur', 'Kairouan', 'Sfax'].map(toLink) },
    ],
  },
  { label: 'Hôtels dans le monde', href: '#voyages' },
  {
    label: "Voyages à l'étranger",
    href: '#voyages',
    groups: [
      { title: 'Destinations', links: ['Turquie', 'Égypte', 'Maroc', 'Algérie', 'Asie', 'Europe'].map(toLink) },
      {
        title: 'Nos sélections',
        links: ['Voyages sans visa', 'Voyages à petit prix', 'Voyages de luxe', 'Voyages en Afrique'].map(toLink),
      },
    ],
  },
  {
    label: 'Vols',
    href: '#vols',
    groups: [
      {
        title: 'Vols au départ de Tunis',
        links: ['Paris', 'Istanbul', 'Dubaï', 'Doha', 'Rome', 'Barcelone', 'Le Caire', 'Casablanca', 'Amsterdam', 'Berlin'].map(
          (city) => ({ label: `Tunis → ${city}`, href: '#vols' }),
        ),
      },
    ],
  },
  { label: 'CTN', href: '#services' },
  {
    label: 'Promos',
    href: '#promos',
    groups: [
      {
        links: ['Ventes flash', 'Promo du mois', 'Gratuités enfants', 'Early booking', 'Hôtels recommandés', 'Résidences hôtelières'].map(
          (label) => ({ label, href: '#promos' }),
        ),
      },
    ],
  },
  { label: 'TRE', href: '#services' },
  { label: 'Visa', href: '#omra-visa' },
  { label: 'Omra', href: '#omra-visa' },
  {
    label: 'Plus',
    href: '#',
    groups: [
      { links: ['Nos agences', 'Coffret cadeau', 'Espace entreprises', 'Blog', 'Contact'].map((label) => ({ label, href: '#contact' })) },
    ],
  },
];

/** Agency contact details. Email is still a placeholder until the client provides one. */
export const AGENCY_CONTACT = {
  phone: '+216 56 762 281',
  phoneHref: 'tel:+21656762281',
  email: 'contact@gezenthi.tn',
  address: 'Avenue Mohamed Kedidi, Nabeul, Tunisie',
  mapUrl: 'https://www.google.com/maps/search/?api=1&query=Avenue+Mohamed+Kedidi+Nabeul+Tunisie',
};

function toLink(label: string) {
  return { label, href: '#hotels-tunisie' };
}
