import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import Handle from "../components/Handle";
import ClaimThread from "../components/ClaimThread";

/**
 * Everything this person has going on: their own listings with the responses
 * to each, and the listings they have responded to.
 */
export default function MyExchangesPage() {
    const [listings, setListings] = useState([]);
    const [claims, setClaims] = useState([]);
    const [claimsByListing, setClaimsByListing] = useState({});
    const [openThread, setOpenThread] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        api.myListings().then(setListings).catch(() => setListings([]));
        api.myClaims().then(setClaims).catch(() => setClaims([]));

        // Clears the dot in the header. It disappears on the next navigation
        // rather than instantly — making it vanish the moment this page opens
        // would need the header to know what another page just did, and that
        // wiring is not worth it for a dot.
        api.markExchangesSeen().catch(() => {});
    }, []);

    useEffect(() => {
        listings.forEach((listing) => {
            api.listingClaims(listing.id)
                .then((result) =>
                    setClaimsByListing((prev) => ({ ...prev, [listing.id]: result })))
                .catch(() => {});
        });
    }, [listings]);

    function replaceClaim(updated) {
        setClaims((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
        setClaimsByListing((prev) => {
            const next = { ...prev };
            Object.keys(next).forEach((key) => {
                next[key] = next[key].map((c) => (c.id === updated.id ? updated : c));
            });
            return next;
        });
        if (openThread?.id === updated.id) setOpenThread(updated);
    }

    /** Accepting or declining changes the listing's status too, so reload it. */
    async function refreshListings() {
        try {
            setListings(await api.myListings());
        } catch {
            // Leave the current view alone if this fails.
        }
    }

    async function decide(claimId, accept) {
        setError(null);
        try {
            replaceClaim(accept
                ? await api.acceptClaim(claimId)
                : await api.declineClaim(claimId));
            await refreshListings();
        } catch (err) {
            setError(err.message);
        }
    }

    async function withdraw(claimId) {
        setError(null);
        try {
            replaceClaim(await api.withdrawClaim(claimId));
        } catch (err) {
            setError(err.message);
        }
    }

    /** The donor saying she has nothing left, which closes a shared listing. */
    async function closeListing(listingId) {
        setError(null);
        try {
            await api.markListingFulfilled(listingId);
            await refreshListings();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <div className="shell">
            <div className="page-head">
                <h1>Your exchanges</h1>
                <p>What you have offered or asked for, and who has responded.</p>
            </div>

            {error && <div className="notice notice-error">{error}</div>}

            <h2 className="directory-heading">Things you posted</h2>

            {listings.length === 0 ? (
                <p style={{ color: "var(--ink-faint)" }}>
                    Nothing yet. <Link to="/donations">Post something</Link>.
                </p>
            ) : (
                <ul className="feed">
                    {listings.map((listing) => {
                        const responses = claimsByListing[listing.id] || [];
                        const isOpen = listing.status === "OPEN"
                            || listing.status === "MATCHED";

                        return (
                            <li key={listing.id}>
                                <div className="card">
                                    <div className="card-meta">
                                        <span className={listing.listingType === "REQUEST"
                                            ? "need-tag" : "give-tag"}>
                                            {listing.listingType === "REQUEST"
                                                ? "Needed" : "Offered"}
                                        </span>
                                        <span>{listing.itemLabel}</span>
                                        {listing.canSplit && (
                                            <>
                                                <span className="dot">·</span>
                                                <span>can be shared</span>
                                            </>
                                        )}
                                        <span className="dot">·</span>
                                        <span>{listing.status.toLowerCase()}</span>
                                    </div>

                                    <h2>
                                        <Link to={`/donations/${listing.id}`}>
                                            {listing.title}
                                        </Link>
                                    </h2>

                                    {responses.length === 0 ? (
                                        <p style={{ color: "var(--ink-faint)", fontSize: 14 }}>
                                            No responses yet.
                                        </p>
                                    ) : (
                                        <ul className="contact-list" style={{ marginTop: 12 }}>
                                            {responses.map((claim) => (
                                                <li key={claim.id}
                                                    style={{ display: "block" }}>
                                                    <div className="comment-meta">
                                                        <Handle handle={claim.claimantHandle} />
                                                        <span className="dot">·</span>
                                                        <span>{claim.status.toLowerCase()}</span>
                                                    </div>

                                                    {claim.message && (
                                                        <p style={{ margin: "4px 0 0", fontSize: 15 }}>
                                                            {claim.message}
                                                        </p>
                                                    )}

                                                    <div style={{ display: "flex", gap: 8,
                                                                  marginTop: 10, flexWrap: "wrap" }}>
                                                        {claim.status === "PENDING" && (
                                                            <>
                                                                <button
                                                                    className="btn btn-safe"
                                                                    onClick={() => decide(claim.id, true)}
                                                                >
                                                                    Accept
                                                                </button>
                                                                <button
                                                                    className="btn btn-outline"
                                                                    onClick={() => decide(claim.id, false)}
                                                                >
                                                                    Decline
                                                                </button>
                                                            </>
                                                        )}
                                                        {(claim.status === "ACCEPTED"
                                                          || claim.status === "COMPLETED") && (
                                                            <button
                                                                className="btn btn-outline"
                                                                onClick={() => setOpenThread(
                                                                    openThread?.id === claim.id
                                                                        ? null : claim)}
                                                            >
                                                                {openThread?.id === claim.id
                                                                    ? "Close" : "Open conversation"}
                                                            </button>
                                                        )}
                                                    </div>

                                                    {openThread?.id === claim.id && (
                                                        <ClaimThread
                                                            claim={openThread}
                                                            onUpdate={replaceClaim}
                                                        />
                                                    )}
                                                </li>
                                            ))}
                                        </ul>
                                    )}

                                    {/*
                                       A shared listing never closes itself, so the
                                       donor needs a way to say she has run out.
                                    */}
                                    {listing.canSplit && isOpen && (
                                        <div style={{ marginTop: 14 }}>
                                            <button
                                                className="btn btn-outline"
                                                onClick={() => closeListing(listing.id)}
                                            >
                                                Nothing left — close this
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </li>
                        );
                    })}
                </ul>
            )}

            <h2 className="directory-heading">Things you responded to</h2>

            {claims.length === 0 ? (
                <p style={{ color: "var(--ink-faint)" }}>Nothing yet.</p>
            ) : (
                <ul className="feed">
                    {claims.map((claim) => (
                        <li key={claim.id}>
                            <div className="card">
                                <div className="card-meta">
                                    <span>{claim.status.toLowerCase()}</span>
                                </div>

                                <h2>
                                    <Link to={`/donations/${claim.listingId}`}>
                                        {claim.listingTitle}
                                    </Link>
                                </h2>

                                <div style={{ display: "flex", gap: 8, marginTop: 10,
                                              flexWrap: "wrap" }}>
                                    {(claim.status === "ACCEPTED"
                                      || claim.status === "COMPLETED") && (
                                        <button
                                            className="btn btn-outline"
                                            onClick={() => setOpenThread(
                                                openThread?.id === claim.id ? null : claim)}
                                        >
                                            {openThread?.id === claim.id
                                                ? "Close" : "Open conversation"}
                                        </button>
                                    )}

                                    {/*
                                       Available right up until the handover is
                                       marked done, including after agreeing a
                                       place. Changing your mind about meeting a
                                       stranger should never be made difficult.
                                    */}
                                    {(claim.status === "PENDING"
                                      || claim.status === "ACCEPTED") && (
                                        <button
                                            className="btn"
                                            onClick={() => withdraw(claim.id)}
                                        >
                                            Withdraw
                                        </button>
                                    )}
                                </div>

                                {openThread?.id === claim.id && (
                                    <ClaimThread claim={openThread} onUpdate={replaceClaim} />
                                )}
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
