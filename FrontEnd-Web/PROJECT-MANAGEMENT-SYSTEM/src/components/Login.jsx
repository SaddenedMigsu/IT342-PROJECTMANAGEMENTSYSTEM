import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./Login.css";
import cituLogo from "../assets/citu-logo.png";
import authService from "../services/authService";
import { Visibility, VisibilityOff } from "@mui/icons-material";

export default function Login() {
  const [formData, setFormData] = useState({
    identifier: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  // Clear any existing auth data on component mount
  useEffect(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
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
      const response = await authService.login(
        formData.identifier,
        formData.password
      );
      if (response && response.token) {
        // Check if user has admin role
        const userData = JSON.parse(localStorage.getItem("user"));
        if (userData && userData.role === "ADMIN") {
          navigate("/admin/dashboard");
        } else {
          // If not admin, log them out and show error
          authService.logout();
          setError("Access denied. Only administrators can access this system.");
        }
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
        <h2 className="modal-title">ADMIN LOG IN</h2>
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
          <div className="password-input-container">
            <input
              type={showPassword ? "text" : "password"}
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
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? "Hide password" : "Show password"}
            >
              {showPassword ? <VisibilityOff /> : <Visibility />}
            </button>
          </div>
          <button
            type="submit"
            className="login-button"
            disabled={loading}
            aria-busy={loading}
          >
            {loading ? "LOGGING IN..." : "LOGIN"}
          </button>
        </form>
      </div>
    </div>
  );
}
