import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";

export default function NewPostPage() {
    const { user } = useAuth();
    const navigate = useNavigate();

    const [categories, setCategories] = useState([]);
    const [form, setForm] = useState({
        title: "",
        body: "",
        categorySlug: "",
        areaName: "",
        city: user?.displayCity || "",
        state: "",
    });
    const [error, setError] = useState(null);
    const [fields, setFields] = useState({});
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        api.categories().then(setCategories).catch(() => setCategories([]));
    }, []);

    function update(key, value) {
        setForm((prev) => ({ ...prev, [key]: value }));
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError(null);
        setFields({});
        setBusy(true);
        try {
            const created = await api.createPost(form);
            navigate(`/posts/${created.id}`);
        } catch (err) {
            setError(err.message);
            setFields(err.fields || {});
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="shell">
            <div className="panel panel-wide">
                <h1>Write a post</h1>
                <p className="panel-intro">
                    This will appear as {user?.anonHandle}. Leave out anything that
                    would identify you — names, workplaces, exact addresses.
                </p>

                <form onSubmit={handleSubmit} className="stack">
                    {error && <div className="notice notice-error">{error}</div>}

                    <label className="field">
                        <span className="field-label">Title</span>
                        <input
                            type="text"
                            value={form.title}
                            onChange={(e) => update("title", e.target.value)}
                            maxLength={200}
                            required
                        />
                        {fields.title && <span className="field-error">{fields.title}</span>}
                    </label>

                    <label className="field">
                        <span className="field-label">Category</span>
                        <select
                            value={form.categorySlug}
                            onChange={(e) => update("categorySlug", e.target.value)}
                            required
                        >
                            <option value="">Choose one</option>
                            {categories.map((item) => (
                                <option key={item.slug} value={item.slug}>
                                    {item.label}
                                </option>
                            ))}
                        </select>
                        {fields.categorySlug && (
                            <span className="field-error">{fields.categorySlug}</span>
                        )}
                    </label>

                    <label className="field">
                        <span className="field-label">What is going on?</span>
                        <textarea
                            value={form.body}
                            onChange={(e) => update("body", e.target.value)}
                            maxLength={10000}
                            required
                        />
                        {fields.body && <span className="field-error">{fields.body}</span>}
                    </label>

                    <div className="filter-row" style={{ padding: 0, gap: 12 }}>
                        <label className="field" style={{ flex: 1, minWidth: 150 }}>
                            <span className="field-label">Area (optional)</span>
                            <input
                                type="text"
                                value={form.areaName}
                                onChange={(e) => update("areaName", e.target.value)}
                            />
                        </label>
                        <label className="field" style={{ flex: 1, minWidth: 150 }}>
                            <span className="field-label">City (optional)</span>
                            <input
                                type="text"
                                value={form.city}
                                onChange={(e) => update("city", e.target.value)}
                            />
                        </label>
                    </div>

                    <div className="notice notice-quiet">
                        Location is stored roughly, to about a kilometre. It is enough
                        to show your post to people nearby and not enough to find you.
                    </div>

                    <button className="btn btn-primary btn-block" disabled={busy}>
                        {busy ? "Posting…" : "Post"}
                    </button>
                </form>
            </div>
        </div>
    );
}
