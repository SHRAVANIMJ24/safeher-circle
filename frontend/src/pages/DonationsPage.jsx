import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import Handle from "../components/Handle";

const TYPES = [
    { id: "", label: "Everything" },
    { id: "REQUEST", label: "People who need things" },
    { id: "OFFER", label: "Things being given away" },
];

/**
 * The donation board.
 *
 * Requests are listed publicly, not hidden behind an offer. Hiding them would
 * break the loop — nobody would know what was needed, so nobody would offer.
 * Seeing "three people near me need pads this month" is the thing that prompts
 * someone to buy a pack she was not otherwise going to buy.
 *
 * The privacy risk that creates is handled two other ways: a separate donation
 * handle, and the option to withhold a request's description until contact.
 */
export default function DonationsPage() {
    const { user } = useAuth();
    const [type, setType] = useState("");
    const [item, setItem] = useState("");
    const [city, setCity] = useState(user?.displayCity || "");
    const [items, setItems] = useState([]);
    const [page, setPage] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        api.itemTypes().then(setItems).catch(() => setItems([]));
    }, []);

    useEffect(() => {
        setLoading(true);
        api.listings({ type, item, city })
            .then(setPage)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [type, item, city]);

    return (
        <div className="shell">
            <div className="page-head">
                <h1>Give and receive</h1>
                <p>
                    Sanitary products and other essentials, offered and asked for by
                    people nearby. No money changes hands here.
                </p>
            </div>

            {user && (
                <div style={{ display: "flex", gap: 8, marginBottom: 20 }}>
                    <Link to="/donations/new?type=OFFER" className="btn btn-primary">
                        Offer something
                    </Link>
                    <Link to="/donations/new?type=REQUEST" className="btn btn-outline">
                        Ask for something
                    </Link>
                </div>
            )}

            <div className="filters">
                {TYPES.map((option) => (
                    <button
                        key={option.id}
                        className="chip"
                        aria-pressed={type === option.id}
                        onClick={() => setType(option.id)}
                    >
                        {option.label}
                    </button>
                ))}
            </div>

            <div className="filter-row">
                <select
                    value={item}
                    onChange={(e) => setItem(e.target.value)}
                    style={{ maxWidth: 220 }}
                >
                    <option value="">Any item</option>
                    {items.map((option) => (
                        <option key={option.slug} value={option.slug}>
                            {option.label}
                        </option>
                    ))}
                </select>

                <input
                    type="text"
                    placeholder="City"
                    defaultValue={city}
                    style={{ maxWidth: 180 }}
                    onKeyDown={(e) => {
                        if (e.key === "Enter") setCity(e.target.value.trim());
                    }}
                />
            </div>

            {error && <div className="notice notice-error">{error}</div>}
            {loading && <p style={{ color: "var(--ink-faint)" }}>Loading…</p>}

            {!loading && page?.items.length === 0 && (
                <div className="empty">
                    <h2>Nothing listed here yet</h2>
                    <p>Try a different city or item, or be the first to post.</p>
                </div>
            )}

            <ul className="feed">
                {page?.items.map((listing) => (
                    <li key={listing.id}>
                        <ListingCard listing={listing} />
                    </li>
                ))}
            </ul>

            {!loading && page?.items.length > 0 && (
                <div className="notice notice-quiet" style={{ marginTop: 24 }}>
                    <strong>Meeting someone.</strong> Arrange handovers in a public
                    place during the day — a station, a shop, outside a police
                    chowki. Tell someone where you are going. This platform cannot
                    vouch for anyone on it.
                </div>
            )}
        </div>
    );
}

function ListingCard({ listing }) {
    const isRequest = listing.listingType === "REQUEST";
    const place = [listing.areaName, listing.city].filter(Boolean).join(", ");

    return (
        <Link to={`/donations/${listing.id}`} className="card">
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
                {listing.canSplit && (
                    <>
                        <span className="dot">·</span>
                        <span>can be shared</span>
                    </>
                )}
                {listing.handledBy === "ORGANISATION" && listing.organisationName && (
                    <>
                        <span className="dot">·</span>
                        <span>via {listing.organisationName}</span>
                    </>
                )}
            </div>

            <h2>{listing.title}</h2>

            {listing.detailHidden ? (
                <p className="listing-hidden">
                    The details are private until someone gets in touch.
                </p>
            ) : (
                listing.description && (
                    <p className="card-body">{listing.description}</p>
                )
            )}

            <div className="card-foot">
                <Handle handle={listing.userHandle} />
                {listing.quantity && <span>{listing.quantity}</span>}
                {/*
                   Phrased as people rather than responses, so someone deciding
                   whether it is worth asking can see how many are ahead of her.
                */}
                {listing.claimCount > 0 && (
                    <span>
                        {listing.claimCount === 1
                            ? "1 person has asked"
                            : `${listing.claimCount} people have asked`}
                    </span>
                )}
            </div>
        </Link>
    );
}
