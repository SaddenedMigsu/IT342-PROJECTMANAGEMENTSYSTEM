import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./Login.css";
import cituLogo from "../assets/citu-logo.png";
import authService from "../services/authService";

export default function Login() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await authService.login(identifier, password);
      console.log("Login successful:", response);

      // Store the token
      if (response.token) {
        localStorage.setItem("token", response.token);
      }

      // Redirect to admin dashboard
      navigate("/admin/dashboard");
    } catch (err) {
      console.error("Login error:", err);
      setError(
        err.response?.data?.message ||
          "Login failed. Please check your credentials and try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Blurred background content */}
      <div className="background-layer" />

      {/* Modal Login Card */}
      <div className="login-modal">
        <img src={cituLogo} alt="CIT-U Logo" className="modal-logo" />
        <h2 className="modal-title">Log in</h2>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit} className="modal-form">
          <input
            type="text"
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            placeholder="Enter ID no. or Email"
            className="input-field"
            required
            disabled={loading}
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Password"
            className="input-field"
            required
            disabled={loading}
          />
          <button type="submit" className="login-button" disabled={loading}>
            {loading ? "LOGGING IN..." : "LOGIN"}
          </button>
        </form>
        <p className="signup-text">
          Don't have an account?{" "}
          <Link to="/signup" className="signup-link">
            SIGN UP
          </Link>
        </p>
      </div>
    </div>
  );
}
