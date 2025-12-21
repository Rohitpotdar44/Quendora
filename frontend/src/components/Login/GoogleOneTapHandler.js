// Handles Google's One Tap/Sign-In response
export function handleCredentialResponse(response) {
  const idToken = response.credential;
  // Send this token to your backend endpoint for Google authentication
  fetch("http://localhost:8087/api/google-login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ token: idToken })
  })
    .then(res => res.json())
    .then(data => {
      // Here, do what you do on successful login: save the JWT, set state, redirect, etc.
      console.log("Google login successful:", data);
      // For example:
      // localStorage.setItem('auth_state', ...)
      // navigate('/dashboard');
    })
    .catch(err => {
      console.error("Google login error:", err);
    });
}

// Register globally for Google
if (typeof window !== "undefined") {
  window.handleCredentialResponse = handleCredentialResponse;
}

