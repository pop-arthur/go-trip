import React from 'react';
import { Link, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import styles from './Layout.module.css';

const Layout = () => {
  const { user, logout, isAdmin } = useAuth();
  const location = useLocation();

  const navItems = [
    { path: '/profile', label: 'Профиль', icon: 'fa-user' },
    { path: '/notifications', label: 'Уведомления', icon: 'fa-bell' },
    { path: '/achievements', label: 'Достижения', icon: 'fa-trophy' },
    { path: '/reviews', label: 'Отзывы', icon: 'fa-star' },
    { path: '/locations', label: 'Локации', icon: 'fa-location-dot' },
    { path: '/providers', label: 'Провайдеры', icon: 'fa-building' },
    { path: '/services', label: 'Доп. услуги', icon: 'fa-concierge-bell' },
    { path: '/trips', label: 'Поездки', icon: 'fa-plane' },
    { path: '/statistics', label: 'Статистика', icon: 'fa-chart-simple' },
    { path: '/recommendations', label: 'Рекомендации', icon: 'fa-lightbulb' },
    ...(isAdmin ? [{ path: '/admin', label: 'Админ', icon: 'fa-shield-halved' }] : []),
  ];

  return (
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <div className={styles.logo}>
          <i className="fas fa-globe-asia" style={{ fontSize: 24, color: 'var(--color-primary-dark)' }}></i>
          <span>GO-TRIP</span>
        </div>
        <div className={styles.userInfo}>
          <div className={styles.email}><i className="fas fa-envelope" style={{ marginRight: 8 }}></i>{user?.email}</div>
          <div className={styles.roles}><i className="fas fa-user-tag" style={{ marginRight: 8 }}></i>{user?.roles?.join(', ') || 'Пользователь'}</div>
        </div>
        <nav className={styles.nav}>
          {navItems.map(item => (
            <Link
              key={item.path}
              to={item.path}
              className={location.pathname === item.path ? styles.active : ''}
            >
              <i className={`fas ${item.icon}`} style={{ width: 20 }}></i>
              {item.label}
            </Link>
          ))}
        </nav>
        <button className={styles.logout} onClick={logout}>
          <i className="fas fa-sign-out-alt"></i> Выйти
        </button>
      </aside>
      <main className={styles.content}>
        <Outlet />
      </main>
    </div>
  );
};

export default Layout;