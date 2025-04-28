import React, { useState, useEffect } from "react";
import { Box, Typography, IconButton, Paper } from "@mui/material";
import { ChevronLeft, ChevronRight } from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import appointmentService from "../services/appointmentService";

const Calendar = () => {
  const navigate = useNavigate();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAppointments();
  }, [currentDate]);

  const fetchAppointments = async () => {
    try {
      setLoading(true);
      const data = await appointmentService.getAllFacultyAppointments();
      setAppointments(data);
    } catch (err) {
      console.error("Error fetching appointments:", err);
    } finally {
      setLoading(false);
    }
  };

  const daysInMonth = new Date(
    currentDate.getFullYear(),
    currentDate.getMonth() + 1,
    0
  ).getDate();

  const firstDayOfMonth = new Date(
    currentDate.getFullYear(),
    currentDate.getMonth(),
    1
  ).getDay();

  const months = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];

  const handlePreviousMonth = () => {
    setCurrentDate(
      new Date(currentDate.getFullYear(), currentDate.getMonth() - 1)
    );
  };

  const handleNextMonth = () => {
    setCurrentDate(
      new Date(currentDate.getFullYear(), currentDate.getMonth() + 1)
    );
  };

  const isDateBooked = (date) => {
    const dateString = `${currentDate.getFullYear()}-${String(
      currentDate.getMonth() + 1
    ).padStart(2, "0")}-${String(date).padStart(2, "0")}`;
    
    return appointments.some(apt => {
      const aptDate = new Date(apt.startTime._seconds * 1000);
      return aptDate.toISOString().split('T')[0] === dateString;
    });
  };

  const handleDateClick = (date) => {
    const dateString = `${currentDate.getFullYear()}-${String(
      currentDate.getMonth() + 1
    ).padStart(2, "0")}-${String(date).padStart(2, "0")}`;
    navigate(`/admin/schedule?date=${dateString}`);
  };

  const renderCalendarDays = () => {
    const days = [];
    const previousMonthDays = firstDayOfMonth;
    const totalDays = Math.ceil((daysInMonth + previousMonthDays) / 7) * 7;

    // Previous month days
    for (let i = 0; i < previousMonthDays; i++) {
      days.push(
        <Box
          key={`prev-${i}`}
          sx={{
            p: 2,
            color: "#CBD5E1",
            textAlign: "center",
          }}
        >
          <Typography sx={{ fontSize: "14px", mb: 0.5 }}>
            {new Date(
              currentDate.getFullYear(),
              currentDate.getMonth(),
              -previousMonthDays + i + 1
            ).getDate()}
          </Typography>
        </Box>
      );
    }

    // Current month days
    for (let i = 1; i <= daysInMonth; i++) {
      const isBooked = isDateBooked(i);
      days.push(
        <Box
          key={i}
          onClick={() => handleDateClick(i)}
          sx={{
            p: 2,
            textAlign: "center",
            position: "relative",
            cursor: "pointer",
            "&:hover": {
              bgcolor: "#F8FAFC",
            },
          }}
        >
          <Typography sx={{ fontSize: "14px", mb: 0.5 }}>{i}</Typography>
          {isBooked && (
            <Typography
              sx={{
                fontSize: "10px",
                color: "#3B82F6",
                bgcolor: "#EFF6FF",
                borderRadius: "4px",
                py: 0.25,
                px: 1,
                position: "absolute",
                bottom: "8px",
                left: "50%",
                transform: "translateX(-50%)",
                whiteSpace: "nowrap",
              }}
            >
              BOOKED
            </Typography>
          )}
        </Box>
      );
    }

    // Next month days
    const remainingDays = totalDays - (previousMonthDays + daysInMonth);
    for (let i = 1; i <= remainingDays; i++) {
      days.push(
        <Box
          key={`next-${i}`}
          sx={{
            p: 2,
            color: "#CBD5E1",
            textAlign: "center",
          }}
        >
          <Typography sx={{ fontSize: "14px", mb: 0.5 }}>{i}</Typography>
        </Box>
      );
    }

    return days;
  };

  return (
    <Paper
      sx={{
        p: 3,
        borderRadius: 2,
        bgcolor: "white",
        boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column"
      }}
    >
      {/* Calendar Header */}
      <Box sx={{ display: "flex", alignItems: "center", mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          {months[currentDate.getMonth()]} {currentDate.getFullYear()}
        </Typography>
        <Box sx={{ ml: "auto", display: "flex", gap: 1 }}>
          <IconButton onClick={handlePreviousMonth} size="small">
            <ChevronLeft />
          </IconButton>
          <IconButton onClick={handleNextMonth} size="small">
            <ChevronRight />
          </IconButton>
        </Box>
      </Box>

      {/* Calendar Grid */}
      <Box sx={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "repeat(7, 1fr)",
            borderBottom: "1px solid #E2E8F0",
            mb: 1,
          }}
        >
          {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
            <Box key={day} sx={{ p: 2, textAlign: "center" }}>
              <Typography
                sx={{ color: "#64748B", fontSize: "12px", fontWeight: 500 }}
              >
                {day}
              </Typography>
            </Box>
          ))}
        </Box>
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "repeat(7, 1fr)",
            flex: 1,
            minHeight: 0
          }}
        >
          {renderCalendarDays()}
        </Box>
      </Box>
    </Paper>
  );
};

export default Calendar;
