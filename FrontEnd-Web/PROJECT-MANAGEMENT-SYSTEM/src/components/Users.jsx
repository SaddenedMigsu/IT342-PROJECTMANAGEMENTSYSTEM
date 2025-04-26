import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Typography,
  Paper,
  InputAdornment,
  TextField,
  Select,
  MenuItem,
  IconButton,
  Avatar,
  Chip,
  Button,
  Pagination,
  Stack,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Checkbox,
} from "@mui/material";
import { Search, MoreVert, Delete } from "@mui/icons-material";
import AdminLayout from "./AdminLayout";
import userService from "../services/userService";
import authService from "../services/authService";

// Color palette for avatars
const avatarColors = [
  "#8B0000", // Maroon
  "#D4A017", // Golden Brown
  "#2E5090", // Navy Blue
  "#8B4513", // Saddle Brown
  "#006400", // Dark Green
  "#800080", // Purple
  "#FF8C00", // Dark Orange
  "#4B0082", // Indigo
  "#B22222", // Fire Brick
  "#2F4F4F", // Dark Slate Gray
];

// Function to get color based on name
const getAvatarColor = (name) => {
  // Special case for Primo Blue
  if (name === "Primo Blue") {
    return "#2563eb"; // Royal Blue
  }
  const index = name
    .split("")
    .reduce((acc, char) => acc + char.charCodeAt(0), 0);
  return avatarColors[index % avatarColors.length];
};

const Users = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState("");
  const [selectedRoles, setSelectedRoles] = useState(["All"]);
  const [page, setPage] = useState(1);
  const [rowsPerPage] = useState(8);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [availableRoles, setAvailableRoles] = useState(["All"]);
  const [imageLoadErrors, setImageLoadErrors] = useState({});

  // Check authentication on component mount
  useEffect(() => {
    const checkAuth = () => {
      if (!authService.isAuthenticated()) {
        navigate("/admin/login");
        return;
      }
    };
    checkAuth();
  }, [navigate]);

  // Fetch users from the backend
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        if (!authService.isAuthenticated()) {
          navigate("/admin/login");
          return;
        }

        setLoading(true);
        const data = await userService.getAllUsers();
        
        // Transform the data to match our component's structure
        const transformedUsers = data.map((user) => {
          // Handle different date formats
          let createdDate;
          if (user.createdAt) {
            if (typeof user.createdAt === 'object' && user.createdAt.seconds) {
              // Handle Firestore timestamp
              createdDate = new Date(user.createdAt.seconds * 1000).toISOString();
            } else if (typeof user.createdAt === 'string') {
              // Handle string date
              createdDate = new Date(user.createdAt).toISOString();
            } else {
              // Handle timestamp in milliseconds
              createdDate = new Date(user.createdAt).toISOString();
            }
          } else {
            console.warn(`No creation date for user ${user.userId}`);
            createdDate = new Date(0).toISOString(); // Use epoch time as fallback
          }

          return {
            id: user.userId,
            name: `${user.firstName} ${user.lastName}`.trim(),
            role: user.role || 'User',
            email: user.email,
            status: user.status || "Active",
            createdAt: createdDate,
            profilePicture: user.profilePicture || null
          };
        });

        console.log('Transformed users with dates:', transformedUsers.slice(0, 3));

        // Extract unique roles from users
        const roles = [...new Set(transformedUsers.map((user) => user.role))];
        setAvailableRoles(["All", ...roles]);

        setUsers(transformedUsers);
        setError(null);
      } catch (err) {
        console.error("Error fetching users:", err);
        if (err.response?.status === 401 || err.response?.status === 403) {
          authService.logout();
          navigate("/admin/login");
        } else {
          setError("Failed to load users. Please try again later.");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, [navigate]);

  // Reset page when search query or roles change
  useEffect(() => {
    setPage(1);
  }, [searchQuery, selectedRoles]);

  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
  };

  const handleSortChange = (event) => {
    console.log('Sort value changed to:', event.target.value);
    setSortBy(event.target.value);
  };

  const handleRoleChange = (role) => {
    setSelectedRoles((prev) => {
      if (role === "All") {
        return ["All"];
      }
      
      const newRoles = prev.includes(role)
        ? prev.filter((r) => r !== role)
        : [...prev.filter((r) => r !== "All"), role];
      
      return newRoles.length === 0 ? ["All"] : newRoles;
    });
  };

  const handlePageChange = (event, newPage) => {
    setPage(newPage);
  };

  const handleDeleteClick = (user) => {
    setUserToDelete(user);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await userService.deleteUser(userToDelete.id);
      setUsers(users.filter((user) => user.id !== userToDelete.id));
      setDeleteDialogOpen(false);
      setUserToDelete(null);
      setDeleteError(null);
    } catch (error) {
      console.error("Error deleting user:", error);
      setDeleteError(error.response?.data || "Failed to delete user");
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setUserToDelete(null);
    setDeleteError(null);
  };

  const handleImageError = (userId) => {
    setImageLoadErrors(prev => ({
      ...prev,
      [userId]: true
    }));
  };

  // Filter and sort users
  const getFilteredAndSortedUsers = () => {
    let filtered = users;

    // Apply search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (user) =>
          user.name.toLowerCase().includes(query) ||
          user.email.toLowerCase().includes(query) ||
          user.role.toLowerCase().includes(query)
      );
    }

    // Apply role filter
    if (!selectedRoles.includes("All")) {
      filtered = filtered.filter((user) => selectedRoles.includes(user.role));
    }

    // Create a new array for sorting to avoid mutating the original
    const sortedUsers = [...filtered];

    // Only sort if a sort option is selected
    if (sortBy) {
      sortedUsers.sort((a, b) => {
        const dateA = new Date(a.createdAt).getTime();
        const dateB = new Date(b.createdAt).getTime();
        
        return sortBy === "Newest" ? dateB - dateA : dateA - dateB;
      });
    }

    return sortedUsers;
  };

  // Add effect to log when sort changes
  useEffect(() => {
    console.log('Sort changed to:', sortBy);
  }, [sortBy]);

  const filteredAndSortedUsers = getFilteredAndSortedUsers();
  const totalPages = Math.ceil(filteredAndSortedUsers.length / rowsPerPage);
  const startIndex = (page - 1) * rowsPerPage;
  const displayedUsers = filteredAndSortedUsers.slice(
    startIndex,
    startIndex + rowsPerPage
  );

  const renderUserRow = (user) => (
    <Box
      key={user.id}
      sx={{
        display: "flex",
        alignItems: "center",
        p: 2,
        borderBottom: "1px solid",
        borderColor: "divider",
      }}
    >
      <Avatar
        src={imageLoadErrors[user.id] ? null : user.profilePicture}
        alt={user.name}
        imgProps={{
          crossOrigin: "anonymous",
          referrerPolicy: "no-referrer",
          loading: "lazy",
          onError: (e) => {
            console.error('Error loading profile picture:', e);
            handleImageError(user.id);
          }
        }}
        sx={{
          width: 40,
          height: 40,
          bgcolor: imageLoadErrors[user.id] || !user.profilePicture ? getAvatarColor(user.name) : 'transparent',
          fontSize: "1rem",
          fontWeight: 600,
          border: '2px solid white',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
          transition: 'transform 0.2s ease-in-out',
          '&:hover': {
            transform: 'scale(1.1)'
          },
          '& img': {
            objectFit: 'cover',
            width: '100%',
            height: '100%'
          }
        }}
        onError={() => handleImageError(user.id)}
      >
        {(imageLoadErrors[user.id] || !user.profilePicture) && user.name
          .split(" ")
          .map((n) => n[0])
          .join("")}
      </Avatar>
      <Typography
        sx={{ 
          color: "#1a1f36", 
          fontSize: "0.875rem", 
          fontWeight: 500,
          transition: 'color 0.2s ease',
          '&:hover': {
            color: '#8B0000'
          }
        }}
      >
        {user.name}
      </Typography>
    </Box>
  );

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
            Users
          </Typography>
        </Box>

        {/* Search and Filter Section */}
        <Box 
          sx={{ 
            display: "flex", 
            gap: 2, 
            alignItems: 'center', 
            mb: 4,
            flexWrap: "wrap",
            position: "relative",
            zIndex: 1
          }}
        >
          <TextField
            placeholder="Search users..."
            value={searchQuery}
            onChange={handleSearchChange}
            sx={{
              flex: { xs: '1 1 100%', sm: '1 1 280px' },
              maxWidth: { sm: 320 },
              '& .MuiOutlinedInput-root': {
                borderRadius: '16px',
                height: '45px',
                transition: 'all 0.2s',
                bgcolor: '#ffffff',
                border: '1px solid #E2E8F0',
                '&:hover': {
                  borderColor: '#8B0000',
                  bgcolor: '#ffffff',
                },
                '&.Mui-focused': {
                  borderColor: '#8B0000',
                  bgcolor: '#ffffff',
                  '& .MuiOutlinedInput-notchedOutline': {
                    border: 'none'
                  }
                },
                '& .MuiOutlinedInput-notchedOutline': {
                  border: 'none'
                }
              },
              '& .MuiInputBase-input': {
                color: '#1a1f36',
                '&::placeholder': {
                  color: '#64748B',
                  opacity: 1
                }
              }
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search sx={{ color: "#64748B", ml: 0.5 }} />
                </InputAdornment>
              ),
            }}
          />

          <FormControl sx={{ minWidth: 220, flex: { xs: '1 1 100%', sm: 'initial' } }}>
            <Select
              displayEmpty
              value={selectedRoles}
              multiple
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.length === 1 && selected[0] === "All" ? (
                    <Typography sx={{ color: '#64748B' }}>All Roles</Typography>
                  ) : (
                    selected.map((role) => (
                      <Chip
                        key={role}
                        label={role}
                        size="small"
                        sx={{
                          bgcolor: 'rgba(139, 0, 0, 0.08)',
                          color: '#8B0000',
                          fontWeight: 500,
                          borderRadius: '8px',
                          '&:hover': {
                            bgcolor: 'rgba(139, 0, 0, 0.12)',
                          },
                        }}
                      />
                    ))
                  )}
                </Box>
              )}
              sx={{
                height: '45px',
                borderRadius: '16px',
                bgcolor: '#ffffff',
                border: '1px solid #E2E8F0',
                '&:hover': {
                  borderColor: '#8B0000',
                  bgcolor: '#ffffff',
                },
                '&.Mui-focused': {
                  borderColor: '#8B0000',
                  bgcolor: '#ffffff',
                },
                '& .MuiOutlinedInput-notchedOutline': {
                  border: 'none'
                },
                '& .MuiSelect-select': {
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1,
                  padding: '8px 14px',
                  color: '#1a1f36',
                },
              }}
              MenuProps={{
                PaperProps: {
                  sx: {
                    maxHeight: 300,
                    mt: 1,
                    borderRadius: '16px',
                    boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
                    '& .MuiMenuItem-root': {
                      py: 1.2,
                      px: 2,
                    },
                  },
                },
              }}
            >
              {availableRoles.map((role) => (
                <MenuItem 
                  key={role} 
                  value={role}
                  onClick={() => handleRoleChange(role)}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    borderRadius: '8px',
                    mx: 0.8,
                    my: 0.3,
                    '&.Mui-selected': {
                      backgroundColor: 'rgba(139, 0, 0, 0.08)',
                    },
                    '&.Mui-selected:hover': {
                      backgroundColor: 'rgba(139, 0, 0, 0.12)',
                    },
                    '&:hover': {
                      backgroundColor: 'rgba(139, 0, 0, 0.04)',
                    },
                  }}
                >
                  <Checkbox 
                    checked={selectedRoles.includes(role)}
                    sx={{
                      color: '#8B0000',
                      borderRadius: '6px',
                      '&.Mui-checked': {
                        color: '#8B0000',
                      },
                      '& .MuiSvgIcon-root': {
                        fontSize: 20,
                      },
                    }}
                  />
                  <Typography
                    sx={{
                      fontWeight: selectedRoles.includes(role) ? 600 : 400,
                      color: selectedRoles.includes(role) ? '#8B0000' : '#1a1f36',
                    }}
                  >
                    {role}
                  </Typography>
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl sx={{ minWidth: 180, flex: { xs: '1 1 100%', sm: 'initial' } }}>
            <Select
              value={sortBy}
              onChange={handleSortChange}
              displayEmpty
              renderValue={(value) => (
                <Typography sx={{ color: value ? '#1a1f36' : '#64748B' }}>
                  {value || "Sort by"}
                </Typography>
              )}
              sx={{
                height: '45px',
                borderRadius: '16px',
                bgcolor: '#ffffff',
                border: '1px solid #E2E8F0',
                '&:hover': {
                  borderColor: '#8B0000',
                  bgcolor: '#ffffff',
                },
                '&.Mui-focused': {
                  borderColor: '#8B0000',
                  bgcolor: '#ffffff',
                },
                '& .MuiOutlinedInput-notchedOutline': {
                  border: 'none'
                },
                '& .MuiSelect-select': {
                  display: 'flex',
                  alignItems: 'center',
                  fontSize: '0.875rem',
                  fontWeight: 500,
                  padding: '8px 14px',
                },
              }}
              MenuProps={{
                PaperProps: {
                  sx: {
                    mt: 1,
                    borderRadius: '16px',
                    boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
                  },
                },
              }}
            >
              <MenuItem
                value=""
                onClick={() => setSortBy("")}
                sx={{
                  fontSize: '0.875rem',
                  py: 1.2,
                  px: 2,
                  borderRadius: '8px',
                  mx: 0.8,
                  my: 0.3,
                  fontWeight: !sortBy ? 600 : 400,
                  color: !sortBy ? '#8B0000' : '#1a1f36',
                  '&:hover': {
                    backgroundColor: 'rgba(139, 0, 0, 0.04)',
                  },
                  '&.Mui-selected': {
                    backgroundColor: 'rgba(139, 0, 0, 0.08)',
                    color: '#8B0000',
                    fontWeight: 600,
                    '&:hover': {
                      backgroundColor: 'rgba(139, 0, 0, 0.12)',
                    },
                  },
                }}
              >
                None
              </MenuItem>
              <MenuItem
                value="Newest"
                onClick={() => setSortBy("Newest")}
                sx={{
                  fontSize: '0.875rem',
                  py: 1.2,
                  px: 2,
                  borderRadius: '8px',
                  mx: 0.8,
                  my: 0.3,
                  fontWeight: sortBy === "Newest" ? 600 : 400,
                  color: sortBy === "Newest" ? '#8B0000' : '#1a1f36',
                  '&:hover': {
                    backgroundColor: 'rgba(139, 0, 0, 0.04)',
                  },
                  '&.Mui-selected': {
                    backgroundColor: 'rgba(139, 0, 0, 0.08)',
                    color: '#8B0000',
                    fontWeight: 600,
                    '&:hover': {
                      backgroundColor: 'rgba(139, 0, 0, 0.12)',
                    },
                  },
                }}
              >
                Newest
              </MenuItem>
              <MenuItem
                value="Oldest"
                onClick={() => setSortBy("Oldest")}
                sx={{
                  fontSize: '0.875rem',
                  py: 1.2,
                  px: 2,
                  borderRadius: '8px',
                  mx: 0.8,
                  my: 0.3,
                  fontWeight: sortBy === "Oldest" ? 600 : 400,
                  color: sortBy === "Oldest" ? '#8B0000' : '#1a1f36',
                  '&:hover': {
                    backgroundColor: 'rgba(139, 0, 0, 0.04)',
                  },
                  '&.Mui-selected': {
                    backgroundColor: 'rgba(139, 0, 0, 0.08)',
                    color: '#8B0000',
                    fontWeight: 600,
                    '&:hover': {
                      backgroundColor: 'rgba(139, 0, 0, 0.12)',
                    },
                  },
                }}
              >
                Oldest
              </MenuItem>
            </Select>
          </FormControl>
        </Box>

        {/* Error message */}
        {error && (
          <Alert 
            severity="error" 
            sx={{ 
              mb: 3,
              borderRadius: '16px',
              border: '1px solid rgba(239, 68, 68, 0.2)',
              bgcolor: 'rgba(239, 68, 68, 0.05)',
              position: "relative",
              zIndex: 1,
              '& .MuiAlert-icon': {
                color: '#ef4444'
              }
            }}
          >
            {error}
          </Alert>
        )}

        {/* Users Table */}
        <Paper
          elevation={0}
          sx={{
            position: "relative",
            zIndex: 1,
            borderRadius: '24px',
            overflow: "hidden",
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
          {/* Table Header */}
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: "2.5fr 1fr 2fr 1fr 1fr",
              borderBottom: "1px solid #E2E8F0",
              bgcolor: "#f8fafc",
              p: 2,
            }}
          >
            <Typography
              sx={{ color: "#1a1f36", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Name
            </Typography>
            <Typography
              sx={{ color: "#1a1f36", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Role
            </Typography>
            <Typography
              sx={{ color: "#1a1f36", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Email
            </Typography>
            <Typography
              sx={{ color: "#1a1f36", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Status
            </Typography>
            <Typography
              sx={{ color: "#1a1f36", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Action
            </Typography>
          </Box>

          {/* Loading indicator */}
          {loading ? (
            <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
              <CircularProgress sx={{ color: "#8B0000" }} />
            </Box>
          ) : displayedUsers.length > 0 ? (
            displayedUsers.map((user) => (
              <Box
                key={user.id}
                sx={{
                  display: "grid",
                  gridTemplateColumns: "2.5fr 1fr 2fr 1fr 1fr",
                  borderBottom: "1px solid #E2E8F0",
                  p: 2,
                  bgcolor: "white",
                  transition: "all 0.2s ease",
                  "&:hover": { 
                    bgcolor: "#f8fafc",
                    transform: 'translateX(4px)'
                  },
                  "&:last-child": { borderBottom: "none" },
                }}
              >
                <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                  <Avatar
                    src={imageLoadErrors[user.id] ? null : user.profilePicture}
                    alt={user.name}
                    imgProps={{
                      crossOrigin: "anonymous",
                      referrerPolicy: "no-referrer",
                      loading: "lazy",
                      onError: (e) => {
                        console.error('Error loading profile picture:', e);
                        handleImageError(user.id);
                      }
                    }}
                    sx={{
                      width: 40,
                      height: 40,
                      bgcolor: imageLoadErrors[user.id] || !user.profilePicture ? getAvatarColor(user.name) : 'transparent',
                      fontSize: "1rem",
                      fontWeight: 600,
                      border: '2px solid white',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                      transition: 'transform 0.2s ease-in-out',
                      '&:hover': {
                        transform: 'scale(1.1)'
                      },
                      '& img': {
                        objectFit: 'cover',
                        width: '100%',
                        height: '100%'
                      }
                    }}
                    onError={() => handleImageError(user.id)}
                  >
                    {(imageLoadErrors[user.id] || !user.profilePicture) && user.name
                      .split(" ")
                      .map((n) => n[0])
                      .join("")}
                  </Avatar>
                  <Typography
                    sx={{ 
                      color: "#1a1f36", 
                      fontSize: "0.875rem", 
                      fontWeight: 500,
                      transition: 'color 0.2s ease',
                      '&:hover': {
                        color: '#8B0000'
                      }
                    }}
                  >
                    {user.name}
                  </Typography>
                </Box>
                <Typography sx={{ color: "#64748B", fontSize: "0.875rem" }}>
                  {user.role}
                </Typography>
                <Typography sx={{ color: "#64748B", fontSize: "0.875rem" }}>
                  {user.email}
                </Typography>
                <Box>
                  <Chip
                    label={user.status}
                    size="small"
                    sx={{
                      bgcolor: user.status === "Active" ? "rgba(22, 163, 74, 0.1)" : "rgba(239, 68, 68, 0.1)",
                      color: user.status === "Active" ? "#16a34a" : "#ef4444",
                      fontWeight: 600,
                      fontSize: "0.75rem",
                      borderRadius: '8px',
                      border: `1px solid ${user.status === "Active" ? "rgba(22, 163, 74, 0.2)" : "rgba(239, 68, 68, 0.2)"}`,
                    }}
                  />
                </Box>
                <Box>
                  <IconButton
                    size="small"
                    onClick={() => handleDeleteClick(user)}
                    sx={{
                      color: "#8B0000",
                      transition: 'all 0.2s ease',
                      "&:hover": { 
                        bgcolor: "rgba(139, 0, 0, 0.1)",
                        transform: 'scale(1.1)'
                      },
                    }}
                  >
                    <Delete sx={{ fontSize: 20 }} />
                  </IconButton>
                </Box>
              </Box>
            ))
          ) : (
            <Box sx={{ p: 3, textAlign: "center" }}>
              <Typography sx={{ color: "#64748B" }}>
                {searchQuery
                  ? "No users found matching your search"
                  : "No users found"}
              </Typography>
            </Box>
          )}
        </Paper>

        {/* Pagination */}
        {filteredAndSortedUsers.length > 0 && (
          <Box
            sx={{
              mt: 3,
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              px: 1,
              position: "relative",
              zIndex: 1,
            }}
          >
            <Typography sx={{ color: "#64748B", fontSize: "0.875rem" }}>
              Showing {startIndex + 1} to{" "}
              {Math.min(startIndex + rowsPerPage, filteredAndSortedUsers.length)}{" "}
              of {filteredAndSortedUsers.length} entries
            </Typography>
            <Stack spacing={2}>
              <Pagination
                count={totalPages}
                page={page}
                onChange={handlePageChange}
                shape="rounded"
                showFirstButton
                showLastButton
                sx={{
                  '& .MuiPaginationItem-root': {
                    color: "#64748B",
                    borderRadius: '8px',
                    transition: 'all 0.2s ease',
                    '&.Mui-selected': {
                      bgcolor: "#8B0000",
                      color: "white",
                      '&:hover': {
                        bgcolor: "#6B0000",
                      },
                    },
                    '&:hover': {
                      bgcolor: "rgba(139, 0, 0, 0.1)",
                    },
                  },
                }}
              />
            </Stack>
          </Box>
        )}
      </Box>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        PaperProps={{
          sx: {
            borderRadius: '24px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
            padding: 2
          }
        }}
      >
        <DialogTitle 
          sx={{
            fontSize: '1.25rem',
            fontWeight: 600,
            color: '#1a1f36',
            pb: 1
          }}
        >
          Delete User
        </DialogTitle>
        <DialogContent>
          <Typography sx={{ color: '#64748B', mb: 2 }}>
            Are you sure you want to delete {userToDelete?.name}? This action
            cannot be undone.
          </Typography>
          {deleteError && (
            <Alert 
              severity="error" 
              sx={{ 
                mt: 2,
                borderRadius: '16px',
                border: '1px solid rgba(239, 68, 68, 0.2)',
                bgcolor: 'rgba(239, 68, 68, 0.05)',
              }}
            >
              {deleteError}
            </Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button 
            onClick={handleDeleteCancel}
            sx={{
              color: '#64748B',
              '&:hover': {
                bgcolor: 'rgba(100, 116, 139, 0.1)',
              },
              borderRadius: '12px',
              textTransform: 'none',
              fontWeight: 500
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleDeleteConfirm}
            variant="contained"
            sx={{
              bgcolor: '#ef4444',
              color: 'white',
              '&:hover': {
                bgcolor: '#dc2626',
              },
              borderRadius: '12px',
              textTransform: 'none',
              fontWeight: 500,
              px: 3
            }}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </AdminLayout>
  );
};

export default Users;
