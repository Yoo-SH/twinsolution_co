const rawBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const API_BASE_URL = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl;

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface RequestOptions {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
  query?: Record<string, string | number | boolean | undefined | null>;
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly details: unknown;

  constructor(message: string, status: number, details?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

const buildQueryString = (
  query?: Record<string, string | number | boolean | undefined | null>,
) => {
  if (!query) return '';

  const params = new URLSearchParams();

  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    params.append(key, String(value));
  });

  const queryString = params.toString();
  return queryString ? `?${queryString}` : '';
};

const buildUrl = (path: string, query?: RequestOptions['query']) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}${buildQueryString(query)}`;
};

const parseBody = async (response: Response) => {
  const contentType = response.headers.get('content-type');

  if (!contentType) {
    return null;
  }

  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
};

const request = async <T>(
  path: string,
  { method = 'GET', body, headers = {}, signal, query }: RequestOptions = {},
): Promise<T> => {
  const isFormData = body instanceof FormData;
  const init: RequestInit = {
    method,
    headers: {
      Accept: 'application/json',
      ...headers,
    },
    signal,
  };

  if (body !== undefined) {
    if (isFormData) {
      init.body = body;
    } else {
      init.headers = {
        ...init.headers,
        'Content-Type': 'application/json',
      };
      init.body = JSON.stringify(body);
    }
  }

  const response = await fetch(buildUrl(path, query), init);

  if (!response.ok) {
    const errorPayload = await parseBody(response);
    throw new ApiError(
      errorPayload?.message ?? response.statusText,
      response.status,
      errorPayload,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await parseBody(response)) as T;
};

const apiClient = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'method' | 'body'>) =>
    request<T>(path, { ...options, method: 'GET' }),
  post: <T>(
    path: string,
    body?: RequestOptions['body'],
    options?: Omit<RequestOptions, 'method' | 'body'>,
  ) => request<T>(path, { ...options, method: 'POST', body }),
  put: <T>(
    path: string,
    body?: RequestOptions['body'],
    options?: Omit<RequestOptions, 'method' | 'body'>,
  ) => request<T>(path, { ...options, method: 'PUT', body }),
  patch: <T>(
    path: string,
    body?: RequestOptions['body'],
    options?: Omit<RequestOptions, 'method' | 'body'>,
  ) => request<T>(path, { ...options, method: 'PATCH', body }),
  delete: <T>(
    path: string,
    options?: Omit<RequestOptions, 'method' | 'body'>,
  ) => request<T>(path, { ...options, method: 'DELETE' }),
};

export default apiClient;

