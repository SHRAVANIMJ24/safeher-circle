import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";

const REASONS = [
    { id: "ABUSE", label: "Abusive or hateful" },
    { id: "DOXXING", label: "Reveals someone's identity" },
    { id: "SPAM", label: "Spam or advertising" },
    { id: "FAKE", label: "Fabricated or a scam" },
    { id: "SAFETY", label: "Someone may be in danger" },
    { id: "OTHER", label: "Something else" },
];

/**
 * Report control for a post or a comment.
 *
 * Deliberately understated — small, grey, at the end of the meta row. A
 * prominent report button on a board about harassment invites use as a weapon
 * against posts people simply disagree with.
 */
export default function ReportButton({ targetType, targetId }) {
    const { user } = useAuth();
    const [open, setOpen] = useState(false);
    const [reason, setReason] = useState("");
    const [detail, setDetail] = useState("");
    const [done, setDone] = useState(false);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    if (done) {
        return <span className="report-done">Reported. Thanks.</span>;
    }

    if (!open) {
        return (
            <button className="report-trigger" onClick={() => setOpen(true)}>
                Report
            </button>
        );
    }

    if (!user) {
        return (
            <div className="report-box">
                <p style={{ margin: 0, fontSize: 14 }}>
                    <Link to="/sign-in">Sign in</Link> to report this.
                </p>
            </div>
        );
    }

    async function submit(event) {
        event.preventDefault();
        setBusy(true);
        setError(null);
        try {
            await api.report({ targetType, targetId, reason, detail });
            setDone(true);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <form className="report-box" onSubmit={submit}>
            {error && <div className="notice notice-error">{error}</div>}

            <span className="field-label">What is wrong with this?</span>

            <div className="report-reasons">
                {REASONS.map((option) => (
                    <label key={option.id} className="report-reason">
                        <input
                            type="radio"
                            name={`reason-${targetId}`}
                            value={option.id}
                            checked={reason === option.id}
                            onChange={() => setReason(option.id)}
                        />
                        <span>{option.label}</span>
                    </label>
                ))}
            </div>

            <textarea
                value={detail}
                onChange={(e) => setDetail(e.target.value)}
                placeholder="Anything else a moderator should know (optional)"
                maxLength={2000}
                style={{ minHeight: 70, marginTop: 10 }}
            />

            <div style={{ display: "flex", gap: 8, marginTop: 10 }}>
                <button className="btn btn-primary" disabled={!reason || busy}>
                    {busy ? "Sending…" : "Send report"}
                </button>
                <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => setOpen(false)}
                >
                    Cancel
                </button>
            </div>
        </form>
    );
}
