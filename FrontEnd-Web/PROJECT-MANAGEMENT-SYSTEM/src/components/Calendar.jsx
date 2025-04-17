import React, { useState } from "react";
import { Box, Typography, IconButton, Paper } from "@mui/material";
import { ChevronLeft, ChevronRight } from "@mui/icons-material";

const Calendar = () => {
  const [currentDate, setCurrentDate] = useState(new Date(2025, 2)); // March 2025

  // Mock booked dates
  const bookedDates = [
    "2025-03-05",
    "2025-03-13",
    "2025-03-15",
    "2025-03-18",
    "2025-03-20",
    "2025-03-25",
    "2025-03-29",
    "2025-03-31",
  ];

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
    return bookedDates.includes(dateString);
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
      <Box sx={{ mb: 2 }}>
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
          }}
        >
          {renderCalendarDays()}
        </Box>
      </Box>
    </Paper>
  );
};

export default Calendar;
