import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";

export default function NewListingPage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const initialType = searchParams.get("type") === "REQUEST" ? "REQUEST" : "OFFER";

    const [items, setItems] = useState([]);
    const [orgs, setOrgs] = useState([]);
    const [form, setForm] = useState({
        listingType: initialType,
        itemSlug: "",
        title: "",
        description: "",
        quantity: "",
        areaName: "",
        city: user?.displayCity || "",
        detailHidden: false,
        canSplit: false,
        handledBy: "INDIVIDUAL",
        organisationId: "",
    });
    const [error, setError] = useState(null);
    const [fields, setFields] = useState({});
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        api.itemTypes().then(setItems).catch(() => setItems([]));
        api.directory({ type: "NGO" }).then(setOrgs).catch(() => setOrgs([]));
    }, []);

    function update(key, value) {
        setForm((prev) => ({ ...prev, [key]: value }));
    }

    async function submit(event) {
        event.preventDefault();
        setError(null);
        setFields({});
        setBusy(true);
        try {
            const payload = {
                ...form,
                organisationId: form.handledBy === "ORGANISATION"
                    ? form.organisationId || null
                    : null,
                // Splitting only means anything when you have something to give.
                canSplit: form.listingType === "OFFER" ? form.canSplit : false,
            };
            const created = await api.createListing(payload);
            navigate(`/donations/${created.id}`);
        } catch (err) {
            setError(err.message);
            setFields(err.fields || {});
        } finally {
            setBusy(false);
        }
    }

    const isRequest = form.listingType === "REQUEST";

    return (
        <div className="shell">
            <div className="panel panel-wide">
                <h1>{isRequest ? "Ask for something" : "Offer something"}</h1>
                <p className="panel-intro">
                    This appears under a separate name from your posts, so nothing
                    here connects to anything else you have written.
                </p>

                <form onSubmit={submit} className="stack">
                    {error && <div className="notice notice-error">{error}</div>}

                    <div className="field">
                        <span className="field-label">Are you giving or asking?</span>
                        <div className="filters" style={{ paddingBottom: 0 }}>
                            <button
                                type="button"
                                className="chip"
                                aria-pressed={form.listingType === "OFFER"}
                                onClick={() => update("listingType", "OFFER")}
                            >
                                I have something to give
                            </button>
                            <button
                                type="button"
                                className="chip"
                                aria-pressed={form.listingType === "REQUEST"}
                                onClick={() => update("listingType", "REQUEST")}
                            >
                                I need something
                            </button>
                        </div>
                    </div>

                    <label className="field">
                        <span className="field-label">What kind of item</span>
                        <select
                            value={form.itemSlug}
                            onChange={(e) => update("itemSlug", e.target.value)}
                            required
                        >
                            <option value="">Choose one</option>
                            {items.map((option) => (
                                <option key={option.slug} value={option.slug}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                        {fields.itemSlug && (
                            <span className="field-error">{fields.itemSlug}</span>
                        )}
                    </label>

                    <label className="field">
                        <span className="field-label">Short title</span>
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
                        <span className="field-label">Details (optional)</span>
                        <textarea
                            value={form.description}
                            onChange={(e) => update("description", e.target.value)}
                            maxLength={5000}
                            style={{ minHeight: 110 }}
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">How much (optional)</span>
                        <input
                            type="text"
                            value={form.quantity}
                            onChange={(e) => update("quantity", e.target.value)}
                            placeholder="2 packs, roughly 20 pads"
                            maxLength={100}
                        />
                    </label>

                    <div className="filter-row" style={{ padding: 0, gap: 12 }}>
                        <label className="field" style={{ flex: 1, minWidth: 150 }}>
                            <span className="field-label">Area</span>
                            <input
                                type="text"
                                value={form.areaName}
                                onChange={(e) => update("areaName", e.target.value)}
                            />
                        </label>
                        <label className="field" style={{ flex: 1, minWidth: 150 }}>
                            <span className="field-label">City</span>
                            <input
                                type="text"
                                value={form.city}
                                onChange={(e) => update("city", e.target.value)}
                            />
                        </label>
                    </div>

                    <div className="field">
                        <span className="field-label">How should this be handled?</span>
                        <div className="filters" style={{ paddingBottom: 0 }}>
                            <button
                                type="button"
                                className="chip"
                                aria-pressed={form.handledBy === "INDIVIDUAL"}
                                onClick={() => update("handledBy", "INDIVIDUAL")}
                            >
                                Person to person
                            </button>
                            <button
                                type="button"
                                className="chip"
                                aria-pressed={form.handledBy === "ORGANISATION"}
                                onClick={() => update("handledBy", "ORGANISATION")}
                            >
                                Through an organisation
                            </button>
                        </div>
                        <p className="field-hint" style={{ marginTop: 8 }}>
                            Going through an organisation means neither of you has to
                            meet a stranger. Person to person is quicker.
                        </p>
                    </div>

                    {form.handledBy === "ORGANISATION" && (
                        <label className="field">
                            <span className="field-label">Which organisation</span>
                            <select
                                value={form.organisationId}
                                onChange={(e) => update("organisationId", e.target.value)}
                                required
                            >
                                <option value="">Choose one</option>
                                {orgs.map((org) => (
                                    <option key={org.id} value={org.id}>
                                        {org.name}{org.city ? ` — ${org.city}` : ""}
                                    </option>
                                ))}
                            </select>
                            <span className="field-hint" style={{ marginTop: 6 }}>
                                Contact them yourself first to check they can take this.
                            </span>
                        </label>
                    )}

                    {isRequest && (
                        <label className="sound" style={{ cursor: "pointer" }}>
                            <input
                                type="checkbox"
                                checked={form.detailHidden}
                                onChange={(e) => update("detailHidden", e.target.checked)}
                            />
                            <span className="sound-text">
                                <strong>Keep the details private</strong>
                                <span>
                                    Your item, area and title stay visible so people
                                    know what is needed. The rest is only shown to
                                    someone who gets in touch.
                                </span>
                            </span>
                        </label>
                    )}

                    {/*
                       Offers only. Quantity is free text, so the platform cannot
                       work out that five packs covers two people asking for two
                       and three — the donor says so herself.
                    */}
                    {!isRequest && (
                        <label className="sound" style={{ cursor: "pointer" }}>
                            <input
                                type="checkbox"
                                checked={form.canSplit}
                                onChange={(e) => update("canSplit", e.target.checked)}
                            />
                            <span className="sound-text">
                                <strong>I can split this between people</strong>
                                <span>
                                    The listing stays up after you accept someone, so
                                    others can still ask. Close it yourself when you
                                    have nothing left.
                                </span>
                            </span>
                        </label>
                    )}

                    <div className="notice notice-quiet">
                        No money is exchanged here. If someone asks you to send money
                        or share bank details, report the listing.
                    </div>

                    <button className="btn btn-primary btn-block" disabled={busy}>
                        {busy ? "Posting…" : "Post it"}
                    </button>
                </form>
            </div>
        </div>
    );
}
