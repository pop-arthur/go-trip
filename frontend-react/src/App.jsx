import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ToastProvider } from './contexts/ToastContext';
import Layout from './components/common/Layout';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import Profile from './components/profile/Profile';
import Notifications from './components/notifications/Notifications';
import Achievements from './components/achievements/Achievements';
import Reviews from './components/reviews/Reviews';
import Locations from './components/locations/Locations';
import Providers from './components/providers/Providers';
import Services from './components/services/Services';
import Trips from './components/trips/Trips';
import TripDetail from './components/trips/TripDetail';
import Statistics from './components/statistics/Statistics';
import Recommendations from './components/recommendations/Recommendations';
import Admin from './components/admin/Admin';
import './styles/index.css';

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return <div>Загрузка...</div>;
  if (!user) return <Navigate to="/login" replace />;
  return children;
};

const AdminRoute = ({ children }) => {
  const { user, loading, isAdmin } = useAuth();
  if (loading) return <div>Загрузка...</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (!isAdmin) return <Navigate to="/profile" replace />;
  return children;
};

function App() {
  return (
    <BrowserRouter
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
              <Route index element={<Navigate to="/profile" />} />
              <Route path="profile" element={<Profile />} />
              <Route path="notifications" element={<Notifications />} />
              <Route path="achievements" element={<Achievements />} />
              <Route path="reviews" element={<Reviews />} />
              <Route path="locations" element={<Locations />} />
              <Route path="providers" element={<Providers />} />
              <Route path="services" element={<Services />} />
              <Route path="trips" element={<Trips />} />
              <Route path="trips/:tripId" element={<TripDetail />} />
              <Route path="statistics" element={<Statistics />} />
              <Route path="recommendations" element={<Recommendations />} />
              <Route path="admin" element={<AdminRoute><Admin /></AdminRoute>} />
            </Route>
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;