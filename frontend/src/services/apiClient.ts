export type AccessTokenProvider = () => string | null | undefined;

export interface ApiClientOptions {
  baseUrl: string;
  accessTokenProvider?: AccessTokenProvider;
  fetcher?: typeof fetch;
}

export class ApiError extends Error {
  readonly status: number;
  readonly payload: unknown;

  constructor(message: string, status: number, payload: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

export class ApiClient {
  private readonly baseUrl: string;
  private readonly accessTokenProvider?: AccessTokenProvider;
  private readonly fetcher: typeof fetch;

  constructor(options: ApiClientOptions) {
    this.baseUrl = options.baseUrl.replace(/\/+$/, '');
    this.accessTokenProvider = options.accessTokenProvider;
    this.fetcher = options.fetcher ?? fetch;
  }

  get<TResponse>(path: string, init?: RequestInit): Promise<TResponse> {
    return this.request<TResponse>(path, { ...init, method: 'GET' });
  }

  post<TResponse, TBody = unknown>(
    path: string,
    body: TBody,
    init?: RequestInit
  ): Promise<TResponse> {
    return this.request<TResponse>(path, {
      ...init,
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  async request<TResponse>(path: string, init: RequestInit = {}): Promise<TResponse> {
    const response = await this.fetcher(this.buildUrl(path), {
      ...init,
      headers: this.buildHeaders(init.headers)
    });

    const payload = await this.readPayload(response);

    if (!response.ok) {
      throw new ApiError(response.statusText || 'Request failed', response.status, payload);
    }

    return payload as TResponse;
  }

  private buildUrl(path: string): string {
    const cleanPath = path.replace(/^\/+/, '');
    return `${this.baseUrl}/${cleanPath}`;
  }

  private buildHeaders(headers?: HeadersInit): Record<string, string> {
    const mergedHeaders: Record<string, string> = {
      Accept: 'application/json'
    };

    if (headers instanceof Headers) {
      headers.forEach((value, key) => {
        mergedHeaders[key] = value;
      });
    } else if (Array.isArray(headers)) {
      headers.forEach(([key, value]) => {
        mergedHeaders[key] = value;
      });
    } else if (headers) {
      Object.assign(mergedHeaders, headers);
    }

    const token = this.accessTokenProvider?.();
    if (token) {
      mergedHeaders.Authorization = `Bearer ${token}`;
    }

    return mergedHeaders;
  }

  private async readPayload(response: Response): Promise<unknown> {
    if (response.status === 204) {
      return undefined;
    }

    const contentType = response.headers.get('Content-Type') ?? '';
    if (contentType.includes('application/json')) {
      return response.json();
    }

    return response.text();
  }
}
