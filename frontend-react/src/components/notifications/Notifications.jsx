import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import { useLocalStorage } from '../../hooks/useLocalStorage';

const Notifications = () => {
  const { showToast } = useToast();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [enabled, setEnabled] = useLocalStorage('notificationEnabled', true);
  const [saving, setSaving] = useState(false);

  const loadPreferences = async () => {
    try {
      const resp = await apiFetch('/notification-preferences');
      if (resp.ok) {
        const data = await resp.json();
        setEnabled(data.isEnabled);
      } else if (resp.status === 404) {
        // создать настройки по умолчанию
        const createResp = await apiFetch('/notification-preferences', {
          method: 'PUT',
          body: JSON.stringify({ isEnabled: true }),
        });
        if (createResp.ok) {
          const data = await createResp.json();
          setEnabled(data.isEnabled);
        }
      }
    } catch (e) {
      showToast('Ошибка загрузки настроек уведомлений', 'error');
    }
  };

  const loadNotifications = async () => {
    try {
      const resp = await apiFetch('/notifications');
      if (resp.ok) {
        const data = await resp.json();
        setNotifications(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки уведомлений', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPreferences();
    loadNotifications();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      const resp = await apiFetch('/notification-preferences', {
        method: 'PUT',
        body: JSON.stringify({ isEnabled: enabled }),
      });
      if (resp.ok) {
        showToast('Настройки сохранены');
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка сохранения', 'error');
      }
    } catch (e) {
      showToast('Ошибка сохранения', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <h2>Уведомления</h2>

      <Card title="Настройки уведомлений">
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <label>
            <input
              type="checkbox"
              checked={enabled}
              onChange={e => setEnabled(e.target.checked)}
            /> Получать уведомления
          </label>
          <Button onClick={handleSave} disabled={saving}>Сохранить</Button>
        </div>
      </Card>

      <Card title="История уведомлений">
        {loading ? (
          <p>Загрузка...</p>
        ) : notifications.length === 0 ? (
          <p>Нет уведомлений</p>
        ) : (
          notifications.map(n => (
            <div key={n.id} style={{ borderBottom: '1px solid var(--color-border)', padding: '8px 0' }}>
              <div style={{ fontWeight: 600 }}>{n.title}</div>
              <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>
                {n.body} — {new Date(n.sent_at).toLocaleString()}
              </div>
              <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                {n.is_read ? 'Прочитано' : 'Новое'}
              </span>
            </div>
          ))
        )}
      </Card>
    </div>
  );
};

export default Notifications;