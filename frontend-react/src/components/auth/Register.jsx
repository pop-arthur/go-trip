import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import Button from '../common/Button';
import Input from '../common/Input';
import Card from '../common/Card';

const Register = () => {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(email, password, fullName);
      navigate('/profile');
    } catch (err) {
      setError(err.message || 'Ошибка регистрации');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 420, margin: '60px auto' }}>
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <i className="fas fa-globe-asia" style={{ fontSize: 48, color: 'var(--color-primary-dark)' }}></i>
        <h1 style={{ fontSize: 32, fontWeight: 800, marginTop: 8 }}>GO-TRIP</h1>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 16 }}>travel plan service</p>
      </div>
      <Card>
        <h2 style={{ textAlign: 'center' }}>Регистрация</h2>
        <form onSubmit={handleSubmit}>
          <div style={{ position: 'relative' }}>
            <i className="fas fa-user" style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-secondary)' }}></i>
            <Input
              type="text"
              placeholder="Полное имя"
              value={fullName}
              onChange={e => setFullName(e.target.value)}
              style={{ paddingLeft: 40 }}
            />
          </div>
          <div style={{ position: 'relative' }}>
            <i className="fas fa-envelope" style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-secondary)' }}></i>
            <Input
              type="email"
              placeholder="Email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
              style={{ paddingLeft: 40 }}
            />
          </div>
          <div style={{ position: 'relative' }}>
            <i className="fas fa-lock" style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-secondary)' }}></i>
            <Input
              type="password"
              placeholder="Пароль (мин. 8)"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              minLength="8"
              style={{ paddingLeft: 40 }}
            />
          </div>
          {error && <div style={{ color: 'var(--color-accent-dark)', marginBottom: 12 }}>{error}</div>}
          <Button type="submit" disabled={loading} fullWidth>
            {loading ? 'Загрузка...' : 'Зарегистрироваться'}
          </Button>
        </form>
        <p style={{ marginTop: 16, fontSize: 14, textAlign: 'center' }}>
          Уже есть аккаунт? <Link to="/login" style={{ color: 'var(--color-primary-dark)', fontWeight: 600 }}>Войти</Link>
        </p>
      </Card>
    </div>
  );
};

export default Register;