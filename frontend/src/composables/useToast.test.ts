import { describe, expect, it, vi } from 'vitest';

import { createToastService, getGlobalToastService, useToast } from './useToast';

describe('createToastService', () => {
  it('adds and removes toasts', () => {
    const { toasts, showToast, removeToast } = createToastService();

    expect(toasts.value).toHaveLength(0);

    showToast('hello', 'success');
    expect(toasts.value).toHaveLength(1);
    expect(toasts.value[0].message).toBe('hello');
    expect(toasts.value[0].type).toBe('success');

    const id = toasts.value[0].id;
    removeToast(id);
    expect(toasts.value).toHaveLength(0);
  });

  it('auto-removes toast after timeout', () => {
    vi.useFakeTimers();
    const { toasts, showToast } = createToastService();

    showToast('auto', 'error');
    expect(toasts.value).toHaveLength(1);

    vi.advanceTimersByTime(3000);
    expect(toasts.value).toHaveLength(0);

    vi.useRealTimers();
  });

  it('defaults type to success', () => {
    const { toasts, showToast } = createToastService();
    showToast('default');
    expect(toasts.value[0].type).toBe('success');
  });
});

describe('useToast', () => {
  it('returns the global toast service outside setup', () => {
    const service = useToast();
    expect(service).toBe(getGlobalToastService());
  });
});
