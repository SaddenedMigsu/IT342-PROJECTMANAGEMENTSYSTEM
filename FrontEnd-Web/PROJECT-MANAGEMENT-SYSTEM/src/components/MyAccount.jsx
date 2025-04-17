import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  TextField,
  Button,
  Avatar,
  IconButton,
  Paper,
} from "@mui/material";
import { ArrowBack, CameraAlt } from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import AdminLayout from "./AdminLayout";

const MyAccount = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    contactNumber: "",
  });
  const [profileImage, setProfileImage] = useState(null);

  useEffect(() => {
    // TODO: Fetch user data from backend
    // For now using mock data
    setFormData({
      firstName: "Juan",
      lastName: "Dela Cruz",
      email: "juan_delacruz@cit.edu",
      contactNumber: "+63 912 345 6789",
    });
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleImageChange = (event) => {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setProfileImage(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // TODO: Implement save functionality
    console.log("Saving profile:", formData);
  };

  return (
    <AdminLayout>
      <Box sx={{ p: 3, maxWidth: "1200px", margin: "0 auto" }}>
        {/* Header */}
        <Box sx={{ display: "flex", alignItems: "center", mb: 4 }}>
          <IconButton
            onClick={() => navigate(-1)}
            sx={{ mr: 2, bgcolor: "white", "&:hover": { bgcolor: "#f5f5f5" } }}
          >
            <ArrowBack />
          </IconButton>
          <Typography variant="h4" sx={{ fontWeight: 600 }}>
            My Account
          </Typography>
        </Box>

        <Paper
          sx={{
            p: 4,
            borderRadius: 2,
            bgcolor: "white",
            boxShadow: "0 2px 10px rgba(0,0,0,0.05)",
          }}
        >
          <Typography
            variant="h5"
            sx={{
              color: "#8B0000",
              fontWeight: 600,
              mb: 4,
            }}
          >
            Personal Information
          </Typography>

          <Box sx={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
            {/* Profile Image Section */}
            <Box
              sx={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                minWidth: "200px",
              }}
            >
              <Box sx={{ position: "relative" }}>
                <Avatar
                  src={profileImage}
                  sx={{
                    width: 200,
                    height: 200,
                    border: "4px solid white",
                    boxShadow: "0 2px 10px rgba(0,0,0,0.1)",
                  }}
                />
                <IconButton
                  component="label"
                  sx={{
                    position: "absolute",
                    bottom: 10,
                    right: 10,
                    bgcolor: "white",
                    "&:hover": { bgcolor: "#f5f5f5" },
                    boxShadow: "0 2px 5px rgba(0,0,0,0.2)",
                  }}
                >
                  <input
                    type="file"
                    hidden
                    accept="image/*"
                    onChange={handleImageChange}
                  />
                  <CameraAlt />
                </IconButton>
              </Box>
            </Box>

            {/* Form Fields */}
            <Box sx={{ flex: 1 }}>
              <form onSubmit={handleSubmit}>
                <Box
                  sx={{
                    display: "grid",
                    gridTemplateColumns: "repeat(2, 1fr)",
                    gap: 3,
                  }}
                >
                  <TextField
                    label="First Name"
                    name="firstName"
                    value={formData.firstName}
                    onChange={handleChange}
                    required
                    fullWidth
                    variant="outlined"
                    sx={{
                      "& .MuiOutlinedInput-root": {
                        bgcolor: "#f8fafc",
                      },
                    }}
                  />
                  <TextField
                    label="Last Name"
                    name="lastName"
                    value={formData.lastName}
                    onChange={handleChange}
                    required
                    fullWidth
                    variant="outlined"
                    sx={{
                      "& .MuiOutlinedInput-root": {
                        bgcolor: "#f8fafc",
                      },
                    }}
                  />
                  <TextField
                    label="Email Address"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    fullWidth
                    variant="outlined"
                    sx={{
                      gridColumn: "1 / -1",
                      "& .MuiOutlinedInput-root": {
                        bgcolor: "#f8fafc",
                      },
                    }}
                  />
                  <TextField
                    label="Contact Number"
                    name="contactNumber"
                    value={formData.contactNumber}
                    onChange={handleChange}
                    fullWidth
                    variant="outlined"
                    sx={{
                      gridColumn: "1 / -1",
                      "& .MuiOutlinedInput-root": {
                        bgcolor: "#f8fafc",
                      },
                    }}
                  />
                </Box>

                <Box
                  sx={{ mt: 4, display: "flex", justifyContent: "flex-end" }}
                >
                  <Button
                    type="submit"
                    variant="contained"
                    sx={{
                      bgcolor: "#DAA520",
                      color: "white",
                      px: 4,
                      py: 1,
                      "&:hover": {
                        bgcolor: "#B8860B",
                      },
                    }}
                  >
                    Save Changes
                  </Button>
                </Box>
              </form>
            </Box>
          </Box>
        </Paper>
      </Box>
    </AdminLayout>
  );
};

export default MyAccount;
