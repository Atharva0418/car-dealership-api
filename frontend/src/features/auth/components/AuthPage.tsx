import type { FormEvent } from 'react';
import { useMemo, useState } from 'react';

import { ApiError } from '../../../shared/api/client';
import { login as loginApi, register as registerApi } from '../api/auth';
import { useAuth } from '../context/AuthContext';

type AuthMode = 'login' | 'register';

type FieldErrors = {
  email?: string;
  password?: string;
};

const modeContent = {
  login: {
    heading: 'Welcome back',
    eyebrow: 'Secure access',
    description: 'Sign in to manage inventory, listings, and dealership workflows.',
    button: 'Login',
    loading: 'Signing in...',
  },
  register: {
    heading: 'Create your account',
    eyebrow: 'New workspace',
    description: 'Register your dealership account, then sign in to continue.',
    button: 'Register',
    loading: 'Creating account...',
  },
} satisfies Record<AuthMode, Record<string, string>>;

function AlertIcon({ className = '' }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function CheckIcon({ className = '' }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="m5 13 4 4L19 7"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
      />
    </svg>
  );
}

function SpinnerIcon({ className = '' }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path
        className="opacity-90"
        d="M22 12a10 10 0 0 1-10 10"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="4"
      />
    </svg>
  );
}

function getBackendMessage(body: unknown) {
  if (typeof body === 'string') {
    return body;
  }

  if (body && typeof body === 'object' && 'message' in body) {
    return String(body.message);
  }

  return '';
}

function getAuthErrorMessage(error: unknown, mode: AuthMode) {
  if (!(error instanceof ApiError)) {
    return 'Service is down, Please try again later.';
  }

  const backendMessage = getBackendMessage(error.body);
  const normalizedMessage = backendMessage.toLowerCase();

  if (error.status === 400) {
    if (normalizedMessage.includes('password')) {
      return backendMessage || 'Please enter a valid password.';
    }

    if (normalizedMessage.includes('email')) {
      return backendMessage || 'Please enter a valid email address.';
    }

    return backendMessage || 'Please check your details and try again.';
  }

  if (error.status === 401) {
    return 'Invalid email or password. Please check your credentials.';
  }

  if (error.status === 403) {
    return 'You do not have permission to access this account.';
  }

  if (error.status === 409) {
    return 'Email Already Exists, please use another email.';
  }

  if (error.status === 429) {
    return 'Too many attempts. Please wait a moment and try again.';
  }

  if (error.status === 503 || error.status === 504 || error.status >= 500) {
    return 'Service is down, Please try again later.';
  }

  return (
    backendMessage ||
    (mode === 'login'
      ? 'Unable to sign in right now. Please try again.'
      : 'Unable to create your account right now. Please try again.')
  );
}

function validateCredentials(email: string, password: string) {
  const errors: FieldErrors = {};

  if (!email.trim()) {
    errors.email = 'Email is required';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = 'Enter a valid email address';
  }

  if (!password) {
    errors.password = 'Password is required';
  }

  return errors;
}

export function AuthPage() {
  const auth = useAuth();
  const [mode, setMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [apiError, setApiError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const content = modeContent[mode];
  const passwordHelp = useMemo(
    () => (mode === 'register' ? 'Use a strong password for your dealership account.' : 'Enter your account password.'),
    [mode],
  );

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setFieldErrors({});
    setApiError('');
    setSuccessMessage('');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const nextFieldErrors = validateCredentials(email, password);
    setFieldErrors(nextFieldErrors);
    setApiError('');
    setSuccessMessage('');

    if (Object.keys(nextFieldErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      if (mode === 'login') {
        const response = await loginApi({ email, password });
        auth.login(response.accessToken, response.refreshToken, email);
        return;
      }

      await registerApi({ email, password });
      setMode('login');
      setSuccessMessage('Registration successful. Please log in to continue.');
    } catch (error) {
      setApiError(getAuthErrorMessage(error, mode));
    } finally {
      setIsSubmitting(false);
    }
  }

  const emailErrorId = fieldErrors.email ? 'auth-email-error' : undefined;
  const passwordErrorId = fieldErrors.password ? 'auth-password-error' : undefined;

  return (
    <main className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_left,_#cffafe,_transparent_34%),linear-gradient(135deg,_#f8fafc_0%,_#e0f2fe_48%,_#ecfdf5_100%)] px-4 py-10 text-slate-950 sm:px-6">
      <section className="w-full max-w-md overflow-hidden rounded-3xl border border-white/70 bg-white/90 p-5 shadow-2xl shadow-cyan-950/10 backdrop-blur sm:p-8">
        <div className="mb-7">
          <p className="text-xs font-bold uppercase tracking-widest text-cyan-700">
            {content.eyebrow}
          </p>
          <h1 className="mt-3 text-3xl font-bold tracking-normal text-slate-950 sm:text-4xl">
            {content.heading}
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-600">{content.description}</p>
        </div>

        <div
          aria-label="Authentication options"
          className="relative mb-7 grid grid-cols-2 rounded-2xl bg-slate-100 p-1"
          role="tablist"
        >
          <span
            className={`absolute bottom-1 top-1 w-[calc(50%-0.25rem)] rounded-xl bg-white shadow-sm shadow-slate-950/10 transition-transform duration-200 ease-out ${
              mode === 'register' ? 'translate-x-full' : 'translate-x-0'
            }`}
          />
          <button
            aria-selected={mode === 'login'}
            className="relative z-10 rounded-xl px-4 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:text-slate-950 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:ring-offset-2"
            onClick={() => switchMode('login')}
            role="tab"
            type="button"
          >
            Login
          </button>
          <button
            aria-selected={mode === 'register'}
            className="relative z-10 rounded-xl px-4 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:text-slate-950 focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:ring-offset-2"
            onClick={() => switchMode('register')}
            role="tab"
            type="button"
          >
            Register
          </button>
        </div>

        <div aria-live="polite" className="min-h-0">
          {apiError ? (
            <div className="mb-5 flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-800">
              <AlertIcon className="mt-0.5 h-5 w-5 flex-none" />
              <p>{apiError}</p>
            </div>
          ) : null}

          {successMessage ? (
            <div className="mb-5 flex gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800">
              <CheckIcon className="mt-0.5 h-5 w-5 flex-none" />
              <p>{successMessage}</p>
            </div>
          ) : null}
        </div>

        <form
          className="animate-[auth-panel-enter_180ms_ease-out] space-y-5"
          key={mode}
          noValidate
          onSubmit={handleSubmit}
        >
          <div>
            <label className="text-sm font-semibold text-slate-800" htmlFor="auth-email">
              Email
            </label>
            <input
              aria-describedby={emailErrorId}
              aria-invalid={fieldErrors.email ? 'true' : 'false'}
              autoComplete="email"
              className="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-base text-slate-950 shadow-sm transition placeholder:text-slate-400 focus:border-cyan-500 focus:outline-none focus:ring-4 focus:ring-cyan-100"
              id="auth-email"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              type="email"
              value={email}
            />
            {fieldErrors.email ? (
              <p className="mt-2 text-sm font-medium text-rose-700" id="auth-email-error">
                {fieldErrors.email}
              </p>
            ) : null}
          </div>

          <div>
            <label className="text-sm font-semibold text-slate-800" htmlFor="auth-password">
              Password
            </label>
            <input
              aria-describedby={passwordErrorId}
              aria-invalid={fieldErrors.password ? 'true' : 'false'}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              className="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-base text-slate-950 shadow-sm transition placeholder:text-slate-400 focus:border-cyan-500 focus:outline-none focus:ring-4 focus:ring-cyan-100"
              id="auth-password"
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter your password"
              type="password"
              value={password}
            />
            {fieldErrors.password ? (
              <p className="mt-2 text-sm font-medium text-rose-700" id="auth-password-error">
                {fieldErrors.password}
              </p>
            ) : (
              <p className="mt-2 text-sm text-slate-500">{passwordHelp}</p>
            )}
          </div>

          <button
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-950 px-5 py-3.5 text-sm font-bold text-white shadow-lg shadow-slate-950/20 transition hover:bg-cyan-800 focus:outline-none focus:ring-4 focus:ring-cyan-200 disabled:cursor-not-allowed disabled:bg-slate-400 disabled:shadow-none"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? <SpinnerIcon className="h-4 w-4 animate-spin" /> : null}
            {isSubmitting ? content.loading : content.button}
          </button>
        </form>
      </section>
    </main>
  );
}
