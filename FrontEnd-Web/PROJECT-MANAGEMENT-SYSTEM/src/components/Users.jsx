import React, { useState, useEffect } from "react";
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
} from "@mui/material";
import { Search, MoreVert, Delete } from "@mui/icons-material";
import AdminLayout from "./AdminLayout";
import userService from "../services/userService";

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
  const [users, setUsers] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [sortBy, setSortBy] = useState("Newest");
  const [selectedRole, setSelectedRole] = useState("All");
  const [page, setPage] = useState(1);
  const [rowsPerPage] = useState(8);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [availableRoles, setAvailableRoles] = useState(["All"]);

  // Fetch users from the backend
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        setLoading(true);
        const data = await userService.getAllUsers();

        // Transform the data to match our component's structure
        const transformedUsers = data.map((user) => ({
          id: user.userId,
          name: `${user.firstName} ${user.lastName}`,
          role: user.role,
          email: user.email,
          status: "Active", // Default status since backend doesn't provide it
          createdAt: user.createdAt,
        }));

        // Extract unique roles from users
        const roles = [...new Set(transformedUsers.map((user) => user.role))];
        setAvailableRoles(["All", ...roles]);

        setUsers(transformedUsers);
        setError(null);
      } catch (err) {
        console.error("Error fetching users:", err);
        setError("Failed to load users. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, []);

  // Reset page when search query or role changes
  useEffect(() => {
    setPage(1);
  }, [searchQuery, selectedRole]);

  const handleSearchChange = (event) => {
    setSearchQuery(event.target.value);
  };

  const handleSortChange = (event) => {
    setSortBy(event.target.value);
  };

  const handleRoleChange = (event) => {
    setSelectedRole(event.target.value);
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
    if (selectedRole !== "All") {
      filtered = filtered.filter((user) => user.role === selectedRole);
    }

    // Apply sorting
    return filtered.sort((a, b) => {
      switch (sortBy) {
        case "Newest":
          return new Date(b.createdAt) - new Date(a.createdAt);
        case "Oldest":
          return new Date(a.createdAt) - new Date(b.createdAt);
        case "Name":
          return a.name.localeCompare(b.name);
        default:
          return 0;
      }
    });
  };

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
        display: "grid",
        gridTemplateColumns: "2.5fr 1fr 2fr 1fr 1fr",
        borderBottom: "1px solid #E2E8F0",
        p: 2,
        bgcolor: "white",
        "&:hover": { bgcolor: "#F8FAFC" },
        "&:last-child": { borderBottom: "none" },
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
        <Avatar
          sx={{
            width: 32,
            height: 32,
            bgcolor: getAvatarColor(user.name),
            fontSize: "0.875rem",
            fontWeight: 600,
          }}
        >
          {user.name
            .split(" ")
            .map((n) => n[0])
            .join("")}
        </Avatar>
        <Typography
          sx={{ color: "#1a1f36", fontSize: "0.875rem", fontWeight: 500 }}
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
            bgcolor: user.status === "Active" ? "#dcfce7" : "#fee2e2",
            color: user.status === "Active" ? "#16a34a" : "#ef4444",
            fontWeight: 500,
            fontSize: "0.75rem",
          }}
        />
      </Box>
      <Box>
        <IconButton
          size="small"
          sx={{
            color: "#8B0000",
            "&:hover": { bgcolor: "rgba(139, 0, 0, 0.1)" },
          }}
          onClick={() => handleDeleteClick(user)}
        >
          <Delete sx={{ fontSize: 20 }} />
        </IconButton>
      </Box>
    </Box>
  );

  return (
    <AdminLayout>
      <Box
        sx={{
          px: 3,
          py: 2,
          width: "100%",
          maxWidth: "100%",
          overflowX: "hidden",
          bgcolor: "#f8fafc",
        }}
      >
        {/* Header */}
        <Box
          sx={{
            mb: 4,
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Typography
            variant="h5"
            sx={{
              fontWeight: 700,
              color: "#1a1f36",
              fontSize: "1.5rem",
            }}
          >
            Users
          </Typography>
        </Box>

        {/* Search and Filter Section */}
        <Box
          sx={{
            mb: 3,
            display: "flex",
            gap: 2,
            alignItems: "center",
          }}
        >
          <TextField
            placeholder="Search users..."
            value={searchQuery}
            onChange={handleSearchChange}
            sx={{
              flex: 1,
              maxWidth: 300,
              bgcolor: "white",
              "& .MuiOutlinedInput-root": {
                "& fieldset": {
                  borderColor: "#E2E8F0",
                },
                "&:hover fieldset": {
                  borderColor: "#8B0000",
                },
                "&.Mui-focused fieldset": {
                  borderColor: "#8B0000",
                },
              },
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search sx={{ color: "#64748B" }} />
                </InputAdornment>
              ),
            }}
          />

          <FormControl sx={{ minWidth: 150 }}>
            <InputLabel id="role-filter-label" sx={{ color: "#64748B" }}>
              Filter by Role
            </InputLabel>
            <Select
              labelId="role-filter-label"
              value={selectedRole}
              label="Filter by Role"
              onChange={handleRoleChange}
              sx={{
                bgcolor: "white",
                "& .MuiOutlinedInput-notchedOutline": {
                  borderColor: "#E2E8F0",
                },
                "&:hover .MuiOutlinedInput-notchedOutline": {
                  borderColor: "#8B0000",
                },
                "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                  borderColor: "#8B0000",
                },
              }}
            >
              {availableRoles.map((role) => (
                <MenuItem key={role} value={role}>
                  {role}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl sx={{ minWidth: 150 }}>
            <InputLabel id="sort-by-label" sx={{ color: "#64748B" }}>
              Sort by
            </InputLabel>
            <Select
              labelId="sort-by-label"
              value={sortBy}
              label="Sort by"
              onChange={handleSortChange}
              sx={{
                bgcolor: "white",
                "& .MuiOutlinedInput-notchedOutline": {
                  borderColor: "#E2E8F0",
                },
                "&:hover .MuiOutlinedInput-notchedOutline": {
                  borderColor: "#8B0000",
                },
                "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                  borderColor: "#8B0000",
                },
              }}
            >
              <MenuItem value="Newest">Newest</MenuItem>
              <MenuItem value="Oldest">Oldest</MenuItem>
              <MenuItem value="Name">Name</MenuItem>
            </Select>
          </FormControl>
        </Box>

        {/* Error message */}
        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {/* Users Table */}
        <Paper
          sx={{
            borderRadius: 2,
            boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
            overflow: "hidden",
            transition:
              "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
            "&:hover": {
              transform: "translateY(-2px)",
              boxShadow: "0 4px 8px rgba(0,0,0,0.08)",
            },
          }}
        >
          {/* Table Header */}
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: "2.5fr 1fr 2fr 1fr 1fr",
              borderBottom: "1px solid #E2E8F0",
              bgcolor: "#F8FAFC",
              p: 2,
            }}
          >
            <Typography
              sx={{ color: "#64748B", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Name
            </Typography>
            <Typography
              sx={{ color: "#64748B", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Role
            </Typography>
            <Typography
              sx={{ color: "#64748B", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Email
            </Typography>
            <Typography
              sx={{ color: "#64748B", fontWeight: 600, fontSize: "0.875rem" }}
            >
              Status
            </Typography>
            <Typography
              sx={{ color: "#64748B", fontWeight: 600, fontSize: "0.875rem" }}
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
            displayedUsers.map(renderUserRow)
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
            }}
          >
            <Typography sx={{ color: "#64748B", fontSize: "0.875rem" }}>
              Showing {startIndex + 1} to{" "}
              {Math.min(
                startIndex + rowsPerPage,
                filteredAndSortedUsers.length
              )}{" "}
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
                  "& .MuiPaginationItem-root": {
                    color: "#64748B",
                    borderRadius: 1,
                    "&.Mui-selected": {
                      bgcolor: "#8B0000",
                      color: "white",
                      "&:hover": {
                        bgcolor: "#6B0000",
                      },
                    },
                    "&:hover": {
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
        aria-labelledby="delete-dialog-title"
      >
        <DialogTitle id="delete-dialog-title">Delete User</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete {userToDelete?.name}? This action
            cannot be undone.
          </Typography>
          {deleteError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {deleteError}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel}>Cancel</Button>
          <Button
            onClick={handleDeleteConfirm}
            color="error"
            variant="contained"
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </AdminLayout>
  );
};

export default Users;
