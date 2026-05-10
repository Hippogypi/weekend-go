import { describe, expect, it, vi } from 'vitest';

import { useAsyncAction } from './useAsyncAction';

describe('useAsyncAction', () => {
  it('starts with idle state', () => {
    const { loading, error, data } = useAsyncAction<string>();

    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
    expect(data.value).toBeNull();
  });

  it('sets loading and stores data on success', async () => {
    const { loading, error, data, execute } = useAsyncAction<string>();

    let wasLoadingDuringExecution = false;
    const promise = execute(async () => {
      wasLoadingDuringExecution = loading.value;
      return 'hello';
    });

    expect(loading.value).toBe(true);
    await promise;

    expect(wasLoadingDuringExecution).toBe(true);
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
    expect(data.value).toBe('hello');
  });

  it('sets error on failure and calls onError', async () => {
    const onError = vi.fn();
    const { loading, error, data, execute } = useAsyncAction<string>({ onError });

    await execute(async () => {
      throw new Error('boom');
    });

    expect(loading.value).toBe(false);
    expect(data.value).toBeNull();
    expect(error.value).toBe('boom');
    expect(onError).toHaveBeenCalledWith('boom');
  });

  it('handles non-Error rejections', async () => {
    const { error, execute } = useAsyncAction<string>();

    await execute(async () => {
      throw 'weird';
    });

    expect(error.value).toBe('请求失败，请稍后重试。');
  });
});
