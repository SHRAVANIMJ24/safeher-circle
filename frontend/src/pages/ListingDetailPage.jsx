import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import Handle from "../components/Handle";
import ReportButton from "../components/ReportButton";

export default function ListingDetailPage() {
    const { id } = useParams();
    const { user } = useAuth();

    const [listing, setListing] = useState(null);
    const [claims, setClaims] = useState([]);
    const [message, setMessage] = useState("");
    const [claimed, setClaimed] = useState(false);
    const [error, setError] = useState(null);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        api.listing(id).then(setListing).catch((err) => setError(err.message));
    }, [id]);

    // Only the owner can see who has responded.
    useEffect(() => {
        if (!listing || !user) return;
        api.listingClaims(id).then(setClaims).catch(() => setClaims([]));
    }, [listing, user, id]);

    async function respond(event) {
        event.preventDefault();
        setBusy(true);
        setError(null);
        try {
            await api.claimListing(id, { message });
            setClaimed(true);
            setMessage("");
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    if (error && !listing) {
        return (
            <div className="shell">
                <div className="empty" style={{ marginTop: 48 }}>
                    <h2>{error}</h2>
                    <p><Link to="/donations">Back to the board</Link></p>
                </div>
            </div>
        );
    }

    if (!listing) {
        return (
            <div className="shell">
                <div className="skeleton" style={{ height: 240, marginTop: 48 }} />
            </div>
        );
    }

    const isRequest = listing.listingType === "REQUEST";
    const place = [listing.areaName, listing.city].filter(Boolean).join(", ");

    return (
        <div className="shell">
            <p style={{ margin: "28px 0 16px" }}>
                <Link to="/donations">← Back to the board</Link>
            </p>

            <article className="article">
                <div className="card-meta">
                    <span className={isRequest ? "need-tag" : "give-tag"}>
                        {isRequest ? "Needed" : "Offered"}
                    </span>
                    <span>{listing.itemLabel}</span>
                    {place && (
                        <>
                            <span className="dot">·</span>
                            <span>{place}</span>
                        </>
                    )}
                </div>

                <h1>{listing.title}</h1>

                {listing.detailHidden ? (
                    <p className="listing-hidden" style={{ marginTop: 16 }}>
                        The details on this one are private. Get in touch below and
                        they will be shared with you.
                    </p>
                ) : (
                    listing.description && (
                        <p className="article-body">{listing.description}</p>
                    )
                )}

                {listing.quantity && (
                    <p style={{ marginTop: 16, color: "var(--ink-soft)", fontSize: 15 }}>
                        Quantity: {listing.quantity}
                    </p>
                )}

                {listing.handledBy === "ORGANISATION" && listing.organisationName && (
                    <div className="notice notice-quiet" style={{ marginTop: 16 }}>
                        Handled through <strong>{listing.organisationName}</strong>.
                        Neither of you needs to meet the other.
                    </div>
                )}

                <div style={{ marginTop: 16 }}>
                    <Handle handle={listing.userHandle} />
                </div>
            </article>

            <div style={{ marginTop: 12 }}>
                <ReportButton targetType="LISTING" targetId={listing.id} />
            </div>

            {claims.length > 0 && (
                <section style={{ marginTop: 28 }}>
                    <h2 style={{ fontSize: 19, marginBottom: 12 }}>
                        {claims.length === 1
                            ? "1 person has responded"
                            : `${claims.length} people have responded`}
                    </h2>
                    <ul className="contact-list">
                        {claims.map((claim) => (
                            <li key={claim.id}>
                                <div>
                                    <Handle handle={claim.claimantHandle} />
                                    {claim.message && (
                                        <p style={{ margin: "6px 0 0", fontSize: 15 }}>
                                            {claim.message}
                                        </p>
                                    )}
                                </div>
                                <span style={{ fontSize: 13, color: "var(--ink-faint)" }}>
                                    {claim.status.toLowerCase()}
                                </span>
                            </li>
                        ))}
                    </ul>
                </section>
            )}

            {!user ? (
                <div className="notice notice-quiet" style={{ marginTop: 24 }}>
                    <Link to="/sign-in">Sign in</Link> to respond to this.
                </div>
            ) : claimed ? (
                <div className="notice notice-quiet" style={{ marginTop: 24 }}>
                    Sent. You will see their reply under your responses.
                </div>
            ) : (
                <form onSubmit={respond} className="comment-form" style={{ marginTop: 24 }}>
                    <span className="field-label">
                        {isRequest ? "Offer to help" : "Ask for this"}
                    </span>
                    {error && <div className="notice notice-error">{error}</div>}
                    <textarea
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        placeholder="Say roughly where you are and when you could manage. Do not share your address or phone number here."
                        maxLength={1000}
                        style={{ minHeight: 100, marginTop: 8 }}
                    />
                    <button className="btn btn-primary" disabled={busy}>
                        {busy ? "Sending…" : "Send"}
                    </button>
                </form>
            )}

            <div className="notice notice-quiet" style={{ marginTop: 20 }}>
                <strong>If you arrange to meet.</strong> Somewhere public, in
                daylight, and tell someone where you are going. Nobody on this
                platform has been verified.
            </div>
        </div>
    );
}
