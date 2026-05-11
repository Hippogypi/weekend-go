import { describe, expect, it } from 'vitest';

import { createWeekendGoApi } from './weekendGoApi';

describe('WeekendGoApi', () => {
  it('builds place discovery URLs from search and nearby parameters', async () => {
    const calls: Array<{ input: RequestInfo | URL; init?: RequestInit }> = [];
    const api = createWeekendGoApi({
      baseUrl: 'https://api.example.test/api',
      fetcher: async (input, init) => {
        calls.push({ input, init });
        return new Response(JSON.stringify({ success: true, code: 'OK', message: 'success', data: [] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        });
      }
    });

    await api.searchPlaces({ keyword: 'study room', city: 'Shanghai' });
    await api.nearbyPlaces({ longitude: '121.4737', latitude: '31.2304', keyword: 'library' });

    expect(calls[0].input).toBe('https://api.example.test/api/workspaces/search?keyword=study+room&city=Shanghai&page=1&offset=10');
    expect(calls[1].input).toBe('https://api.example.test/api/workspaces/nearby?longitude=121.4737&latitude=31.2304&keyword=library&radius=1000&page=1&offset=10');
  });

  it('builds map markers URL with default radius', async () => {
    const calls: Array<{ input: RequestInfo | URL; init?: RequestInit }> = [];
    const api = createWeekendGoApi({
      baseUrl: 'https://api.example.test/api',
      fetcher: async (input, init) => {
        calls.push({ input, init });
        return new Response(JSON.stringify({ success: true, code: 'OK', message: 'success', data: [{ id: 1, name: 'A', longitude: 116.4, latitude: 39.9, marked: true, favorited: false }] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        });
      }
    });

    const result = await api.mapMarkers({ longitude: '116.4', latitude: '39.9' });
    expect(calls[0].input).toBe('https://api.example.test/api/map/markers?longitude=116.4&latitude=39.9&radius=5000');
    expect(result).toEqual([{ id: 1, name: 'A', longitude: 116.4, latitude: 39.9, marked: true, favorited: false }]);
  });

  it('sends auth, contribution, checkin, review, image, favorite and audit requests to documented endpoints', async () => {
    const calls: Array<{ input: RequestInfo | URL; init?: RequestInit }> = [];
    const api = createWeekendGoApi({
      baseUrl: 'https://api.example.test/api',
      accessTokenProvider: () => 'token-1',
      fetcher: async (input, init) => {
        calls.push({ input, init });
        return new Response(JSON.stringify({ success: true, code: 'OK', message: 'success', data: {} }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        });
      }
    });

    await api.login({ username: 'demo', password: 'secret123' });
    await api.submitProfile(7, { quietScore: 4, wifiScore: 4, socketScore: 3, seatScore: 5 });
    await api.submitCheckin(7, { crowdLevel: 'NORMAL', noiseLevel: 'RELATIVELY_QUIET', hasSeat: true });
    await api.submitReview(7, { quietScore: 5, wifiScore: 4, socketScore: 4, comfortScore: 4, costScore: 3, content: 'Good' });
    await api.submitImage(7, { imageUrl: 'https://example.com/a.jpg', description: 'Window seats' });
    await api.addFavorite(7);
    await api.removeFavorite(7);
    await api.auditReview(9, { auditStatus: 'APPROVED', reason: 'ok' });
    await api.approveProfileSubmission(11, 'ok');
    await api.updateNickname('new-nick');
    await api.myProfileSubmissions();
    await api.myCheckins();
    await api.myReviews();
    await api.pendingList('review', 1, 20);
    await api.auditStats();

    expect(calls.map((call) => `${call.init?.method} ${call.input}`)).toEqual([
      'POST https://api.example.test/api/auth/login',
      'POST https://api.example.test/api/places/7/profile-submissions',
      'POST https://api.example.test/api/places/7/checkins',
      'POST https://api.example.test/api/places/7/reviews',
      'POST https://api.example.test/api/places/7/images',
      'POST https://api.example.test/api/places/7/favorite',
      'DELETE https://api.example.test/api/places/7/favorite',
      'PATCH https://api.example.test/api/admin/reviews/9/audit',
      'POST https://api.example.test/api/admin/profile-submissions/11/approve',
      'PATCH https://api.example.test/api/auth/me',
      'GET https://api.example.test/api/me/profile-submissions',
      'GET https://api.example.test/api/me/checkins',
      'GET https://api.example.test/api/me/reviews',
      'GET https://api.example.test/api/admin/audits/pending-list?type=review&page=1&size=20',
      'GET https://api.example.test/api/admin/audits/stats'
    ]);
    expect(calls[1].init?.headers).toMatchObject({ Authorization: 'Bearer token-1' });
  });

  it('sends question and answer requests to documented endpoints', async () => {
    const calls: Array<{ input: RequestInfo | URL; init?: RequestInit }> = [];
    const api = createWeekendGoApi({
      baseUrl: 'https://api.example.test/api',
      accessTokenProvider: () => 'token-1',
      fetcher: async (input, init) => {
        calls.push({ input, init });
        return new Response(JSON.stringify({ success: true, code: 'OK', message: 'success', data: {} }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        });
      }
    });

    await api.createQuestion(7, 'Is Wi-Fi fast?');
    await api.getQuestions(7);
    await api.createAnswer(3, 'Yes, very fast.');
    await api.getAnswers(3);

    expect(calls.map((call) => `${call.init?.method} ${call.input}`)).toEqual([
      'POST https://api.example.test/api/places/7/questions',
      'GET https://api.example.test/api/places/7/questions',
      'POST https://api.example.test/api/questions/3/answers',
      'GET https://api.example.test/api/questions/3/answers'
    ]);
  });
});
