// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';

import App from '../../../app/App';
import { AuthProvider } from '../context/AuthContext';
import { getLastRequest, resetRequestSnapshots } from '../../../shared/api/tests/msw/handlers';
import { server } from '../../../shared/api/tests/msw/server';

function renderApp() {
  return render(
    <AuthProvider>
      <App />
    </AuthProvider>,
  );
}

function fillCredentials(email = 'buyer@example.com', password = 'secret-password') {
  fireEvent.change(screen.getByLabelText(/email/i), {
    target: { value: email },
  });
  fireEvent.change(screen.getByLabelText(/password/i), {
    target: { value: password },
  });
}

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' });
});

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  cleanup();
  resetRequestSnapshots();
  server.resetHandlers();
});

afterAll(() => {
  server.close();
});

describe('auth page', () => {
  it('shows login and register tabs with the login form by default', () => {
    renderApp();

    expect(screen.getByRole('tab', { name: /login/i })).not.toBeNull();
    expect(screen.getByRole('tab', { name: /register/i })).not.toBeNull();
    expect(screen.getByLabelText(/email/i)).not.toBeNull();
    expect(screen.getByLabelText(/password/i)).not.toBeNull();
  });

  it('submits login credentials to the login API', async () => {
    renderApp();

    fillCredentials();
    fireEvent.click(screen.getByRole('button', { name: /^login$/i }));

    await waitFor(() => {
      const request = getLastRequest();

      expect(request.method).toBe('POST');
      expect(new URL(request.url).pathname).toBe('/api/auth/login');
      expect(request.body).toEqual({
        email: 'buyer@example.com',
        password: 'secret-password',
      });
    });
  });

  it('submits register credentials to the register API', async () => {
    renderApp();

    fireEvent.click(screen.getByRole('tab', { name: /register/i }));
    fillCredentials('seller@example.com', 'seller-password');
    fireEvent.click(screen.getByRole('button', { name: /^register$/i }));

    await waitFor(() => {
      const request = getLastRequest();

      expect(request.method).toBe('POST');
      expect(new URL(request.url).pathname).toBe('/api/auth/register');
      expect(request.body).toEqual({
        email: 'seller@example.com',
        password: 'seller-password',
      });
    });
  });

  it('shows login API errors without entering the dashboard', async () => {
    server.use(
      http.post('http://localhost/api/auth/login', () =>
        HttpResponse.json({ message: 'Invalid email or password' }, { status: 401 }),
      ),
    );

    renderApp();

    fillCredentials();
    fireEvent.click(screen.getByRole('button', { name: /^login$/i }));

    expect(await screen.findByText(/invalid email or password/i)).not.toBeNull();
    expect(screen.queryByText(/inventory workspace/i)).toBeNull();
  });

  it('shows register API errors', async () => {
    server.use(
      http.post('http://localhost/api/auth/register', () =>
        HttpResponse.json({ message: 'Email already exists' }, { status: 409 }),
      ),
    );

    renderApp();

    fireEvent.click(screen.getByRole('tab', { name: /register/i }));
    fillCredentials('taken@example.com', 'secret-password');
    fireEvent.click(screen.getByRole('button', { name: /^register$/i }));

    expect(await screen.findByText(/email already exists/i)).not.toBeNull();
  });

  it('shows client validation errors and does not call the API when required fields are missing', async () => {
    renderApp();

    fireEvent.click(screen.getByRole('button', { name: /^login$/i }));

    expect(await screen.findByText(/email is required/i)).not.toBeNull();
    expect(screen.getByText(/password is required/i)).not.toBeNull();
    expect(() => getLastRequest()).toThrow('No request was captured');
  });

  it('shows registration success and returns the user to the login flow', async () => {
    renderApp();

    fireEvent.click(screen.getByRole('tab', { name: /register/i }));
    fillCredentials('new-owner@example.com', 'secret-password');
    fireEvent.click(screen.getByRole('button', { name: /^register$/i }));

    expect(await screen.findByText(/registration successful/i)).not.toBeNull();
    expect(screen.getByText(/please log in/i)).not.toBeNull();
    expect(screen.getByRole('tab', { name: /login/i }).getAttribute('aria-selected')).toBe(
      'true',
    );
  });

  it('enters the dashboard after successful login', async () => {
    renderApp();

    fillCredentials();
    fireEvent.click(screen.getByRole('button', { name: /^login$/i }));

    expect(await screen.findByText(/inventory workspace/i)).not.toBeNull();
    expect(screen.queryByRole('tab', { name: /login/i })).toBeNull();
  });
});
