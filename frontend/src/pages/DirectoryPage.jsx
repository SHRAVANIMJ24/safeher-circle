import { useEffect, useState } from "react";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";

const TYPES = [
    { id: "", label: "Everything" },
    { id: "HELPLINE", label: "Helplines" },
    { id: "NGO", label: "Organisations" },
    { id: "SHELTER", label: "Shelters" },
    { id: "LEGAL_AID", label: "Legal aid" },
    { id: "POLICE", label: "Police" },
];

/**
 * Helplines and organisations.
 *
 * National numbers are pinned to the top and never filtered out by city,
 * because 112 works everywhere and someone scanning this page in a hurry
 * should hit it first.
 */
export default function DirectoryPage() {
    const { user } = useAuth();
    const [orgs, setOrgs] = useState([]);
    const [cities, setCities] = useState([]);
    const [city, setCity] = useState(user?.displayCity || "");
    const [type, setType] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        api.directoryCities().then(setCities).catch(() => setCities([]));
    }, []);

    useEffect(() => {
        setLoading(true);
        api.directory({ city, type })
            .then(setOrgs)
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    }, [city, type]);

    const national = orgs.filter((o) => o.national);
    const local = orgs.filter((o) => !o.national);

    return (
        <div className="shell">
            <div className="page-head">
                <h1>Where to get help</h1>
                <p>
                    Numbers you can call now. The first few work anywhere in India;
                    below those are organisations in specific cities.
                </p>
            </div>

            <div className="filter-row">
                <select
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    style={{ maxWidth: 220 }}
                >
                    <option value="">All cities</option>
                    {cities.map((name) => (
                        <option key={name} value={name}>{name}</option>
                    ))}
                </select>
            </div>

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

            {error && <div className="notice notice-error">{error}</div>}
            {loading && <p style={{ color: "var(--ink-faint)" }}>Loading…</p>}

            {national.length > 0 && (
                <>
                    <h2 className="directory-heading">Anywhere in India</h2>
                    <ul className="org-list">
                        {national.map((org) => (
                            <li key={org.id}><OrgCard org={org} /></li>
                        ))}
                    </ul>
                </>
            )}

            {local.length > 0 && (
                <>
                    <h2 className="directory-heading">
                        {city ? `In ${city}` : "By city"}
                    </h2>
                    <ul className="org-list">
                        {local.map((org) => (
                            <li key={org.id}><OrgCard org={org} /></li>
                        ))}
                    </ul>
                </>
            )}

            {!loading && orgs.length === 0 && (
                <div className="empty">
                    <h2>Nothing listed here yet</h2>
                    <p>Try a different city, or clear the filters.</p>
                </div>
            )}
        </div>
    );
}

function OrgCard({ org }) {
    return (
        <div className="org">
            <div className="org-head">
                <h3>{org.name}</h3>
                {org.available24x7 && <span className="org-badge">24 hours</span>}
            </div>

            <p className="org-type">
                {typeLabel(org.orgType)}
                {org.city && ` · ${org.city}`}
            </p>

            {org.description && <p className="org-body">{org.description}</p>}

            <div className="org-actions">
                {org.phone && (
                    /* tel: opens the dialler with the number filled in — one tap
                       rather than reading and retyping it. */
                    <a href={`tel:${org.phone}`} className="btn btn-primary">
                        Call {org.phone}
                    </a>
                )}
                {org.website && (
                    <a
                        href={org.website}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn btn-outline"
                    >
                        Website
                    </a>
                )}
            </div>

            {!org.verified && (
                <p className="org-unverified">
                    We have not confirmed these details ourselves. Check the
                    organisation's own website before relying on them.
                </p>
            )}
        </div>
    );
}

function typeLabel(type) {
    return {
        HELPLINE: "Helpline",
        NGO: "Organisation",
        SHELTER: "Shelter",
        LEGAL_AID: "Legal aid",
        POLICE: "Police",
    }[type] || type;
}
