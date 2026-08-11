import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import Header from "./components/Header";
import FeedPage from "./pages/FeedPage";
import PostDetailPage from "./pages/PostDetailPage";
import SignInPage from "./pages/SignInPage";
import JoinPage from "./pages/JoinPage";
import NewPostPage from "./pages/NewPostPage";
import SafetyPage from "./pages/SafetyPage";
import ContactsPage from "./pages/ContactsPage";
import MobileNav from "./components/MobileNav";
/** Sends signed-out visitors to the sign-in page instead of a blank screen. */
function RequireAuth({ children }) {
    const { user, loading } = useAuth();
    if (loading) return null;
    if (!user) return <Navigate to="/sign-in" replace />;
    return children;
}

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Header />
                <main>
                    <Routes>
                        <Route path="/" element={<FeedPage />} />
                        <Route path="/posts/:id" element={<PostDetailPage />} />
                        <Route path="/sign-in" element={<SignInPage />} />
                        <Route path="/join" element={<JoinPage />} />
                        <Route
                            path="/new"
                            element={
                                <RequireAuth>
                                    <NewPostPage />
                                </RequireAuth>
                            }
                        />
                        <Route path="/safety" element={<RequireAuth><SafetyPage /></RequireAuth>} />
                        <Route path="/contacts" element={<RequireAuth><ContactsPage /></RequireAuth>} />
                        <Route path="*" element={<Navigate to="/" replace />} />
                    </Routes>
                </main>
             
                <footer className="site-foot">
                    <div className="shell" style={{ paddingBottom: 0 }}>
                        This is a student project, not an emergency service. In an
                        emergency in India, dial <strong>112</strong>. The women's
                        helpline is <strong>181</strong>.
                    </div>
                </footer>
                <MobileNav />
            </AuthProvider>
        </BrowserRouter>
    );
}
