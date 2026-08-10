import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function JoinPage() {
    const { signUp } = useAuth();
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [city, setCity] = useState("");
    const [error, setError] = useState(null);
    const [fields, setFields] = useState({});
    const [busy, setBusy] = useState(false);

    async function handleSubmit(event) {
        event.preventDefault();
        setError(null);
        setFields({});
        setBusy(true);
        try {
            await signUp(email, password, city);
            navigate("/");
        } catch (err) {
            setError(err.message);
            setFields(err.fields || {});
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="shell">
            <div className="panel">
                <h1>Join</h1>
                <p className="panel-intro">
                    You will be given a generated name like calm-maple-3867. That is
                    what appears on everything you write.
                </p>

                <form onSubmit={handleSubmit} className="stack">
                    {error && <div className="notice notice-error">{error}</div>}

                    <label className="field">
                        <span className="field-label">Email</span>
                        <span className="field-hint">Used to sign in. Never shown publicly.</span>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="email"
                            required
                        />
                        {fields.email && <span className="field-error">{fields.email}</span>}
                    </label>

                    <label className="field">
                        <span className="field-label">Password</span>
                        <span className="field-hint">At least 8 characters.</span>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="new-password"
                            required
                        />
                        {fields.password && <span className="field-error">{fields.password}</span>}
                    </label>

                    <label className="field">
                        <span className="field-label">City (optional)</span>
                        <span className="field-hint">Helps show posts from near you.</span>
                        <input
                            type="text"
                            value={city}
                            onChange={(e) => setCity(e.target.value)}
                        />
                    </label>

                    <button className="btn btn-primary btn-block" disabled={busy}>
                        {busy ? "Creating your account…" : "Create account"}
                    </button>
                </form>

                <p className="panel-foot">
                    Already have an account? <Link to="/sign-in">Sign in</Link>
                </p>
            </div>
        </div>
    );
}
