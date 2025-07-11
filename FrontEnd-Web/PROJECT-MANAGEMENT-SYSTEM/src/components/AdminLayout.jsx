import React, { useState, useEffect } from "react";
import { Box, Typography, Avatar } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import {
  TrendingUp,
  Group,
  CalendarToday,
  Person,
  Logout,
} from "@mui/icons-material";
import pmsLogo from "../assets/pms-logo.png";
import userService from "../services/userService";

const AdminLayout = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [userProfile, setUserProfile] = useState({
    firstName: "",
    lastName: "",
    email: "",
    profilePicture: null,
  });

  // Function to check if menu item is active
  const isActive = (path) => {
    return location.pathname === path;
  };

  // Menu item styles
  const getMenuItemStyles = (path) => ({
    display: "flex",
    alignItems: "center",
    p: 1.5,
    borderRadius: 1,
    mb: 1,
    cursor: "pointer",
    bgcolor: isActive(path) ? "#EEF2FF" : "transparent",
    color: isActive(path) ? "#4F46E5" : "#64748B",
    "&:hover": {
      bgcolor: isActive(path) ? "#EEF2FF" : "#f1f5f9",
    },
  });

  // Icon styles
  const getIconStyles = (path) => ({
    color: "inherit",
    mr: 2,
    fontSize: 20,
  });

  // Text styles
  const getTextStyles = (path) => ({
    color: "inherit",
    fontSize: "14px",
    fontWeight: 500,
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
  });

  useEffect(() => {
    const fetchUserProfile = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        // Try to get cached user data first
        const cachedUserData = localStorage.getItem("user");
        let initialData = null;
        
        if (cachedUserData) {
          try {
            initialData = JSON.parse(cachedUserData);
            setUserProfile({
              firstName: initialData.firstName || "",
              lastName: initialData.lastName || "",
              email: initialData.email || "",
              profilePicture: initialData.profilePicture || null,
            });
          } catch (e) {
            console.error("Error parsing cached user data:", e);
            localStorage.removeItem("user");
          }
        }

        // Fetch fresh data from API
        const userData = await userService.getCurrentProfile();
        if (userData) {
          const newUserData = {
            firstName: userData.firstName || "",
            lastName: userData.lastName || "",
            email: userData.email || "",
            profilePicture: userData.profilePicture || null,
          };
          
          setUserProfile(newUserData);
          localStorage.setItem("user", JSON.stringify(newUserData));
        }
      } catch (error) {
        console.error("Error fetching user profile:", error);
        if (error.response?.status === 401 || error.response?.status === 403) {
          localStorage.removeItem("token");
          localStorage.removeItem("user");
          navigate("/login");
        }
      }
    };

    const handleProfileUpdate = (event) => {
      if (event.detail) {
        setUserProfile({
          firstName: event.detail.firstName || "",
          lastName: event.detail.lastName || "",
          email: event.detail.email || "",
          profilePicture: event.detail.profilePicture || null,
        });
      }
    };

    // Initial profile fetch
    fetchUserProfile();

    // Listen for profile updates
    window.addEventListener("profileUpdated", handleProfileUpdate);

    return () => {
      window.removeEventListener("profileUpdated", handleProfileUpdate);
    };
  }, [navigate]);

  const handleSignOut = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/login");
  };

  return (
    <Box
      sx={{
        display: "flex",
        minHeight: "100vh",
        width: "100vw",
        margin: 0,
        padding: 0,
        bgcolor: "#f8fafc",
        overflow: "hidden",
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
      }}
    >
      {/* Sidebar */}
      <Box
        sx={{
          width: "240px",
          minWidth: "240px",
          bgcolor: "white",
          borderRight: "1px solid #e2e8f0",
          height: "100vh",
          position: "fixed",
          left: 0,
          top: 0,
          p: 3,
          display: "flex",
          flexDirection: "column",
          overflowY: "auto",
          zIndex: 1,
        }}
      >
        <Box sx={{ mb: 6 }}>
          <Box
            component="div"
            onClick={() => navigate("/admin/dashboard")}
            sx={{
              cursor: "pointer",
              display: "flex",
              justifyContent: "center",
              "&:hover": {
                opacity: 0.8,
              },
              transition: "opacity 0.2s ease-in-out",
            }}
          >
            <img
              src={pmsLogo}
              alt="PMS Logo"
              style={{
                width: "100%",
                maxWidth: "150px",
                height: "auto",
                display: "block",
              }}
            />
          </Box>
        </Box>

        <Box
          sx={getMenuItemStyles("/admin/dashboard")}
          onClick={() => navigate("/admin/dashboard")}
        >
          <TrendingUp sx={getIconStyles("/admin/dashboard")} />
          <Typography sx={getTextStyles("/admin/dashboard")}>
            Dashboard
          </Typography>
        </Box>

        <Box
          sx={getMenuItemStyles("/admin/users")}
          onClick={() => navigate("/admin/users")}
        >
          <Group sx={getIconStyles("/admin/users")} />
          <Typography sx={getTextStyles("/admin/users")}>Users</Typography>
        </Box>

        <Box
          sx={getMenuItemStyles("/admin/schedule")}
          onClick={() => navigate("/admin/schedule")}
        >
          <CalendarToday sx={getIconStyles("/admin/schedule")} />
          <Typography sx={getTextStyles("/admin/schedule")}>
            Schedule
          </Typography>
        </Box>

        <Box
          sx={getMenuItemStyles("/admin/profile")}
          onClick={() => navigate("/admin/profile")}
        >
          <Person sx={getIconStyles("/admin/profile")} />
          <Typography sx={getTextStyles("/admin/profile")}>
            My Account
          </Typography>
        </Box>

        {/* User Profile */}
        <Box sx={{ mt: "auto", pt: 3, borderTop: "1px solid #e2e8f0" }}>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              px: 1.5,
              py: 1,
              cursor: "pointer",
              borderRadius: 1,
              "&:hover": {
                bgcolor: "#f1f5f9",
              },
            }}
            onClick={() => navigate("/admin/profile")}
          >
            <Avatar
              src={userProfile.profilePicture}
              alt={`${userProfile.firstName} ${userProfile.lastName}`}
              sx={{
                width: 40,
                height: 40,
                mr: 2,
                bgcolor: userProfile.profilePicture ? 'transparent' : '#8B0000',
              }}
            >
              {!userProfile.profilePicture && userProfile.firstName && userProfile.lastName ? 
                `${userProfile.firstName[0]}${userProfile.lastName[0]}` : ""}
            </Avatar>
            <Box>
              <Typography
                sx={{
                  fontSize: "0.875rem",
                  fontWeight: 500,
                  color: "#1a1f36",
                  lineHeight: 1.2,
                }}
              >
                {userProfile.firstName} {userProfile.lastName}
              </Typography>
              <Typography
                sx={{
                  fontSize: "0.75rem",
                  color: "#64748B",
                  lineHeight: 1.2,
                }}
              >
                {userProfile.email}
              </Typography>
            </Box>
          </Box>

          {/* Sign Out Button */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              px: 1.5,
              py: 1,
              mt: 0.5,
              cursor: "pointer",
              borderRadius: 1,
              "&:hover": {
                bgcolor: "#f1f5f9",
              },
            }}
            onClick={handleSignOut}
          >
            <Logout
              sx={{
                fontSize: 16,
                mr: 1.5,
                color: "#8B0000",
              }}
            />
            <Typography
              sx={{
                fontSize: "0.875rem",
                fontWeight: 400,
                color: "#64748B",
              }}
            >
              Sign Out
            </Typography>
          </Box>
        </Box>
      </Box>

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 2,
          bgcolor: "#f8fafc",
          minHeight: "100vh",
          marginLeft: "240px",
          maxWidth: "calc(100% - 240px)",
          overflow: "auto",
        }}
      >
        {children}
      </Box>
    </Box>
  );
};

export default AdminLayout;
