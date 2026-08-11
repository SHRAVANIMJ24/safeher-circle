import { useEffect, useState } from "react";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import Handle from "../components/Handle";

const FILTERS = ["OPEN", "ACTIONED", "DISMISSED", "ALL"];

export default function ModerationPage() {
    const { user } = useAuth();
    const [filter, setFilter] = useState("OPEN");
    const [page, setPage] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const isModerator = user?.role === "MODERATOR" || user?.role === "ADMIN";

    useEffect(() => {
        if (!isModerator) return;
        setLoading(true);
        api.moderationQueue(filter)
            .then(setPage)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [filter, isModerator]);

    async function decide(reportId, action) {
        try {
            await api.decideReport(reportId, { action });
            setPage((prev) => ({
                ...prev,
                items: prev.items.filter((r) => r.id !== reportId),
            }));
        } catch (err) {
            setError(err.message);
        }
    }

    if (!isModerator) {
        return (
            <div className="shell">
                <div className="empty" style={{ marginTop: 48 }}>
                    <h2>Moderators only</h2>
                    <p>This page is not available on your account.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="shell">
            <div className="page-head">
                <h1>Moderation queue</h1>
                <p>
                    Reported content, oldest first. Nothing here has been hidden
                    automatically — flagged posts are still visible to everyone until
                    you decide otherwise.
                </p>
            </div>

            <div className="filters">
                {FILTERS.map((option) => (
                    <button
                        key={option}
                        className="chip"
                        aria-pressed={filter === option}
                        onClick={() => setFilter(option)}
                    >
                        {option.charAt(0) + option.slice(1).toLowerCase()}
                    </button>
                ))}
            </div>

            {error && <div className="notice notice-error">{error}</div>}
            {loading && <p style={{ color: "var(--ink-faint)" }}>Loading…</p>}

            {!loading && page?.items.length === 0 && (
                <div className="empty">
                    <h2>Nothing waiting</h2>
                    <p>No reports match this filter.</p>
                </div>
            )}

            <ul className="feed">
                {page?.items.map((report) => (
                    <li key={report.id}>
                        <div className="card">
                            <div className="card-meta">
                                <span className="reason-tag">{label(report.reason)}</span>
                                <span className="dot">·</span>
                                <span>{report.targetType.toLowerCase()}</span>
                                {report.reportCount > 1 && (
                                    <>
                                        <span className="dot">·</span>
                                        <span>{report.reportCount} reports</span>
                                    </>
                                )}
                                <span className="dot">·</span>
                                <span>{new Date(report.createdAt).toLocaleDateString()}</span>
                            </div>

                            {report.contentTitle && <h2>{report.contentTitle}</h2>}

                            <p className="card-body" style={{ WebkitLineClamp: 6 }}>
                                {report.contentBody}
                            </p>

                            {report.contentAuthorHandle && (
                                <div style={{ marginTop: 10 }}>
                                    <Handle handle={report.contentAuthorHandle} />
                                </div>
                            )}

                            {report.detail && (
                                <p className="report-detail">
                                    Reporter's note: {report.detail}
                                </p>
                            )}

                            {report.status === "OPEN" && (
                                <div style={{ display: "flex", gap: 8, marginTop: 14 }}>
                                    <button
                                        className="btn btn-outline"
                                        onClick={() => decide(report.id, "DISMISS")}
                                    >
                                        Leave it up
                                    </button>
                                    <button
                                        className="btn btn-remove"
                                        onClick={() => decide(report.id, "REMOVE")}
                                    >
                                        Take it down
                                    </button>
                                </div>
                            )}
                        </div>
                    </li>
                ))}
            </ul>
        </div>
    );
}

function label(reason) {
    return {
        ABUSE: "Abusive",
        DOXXING: "Reveals identity",
        SPAM: "Spam",
        FAKE: "Fabricated",
        SAFETY: "Safety concern",
        OTHER: "Other",
    }[reason] || reason;
}
