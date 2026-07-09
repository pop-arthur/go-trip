import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

const Providers = () => {
  const { showToast } = useToast();
  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState('');
  const [filterQuery, setFilterQuery] = useState('');
  const [createData, setCreateData] = useState({
    name: '',
    type: 'OTHER',
    website: '',
    support_contact: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const loadProviders = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterType) params.append('type', filterType);
      if (filterQuery) params.append('query', filterQuery);
      const resp = await apiFetch(`/providers?${params.toString()}`);
      if (resp.ok) {
        const data = await resp.json();
        setProviders(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки провайдеров', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProviders();
  }, [filterType, filterQuery]);

  const handleCreate = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const resp = await apiFetch('/providers', {
        method: 'POST',
        body: JSON.stringify(createData),
      });
      if (resp.ok) {
        showToast('Провайдер создан');
        setCreateData({ name: '', type: 'OTHER', website: '', support_contact: '' });
        loadProviders();
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

  const handleDelete = async (id) => {
    if (!confirm('Удалить провайдера?')) return;
    try {
      const resp = await apiFetch(`/providers/${id}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Провайдер удалён');
        loadProviders();
      }
    } catch (e) {
      showToast('Ошибка удаления', 'error');
    }
  };

  return (
    <div>
      <h2>Провайдеры</h2>

      <Card title="Создать провайдера">
        <form onSubmit={handleCreate}>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
            <Input
              placeholder="Название"
              value={createData.name}
              onChange={e => setCreateData({ ...createData, name: e.target.value })}
              required
              style={{ flex: 1 }}
            />
            <Select
              value={createData.type}
              onChange={e => setCreateData({ ...createData, type: e.target.value })}
              options={[
                { value: 'AIRLINE', label: 'Авиакомпания' },
                { value: 'HOTEL', label: 'Отель' },
                { value: 'TOUR_COMPANY', label: 'Туроператор' },
                { value: 'TRANSPORT_COMPANY', label: 'Транспортная компания' },
                { value: 'BOOKING_PLATFORM', label: 'Платформа бронирования' },
                { value: 'INSURANCE_COMPANY', label: 'Страховая компания' },
                { value: 'OTHER', label: 'Другое' },
              ]}
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Сайт"
              value={createData.website}
              onChange={e => setCreateData({ ...createData, website: e.target.value })}
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Контакты"
              value={createData.support_contact}
              onChange={e => setCreateData({ ...createData, support_contact: e.target.value })}
              style={{ flex: 1 }}
            />
          </div>
          <Button type="submit" disabled={submitting}>Создать</Button>
        </form>
      </Card>

      <Card title="Список провайдеров">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
          <Select
            value={filterType}
            onChange={e => setFilterType(e.target.value)}
            options={[
              { value: '', label: 'Все' },
              { value: 'AIRLINE', label: 'Авиакомпания' },
              { value: 'HOTEL', label: 'Отель' },
              { value: 'TOUR_COMPANY', label: 'Туроператор' },
              { value: 'TRANSPORT_COMPANY', label: 'Транспортная компания' },
              { value: 'BOOKING_PLATFORM', label: 'Платформа бронирования' },
              { value: 'INSURANCE_COMPANY', label: 'Страховая компания' },
              { value: 'OTHER', label: 'Другое' },
            ]}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="Запрос"
            value={filterQuery}
            onChange={e => setFilterQuery(e.target.value)}
            style={{ flex: 2 }}
          />
          <Button variant="secondary" onClick={loadProviders}>Поиск</Button>
        </div>

        {loading ? (
          <p>Загрузка...</p>
        ) : providers.length === 0 ? (
          <p>Нет провайдеров</p>
        ) : (
          providers.map(p => (
            <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--color-border)', padding: '12px 0' }}>
              <div>
                <div style={{ fontWeight: 600 }}>{p.name} ({p.type})</div>
                <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>{p.website || ''} {p.support_contact || ''}</div>
              </div>
              <Button variant="secondary" onClick={() => handleDelete(p.id)} style={{ background: 'var(--color-secondary)' }}>Удалить</Button>
            </div>
          ))
        )}
      </Card>
    </div>
  );
};

export default Providers;