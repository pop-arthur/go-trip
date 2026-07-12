import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

const serviceIcons = {
  FLIGHT: 'fa-plane',
  TRAIN: 'fa-train',
  BUS: 'fa-bus',
  HOTEL: 'fa-hotel',
  TOUR: 'fa-route',
  CAR_RENTAL: 'fa-car',
  INSURANCE: 'fa-shield',
  TAXI: 'fa-taxi',
  ESIM: 'fa-sim-card',
  LOUNGE: 'fa-chair',
  EXTRA_BAGGAGE: 'fa-suitcase-rolling',
  OTHER: 'fa-tag',
};

const statusColors = {
  PENDING_VERIFICATION: '#fff3c4',
  CONFIRMED: '#b8d4e3',
  DELAYED: '#f0d4d4',
  CANCELLED: '#e0d4d4',
  COMPLETED: '#c8e6c9',
  REFUND_PENDING: '#fff3c4',
  REFUNDED: '#e0d4d4',
};

const statusLabels = {
  PENDING_VERIFICATION: 'Ожидает проверки',
  CONFIRMED: 'Подтверждён',
  DELAYED: 'Задержан',
  CANCELLED: 'Отменён',
  COMPLETED: 'Завершён',
  REFUND_PENDING: 'Возврат средств',
  REFUNDED: 'Возвращён',
};

const TripDetail = () => {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [trip, setTrip] = useState(null);
  const [orders, setOrders] = useState([]);
  const [tripLocations, setTripLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('route');

  const [orderFormVisible, setOrderFormVisible] = useState(false);
  const [orderData, setOrderData] = useState({
    service_type: 'FLIGHT',
    title: '',
    status: 'PENDING_VERIFICATION',
    price_amount: '',
    price_currency: '',
    provider_id: '',
    external_order_id: '',
    start_datetime: '',
    end_datetime: '',
    departure_location_id: '',
    arrival_location_id: '',
  });
  const [providers, setProviders] = useState([]);
  const [locations, setLocations] = useState([]);

  const [locationFormVisible, setLocationFormVisible] = useState(false);
  const [locationData, setLocationData] = useState({
    location_id: '',
    visit_order: '',
    arrival_date: '',
    departure_date: '',
  });

  const [files, setFiles] = useState([]);
  const [fileFormVisible, setFileFormVisible] = useState(false);
  const [fileData, setFileData] = useState({ file_url: '', file_type: 'PDF' });
  const [selectedOrderId, setSelectedOrderId] = useState(null);

  const loadTrip = async () => {
    try {
      const resp = await apiFetch(`/trips/${tripId}`);
      if (resp.ok) setTrip(await resp.json());
    } catch (e) {
      showToast('Ошибка загрузки поездки', 'error');
    }
  };

  const loadOrders = async () => {
    try {
      const resp = await apiFetch(`/trips/${tripId}/orders`);
      if (resp.ok) setOrders(await resp.json());
    } catch (e) {
      showToast('Ошибка загрузки заказов', 'error');
    }
  };

  const loadTripLocations = async () => {
    try {
      const resp = await apiFetch(`/trips/${tripId}/locations`);
      if (resp.ok) {
        const data = await resp.json();
        setTripLocations(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки локаций маршрута', 'error');
    }
  };

  const loadFiles = async (orderId) => {
    try {
      const resp = await apiFetch(`/orders/${orderId}/files`);
      if (resp.ok) setFiles(await resp.json());
    } catch (e) {
      showToast('Ошибка загрузки файлов', 'error');
    }
  };

  const loadSelectData = async () => {
    try {
      const [provResp, locResp] = await Promise.all([
        apiFetch('/providers'),
        apiFetch('/locations'),
      ]);
      if (provResp.ok) setProviders(await provResp.json());
      if (locResp.ok) setLocations(await locResp.json());
    } catch (e) {
      showToast('Ошибка загрузки данных для формы', 'error');
    }
  };

  useEffect(() => {
    const init = async () => {
      await loadTrip();
      await loadOrders();
      await loadTripLocations();
      await loadSelectData();
      setLoading(false);
    };
    init();
    // eslint-disable-next-line
  }, [tripId]);

  // --- Заказы ---
  const handleCreateOrder = async (e) => {
    e.preventDefault();
    if (!orderData.title) {
      showToast('Название заказа обязательно', 'error');
      return;
    }
    try {
      const body = {
        ...orderData,
        price_amount: orderData.price_amount ? parseFloat(orderData.price_amount) : null,
        provider_id: orderData.provider_id || null,
        departure_location_id: orderData.departure_location_id || null,
        arrival_location_id: orderData.arrival_location_id || null,
        start_datetime: orderData.start_datetime ? new Date(orderData.start_datetime).toISOString() : null,
        end_datetime: orderData.end_datetime ? new Date(orderData.end_datetime).toISOString() : null,
      };
      const resp = await apiFetch(`/trips/${tripId}/orders`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      if (resp.ok) {
        showToast('Заказ создан');
        setOrderFormVisible(false);
        setOrderData({
          service_type: 'FLIGHT',
          title: '',
          status: 'PENDING_VERIFICATION',
          price_amount: '',
          price_currency: '',
          provider_id: '',
          external_order_id: '',
          start_datetime: '',
          end_datetime: '',
          departure_location_id: '',
          arrival_location_id: '',
        });
        loadOrders();
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка создания заказа', 'error');
      }
    } catch (e) {
      showToast('Ошибка создания заказа', 'error');
    }
  };

  const handleEditOrder = async (orderId) => {
    const order = orders.find(o => o.id === orderId);
    if (!order) return;
    const newTitle = window.prompt('Название:', order.title);
    if (newTitle === null) return;
    const newStatus = window.prompt('Статус (PENDING_VERIFICATION/CONFIRMED/DELAYED/CANCELLED/COMPLETED/REFUND_PENDING/REFUNDED):', order.status);
    if (!newStatus) return;
    const newPrice = window.prompt('Цена:', order.price_amount || '');
    const newCurrency = window.prompt('Валюта:', order.price_currency || '');
    try {
      const resp = await apiFetch(`/orders/${orderId}`, {
        method: 'PATCH',
        body: JSON.stringify({
          title: newTitle,
          status: newStatus,
          price_amount: newPrice ? parseFloat(newPrice) : null,
          price_currency: newCurrency || null,
        }),
      });
      if (resp.ok) {
        showToast('Заказ обновлён');
        loadOrders();
      }
    } catch (e) {
      showToast('Ошибка обновления заказа', 'error');
    }
  };

  const handleChangeStatus = async (orderId) => {
    const newStatus = window.prompt('Новый статус (PENDING_VERIFICATION/CONFIRMED/DELAYED/CANCELLED/COMPLETED/REFUND_PENDING/REFUNDED):');
    if (!newStatus) return;
    const reason = window.prompt('Причина (опционально):');
    try {
      const resp = await apiFetch(`/orders/${orderId}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status: newStatus, reason: reason || null }),
      });
      if (resp.ok) {
        showToast('Статус обновлён');
        loadOrders();
      }
    } catch (e) {
      showToast('Ошибка изменения статуса', 'error');
    }
  };

  const handleDeleteOrder = async (orderId) => {
    if (!window.confirm('Удалить заказ? Это действие нельзя отменить.')) return;
    try {
      const resp = await apiFetch(`/orders/${orderId}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Заказ удалён');
        loadOrders();
      }
    } catch (e) {
      showToast('Ошибка удаления заказа', 'error');
    }
  };

  // --- Локации маршрута ---
  const handleCreateLocation = async (e) => {
    e.preventDefault();
    if (!locationData.location_id) {
      showToast('Выберите локацию', 'error');
      return;
    }
    try {
      const body = {
        location_id: locationData.location_id,
        visit_order: locationData.visit_order ? parseInt(locationData.visit_order) : undefined,
        arrival_date: locationData.arrival_date ? new Date(locationData.arrival_date).toISOString() : null,
        departure_date: locationData.departure_date ? new Date(locationData.departure_date).toISOString() : null,
      };
      const resp = await apiFetch(`/trips/${tripId}/locations`, {
        method: 'POST',
        body: JSON.stringify(body),
      });
      if (resp.ok) {
        showToast('Локация добавлена в маршрут');
        setLocationFormVisible(false);
        setLocationData({ location_id: '', visit_order: '', arrival_date: '', departure_date: '' });
        loadTripLocations();
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка добавления локации', 'error');
      }
    } catch (e) {
      showToast('Ошибка добавления локации', 'error');
    }
  };

  const handleEditLocation = async (locationId) => {
    const loc = tripLocations.find(l => l.id === locationId);
    if (!loc) {
      showToast('Локация не найдена', 'error');
      return;
    }

    const visitOrderInput = window.prompt('Новый порядок посещения (число > 0):', loc.visit_order);
    if (visitOrderInput === null) return;

    const newOrder = parseInt(visitOrderInput, 10);
    if (isNaN(newOrder) || newOrder < 1) {
      showToast('Порядок должен быть положительным числом', 'error');
      return;
    }

    if (newOrder === loc.visit_order) {
      showToast('Порядок не изменился', 'info');
      return;
    }

    const conflictingLoc = tripLocations.find(l => l.id !== locationId && l.visit_order === newOrder);

    try {
      if (conflictingLoc) {
        const maxOrder = tripLocations.reduce((max, l) => Math.max(max, l.visit_order), 0);
        const tempOrder = maxOrder + 1;

        const resp1 = await apiFetch(`/trips/${tripId}/locations/${locationId}`, {
          method: 'PATCH',
          body: JSON.stringify({ visit_order: tempOrder }),
        });
        if (!resp1.ok) throw new Error('Не удалось установить временный порядок');

        const resp2 = await apiFetch(`/trips/${tripId}/locations/${conflictingLoc.id}`, {
          method: 'PATCH',
          body: JSON.stringify({ visit_order: loc.visit_order }),
        });
        if (!resp2.ok) throw new Error('Не удалось обновить конфликтующую локацию');

        const resp3 = await apiFetch(`/trips/${tripId}/locations/${locationId}`, {
          method: 'PATCH',
          body: JSON.stringify({ visit_order: newOrder }),
        });
        if (!resp3.ok) throw new Error('Не удалось установить новый порядок');

        showToast('Порядок успешно изменен');
      } else {
        const resp = await apiFetch(`/trips/${tripId}/locations/${locationId}`, {
          method: 'PATCH',
          body: JSON.stringify({ visit_order: newOrder }),
        });
        if (!resp.ok) {
          const text = await resp.text();
          throw new Error(text || 'Ошибка обновления');
        }
        showToast('Локация обновлена');
      }

      loadTripLocations();
    } catch (e) {
      showToast('Ошибка: ' + e.message, 'error');
      console.error('Update location error:', e);
    }
  };

  const handleDeleteLocation = async (locationId) => {
    if (!window.confirm('Удалить эту локацию из маршрута?')) return;
    try {
      const resp = await apiFetch(`/trips/${tripId}/locations/${locationId}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Локация удалена из маршрута');
        loadTripLocations();
      }
    } catch (e) {
      showToast('Ошибка удаления', 'error');
    }
  };

  // --- Файлы ---
  const handleAddFile = async (e) => {
    e.preventDefault();
    if (!fileData.file_url) {
      showToast('URL файла обязателен', 'error');
      return;
    }
    try {
      const resp = await apiFetch(`/orders/${selectedOrderId}/files`, {
        method: 'POST',
        body: JSON.stringify(fileData),
      });
      if (resp.ok) {
        showToast('Файл добавлен');
        setFileFormVisible(false);
        setFileData({ file_url: '', file_type: 'PDF' });
        loadFiles(selectedOrderId);
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка добавления файла', 'error');
      }
    } catch (e) {
      showToast('Ошибка добавления файла', 'error');
    }
  };

  const handleDeleteFile = async (fileId) => {
    if (!window.confirm('Удалить файл?')) return;
    try {
      const resp = await apiFetch(`/orders/${selectedOrderId}/files/${fileId}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Файл удалён');
        loadFiles(selectedOrderId);
      }
    } catch (e) {
      showToast('Ошибка удаления файла', 'error');
    }
  };

  const handleViewFile = (fileUrl) => {
    const baseUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080';
    window.open(`${baseUrl}/${fileUrl}`, '_blank');
  };

  if (loading) return <div className="loading-state">Загрузка данных поездки...</div>;
  if (!trip) return <div className="error-state">Поездка не найдена</div>;

  return (
    <div>
      <button onClick={() => navigate('/trips')} style={{ background: 'none', border: 'none', color: 'var(--color-primary-dark)', cursor: 'pointer', marginBottom: 16, fontSize: 16 }}>
        <i className="fas fa-arrow-left"></i> Назад к списку
      </button>

      <h2><i className="fas fa-route" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>{trip.title}</h2>

      <Card>
        <div className="flex-row" style={{ flexWrap: 'wrap', gap: 8 }}>
          <span className="status-badge" style={{ background: statusColors[trip.status] || 'var(--color-primary)', color: '#2d2a2a' }}>
            {trip.status === 'PLANNED' ? 'Запланирована' :
             trip.status === 'ACTIVE' ? 'Активна' :
             trip.status === 'COMPLETED' ? 'Завершена' : 'Отменена'}
          </span>
          <span><i className="fas fa-calendar-alt"></i> {trip.start_date || '—'} — {trip.end_date || '—'}</span>
          <span><i className="fas fa-id-card"></i> ID: {trip.id.slice(0, 8)}</span>
        </div>
      </Card>

      {/* Табы */}
      <div className="flex-row" style={{ marginBottom: 16, borderBottom: '1px solid var(--color-border)' }}>
        <button
          onClick={() => setActiveTab('route')}
          style={{
            padding: '10px 20px',
            borderBottom: activeTab === 'route' ? '2px solid var(--color-primary-dark)' : 'none',
            background: 'none',
            cursor: 'pointer',
            fontWeight: activeTab === 'route' ? 700 : 400,
          }}
        >
          <i className="fas fa-map-pin"></i> Маршрут
        </button>
        <button
          onClick={() => setActiveTab('orders')}
          style={{
            padding: '10px 20px',
            borderBottom: activeTab === 'orders' ? '2px solid var(--color-primary-dark)' : 'none',
            background: 'none',
            cursor: 'pointer',
            fontWeight: activeTab === 'orders' ? 700 : 400,
          }}
        >
          <i className="fas fa-ticket"></i> Заказы ({orders.length})
        </button>
      </div>

      {/* Содержимое вкладок */}
      {activeTab === 'route' && (
        <Card title="Маршрут" icon="fa-route">
          <Button onClick={() => setLocationFormVisible(true)} style={{ marginBottom: 16 }}>
            <i className="fas fa-plus"></i> Добавить локацию
          </Button>

          {locationFormVisible && (
            <div style={{ marginBottom: 20, padding: 16, background: 'var(--color-bg)', borderRadius: 'var(--border-radius-sm)' }}>
              <h4><i className="fas fa-pen"></i> Добавить локацию</h4>
              <form onSubmit={handleCreateLocation}>
                <div className="flex-row">
                  <Select
                    value={locationData.location_id}
                    onChange={e => setLocationData({ ...locationData, location_id: e.target.value })}
                    options={[
                      { value: '', label: 'Выберите локацию' },
                      ...locations.map(l => ({ value: l.id, label: l.name })),
                    ]}
                    style={{ flex: 1 }}
                  />
                  <Input
                    type="number"
                    placeholder="Порядок (опционально)"
                    value={locationData.visit_order}
                    onChange={e => setLocationData({ ...locationData, visit_order: e.target.value })}
                    style={{ flex: 1 }}
                  />
                  <Input
                    type="datetime-local"
                    placeholder="Прибытие (опционально)"
                    value={locationData.arrival_date}
                    onChange={e => setLocationData({ ...locationData, arrival_date: e.target.value })}
                    style={{ flex: 1 }}
                  />
                  <Input
                    type="datetime-local"
                    placeholder="Отбытие (опционально)"
                    value={locationData.departure_date}
                    onChange={e => setLocationData({ ...locationData, departure_date: e.target.value })}
                    style={{ flex: 1 }}
                  />
                </div>
                <div className="flex-row" style={{ marginTop: 12 }}>
                  <Button type="submit"><i className="fas fa-save"></i> Добавить</Button>
                  <Button variant="secondary" onClick={() => setLocationFormVisible(false)}><i className="fas fa-times"></i> Отмена</Button>
                </div>
              </form>
            </div>
          )}

          {tripLocations.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-map-marker-alt"></i>
              <p>В маршруте пока нет локаций. Добавьте места, которые вы хотите посетить.</p>
            </div>
          ) : (
            tripLocations.map(l => {
              const locName = locations.find(loc => loc.id === l.location_id)?.name || l.location_id.slice(0, 8);
              return (
                <div key={l.id} className="list-item">
                  <div className="info">
                    <div className="title">
                      <i className="fas fa-map-marker-alt" style={{ marginRight: 8 }}></i>
                      {locName} <span style={{ fontSize: 14, color: 'var(--color-text-secondary)', marginLeft: 8 }}>— порядок {l.visit_order}</span>
                    </div>
                    <div className="sub">
                      Прибытие: {l.arrival_date ? new Date(l.arrival_date).toLocaleString() : '—'} 
                      &nbsp;| Отбытие: {l.departure_date ? new Date(l.departure_date).toLocaleString() : '—'}
                    </div>
                  </div>
                  <div className="actions">
                    <Button variant="secondary" className="btn-sm" onClick={() => handleEditLocation(l.id)}>
                      <i className="fas fa-edit"></i>
                    </Button>
                    <Button variant="danger" className="btn-sm" onClick={() => handleDeleteLocation(l.id)}>
                      <i className="fas fa-trash"></i>
                    </Button>
                  </div>
                </div>
              );
            })
          )}
        </Card>
      )}

      {activeTab === 'orders' && (
        <Card title="Заказы" icon="fa-ticket">
          <Button onClick={() => setOrderFormVisible(true)} style={{ marginBottom: 16 }}>
            <i className="fas fa-plus"></i> Добавить заказ
          </Button>

          {orderFormVisible && (
            <div style={{ marginBottom: 20, padding: 16, background: 'var(--color-bg)', borderRadius: 'var(--border-radius-sm)' }}>
              <h4><i className="fas fa-pen"></i> Новый заказ</h4>
              <form onSubmit={handleCreateOrder}>
                <div className="flex-row">
                  <Select
                    value={orderData.service_type}
                    onChange={e => setOrderData({ ...orderData, service_type: e.target.value })}
                    options={[
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
                    placeholder="Название *"
                    value={orderData.title}
                    onChange={e => setOrderData({ ...orderData, title: e.target.value })}
                    required
                    style={{ flex: 1 }}
                  />
                  <Select
                    value={orderData.status}
                    onChange={e => setOrderData({ ...orderData, status: e.target.value })}
                    options={[
                      { value: 'PENDING_VERIFICATION', label: 'Ожидает проверки' },
                      { value: 'CONFIRMED', label: 'Подтверждён' },
                      { value: 'DELAYED', label: 'Задержан' },
                      { value: 'CANCELLED', label: 'Отменён' },
                      { value: 'COMPLETED', label: 'Завершён' },
                      { value: 'REFUND_PENDING', label: 'Возврат средств' },
                      { value: 'REFUNDED', label: 'Возвращён' },
                    ]}
                    style={{ flex: 1 }}
                  />
                  <Input
                    type="number"
                    step="any"
                    placeholder="Цена"
                    value={orderData.price_amount}
                    onChange={e => setOrderData({ ...orderData, price_amount: e.target.value })}
                    style={{ flex: 1 }}
                  />
                  <Input
                    placeholder="Валюта"
                    value={orderData.price_currency}
                    onChange={e => setOrderData({ ...orderData, price_currency: e.target.value })}
                    style={{ flex: 1 }}
                  />
                  <Select
                    value={orderData.provider_id}
                    onChange={e => setOrderData({ ...orderData, provider_id: e.target.value })}
                    options={[{ value: '', label: 'Провайдер (опционально)' }, ...providers.map(p => ({ value: p.id, label: p.name }))]}
                    style={{ flex: 1 }}
                  />
                  <Input
                    placeholder="Внешний ID (опционально)"
                    value={orderData.external_order_id}
                    onChange={e => setOrderData({ ...orderData, external_order_id: e.target.value })}
                    style={{ flex: 1 }}
                  />
                  <Input
                    type="datetime-local"
                    placeholder="Начало (опционально)"
                    value={orderData.start_datetime}
                    onChange={e => setOrderData({ ...orderData, start_datetime: e.target.value })}
                    style={{ flex: 1 }}
                  />
                  <Input
                    type="datetime-local"
                    placeholder="Конец (опционально)"
                    value={orderData.end_datetime}
                    onChange={e => setOrderData({ ...orderData, end_datetime: e.target.value })}
                    style={{ flex: 1 }}
                  />
                  <Select
                    value={orderData.departure_location_id}
                    onChange={e => setOrderData({ ...orderData, departure_location_id: e.target.value })}
                    options={[{ value: '', label: 'Отправление (опционально)' }, ...locations.map(l => ({ value: l.id, label: l.name }))]}
                    style={{ flex: 1 }}
                  />
                  <Select
                    value={orderData.arrival_location_id}
                    onChange={e => setOrderData({ ...orderData, arrival_location_id: e.target.value })}
                    options={[{ value: '', label: 'Прибытие (опционально)' }, ...locations.map(l => ({ value: l.id, label: l.name }))]}
                    style={{ flex: 1 }}
                  />
                </div>
                <div className="flex-row" style={{ marginTop: 12 }}>
                  <Button type="submit"><i className="fas fa-save"></i> Сохранить</Button>
                  <Button variant="secondary" onClick={() => setOrderFormVisible(false)}><i className="fas fa-times"></i> Отмена</Button>
                </div>
              </form>
            </div>
          )}

          {orders.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-box-open"></i>
              <p>Нет заказов. Добавьте первый заказ, чтобы начать планирование.</p>
            </div>
          ) : (
            orders.map(o => (
              <div key={o.id} className="list-item">
                <div className="info">
                  <div className="title">
                    <i className={`fas ${serviceIcons[o.service_type] || 'fa-tag'}`} style={{ marginRight: 8 }}></i>
                    {o.title} 
                    <span style={{ fontSize: 14, color: 'var(--color-text-secondary)', marginLeft: 8 }}>({o.service_type})</span>
                  </div>
                  <div className="sub">
                    <span className="status-badge" style={{ background: statusColors[o.status] || '#e8e0d8' }}>
                      {statusLabels[o.status] || o.status}
                    </span>
                    {o.price_amount && ` — ${o.price_amount} ${o.price_currency}`}
                  </div>
                  <div className="sub" style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                    ID: {o.id}
                  </div>
                </div>
                <div className="actions">
                  <Button variant="secondary" className="btn-sm" onClick={() => { setSelectedOrderId(o.id); setFileFormVisible(true); loadFiles(o.id); }}>
                    <i className="fas fa-file"></i>
                  </Button>
                  <Button variant="secondary" className="btn-sm" onClick={() => handleEditOrder(o.id)}>
                    <i className="fas fa-edit"></i>
                  </Button>
                  <Button variant="secondary" className="btn-sm" onClick={() => handleChangeStatus(o.id)}>
                    <i className="fas fa-sync"></i>
                  </Button>
                  <Button variant="danger" className="btn-sm" onClick={() => handleDeleteOrder(o.id)}>
                    <i className="fas fa-trash"></i>
                  </Button>
                </div>
              </div>
            ))
          )}
        </Card>
      )}

      {fileFormVisible && selectedOrderId && (
        <Card title="Файлы заказа" icon="fa-paperclip">
          <form onSubmit={handleAddFile} className="flex-row">
            <Input
              placeholder="URL файла *"
              value={fileData.file_url}
              onChange={e => setFileData({ ...fileData, file_url: e.target.value })}
              required
              style={{ flex: 2 }}
            />
            <Select
              value={fileData.file_type}
              onChange={e => setFileData({ ...fileData, file_type: e.target.value })}
              options={[
                { value: 'PDF', label: 'PDF' },
                { value: 'IMAGE', label: 'Изображение' },
                { value: 'EMAIL', label: 'Email' },
                { value: 'JSON', label: 'JSON' },
                { value: 'OTHER', label: 'Другое' },
              ]}
              style={{ flex: 1 }}
            />
            <Button type="submit"><i className="fas fa-upload"></i> Добавить</Button>
            <Button variant="secondary" onClick={() => setFileFormVisible(false)}><i className="fas fa-times"></i> Закрыть</Button>
          </form>
          {files.length === 0 ? (
            <p>Нет файлов</p>
          ) : (
            files.map(f => (
              <div key={f.id} className="list-item">
                <div className="info">
                  <div className="title">{f.file_url}</div>
                  <div className="sub">{f.file_type} — {new Date(f.uploaded_at).toLocaleString()}</div>
                </div>
                <div className="actions">
                  <Button variant="secondary" className="btn-sm" onClick={() => handleViewFile(f.file_url)}>
                    <i className="fas fa-eye"></i>
                  </Button>
                  <Button variant="danger" className="btn-sm" onClick={() => handleDeleteFile(f.id)}>
                    <i className="fas fa-trash"></i>
                  </Button>
                </div>
              </div>
            ))
          )}
        </Card>
      )}
    </div>
  );
};

export default TripDetail;