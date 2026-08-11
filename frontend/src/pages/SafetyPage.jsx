import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { currentPosition } from "../api/location";
import { ALARM_TYPES, playAlarm, previewAlarm } from "../audio/alarms";

/**
 * The alarm screen.
 *
 * Two things are deliberately separate here: the sound and the alert. Someone
 * may want noise without telling anyone, or may want their contacts told
 * without a sound giving them away. Neither should force the other.
 */
export default function SafetyPage() {
    const [alarmType, setAlarmType] = useState("SIREN");
    const [sounding, setSounding] = useState(false);
    const [alert, setAlert] = useState(null);
    const [contacts, setContacts] = useState([]);
    const [status, setStatus] = useState(null);
    const [error, setError] = useState(null);

    // Holds the stop function returned by playAlarm.
    const stopRef = useRef(null);

    useEffect(() => {
        api.contacts().then(setContacts).catch(() => setContacts([]));
        api.activeAlert().then(setAlert).catch(() => setAlert(null));

        // If this component goes away while sounding, silence it.
        return () => {
            if (stopRef.current) stopRef.current();
        };
    }, []);

    function toggleSound() {
        if (sounding) {
            stopRef.current?.();
            stopRef.current = null;
            setSounding(false);
        } else {
            stopRef.current = playAlarm(alarmType);
            setSounding(true);
        }
    }

    async function raiseAlert() {
        setError(null);
        setStatus("Getting your location…");

        const position = await currentPosition();

        setStatus("Telling your contacts…");
        try {
            const created = await api.triggerSos({
                ...(position || {}),
                triggerMethod: "BUTTON",
                alarmType,
            });
            setAlert(created);
            setStatus(null);
        } catch (err) {
            setError(err.message);
            setStatus(null);
        }
    }

    async function markSafe() {
        try {
            const resolved = await api.markSafe(alert.id);
            setAlert(null);
            const time = new Date(resolved.resolvedAt).toLocaleTimeString();
            setStatus(`Marked safe at ${time}.`);
            setTimeout(() => setStatus(null), 4000);
            stopRef.current?.();
            stopRef.current = null;
            setSounding(false);
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <div className="shell">
            <div className="page-head">
                <h1>Alarm</h1>
                <p>
                    The sound and the alert are separate. Use whichever fits the
                    situation, or both.
                </p>
            </div>

            {error && <div className="notice notice-error">{error}</div>}

            {alert && (
                <div className="alert-live">
                    <div>
                        <strong>An alert is live.</strong>
                        <p style={{ margin: "4px 0 0", fontSize: 14 }}>
                            Raised at {new Date(alert.triggeredAt).toLocaleTimeString()}.{" "}
                            {alert.contactsNotified === 1
                                ? "1 contact was told."
                                : `${alert.contactsNotified} contacts were told.`}
                        </p>
                    </div>
                    <button className="btn btn-safe" onClick={markSafe}>
                        I'm safe
                    </button>
                </div>
            )}

            <div className="panic-area">
                <button
                    className={sounding ? "panic panic-on" : "panic"}
                    onClick={toggleSound}
                    aria-pressed={sounding}
                >
                    {sounding ? "Stop the sound" : "Sound the alarm"}
                </button>

                <button
                    className="btn btn-outline"
                    onClick={raiseAlert}
                    disabled={!!alert || !!status}
                >
                    {status || (alert ? "Contacts already told" : "Tell my contacts")}
                </button>
            </div>

            <section style={{ marginTop: 36 }}>
                <h2 style={{ fontSize: 19, marginBottom: 4 }}>Which sound</h2>
                <p style={{ color: "var(--ink-soft)", fontSize: 15, marginTop: 0 }}>
                    Listen to these now, somewhere safe, so you know what you are
                    choosing between.
                </p>

                <div className="sound-list">
                    {ALARM_TYPES.map((option) => (
                        <label
                            key={option.id}
                            className={
                                alarmType === option.id ? "sound sound-on" : "sound"
                            }
                        >
                            <input
                                type="radio"
                                name="alarmType"
                                value={option.id}
                                checked={alarmType === option.id}
                                onChange={() => setAlarmType(option.id)}
                            />
                            <span className="sound-text">
                                <strong>{option.label}</strong>
                                <span>{option.description}</span>
                            </span>
                            <button
                                type="button"
                                className="btn btn-outline"
                                onClick={(e) => {
                                    e.preventDefault();
                                    previewAlarm(option.id);
                                }}
                            >
                                Play 1s
                            </button>
                        </label>
                    ))}
                </div>
            </section>

            <section style={{ marginTop: 36 }}>
                <h2 style={{ fontSize: 19, marginBottom: 4 }}>Who gets told</h2>
                {contacts.length === 0 ? (
                    <div className="notice notice-quiet">
                        You have not added anyone yet, so "Tell my contacts" will
                        raise an alert that reaches nobody.{" "}
                        <Link to="/contacts">Add someone now</Link>.
                    </div>
                ) : (
                    <>
                        <ul className="contact-list">
                            {contacts.map((contact) => (
                                <li key={contact.id}>
                                    <strong>{contact.name}</strong>
                                    <span>{contact.relationship || "Contact"}</span>
                                </li>
                            ))}
                        </ul>
                        <p style={{ marginTop: 10 }}>
                            <Link to="/contacts">Manage contacts</Link>
                        </p>
                    </>
                )}
            </section>
        </div>
    );
}
