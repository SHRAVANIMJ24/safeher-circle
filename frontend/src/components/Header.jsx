import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import Handle from "./Handle";

export default function Header() {
    const { user, signOut } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [unread, setUnread] = useState(false);

    /*
       Checked on each navigation rather than polled. Someone will land on a
       page shortly after anything happens, and a timer running against the
       server every few seconds is not worth a dot.
    */
    useEffect(() => {
        if (!user) {
            setUnread(false);
            return;
        }
        api.exchangesUnread()
            .then((result) => setUnread(result.unread))
            .catch(() => setUnread(false));
    }, [user, location.pathname]);

    function handleSignOut() {
        signOut();
        navigate("/");
    }

    return (
        <header className="masthead">
            <div className="masthead-inner">
                <Link to="/" className="wordmark">SafeHer Circle</Link>
                <Link to="/help" className="btn">Get help</Link>
                <Link to="/donations" className="btn">Give &amp; receive</Link>
                <span className="masthead-spacer" />
                <nav>
                    {user ? (
                        <>
                            <Handle handle={user.anonHandle} />
                            <Link to="/safety" className="btn">Alarm</Link>
                            <Link to="/fake-call" className="btn">Fake call</Link>
                            <Link to="/exchanges" className="btn">
                                Exchanges
                                {unread && (
                                    <span
                                        className="unread-dot"
                                        aria-label="new activity"
                                    />
                                )}
                            </Link>
                            {(user.role === "MODERATOR" || user.role === "ADMIN") && (
                                <Link to="/moderation" className="btn">Queue</Link>
                            )}
                            <Link to="/new" className="btn btn-primary">Write a post</Link>
                            <button className="btn" onClick={handleSignOut}>Sign out</button>
                        </>
                    ) : (
                        <>
                            <Link to="/sign-in" className="btn">Sign in</Link>
                            <Link to="/join" className="btn btn-primary">Join</Link>
                        </>
                    )}
                </nav>
            </div>
        </header>
    );
}
