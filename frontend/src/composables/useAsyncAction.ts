import { ref, type Ref } from 'vue';

export interface AsyncActionState<T> {
  loading: Ref<boolean>;
  error: Ref<string | null>;
  data: Ref<T | null>;
  execute: (action: () => Promise<T>) => Promise<void>;
}

export function useAsyncAction<T>(options?: { onError?: (message: string) => void }): AsyncActionState<T> {
  const loading = ref(false);
  const error = ref<string | null>(null);
  const data: Ref<T | null> = ref(null);

  async function execute(action: () => Promise<T>): Promise<void> {
    loading.value = true;
    error.value = null;

    try {
      const result = await action();
      data.value = result;
    } catch (err) {
      const message = err instanceof Error ? err.message : '请求失败，请稍后重试。';
      error.value = message;
      options?.onError?.(message);
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    error,
    data,
    execute
  };
}
