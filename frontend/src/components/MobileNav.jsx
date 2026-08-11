import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * Fixed bottom navigation, mobile only.
 *
 * The alarm is given its own centred, visually distinct slot rather than
 * sitting in a row of equals. On a safety app the emergency action should be
 * reachable with one thumb from any screen, without a menu, and findable
 * without reading — which is why it is the one item with colour.
 */
export default function MobileNav() {
    const { user } = useAuth();

    return (
        <nav className="mobile-nav" aria-label="Main">
            <NavLink to="/" end className="mobile-nav-item">
                <IconBoard />
                <span>Board</span>
            </NavLink>

            {user ? (
                <>
                    <NavLink to="/new" className="mobile-nav-item">
                        <IconWrite />
                        <span>Write</span>
                    </NavLink>

                    <NavLink to="/safety" className="mobile-nav-item mobile-nav-alarm">
                        <IconAlarm />
                        <span>Alarm</span>
                    </NavLink>

                    <NavLink to="/fake-call" className="mobile-nav-item">
                        <IconPhone />
                        <span>Fake call</span>
                    </NavLink>
                </>
            ) : (
                <>
                    <NavLink to="/sign-in" className="mobile-nav-item">
                        <IconWrite />
                        <span>Sign in</span>
                    </NavLink>
                    <NavLink to="/join" className="mobile-nav-item">
                        <IconPeople />
                        <span>Join</span>
                    </NavLink>
                </>
            )}
        </nav>
    );
}

/* Icons are inline so there is no library to load and no flash of missing
   glyphs on a slow connection. */

function IconBoard() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <rect x="3" y="4" width="18" height="16" rx="2" />
            <path d="M7 9h10M7 13h7" />
        </svg>
    );
}

function IconWrite() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M4 20h4l10-10a2.8 2.8 0 0 0-4-4L4 16v4z" />
            <path d="M13.5 6.5l4 4" />
        </svg>
    );
}
function IconPhone() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M6.6 10.8c1.4 2.8 3.8 5.1 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.4.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1C10.7 21 3 13.3 3 3.9c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.4 0 .8-.2 1l-2.3 2.3z" />
        </svg>
    );
}

function IconAlarm() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 3a6 6 0 0 0-6 6c0 4-1.5 5.5-1.5 5.5h15S18 13 18 9a6 6 0 0 0-6-6z" />
            <path d="M10.5 18a1.8 1.8 0 0 0 3 0" />
        </svg>
    );
}

function IconPeople() {
    return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
             strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <circle cx="9" cy="8" r="3.2" />
            <path d="M3.5 19c0-3 2.5-5 5.5-5s5.5 2 5.5 5" />
            <path d="M16 6.5a3 3 0 0 1 0 5.6M17.5 19c0-2-.7-3.6-2-4.6" />
        </svg>
    );
}
