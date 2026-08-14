import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import Handle from "./Handle";

/**
 * The header carries three public destinations, an alarm, and a menu.
 *
 * The previous version put eight links in one row, which wrapped on a laptop
 * and broke the wordmark in two. The fix is not smaller type — it is admitting
 * that these are three different kinds of destination:
 *
 *   public    the board, help, give and receive — always visible
 *   urgent    the alarm — visually separate, findable without reading
 *   personal  posts, exchanges, sign out — behind the person's own handle
 *
 * An emergency control does not belong in a row of equals, and a person's own
 * things belong behind their own name.
 */
export default function Header() {
    const { user, signOut } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const [unread, setUnread] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);
    const menuRef = useRef(null);

    useEffect(() => {
        if (!user) {
            setUnread(false);
            return;
        }
        api.exchangesUnread()
            .then((result) => setUnread(result.unread))
            .catch(() => setUnread(false));
    }, [user, location.pathname]);

    // Close the menu on navigation, on Escape, and on a click elsewhere.
    useEffect(() => setMenuOpen(false), [location.pathname]);

    useEffect(() => {
        if (!menuOpen) return;

        function onKey(event) {
            if (event.key === "Escape") setMenuOpen(false);
        }
        function onClick(event) {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setMenuOpen(false);
            }
        }

        document.addEventListener("keydown", onKey);
        document.addEventListener("mousedown", onClick);
        return () => {
            document.removeEventListener("keydown", onKey);
            document.removeEventListener("mousedown", onClick);
        };
    }, [menuOpen]);

    function handleSignOut() {
        signOut();
        navigate("/");
    }

    const isModerator = user?.role === "MODERATOR" || user?.role === "ADMIN";

    return (
        <header className="masthead">
            <div className="masthead-inner">
                <Link to="/" className="wordmark">
                    <Mark />
                    <span>SafeHer&nbsp;Circle</span>
                </Link>

                <nav className="masthead-links" aria-label="Sections">
                    <NavLink to="/" end className="masthead-link">Board</NavLink>
                    <NavLink to="/help" className="masthead-link">Get help</NavLink>
                    <NavLink to="/donations" className="masthead-link">Give &amp; receive</NavLink>
                </nav>

                <span className="masthead-spacer" />

                {user ? (
                    <div className="masthead-actions">
                        {/*
                           The alarm is the only coloured control up here, and the
                           only one with an icon. In an emergency nobody reads a
                           row of labels.
                        */}
                        <Link to="/safety" className="alarm-link">
                            <AlarmIcon />
                            <span>Alarm</span>
                        </Link>

                        <div className="menu" ref={menuRef}>
                            <button
                                className="menu-trigger"
                                onClick={() => setMenuOpen((open) => !open)}
                                aria-expanded={menuOpen}
                                aria-haspopup="menu"
                            >
                                <Handle handle={user.anonHandle} />
                                {unread && <span className="unread-dot" aria-label="new activity" />}
                                <ChevronIcon />
                            </button>

                            {menuOpen && (
                                <div className="menu-panel" role="menu">
                                    <Link to="/new" className="menu-item" role="menuitem">
                                        Write a post
                                    </Link>
                                    <Link to="/exchanges" className="menu-item" role="menuitem">
                                        Your exchanges
                                        {unread && <span className="unread-dot" />}
                                    </Link>
                                    <Link to="/contacts" className="menu-item" role="menuitem">
                                        Trusted contacts
                                    </Link>
                                    <Link to="/fake-call" className="menu-item" role="menuitem">
                                        Fake call
                                    </Link>

                                    {isModerator && (
                                        <>
                                            <span className="menu-divider" />
                                            <Link to="/moderation" className="menu-item" role="menuitem">
                                                Moderation queue
                                            </Link>
                                        </>
                                    )}

                                    <span className="menu-divider" />
                                    <button
                                        className="menu-item"
                                        onClick={handleSignOut}
                                        role="menuitem"
                                    >
                                        Sign out
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                ) : (
                    <div className="masthead-actions">
                        <Link to="/sign-in" className="btn">Sign in</Link>
                        <Link to="/join" className="btn btn-primary">Join</Link>
                    </div>
                )}
            </div>
        </header>
    );
}

function AlarmIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 3a6 6 0 0 0-6 6c0 4-1.5 5.5-1.5 5.5h15S18 13 18 9a6 6 0 0 0-6-6z" />
            <path d="M10.5 18a1.8 1.8 0 0 0 3 0" />
        </svg>
    );
}

function ChevronIcon() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"
             className="chevron">
            <path d="M6 9l6 6 6-6" />
        </svg>
    );
}

/**
 * An open ring with a dot in the opening.
 *
 * The name is literally a circle, but an open one — a support circle is
 * something you can join, not a wall. The dot is someone at the entrance.
 * Two elements only, so it still reads at 16px.
 */
function Mark() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true" className="mark">
            <path d="M19.05 7.25 A8.5 8.5 0 1 0 19.05 16.75"
                  fill="none" stroke="currentColor" strokeWidth="2"
                  strokeLinecap="round" />
            <circle cx="20.5" cy="12" r="1.7" className="mark-dot" />
        </svg>
    );
}
