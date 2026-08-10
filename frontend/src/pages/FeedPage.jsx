import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { api } from "../api/client";
import PostCard from "../components/PostCard";

/**
 * The board. Filters live in the URL rather than component state, so a filtered
 * view can be bookmarked or sent to someone — "legal help in Pune" is a link.
 */
export default function FeedPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const [categories, setCategories] = useState([]);
    const [page, setPage] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const category = searchParams.get("category") || "";
    const city = searchParams.get("city") || "";
    const search = searchParams.get("search") || "";

    useEffect(() => {
        api.categories().then(setCategories).catch(() => setCategories([]));
    }, []);

    useEffect(() => {
        setLoading(true);
        setError(null);
        api.posts({ category, city, search })
            .then(setPage)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [category, city, search]);

    function updateFilter(key, value) {
        const next = new URLSearchParams(searchParams);
        if (value) {
            next.set(key, value);
        } else {
            next.delete(key);
        }
        setSearchParams(next);
    }

    return (
        <div className="shell">
            <div className="page-head">
                <h1>What people are saying</h1>
                <p>
                    Posts here are written under a generated name. Filter by what
                    you need or where you are.
                </p>
            </div>

            <div className="filters">
                <button
                    className="chip"
                    aria-pressed={category === ""}
                    onClick={() => updateFilter("category", "")}
                >
                    Everything
                </button>
                {categories.map((item) => (
                    <button
                        key={item.slug}
                        className="chip"
                        aria-pressed={category === item.slug}
                        onClick={() => updateFilter("category", item.slug)}
                    >
                        {item.label}
                    </button>
                ))}
            </div>

            <div className="filter-row">
                <input
                    type="search"
                    placeholder="Search posts"
                    defaultValue={search}
                    style={{ maxWidth: 280 }}
                    onKeyDown={(event) => {
                        if (event.key === "Enter") {
                            updateFilter("search", event.target.value.trim());
                        }
                    }}
                />
                <input
                    type="text"
                    placeholder="City"
                    defaultValue={city}
                    style={{ maxWidth: 180 }}
                    onKeyDown={(event) => {
                        if (event.key === "Enter") {
                            updateFilter("city", event.target.value.trim());
                        }
                    }}
                />
                {(category || city || search) && (
                    <button className="btn btn-outline" onClick={() => setSearchParams({})}>
                        Clear filters
                    </button>
                )}
            </div>

            {error && <div className="notice notice-error">{error}</div>}

            {loading && (
                <ul className="feed">
                    {[0, 1, 2].map((n) => (
                        <li key={n}><div className="skeleton" /></li>
                    ))}
                </ul>
            )}

            {!loading && page?.items.length === 0 && (
                <div className="empty">
                    <h2>Nothing here yet</h2>
                    <p>
                        No posts match these filters. Try clearing them, or be the
                        first to write about this.
                    </p>
                </div>
            )}

            {!loading && page?.items.length > 0 && (
                <>
                    <ul className="feed">
                        {page.items.map((post) => (
                            <li key={post.id}><PostCard post={post} /></li>
                        ))}
                    </ul>
                    <p style={{ color: "var(--ink-faint)", fontSize: 14, marginTop: 20 }}>
                        Showing {page.items.length} of {page.totalItems}
                    </p>
                </>
            )}
        </div>
    );
}
