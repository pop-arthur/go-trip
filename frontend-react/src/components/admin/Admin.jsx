import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

const Admin = () => {
  const { showToast } = useToast();
  const [activeTab, setActiveTab] = useState('achievements');

  // --- Достижения ---
  const [achievements, setAchievements] = useState([]);
  const [achLoading, setAchLoading] = useState(true);
  const [achCreateData, setAchCreateData] = useState({
    code: '',
    title: '',
    description: '',
    conditionType: 'TRIPS_COUNT',
    conditionValue: '',
    iconUrl: '',
  });
  const [achSubmitting, setAchSubmitting] = useState(false);

  // --- Провайдеры ---
  const [providers, setProviders] = useState([]);
  const [provLoading, setProvLoading] = useState(true);
  const [provCreateData, setProvCreateData] = useState({
    name: '',
    type: 'OTHER',
    website: '',
    support_contact: '',
  });
  const [provSubmitting, setProvSubmitting] = useState(false);

  // --- Доп. услуги ---
  const [services, setServices] = useState([]);
  const [servLoading, setServLoading] = useState(true);
  const [servCreateData, setServCreateData] = useState({
    title: '',
    description: '',
    service_type: 'OTHER',
    provider_id: '',
    location_id: '',
    price_amount: '',
    price_currency: '',
    is_active: true,
  });
  const [servSubmitting, setServSubmitting] = useState(false);

  // --- Заказы (симуляция) ---
  const [selectedOrderId, setSelectedOrderId] = useState('');
  const [statusUpdate, setStatusUpdate] = useState({ status: '', reason: '' });
  const [statusSubmitting, setStatusSubmitting] = useState(false);

  // Состояния для модалок подтверждения
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);
  const [confirmId, setConfirmId] = useState(null);

  // Загрузка данных при смене вкладки
  useEffect(() => {
    if (activeTab === 'achievements') loadAchievements();
    if (activeTab === 'providers') loadProviders();
    if (activeTab === 'services') loadServices();
    // eslint-disable-next-line
  }, [activeTab]);

  // --- Достижения ---
  const loadAchievements = async () => {
    setAchLoading(true);
    try {
      const resp = await apiFetch('/achievements');
      if (resp.ok) {
        const data = await resp.json();
        setAchievements(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки достижений', 'error');
    } finally {
      setAchLoading(false);
    }
  };

  const handleCreateAchievement = async (e) => {
    e.preventDefault();
    setAchSubmitting(true);
    try {
      const resp = await apiFetch('/admin/achievements', {
        method: 'POST',
        body: JSON.stringify({
          ...achCreateData,
          conditionValue: parseInt(achCreateData.conditionValue),
        }),
      });
      if (resp.ok) {
        showToast('Достижение создано');
        setAchCreateData({ code: '', title: '', description: '', conditionType: 'TRIPS_COUNT', conditionValue: '', iconUrl: '' });
        loadAchievements();
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка создания', 'error');
      }
    } catch (e) {
      showToast('Ошибка создания', 'error');
    } finally {
      setAchSubmitting(false);
    }
  };

  const handleEditAchievement = async (id) => {
    const ach = achievements.find(a => a.id === id);
    if (!ach) return;
    const code = window.prompt('Код:', ach.code);
    if (code === null) return;
    const title = window.prompt('Название:', ach.title);
    if (title === null) return;
    const desc = window.prompt('Описание:', ach.description || '');
    const condType = window.prompt('Тип условия:', ach.conditionType);
    const condValue = window.prompt('Значение:', ach.conditionValue);
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

  const handleDeleteAchievement = async (id) => {
    setConfirmId(id);
    setConfirmAction(() => async () => {
      try {
        const resp = await apiFetch(`/admin/achievements/${id}`, { method: 'DELETE' });
        if (resp.ok) {
          showToast('Достижение удалено');
          loadAchievements();
        }
      } catch (e) {
        showToast('Ошибка удаления', 'error');
      }
      setShowConfirmModal(false);
    });
    setShowConfirmModal(true);
  };

  // --- Провайдеры ---
  const loadProviders = async () => {
    setProvLoading(true);
    try {
      const resp = await apiFetch('/providers');
      if (resp.ok) {
        const data = await resp.json();
        setProviders(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки провайдеров', 'error');
    } finally {
      setProvLoading(false);
    }
  };

  const handleCreateProvider = async (e) => {
    e.preventDefault();
    setProvSubmitting(true);
    try {
      const resp = await apiFetch('/admin/providers', {
        method: 'POST',
        body: JSON.stringify(provCreateData),
      });
      if (resp.ok) {
        showToast('Провайдер создан');
        setProvCreateData({ name: '', type: 'OTHER', website: '', support_contact: '' });
        loadProviders();
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка создания', 'error');
      }
    } catch (e) {
      showToast('Ошибка создания', 'error');
    } finally {
      setProvSubmitting(false);
    }
  };

  const handleDeleteProvider = async (id) => {
    setConfirmId(id);
    setConfirmAction(() => async () => {
      try {
        const resp = await apiFetch(`/admin/providers/${id}`, { method: 'DELETE' });
        if (resp.ok) {
          showToast('Провайдер удалён');
          loadProviders();
        }
      } catch (e) {
        showToast('Ошибка удаления', 'error');
      }
      setShowConfirmModal(false);
    });
    setShowConfirmModal(true);
  };

  // --- Доп. услуги ---
  const loadServices = async () => {
    setServLoading(true);
    try {
      const resp = await apiFetch('/additional-services');
      if (resp.ok) {
        const data = await resp.json();
        setServices(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки услуг', 'error');
    } finally {
      setServLoading(false);
    }
  };

  const handleCreateService = async (e) => {
    e.preventDefault();
    setServSubmitting(true);
    try {
      const resp = await apiFetch('/admin/additional-services', {
        method: 'POST',
        body: JSON.stringify({
          ...servCreateData,
          price_amount: servCreateData.price_amount ? parseFloat(servCreateData.price_amount) : null,
        }),
      });
      if (resp.ok) {
        showToast('Услуга создана');
        setServCreateData({
          title: '',
          description: '',
          service_type: 'OTHER',
          provider_id: '',
          location_id: '',
          price_amount: '',
          price_currency: '',
          is_active: true,
        });
        loadServices();
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка создания', 'error');
      }
    } catch (e) {
      showToast('Ошибка создания', 'error');
    } finally {
      setServSubmitting(false);
    }
  };

  const handleEditService = async (id) => {
    const serv = services.find(s => s.id === id);
    if (!serv) return;
    const title = window.prompt('Название:', serv.title);
    if (title === null) return;
    const description = window.prompt('Описание:', serv.description || '');
    const serviceType = window.prompt('Тип:', serv.service_type);
    const price = window.prompt('Цена:', serv.price_amount || '');
    const currency = window.prompt('Валюта:', serv.price_currency || '');
    const isActive = window.confirm('Активна?');
    try {
      const resp = await apiFetch(`/admin/additional-services/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({
          title,
          description: description || null,
          service_type: serviceType,
          price_amount: price ? parseFloat(price) : null,
          price_currency: currency || null,
          is_active: isActive,
        }),
      });
      if (resp.ok) {
        showToast('Услуга обновлена');
        loadServices();
      }
    } catch (e) {
      showToast('Ошибка обновления', 'error');
    }
  };

  const handleDeleteService = async (id) => {
    setConfirmId(id);
    setConfirmAction(() => async () => {
      try {
        const resp = await apiFetch(`/admin/additional-services/${id}`, { method: 'DELETE' });
        if (resp.ok) {
          showToast('Услуга удалена');
          loadServices();
        }
      } catch (e) {
        showToast('Ошибка удаления', 'error');
      }
      setShowConfirmModal(false);
    });
    setShowConfirmModal(true);
  };

  // --- Симуляция статуса заказа ---
  const handleSimulateStatus = async (e) => {
    e.preventDefault();
    if (!selectedOrderId || !statusUpdate.status) {
      showToast('Выберите заказ и статус', 'error');
      return;
    }
    setStatusSubmitting(true);
    try {
      const resp = await apiFetch(`/admin/orders/${selectedOrderId}/simulate-status-change`, {
        method: 'POST',
        body: JSON.stringify(statusUpdate),
      });
      if (resp.ok) {
        showToast('Статус обновлён (симуляция)');
        setStatusUpdate({ status: '', reason: '' });
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка симуляции', 'error');
      }
    } catch (e) {
      showToast('Ошибка симуляции', 'error');
    } finally {
      setStatusSubmitting(false);
    }
  };

  const renderTab = () => {
    switch (activeTab) {
      case 'achievements': return renderAchievementsTab();
      case 'providers': return renderProvidersTab();
      case 'services': return renderServicesTab();
      case 'orders': return renderOrdersTab();
      default: return <div>Неизвестная вкладка</div>;
    }
  };

  const renderAchievementsTab = () => (
    <Card title="Управление достижениями" icon="fa-trophy">
      <form onSubmit={handleCreateAchievement}>
        <div className="flex-row">
          <Input placeholder="Код" value={achCreateData.code} onChange={e => setAchCreateData({ ...achCreateData, code: e.target.value })} required style={{ flex: 1 }} />
          <Input placeholder="Название" value={achCreateData.title} onChange={e => setAchCreateData({ ...achCreateData, title: e.target.value })} required style={{ flex: 1 }} />
          <Input placeholder="Описание" value={achCreateData.description} onChange={e => setAchCreateData({ ...achCreateData, description: e.target.value })} style={{ flex: 1 }} />
          <Select value={achCreateData.conditionType} onChange={e => setAchCreateData({ ...achCreateData, conditionType: e.target.value })} options={[
            { value: 'TRIPS_COUNT', label: 'Количество поездок' },
            { value: 'COUNTRIES_COUNT', label: 'Количество стран' },
            { value: 'ORDERS_COUNT', label: 'Количество заказов' },
            { value: 'REVIEWS_COUNT', label: 'Количество отзывов' },
            { value: 'SPENDING_AMOUNT', label: 'Сумма трат' },
          ]} style={{ flex: 1 }} />
          <Input type="number" placeholder="Значение" value={achCreateData.conditionValue} onChange={e => setAchCreateData({ ...achCreateData, conditionValue: e.target.value })} required style={{ flex: 1 }} />
          <Input placeholder="URL иконки" value={achCreateData.iconUrl} onChange={e => setAchCreateData({ ...achCreateData, iconUrl: e.target.value })} style={{ flex: 1 }} />
        </div>
        <Button type="submit" disabled={achSubmitting}>Создать</Button>
      </form>
      <hr style={{ margin: '20px 0', borderColor: 'var(--color-border)' }} />
      {achLoading ? <p>Загрузка...</p> : achievements.length === 0 ? <p>Нет достижений</p> : achievements.map(a => (
        <div key={a.id} className="list-item">
          <div className="info"><div className="title">{a.title} ({a.code})</div><div className="sub">{a.description || ''}</div></div>
          <div className="actions">
            <Button variant="secondary" className="btn-sm" onClick={() => handleEditAchievement(a.id)}>Редактировать</Button>
            <Button variant="danger" className="btn-sm" onClick={() => handleDeleteAchievement(a.id)}>Удалить</Button>
          </div>
        </div>
      ))}
    </Card>
  );

  const renderProvidersTab = () => (
    <Card title="Управление провайдерами" icon="fa-building">
      <form onSubmit={handleCreateProvider}>
        <div className="flex-row">
          <Input placeholder="Название" value={provCreateData.name} onChange={e => setProvCreateData({ ...provCreateData, name: e.target.value })} required style={{ flex: 1 }} />
          <Select value={provCreateData.type} onChange={e => setProvCreateData({ ...provCreateData, type: e.target.value })} options={[
            { value: 'AIRLINE', label: 'Авиакомпания' },
            { value: 'HOTEL', label: 'Отель' },
            { value: 'TOUR_COMPANY', label: 'Туроператор' },
            { value: 'TRANSPORT_COMPANY', label: 'Транспортная компания' },
            { value: 'BOOKING_PLATFORM', label: 'Платформа бронирования' },
            { value: 'INSURANCE_COMPANY', label: 'Страховая компания' },
            { value: 'OTHER', label: 'Другое' },
          ]} style={{ flex: 1 }} />
          <Input placeholder="Сайт" value={provCreateData.website} onChange={e => setProvCreateData({ ...provCreateData, website: e.target.value })} style={{ flex: 1 }} />
          <Input placeholder="Контакты" value={provCreateData.support_contact} onChange={e => setProvCreateData({ ...provCreateData, support_contact: e.target.value })} style={{ flex: 1 }} />
          <Button type="submit" disabled={provSubmitting}>Создать</Button>
        </div>
      </form>
      <hr style={{ margin: '20px 0', borderColor: 'var(--color-border)' }} />
      {provLoading ? <p>Загрузка...</p> : providers.length === 0 ? <p>Нет провайдеров</p> : providers.map(p => (
        <div key={p.id} className="list-item">
          <div className="info"><div className="title">{p.name} ({p.type})</div><div className="sub">{p.website || ''} {p.support_contact || ''}</div></div>
          <div className="actions">
            <Button variant="secondary" className="btn-sm" onClick={() => {
              const name = window.prompt('Новое название:', p.name);
              if (name === null) return;
              const type = window.prompt('Новый тип:', p.type);
              if (type === null) return;
              const website = window.prompt('Сайт:', p.website || '');
              const contact = window.prompt('Контакты:', p.support_contact || '');
              apiFetch(`/admin/providers/${p.id}`, { method: 'PATCH', body: JSON.stringify({ name, type, website: website || null, support_contact: contact || null }) })
                .then(resp => { if (resp.ok) { showToast('Провайдер обновлён'); loadProviders(); } else showToast('Ошибка', 'error'); });
            }}>Редактировать</Button>
            <Button variant="danger" className="btn-sm" onClick={() => handleDeleteProvider(p.id)}>Удалить</Button>
          </div>
        </div>
      ))}
    </Card>
  );

  const renderServicesTab = () => (
    <Card title="Управление доп. услугами" icon="fa-concierge-bell">
      <form onSubmit={handleCreateService}>
        <div className="flex-row">
          <Input placeholder="Название" value={servCreateData.title} onChange={e => setServCreateData({ ...servCreateData, title: e.target.value })} required style={{ flex: 1 }} />
          <Input placeholder="Описание" value={servCreateData.description} onChange={e => setServCreateData({ ...servCreateData, description: e.target.value })} style={{ flex: 1 }} />
          <Select value={servCreateData.service_type} onChange={e => setServCreateData({ ...servCreateData, service_type: e.target.value })} options={[
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
          ]} style={{ flex: 1 }} />
          <Input type="number" step="any" placeholder="Цена" value={servCreateData.price_amount} onChange={e => setServCreateData({ ...servCreateData, price_amount: e.target.value })} style={{ flex: 1 }} />
          <Input placeholder="Валюта" value={servCreateData.price_currency} onChange={e => setServCreateData({ ...servCreateData, price_currency: e.target.value })} style={{ flex: 1 }} />
          <Input placeholder="ID провайдера" value={servCreateData.provider_id} onChange={e => setServCreateData({ ...servCreateData, provider_id: e.target.value })} style={{ flex: 1 }} />
          <Input placeholder="ID локации" value={servCreateData.location_id} onChange={e => setServCreateData({ ...servCreateData, location_id: e.target.value })} style={{ flex: 1 }} />
          <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <input type="checkbox" checked={servCreateData.is_active} onChange={e => setServCreateData({ ...servCreateData, is_active: e.target.checked })} /> Активна
          </label>
          <Button type="submit" disabled={servSubmitting}>Создать</Button>
        </div>
      </form>
      <hr style={{ margin: '20px 0', borderColor: 'var(--color-border)' }} />
      {servLoading ? <p>Загрузка...</p> : services.length === 0 ? <p>Нет услуг</p> : services.map(s => (
        <div key={s.id} className="list-item">
          <div className="info"><div className="title">{s.title} ({s.service_type})</div><div className="sub">{s.description || ''} {s.price_amount && `${s.price_amount} ${s.price_currency}`}</div></div>
          <div className="actions">
            <Button variant="secondary" className="btn-sm" onClick={() => handleEditService(s.id)}>Редактировать</Button>
            <Button variant="danger" className="btn-sm" onClick={() => handleDeleteService(s.id)}>Удалить</Button>
          </div>
        </div>
      ))}
    </Card>
  );

  const renderOrdersTab = () => (
    <Card title="Симуляция статуса заказа" icon="fa-sync">
      <form onSubmit={handleSimulateStatus}>
        <div className="flex-row">
          <Input placeholder="ID заказа" value={selectedOrderId} onChange={e => setSelectedOrderId(e.target.value)} required style={{ flex: 1 }} />
          <Select value={statusUpdate.status} onChange={e => setStatusUpdate({ ...statusUpdate, status: e.target.value })} options={[
            { value: '', label: 'Выберите статус' },
            { value: 'PENDING_VERIFICATION', label: 'Ожидает проверки' },
            { value: 'CONFIRMED', label: 'Подтверждён' },
            { value: 'DELAYED', label: 'Задержан' },
            { value: 'CANCELLED', label: 'Отменён' },
            { value: 'COMPLETED', label: 'Завершён' },
            { value: 'REFUND_PENDING', label: 'Возврат средств' },
            { value: 'REFUNDED', label: 'Возвращён' },
          ]} style={{ flex: 1 }} />
          <Input placeholder="Причина (опционально)" value={statusUpdate.reason} onChange={e => setStatusUpdate({ ...statusUpdate, reason: e.target.value })} style={{ flex: 1 }} />
          <Button type="submit" disabled={statusSubmitting}>Симулировать</Button>
        </div>
      </form>
      <p style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>Введите ID заказа и выберите новый статус. Система создаст событие с источником "admin_simulation".</p>
    </Card>
  );

  // Модалка подтверждения удаления
  const ConfirmModal = () => {
    if (!showConfirmModal) return null;
    return (
      <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 9999 }} onClick={() => setShowConfirmModal(false)}>
        <div style={{ background: 'white', borderRadius: 'var(--border-radius)', padding: '32px', maxWidth: '400px', width: '100%' }} onClick={e => e.stopPropagation()}>
          <h3>Подтверждение удаления</h3>
          <p>Вы уверены, что хотите удалить этот элемент? Действие нельзя отменить.</p>
          <div className="flex-row" style={{ marginTop: 16 }}>
            <Button variant="danger" onClick={confirmAction}>Удалить</Button>
            <Button variant="secondary" onClick={() => setShowConfirmModal(false)}>Отмена</Button>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div>
      <h2><i className="fas fa-shield-halved" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>Административная панель</h2>
      <div className="flex-row" style={{ marginBottom: 16 }}>
        {['achievements', 'providers', 'services', 'orders'].map(tab => (
          <button key={tab} onClick={() => setActiveTab(tab)} style={{
            padding: '8px 16px',
            background: activeTab === tab ? 'var(--color-primary)' : 'transparent',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--border-radius-sm)',
            cursor: 'pointer',
            fontWeight: activeTab === tab ? 700 : 400,
          }}>
            {tab === 'achievements' ? 'Достижения' :
             tab === 'providers' ? 'Провайдеры' :
             tab === 'services' ? 'Доп. услуги' : 'Симуляция заказов'}
          </button>
        ))}
      </div>
      {renderTab()}
      <ConfirmModal />
    </div>
  );
};

export default Admin;