import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

const Locations = () => {
  const { showToast } = useToast();
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState('');
  const [filterCountry, setFilterCountry] = useState('');
  const [filterCity, setFilterCity] = useState('');
  const [filterQuery, setFilterQuery] = useState('');
  const [createData, setCreateData] = useState({
    name: '',
    type: 'CITY',
    country: '',
    city: '',
    address: '',
    latitude: '',
    longitude: '',
  });
  const [submitting, setSubmitting] = useState(false);

  const loadLocations = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterType) params.append('type', filterType);
      if (filterCountry) params.append('country', filterCountry);
      if (filterCity) params.append('city', filterCity);
      if (filterQuery) params.append('query', filterQuery);
      const resp = await apiFetch(`/locations?${params.toString()}`);
      if (resp.ok) {
        const data = await resp.json();
        setLocations(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки локаций', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLocations();
  }, [filterType, filterCountry, filterCity, filterQuery]);

  const handleCreate = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const body = {
        ...createData,
        latitude: createData.latitude ? parseFloat(createData.latitude) : null,
        longitude: createData.longitude ? parseFloat(createData.longitude) : null,
      };
      const resp = await apiFetch('/locations', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      if (resp.ok) {
        showToast('Локация создана');
        setCreateData({ name: '', type: 'CITY', country: '', city: '', address: '', latitude: '', longitude: '' });
        loadLocations();
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
    const loc = locations.find(l => l.id === id);
    if (!loc) return;
    const name = prompt('Название:', loc.name);
    if (name === null) return;
    const type = prompt('Тип:', loc.type);
    if (type === null) return;
    const country = prompt('Страна:', loc.country || '');
    const city = prompt('Город:', loc.city || '');
    try {
      const resp = await apiFetch(`/locations/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ name, type, country: country || null, city: city || null }),
      });
      if (resp.ok) {
        showToast('Локация обновлена');
        loadLocations();
      }
    } catch (e) {
      showToast('Ошибка обновления', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Удалить локацию?')) return;
    try {
      const resp = await apiFetch(`/locations/${id}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Локация удалена');
        loadLocations();
      }
    } catch (e) {
      showToast('Ошибка удаления', 'error');
    }
  };

  return (
    <div>
      <h2>Локации</h2>

      <Card title="Создать локацию">
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
                { value: 'COUNTRY', label: 'Страна' },
                { value: 'CITY', label: 'Город' },
                { value: 'AIRPORT', label: 'Аэропорт' },
                { value: 'TRAIN_STATION', label: 'Вокзал' },
                { value: 'BUS_STATION', label: 'Автовокзал' },
                { value: 'PORT', label: 'Порт' },
                { value: 'HOTEL', label: 'Отель' },
                { value: 'MEETING_POINT', label: 'Место сбора' },
                { value: 'ATTRACTION', label: 'Достопримечательность' },
                { value: 'OTHER', label: 'Другое' },
              ]}
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Страна"
              value={createData.country}
              onChange={e => setCreateData({ ...createData, country: e.target.value })}
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Город"
              value={createData.city}
              onChange={e => setCreateData({ ...createData, city: e.target.value })}
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Адрес"
              value={createData.address}
              onChange={e => setCreateData({ ...createData, address: e.target.value })}
              style={{ flex: 1 }}
            />
            <Input
              type="number"
              step="any"
              placeholder="Широта"
              value={createData.latitude}
              onChange={e => setCreateData({ ...createData, latitude: e.target.value })}
              style={{ flex: 1 }}
            />
            <Input
              type="number"
              step="any"
              placeholder="Долгота"
              value={createData.longitude}
              onChange={e => setCreateData({ ...createData, longitude: e.target.value })}
              style={{ flex: 1 }}
            />
          </div>
          <Button type="submit" disabled={submitting}>Создать</Button>
        </form>
      </Card>

      <Card title="Поиск локаций">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
          <Select
            value={filterType}
            onChange={e => setFilterType(e.target.value)}
            options={[
              { value: '', label: 'Все' },
              { value: 'COUNTRY', label: 'Страна' },
              { value: 'CITY', label: 'Город' },
              { value: 'AIRPORT', label: 'Аэропорт' },
              { value: 'TRAIN_STATION', label: 'Вокзал' },
              { value: 'BUS_STATION', label: 'Автовокзал' },
              { value: 'PORT', label: 'Порт' },
              { value: 'HOTEL', label: 'Отель' },
              { value: 'MEETING_POINT', label: 'Место сбора' },
              { value: 'ATTRACTION', label: 'Достопримечательность' },
              { value: 'OTHER', label: 'Другое' },
            ]}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="Страна"
            value={filterCountry}
            onChange={e => setFilterCountry(e.target.value)}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="Город"
            value={filterCity}
            onChange={e => setFilterCity(e.target.value)}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="Запрос"
            value={filterQuery}
            onChange={e => setFilterQuery(e.target.value)}
            style={{ flex: 1 }}
          />
        </div>

        {loading ? (
          <p>Загрузка...</p>
        ) : locations.length === 0 ? (
          <p>Нет локаций</p>
        ) : (
          locations.map(l => (
            <div key={l.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--color-border)', padding: '12px 0' }}>
              <div>
                <div style={{ fontWeight: 600 }}>{l.name} ({l.type})</div>
                <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>{l.country || ''} {l.city || ''} {l.address || ''}</div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <Button variant="secondary" onClick={() => handleEdit(l.id)}>Редактировать</Button>
                <Button variant="secondary" onClick={() => handleDelete(l.id)} style={{ background: 'var(--color-secondary)' }}>Удалить</Button>
              </div>
            </div>
          ))
        )}
      </Card>
    </div>
  );
};

export default Locations;