import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import Handle from "../components/Handle";

export default function PostDetailPage() {
    const { id } = useParams();
    const [post, setPost] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        api.post(id).then(setPost).catch((err) => setError(err.message));
    }, [id]);

    if (error) {
        return (
            <div className="shell">
                <div className="empty" style={{ marginTop: 48 }}>
                    <h2>{error}</h2>
                    <p><Link to="/">Back to the board</Link></p>
                </div>
            </div>
        );
    }

    if (!post) {
        return (
            <div className="shell">
                <div className="skeleton" style={{ height: 300, marginTop: 48 }} />
            </div>
        );
    }

    const place = [post.areaName, post.city, post.state].filter(Boolean).join(", ");

    return (
        <div className="shell">
            <p style={{ margin: "28px 0 16px" }}>
                <Link to="/">← Back to the board</Link>
            </p>

            <article className="article">
                <div className="card-meta">
                    <Handle handle={post.authorHandle} />
                    <span className="category-tag">{post.categoryLabel}</span>
                </div>

                <h1>{post.title}</h1>

                {place && (
                    <p style={{ color: "var(--ink-soft)", fontSize: 14, margin: 0 }}>
                        {place}
                    </p>
                )}

                <p className="article-body">{post.body}</p>
            </article>

            <p style={{ color: "var(--ink-faint)", fontSize: 14, marginTop: 16 }}>
                Replies are coming soon.
            </p>
        </div>
    );
}
