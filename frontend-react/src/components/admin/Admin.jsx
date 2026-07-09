import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

const Admin = () => {
  const { showToast } = useToast();
  const [achievements, setAchievements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [createData, setCreateData] = useState({
    code: '',
    title: '',
    description: '',
    conditionType: 'TRIPS_COUNT',
    conditionValue: '',
    iconUrl: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const loadAchievements = async () => {
    setLoading(true);
    try {
      const resp = await apiFetch('/achievements');
      if (resp.ok) {
        const data = await resp.json();
        setAchievements(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки достижений', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAchievements();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const resp = await apiFetch('/admin/achievements', {
        method: 'POST',
        body: JSON.stringify({
          ...createData,
          conditionValue: parseInt(createData.conditionValue),
        }),
      });
      if (resp.ok) {
        showToast('Достижение создано');
        setCreateData({ code: '', title: '', description: '', conditionType: 'TRIPS_COUNT', conditionValue: '', iconUrl: '' });
        loadAchievements();
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка создания', 'error');
      }
    } catch (e) {
      showToast('Ошибка создания', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async (id) => {
    const ach = achievements.find(a => a.id === id);
    if (!ach) return;
    const code = prompt('Код:', ach.code);
    if (code === null) return;
    const title = prompt('Название:', ach.title);
    if (title === null) return;
    const desc = prompt('Описание:', ach.description || '');
    const condType = prompt('Тип условия:', ach.conditionType);
    const condValue = prompt('Значение:', ach.conditionValue);
    if (condValue === null) return;
    try {
      const resp = await apiFetch(`/admin/achievements/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({
          code,
          title,
          description: desc || null,
          conditionType: condType,
          conditionValue: parseInt(condValue),
        }),
      });
      if (resp.ok) {
        showToast('Достижение обновлено');
        loadAchievements();
      }
    } catch (e) {
      showToast('Ошибка обновления', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Удалить достижение?')) return;
    try {
      const resp = await apiFetch(`/admin/achievements/${id}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Достижение удалено');
        loadAchievements();
      }
    } catch (e) {
      showToast('Ошибка удаления', 'error');
    }
  };

  return (
    <div>
      <h2>Административная панель</h2>

      <Card title="Управление достижениями">
        <form onSubmit={handleCreate}>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            <Input
              placeholder="Код"
              value={createData.code}
              onChange={e => setCreateData({ ...createData, code: e.target.value })}
              required
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Название"
              value={createData.title}
              onChange={e => setCreateData({ ...createData, title: e.target.value })}
              required
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Описание"
              value={createData.description}
              onChange={e => setCreateData({ ...createData, description: e.target.value })}
              style={{ flex: 1 }}
            />
            <Select
              value={createData.conditionType}
              onChange={e => setCreateData({ ...createData, conditionType: e.target.value })}
              options={[
                { value: 'TRIPS_COUNT', label: 'Количество поездок' },
                { value: 'COUNTRIES_COUNT', label: 'Количество стран' },
                { value: 'ORDERS_COUNT', label: 'Количество заказов' },
                { value: 'REVIEWS_COUNT', label: 'Количество отзывов' },
                { value: 'SPENDING_AMOUNT', label: 'Сумма трат' },
              ]}
              style={{ flex: 1 }}
            />
            <Input
              type="number"
              placeholder="Значение"
              value={createData.conditionValue}
              onChange={e => setCreateData({ ...createData, conditionValue: e.target.value })}
              required
              style={{ flex: 1 }}
            />
            <Input
              placeholder="URL иконки"
              value={createData.iconUrl}
              onChange={e => setCreateData({ ...createData, iconUrl: e.target.value })}
              style={{ flex: 1 }}
            />
          </div>
          <Button type="submit" disabled={submitting}>Создать</Button>
        </form>

        <hr style={{ margin: '20px 0', borderColor: 'var(--color-border)' }} />

        {loading ? (
          <p>Загрузка...</p>
        ) : achievements.length === 0 ? (
          <p>Нет достижений</p>
        ) : (
          achievements.map(a => (
            <div key={a.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--color-border)', padding: '12px 0' }}>
              <div>
                <div style={{ fontWeight: 600 }}>{a.title} ({a.code})</div>
                <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>{a.description || ''}</div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <Button variant="secondary" onClick={() => handleEdit(a.id)}>Редактировать</Button>
                <Button variant="secondary" onClick={() => handleDelete(a.id)} style={{ background: 'var(--color-secondary)' }}>Удалить</Button>
              </div>
            </div>
          ))
        )}
      </Card>
    </div>
  );
};

export default Admin;