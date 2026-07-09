import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

const typeLabels = {
  PROVIDER: 'провайдеров',
  LOCATION: 'локаций',
  ORDER: 'заказов',
  ADDITIONAL_SERVICE: 'доп. услуг',
};

const Reviews = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(false);

  const [viewMode, setViewMode] = useState('all');

  const [filterType, setFilterType] = useState('');
  const [filterTargetId, setFilterTargetId] = useState('');
  const [filterTargetOptions, setFilterTargetOptions] = useState([]);

  const [createTargetType, setCreateTargetType] = useState('PROVIDER');
  const [createTargetId, setCreateTargetId] = useState('');
  const [createRating, setCreateRating] = useState('');
  const [createText, setCreateText] = useState('');

  const [targetOptions, setTargetOptions] = useState([]);
  const [loadingTargets, setLoadingTargets] = useState(false);

  const [trips, setTrips] = useState([]);
  const [orders, setOrders] = useState([]);
  const [selectedTripId, setSelectedTripId] = useState('');

  const [providersMap, setProvidersMap] = useState({});
  const [locationsMap, setLocationsMap] = useState({});
  const [servicesMap, setServicesMap] = useState({});

  const [showTargetModal, setShowTargetModal] = useState(false);
  const [selectedTarget, setSelectedTarget] = useState(null);
  const [targetRating, setTargetRating] = useState(null);

  // Загрузка справочников
  useEffect(() => {
    const loadMaps = async () => {
      try {
        const [provResp, locResp, servResp] = await Promise.all([
          apiFetch('/providers'),
          apiFetch('/locations'),
          apiFetch('/additional-services'),
        ]);
        if (provResp.ok) {
          const data = await provResp.json();
          const map = {};
          data.forEach(item => map[item.id] = item.name);
          setProvidersMap(map);
        }
        if (locResp.ok) {
          const data = await locResp.json();
          const map = {};
          data.forEach(item => map[item.id] = item.name);
          setLocationsMap(map);
        }
        if (servResp.ok) {
          const data = await servResp.json();
          const map = {};
          data.forEach(item => map[item.id] = item.title);
          setServicesMap(map);
        }
      } catch (e) {
        showToast('Ошибка загрузки справочников', 'error');
      }
    };
    loadMaps();
    // eslint-disable-next-line
  }, []);

  // Загрузка отзывов
  const loadReviews = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterType) {
        params.append('targetType', filterType);
        if (filterTargetId) {
          params.append('targetId', filterTargetId);
        }
      }
      const resp = await apiFetch(`/reviews?${params.toString()}`);
      if (resp.ok) {
        let data = await resp.json();
        if (viewMode === 'mine') {
          data = data.filter(r => r.userId === user?.id);
        }
        setReviews(data);
      } else {
        showToast('Ошибка загрузки отзывов', 'error');
      }
    } catch (e) {
      showToast('Ошибка загрузки отзывов', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Загрузка поездок для выбора заказов
  useEffect(() => {
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
    loadTrips();
    // eslint-disable-next-line
  }, []);

  // Загрузка заказов при выборе поездки
  useEffect(() => {
    if (selectedTripId) {
      const loadOrders = async () => {
        try {
          const resp = await apiFetch(`/trips/${selectedTripId}/orders`);
          if (resp.ok) {
            const data = await resp.json();
            setOrders(data);
          }
        } catch (e) {
          showToast('Ошибка загрузки заказов', 'error');
        }
      };
      loadOrders();
    } else {
      setOrders([]);
    }
    // eslint-disable-next-line
  }, [selectedTripId]);

  // Загрузка целей для формы создания
  useEffect(() => {
    if (createTargetType === 'ORDER') {
      setTargetOptions([]);
      return;
    }
    const loadTargets = async () => {
      setLoadingTargets(true);
      try {
        let url = '';
        let labelField = 'name';
        if (createTargetType === 'PROVIDER') {
          url = '/providers';
        } else if (createTargetType === 'LOCATION') {
          url = '/locations';
        } else if (createTargetType === 'ADDITIONAL_SERVICE') {
          url = '/additional-services';
          labelField = 'title';
        } else {
          setTargetOptions([]);
          setLoadingTargets(false);
          return;
        }
        const resp = await apiFetch(url);
        if (resp.ok) {
          const data = await resp.json();
          setTargetOptions(
            data.map((item) => ({
              value: item.id,
              label: item[labelField] || item.id,
            }))
          );
        } else {
          showToast('Ошибка загрузки целей', 'error');
        }
      } catch (e) {
        showToast('Ошибка загрузки целей', 'error');
      } finally {
        setLoadingTargets(false);
      }
    };
    loadTargets();
    // eslint-disable-next-line
  }, [createTargetType]);

  // Загрузка целей для фильтра
  useEffect(() => {
    if (!filterType) {
      setFilterTargetOptions([]);
      setFilterTargetId('');
      return;
    }
    const loadFilterTargets = async () => {
      try {
        let url = '';
        let labelField = 'name';
        if (filterType === 'PROVIDER') {
          url = '/providers';
        } else if (filterType === 'LOCATION') {
          url = '/locations';
        } else if (filterType === 'ADDITIONAL_SERVICE') {
          url = '/additional-services';
          labelField = 'title';
        } else if (filterType === 'ORDER') {
          setFilterTargetOptions([]);
          setFilterTargetId('');
          return;
        }
        if (url) {
          const resp = await apiFetch(url);
          if (resp.ok) {
            const data = await resp.json();
            setFilterTargetOptions(
              data.map((item) => ({
                value: item.id,
                label: item[labelField] || item.id,
              }))
            );
            setFilterTargetId('');
          }
        }
      } catch (e) {
        showToast('Ошибка загрузки целей для фильтра', 'error');
      }
    };
    loadFilterTargets();
    // eslint-disable-next-line
  }, [filterType]);

  // Перезагрузка при изменении вкладки или фильтров
  useEffect(() => {
    loadReviews();
    // eslint-disable-next-line
  }, [viewMode, filterType, filterTargetId]);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!createTargetId || !createRating) {
      showToast('Выберите цель и оценку', 'error');
      return;
    }
    try {
      const resp = await apiFetch('/reviews', {
        method: 'POST',
        body: JSON.stringify({
          targetType: createTargetType,
          targetId: createTargetId,
          rating: parseInt(createRating, 10),
          text: createText || null,
        }),
      });
      if (resp.ok) {
        showToast('Отзыв создан');
        setCreateRating('');
        setCreateText('');
        setCreateTargetId('');
        if (createTargetType === 'ORDER') {
          setSelectedTripId('');
          setOrders([]);
        }
        setFilterType('');
        setFilterTargetId('');
        setFilterTargetOptions([]);
        loadReviews();
      } else {
        const err = await resp.json();
        showToast(err.message || 'Ошибка создания отзыва', 'error');
      }
    } catch (e) {
      showToast('Ошибка создания отзыва', 'error');
    }
  };

  const handleEdit = async (id) => {
    const review = reviews.find((r) => r.id === id);
    if (!review) return;
    const newRating = window.prompt('Новая оценка (1-5):', review.rating);
    if (newRating === null) return;
    const newText = window.prompt('Новый текст:', review.text || '');
    if (newText === null) return;
    try {
      const resp = await apiFetch(`/reviews/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({
          rating: parseInt(newRating, 10),
          text: newText,
        }),
      });
      if (resp.ok) {
        showToast('Отзыв обновлён');
        loadReviews();
      }
    } catch (e) {
      showToast('Ошибка обновления отзыва', 'error');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Удалить этот отзыв?')) return;
    try {
      const resp = await apiFetch(`/reviews/${id}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Отзыв удалён');
        loadReviews();
      }
    } catch (e) {
      showToast('Ошибка удаления отзыва', 'error');
    }
  };

  const getTargetName = (targetType, targetId) => {
    if (!targetType || !targetId) return 'неизвестно';
    const type = targetType.toUpperCase();
    switch (type) {
      case 'PROVIDER':
        return providersMap[targetId] || `Провайдер #${targetId.slice(0, 8)}`;
      case 'LOCATION':
        return locationsMap[targetId] || `Локация #${targetId.slice(0, 8)}`;
      case 'ADDITIONAL_SERVICE':
      case 'ADDITIONALSERVICE':
        return servicesMap[targetId] || `Услуга #${targetId.slice(0, 8)}`;
      case 'ORDER':
        return `Заказ #${targetId.slice(0, 8)}`;
      default:
        return targetId.slice(0, 8);
    }
  };

  const getFilterLabel = (filterType) => {
    if (!filterType) return 'Все';
    const label = typeLabels[filterType.toUpperCase()];
    return label ? `Все ${label}` : 'Все';
  };

  const getUserLabel = (userId) => {
    if (!userId) return 'неизвестно';
    if (userId === user?.id) return 'Вы';
    return `#${userId.slice(0, 8)}`;
  };

    const handleViewTarget = (targetType, targetId) => {
      if (!targetType || !targetId) return;
      const name = getTargetName(targetType, targetId);
      setSelectedTarget({ targetType, targetId, name });
      apiFetch(`/reviews/rating-summary?targetType=${targetType.toUpperCase()}&targetId=${targetId}`)
        .then(resp => {
          if (!resp.ok) {
            if (resp.status === 400) {
              showToast('Неверный формат типа цели', 'error');
            }
            return resp.json().catch(() => ({}));
          }
          return resp.json();
        })
        .then(data => {
          setTargetRating(data);
          setShowTargetModal(true);
        })
        .catch(() => {
          setTargetRating(null);
          setShowTargetModal(true);
        });
  };

  const closeTargetModal = () => {
    setShowTargetModal(false);
    setSelectedTarget(null);
    setTargetRating(null);
  };

  const clearFilters = () => {
    setFilterType('');
    setFilterTargetId('');
    setFilterTargetOptions([]);
  };

  return (
    <div>
      <h2><i className="fas fa-star" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>Отзывы</h2>

      <Card title="Создать отзыв" icon="fa-pen">
        <p style={{ fontSize: 14, color: 'var(--color-text-secondary)', marginBottom: 12 }}>
          Оставьте отзыв о провайдере, локации, заказе или дополнительной услуге.
        </p>
        <form onSubmit={handleCreate}>
          <div className="flex-row">
            <Select
              value={createTargetType}
              onChange={(e) => setCreateTargetType(e.target.value)}
              options={[
                { value: 'PROVIDER', label: 'Провайдер' },
                { value: 'LOCATION', label: 'Локация' },
                { value: 'ADDITIONAL_SERVICE', label: 'Доп. услуга' },
                { value: 'ORDER', label: 'Заказ' },
              ]}
              style={{ flex: 1 }}
            />

            {createTargetType === 'ORDER' ? (
              <>
                <Select
                  value={selectedTripId}
                  onChange={(e) => setSelectedTripId(e.target.value)}
                  options={[
                    { value: '', label: 'Выберите поездку' },
                    ...trips.map((t) => ({ value: t.id, label: t.title || t.id })),
                  ]}
                  style={{ flex: 1 }}
                />
                <Select
                  value={createTargetId}
                  onChange={(e) => setCreateTargetId(e.target.value)}
                  disabled={!selectedTripId}
                  options={[
                    { value: '', label: 'Выберите заказ' },
                    ...orders.map((o) => ({ value: o.id, label: o.title || o.id })),
                  ]}
                  style={{ flex: 1 }}
                />
              </>
            ) : (
              <Select
                value={createTargetId}
                onChange={(e) => setCreateTargetId(e.target.value)}
                disabled={loadingTargets}
                options={[{ value: '', label: 'Выберите цель' }, ...targetOptions]}
                style={{ flex: 1 }}
              />
            )}

            <Input
              type="number"
              min="1"
              max="5"
              placeholder="Оценка *"
              value={createRating}
              onChange={(e) => setCreateRating(e.target.value)}
              required
              style={{ flex: 1 }}
            />
            <Input
              placeholder="Текст (опционально)"
              value={createText}
              onChange={(e) => setCreateText(e.target.value)}
              style={{ flex: 1 }}
            />
          </div>
          <Button type="submit"><i className="fas fa-plus"></i> Оставить отзыв</Button>
        </form>
      </Card>

      <Card title="Список отзывов" icon="fa-list">
        <div className="flex-row" style={{ marginBottom: 16 }}>
          <button
            onClick={() => setViewMode('all')}
            style={{
              padding: '8px 16px',
              background: viewMode === 'all' ? 'var(--color-primary)' : 'transparent',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--border-radius-sm)',
              cursor: 'pointer',
              fontWeight: viewMode === 'all' ? 700 : 400,
            }}
          >
            Все
          </button>
          <button
            onClick={() => setViewMode('mine')}
            style={{
              padding: '8px 16px',
              background: viewMode === 'mine' ? 'var(--color-primary)' : 'transparent',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--border-radius-sm)',
              cursor: 'pointer',
              fontWeight: viewMode === 'mine' ? 700 : 400,
            }}
          >
            Мои
          </button>
        </div>

        <div className="flex-row" style={{ marginBottom: 16 }}>
          <Select
            value={filterType}
            onChange={(e) => {
              setFilterType(e.target.value);
              setFilterTargetId('');
            }}
            options={[
              { value: '', label: 'Все типы' },
              { value: 'PROVIDER', label: 'Провайдеры' },
              { value: 'LOCATION', label: 'Локации' },
              { value: 'ORDER', label: 'Заказы' },
              { value: 'ADDITIONAL_SERVICE', label: 'Доп. услуги' },
            ]}
            style={{ flex: 1 }}
          />
          {filterType && filterTargetOptions.length > 0 && (
            <Select
              value={filterTargetId}
              onChange={(e) => setFilterTargetId(e.target.value)}
              options={[
                { value: '', label: getFilterLabel(filterType) },
                ...filterTargetOptions,
              ]}
              style={{ flex: 1 }}
            />
          )}
          {filterType === 'ORDER' && (
            <Input
              placeholder="ID заказа (опционально)"
              value={filterTargetId}
              onChange={(e) => setFilterTargetId(e.target.value)}
              style={{ flex: 1 }}
            />
          )}
          <Button variant="secondary" onClick={loadReviews}>
            <i className="fas fa-search"></i> Найти
          </Button>
          <Button variant="secondary" onClick={clearFilters}>
            <i className="fas fa-undo"></i> Сбросить
          </Button>
        </div>

        {loading ? (
          <div className="loading-state">Загрузка отзывов...</div>
        ) : reviews.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-star"></i>
            <p>Нет отзывов. Оставьте первый!</p>
          </div>
        ) : (
          reviews.map((r) => (
            <div key={r.id} className="list-item">
              <div className="info">
                <div className="title">
                  <i className="fas fa-star" style={{ color: '#f5b342', marginRight: 8 }}></i>
                  {r.rating}/5 — {getTargetName(r.targetType, r.targetId)}
                  <Button
                    variant="secondary"
                    className="btn-sm"
                    style={{ marginLeft: 8 }}
                    onClick={() => handleViewTarget(r.targetType, r.targetId)}
                    title="Посмотреть рейтинг и детали"
                  >
                    <i className="fas fa-info-circle"></i>
                  </Button>
                </div>
                <div className="sub">{r.text || '(без текста)'}</div>
                <div className="sub" style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                  От пользователя: {getUserLabel(r.userId)}
                </div>
              </div>
              <div className="actions">
                <Button
                  variant="secondary"
                  className="btn-sm"
                  onClick={() => handleEdit(r.id)}
                >
                  <i className="fas fa-edit"></i>
                </Button>
                <Button
                  variant="danger"
                  className="btn-sm"
                  onClick={() => handleDelete(r.id)}
                >
                  <i className="fas fa-trash"></i>
                </Button>
              </div>
            </div>
          ))
        )}
      </Card>

      {showTargetModal && selectedTarget && (
        <div
          style={{
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
          }}
          onClick={closeTargetModal}
        >
          <div
            style={{
              background: 'white',
              borderRadius: 'var(--border-radius)',
              padding: '32px',
              maxWidth: '500px',
              width: '100%',
              maxHeight: '80vh',
              overflow: 'auto',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3>{selectedTarget.name}</h3>
            <div style={{ marginTop: 16 }}>
              <p><strong>Тип:</strong> {selectedTarget.targetType}</p>
              <p><strong>ID:</strong> {selectedTarget.targetId}</p>
              {targetRating && (
                <div style={{ marginTop: 12, padding: 12, background: 'var(--color-bg)', borderRadius: 'var(--border-radius-sm)' }}>
                  <p><strong>⭐ Средняя оценка:</strong> {targetRating.averageRating ? targetRating.averageRating.toFixed(1) : 'нет оценок'}</p>
                  <p><strong>📝 Количество отзывов:</strong> {targetRating.reviewCount || 0}</p>
                </div>
              )}
            </div>
            <Button onClick={closeTargetModal} style={{ marginTop: 16 }}>Закрыть</Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Reviews;