import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import Handle from "./Handle";

export default function Header() {
    const { user, signOut } = useAuth();
    const navigate = useNavigate();

    function handleSignOut() {
        signOut();
        navigate("/");
    }

    return (
        <header className="masthead">
            <div className="masthead-inner">
                <Link to="/" className="wordmark">SafeHer Circle</Link>
                <span className="masthead-spacer" />
                <nav>
                    {user ? (
                        <>
                            <Handle handle={user.anonHandle} />
                            <Link to="/safety" className="btn">Alarm</Link>
                            <Link to="/fake-call" className="btn">Fake call</Link>
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
