import api from "./axiosConfig";

const userService = {
  // Get all users (admin only)
  getAllUsers: async () => {
    try {
      const response = await api.get("/users/all");
      return response.data;
    } catch (error) {
      console.error("Error fetching users:", error);
      throw error;
    }
  },

  // Delete user (admin only)
  deleteUser: async (userId) => {
    try {
      const response = await api.delete("/users/${userId}");
      return response.data;
    } catch (error) {
      console.error("Error deleting user:", error);
      throw error;
    }
  },
};

export default userService;
