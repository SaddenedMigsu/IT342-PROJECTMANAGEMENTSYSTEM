import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  IconButton,
  Button,
  TextField,
  Paper,
  Tooltip,
  Avatar,
  Chip,
  Menu,
  MenuItem,
} from "@mui/material";
import {
  ChevronLeft,
  ChevronRight,
  Search as SearchIcon,
  Event,
  AccessTime,
  Group,
  ExpandMore,
} from "@mui/icons-material";
import AdminLayout from "./AdminLayout";

function Schedule() {
  const [searchQuery, setSearchQuery] = useState("");
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [yearMenuAnchor, setYearMenuAnchor] = useState(null);

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

  // Mock data for appointments
  const appointments = [
    // March appointments
    {
      id: 1,
      title: "Capstone Consultation",
      group: "Group 12",
      members: ["John Doe", "Jane Smith", "Mike Johnson"],
      start: new Date(2024, 2, 18, 8, 0), // Monday
      end: new Date(2024, 2, 18, 9, 0),
      status: "Confirmed",
    },
    {
      id: 2,
      title: "Capstone Consultation",
      group: "Group 21",
      members: ["Alice Brown", "Bob Wilson"],
      start: new Date(2024, 2, 19, 9, 0), // Tuesday
      end: new Date(2024, 2, 19, 10, 0),
      status: "Pending",
    },
    {
      id: 3,
      title: "Capstone Consultation",
      group: "Group 22",
      members: ["Sarah Lee", "Tom Clark"],
      start: new Date(2024, 2, 20, 13, 0), // Wednesday
      end: new Date(2024, 2, 20, 14, 0),
      status: "Confirmed",
    },
    {
      id: 4,
      title: "Capstone Consultation",
      group: "Group 26",
      members: ["Emma Davis", "James Miller"],
      start: new Date(2024, 2, 21, 13, 0), // Thursday
      end: new Date(2024, 2, 21, 14, 0),
      status: "Confirmed",
    },
    {
      id: 5,
      title: "Capstone Consultation",
      group: "Group 2",
      members: ["Oliver Wilson", "Sophia Martin"],
      start: new Date(2024, 2, 22, 9, 0), // Friday
      end: new Date(2024, 2, 22, 10, 0),
      status: "Pending",
    },
    {
      id: 6,
      title: "Capstone Consultation",
      group: "Group 15",
      members: ["Mark Chen", "Lisa Wang", "Kevin Park"],
      start: new Date(2024, 2, 18, 10, 0), // Monday
      end: new Date(2024, 2, 18, 11, 0),
      status: "Confirmed",
    },
    {
      id: 7,
      title: "Capstone Consultation",
      group: "Group 8",
      members: ["Rachel Kim", "David Lee"],
      start: new Date(2024, 2, 19, 13, 0), // Tuesday
      end: new Date(2024, 2, 19, 14, 0),
      status: "Pending",
    },
    {
      id: 8,
      title: "Capstone Consultation",
      group: "Group 17",
      members: ["Michael Chang", "Emily Liu"],
      start: new Date(2024, 2, 20, 11, 0), // Wednesday
      end: new Date(2024, 2, 20, 12, 0),
      status: "Confirmed",
    },
    {
      id: 9,
      title: "Capstone Consultation",
      group: "Group 5",
      members: ["Jessica Wu", "Andrew Tan", "Michelle Lin"],
      start: new Date(2024, 2, 21, 9, 0), // Thursday
      end: new Date(2024, 2, 21, 10, 0),
      status: "Confirmed",
    },
    {
      id: 10,
      title: "Capstone Consultation",
      group: "Group 11",
      members: ["Daniel Park", "Sophie Chen"],
      start: new Date(2024, 2, 22, 15, 0), // Friday
      end: new Date(2024, 2, 22, 16, 0),
      status: "Pending",
    },
    // April appointments
    {
      id: 11,
      title: "Capstone Consultation",
      group: "Group 19",
      members: ["Ryan Zhang", "Emma Wong"],
      start: new Date(2024, 3, 1, 10, 0), // Monday in April
      end: new Date(2024, 3, 1, 11, 0),
      status: "Confirmed",
    },
    {
      id: 12,
      title: "Capstone Consultation",
      group: "Group 7",
      members: ["Alex Liu", "Isabella Chen", "William Kim"],
      start: new Date(2024, 3, 2, 14, 0), // Tuesday in April
      end: new Date(2024, 3, 2, 15, 0),
      status: "Pending",
    },
    {
      id: 13,
      title: "Capstone Consultation",
      group: "Group 14",
      members: ["Nathan Park", "Olivia Wu"],
      start: new Date(2024, 3, 3, 11, 0), // Wednesday in April
      end: new Date(2024, 3, 3, 12, 0),
      status: "Confirmed",
    },
    // February appointments (past)
    {
      id: 14,
      title: "Capstone Consultation",
      group: "Group 23",
      members: ["Chris Lee", "Hannah Kim", "Jason Chen"],
      start: new Date(2024, 1, 26, 14, 0), // February
      end: new Date(2024, 1, 26, 15, 0),
      status: "Confirmed",
    },
    {
      id: 15,
      title: "Capstone Consultation",
      group: "Group 3",
      members: ["Victoria Wang", "Brandon Zhang"],
      start: new Date(2024, 1, 27, 9, 0), // February
      end: new Date(2024, 1, 27, 10, 0),
      status: "Confirmed",
    },
    // Additional March appointments
    {
      id: 16,
      title: "Capstone Consultation",
      group: "Group 25",
      members: ["Lucas Kim", "Mia Chen"],
      start: new Date(2024, 2, 25, 13, 0), // Monday
      end: new Date(2024, 2, 25, 14, 0),
      status: "Confirmed",
    },
    {
      id: 17,
      title: "Capstone Consultation",
      group: "Group 16",
      members: ["Ethan Wong", "Ava Liu", "Noah Park"],
      start: new Date(2024, 2, 26, 10, 0), // Tuesday
      end: new Date(2024, 2, 26, 11, 0),
      status: "Pending",
    },
    {
      id: 18,
      title: "Capstone Consultation",
      group: "Group 9",
      members: ["Liam Chen", "Sophia Wu"],
      start: new Date(2024, 2, 27, 14, 0), // Wednesday
      end: new Date(2024, 2, 27, 15, 0),
      status: "Confirmed",
    },
    // May appointments (future)
    {
      id: 19,
      title: "Capstone Consultation",
      group: "Group 28",
      members: ["Mason Lee", "Isabella Wang"],
      start: new Date(2024, 4, 6, 9, 0), // Monday in May
      end: new Date(2024, 4, 6, 10, 0),
      status: "Pending",
    },
    {
      id: 20,
      title: "Capstone Consultation",
      group: "Group 30",
      members: ["Jack Zhang", "Emma Chen", "William Liu"],
      start: new Date(2024, 4, 7, 13, 0), // Tuesday in May
      end: new Date(2024, 4, 7, 14, 0),
      status: "Confirmed",
    },
  ];

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

  const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const hours = Array.from({ length: 11 }, (_, i) => i + 7); // 7 AM to 5 PM

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
      const aptDate = new Date(apt.start);
      return aptDate.getDate() === date.getDate() &&
        aptDate.getMonth() === date.getMonth() &&
        aptDate.getFullYear() === date.getFullYear();
    });
  };

  const calendarDays = getDaysInMonth(currentDate.getFullYear(), currentDate.getMonth());

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
                const appointments = getAppointmentsForDate(date);

                return (
                  <Box
                    key={index}
                    onClick={() => setSelectedDate(date)}
                    sx={{
                      position: "relative",
                      minHeight: "120px",
                      p: 1,
                      border: "1px solid #E2E8F0",
                      borderRadius: "16px",
                      bgcolor: isSelected(date)
                        ? "rgba(139, 0, 0, 0.05)"
                        : isToday(date)
                        ? "rgba(37, 99, 235, 0.05)"
                        : "transparent",
                      cursor: "pointer",
                      transition: "all 0.2s ease",
                      "&:hover": {
                        bgcolor: "rgba(139, 0, 0, 0.05)",
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
                        mb: 1,
                      }}
                    >
                      {date.getDate()}
                    </Typography>

                    <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5 }}>
                      {appointments.slice(0, 2).map((appointment) => (
                        <Tooltip
                          key={appointment.id}
                          title={
                            <Box>
                              <Typography variant="subtitle2">
                                {appointment.title}
                              </Typography>
                              <Typography variant="body2">
                                {appointment.group}
                              </Typography>
                              <Typography variant="body2">
                                {`${appointment.start.toLocaleTimeString([], {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })} - ${appointment.end.toLocaleTimeString([], {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })}`}
                              </Typography>
                            </Box>
                          }
                        >
                          <Chip
                            size="small"
                            label={appointment.group}
                            sx={{
                              bgcolor: appointment.status === "Confirmed"
                                ? "rgba(22, 163, 74, 0.1)"
                                : "rgba(239, 68, 68, 0.1)",
                              color: appointment.status === "Confirmed"
                                ? "#16a34a"
                                : "#ef4444",
                              fontWeight: 500,
                              fontSize: "0.75rem",
                              width: "100%",
                              borderRadius: "8px",
                              '& .MuiChip-label': {
                                px: 1,
                              },
                            }}
                          />
                        </Tooltip>
                      ))}
                      {appointments.length > 2 && (
                        <Typography
                          sx={{
                            color: "#64748B",
                            fontSize: "0.75rem",
                            textAlign: "center",
                          }}
                        >
                          +{appointments.length - 2} more
                        </Typography>
                      )}
                    </Box>
                  </Box>
                );
              })}
            </Box>
          </Box>
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
      </Box>
    </AdminLayout>
  );
}

export default Schedule;