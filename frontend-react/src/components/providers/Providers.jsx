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

  // Модалка для просмотра
  const [selectedProvider, setSelectedProvider] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [ratingSummary, setRatingSummary] = useState(null);

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
    // eslint-disable-next-line
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
    // eslint-disable-next-line no-restricted-globals
    if (!window.confirm('Удалить провайдера?')) return;
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

  const handleViewDetails = async (id) => {
    try {
      const resp = await apiFetch(`/providers/${id}`);
      if (resp.ok) {
        const data = await resp.json();
        setSelectedProvider(data);
        try {
          const ratingResp = await apiFetch(`/reviews/rating-summary?targetType=PROVIDER&targetId=${id}`);
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
    setSelectedProvider(null);
    setRatingSummary(null);
  };

  return (
    <div>
      <h2><i className="fas fa-building" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>Провайдеры</h2>

      <Card title="Создать провайдера" icon="fa-plus-circle">
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
          <Button type="submit" disabled={submitting}><i className="fas fa-save"></i> Создать</Button>
        </form>
      </Card>

      <Card title="Список провайдеров" icon="fa-list">
        <div className="flex-row" style={{ marginBottom: 16 }}>
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
            style={{ flex: 1 }}
          />
          <Button variant="secondary" onClick={loadProviders}><i className="fas fa-search"></i> Поиск</Button>
        </div>

        {loading ? (
          <p>Загрузка...</p>
        ) : providers.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-building"></i>
            <p>Нет провайдеров</p>
          </div>
        ) : (
          providers.map(p => (
            <div key={p.id} className="list-item">
              <div className="info">
                <div className="title">{p.name} ({p.type})</div>
                <div className="sub">{p.website || ''} {p.support_contact || ''}</div>
              </div>
              <div className="actions">
                <Button variant="secondary" className="btn-sm" onClick={() => handleViewDetails(p.id)}>
                  <i className="fas fa-eye"></i>
                </Button>
                <Button variant="danger" className="btn-sm" onClick={() => handleDelete(p.id)}>
                  <i className="fas fa-trash"></i>
                </Button>
              </div>
            </div>
          ))
        )}
      </Card>

      {showModal && selectedProvider && (
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
            <h3>{selectedProvider.name}</h3>
            <div style={{ marginTop: 16 }}>
              <p><strong>ID:</strong> {selectedProvider.id}</p>
              <p><strong>Тип:</strong> {selectedProvider.type}</p>
              <p><strong>Сайт:</strong> {selectedProvider.website || '—'}</p>
              <p><strong>Контакты:</strong> {selectedProvider.support_contact || '—'}</p>
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

export default Providers;