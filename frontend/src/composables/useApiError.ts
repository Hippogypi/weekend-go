import { ref } from 'vue';
import { ApiError } from '../services';

const STATUS_MESSAGES: Record<number, string> = {
  400: '请求参数错误，请检查输入',
  401: '登录已过期，请重新登录',
  403: '权限不足，无法执行此操作',
  404: '资源不存在或已被删除',
  409: '操作冲突，请刷新后重试',
  415: '文件格式不支持',
  500: '系统异常，请稍后重试',
  502: '外部位置服务暂时不可用'
};

export function useApiError() {
  const errorMessage = ref<string | null>(null);

  function setError(err: unknown): string {
    let message: string;

    if (err instanceof ApiError) {
      message = STATUS_MESSAGES[err.status] ?? (err.payload as { message?: string })?.message ?? '请求失败，请稍后重试。';
    } else if (err instanceof Error && err.message.includes('fetch')) {
      message = '网络异常，请检查连接';
    } else if (err instanceof Error) {
      message = err.message;
    } else {
      message = '网络异常，请检查连接';
    }

    errorMessage.value = message;
    return message;
  }

  function clearError(): void {
    errorMessage.value = null;
  }

  return {
    errorMessage,
    setError,
    clearError
  };
}
