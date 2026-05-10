import { inject, provide, ref, type Ref } from 'vue';

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning';
}

export interface ToastService {
  toasts: Ref<Toast[]>;
  showToast: (message: string, type?: Toast['type']) => void;
  removeToast: (id: number) => void;
}

const TOAST_KEY = Symbol('toast');

let globalToastService: ToastService | null = null;

export function createToastService(): ToastService {
  const toasts = ref<Toast[]>([]);
  let nextId = 1;

  function showToast(message: string, type: Toast['type'] = 'success'): void {
    const id = nextId++;
    toasts.value.push({ id, message, type });

    setTimeout(() => {
      removeToast(id);
    }, 3000);
  }

  function removeToast(id: number): void {
    toasts.value = toasts.value.filter((t) => t.id !== id);
  }

  return { toasts, showToast, removeToast };
}

export function getGlobalToastService(): ToastService {
  if (!globalToastService) {
    globalToastService = createToastService();
  }
  return globalToastService;
}

export function provideToast(): ToastService {
  const service = getGlobalToastService();
  provide(TOAST_KEY, service);
  return service;
}

export function useToast(): ToastService {
  const injected = inject<ToastService | undefined>(TOAST_KEY, undefined);
  return injected ?? getGlobalToastService();
}
