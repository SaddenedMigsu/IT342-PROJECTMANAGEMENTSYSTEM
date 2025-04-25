import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  TextField,
  Button,
  Avatar,
  IconButton,
  Paper,
  Alert,
  Snackbar,
  CircularProgress,
} from "@mui/material";
import { ArrowBack, CameraAlt } from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import AdminLayout from "./AdminLayout";
import userService from "../services/userService";

const MyAccount = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phoneNumber: "",
  });
  const [profileImage, setProfileImage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        setInitialLoading(true);
        // Check if user is authenticated
        const token = localStorage.getItem("token");
        if (!token) {
          navigate("/login");
          return;
        }

        const userData = await userService.getCurrentProfile();
        console.log("Fetched user data:", userData); // Debug log
        if (userData) {
          setFormData({
            firstName: userData.firstName || "",
            lastName: userData.lastName || "",
            email: userData.email || "",
            phoneNumber: userData.phoneNumber || "",
          });
          
          // Set profile image if it exists
          if (userData.profilePicture) {
            setProfileImage(userData.profilePicture);
          }
          
          // Update localStorage with latest data
          localStorage.setItem("user", JSON.stringify(userData));
          
          // Dispatch event to update other components
          window.dispatchEvent(new CustomEvent("profileUpdated", {
            detail: userData
          }));
        }
      } catch (error) {
        console.error("Error fetching user data:", error);
        if (error.response?.status === 401 || error.response?.status === 403) {
          localStorage.removeItem("token");
          localStorage.removeItem("user");
          navigate("/login");
        } else {
          setError("Failed to load user data. Please try again.");
        }
      } finally {
        setInitialLoading(false);
      }
    };

    fetchUserData();
  }, [navigate]);

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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      // Create updates object with all fields
      const updates = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        phoneNumber: formData.phoneNumber,
      };

      // Only include profile image if it has changed and is not a URL
      if (profileImage && !profileImage.startsWith('http')) {
        updates.profilePicture = profileImage;
      }

      const updatedProfile = await userService.updateUserProfile(updates);
      
      if (updatedProfile) {
        // Update form data with the response
        setFormData(prev => ({
          ...prev,
          firstName: updatedProfile.firstName || prev.firstName,
          lastName: updatedProfile.lastName || prev.lastName,
          email: updatedProfile.email || prev.email,
          phoneNumber: updatedProfile.phoneNumber || prev.phoneNumber,
        }));

        // Update profile image if it exists in the response
        if (updatedProfile.profilePicture) {
          setProfileImage(updatedProfile.profilePicture);
        }

        // Update local storage with new user data
        localStorage.setItem("user", JSON.stringify(updatedProfile));

        // Dispatch event to notify other components of profile update
        window.dispatchEvent(new CustomEvent("profileUpdated", {
          detail: updatedProfile
        }));

        setSuccess(true);
      }
    } catch (error) {
      console.error("Error updating profile:", error);
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        navigate("/login");
      } else {
        setError(
          error.response?.data || 
          "Failed to update profile. Please try again."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading) {
    return (
      <AdminLayout>
        <Box
          sx={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            minHeight: "400px",
          }}
        >
          <CircularProgress sx={{ color: "#8B0000" }} />
        </Box>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <Box
        sx={{
          p: { xs: 2, sm: 3, md: 4 },
          width: "100%",
          maxWidth: "100%",
          bgcolor: "#ffffff",
          minHeight: "100vh",
          position: "relative",
          "&::before": {
            content: '""',
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            height: "200px",
            background: "linear-gradient(to right, #8B0000, #6B0000)",
            zIndex: 0,
          }
        }}
      >
        {/* Header */}
        <Box
          sx={{
            position: "relative",
            zIndex: 1,
            display: "flex",
            alignItems: "center",
            mb: 4,
          }}
        >
          <Typography 
            variant="h5" 
            sx={{ 
              fontWeight: 700,
              color: '#ffffff',
              fontSize: { xs: "1.5rem", sm: "1.75rem" },
              textShadow: '0 2px 4px rgba(0,0,0,0.1)',
              letterSpacing: '-0.5px'
            }}
          >
            My Account
          </Typography>
        </Box>

        <Paper
          elevation={0}
          sx={{
            position: "relative",
            zIndex: 1,
            p: { xs: 3, sm: 4 },
            borderRadius: '24px',
            bgcolor: "white",
            border: '1px solid rgba(226, 232, 240, 0.8)',
            backdropFilter: 'blur(20px)',
            boxShadow: '0 20px 40px rgba(0,0,0,0.08)',
            transition: 'all 0.3s ease',
            '&:hover': {
              transform: 'translateY(-4px)',
              boxShadow: '0 30px 60px rgba(0,0,0,0.12)'
            }
          }}
        >
          <Typography
            variant="h6"
            sx={{
              color: "#1a1f36",
              fontWeight: 700,
              mb: 4,
              fontSize: '1.25rem',
              position: 'relative',
              '&::after': {
                content: '""',
                position: 'absolute',
                bottom: '-8px',
                left: 0,
                width: '40px',
                height: '3px',
                background: 'linear-gradient(to right, #8B0000, #6B0000)',
                borderRadius: '4px'
              }
            }}
          >
            Personal Information
          </Typography>

          <Box sx={{ 
            display: "flex", 
            gap: { xs: 4, md: 8 }, 
            flexWrap: "wrap",
            alignItems: "flex-start"
          }}>
            {/* Profile Image Section */}
            <Box
              sx={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                minWidth: { xs: "100%", sm: "240px" },
                mb: { xs: 4, sm: 0 }
              }}
            >
              <Box 
                sx={{ 
                  position: "relative",
                }}
              >
                <Avatar
                  src={profileImage}
                  alt={`${formData.firstName} ${formData.lastName}`}
                  sx={{
                    width: { xs: 140, sm: 180 },
                    height: { xs: 140, sm: 180 },
                    bgcolor: "#8B0000",
                    fontSize: "3rem",
                    border: '4px solid white',
                    position: 'relative',
                    boxShadow: '0 4px 14px rgba(0,0,0,0.1)',
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      transform: 'scale(1.05)'
                    }
                  }}
                >
                  {formData.firstName && formData.lastName
                    ? `${formData.firstName[0]}${formData.lastName[0]}`
                    : ""}
                </Avatar>
                <IconButton
                  component="label"
                  sx={{
                    position: "absolute",
                    bottom: 5,
                    right: 5,
                    bgcolor: "#8B0000",
                    color: "white",
                    width: 44,
                    height: 44,
                    '&:hover': {
                      bgcolor: "#6B0000",
                      transform: 'scale(1.1)',
                    },
                    boxShadow: '0 4px 12px rgba(139, 0, 0, 0.3)',
                    transition: 'all 0.2s ease',
                    zIndex: 2
                  }}
                >
                  <input
                    type="file"
                    hidden
                    accept="image/*"
                    onChange={handleImageChange}
                  />
                  <CameraAlt sx={{ fontSize: 22 }} />
                </IconButton>
              </Box>
              <Typography
                sx={{
                  mt: 3,
                  color: "#64748b",
                  fontSize: "0.875rem",
                  fontWeight: 500,
                  textAlign: "center",
                  maxWidth: "200px",
                  lineHeight: 1.5
                }}
              >
                Click the camera icon to update your profile picture
              </Typography>
            </Box>

            {/* Form Section */}
            <Box sx={{ flex: 1, minWidth: { xs: "100%", sm: "300px" } }}>
              <form onSubmit={handleSubmit}>
                <Box sx={{ 
                  display: "grid", 
                  gap: 3,
                  gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" }
                }}>
                  <TextField
                    name="firstName"
                    label="First Name"
                    value={formData.firstName}
                    onChange={handleChange}
                    fullWidth
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '16px',
                        bgcolor: '#f8fafc',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: '#f1f5f9',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                          borderColor: '#8B0000',
                        },
                        '&.Mui-focused': {
                          bgcolor: '#ffffff',
                          '& .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#8B0000',
                            borderWidth: '2px',
                          }
                        }
                      },
                      '& .MuiInputLabel-root': {
                        '&.Mui-focused': {
                          color: '#8B0000'
                        }
                      }
                    }}
                  />
                  <TextField
                    name="lastName"
                    label="Last Name"
                    value={formData.lastName}
                    onChange={handleChange}
                    fullWidth
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '16px',
                        bgcolor: '#f8fafc',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: '#f1f5f9',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                          borderColor: '#8B0000',
                        },
                        '&.Mui-focused': {
                          bgcolor: '#ffffff',
                          '& .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#8B0000',
                            borderWidth: '2px',
                          }
                        }
                      },
                      '& .MuiInputLabel-root': {
                        '&.Mui-focused': {
                          color: '#8B0000'
                        }
                      }
                    }}
                  />
                  <TextField
                    name="email"
                    label="Email Address"
                    value={formData.email}
                    onChange={handleChange}
                    fullWidth
                    sx={{
                      gridColumn: { xs: '1', md: '1 / -1' },
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '16px',
                        bgcolor: '#f8fafc',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: '#f1f5f9',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                          borderColor: '#8B0000',
                        },
                        '&.Mui-focused': {
                          bgcolor: '#ffffff',
                          '& .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#8B0000',
                            borderWidth: '2px',
                          }
                        }
                      },
                      '& .MuiInputLabel-root': {
                        '&.Mui-focused': {
                          color: '#8B0000'
                        }
                      }
                    }}
                  />
                  <TextField
                    name="phoneNumber"
                    label="Phone Number"
                    value={formData.phoneNumber}
                    onChange={handleChange}
                    fullWidth
                    sx={{
                      gridColumn: { xs: '1', md: '1 / -1' },
                      '& .MuiOutlinedInput-root': {
                        borderRadius: '16px',
                        bgcolor: '#f8fafc',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: '#f1f5f9',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                          borderColor: '#8B0000',
                        },
                        '&.Mui-focused': {
                          bgcolor: '#ffffff',
                          '& .MuiOutlinedInput-notchedOutline': {
                            borderColor: '#8B0000',
                            borderWidth: '2px',
                          }
                        }
                      },
                      '& .MuiInputLabel-root': {
                        '&.Mui-focused': {
                          color: '#8B0000'
                        }
                      }
                    }}
                  />
                </Box>

                {error && (
                  <Alert 
                    severity="error" 
                    sx={{ 
                      mt: 3, 
                      borderRadius: '16px',
                      border: '1px solid rgba(239, 68, 68, 0.2)',
                      bgcolor: 'rgba(239, 68, 68, 0.05)',
                      '& .MuiAlert-icon': {
                        color: '#ef4444'
                      }
                    }}
                  >
                    {error}
                  </Alert>
                )}

                <Button
                  type="submit"
                  variant="contained"
                  disabled={loading}
                  sx={{
                    mt: 4,
                    background: 'linear-gradient(45deg, #8B0000, #6B0000)',
                    color: "white",
                    px: 6,
                    py: 1.5,
                    borderRadius: '16px',
                    textTransform: 'none',
                    fontSize: '1rem',
                    fontWeight: 600,
                    boxShadow: '0 8px 16px rgba(139, 0, 0, 0.2)',
                    '&:hover': {
                      background: 'linear-gradient(45deg, #6B0000, #8B0000)',
                      transform: 'translateY(-2px)',
                      boxShadow: '0 12px 20px rgba(139, 0, 0, 0.3)',
                    },
                    transition: 'all 0.3s ease',
                    position: 'relative',
                    overflow: 'hidden',
                    '&::after': {
                      content: '""',
                      position: 'absolute',
                      top: 0,
                      left: '-100%',
                      width: '200%',
                      height: '100%',
                      background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent)',
                      transition: 'all 0.5s ease',
                    },
                    '&:hover::after': {
                      left: '100%',
                    }
                  }}
                >
                  {loading ? (
                    <CircularProgress size={24} sx={{ color: "white" }} />
                  ) : (
                    "Save Changes"
                  )}
                </Button>
              </form>
            </Box>
          </Box>
        </Paper>
      </Box>

      <Snackbar
        open={success}
        autoHideDuration={6000}
        onClose={() => setSuccess(false)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert 
          onClose={() => setSuccess(false)} 
          severity="success"
          sx={{ 
            width: '100%',
            borderRadius: '16px',
            bgcolor: 'rgba(22, 163, 74, 0.05)',
            border: '1px solid rgba(22, 163, 74, 0.2)',
            color: '#16a34a',
            '& .MuiAlert-icon': {
              color: '#16a34a'
            }
          }}
        >
          Profile updated successfully!
        </Alert>
      </Snackbar>
    </AdminLayout>
  );
};

export default MyAccount;