import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Typography,
  Paper,
  CircularProgress,
  Alert,
  IconButton,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Tooltip,
  Chip,
  Menu,
  MenuItem,
  TextField,
} from "@mui/material";
import {
  ChevronLeft,
  ChevronRight,
  Refresh,
  Info,
  Search as SearchIcon,
  ExpandMore,
} from "@mui/icons-material";
import AdminLayout from "./AdminLayout";
import appointmentService from "../services/appointmentService";
import authService from "../services/authService";

const Schedule = () => {
  const navigate = useNavigate();
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [yearMenuAnchor, setYearMenuAnchor] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  // Generate array of years (current year ± 10 years)
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 21 }, (_, i) => currentYear - 10 + i);

  const handleYearClick = (event) => {
    setYearMenuAnchor(event.currentTarget);
  };

  const handleYearClose = () => {
    setYearMenuAnchor(null);
  };

  const handleYearSelect = (year) => {
    setCurrentDate(new Date(year, currentDate.getMonth(), 1));
    handleYearClose();
  };

  const fetchAppointments = async () => {
    try {
      setLoading(true);
      setError(null);
      // Check authentication
      if (!authService.isAuthenticated()) {
        navigate("/admin/login");
        return;
      }

      const data = await appointmentService.getAllFacultyAppointments();
      setAppointments(data);
    } catch (err) {
      console.error("Error fetching appointments:", err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        authService.logout();
        navigate("/admin/login");
      } else {
        let errorMessage = "Failed to load appointments. ";
        if (err.response?.status === 404) {
          errorMessage += "The appointment service is currently unavailable. ";
        } else if (!navigator.onLine) {
          errorMessage += "Please check your internet connection. ";
        }
        errorMessage += "Please try again later.";
        setError(errorMessage);
      }
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAppointments();
  }, [navigate]);

  // Function to get all dates for the current month view
  const getDaysInMonth = (year, month) => {
    const date = new Date(year, month, 1);
    const days = [];
    
    // Get the first day of the month
    const firstDay = new Date(date.getFullYear(), date.getMonth(), 1);
    // Get the last day of the month
    const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0);
    
    // Get the day of week for the first day (0-6)
    const firstDayIndex = firstDay.getDay();
    
    // Add days from previous month
    const prevMonthLastDay = new Date(date.getFullYear(), date.getMonth(), 0).getDate();
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      const prevDate = new Date(date.getFullYear(), date.getMonth() - 1, prevMonthLastDay - i);
      days.push({
        date: prevDate,
        isCurrentMonth: false
      });
    }
    
    // Add days of current month
    for (let day = 1; day <= lastDay.getDate(); day++) {
      const currentDate = new Date(date.getFullYear(), date.getMonth(), day);
      days.push({
        date: currentDate,
        isCurrentMonth: true
      });
    }
    
    // Add days from next month to complete the calendar grid
    const remainingDays = 42 - days.length; // 6 rows * 7 days = 42
    for (let day = 1; day <= remainingDays; day++) {
      const nextDate = new Date(date.getFullYear(), date.getMonth() + 1, day);
      days.push({
        date: nextDate,
        isCurrentMonth: false
      });
    }
    
    return days;
  };

  // Navigation functions
  const goToToday = () => {
    setCurrentDate(new Date());
    setSelectedDate(new Date());
  };

  const goToPreviousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const getMonthName = (date) => {
    return date.toLocaleString("default", { month: "long" });
  };

  const isToday = (date) => {
    const today = new Date();
    return date.getDate() === today.getDate() &&
      date.getMonth() === today.getMonth() &&
      date.getFullYear() === today.getFullYear();
  };

  const isSelected = (date) => {
    return date.getDate() === selectedDate.getDate() &&
      date.getMonth() === selectedDate.getMonth() &&
      date.getFullYear() === selectedDate.getFullYear();
  };

  const getAppointmentsForDate = (date) => {
    return appointments.filter(apt => {
      const aptDate = new Date(apt.startTime._seconds * 1000);
      return aptDate.getDate() === date.getDate() &&
        aptDate.getMonth() === date.getMonth() &&
        aptDate.getFullYear() === date.getFullYear();
    });
  };

  const filteredAppointments = appointments.filter(apt => {
    const matchesSearch = searchQuery.toLowerCase() === '' || 
      apt.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      apt.facultyName.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesSearch;
  });

  const handleDateClick = (date, dateAppointments) => {
    if (dateAppointments.length > 0) {
      setSelectedAppointment(dateAppointments[0]); // Show first appointment if multiple exist
      setIsDialogOpen(true);
    }
  };

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
            justifyContent: "space-between",
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
            Schedule
          </Typography>
          {error && (
            <Button
              startIcon={<Refresh />}
              onClick={fetchAppointments}
              variant="contained"
              sx={{
                bgcolor: 'rgba(255, 255, 255, 0.1)',
                color: '#ffffff',
                '&:hover': {
                  bgcolor: 'rgba(255, 255, 255, 0.2)',
                },
              }}
            >
              Retry
            </Button>
          )}
        </Box>

        {/* Search and Navigation Controls */}
        <Box 
          sx={{ 
            display: "flex", 
            gap: 2, 
            alignItems: 'center', 
            mb: 4,
            flexWrap: "wrap",
            position: "relative",
            zIndex: 1,
            justifyContent: 'space-between'
          }}
        >
          {/* Left side: Search */}
          <Box
            sx={{
              flex: { xs: '1 1 100%', sm: '1 1 280px' },
              maxWidth: { sm: 320 },
              position: "relative",
              zIndex: 2,
            }}
          >
            <TextField
              placeholder="Search appointments..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              sx={{
                flex: { xs: '1 1 100%', sm: '1 1 280px' },
                maxWidth: { sm: 320 },
                '& .MuiOutlinedInput-root': {
                  borderRadius: '16px',
                  height: '45px',
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
                  <SearchIcon sx={{ color: "#64748B", ml: 0.5, mr: 1 }} />
                ),
              }}
            />
          </Box>

          {/* Center: Month Navigation */}
          <Box 
            sx={{ 
              display: 'flex', 
              gap: 2, 
              alignItems: 'center',
              flex: 1,
              justifyContent: 'center',
              position: 'absolute',
              left: '50%',
              transform: 'translateX(-50%)',
              width: 'auto',
              zIndex: 2
            }}
          >
            <IconButton
              onClick={goToPreviousMonth}
              sx={{
                color: '#ffffff',
                p: 1.5,
                '&:hover': {
                  bgcolor: 'transparent',
                },
              }}
            >
              <ChevronLeft sx={{ fontSize: 42 }} />
            </IconButton>

            <Box sx={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: 1.5, 
              minWidth: '300px', 
              justifyContent: 'center',
            }}>
              <Typography
                sx={{
                  color: '#ffffff',
                  fontWeight: 600,
                  fontSize: '2rem',
                  textAlign: 'center',
                }}
              >
                {getMonthName(currentDate)}
              </Typography>
              <Typography
                sx={{
                  color: '#ffffff',
                  fontWeight: 600,
                  fontSize: '2rem',
                  textAlign: 'center',
                }}
              >
                {currentDate.getFullYear()}
              </Typography>
            </Box>

            <IconButton
              onClick={goToNextMonth}
              sx={{
                color: '#ffffff',
                p: 1.5,
                '&:hover': {
                  bgcolor: 'transparent',
                },
              }}
            >
              <ChevronRight sx={{ fontSize: 42 }} />
            </IconButton>
          </Box>

          {/* Right side: Today button and Year selector */}
          <Box sx={{ 
            display: 'flex', 
            gap: 1, 
            alignItems: 'center',
            ml: 'auto',
            position: 'relative',
            zIndex: 1
          }}>
            <Button
              onClick={goToToday}
              variant="contained"
              sx={{
                bgcolor: '#ffffff',
                color: '#8B0000',
                borderRadius: '12px',
                textTransform: 'none',
                fontWeight: 600,
                '&:hover': {
                  bgcolor: 'rgba(255, 255, 255, 0.9)',
                },
              }}
            >
              Today
            </Button>

            <Button
              onClick={handleYearClick}
              sx={{
                bgcolor: '#ffffff',
                color: '#8B0000',
                borderRadius: '12px',
                textTransform: 'none',
                fontWeight: 600,
                '&:hover': {
                  bgcolor: 'rgba(255, 255, 255, 0.9)',
                },
                minWidth: '100px',
                px: 2
              }}
              endIcon={<ExpandMore />}
            >
              {currentDate.getFullYear()}
            </Button>
          </Box>
        </Box>

        {/* Calendar Grid */}
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
          {loading ? (
            <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
              <CircularProgress sx={{ color: "#8B0000" }} />
            </Box>
          ) : error ? (
            <Alert 
              severity="error" 
              sx={{ 
                m: 2,
                borderRadius: '16px',
                border: '1px solid rgba(239, 68, 68, 0.2)',
                bgcolor: 'rgba(239, 68, 68, 0.05)'
              }}
              action={
                <IconButton
                  color="inherit"
                  size="small"
                  onClick={fetchAppointments}
                >
                  <Refresh />
                </IconButton>
              }
            >
              {error}
            </Alert>
          ) : (
            <>
              {/* Calendar Header */}
              <Box
                sx={{
                  display: "grid",
                  gridTemplateColumns: "repeat(7, 1fr)",
                  bgcolor: "#f8fafc",
                  borderBottom: "1px solid #E2E8F0",
                  p: 2,
                }}
              >
                {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
                  <Typography
                    key={day}
                    sx={{
                      textAlign: "center",
                      color: "#1a1f36",
                      fontWeight: 600,
                      fontSize: "0.875rem",
                    }}
                  >
                    {day}
                  </Typography>
                ))}
              </Box>

              {/* Calendar Days */}
              <Box sx={{ p: 2 }}>
                <Box
                  sx={{
                    display: "grid",
                    gridTemplateColumns: "repeat(7, 1fr)",
                    gap: 1,
                  }}
                >
                  {getDaysInMonth(
                    currentDate.getFullYear(),
                    currentDate.getMonth()
                  ).map((dayObj, index) => {
                    const date = dayObj.date;
                    const isCurrentMonth = dayObj.isCurrentMonth;
                    const dayAppointments = getAppointmentsForDate(date);

                    return (
                      <Box
                        key={index}
                        onClick={() => setSelectedDate(date)}
                        sx={{
                          position: "relative",
                          minHeight: "120px",
                          p: 1.5,
                          border: "1px solid #E2E8F0",
                          borderRadius: "16px",
                          bgcolor: isSelected(date)
                            ? "rgba(139, 0, 0, 0.03)"
                            : isToday(date)
                            ? "rgba(37, 99, 235, 0.03)"
                            : "transparent",
                          cursor: "pointer",
                          transition: "all 0.2s ease",
                          "&:hover": {
                            bgcolor: "rgba(139, 0, 0, 0.03)",
                            transform: "translateY(-2px)",
                            boxShadow: "0 4px 12px rgba(0,0,0,0.05)",
                          },
                        }}
                      >
                        <Typography
                          sx={{
                            textAlign: "center",
                            color: !isCurrentMonth
                              ? "#CBD5E1"
                              : isToday(date)
                              ? "#2563EB"
                              : "#1a1f36",
                            fontWeight: isToday(date) ? 600 : 500,
                            mb: 1.5,
                            fontSize: '0.875rem',
                          }}
                        >
                          {date.getDate()}
                        </Typography>

                        <Box sx={{ display: "flex", flexDirection: "column", gap: 0.75 }}>
                          {dayAppointments.slice(0, 2).map((appointment) => (
                            <Tooltip
                              key={appointment.appointmentId}
                              title={
                                <Box>
                                  <Typography variant="subtitle2">
                                    {appointment.title}
                                  </Typography>
                                  <Typography variant="body2">
                                    Faculty: {appointment.facultyName}
                                  </Typography>
                                  <Typography variant="body2">
                                    {appointmentService.formatDate(appointment.startTime)}
                                  </Typography>
                                </Box>
                              }
                            >
                              <Box
                                sx={{
                                  background: 'linear-gradient(135deg, #8B0000 0%, #6B0000 100%)',
                                  color: '#ffffff',
                                  p: '8px 10px',
                                  borderRadius: '6px',
                                  width: '100%',
                                  cursor: 'pointer',
                                  transition: 'all 0.2s ease',
                                  boxShadow: '0 2px 4px rgba(139, 0, 0, 0.15)',
                                  '&:hover': {
                                    background: 'linear-gradient(135deg, #7B0000 0%, #5B0000 100%)',
                                    transform: 'translateY(-1px)',
                                    boxShadow: '0 4px 8px rgba(139, 0, 0, 0.2)',
                                  },
                                }}
                              >
                                {appointment.startTime && (
                                  <Typography
                                    sx={{
                                      fontSize: '0.7rem',
                                      fontWeight: 500,
                                      color: 'rgba(255, 255, 255, 0.9)',
                                      mb: 0.5,
                                      letterSpacing: '0.3px',
                                    }}
                                  >
                                    {new Date(appointment.startTime._seconds * 1000).toLocaleTimeString([], {
                                      hour: '2-digit',
                                      minute: '2-digit',
                                      hour12: true
                                    })}
                                  </Typography>
                                )}
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                                  <Typography
                                    sx={{
                                      fontSize: '0.75rem',
                                      fontWeight: 600,
                                      color: 'inherit',
                                      whiteSpace: 'nowrap',
                                      overflow: 'hidden',
                                      textOverflow: 'ellipsis',
                                      lineHeight: 1.2,
                                    }}
                                  >
                                    Meeting with {appointment.facultyName}
                                  </Typography>
                                  <Typography
                                    sx={{
                                      fontSize: '0.7rem',
                                      color: 'rgba(255, 255, 255, 0.85)',
                                      whiteSpace: 'nowrap',
                                      overflow: 'hidden',
                                      textOverflow: 'ellipsis',
                                      display: 'flex',
                                      alignItems: 'center',
                                      gap: 0.5,
                                    }}
                                  >
                                    Faculty: {appointment.facultyName}
                                  </Typography>
                                  <Typography
                                    sx={{
                                      fontSize: '0.7rem',
                                      color: 'rgba(255, 255, 255, 0.85)',
                                      whiteSpace: 'nowrap',
                                      overflow: 'hidden',
                                      textOverflow: 'ellipsis',
                                    }}
                                  >
                                    {new Date(appointment.startTime._seconds * 1000).toLocaleDateString([], {
                                      month: 'numeric',
                                      day: 'numeric',
                                      year: 'numeric',
                                    })}, {new Date(appointment.startTime._seconds * 1000).toLocaleTimeString([], {
                                      hour: '2-digit',
                                      minute: '2-digit',
                                      hour12: true
                                    })}
                                  </Typography>
                                </Box>
                              </Box>
                            </Tooltip>
                          ))}
                          {dayAppointments.length > 2 && (
                            <Typography
                              sx={{
                                color: "#64748B",
                                fontSize: "0.75rem",
                                textAlign: "center",
                                mt: 0.5,
                                fontWeight: 500,
                              }}
                            >
                              +{dayAppointments.length - 2} more
                            </Typography>
                          )}
                        </Box>
                      </Box>
                    );
                  })}
                </Box>
              </Box>
            </>
          )}
        </Paper>

        {/* Year Selection Menu */}
        <Menu
          anchorEl={yearMenuAnchor}
          open={Boolean(yearMenuAnchor)}
          onClose={handleYearClose}
          PaperProps={{
            sx: {
              mt: 1,
              borderRadius: '16px',
              boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
              maxHeight: 300,
            },
          }}
        >
          {years.map((year) => (
            <MenuItem
              key={year}
              onClick={() => handleYearSelect(year)}
              sx={{
                py: 1,
                px: 2,
                color: year === currentDate.getFullYear() ? '#8B0000' : '#1a1f36',
                fontWeight: year === currentDate.getFullYear() ? 600 : 400,
                '&:hover': {
                  bgcolor: 'rgba(139, 0, 0, 0.04)',
                },
              }}
            >
              {year}
            </MenuItem>
          ))}
        </Menu>

        {/* Appointment Details Dialog */}
        <Dialog 
          open={isDialogOpen} 
          onClose={() => setIsDialogOpen(false)}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle sx={{ fontWeight: 600 }}>
            Appointment Details
          </DialogTitle>
          <DialogContent dividers>
            {selectedAppointment && (
              <Box sx={{ py: 1 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 2 }}>
                  {selectedAppointment.title}
                </Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>
                  <strong>Faculty:</strong> {selectedAppointment.facultyName}
                </Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>
                  <strong>Created By:</strong> {selectedAppointment.creatorName}
                </Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>
                  <strong>Start Time:</strong> {appointmentService.formatDate(selectedAppointment.startTime)}
                </Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>
                  <strong>End Time:</strong> {appointmentService.formatDate(selectedAppointment.endTime)}
                </Typography>
                <Typography variant="body2" sx={{ mb: 1 }}>
                  <strong>Status:</strong> {selectedAppointment.status}
                </Typography>
                <Typography variant="body2">
                  <strong>Approved:</strong> {selectedAppointment.hasApproved ? "Yes" : "No"}
                </Typography>
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setIsDialogOpen(false)}>Close</Button>
          </DialogActions>
        </Dialog>
      </Box>
    </AdminLayout>
  );
};

export default Schedule;