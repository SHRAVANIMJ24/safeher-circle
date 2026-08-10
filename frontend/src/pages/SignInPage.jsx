import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function SignInPage() {
    const { signIn } = useAuth();
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        setError(null);
        setBusy(true);
        try {
            await signIn(email, password);
            navigate("/");
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="shell">
            <div className="panel">
                <h1>Sign in</h1>
                <p className="panel-intro">
                    Your email is only used to sign in. It never appears on a post.
                </p>

                <form onSubmit={handleSubmit} className="stack">
                    {error && <div className="notice notice-error">{error}</div>}

                    <label className="field">
                        <span className="field-label">Email</span>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="email"
                            required
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">Password</span>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            required
                        />
                    </label>

                    <button className="btn btn-primary btn-block" disabled={busy}>
                        {busy ? "Signing in…" : "Sign in"}
                    </button>
                </form>

                <p className="panel-foot">
                    No account yet? <Link to="/join">Join</Link>
                </p>
            </div>
        </div>
    );
}
