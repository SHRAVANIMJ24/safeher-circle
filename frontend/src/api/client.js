/**
 * One place that knows how to talk to the backend.
 *
 * Everything goes through request(), so the token header, JSON parsing, and
 * the error shape are handled once instead of in every component.
 */

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8081";

const TOKEN_KEY = "safeher.token";

export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        localStorage.removeItem(TOKEN_KEY);
    }
}

/** Thrown for any non-2xx response, carrying the server's own message. */
export class ApiError extends Error {
    constructor(status, message, fields) {
        super(message);
        this.status = status;
        this.fields = fields || {};
    }
}

async function request(path, { method = "GET", body, auth = false } = {}) {
    const headers = {};

    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
    }

    if (auth) {
        const token = getToken();
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }
    }

    let response;
    try {
        response = await fetch(`${BASE_URL}${path}`, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch {
        // fetch only rejects on network failure, not on HTTP error codes.
        throw new ApiError(0, "Could not reach the server. Check your connection.");
    }

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();
    const payload = text ? safeParse(text) : null;

    if (!response.ok) {
        if (response.status === 401) {
            setToken(null);
        }
        throw new ApiError(
            response.status,
            payload?.message || "Something went wrong. Try again.",
            payload?.fields
        );
    }

    return payload;
}

function safeParse(text) {
    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}

export const api = {
    register: (data) =>
        request("/api/auth/register", { method: "POST", body: data }),

    login: (data) =>
        request("/api/auth/login", { method: "POST", body: data }),

    me: () => request("/api/auth/me", { auth: true }),

    categories: () => request("/api/categories"),

    posts: (params = {}) => {
        const query = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== "") {
                query.set(key, value);
            }
        });
        const suffix = query.toString() ? `?${query}` : "";
        return request(`/api/posts${suffix}`);
    },

    post: (id) => request(`/api/posts/${id}`),

    createPost: (data) =>
        request("/api/posts", { method: "POST", body: data, auth: true }),
};
