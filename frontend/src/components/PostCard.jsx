import { Link } from "react-router-dom";
import Handle from "./Handle";

export default function PostCard({ post }) {
    const place = [post.areaName, post.city].filter(Boolean).join(", ");

    return (
        <Link to={`/posts/${post.id}`} className="card">
            <div className="card-meta">
                <Handle handle={post.authorHandle} />
                <span className="category-tag">{post.categoryLabel}</span>
                {place && (
                    <>
                        <span className="dot">·</span>
                        <span>{place}</span>
                    </>
                )}
            </div>
            <h2>{post.title}</h2>
            <p className="card-body">{post.body}</p>
            <div className="card-foot">
                <span>{relativeTime(post.createdAt)}</span>
                <span>
                    {post.commentCount === 1 ? "1 reply" : `${post.commentCount} replies`}
                </span>
            </div>
        </Link>
    );
}

function relativeTime(isoDate) {
    const then = new Date(isoDate);
    const minutes = Math.round((Date.now() - then.getTime()) / 60000);

    if (minutes < 1) return "just now";
    if (minutes < 60) return `${minutes}m ago`;

    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours}h ago`;

    const days = Math.round(hours / 24);
    if (days < 7) return `${days}d ago`;

    return then.toLocaleDateString(undefined, { day: "numeric", month: "short" });
}
