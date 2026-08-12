import { useEffect, useState } from "react";
import { api } from "../api/client";
import Handle from "./Handle";

/**
 * The private conversation between the two sides of a claim, plus the handover
 * arrangement.
 *
 * The place and time sit above the messages rather than inside them because
 * "where are we meeting" is the one thing both people need at a glance, and it
 * is exactly what gets buried in a chat.
 */
export default function ClaimThread({ claim, onUpdate }) {
    const [messages, setMessages] = useState([]);
    const [body, setBody] = useState("");
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        api.claimMessages(claim.id).then(setMessages).catch(() => setMessages([]));
    }, [claim.id]);

    // Events add system messages server-side, so reload the thread whenever the
    // claim changes — otherwise "This response was accepted" would not appear
    // until the next visit.
    useEffect(() => {
        api.claimMessages(claim.id).then(setMessages).catch(() => {});
    }, [claim.status, claim.handoverConfirmed, claim.proposedPlace, claim.id]);

    async function send(event) {
        event.preventDefault();
        if (!body.trim()) return;
        setBusy(true);
        try {
            const sent = await api.sendClaimMessage(claim.id, body.trim());
            setMessages((prev) => [...prev, sent]);
            setBody("");
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="claim-thread">
            {error && <div className="notice notice-error">{error}</div>}

            {claim.status === "ACCEPTED" && (
                <Handover claim={claim} onUpdate={onUpdate} />
            )}

            <ul className="message-list">
                {messages.map((message) => (
                    /*
                       System messages render as plain grey lines, deliberately
                       not styled as anybody speaking. Nobody said this — it
                       just happened.
                    */
                    message.system ? (
                        <li key={message.id} className="system-message">
                            {message.body}
                        </li>
                    ) : (
                        <li key={message.id}>
                            <div className="comment-meta">
                                <Handle handle={message.senderHandle} />
                                <span className="dot">·</span>
                                <span>
                                    {new Date(message.createdAt).toLocaleString(undefined, {
                                        day: "numeric", month: "short",
                                        hour: "2-digit", minute: "2-digit",
                                    })}
                                </span>
                            </div>
                            <p className="comment-body">{message.body}</p>
                        </li>
                    )
                ))}
            </ul>

            {messages.length === 0 && (
                <p style={{ color: "var(--ink-faint)", fontSize: 14 }}>
                    No messages yet.
                </p>
            )}

            {claim.status === "WITHDRAWN" || claim.status === "DECLINED" ? (
                <p style={{ color: "var(--ink-faint)", fontSize: 14, marginTop: 14 }}>
                    This exchange is closed.
                </p>
            ) : (
                <form onSubmit={send} className="comment-form" style={{ marginTop: 14 }}>
                    <textarea
                        value={body}
                        onChange={(e) => setBody(e.target.value)}
                        placeholder="Keep it to arranging the handover. Do not share your address or phone number."
                        maxLength={2000}
                        style={{ minHeight: 80 }}
                    />
                    <button className="btn btn-primary" disabled={busy || !body.trim()}>
                        {busy ? "Sending…" : "Send"}
                    </button>
                </form>
            )}
        </div>
    );
}

/**
 * Proposing and agreeing a place and time.
 *
 * Either side may propose; only the other side may confirm. That is deliberate
 * — if the person giving could set the meeting point alone, someone with no
 * fare and no say could be sent somewhere she cannot reach or does not feel
 * safe going.
 */
function Handover({ claim, onUpdate }) {
    const [place, setPlace] = useState(claim.proposedPlace || "");
    const [time, setTime] = useState(claim.proposedTime || "");
    const [editing, setEditing] = useState(!claim.proposedPlace);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);

    async function propose(event) {
        event.preventDefault();
        setBusy(true);
        setError(null);
        try {
            const updated = await api.proposeHandover(claim.id, { place, time });
            onUpdate(updated);
            setEditing(false);
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    async function confirm() {
        setBusy(true);
        setError(null);
        try {
            onUpdate(await api.confirmHandover(claim.id));
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    async function complete() {
        setBusy(true);
        setError(null);
        try {
            onUpdate(await api.completeHandover(claim.id));
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    if (claim.status === "COMPLETED") {
        return (
            <div className="handover handover-done">
                <strong>Handed over.</strong> Nothing further to arrange.
            </div>
        );
    }

    if (editing) {
        return (
            <form className="handover" onSubmit={propose}>
                <strong>Suggest where and when</strong>
                {error && <div className="notice notice-error">{error}</div>}

                <label className="field" style={{ marginTop: 10 }}>
                    <span className="field-label">Where</span>
                    <span className="field-hint">
                        Somewhere public and busy. A station, a shop, outside a
                        police chowki.
                    </span>
                    <input
                        type="text"
                        value={place}
                        onChange={(e) => setPlace(e.target.value)}
                        placeholder="Outside Andheri station, west side"
                        maxLength={300}
                        required
                    />
                </label>

                <label className="field" style={{ marginTop: 10 }}>
                    <span className="field-label">When</span>
                    <span className="field-hint">Daylight hours if you can.</span>
                    <input
                        type="text"
                        value={time}
                        onChange={(e) => setTime(e.target.value)}
                        placeholder="Saturday around 11am"
                        maxLength={120}
                        required
                    />
                </label>

                <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                    <button className="btn btn-primary" disabled={busy}>
                        {busy ? "Sending…" : "Suggest this"}
                    </button>
                    {claim.proposedPlace && (
                        <button
                            type="button"
                            className="btn btn-outline"
                            onClick={() => setEditing(false)}
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </form>
        );
    }

    return (
        <div className={claim.handoverConfirmed ? "handover handover-agreed" : "handover"}>
            <strong>
                {claim.handoverConfirmed ? "Agreed" : "Suggested"}
            </strong>

            <p style={{ margin: "6px 0 0" }}>
                {claim.proposedPlace}<br />
                {claim.proposedTime}
            </p>

            {error && <div className="notice notice-error">{error}</div>}

            <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
                {!claim.handoverConfirmed && !claim.proposedByMe && (
                    <button className="btn btn-safe" onClick={confirm} disabled={busy}>
                        That works
                    </button>
                )}

                {!claim.handoverConfirmed && claim.proposedByMe && (
                    <span style={{ fontSize: 14, color: "var(--ink-soft)" }}>
                        Waiting for them to confirm.
                    </span>
                )}

                {/*
                   Available even after agreeing. Changing your mind about
                   meeting a stranger should never be made difficult.
                */}
                <button
                    className="btn btn-outline"
                    onClick={() => setEditing(true)}
                    disabled={busy}
                >
                    Suggest something else
                </button>

                {claim.handoverConfirmed && (
                    <button className="btn btn-safe" onClick={complete} disabled={busy}>
                        Mark as done
                    </button>
                )}
            </div>
        </div>
    );
}
