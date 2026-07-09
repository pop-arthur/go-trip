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

const TripDetail = () => {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [trip, setTrip] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
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
      await loadSelectData();
      setLoading(false);
    };
    init();
    // eslint-disable-next-line
  }, [tripId]);

  const handleCreateOrder = async (e) => {
    e.preventDefault();
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
    const newTitle = prompt('Название:', order.title);
    if (newTitle === null) return;
    const newStatus = prompt('Статус (PENDING_VERIFICATION/CONFIRMED/DELAYED/CANCELLED/COMPLETED/REFUND_PENDING/REFUNDED):', order.status);
    if (!newStatus) return;
    const newPrice = prompt('Цена:', order.price_amount || '');
    const newCurrency = prompt('Валюта:', order.price_currency || '');
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
    const newStatus = prompt('Новый статус (PENDING_VERIFICATION/CONFIRMED/DELAYED/CANCELLED/COMPLETED/REFUND_PENDING/REFUNDED):');
    if (!newStatus) return;
    const reason = prompt('Причина (опционально):');
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
    // eslint-disable-next-line no-restricted-globals
    if (!confirm('Удалить заказ?')) return;
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

  const handleAddFile = async (e) => {
    e.preventDefault();
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
    // eslint-disable-next-line no-restricted-globals
    if (!confirm('Удалить файл?')) return;
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

  if (loading) return <div>Загрузка...</div>;
  if (!trip) return <div>Поездка не найдена</div>;

  const statusLabel = {
    PLANNED: 'Запланирована',
    ACTIVE: 'Активна',
    COMPLETED: 'Завершена',
    CANCELLED: 'Отменена',
  };

  return (
    <div>
      <button onClick={() => navigate('/trips')} style={{ background: 'none', border: 'none', color: 'var(--color-primary-dark)', cursor: 'pointer', marginBottom: 16, fontSize: 16 }}>
        <i className="fas fa-arrow-left"></i> Назад к списку
      </button>
      <h2><i className="fas fa-route" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>{trip.title}</h2>

      <Card>
        <div className="flex-row" style={{ flexWrap: 'wrap', gap: 8 }}>
          <span className="status-badge" style={{ background: statusColors[trip.status] || 'var(--color-primary)', color: '#2d2a2a' }}>
            {statusLabel[trip.status] || trip.status}
          </span>
          <span><i className="fas fa-calendar-alt"></i> {trip.start_date || '—'} — {trip.end_date || '—'}</span>
          <span><i className="fas fa-id-card"></i> {trip.id}</span>
        </div>
      </Card>

      <Card title="Заказы" icon="fa-ticket">
        {/* Улучшенная кнопка добавления заказа */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <span style={{ color: 'var(--color-text-secondary)' }}>
            <i className="fas fa-info-circle"></i> Добавьте заказы к поездке
          </span>
          <Button 
            onClick={() => setOrderFormVisible(true)} 
            style={{ 
              padding: '12px 24px', 
              fontSize: '16px', 
              borderRadius: 'var(--border-radius)',
              boxShadow: '0 4px 12px rgba(184, 212, 227, 0.4)'
            }}
          >
            <i className="fas fa-plus-circle"></i> Создать заказ
          </Button>
        </div>

        {orderFormVisible && (
          <div style={{ marginBottom: 20, padding: 20, background: 'var(--color-bg)', borderRadius: 'var(--border-radius-sm)' }}>
            <h4 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <i className="fas fa-pen"></i> Новый заказ
            </h4>
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
                  placeholder="Название"
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
                  options={[{ value: '', label: 'Выберите провайдера' }, ...providers.map(p => ({ value: p.id, label: p.name }))]}
                  style={{ flex: 1 }}
                />
                <Input
                  placeholder="Внешний ID"
                  value={orderData.external_order_id}
                  onChange={e => setOrderData({ ...orderData, external_order_id: e.target.value })}
                  style={{ flex: 1 }}
                />
                <Input
                  type="datetime-local"
                  placeholder="Начало"
                  value={orderData.start_datetime}
                  onChange={e => setOrderData({ ...orderData, start_datetime: e.target.value })}
                  style={{ flex: 1 }}
                />
                <Input
                  type="datetime-local"
                  placeholder="Конец"
                  value={orderData.end_datetime}
                  onChange={e => setOrderData({ ...orderData, end_datetime: e.target.value })}
                  style={{ flex: 1 }}
                />
                <Select
                  value={orderData.departure_location_id}
                  onChange={e => setOrderData({ ...orderData, departure_location_id: e.target.value })}
                  options={[{ value: '', label: 'Локация отправления' }, ...locations.map(l => ({ value: l.id, label: l.name }))]}
                  style={{ flex: 1 }}
                />
                <Select
                  value={orderData.arrival_location_id}
                  onChange={e => setOrderData({ ...orderData, arrival_location_id: e.target.value })}
                  options={[{ value: '', label: 'Локация прибытия' }, ...locations.map(l => ({ value: l.id, label: l.name }))]}
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
            <p>Нет заказов</p>
          </div>
        ) : (
          orders.map(o => (
            <div key={o.id} className="list-item">
              <div className="info">
                <div className="title">
                  <i className={`fas ${serviceIcons[o.service_type] || 'fa-tag'}`} style={{ marginRight: 8 }}></i>
                  {o.title} ({o.service_type})
                </div>
                <div className="sub">
                  <span className="status-badge" style={{ background: statusColors[o.status] || '#e8e0d8' }}>
                    {o.status}
                  </span>
                  {o.price_amount && `${o.price_amount} ${o.price_currency}`}
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

      {fileFormVisible && selectedOrderId && (
        <Card title="Файлы заказа" icon="fa-paperclip">
          <form onSubmit={handleAddFile} className="flex-row">
            <Input
              placeholder="URL файла"
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
                <Button variant="danger" className="btn-sm" onClick={() => handleDeleteFile(f.id)}>
                  <i className="fas fa-trash"></i>
                </Button>
              </div>
            ))
          )}
        </Card>
      )}
    </div>
  );
};

export default TripDetail;