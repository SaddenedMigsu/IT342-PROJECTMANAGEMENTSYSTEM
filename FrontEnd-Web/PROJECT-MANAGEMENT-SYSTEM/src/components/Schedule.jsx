import React, { useState } from "react";
import {
  Box,
  Typography,
  IconButton,
  Button,
  TextField,
  ToggleButtonGroup,
  ToggleButton,
  Paper,
  Tooltip,
  Avatar,
  Chip,
} from "@mui/material";
import {
  ChevronLeft,
  ChevronRight,
  Search as SearchIcon,
  ArrowBack,
  Event,
  AccessTime,
  Group,
} from "@mui/icons-material";
import AdminLayout from "./AdminLayout";

const Schedule = () => {
  const [viewMode, setViewMode] = useState("Week");
  const [searchQuery, setSearchQuery] = useState("");
  const [currentDate, setCurrentDate] = useState(new Date(2024, 2, 1));

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

  // Function to get the start of the week
  const getStartOfWeek = (date) => {
    const newDate = new Date(date);
    const day = newDate.getDay();
    newDate.setDate(newDate.getDate() - day);
    return newDate;
  };

  // Function to get dates for the week
  const getWeekDates = () => {
    const startDate = getStartOfWeek(currentDate);
    return Array.from({ length: 7 }, (_, i) => {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + i);
      return date;
    });
  };

  // Function to get month name
  const getMonthName = (date) => {
    return new Date(date).toLocaleString("default", { month: "long" });
  };

  // Navigation functions
  const goToToday = () => {
    setCurrentDate(new Date());
  };

  const goToPreviousMonth = () => {
    const newDate = new Date(currentDate);
    newDate.setMonth(currentDate.getMonth() - 1);
    setCurrentDate(newDate);
  };

  const goToNextMonth = () => {
    const newDate = new Date(currentDate);
    newDate.setMonth(currentDate.getMonth() + 1);
    setCurrentDate(newDate);
  };

  // Function to get all dates for the current month
  const getMonthDates = () => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    // Get the first Monday
    const start = new Date(firstDay);
    while (start.getDay() !== 1) {
      // 1 represents Monday
      start.setDate(start.getDate() - 1);
    }

    const dates = [];
    const current = new Date(start);

    // Add 42 days (6 weeks) to ensure we cover the whole month
    while (dates.length < 42) {
      if (current.getDay() !== 0) {
        // Skip Sundays
        dates.push(new Date(current));
      }
      current.setDate(current.getDate() + 1);
    }

    return dates;
  };

  // Update weekDates to use month dates
  const monthDates = getMonthDates();
  const weekRows = Array.from({ length: 6 }, (_, i) =>
    monthDates.slice(i * 7, (i + 1) * 7)
  );

  const weekDates = getWeekDates();
  const days = ["MON", "TUE", "WED", "THU", "FRI", "SAT"];
  const hours = Array.from({ length: 11 }, (_, i) => i + 7); // 7 AM to 5 PM

  const getAppointmentsForTimeSlot = (date, hour) => {
    return appointments.filter((apt) => {
      const aptDate = new Date(apt.start).getDate();
      const aptHour = new Date(apt.start).getHours();
      return aptDate === date.getDate() && aptHour === hour;
    });
  };

  return (
    <AdminLayout>
      <Box
        sx={{
          px: 3,
          py: 2,
          width: "100%",
          maxWidth: "100%",
          overflowX: "hidden",
          bgcolor: "#fff",
        }}
      >
        {/* Header */}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            mb: 3,
            gap: 2,
          }}
        >
          <IconButton
            sx={{
              color: "#8B0000",
              bgcolor: "rgba(139, 0, 0, 0.1)",
              "&:hover": {
                bgcolor: "rgba(139, 0, 0, 0.15)",
              },
            }}
          >
            <ArrowBack />
          </IconButton>
          <Typography
            variant="h5"
            sx={{
              fontWeight: 700,
              color: "#8B0000",
              fontSize: "1.5rem",
            }}
          >
            Sir Amparo's Appointment Schedule
          </Typography>
        </Box>

        {/* Controls */}
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            mb: 3,
            gap: 2,
            flexWrap: "wrap",
          }}
        >
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              bgcolor: "white",
              borderRadius: 2,
              border: "1px solid #e2e8f0",
              p: 0.5,
            }}
          >
            <IconButton
              size="small"
              sx={{ color: "#8B0000" }}
              onClick={goToPreviousMonth}
            >
              <ChevronLeft />
            </IconButton>
            <Typography
              sx={{
                mx: 2,
                color: "#8B0000",
                fontWeight: 600,
                minWidth: "100px",
                textAlign: "center",
              }}
            >
              {getMonthName(currentDate)}
            </Typography>
            <IconButton
              size="small"
              sx={{ color: "#8B0000" }}
              onClick={goToNextMonth}
            >
              <ChevronRight />
            </IconButton>
          </Box>

          <ToggleButtonGroup
            value={viewMode}
            exclusive
            onChange={(e, value) => value && setViewMode(value)}
            sx={{
              "& .MuiToggleButton-root": {
                border: "1px solid #e2e8f0",
                color: "#64748b",
                "&.Mui-selected": {
                  bgcolor: "#8B0000",
                  color: "white",
                  "&:hover": {
                    bgcolor: "#6B0000",
                  },
                },
                "&:hover": {
                  bgcolor: "rgba(139, 0, 0, 0.04)",
                },
              },
            }}
          >
            <ToggleButton value="Day">Day</ToggleButton>
            <ToggleButton value="Week">Week</ToggleButton>
            <ToggleButton value="Month">Month</ToggleButton>
            <ToggleButton value="Year">Year</ToggleButton>
          </ToggleButtonGroup>

          <TextField
            size="small"
            placeholder="Search appointments..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            sx={{
              minWidth: "300px",
              flex: 1,
              maxWidth: "400px",
              ml: "auto",
              "& .MuiOutlinedInput-root": {
                bgcolor: "white",
                borderRadius: 2,
                "&:hover fieldset": {
                  borderColor: "#E2E8F0",
                },
                "&.Mui-focused fieldset": {
                  borderColor: "#8B0000",
                },
              },
            }}
            InputProps={{
              startAdornment: <SearchIcon sx={{ color: "#64748b", mr: 1 }} />,
            }}
          />
        </Box>

        {/* Calendar Grid */}
        <Paper
          sx={{
            height: "calc(100vh - 220px)",
            overflow: "auto",
            bgcolor: "white",
            borderRadius: 2,
            boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
            transition:
              "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
            "&:hover": {
              transform: "translateY(-2px)",
              boxShadow: "0 4px 8px rgba(0,0,0,0.08)",
            },
          }}
        >
          {/* Days header */}
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: "80px repeat(6, 1fr)",
              borderBottom: "1px solid #e2e8f0",
              position: "sticky",
              top: 0,
              bgcolor: "white",
              zIndex: 1,
            }}
          >
            <Box sx={{ p: 2 }} />
            {days.map((day, index) => (
              <Box
                key={index}
                sx={{
                  p: 2,
                  textAlign: "center",
                  borderLeft: "1px solid #e2e8f0",
                }}
              >
                <Typography
                  sx={{
                    color: "#1a1f36",
                    fontSize: "0.875rem",
                    fontWeight: 600,
                    mb: 0.5,
                  }}
                >
                  {day}
                </Typography>
                <Typography
                  sx={{
                    color:
                      weekDates[index].toDateString() ===
                      new Date().toDateString()
                        ? "#8B0000"
                        : "#1a1f36",
                    fontSize: "1.125rem",
                    fontWeight: 600,
                  }}
                >
                  {weekDates[index].getDate()}
                </Typography>
              </Box>
            ))}
          </Box>

          {/* Time slots */}
          {hours.map((hour) => (
            <Box
              key={hour}
              sx={{
                display: "grid",
                gridTemplateColumns: "80px repeat(6, 1fr)",
                borderBottom: "1px solid #e2e8f0",
                minHeight: "100px",
              }}
            >
              <Box
                sx={{
                  p: 1.5,
                  textAlign: "center",
                  borderRight: "1px solid #e2e8f0",
                  color: "#1a1f36",
                  fontSize: "0.875rem",
                  fontWeight: 600,
                }}
              >
                {`${hour % 12 || 12}:00 ${hour < 12 ? "AM" : "PM"}`}
              </Box>
              {weekRows[0].map((date, dateIndex) => {
                const appointments = getAppointmentsForTimeSlot(date, hour);
                return (
                  <Box
                    key={`${dateIndex}-${hour}`}
                    sx={{
                      p: 1,
                      borderLeft: "1px solid #e2e8f0",
                      position: "relative",
                      cursor: "pointer",
                    }}
                  >
                    {appointments.map((apt) => (
                      <Tooltip
                        key={apt.id}
                        title={
                          <Box sx={{ p: 1 }}>
                            <Typography
                              sx={{ fontWeight: 600, mb: 1, color: "white" }}
                            >
                              {apt.title} - {apt.group}
                            </Typography>
                            <Box
                              sx={{
                                display: "flex",
                                alignItems: "center",
                                mb: 1,
                              }}
                            >
                              <AccessTime
                                sx={{ fontSize: 16, mr: 1, color: "white" }}
                              />
                              <Typography
                                variant="body2"
                                sx={{ color: "white" }}
                              >
                                {`${apt.start.getHours() % 12 || 12}:00 ${
                                  apt.start.getHours() < 12 ? "AM" : "PM"
                                } - 
                                  ${apt.end.getHours() % 12 || 12}:00 ${
                                  apt.end.getHours() < 12 ? "AM" : "PM"
                                }`}
                              </Typography>
                            </Box>
                            <Box sx={{ display: "flex", alignItems: "center" }}>
                              <Group
                                sx={{ fontSize: 16, mr: 1, color: "white" }}
                              />
                              <Typography
                                variant="body2"
                                sx={{ color: "white" }}
                              >
                                {apt.members.join(", ")}
                              </Typography>
                            </Box>
                          </Box>
                        }
                        arrow
                        placement="top"
                        componentsProps={{
                          tooltip: {
                            sx: {
                              bgcolor:
                                apt.status === "Confirmed"
                                  ? "#8B0000"
                                  : "#D4A017",
                              "& .MuiTooltip-arrow": {
                                color:
                                  apt.status === "Confirmed"
                                    ? "#8B0000"
                                    : "#D4A017",
                              },
                              maxWidth: "none",
                            },
                          },
                        }}
                      >
                        <Paper
                          elevation={0}
                          sx={{
                            p: 1.5,
                            border: "2px solid",
                            borderColor:
                              apt.status === "Confirmed"
                                ? "#8B0000"
                                : "#D4A017",
                            borderRadius: 2,
                            mb: 1,
                            cursor: "pointer",
                            transition: "all 0.2s",
                            "&:hover": {
                              transform: "translateY(-2px)",
                              boxShadow: "0 4px 8px rgba(0,0,0,0.1)",
                            },
                          }}
                        >
                          <Box
                            sx={{
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "space-between",
                              mb: 1,
                            }}
                          >
                            <Typography
                              sx={{
                                fontSize: "1rem",
                                fontWeight: 700,
                                color:
                                  apt.status === "Confirmed"
                                    ? "#8B0000"
                                    : "#D4A017",
                              }}
                            >
                              {apt.group}
                            </Typography>
                            <Chip
                              label={apt.status}
                              size="small"
                              sx={{
                                height: 24,
                                fontSize: "0.8rem",
                                border: "1px solid",
                                borderColor:
                                  apt.status === "Confirmed"
                                    ? "#8B0000"
                                    : "#D4A017",
                                color:
                                  apt.status === "Confirmed"
                                    ? "#8B0000"
                                    : "#D4A017",
                                bgcolor: "transparent",
                                fontWeight: 600,
                                px: 1,
                                "& .MuiChip-label": {
                                  px: 1,
                                },
                              }}
                            />
                          </Box>
                          <Box
                            sx={{
                              display: "flex",
                              alignItems: "center",
                              gap: 1,
                            }}
                          >
                            <Avatar
                              sx={{
                                width: 24,
                                height: 24,
                                fontSize: "0.8rem",
                                bgcolor:
                                  apt.status === "Confirmed"
                                    ? "#8B0000"
                                    : "#D4A017",
                                color: "white",
                              }}
                            >
                              {apt.members[0]
                                .split(" ")
                                .map((n) => n[0])
                                .join("")}
                            </Avatar>
                            <Typography
                              sx={{
                                fontSize: "0.875rem",
                                color: "#1a1f36",
                                fontWeight: 500,
                              }}
                            >
                              {apt.members.length} members
                            </Typography>
                          </Box>
                        </Paper>
                      </Tooltip>
                    ))}
                  </Box>
                );
              })}
            </Box>
          ))}
        </Paper>
      </Box>
    </AdminLayout>
  );
};

export default Schedule;
