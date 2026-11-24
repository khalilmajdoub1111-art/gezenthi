import { BOARD_TYPES, CatalogKey, IconName, SERVICE_KINDS } from '../core/models/travel.models';

/**
 * Declarative description of every offer type the admin manages.
 * The generic list/form pages render from this, so adding a field or a whole
 * new offer type is done here (plus the model), not in the UI components.
 */

export type FieldType = 'text' | 'number' | 'textarea' | 'select' | 'checkbox' | 'image' | 'tags';

export interface FieldDef {
  key: string;
  label: string;
  type: FieldType;
  required?: boolean;
  min?: number;
  max?: number;
  step?: number;
  options?: { value: string | number; label: string }[];
  placeholder?: string;
  hint?: string;
  /** Spans the full form width on desktop. */
  wide?: boolean;
}

export interface ColumnDef {
  key: string;
  label: string;
  format?: 'price' | 'stars' | 'days' | 'text';
}

export interface EntityConfig {
  key: CatalogKey;
  label: string; // plural, menu label
  singular: string;
  icon: IconName;
  description: string;
  titleField: string;
  imageField?: string;
  columns: ColumnDef[];
  fields: FieldDef[];
  defaults: Record<string, unknown>;
}

const opts = (values: readonly (string | number)[]) => values.map((v) => ({ value: v, label: String(v) }));
const PUBLISHED: FieldDef = { key: 'published', label: 'Publié sur le site', type: 'checkbox', hint: 'Décochez pour masquer (brouillon, complet…)' };

export const ADMIN_ENTITIES: EntityConfig[] = [
  {
    key: 'hotels',
    label: 'Hôtels',
    singular: 'hôtel',
    icon: 'hotel',
    description: 'Séjours en Tunisie et hôtels dans le monde',
    titleField: 'name',
    imageField: 'image',
    columns: [
      { key: 'city', label: 'Ville' },
      { key: 'stars', label: 'Étoiles', format: 'stars' },
      { key: 'board', label: 'Pension' },
      { key: 'pricePerNight', label: 'Prix / nuit', format: 'price' },
    ],
    fields: [
      { key: 'name', label: 'Nom de l’hôtel', type: 'text', required: true, wide: true },
      { key: 'region', label: 'Catégorie', type: 'select', required: true,
        options: [{ value: 'tunisie', label: 'Séjour en Tunisie' }, { value: 'monde', label: 'Hôtel dans le monde' }] },
      { key: 'city', label: 'Ville', type: 'text', required: true, placeholder: 'Hammamet' },
      { key: 'stars', label: 'Étoiles', type: 'select', required: true, options: opts([3, 4, 5]) },
      { key: 'board', label: 'Pension', type: 'select', required: true, options: opts(BOARD_TYPES) },
      { key: 'pricePerNight', label: 'Prix / nuit / pers. (DT)', type: 'number', required: true, min: 0 },
      { key: 'oldPrice', label: 'Ancien prix (DT)', type: 'number', min: 0, hint: 'Optionnel : affiche une réduction' },
      { key: 'rating', label: 'Note clients (/10)', type: 'number', min: 0, max: 10, step: 0.1 },
      { key: 'reviews', label: 'Nombre d’avis', type: 'number', min: 0 },
      { key: 'tags', label: 'Atouts', type: 'tags', placeholder: 'Plage privée, Spa, Kids club', hint: 'Séparés par des virgules', wide: true },
      { key: 'image', label: 'Photo principale', type: 'image', required: true, wide: true },
      PUBLISHED,
    ],
    defaults: { region: 'tunisie', stars: 4, board: 'Tout compris', rating: 8.5, reviews: 0, tags: [], published: true },
  },
  {
    key: 'trips',
    label: 'Voyages organisés',
    singular: 'voyage',
    icon: 'globe',
    description: 'Circuits et voyages à l’étranger',
    titleField: 'name',
    imageField: 'image',
    columns: [
      { key: 'country', label: 'Pays' },
      { key: 'durationDays', label: 'Durée', format: 'days' },
      { key: 'priceFrom', label: 'À partir de', format: 'price' },
    ],
    fields: [
      { key: 'name', label: 'Destination', type: 'text', required: true, placeholder: 'Istanbul' },
      { key: 'country', label: 'Pays', type: 'text', required: true, placeholder: 'Turquie' },
      { key: 'durationDays', label: 'Durée (jours)', type: 'number', required: true, min: 1 },
      { key: 'priceFrom', label: 'Prix à partir de (DT)', type: 'number', required: true, min: 0 },
      { key: 'badge', label: 'Badge', type: 'text', placeholder: 'Best-seller, Nouveau…' },
      { key: 'noVisa', label: 'Sans visa pour les Tunisiens', type: 'checkbox' },
      { key: 'image', label: 'Photo', type: 'image', required: true, wide: true },
      PUBLISHED,
    ],
    defaults: { durationDays: 7, noVisa: false, published: true },
  },
  {
    key: 'omra',
    label: 'Omra',
    singular: 'programme Omra',
    icon: 'moon',
    description: 'Programmes Omra',
    titleField: 'title',
    imageField: 'image',
    columns: [
      { key: 'departure', label: 'Départ' },
      { key: 'durationDays', label: 'Durée', format: 'days' },
      { key: 'priceFrom', label: 'À partir de', format: 'price' },
    ],
    fields: [
      { key: 'title', label: 'Nom du programme', type: 'text', required: true, placeholder: 'Omra Confort', wide: true },
      { key: 'departure', label: 'Date de départ', type: 'text', required: true, placeholder: '5 mars 2027' },
      { key: 'durationDays', label: 'Durée (jours)', type: 'number', required: true, min: 1 },
      { key: 'hotelDistance', label: 'Distance hôtel – Haram (m)', type: 'number', min: 0 },
      { key: 'priceFrom', label: 'Prix à partir de (DT)', type: 'number', required: true, min: 0 },
      { key: 'includes', label: 'Inclus', type: 'tags', placeholder: 'Vol direct, Visa, Guide religieux', hint: 'Séparés par des virgules', wide: true },
      { key: 'image', label: 'Photo', type: 'image', wide: true },
      PUBLISHED,
    ],
    defaults: { durationDays: 15, hotelDistance: 300, includes: [], image: '', published: true },
  },
  {
    key: 'flights',
    label: 'Vols',
    singular: 'vol',
    icon: 'plane',
    description: 'Vols au départ de Tunis',
    titleField: 'city',
    columns: [
      { key: 'airportCode', label: 'Aéroport' },
      { key: 'priceFrom', label: 'A/R dès', format: 'price' },
    ],
    fields: [
      { key: 'city', label: 'Ville de destination', type: 'text', required: true, placeholder: 'Paris' },
      { key: 'airportCode', label: 'Code aéroport', type: 'text', required: true, placeholder: 'ORY' },
      { key: 'priceFrom', label: 'Prix aller-retour dès (DT)', type: 'number', required: true, min: 0 },
      PUBLISHED,
    ],
    defaults: { published: true },
  },
  {
    key: 'services',
    label: 'Visa, CTN & TRE',
    singular: 'service',
    icon: 'passport',
    description: 'Visas, traversées CTN, offres TRE',
    titleField: 'title',
    columns: [
      { key: 'kind', label: 'Type' },
      { key: 'priceFrom', label: 'À partir de', format: 'price' },
    ],
    fields: [
      { key: 'kind', label: 'Type de service', type: 'select', required: true, options: opts(SERVICE_KINDS) },
      { key: 'title', label: 'Titre', type: 'text', required: true, placeholder: 'Visa Schengen' },
      { key: 'priceFrom', label: 'Prix à partir de (DT)', type: 'number', min: 0, hint: 'Optionnel' },
      { key: 'description', label: 'Description', type: 'textarea', required: true, wide: true },
      PUBLISHED,
    ],
    defaults: { kind: 'Visa', published: true },
  },
];

export function findEntity(key: string | null | undefined): EntityConfig | undefined {
  return ADMIN_ENTITIES.find((e) => e.key === key);
}
