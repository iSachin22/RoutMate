/* =============================================================
 * RouteMate frontend · api.js
 * Centralized API layer (same error taxonomy as the original spec):
 *  · auto-attaches Content-Type: application/json + Bearer JWT
 *  · 401 → "Session expired…" / "Invalid email or password"
 *  · 403 → "Unauthorized / Role Mismatch…"
 *  · fetch failure → "Network Error: could not reach the backend"
 *  · client-side validators run BEFORE any fetch (TC-05)
 * ============================================================= */

const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1';

export const STORAGE_KEYS = {
  token: 'jwtToken',
  userId: 'userId',
  userRole: 'userRole',
  userName: 'userName',
};

export const PARCEL_RATE_PER_KG = 15;

export class ApiError extends Error {
  constructor(message, { kind = 'UNKNOWN', status = 0, userMessage = message, errors = [] } = {}) {
    super(message);
    this.name = 'ApiError';
    this.kind = kind;
    this.status = status;
    this.userMessage = userMessage;
    this.errors = errors;
  }
}

function extractServerMessage(payload) {
  if (!payload) return '';
  if (typeof payload === 'string') return payload;
  return payload.message || payload.error || payload.detail || '';
}

function buildHttpError(status, payload, path) {
  const serverMessage = extractServerMessage(payload);
  if (status === 401) {
    if (path.includes('/auth/login')) {
      return new ApiError(serverMessage || 'Unauthorized', {
        kind: 'AUTH', status, userMessage: 'Invalid email or password.',
      });
    }
    return new ApiError(serverMessage || 'Unauthorized', {
      kind: 'AUTH', status, userMessage: 'Session expired or invalid token. Please log in again.',
    });
  }
  if (status === 403) {
    return new ApiError(serverMessage || 'Forbidden', {
      kind: 'FORBIDDEN', status,
      userMessage: 'Unauthorized / Role Mismatch: your account is not allowed to perform this action.',
    });
  }
  if (status === 400 || status === 422) {
    return new ApiError(serverMessage || 'Bad request', {
      kind: 'VALIDATION', status,
      userMessage: 'The backend rejected the data' + (serverMessage ? ': ' + serverMessage : '.'),
    });
  }
  if (status === 404) {
    return new ApiError(serverMessage || 'Not found', {
      kind: 'NOT_FOUND', status, userMessage: 'Not found: the requested resource does not exist.',
    });
  }
  if (status >= 500) {
    return new ApiError(serverMessage || 'Server error', {
      kind: 'SERVER', status, userMessage: `Server error (${status}): the backend failed to process the request.`,
    });
  }
  return new ApiError(serverMessage || `HTTP ${status}`, {
    kind: 'HTTP', status,
    userMessage: `Request failed with HTTP ${status}${serverMessage ? ': ' + serverMessage : '.'}`,
  });
}

async function request(method, path, options = {}) {
  const { body, auth = true, role, query } = options;

  if (role && getSession()?.role !== role) {
    throw new ApiError('Role mismatch', {
      kind: 'FORBIDDEN', status: 403,
      userMessage: `Unauthorized / Role Mismatch: this action requires a ${role} account.`,
    });
  }

  const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
  if (auth) {
    const token = localStorage.getItem(STORAGE_KEYS.token);
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let url = API_BASE + path;
  if (query) {
    const params = Object.entries(query)
      .filter(([, v]) => v !== undefined && v !== null && v !== '')
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
    if (params.length) url += `?${params.join('&')}`;
  }

  let response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body !== undefined && body !== null ? JSON.stringify(body) : undefined,
    });
  } catch (networkErr) {
    throw new ApiError(`Network Error: ${networkErr.message}`, {
      kind: 'NETWORK', status: 0,
      userMessage:
        'Network Error: could not reach the RouteMate backend. Make sure the Spring Boot server is running on http://localhost:8081.',
    });
  }

  const text = await response.text();
  let payload = null;
  if (text) {
    try { payload = JSON.parse(text); } catch { payload = text; }
  }
  if (!response.ok) {
    const err = buildHttpError(response.status, payload, path);
    if (err.kind === 'AUTH' && !path.includes('/auth/login')) clearSession();
    throw err;
  }
  return payload;
}

/* ---------------- session ---------------- */

export function saveSession(data) {
  if (!data?.token) return null;
  localStorage.setItem(STORAGE_KEYS.token, data.token);
  localStorage.setItem(STORAGE_KEYS.userId, data.userId || '');
  localStorage.setItem(STORAGE_KEYS.userRole, data.role || '');
  localStorage.setItem(STORAGE_KEYS.userName, data.name || '');
  return data;
}

export function getSession() {
  const token = localStorage.getItem(STORAGE_KEYS.token);
  if (!token) return null;
  return {
    token,
    userId: localStorage.getItem(STORAGE_KEYS.userId),
    role: localStorage.getItem(STORAGE_KEYS.userRole),
    name: localStorage.getItem(STORAGE_KEYS.userName),
  };
}

export function clearSession() {
  Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k));
}

export function logout() {
  clearSession();
}

/* ---------------- validators (run BEFORE any fetch — TC-05) ---------------- */

export const validators = {
  isValidLat: (lat) => typeof lat === 'number' && Number.isFinite(lat) && lat >= -90 && lat <= 90,
  isValidLng: (lng) => typeof lng === 'number' && Number.isFinite(lng) && lng >= -180 && lng <= 180,

  validateTripPayload(p) {
    const errors = [];
    if (!p || typeof p !== 'object') return { valid: false, errors: ['Trip payload is missing.'] };
    if (!p.originName?.trim()) errors.push('Origin name is required.');
    if (!p.destinationName?.trim()) errors.push('Destination name is required.');
    if (!Array.isArray(p.routeWaypoints) || p.routeWaypoints.length < 2) {
      errors.push('routeWaypoints requires at least 2 valid coordinate pairs (lng, lat) to form a LineString.');
    } else {
      p.routeWaypoints.forEach((wp, i) => {
        if (!wp || !validators.isValidLng(wp.lng) || !validators.isValidLat(wp.lat)) {
          errors.push(
            `Waypoint ${i + 1} is invalid (lng: ${wp?.lng}, lat: ${wp?.lat}). ` +
            'Latitude must be -90 to 90 and longitude -180 to 180.'
          );
        }
      });
    }
    if (!Number.isInteger(p.availableSeats) || p.availableSeats < 0) errors.push('availableSeats must be a whole number of 0 or more.');
    if (!(typeof p.availableBootKg === 'number' && p.availableBootKg >= 0)) errors.push('availableBootKg must be 0 or more.');
    if (typeof p.baseFare !== 'number' || !(p.baseFare > 0)) errors.push('baseFare must be a positive amount.');
    if (!p.departureTime || Number.isNaN(new Date(p.departureTime).getTime())) errors.push('departureTime must be a valid date/time.');
    return { valid: errors.length === 0, errors };
  },

  validateBookingPayload(p) {
    const errors = [];
    if (!p || typeof p !== 'object') return { valid: false, errors: ['Booking payload is missing.'] };
    if (!p.tripId) errors.push('tripId is required.');
    if (p.bookingType !== 'SEAT' && p.bookingType !== 'PARCEL') errors.push('bookingType must be "SEAT" or "PARCEL".');
    if (p.bookingType === 'SEAT') {
      if (!Number.isInteger(p.bookedSeats) || p.bookedSeats < 1) errors.push('A SEAT booking needs bookedSeats of at least 1.');
      if (p.parcelWeightKg) errors.push('A SEAT booking must have parcelWeightKg of 0.');
    }
    if (p.bookingType === 'PARCEL') {
      if (p.bookedSeats !== 0) errors.push('A PARCEL booking must have bookedSeats of 0.');
      if (!(p.parcelWeightKg > 0)) errors.push('A PARCEL booking needs parcelWeightKg greater than 0.');
    }
    if (!validators.isValidLat(p.pickupLat)) errors.push('pickupLat must be between -90 and 90.');
    if (!validators.isValidLng(p.pickupLng)) errors.push('pickupLng must be between -180 and 180.');
    if (!validators.isValidLat(p.dropLat)) errors.push('dropLat must be between -90 and 90.');
    if (!validators.isValidLng(p.dropLng)) errors.push('dropLng must be between -180 and 180.');
    return { valid: errors.length === 0, errors };
  },
};

function clientValidationError(verdict) {
  return new ApiError('Validation failed', {
    kind: 'CLIENT_VALIDATION',
    errors: verdict.errors,
    userMessage: `Please fix the highlighted problems: ${verdict.errors.join(' ')}`,
  });
}

/* ---------------- API calls ---------------- */

export const api = {
  register: (payload) => request('POST', '/auth/register', { body: payload, auth: false }),
  login: async (payload) => saveSession(await request('POST', '/auth/login', { body: payload, auth: false })),

  publishTrip(payload) {
    const verdict = validators.validateTripPayload(payload);
    if (!verdict.valid) return Promise.reject(clientValidationError(verdict));
    return request('POST', '/trips', { body: payload, role: 'DRIVER' });
  },
  searchTrips: (payload) => request('POST', '/trips/search', { body: payload }),
  getDriverTrips: (driverId) => request('GET', `/trips/driver/${encodeURIComponent(driverId)}`, { role: 'DRIVER' }),

  createBooking(payload) {
    const verdict = validators.validateBookingPayload(payload);
    if (!verdict.valid) return Promise.reject(clientValidationError(verdict));
    return request('POST', '/bookings', { body: payload, role: 'PASSENGER' });
  },
  getTripBookings: (tripId) => request('GET', `/bookings/trip/${encodeURIComponent(tripId)}`, { role: 'DRIVER' }),
  verifyBookingOtp: (bookingId, otpCode) =>
    request('POST', `/bookings/${encodeURIComponent(bookingId)}/verify-otp`, {
      body: { otpCode: String(otpCode) }, role: 'DRIVER',
    }),
};

/* ---------------- small UI helpers ---------------- */

export function asArray(data, listKey) {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (listKey && Array.isArray(data[listKey])) return data[listKey];
  return data.trips || data.bookings || data.content || data.data || data.results || [];
}

export function formatDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? String(iso) : d.toLocaleString();
}

export function formatMoney(amount) {
  if (amount === null || amount === undefined || Number.isNaN(Number(amount))) return '—';
  return `₹${Number(amount).toLocaleString('en-IN')}`;
}

export function estimateFare({ bookingType, baseFare, bookedSeats, parcelWeightKg }) {
  const base = Number(baseFare) || 0;
  if (bookingType === 'PARCEL') return base + (Number(parcelWeightKg) || 0) * PARCEL_RATE_PER_KG;
  return base * (Number(bookedSeats) || 1);
}
