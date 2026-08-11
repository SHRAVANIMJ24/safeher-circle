import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { startRinging, startVibrating } from "../audio/ringtone";

/**
 * A fake incoming call.
 *
 * The purpose is an excuse to leave. Someone stuck in a conversation they want
 * out of can have their phone ring, answer it, say "I have to take this", and
 * walk away — without having to announce that they feel unsafe, which is
 * often the thing that escalates a situation.
 *
 * Everything is stored in component state and nothing is sent anywhere. There
 * is no record that this was used.
 */

const DELAYS = [
    { seconds: 5, label: "5 seconds" },
    { seconds: 15, label: "15 seconds" },
    { seconds: 30, label: "30 seconds" },
    { seconds: 60, label: "1 minute" },
    { seconds: 300, label: "5 minutes" },
];

const STORAGE_KEY = "safeher.fakecall";

export default function FakeCallPage() {
    const navigate = useNavigate();

    const [caller, setCaller] = useState({ name: "Aai", subtitle: "mobile" });
    const [delay, setDelay] = useState(15);
    const [countdown, setCountdown] = useState(null);
    const [ringing, setRinging] = useState(false);
    const [answered, setAnswered] = useState(false);
    const [callSeconds, setCallSeconds] = useState(0);

    const stopSoundRef = useRef(null);
    const stopBuzzRef = useRef(null);

    // Remember the caller name between visits, so it does not have to be
    // retyped when someone is in a hurry.
    useEffect(() => {
        const saved = window.sessionStorage.getItem(STORAGE_KEY);
        if (saved) {
            try {
                setCaller(JSON.parse(saved));
            } catch {
                // Ignore malformed values.
            }
        }
    }, []);

    // The countdown to the call.
    useEffect(() => {
        if (countdown === null) return;

        if (countdown <= 0) {
            setCountdown(null);
            beginRinging();
            return;
        }

        const timer = setTimeout(() => setCountdown((n) => n - 1), 1000);
        return () => clearTimeout(timer);
    }, [countdown]);

    // The call timer, once answered.
    useEffect(() => {
        if (!answered) return;
        const timer = setInterval(() => setCallSeconds((n) => n + 1), 1000);
        return () => clearInterval(timer);
    }, [answered]);

    // Whatever happens, do not leave the phone ringing.
    useEffect(() => {
        return () => {
            stopSoundRef.current?.();
            stopBuzzRef.current?.();
        };
    }, []);

    function schedule() {
        window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(caller));
        setCountdown(delay);
    }

    function beginRinging() {
        stopSoundRef.current = startRinging();
        stopBuzzRef.current = startVibrating();
        setRinging(true);
    }

    function answer() {
        stopSoundRef.current?.();
        stopBuzzRef.current?.();
        stopSoundRef.current = null;
        stopBuzzRef.current = null;
        setAnswered(true);
    }

    function hangUp() {
        stopSoundRef.current?.();
        stopBuzzRef.current?.();
        stopSoundRef.current = null;
        stopBuzzRef.current = null;
        setRinging(false);
        setAnswered(false);
        setCallSeconds(0);
    }

    // ---- the call screen ----

    if (ringing || answered) {
        return (
            <div className="call-screen">
                <div className="call-top">
                    <p className="call-status">
                        {answered ? formatDuration(callSeconds) : "Incoming call"}
                    </p>
                    <h1 className="call-name">{caller.name}</h1>
                    <p className="call-subtitle">{caller.subtitle}</p>
                </div>

                <div className="call-avatar" aria-hidden="true">
                    {initial(caller.name)}
                </div>

                <div className="call-actions">
                    {!answered && (
                        <button className="call-btn call-answer" onClick={answer}>
                            <PhoneIcon />
                            <span>Answer</span>
                        </button>
                    )}
                    <button className="call-btn call-decline" onClick={hangUp}>
                        <PhoneIcon down />
                        <span>{answered ? "End" : "Decline"}</span>
                    </button>
                </div>

                {answered && (
                    <p className="call-hint">
                        Say something and walk away. Nothing is being recorded or sent.
                    </p>
                )}
            </div>
        );
    }

    // ---- the countdown ----

    if (countdown !== null) {
        return (
            <div className="shell">
                <div className="panel" style={{ textAlign: "center" }}>
                    <h1>Ringing in {countdown}</h1>
                    <p className="panel-intro" style={{ marginTop: 10 }}>
                        Put your phone away. It will ring on its own.
                    </p>
                    <button
                        className="btn btn-outline btn-block"
                        onClick={() => setCountdown(null)}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        );
    }

    // ---- setup ----

    return (
        <div className="shell">
            <div className="panel panel-wide">
                <h1>Fake call</h1>
                <p className="panel-intro">
                    Schedule your phone to ring, so you have a reason to step away
                    from a conversation without explaining yourself.
                </p>

                <div className="stack">
                    <label className="field">
                        <span className="field-label">Who is calling</span>
                        <span className="field-hint">
                            Use a name that would not surprise anyone nearby.
                        </span>
                        <input
                            type="text"
                            value={caller.name}
                            onChange={(e) =>
                                setCaller((c) => ({ ...c, name: e.target.value }))
                            }
                            maxLength={40}
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">Label under the name</span>
                        <input
                            type="text"
                            value={caller.subtitle}
                            onChange={(e) =>
                                setCaller((c) => ({ ...c, subtitle: e.target.value }))
                            }
                            maxLength={30}
                            placeholder="mobile"
                        />
                    </label>

                    <div className="field">
                        <span className="field-label">Ring after</span>
                        <div className="filters" style={{ paddingBottom: 0 }}>
                            {DELAYS.map((option) => (
                                <button
                                    key={option.seconds}
                                    type="button"
                                    className="chip"
                                    aria-pressed={delay === option.seconds}
                                    onClick={() => setDelay(option.seconds)}
                                >
                                    {option.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <button className="btn btn-primary btn-block" onClick={schedule}>
                        Schedule the call
                    </button>

                    <button
                        className="btn btn-outline btn-block"
                        onClick={beginRinging}
                    >
                        Ring right now
                    </button>
                </div>

                <p className="panel-foot">
                    This never leaves your phone. No call is placed and nothing is
                    saved to your account.
                </p>
            </div>
        </div>
    );
}

function PhoneIcon({ down }) {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="currentColor"
            aria-hidden="true"
            style={down ? { transform: "rotate(135deg)" } : undefined}
        >
            <path d="M6.6 10.8c1.4 2.8 3.8 5.1 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.4.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1C10.7 21 3 13.3 3 3.9c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.4 0 .8-.2 1l-2.3 2.3z" />
        </svg>
    );
}

function initial(name) {
    return (name || "?").trim().charAt(0).toUpperCase();
}

function formatDuration(totalSeconds) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${String(seconds).padStart(2, "0")}`;
}
