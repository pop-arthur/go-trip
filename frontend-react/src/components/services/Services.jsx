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
  }, [filterType, filterLocation, filterProvider]);

  return (
    <div>
      <h2>Дополнительные услуги</h2>

      <Card title="Список услуг">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
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
          <Button variant="secondary" onClick={loadServices}>Поиск</Button>
        </div>

        {loading ? (
          <p>Загрузка...</p>
        ) : services.length === 0 ? (
          <p>Нет услуг</p>
        ) : (
          services.map(s => (
            <div key={s.id} style={{ borderBottom: '1px solid var(--color-border)', padding: '12px 0' }}>
              <div style={{ fontWeight: 600 }}>{s.title} ({s.service_type})</div>
              <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>
                {s.description || ''} {s.price_amount ? s.price_amount + ' ' + s.price_currency : ''}
              </div>
            </div>
          ))
        )}
      </Card>
    </div>
  );
};

export default Services;