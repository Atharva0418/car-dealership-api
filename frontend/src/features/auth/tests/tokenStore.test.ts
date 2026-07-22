import { describe, expect, it } from 'vitest';

import { getToken, setToken } from '../api/tokenStore';

describe('tokenStore', () => {
  it('returns null before any token is set', () => {
    expect(getToken()).toBeNull();
  });

  it('returns a token after one is set', () => {
    setToken('abc');

    expect(getToken()).toBe('abc');
  });

  it('clears a previously set token', () => {
    setToken('abc');
    setToken(null);

    expect(getToken()).toBeNull();
  });
});
