/**
 * AR-F2: Hook owns all async/state logic. Components must stay declarative.
 * Re-exports useAuthContext as useAuth for ergonomics.
 */
export { useAuthContext as useAuth } from "../context/AuthContext";
