import { describe, expect, it } from 'vitest';

import { ApiClient } from './apiClient';

describe('ApiClient', () => {
  it('builds request URLs and attaches bearer tokens when available', async () => {
    const calls: Array<{ input: RequestInfo | URL; init?: RequestInit }> = [];
    const fetcher: typeof fetch = async (input, init) => {
      calls.push({ input, init });
      return new Response(JSON.stringify({ status: 'ok' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    };

    const client = new ApiClient({
      baseUrl: 'https://api.example.test/v1/',
      accessTokenProvider: () => 'dev-token',
      fetcher
    });

    const result = await client.get<{ status: string }>('/health');

    expect(result).toEqual({ status: 'ok' });
    expect(calls[0].input).toBe('https://api.example.test/v1/health');
    expect(calls[0].init?.headers).toMatchObject({
      Accept: 'application/json',
      Authorization: 'Bearer dev-token'
    });
  });

  it('throws ApiError with status and response body for failed responses', async () => {
    const fetcher: typeof fetch = async () =>
      new Response(JSON.stringify({ message: 'Unauthorized' }), {
        status: 401,
        statusText: 'Unauthorized',
        headers: { 'Content-Type': 'application/json' }
      });

    const client = new ApiClient({
      baseUrl: 'https://api.example.test',
      fetcher
    });

    await expect(client.get('/secure')).rejects.toMatchObject({
      name: 'ApiError',
      status: 401,
      payload: { message: 'Unauthorized' }
    });
  });

  it('sets JSON content type for post bodies by default', async () => {
    const calls: Array<{ input: RequestInfo | URL; init?: RequestInit }> = [];
    const fetcher: typeof fetch = async (input, init) => {
      calls.push({ input, init });
      return new Response(JSON.stringify({ id: 'created' }), {
        status: 201,
        headers: { 'Content-Type': 'application/json' }
      });
    };

    const client = new ApiClient({
      baseUrl: 'https://api.example.test',
      fetcher
    });

    await client.post('/places', { name: 'Desk spot' });

    expect(calls[0].init?.headers).toMatchObject({
      Accept: 'application/json',
      'Content-Type': 'application/json'
    });
    expect(calls[0].init?.body).toBe(JSON.stringify({ name: 'Desk spot' }));
  });

  it('does not overwrite an explicit content type for post requests', async () => {
    const calls: Array<{ input: RequestInfo | URL; init?: RequestInit }> = [];
    const fetcher: typeof fetch = async (input, init) => {
      calls.push({ input, init });
      return new Response(undefined, { status: 204 });
    };

    const client = new ApiClient({
      baseUrl: 'https://api.example.test',
      fetcher
    });

    await client.post('/imports', { rows: [] }, {
      headers: { 'Content-Type': 'application/vnd.weekend-go.import+json' }
    });

    expect(calls[0].init?.headers).toMatchObject({
      Accept: 'application/json',
      'Content-Type': 'application/vnd.weekend-go.import+json'
    });
  });
});
