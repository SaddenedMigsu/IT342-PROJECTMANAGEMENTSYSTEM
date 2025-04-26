import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  CircularProgress,
  Alert
} from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import appointmentService from '../services/appointmentService';
import AdminLayout from './AdminLayout';

const Dashboard = () => {
  const [mostBookedFaculty, setMostBookedFaculty] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        const facultyData = await appointmentService.getMostBookedFaculty();
        
        // Transform the data for the bar chart
        const transformedData = facultyData.map(faculty => ({
          name: faculty.name,
          bookings: faculty.bookingCount
        }));
        
        setMostBookedFaculty(transformedData);
        setError(null);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
        setError('Failed to load dashboard data. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

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
            Dashboard
          </Typography>
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
              zIndex: 1
            }}
          >
            {error}
          </Alert>
        )}

        {/* Most Booked Faculty Chart */}
        <Paper
          elevation={0}
          sx={{
            position: "relative",
            zIndex: 1,
            p: 3,
            borderRadius: '24px',
            border: '1px solid rgba(226, 232, 240, 0.8)',
            backdropFilter: 'blur(20px)',
            boxShadow: '0 20px 40px rgba(0,0,0,0.08)',
            transition: 'all 0.3s ease',
            '&:hover': {
              transform: 'translateY(-4px)',
              boxShadow: '0 30px 60px rgba(0,0,0,0.12)'
            },
            height: 400,
            mb: 4
          }}
        >
          <Typography
            variant="h6"
            sx={{
              mb: 2,
              fontWeight: 600,
              color: '#1a1f36'
            }}
          >
            Most Booked Faculty Members
          </Typography>
          
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
              <CircularProgress sx={{ color: '#8B0000' }} />
            </Box>
          ) : mostBookedFaculty.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={mostBookedFaculty}
                margin={{
                  top: 20,
                  right: 30,
                  left: 20,
                  bottom: 60
                }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="name" 
                  angle={-45}
                  textAnchor="end"
                  height={60}
                  interval={0}
                  tick={{
                    fill: '#64748B',
                    fontSize: 12
                  }}
                />
                <YAxis
                  tick={{
                    fill: '#64748B',
                    fontSize: 12
                  }}
                  label={{ 
                    value: 'Number of Bookings', 
                    angle: -90, 
                    position: 'insideLeft',
                    fill: '#64748B'
                  }}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                    border: '1px solid #E2E8F0',
                    borderRadius: '8px',
                    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
                  }}
                />
                <Bar 
                  dataKey="bookings" 
                  fill="#8B0000"
                  radius={[4, 4, 0, 0]}
                  maxBarSize={50}
                />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
              <Typography sx={{ color: '#64748B' }}>
                No booking data available
              </Typography>
            </Box>
          )}
        </Paper>

        {/* Add more dashboard components here */}
      </Box>
    </AdminLayout>
  );
};

export default Dashboard; 