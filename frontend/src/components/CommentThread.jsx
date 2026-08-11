import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import Handle from "./Handle";
import ReportButton from "./ReportButton";
/**
 * Replies on a post.
 *
 * Nesting is one level deep. Anything more and a phone screen turns into a
 * column of indents twelve characters wide, and this is a board people read on
 * phones.
 */
export default function CommentThread({ postId, postAuthorHandle }) {
    const { user } = useAuth();
    const [comments, setComments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [replyingTo, setReplyingTo] = useState(null);

    useEffect(() => {
        api.comments(postId)
            .then(setComments)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [postId]);

    async function submit(body, parentId) {
        const created = await api.addComment(postId, { body, parentId });

        if (parentId) {
            setComments((prev) =>
                prev.map((c) =>
                    c.id === parentId
                        ? { ...c, replies: [...c.replies, created] }
                        : c
                )
            );
        } else {
            setComments((prev) => [...prev, created]);
        }
        setReplyingTo(null);
    }

    const total = comments.reduce((sum, c) => sum + 1 + c.replies.length, 0);

    return (
        <section className="thread">
            <h2 className="thread-head">
                {total === 0 ? "No replies yet" : total === 1 ? "1 reply" : `${total} replies`}
            </h2>

            {error && <div className="notice notice-error">{error}</div>}

            {user ? (
                <CommentForm
                    onSubmit={(body) => submit(body, null)}
                    placeholder="Share what you know, or just let them know they were heard."
                />
            ) : (
                <div className="notice notice-quiet">
                    <Link to="/sign-in">Sign in</Link> to reply.
                </div>
            )}

            {loading && <p style={{ color: "var(--ink-faint)" }}>Loading replies…</p>}

            <ul className="comment-list">
                {comments.map((comment) => (
                    <li key={comment.id}>
                        <CommentBody comment={comment} />

                        {user && (
                            <button
                                className="link-button"
                                onClick={() =>
                                    setReplyingTo(replyingTo === comment.id ? null : comment.id)
                                }
                            >
                                {replyingTo === comment.id ? "Cancel" : "Reply"}
                            </button>
                        )}

                        {replyingTo === comment.id && (
                            <div className="comment-replies">
                                <CommentForm
                                    onSubmit={(body) => submit(body, comment.id)}
                                    placeholder={`Replying to ${comment.authorHandle}`}
                                    compact
                                />
                            </div>
                        )}

                        {comment.replies.length > 0 && (
                            <ul className="comment-replies">
                                {comment.replies.map((reply) => (
                                    <li key={reply.id}>
                                        <CommentBody comment={reply} />
                                    </li>
                                ))}
                            </ul>
                        )}
                    </li>
                ))}
            </ul>
        </section>
    );
}

function CommentBody({ comment }) {
    return (
        <>
            <div className="comment-meta">
                <Handle handle={comment.authorHandle} />
                {comment.isAuthorOfPost && (
                    <span className="op-badge">original poster</span>
                )}
                <span className="dot">·</span>
                <span>{new Date(comment.createdAt).toLocaleDateString(undefined, {
                    day: "numeric", month: "short",
                })}</span>
            </div>
            <p className="comment-body">{comment.body}</p>
            <div style={{ marginTop: 6 }}>
                <ReportButton targetType="COMMENT" targetId={comment.id} />
            </div>
        </>
    );
}

function CommentForm({ onSubmit, placeholder, compact }) {
    const [body, setBody] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);

    async function handleSubmit(event) {
        event.preventDefault();
        if (!body.trim()) return;

        setBusy(true);
        setError(null);
        try {
            await onSubmit(body.trim());
            setBody("");
        } catch (err) {
            setError(err.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <form onSubmit={handleSubmit} className="comment-form">
            {error && <div className="notice notice-error">{error}</div>}
            <textarea
                value={body}
                onChange={(e) => setBody(e.target.value)}
                placeholder={placeholder}
                maxLength={5000}
                style={{ minHeight: compact ? 80 : 110 }}
            />
            <button className="btn btn-primary" disabled={busy || !body.trim()}>
                {busy ? "Posting…" : "Post reply"}
            </button>
        </form>
    );
}
