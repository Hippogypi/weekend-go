import { beforeAll, describe, expect, it } from 'vitest';

const memoryStorage = (() => {
  let data: Record<string, string> = {};
  return {
    clear: () => { data = {}; },
    getItem: (key: string) => data[key] ?? null,
    removeItem: (key: string) => { delete data[key]; },
    setItem: (key: string, value: string) => { data[key] = value; }
  };
})();

Object.defineProperty(globalThis, 'localStorage', {
  value: memoryStorage,
  configurable: true
});

let ApiError: typeof import('../services').ApiError;
let useApiError: typeof import('./useApiError').useApiError;

describe('useApiError', () => {
  beforeAll(async () => {
    ({ ApiError } = await import('../services'));
    ({ useApiError } = await import('./useApiError'));
  });

  it('returns null message initially', () => {
    const { errorMessage } = useApiError();
    expect(errorMessage.value).toBeNull();
  });

  it.each([
    [400, '请求参数错误，请检查输入'],
    [401, '登录已过期，请重新登录'],
    [403, '权限不足，无法执行此操作'],
    [404, '资源不存在或已被删除'],
    [409, '操作冲突，请刷新后重试'],
    [415, '文件格式不支持'],
    [500, '系统异常，请稍后重试'],
    [502, '外部位置服务暂时不可用']
  ])('maps ApiError status %i to "%s"', (status, expected) => {
    const { errorMessage, setError } = useApiError();
    setError(new ApiError('err', status, {}));
    expect(errorMessage.value).toBe(expected);
  });

  it('falls back to payload message for unknown status', () => {
    const { errorMessage, setError } = useApiError();
    setError(new ApiError('err', 418, { message: 'teapot' }));
    expect(errorMessage.value).toBe('teapot');
  });

  it('falls back to generic message when no payload message', () => {
    const { errorMessage, setError } = useApiError();
    setError(new ApiError('err', 418, {}));
    expect(errorMessage.value).toBe('请求失败，请稍后重试。');
  });

  it('maps network-like errors', () => {
    const { errorMessage, setError } = useApiError();
    setError(new Error('Failed to fetch'));
    expect(errorMessage.value).toBe('网络异常，请检查连接');
  });

  it('clears error', () => {
    const { errorMessage, setError, clearError } = useApiError();
    setError(new ApiError('err', 400, {}));
    clearError();
    expect(errorMessage.value).toBeNull();
  });
});
