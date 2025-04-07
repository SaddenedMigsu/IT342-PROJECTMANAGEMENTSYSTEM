import React, { useState } from "react";
import "./SignUp.css";
import cituLogo from "../assets/citu-logo.png";
import { Link } from "react-router-dom";


export default function Signup() {
  const [formData, setFormData] = useState({
    studentId: "",
    firstName: "",
    lastName: "",
    course: "",
    email: "",
    password: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevState) => ({
      ...prevState,
      [name]: value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log(formData);
  };

  return (
    <div className="signup-container">
      <div className="signup-modal">
        <img src={cituLogo} alt="CIT-U Logo" className="citu-logo" />
        <h1 className="signup-title">Sign Up</h1>
        
        <form className="signup-form" onSubmit={handleSubmit}>
          <input
            className="signup-input"
            type="text"
            name="studentId"
            placeholder="Student ID no."
            value={formData.studentId}
            onChange={handleChange}
          />
          <input
            className="signup-input"
            type="text"
            name="firstName"
            placeholder="First Name"
            value={formData.firstName}
            onChange={handleChange}
          />
          <input
            className="signup-input"
            type="text"
            name="lastName"
            placeholder="Last Name"
            value={formData.lastName}
            onChange={handleChange}
          />
          <input
            className="signup-input"
            type="text"
            name="course"
            placeholder="Course"
            value={formData.course}
            onChange={handleChange}
          />
          <input
            className="signup-input"
            type="email"
            name="email"
            placeholder="Institutional Email"
            value={formData.email}
            onChange={handleChange}
          />
          <input
            className="signup-input"
            type="password"
            name="password"
            placeholder="Password"
            value={formData.password}
            onChange={handleChange}
          />
          <button className="signup-button" type="submit">SIGN UP</button>
        </form>
        
        <p className="login-link-text">
          Already have an account? <Link to="/" className="login-link">Log in</Link>
        </p>
      </div>
    </div>
  );
}
