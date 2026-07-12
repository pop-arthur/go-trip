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

  // Модалка для просмотра деталей локации
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [ratingSummary, setRatingSummary] = useState(null);

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
    // eslint-disable-next-line
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
    const name = window.prompt('Название:', loc.name);
    if (name === null) return;
    const type = window.prompt('Тип:', loc.type);
    if (type === null) return;
    const country = window.prompt('Страна:', loc.country || '');
    const city = window.prompt('Город:', loc.city || '');
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
    // eslint-disable-next-line no-restricted-globals
    if (!window.confirm('Удалить локацию?')) return;
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

  const handleViewDetails = async (id) => {
    try {
      // Загружаем детали локации
      const resp = await apiFetch(`/locations/${id}`);
      if (resp.ok) {
        const data = await resp.json();
        setSelectedLocation(data);
        // Загружаем рейтинг-суммари
        try {
          const ratingResp = await apiFetch(`/reviews/rating-summary?targetType=LOCATION&targetId=${id}`);
          if (ratingResp.ok) {
            const ratingData = await ratingResp.json();
            setRatingSummary(ratingData);
          } else {
            setRatingSummary(null);
          }
        } catch (e) {
          setRatingSummary(null);
        }
        setShowModal(true);
      } else {
        showToast('Ошибка загрузки данных', 'error');
      }
    } catch (e) {
      showToast('Ошибка загрузки данных', 'error');
    }
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedLocation(null);
    setRatingSummary(null);
  };

  return (
    <div>
      <h2><i className="fas fa-location-dot" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>Локации</h2>

      <Card title="Создать локацию" icon="fa-plus-circle">
        <form onSubmit={handleCreate}>
          <div className="flex-row">
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
          <Button type="submit" disabled={submitting}><i className="fas fa-save"></i> Создать</Button>
        </form>
      </Card>

      <Card title="Поиск локаций" icon="fa-search">
        <div className="flex-row" style={{ marginBottom: 16 }}>
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
          <div className="empty-state">
            <i className="fas fa-globe"></i>
            <p>Нет локаций</p>
          </div>
        ) : (
          locations.map(l => (
            <div key={l.id} className="list-item">
              <div className="info">
                <div className="title">{l.name} ({l.type})</div>
                <div className="sub">{l.country || ''} {l.city || ''} {l.address || ''}</div>
              </div>
              <div className="actions">
                <Button variant="secondary" className="btn-sm" onClick={() => handleViewDetails(l.id)}>
                  <i className="fas fa-eye"></i>
                </Button>
                <Button variant="secondary" className="btn-sm" onClick={() => handleEdit(l.id)}>
                  <i className="fas fa-edit"></i>
                </Button>
                <Button variant="danger" className="btn-sm" onClick={() => handleDelete(l.id)}>
                  <i className="fas fa-trash"></i>
                </Button>
              </div>
            </div>
          ))
        )}
      </Card>

      {/* Модалка просмотра */}
      {showModal && selectedLocation && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 9999,
        }} onClick={closeModal}>
          <div style={{
            background: 'white',
            borderRadius: 'var(--border-radius)',
            padding: '32px',
            maxWidth: '500px',
            width: '100%',
            maxHeight: '80vh',
            overflow: 'auto',
          }} onClick={e => e.stopPropagation()}>
            <h3>{selectedLocation.name}</h3>
            <div style={{ marginTop: 16 }}>
              <p><strong>ID:</strong> {selectedLocation.id}</p>
              <p><strong>Тип:</strong> {selectedLocation.type}</p>
              <p><strong>Страна:</strong> {selectedLocation.country || '—'}</p>
              <p><strong>Город:</strong> {selectedLocation.city || '—'}</p>
              <p><strong>Адрес:</strong> {selectedLocation.address || '—'}</p>
              <p><strong>Широта:</strong> {selectedLocation.latitude || '—'}</p>
              <p><strong>Долгота:</strong> {selectedLocation.longitude || '—'}</p>
              {ratingSummary && (
                <div style={{ marginTop: 12, padding: 12, background: 'var(--color-bg)', borderRadius: 'var(--border-radius-sm)' }}>
                  <p><strong>⭐ Средняя оценка:</strong> {ratingSummary.averageRating ? ratingSummary.averageRating.toFixed(1) : 'нет оценок'}</p>
                  <p><strong>📝 Количество отзывов:</strong> {ratingSummary.reviewCount || 0}</p>
                </div>
              )}
            </div>
            <Button onClick={closeModal} style={{ marginTop: 16 }}>Закрыть</Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Locations;