import { describe, expect, it } from 'vitest';

import { appRoutes } from './routes';

describe('appRoutes', () => {
  it('defines the required bootstrap route placeholders', () => {
    expect(appRoutes.map((route) => route.path)).toEqual([
      '/',
      '/places/:placeId',
      '/profile',
      '/admin/reviews'
    ]);
  });

  it('uses stable route names for later feature integration', () => {
    expect(appRoutes.map((route) => route.name)).toEqual([
      'home',
      'place-detail',
      'profile',
      'admin-reviews'
    ]);
  });
});
