import React from "react";
import { Box, Typography, Button } from "@mui/material";

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error: error,
      errorInfo: errorInfo,
    });
    console.error("Error caught by boundary:", error, errorInfo);
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            minHeight: "400px",
            p: 3,
            textAlign: "center",
          }}
        >
          <Typography
            variant="h5"
            sx={{ mb: 2, color: "#1a1f36", fontWeight: 600 }}
          >
            Something went wrong
          </Typography>
          <Typography variant="body1" sx={{ mb: 3, color: "#64748b" }}>
            We're sorry, but there was an error loading this content.
          </Typography>
          <Button
            variant="contained"
            onClick={this.handleRetry}
            sx={{
              bgcolor: "#8B0000",
              "&:hover": { bgcolor: "#6B0000" },
            }}
          >
            Retry
          </Button>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
