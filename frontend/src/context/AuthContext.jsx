import { createContext, useContext, useEffect, useState } from "react";
import { api, getToken, setToken } from "../api/client";

const AuthContext = createContext(null);

/**
 * Holds the signed-in user for the whole app.
 *
 * On load it checks whether the stored token is still good by calling /me, so
 * an expired token signs the person out on arrival rather than failing later,
 * halfway through writing a post.
 */
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!getToken()) {
            setLoading(false);
            return;
        }
        api.me()
            .then(setUser)
            .catch(() => setToken(null))
            .finally(() => setLoading(false));
    }, []);

    async function signIn(email, password) {
        const result = await api.login({ email, password });
        setToken(result.token);
        setUser(await api.me());
    }

    async function signUp(email, password, displayCity) {
        const result = await api.register({ email, password, displayCity });
        setToken(result.token);
        setUser(await api.me());
    }

    function signOut() {
        setToken(null);
        setUser(null);
    }

    return (
        <AuthContext.Provider value={{ user, loading, signIn, signUp, signOut }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used inside an AuthProvider");
    }
    return context;
}
