import api from "./axiosConfig";

const appointmentService = {
  getMostBookedFaculty: async () => {
    try {
      const response = await api.get("/appointments/faculty/most-booked");
      return response.data;
    } catch (error) {
      console.error("Error fetching most booked faculty:", error);
      throw error;
    }
  },

  getAppointmentStats: async () => {
    try {
      const response = await api.get("/appointments/stats");
      return response.data;
    } catch (error) {
      console.error("Error fetching appointment stats:", error);
      throw error;
    }
  },
};

export default appointmentService;
