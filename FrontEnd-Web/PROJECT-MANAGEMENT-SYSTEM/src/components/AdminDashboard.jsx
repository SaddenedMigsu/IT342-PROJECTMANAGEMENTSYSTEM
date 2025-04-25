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
import userService from "../services/userService";
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
  const [activeUsers, setActiveUsers] = useState(0);
  const [loadingActiveUsers, setLoadingActiveUsers] = useState(true);
  const [appointmentStats, setAppointmentStats] = useState({
    confirmedAppointments: 0,
    completedAppointments: 0,
    totalAppointments: 0,
    pendingAppointments: 0,
  });
  const [statsLoading, setStatsLoading] = useState(true);
  const [facultyData, setFacultyData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeRange, setTimeRange] = useState("monthly");

  useEffect(() => {
    const fetchActiveUsers = async () => {
      try {
        setLoadingActiveUsers(true);
        const count = await userService.getActiveUsersCount();
        setActiveUsers(count);
      } catch (err) {
        console.error("Error fetching active users count:", err);
        setActiveUsers(0);
      } finally {
        setLoadingActiveUsers(false);
      }
    };

    fetchActiveUsers();
  }, []);

  useEffect(() => {
    const fetchMostBookedFaculty = async () => {
      try {
        setLoading(true);
        const data = await appointmentService.getMostBookedFaculty();
        setFacultyData(data);
        setError(null);
      } catch (err) {
        console.error("Error fetching most booked faculty:", err);
        setError("Failed to load faculty data");
        setFacultyData([]);
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

  return (
    <AdminLayout>
      <Box
        sx={{
          p: { xs: 2, sm: 3, md: 4 },
          width: "100%",
          maxWidth: "100%",
          bgcolor: "#ffffff",
          minHeight: "100vh"
        }}
      >
        {/* Header */}
        <Typography
          variant="h5"
          sx={{
            fontWeight: 600,
            color: "#1a1f36",
            fontSize: { xs: "1.25rem", sm: "1.5rem" },
            mb: 4,
          }}
        >
          Reports Overview
        </Typography>

        {/* Stats Grid */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={3}>
            <StatsCard
              icon={<GroupIcon sx={{ fontSize: 24, color: "#8B0000" }} />}
              title="Active Users"
              value={activeUsers}
              color="#8B0000"
              loading={loadingActiveUsers}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <Paper
              sx={{
                p: 3,
                borderRadius: 2,
                bgcolor: "white",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                display: 'flex',
                alignItems: 'center',
                gap: 2
              }}
            >
              <Box
                sx={{
                  width: 45,
                  height: 45,
                  borderRadius: 2,
                  bgcolor: '#FFF1F1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <EventNoteIcon sx={{ fontSize: 24, color: "#8B0000" }} />
              </Box>
              <Box>
                <Typography
                  variant="body2"
                  sx={{
                    color: "#64748b",
                    fontSize: "0.875rem",
                    mb: 0.5
                  }}
                >
                  Total Consultations
                </Typography>
                <Typography
                  variant="h4"
                  sx={{
                    fontWeight: 600,
                    color: "#1a1f36",
                    fontSize: "2rem"
                  }}
                >
                  {appointmentStats.confirmedAppointments || 15}
                </Typography>
              </Box>
            </Paper>
          </Grid>
        </Grid>

        {/* Dashboard Analytics Section */}
        <Typography
          variant="h6"
          sx={{
            fontWeight: 600,
            color: "#1a1f36",
            fontSize: "1.25rem",
            mb: 3
          }}
        >
          Dashboard Analytics
        </Typography>

        {/* Main Content Grid */}
        <Grid container spacing={3}>
          {/* Activity Chart */}
          <Grid item xs={12} md={6}>
            <Paper
              sx={{
                p: { xs: 3, md: 4 },
                borderRadius: 2,
                bgcolor: "white",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                height: "100%"
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
                    fontSize: "1rem",
                  }}
                >
                  Most Booked Faculty
                </Typography>
              </Box>
              <Box
                sx={{
                  height: 400,
                  width: "100%",
                }}
              >
                {loading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                    <CircularProgress sx={{ color: '#8B0000' }} />
                  </Box>
                ) : error ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%', color: 'error.main' }}>
                    <Typography>{error}</Typography>
                  </Box>
                ) : (
                  <BarChart
                    dataset={facultyData}
                    xAxis={[
                      {
                        dataKey: "name",
                        scaleType: "band",
                      }
                    ]}
                    series={[
                      {
                        dataKey: "bookingCount",
                        label: "Total Bookings",
                        valueFormatter: (value) => value.toString(),
                        color: "#8B0000"
                      }
                    ]}
                    height={400}
                    sx={{
                      ".MuiChartsAxis-bottom .MuiChartsAxis-tickLabel": {
                        fontSize: "0.75rem",
                        fontWeight: 500,
                        transform: "rotate(-45deg)",
                        transformOrigin: "top left"
                      },
                      ".MuiBarElement-root": {
                        opacity: 0.8,
                        "&:hover": {
                          opacity: 1,
                        },
                      },
                    }}
                    slotProps={{
                      legend: {
                        hidden: true
                      }
                    }}
                  />
                )}
              </Box>
            </Paper>
          </Grid>

          {/* Calendar */}
          <Grid item xs={12} md={6}>
            <Paper
              sx={{
                p: { xs: 3, md: 4 },
                borderRadius: 2,
                bgcolor: "white",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                height: "100%",
                "& .fc": {
                  height: "500px !important"
                }
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
