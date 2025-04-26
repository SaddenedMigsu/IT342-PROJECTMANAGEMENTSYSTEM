import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./Login.css";
import cituLogo from "../assets/citu-logo.png";
import authService from "../services/authService";

export default function Login() {
  const [formData, setFormData] = useState({
    identifier: "",
    password: ""
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // Clear any existing auth data on component mount
  useEffect(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.identifier || !formData.password) {
      setError("Please fill in all fields");
      return;
    }

    setError("");
    setLoading(true);

    try {
      const response = await authService.login(formData.identifier, formData.password);
      if (response && response.token) {
        navigate("/admin/dashboard");
      } else {
        setError("Invalid login response");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError(err.response?.data || "Failed to login. Please try again.");
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
        {error && (
          <div className="error-message" role="alert">
            {error}
          </div>
        )}
        <form onSubmit={handleSubmit} className="modal-form">
          <input
            type="text"
            name="identifier"
            value={formData.identifier}
            onChange={handleChange}
            placeholder="Enter ID no. or Email"
            className="input-field"
            required
            disabled={loading}
            aria-label="Identifier"
          />
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            placeholder="Password"
            className="input-field"
            required
            disabled={loading}
            aria-label="Password"
          />
          <button 
            type="submit" 
            className="login-button" 
            disabled={loading}
            aria-busy={loading}
          >
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
