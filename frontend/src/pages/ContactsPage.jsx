import { useEffect, useState } from "react";
import { api } from "../api/client";

const BLANK = { name: "", phone: "", relationship: "" };

export default function ContactsPage() {
    const [contacts, setContacts] = useState([]);
    const [form, setForm] = useState(BLANK);
    const [error, setError] = useState(null);
    const [fields, setFields] = useState({});
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        api.contacts().then(setContacts).catch((err) => setError(err.message));
    }, []);

    function update(key, value) {
        setForm((prev) => ({ ...prev, [key]: value }));
    }

    async function handleAdd(event) {
        event.preventDefault();
        setError(null);
        setFields({});
        setBusy(true);
        try {
            const created = await api.addContact({
                ...form,
                priority: contacts.length + 1,
            });
            setContacts((prev) => [...prev, created]);
            setForm(BLANK);
        } catch (err) {
            setError(err.message);
            setFields(err.fields || {});
        } finally {
            setBusy(false);
        }
    }

    async function handleRemove(id) {
        try {
            await api.removeContact(id);
            setContacts((prev) => prev.filter((c) => c.id !== id));
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <div className="shell">
            <div className="panel panel-wide">
                <h1>Trusted contacts</h1>
                <p className="panel-intro">
                    Up to five people. When you raise an alert they get a message
                    with your location, in the order listed here.
                </p>

                {error && <div className="notice notice-error">{error}</div>}

                {contacts.length > 0 && (
                    <ul className="contact-list" style={{ marginBottom: 24 }}>
                        {contacts.map((contact) => (
                            <li key={contact.id}>
                                <div>
                                    <strong>{contact.name}</strong>
                                    <span>
                                        {contact.phone}
                                        {contact.relationship && ` · ${contact.relationship}`}
                                    </span>
                                </div>
                                <button
                                    className="btn"
                                    onClick={() => handleRemove(contact.id)}
                                >
                                    Remove
                                </button>
                            </li>
                        ))}
                    </ul>
                )}

                {contacts.length >= 5 ? (
                    <div className="notice notice-quiet">
                        You have five contacts, which is the maximum. Remove one to
                        add someone else.
                    </div>
                ) : (
                    <form onSubmit={handleAdd} className="stack">
                        <label className="field">
                            <span className="field-label">Name</span>
                            <input
                                type="text"
                                value={form.name}
                                onChange={(e) => update("name", e.target.value)}
                                required
                            />
                            {fields.name && <span className="field-error">{fields.name}</span>}
                        </label>

                        <label className="field">
                            <span className="field-label">Phone number</span>
                            <span className="field-hint">
                                Include the country code, for example +91.
                            </span>
                            <input
                                type="text"
                                value={form.phone}
                                onChange={(e) => update("phone", e.target.value)}
                                required
                            />
                            {fields.phone && <span className="field-error">{fields.phone}</span>}
                        </label>

                        <label className="field">
                            <span className="field-label">Relationship (optional)</span>
                            <input
                                type="text"
                                value={form.relationship}
                                onChange={(e) => update("relationship", e.target.value)}
                                placeholder="Sister, friend, neighbour"
                            />
                        </label>

                        <button className="btn btn-primary btn-block" disabled={busy}>
                            {busy ? "Adding…" : "Add contact"}
                        </button>
                    </form>
                )}
            </div>
        </div>
    );
}
