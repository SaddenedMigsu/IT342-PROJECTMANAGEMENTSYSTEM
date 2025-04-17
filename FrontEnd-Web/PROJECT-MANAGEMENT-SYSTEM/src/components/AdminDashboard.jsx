import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Grid,
  Select,
  MenuItem,
  Paper,
  IconButton,
  Avatar,
  Chip,
  CircularProgress,
} from "@mui/material";
import { BarChart } from "@mui/x-charts";
import AdminLayout from "./AdminLayout";
import Calendar from "./Calendar";
import PersonIcon from "@mui/icons-material/Person";
import EventNoteIcon from "@mui/icons-material/EventNote";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import GroupIcon from "@mui/icons-material/Group";
import appointmentService from "../services/appointmentService";
import ErrorBoundary from "./ErrorBoundary";

// Mock data for the chart
const monthlyData = [
  { month: "JAN", value: 100 },
  { month: "FEB", value: 130 },
  { month: "MAR", value: 140 },
  { month: "APR", value: 220 },
  { month: "MAY", value: 260 },
  { month: "JUN", value: 200 },
  { month: "JUL", value: 230 },
  { month: "AUG", value: 100 },
  { month: "SEP", value: 250 },
  { month: "OCT", value: 320 },
  { month: "NOV", value: 380 },
  { month: "DEC", value: 420 },
];

// Mock data for daily activity
const dailyData = [
  { day: "Mon", value: 45 },
  { day: "Tue", value: 52 },
  { day: "Wed", value: 38 },
  { day: "Thu", value: 65 },
  { day: "Fri", value: 48 },
  { day: "Sat", value: 25 },
];

// Mock data for yearly activity
const yearlyData = [
  { year: "2020", value: 2400 },
  { year: "2021", value: 2800 },
  { year: "2022", value: 3200 },
  { year: "2023", value: 3600 },
  { year: "2024", value: 1100 },
];

// Mock data for most booked faculty
const facultyData = [
  { name: "Barbaso, Leah", bookings: 41 },
  { name: "Revilleza, Frederick", bookings: 32 },
  { name: "Amparo, Joemarie", bookings: 29 },
];

// Mock data for recent activities
const recentActivities = [
  {
    id: 1,
    student: "Juan Dela Cruz",
    type: "Capstone Consultation",
    faculty: "Leah Barbaso",
    time: "10:00 AM",
    date: "Today",
    status: "Upcoming",
  },
  {
    id: 2,
    student: "Maria Santos",
    type: "Thesis Defense",
    faculty: "Frederick Revilleza",
    time: "2:30 PM",
    date: "Today",
    status: "Upcoming",
  },
  {
    id: 3,
    student: "Pedro Garcia",
    type: "Project Consultation",
    faculty: "Joemarie Amparo",
    time: "9:00 AM",
    date: "Tomorrow",
    status: "Scheduled",
  },
  {
    id: 4,
    student: "Ana Reyes",
    type: "Research Consultation",
    faculty: "Leah Barbaso",
    time: "3:00 PM",
    date: "Tomorrow",
    status: "Scheduled",
  },
];

const StatsCard = ({ icon, title, value, color, loading = false }) => (
  <Paper
    sx={{
      p: 2.5,
      borderRadius: 2,
      boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
      bgcolor: "white",
      display: "flex",
      alignItems: "flex-start",
      gap: 2,
      transition: "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
      "&:hover": {
        transform: "translateY(-2px)",
        boxShadow: "0 4px 8px rgba(0,0,0,0.08)",
      },
    }}
  >
    <Box
      sx={{
        p: 1.5,
        borderRadius: 2,
        bgcolor: `${color}15`,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      {icon}
    </Box>
    <Box>
      <Typography
        variant="body2"
        color="text.secondary"
        sx={{ mb: 0.5, fontSize: "0.875rem" }}
      >
        {title}
      </Typography>
      {loading ? (
        <CircularProgress size={20} sx={{ color: color }} />
      ) : (
        <Typography variant="h4" sx={{ fontWeight: 600, color: "#1a1f36" }}>
          {value}
        </Typography>
      )}
    </Box>
  </Paper>
);

const MostBookedFacultySection = () => {
  const [mostBookedFaculty, setMostBookedFaculty] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchMostBookedFaculty = async () => {
      try {
        setLoading(true);
        const data = await appointmentService.getMostBookedFaculty();
        // Use the data directly as it matches our needs
        setMostBookedFaculty(Array.isArray(data) ? data : []);
        setError(null);
      } catch (err) {
        console.error("Error fetching most booked faculty:", err);
        setError("Failed to load faculty data");
      } finally {
        setLoading(false);
      }
    };

    fetchMostBookedFaculty();
  }, []);

  if (loading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "200px",
        }}
      >
        <CircularProgress sx={{ color: "#8B0000" }} />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ textAlign: "center", color: "#ef4444", py: 3 }}>
        <Typography variant="body2">{error}</Typography>
      </Box>
    );
  }

  if (!mostBookedFaculty.length) {
    return (
      <Box sx={{ textAlign: "center", color: "#64748b", py: 3 }}>
        <Typography variant="body2">No faculty data available</Typography>
      </Box>
    );
  }

  const maxBookings = Math.max(...mostBookedFaculty.map((f) => f.bookingCount));

  return (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
      {mostBookedFaculty.map((faculty, index) => (
        <Box key={faculty.userId}>
          <Box sx={{ display: "flex", alignItems: "center", mb: 1.5 }}>
            <Avatar
              sx={{
                width: 40,
                height: 40,
                bgcolor:
                  index === 0 ? "#8B0000" : index === 1 ? "#D4A017" : "#64748B",
                mr: 2,
              }}
            >
              {faculty.name ? faculty.name.split(" ")[0][0] : "F"}
            </Avatar>
            <Box sx={{ flexGrow: 1 }}>
              <Typography
                variant="body1"
                sx={{
                  color: "#1a1f36",
                  fontWeight: 600,
                  fontSize: "0.9375rem",
                  mb: 0.5,
                }}
              >
                {faculty.name}
              </Typography>
              <Typography
                variant="body2"
                sx={{
                  color: "#64748b",
                  fontSize: "0.875rem",
                }}
              >
                {faculty.bookingCount} bookings
              </Typography>
            </Box>
          </Box>
          <Box
            sx={{
              position: "relative",
              height: 6,
              width: "100%",
              borderRadius: 3,
              bgcolor: "#f1f5f9",
            }}
          >
            <Box
              sx={{
                position: "absolute",
                top: 0,
                left: 0,
                width: `${(faculty.bookingCount / maxBookings) * 100}%`,
                height: "100%",
                borderRadius: 3,
                bgcolor:
                  index === 0 ? "#8B0000" : index === 1 ? "#D4A017" : "#64748B",
                transition: "width 0.3s ease-in-out",
              }}
            />
          </Box>
        </Box>
      ))}
    </Box>
  );
};

const AdminDashboard = () => {
  const [timeRange, setTimeRange] = useState("Month");
  const [appointmentStats, setAppointmentStats] = useState({
    confirmedAppointments: 0,
    completedAppointments: 0,
    totalAppointments: 0,
    pendingAppointments: 0,
  });
  const [statsLoading, setStatsLoading] = useState(true);
  const [mostBookedFaculty, setMostBookedFaculty] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchMostBookedFaculty = async () => {
      try {
        setLoading(true);
        const data = await appointmentService.getMostBookedFaculty();
        setMostBookedFaculty(data);
        setError(null);
      } catch (err) {
        console.error("Error fetching most booked faculty:", err);
        setError("Failed to load faculty data");
      } finally {
        setLoading(false);
      }
    };

    fetchMostBookedFaculty();
  }, []);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setStatsLoading(true);
        const stats = await appointmentService.getAppointmentStats();
        setAppointmentStats(stats);
      } catch (err) {
        console.error("Error fetching stats:", err);
      } finally {
        setStatsLoading(false);
      }
    };

    fetchStats();
  }, []);

  // Function to get the appropriate data based on selected time range
  const getChartData = () => {
    switch (timeRange) {
      case "Day":
        return {
          data: dailyData,
          dataKey: "day",
          title: "Daily Activity",
        };
      case "Year":
        return {
          data: yearlyData,
          dataKey: "year",
          title: "Yearly Activity",
        };
      default:
        return {
          data: monthlyData,
          dataKey: "month",
          title: "Monthly Activity",
        };
    }
  };

  const chartData = getChartData();

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
        {/* Header with Timeframe Selector */}
        <Box
          sx={{
            mb: 3,
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            width: "100%",
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
            Reports Overview
          </Typography>
          <Select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            size="small"
            sx={{
              minWidth: 180,
              "& .MuiOutlinedInput-notchedOutline": {
                borderColor: "#E2E8F0",
              },
              "& .MuiSelect-select": {
                py: 1,
                bgcolor: "white",
              },
            }}
          >
            <MenuItem value="Day">Daily View</MenuItem>
            <MenuItem value="Month">Monthly View</MenuItem>
            <MenuItem value="Year">Yearly View</MenuItem>
          </Select>
        </Box>

        {/* Stats Grid */}
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={4}>
            <StatsCard
              icon={<PersonIcon sx={{ fontSize: 24, color: "#8B0000" }} />}
              title="Active Users"
              value="212"
              color="#8B0000"
            />
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <StatsCard
              icon={<EventNoteIcon sx={{ fontSize: 24, color: "#8B0000" }} />}
              title="Total Consultations"
              value={appointmentStats.confirmedAppointments}
              color="#8B0000"
              loading={statsLoading}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <StatsCard
              icon={
                <CalendarMonthIcon sx={{ fontSize: 24, color: "#D4A017" }} />
              }
              title="Total Slots"
              value="148"
              color="#D4A017"
            />
          </Grid>
        </Grid>

        {/* Main Content Grid */}
        <Grid container spacing={3}>
          {/* Most Booked Faculty */}
          <Grid item xs={12} md={3}>
            <Paper
              sx={{
                p: 3,
                borderRadius: 2,
                boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
                bgcolor: "white",
                height: "100%",
                transition: "transform 0.2s ease-in-out",
                "&:hover": {
                  transform: "translateY(-2px)",
                  boxShadow: "0 4px 8px rgba(0,0,0,0.08)",
                },
              }}
            >
              <Typography
                variant="h6"
                sx={{
                  fontWeight: 600,
                  color: "#1a1f36",
                  mb: 3,
                  fontSize: "1.125rem",
                }}
              >
                Most Booked Faculty
              </Typography>
              <ErrorBoundary>
                <MostBookedFacultySection />
              </ErrorBoundary>
            </Paper>
          </Grid>

          {/* Activity Chart */}
          <Grid item xs={12} md={9}>
            <Paper
              sx={{
                p: 4,
                borderRadius: 3,
                boxShadow: "0 2px 12px rgba(0,0,0,0.03)",
                bgcolor: "white",
                height: "100%",
                transition: "transform 0.2s ease-in-out",
                "&:hover": {
                  transform: "translateY(-2px)",
                  boxShadow: "0 4px 16px rgba(0,0,0,0.06)",
                },
              }}
            >
              <Box
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  mb: 3,
                }}
              >
                <Typography
                  variant="h6"
                  sx={{
                    fontWeight: 600,
                    color: "#1a1f36",
                    fontSize: "1.125rem",
                  }}
                >
                  Activity
                </Typography>
                <Select
                  value={timeRange}
                  onChange={(e) => setTimeRange(e.target.value)}
                  size="small"
                  sx={{
                    minWidth: 100,
                    "& .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#E2E8F0",
                    },
                    "& .MuiSelect-select": {
                      py: 0.5,
                      pr: 3,
                      pl: 1.5,
                      fontSize: "0.875rem",
                      color: "#64748B",
                    },
                    "&:hover .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#8B0000",
                    },
                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#8B0000",
                    },
                  }}
                >
                  <MenuItem value="Month">Month</MenuItem>
                  <MenuItem value="Day">Day</MenuItem>
                  <MenuItem value="Year">Year</MenuItem>
                </Select>
              </Box>
              <Box
                sx={{
                  height: 350,
                  width: "100%",
                  "& .MuiChartsAxis-root": {
                    transform: "scale(0.9)",
                  },
                }}
              >
                <BarChart
                  dataset={chartData.data}
                  yAxis={[
                    {
                      scaleType: "linear",
                      tickMinStep: 100,
                      tickSize: 0,
                      gridLineStyle: {
                        stroke: "#f1f5f9",
                        strokeWidth: 1,
                      },
                    },
                  ]}
                  series={[
                    {
                      dataKey: "value",
                      label: "Consultations",
                      color: "#8B0000",
                      valueFormatter: (value) => value.toString(),
                      highlightScope: {
                        highlighted: "item",
                        faded: "global",
                      },
                    },
                  ]}
                  xAxis={[
                    {
                      dataKey: chartData.dataKey,
                      scaleType: "band",
                      tickSize: 0,
                    },
                  ]}
                  height={350}
                  margin={{
                    left: 40,
                    right: 40,
                    top: 20,
                    bottom: 30,
                  }}
                  sx={{
                    ".MuiChartsAxis-line": { display: "none" },
                    ".MuiChartsAxis-tick": { display: "none" },
                    ".MuiChartsAxis-tickLabel": {
                      fill: "#64748B",
                      fontSize: "0.75rem",
                      fontWeight: 500,
                    },
                    ".MuiBarElement-root": {
                      opacity: 0.85,
                      "&:hover": {
                        opacity: 1,
                        filter: "brightness(0.9)",
                      },
                    },
                    ".MuiChartsLegend-root": {
                      display: "none",
                    },
                  }}
                  tooltip={{
                    trigger: "item",
                  }}
                  slotProps={{
                    bar: {
                      rx: 4,
                      ry: 4,
                    },
                  }}
                />
              </Box>
            </Paper>
          </Grid>

          {/* Recent Activities */}
          <Grid item xs={12}>
            <Paper
              sx={{
                p: 3,
                borderRadius: 2,
                boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
                bgcolor: "white",
                transition: "transform 0.2s ease-in-out",
                "&:hover": {
                  transform: "translateY(-2px)",
                  boxShadow: "0 4px 8px rgba(0,0,0,0.08)",
                },
              }}
            >
              <Typography
                variant="h6"
                sx={{
                  fontWeight: 600,
                  color: "#1a1f36",
                  mb: 3,
                  fontSize: "1.125rem",
                }}
              >
                Recent Activities
              </Typography>
              <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                {recentActivities.map((activity) => (
                  <Paper
                    key={activity.id}
                    elevation={0}
                    sx={{
                      p: 2,
                      borderRadius: 2,
                      bgcolor: "#f8fafc",
                      transition: "all 0.2s ease-in-out",
                      "&:hover": {
                        transform: "translateX(4px)",
                        bgcolor: "#f1f5f9",
                      },
                    }}
                  >
                    <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
                      <Avatar
                        sx={{
                          width: 40,
                          height: 40,
                          bgcolor: "#8B0000",
                          fontSize: "1rem",
                          mr: 2,
                        }}
                      >
                        {activity.student
                          .split(" ")
                          .map((n) => n[0])
                          .join("")}
                      </Avatar>
                      <Box sx={{ flexGrow: 1 }}>
                        <Box
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "space-between",
                          }}
                        >
                          <Typography
                            variant="subtitle1"
                            sx={{
                              color: "#1a1f36",
                              fontWeight: 600,
                              fontSize: "0.9375rem",
                            }}
                          >
                            {activity.student}
                          </Typography>
                          <Chip
                            label={activity.status}
                            size="small"
                            sx={{
                              bgcolor:
                                activity.status === "Upcoming"
                                  ? "rgba(139, 0, 0, 0.1)"
                                  : "rgba(212, 160, 23, 0.1)",
                              color:
                                activity.status === "Upcoming"
                                  ? "#8B0000"
                                  : "#D4A017",
                              fontWeight: 500,
                              fontSize: "0.75rem",
                              height: 24,
                            }}
                          />
                        </Box>
                        <Typography
                          variant="body2"
                          sx={{
                            color: "#64748b",
                            fontSize: "0.875rem",
                          }}
                        >
                          {activity.type} with {activity.faculty}
                        </Typography>
                      </Box>
                    </Box>
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 3,
                        color: "#64748b",
                        fontSize: "0.8125rem",
                        pl: 7,
                      }}
                    >
                      <Box
                        sx={{ display: "flex", alignItems: "center", gap: 1 }}
                      >
                        <AccessTimeIcon sx={{ fontSize: 16 }} />
                        {activity.time}
                      </Box>
                      <Box
                        sx={{ display: "flex", alignItems: "center", gap: 1 }}
                      >
                        <CalendarMonthIcon sx={{ fontSize: 16 }} />
                        {activity.date}
                      </Box>
                    </Box>
                  </Paper>
                ))}
              </Box>
            </Paper>
          </Grid>

          {/* Calendar */}
          <Grid item xs={12}>
            <Paper
              sx={{
                p: 3,
                borderRadius: 2,
                boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
                bgcolor: "white",
                transition: "transform 0.2s ease-in-out",
                "&:hover": {
                  transform: "translateY(-2px)",
                  boxShadow: "0 4px 8px rgba(0,0,0,0.08)",
                },
              }}
            >
              <Calendar />
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </AdminLayout>
  );
};

export default AdminDashboard;
