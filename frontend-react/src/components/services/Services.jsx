import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Select from '../common/Select';
import Input from '../common/Input';

const Services = () => {
  const { showToast } = useToast();
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState('');
  const [filterLocation, setFilterLocation] = useState('');
  const [filterProvider, setFilterProvider] = useState('');

  // Модалка
  const [selectedService, setSelectedService] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [ratingSummary, setRatingSummary] = useState(null);

  const loadServices = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterType) params.append('serviceType', filterType);
      if (filterLocation) params.append('locationId', filterLocation);
      if (filterProvider) params.append('providerId', filterProvider);
      const resp = await apiFetch(`/additional-services?${params.toString()}`);
      if (resp.ok) {
        const data = await resp.json();
        setServices(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки услуг', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadServices();
    // eslint-disable-next-line
  }, [filterType, filterLocation, filterProvider]);

  const handleViewDetails = async (id) => {
    try {
      const resp = await apiFetch(`/additional-services/${id}`);
      if (resp.ok) {
        const data = await resp.json();
        setSelectedService(data);
        try {
          const ratingResp = await apiFetch(`/reviews/rating-summary?targetType=ADDITIONAL_SERVICE&targetId=${id}`);
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
    setSelectedService(null);
    setRatingSummary(null);
  };

  return (
    <div>
      <h2><i className="fas fa-concierge-bell" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>Дополнительные услуги</h2>

      <Card title="Список услуг" icon="fa-list">
        <div className="flex-row" style={{ marginBottom: 16 }}>
          <Select
            value={filterType}
            onChange={e => setFilterType(e.target.value)}
            options={[
              { value: '', label: 'Все' },
              { value: 'FLIGHT', label: 'Авиа' },
              { value: 'TRAIN', label: 'Поезд' },
              { value: 'BUS', label: 'Автобус' },
              { value: 'HOTEL', label: 'Отель' },
              { value: 'TOUR', label: 'Тур' },
              { value: 'CAR_RENTAL', label: 'Аренда авто' },
              { value: 'INSURANCE', label: 'Страховка' },
              { value: 'TAXI', label: 'Такси' },
              { value: 'ESIM', label: 'eSIM' },
              { value: 'LOUNGE', label: 'Лаунж' },
              { value: 'EXTRA_BAGGAGE', label: 'Доп. багаж' },
              { value: 'OTHER', label: 'Другое' },
            ]}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="ID локации"
            value={filterLocation}
            onChange={e => setFilterLocation(e.target.value)}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="ID провайдера"
            value={filterProvider}
            onChange={e => setFilterProvider(e.target.value)}
            style={{ flex: 1 }}
          />
          <Button variant="secondary" onClick={loadServices}><i className="fas fa-search"></i> Поиск</Button>
        </div>

        {loading ? (
          <p>Загрузка...</p>
        ) : services.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-box"></i>
            <p>Нет услуг</p>
          </div>
        ) : (
          services.map(s => (
            <div key={s.id} className="list-item">
              <div className="info">
                <div className="title">{s.title} ({s.service_type})</div>
                <div className="sub">{s.description || ''} {s.price_amount ? s.price_amount + ' ' + s.price_currency : ''}</div>
              </div>
              <div className="actions">
                <Button variant="secondary" className="btn-sm" onClick={() => handleViewDetails(s.id)}>
                  <i className="fas fa-eye"></i>
                </Button>
              </div>
            </div>
          ))
        )}
      </Card>

      {showModal && selectedService && (
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
            <h3>{selectedService.title}</h3>
            <div style={{ marginTop: 16 }}>
              <p><strong>ID:</strong> {selectedService.id}</p>
              <p><strong>Тип:</strong> {selectedService.service_type}</p>
              <p><strong>Описание:</strong> {selectedService.description || '—'}</p>
              <p><strong>Цена:</strong> {selectedService.price_amount ? `${selectedService.price_amount} ${selectedService.price_currency}` : '—'}</p>
              <p><strong>Активна:</strong> {selectedService.is_active ? '✅ Да' : '❌ Нет'}</p>
              <p><strong>Провайдер:</strong> {selectedService.provider_id || '—'}</p>
              <p><strong>Локация:</strong> {selectedService.location_id || '—'}</p>
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

export default Services;