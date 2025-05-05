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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
} from "@mui/material";
import { BarChart } from "@mui/x-charts";
import AdminLayout from "./AdminLayout";
import Calendar from "./Calendar";
import PersonIcon from "@mui/icons-material/Person";
import EventNoteIcon from "@mui/icons-material/EventNote";
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import GroupIcon from "@mui/icons-material/Group";
import FileDownloadIcon from '@mui/icons-material/FileDownload';
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
      p: 4,
      borderRadius: 2,
      boxShadow: "0 2px 4px rgba(0,0,0,0.04)",
      bgcolor: "white",
      display: "flex",
      alignItems: "center",
      gap: 3,
      height: "100%",
      transition: "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
      "&:hover": {
        transform: "translateY(-2px)",
        boxShadow: "0 4px 8px rgba(0,0,0,0.08)",
      },
    }}
  >
    <Box
      sx={{
        width: 60,
        height: 60,
        borderRadius: 2,
        bgcolor: '#FFF1F1',
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
        sx={{
          color: "#64748b",
          fontSize: "1rem",
          mb: 1,
        }}
      >
        {title}
      </Typography>
      {loading ? (
        <CircularProgress size={32} sx={{ color: color }} />
      ) : (
        <Typography variant="h3" sx={{ fontWeight: 600, color: "#1a1f36", fontSize: "2.5rem" }}>
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
        // Ensure we have an array and it's properly formatted
        const formattedData = Array.isArray(data) ? data.map(faculty => ({
          userId: faculty.userId,
          name: faculty.name,
          bookingCount: faculty.bookingCount
        })) : [];
        setMostBookedFaculty(formattedData);
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
                  index === 0 ? "#8B0000" : 
                  index === 1 ? "#D4A017" : 
                  index === 2 ? "#64748B" :
                  index === 3 ? "#4B5563" :
                  "#6B7280",
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
                  index === 0 ? "#8B0000" : 
                  index === 1 ? "#D4A017" : 
                  index === 2 ? "#64748B" :
                  index === 3 ? "#4B5563" :
                  "#6B7280",
                transition: "width 0.3s ease-in-out",
              }}
            />
          </Box>
        </Box>
      ))}
    </Box>
  );
};

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

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
  const [exportModalOpen, setExportModalOpen] = useState(false);
  const [exportStartDate, setExportStartDate] = useState('');
  const [exportEndDate, setExportEndDate] = useState('');
  const [exportLoading, setExportLoading] = useState(false);
  const [exportError, setExportError] = useState('');

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
        // Transform the data to match the expected format for the chart
        const transformedData = Array.isArray(data) ? data.map(faculty => ({
          name: faculty.name,
          bookingCount: faculty.bookingCount
        })) : [];
        setFacultyData(transformedData);
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

  const handleExportExcel = async () => {
    setExportError('');
    if (!exportStartDate || !exportEndDate) {
      setExportError('Please select both start and end dates.');
      return;
    }
    setExportLoading(true);
    try {
      const token = localStorage.getItem('token');
      if (!token) throw new Error('Not authenticated');
      const url = `https://it342-projectmanagementsystem.onrender.com/api/appointments/export?startDate=${encodeURIComponent(exportStartDate)}&endDate=${encodeURIComponent(exportEndDate)}`;
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      if (!response.ok) throw new Error('Failed to export data');
      const blob = await response.blob();
      downloadBlob(blob, 'consultations_export.csv');
      setExportModalOpen(false);
      setExportStartDate('');
      setExportEndDate('');
    } catch (err) {
      setExportError(err.message || 'Export failed');
    } finally {
      setExportLoading(false);
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
        <Grid container spacing={3} sx={{ mb: 4, alignItems: 'center' }}>
          <Grid item xs={12} sm={6} md={4}>
            <StatsCard
              icon={<GroupIcon sx={{ fontSize: 32, color: "#8B0000" }} />}
              title="Active Users"
              value={activeUsers}
              color="#8B0000"
              loading={loadingActiveUsers}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={8} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Paper
              sx={{
                p: 4,
                borderRadius: 2,
                bgcolor: "white",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                display: 'flex',
                alignItems: 'center',
                gap: 3,
                height: '100%'
              }}
            >
              <Box
                sx={{
                  width: 60,
                  height: 60,
                  borderRadius: 2,
                  bgcolor: '#FFF1F1',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <EventNoteIcon sx={{ fontSize: 32, color: "#8B0000" }} />
              </Box>
              <Box>
                <Typography
                  variant="body2"
                  sx={{
                    color: "#64748b",
                    fontSize: "1rem",
                    mb: 1
                  }}
                >
                  Total Consultations
                </Typography>
                <Typography
                  variant="h3"
                  sx={{
                    fontWeight: 600,
                    color: "#1a1f36",
                    fontSize: "2.5rem"
                  }}
                >
                  {appointmentStats.confirmedAppointments || 15}
                </Typography>
              </Box>
            </Paper>
            <Box sx={{ ml: { xs: 0, sm: 2 }, mt: { xs: 2, sm: 0 } }}>
              <Button
                variant="contained"
                startIcon={<FileDownloadIcon sx={{ fontSize: 22 }} />}
                onClick={() => setExportModalOpen(true)}
                sx={{
                  bgcolor: '#8B0000',
                  color: 'white',
                  fontWeight: 700,
                  fontSize: '1rem',
                  borderRadius: '16px',
                  px: 3,
                  py: 1.5,
                  boxShadow: '0 4px 16px rgba(139,0,0,0.08)',
                  textTransform: 'none',
                  letterSpacing: 0,
                  ml: { xs: 0, sm: 2 },
                  mt: { xs: 2, sm: 0 },
                  transition: 'background 0.2s, box-shadow 0.2s',
                  '&:hover': {
                    bgcolor: '#6B0000',
                    boxShadow: '0 8px 24px rgba(139,0,0,0.12)'
                  }
                }}
              >
                Export Excel
              </Button>
              <Dialog open={exportModalOpen} onClose={() => setExportModalOpen(false)}>
                <DialogTitle sx={{ fontWeight: 600, color: '#8B0000' }}>Export Consultations to CSV</DialogTitle>
                <DialogContent
                  sx={{
                    minWidth: 350,
                    bgcolor: '#fafbfc',
                    borderRadius: 2,
                    p: 3,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 3
                  }}
                >
                  <Typography sx={{ fontWeight: 600, color: '#1a1f36', mb: 1 }}>
                    Select the date range to export consultations as CSV.
                  </Typography>
                  <Box>
                    <Typography sx={{ fontWeight: 500, color: '#1a1f36', mb: 0.5 }}>
                      Start Date
                    </Typography>
                    <TextField
                      type="datetime-local"
                      value={exportStartDate}
                      onChange={e => setExportStartDate(e.target.value)}
                      fullWidth
                      sx={{
                        '& .MuiInputBase-input': {
                          fontSize: '1.1rem',
                          padding: '18px 14px',
                          minHeight: '32px'
                        }
                      }}
                      InputLabelProps={{ shrink: true }}
                      placeholder="Select start date"
                    />
                  </Box>
                  <Box>
                    <Typography sx={{ fontWeight: 500, color: '#1a1f36', mb: 0.5 }}>
                      End Date
                    </Typography>
                    <TextField
                      type="datetime-local"
                      value={exportEndDate}
                      onChange={e => setExportEndDate(e.target.value)}
                      fullWidth
                      sx={{
                        '& .MuiInputBase-input': {
                          fontSize: '1.1rem',
                          padding: '18px 14px',
                          minHeight: '32px'
                        }
                      }}
                      InputLabelProps={{ shrink: true }}
                      placeholder="Select end date"
                    />
                  </Box>
                  {exportError && (
                    <Typography sx={{ color: '#ef4444', mt: 1 }}>{exportError}</Typography>
                  )}
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2 }}>
                  <Button onClick={() => setExportModalOpen(false)} sx={{ color: '#64748B', textTransform: 'none' }}>Cancel</Button>
                  <Button
                    onClick={handleExportExcel}
                    variant="contained"
                    sx={{
                      bgcolor: '#8B0000',
                      color: 'white',
                      '&:hover': { bgcolor: '#6B0000' },
                      borderRadius: '12px',
                      textTransform: 'none',
                      fontWeight: 500,
                      px: 3
                    }}
                    disabled={exportLoading}
                  >
                    {exportLoading ? 'Exporting...' : 'Export'}
                  </Button>
                </DialogActions>
              </Dialog>
            </Box>
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
          <Grid item xs={12} md={5}>
            <Paper
              sx={{
                p: { xs: 3, md: 4 },
                borderRadius: 2,
                bgcolor: "white",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                height: "100%",
                width: "100%",
                overflow: "hidden"
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
                  overflow: "auto"
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
                    width={800}
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
          <Grid item xs={12} md={7}>
            <Paper
              sx={{
                p: { xs: 3, md: 4 },
                borderRadius: 2,
                bgcolor: "white",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                height: "100%",
                width: "100%",
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
