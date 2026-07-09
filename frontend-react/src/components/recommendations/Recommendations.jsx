import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Select from '../common/Select';

const Recommendations = () => {
  const { showToast } = useToast();
  const [trips, setTrips] = useState([]);
  const [selectedTrip, setSelectedTrip] = useState('');
  const [tripRecs, setTripRecs] = useState([]);
  const [loadingTrip, setLoadingTrip] = useState(false);

  const [orders, setOrders] = useState([]);
  const [selectedTripForOrder, setSelectedTripForOrder] = useState('');
  const [selectedOrder, setSelectedOrder] = useState('');
  const [orderRecs, setOrderRecs] = useState([]);
  const [loadingOrder, setLoadingOrder] = useState(false);

  const loadTrips = async () => {
    try {
      const resp = await apiFetch('/trips');
      if (resp.ok) {
        const data = await resp.json();
        setTrips(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки поездок', 'error');
    }
  };

  const loadOrdersForTrip = async (tripId) => {
    if (!tripId) {
      setOrders([]);
      setSelectedOrder('');
      return;
    }
    try {
      const resp = await apiFetch(`/trips/${tripId}/orders`);
      if (resp.ok) {
        const data = await resp.json();
        setOrders(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки заказов', 'error');
    }
  };

  useEffect(() => {
    loadTrips();
  }, []);

  useEffect(() => {
    if (selectedTripForOrder) {
      loadOrdersForTrip(selectedTripForOrder);
    } else {
      setOrders([]);
      setSelectedOrder('');
    }
  }, [selectedTripForOrder]);

  const handleTripRecommend = async () => {
    if (!selectedTrip) {
      showToast('Выберите поездку', 'error');
      return;
    }
    setLoadingTrip(true);
    try {
      const resp = await apiFetch(`/trips/${selectedTrip}/recommendations`);
      if (resp.ok) {
        const data = await resp.json();
        setTripRecs(data);
      } else {
        showToast('Рекомендации не найдены', 'error');
      }
    } catch (e) {
      showToast('Ошибка загрузки рекомендаций', 'error');
    } finally {
      setLoadingTrip(false);
    }
  };

  const handleOrderRecommend = async () => {
    if (!selectedOrder) {
      showToast('Выберите заказ', 'error');
      return;
    }
    setLoadingOrder(true);
    try {
      const resp = await apiFetch(`/orders/${selectedOrder}/recommendations`);
      if (resp.ok) {
        const data = await resp.json();
        setOrderRecs(data);
      } else {
        showToast('Рекомендации не найдены', 'error');
      }
    } catch (e) {
      showToast('Ошибка загрузки рекомендаций', 'error');
    } finally {
      setLoadingOrder(false);
    }
  };

  return (
    <div>
      <h2>Рекомендации</h2>

      <Card title="Рекомендации для поездки">
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <Select
            value={selectedTrip}
            onChange={e => setSelectedTrip(e.target.value)}
            options={[{ value: '', label: 'Выберите поездку' }, ...trips.map(t => ({ value: t.id, label: t.title }))]}
            style={{ flex: 1 }}
          />
          <Button onClick={handleTripRecommend} disabled={loadingTrip}>
            {loadingTrip ? 'Загрузка...' : 'Показать рекомендации'}
          </Button>
        </div>
        <div style={{ marginTop: 16 }}>
          {tripRecs.length === 0 ? (
            <p>Нет рекомендаций</p>
          ) : (
            tripRecs.map((r, idx) => (
              <div key={idx} style={{ borderBottom: '1px solid var(--color-border)', padding: '8px 0' }}>
                <div style={{ fontWeight: 600 }}>{r.service.title}</div>
                <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>{r.reason} {r.score ? `(оценка: ${r.score})` : ''}</div>
              </div>
            ))
          )}
        </div>
      </Card>

      <Card title="Рекомендации для заказа">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
          <Select
            value={selectedTripForOrder}
            onChange={e => setSelectedTripForOrder(e.target.value)}
            options={[{ value: '', label: 'Выберите поездку' }, ...trips.map(t => ({ value: t.id, label: t.title }))]}
            style={{ flex: 1 }}
          />
          <Select
            value={selectedOrder}
            onChange={e => setSelectedOrder(e.target.value)}
            disabled={!selectedTripForOrder}
            options={[{ value: '', label: 'Выберите заказ' }, ...orders.map(o => ({ value: o.id, label: o.title }))]}
            style={{ flex: 1 }}
          />
          <Button onClick={handleOrderRecommend} disabled={loadingOrder || !selectedOrder}>
            {loadingOrder ? 'Загрузка...' : 'Показать рекомендации'}
          </Button>
        </div>
        <div style={{ marginTop: 16 }}>
          {orderRecs.length === 0 ? (
            <p>Нет рекомендаций</p>
          ) : (
            orderRecs.map((r, idx) => (
              <div key={idx} style={{ borderBottom: '1px solid var(--color-border)', padding: '8px 0' }}>
                <div style={{ fontWeight: 600 }}>{r.service.title}</div>
                <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>{r.reason} {r.score ? `(оценка: ${r.score})` : ''}</div>
              </div>
            ))
          )}
        </div>
      </Card>
    </div>
  );
};

export default Recommendations;