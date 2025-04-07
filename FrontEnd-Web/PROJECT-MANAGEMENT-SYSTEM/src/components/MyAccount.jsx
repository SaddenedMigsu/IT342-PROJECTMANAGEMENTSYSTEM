import React, { useState } from 'react';
import './MyAccount.css';
import cituLogo from '../assets/citu-logo.png';
import { Link } from 'react-router-dom';

const MyAccount = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    contactNumber: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log('Form submitted:', formData);
  };

  return (
    <div className="my-account-container">
      {/* Sidebar */}
      <div className="sidebar">
        <div className="logo-container">
          <img src={cituLogo} alt="CIT-U Logo" className="logo" />
          <h2 className="pms-text">PMS</h2>
        </div>

        <nav className="nav-menu">
          <Link to="/dashboard" className="nav-item">
            <i className="nav-icon">📊</i>
            <span>Dashboard</span>
          </Link>
          <Link to="/faculty-list" className="nav-item">
            <i className="nav-icon">👨‍🏫</i>
            <span>Faculty List</span>
          </Link>
          <Link to="/schedule" className="nav-item">
            <i className="nav-icon">📅</i>
            <span>Schedule</span>
          </Link>
        </nav>

        <div className="user-profile">
          <div className="profile-photo">
            <img src="https://via.placeholder.com/40" alt="User" />
          </div>
          <div className="user-info">
            <p className="user-name">Juan Dela Cruz</p>
            <p className="user-email">juan_delacruz@cit.edu</p>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="main-content">
        <div className="header">
          <button className="back-button">←</button>
          <h1 className="page-title">My Account</h1>
        </div>

        <div className="personal-info-section">
          <h2 className="section-title">Personal Information</h2>

          <div className="profile-form">
            <div className="profile-photo-container">
              <img
                src="https://via.placeholder.com/200"
                alt="Profile"
                className="profile-photo-large"
              />
            </div>

            <form onSubmit={handleSubmit} className="info-form">
              <div className="form-group">
                <label htmlFor="firstName">First name<span className="required">*</span></label>
                <input
                  type="text"
                  id="firstName"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="lastName">Last name<span className="required">*</span></label>
                <input
                  type="text"
                  id="lastName"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="email">Email address<span className="required">*</span></label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="form-input"
                  required
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="contactNumber">Contact number</label>
                <input
                  type="tel"
                  id="contactNumber"
                  name="contactNumber"
                  value={formData.contactNumber}
                  onChange={handleChange}
                  className="form-input"
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="save-button">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MyAccount;
