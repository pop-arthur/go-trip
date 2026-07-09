import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';

const Profile = () => {
  const { user, refreshUser } = useAuth();
  const { showToast } = useToast();
  const [email, setEmail] = useState(user?.email || '');
  const [fullName, setFullName] = useState(user?.full_name || '');
  const [loading, setLoading] = useState(false);

  const handleUpdate = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const resp = await apiFetch('/users/me', {
        method: 'PATCH',
        body: JSON.stringify({ email, full_name: fullName || null }),
      });
      if (resp.ok) {
        await refreshUser();
        showToast('Профиль обновлён');
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка обновления', 'error');
      }
    } catch (e) {
      showToast('Ошибка обновления', 'error');
    } finally {
      setLoading(false);
    }
  };

  if (!user) return <p>Загрузка...</p>;

  return (
    <div>
      <h2>Профиль</h2>
      <Card title="Личные данные">
        <form onSubmit={handleUpdate}>
          <Input
            type="email"
            label="Email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
          <Input
            type="text"
            label="Полное имя"
            value={fullName}
            onChange={e => setFullName(e.target.value)}
          />
          <Button type="submit" disabled={loading}>Обновить</Button>
        </form>
      </Card>
      <Card title="Информация">
        <p><strong>ID:</strong> {user.id}</p>
        <p><strong>Роли:</strong> {(user.roles || []).join(', ')}</p>
        <p><strong>Дата регистрации:</strong> {new Date(user.created_at).toLocaleString()}</p>
      </Card>
    </div>
  );
};

export default Profile;