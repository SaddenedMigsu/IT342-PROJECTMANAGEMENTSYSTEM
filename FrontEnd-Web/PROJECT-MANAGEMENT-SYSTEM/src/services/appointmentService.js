import api from "./axiosConfig";

const appointmentService = {
  getMostBookedFaculty: async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await api.get("/appointments/faculty/most-booked", {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching most booked faculty:', error);
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem('token');
        window.location.href = '/admin/login';
      }
      throw error;
    }
  },

  getAppointmentStats: async () => {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        throw new Error("No authentication token found");
      }

      const response = await api.get("/appointments/stats", {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      return response.data;
    } catch (error) {
      console.error("Error fetching appointment stats:", error);
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        window.location.href = "/admin/login";
      }
      throw error;
    }
  },

  // Get all faculty appointments
  getAllFacultyAppointments: async () => {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        throw new Error("No authentication token found");
      }

      console.log('Fetching appointments with token:', token);
      
      const response = await api.get("/appointments/all", {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      
      // Transform the response data to handle Firestore timestamps
      const appointments = response.data.map(appointment => ({
        ...appointment,
        startTime: appointment.startTime ? {
          _seconds: appointment.startTime.seconds,
          _nanoseconds: appointment.startTime.nanoseconds
        } : null,
        endTime: appointment.endTime ? {
          _seconds: appointment.endTime.seconds,
          _nanoseconds: appointment.endTime.nanoseconds
        } : null,
        createdAt: appointment.createdAt ? {
          _seconds: appointment.createdAt.seconds,
          _nanoseconds: appointment.createdAt.nanoseconds
        } : null,
        updatedAt: appointment.updatedAt ? {
          _seconds: appointment.updatedAt.seconds,
          _nanoseconds: appointment.updatedAt.nanoseconds
        } : null
      }));

      console.log('Transformed appointments:', appointments);
      return appointments;
    } catch (error) {
      console.error("Error fetching faculty appointments:", {
        message: error.message,
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        config: {
          url: error.config?.url,
          method: error.config?.method,
          headers: error.config?.headers,
          baseURL: error.config?.baseURL
        }
      });
      
      if (error.response?.status === 401 || error.response?.status === 403) {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        window.location.href = "/admin/login";
      }
      throw error;
    }
  },

  // Format date for display
  formatDate: (timestamp) => {
    if (!timestamp) return '';
    
    // Handle Firestore timestamp format
    if (typeof timestamp === 'object') {
      if ('_seconds' in timestamp) {
        return new Date(timestamp._seconds * 1000).toLocaleString();
      } else if ('seconds' in timestamp) {
        return new Date(timestamp.seconds * 1000).toLocaleString();
      }
    }
    
    return new Date(timestamp).toLocaleString();
  },

  // Get appointment status color
  getStatusColor: (status) => {
    switch (status) {
      case 'SCHEDULED':
        return '#16a34a'; // green
      case 'PENDING':
        return '#eab308'; // yellow
      case 'CANCELLED':
        return '#ef4444'; // red
      default:
        return '#64748b'; // gray
    }
  }
};

export default appointmentService;
