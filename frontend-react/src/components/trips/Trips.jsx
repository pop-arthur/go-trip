import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

const statusIcons = {
  PLANNED: 'fa-clock',
  ACTIVE: 'fa-play-circle',
  COMPLETED: 'fa-check-circle',
  CANCELLED: 'fa-circle-xmark',
};

const statusColors = {
  PLANNED: '#e8e0d8',
  ACTIVE: '#b8d4e3',
  COMPLETED: '#c8e6c9',
  CANCELLED: '#f0d4d4',
};

const Trips = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [filterFrom, setFilterFrom] = useState('');
  const [filterTo, setFilterTo] = useState('');
  const [createData, setCreateData] = useState({
    title: '',
    start_date: '',
    end_date: '',
    status: 'PLANNED',
  });
  const [submitting, setSubmitting] = useState(false);

  const loadTrips = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterStatus) params.append('status', filterStatus);
      if (filterFrom) params.append('fromDate', filterFrom);
      if (filterTo) params.append('toDate', filterTo);
      const resp = await apiFetch(`/trips?${params.toString()}`);
      if (resp.ok) {
        const data = await resp.json();
        setTrips(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки поездок', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTrips();
    // eslint-disable-next-line
  }, [filterStatus, filterFrom, filterTo]);

  const handleCreate = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const resp = await apiFetch('/trips', {
        method: 'POST',
        body: JSON.stringify(createData),
      });
      if (resp.ok) {
        showToast('Поездка создана');
        setCreateData({ title: '', start_date: '', end_date: '', status: 'PLANNED' });
        loadTrips();
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

  const deleteTrip = async (id) => {
    // eslint-disable-next-line no-restricted-globals
    if (!confirm('Удалить поездку?')) return;
    try {
      const resp = await apiFetch(`/trips/${id}`, { method: 'DELETE' });
      if (resp.ok) {
        showToast('Поездка удалена');
        loadTrips();
      }
    } catch (e) {
      showToast('Ошибка удаления', 'error');
    }
  };

  const editTrip = async (id) => {
    const newTitle = prompt('Новое название:');
    if (newTitle === null) return;
    const newStart = prompt('Дата начала (YYYY-MM-DD):') || null;
    const newEnd = prompt('Дата окончания (YYYY-MM-DD):') || null;
    const newStatus = prompt('Статус (PLANNED/ACTIVE/COMPLETED/CANCELLED):');
    if (!newStatus) return;
    try {
      const resp = await apiFetch(`/trips/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({
          title: newTitle,
          start_date: newStart,
          end_date: newEnd,
          status: newStatus,
        }),
      });
      if (resp.ok) {
        showToast('Поездка обновлена');
        loadTrips();
      }
    } catch (e) {
      showToast('Ошибка обновления', 'error');
    }
  };

  return (
    <div>
      <h2><i className="fas fa-plane" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>Поездки</h2>

      <Card title="Создать поездку" icon="fa-plus-circle">
        <form onSubmit={handleCreate}>
          <div className="flex-row">
            <Input
              placeholder="Название"
              value={createData.title}
              onChange={e => setCreateData({ ...createData, title: e.target.value })}
              required
              style={{ flex: 2 }}
            />
            <Input
              type="date"
              placeholder="Дата начала"
              value={createData.start_date}
              onChange={e => setCreateData({ ...createData, start_date: e.target.value })}
              style={{ flex: 1 }}
            />
            <Input
              type="date"
              placeholder="Дата окончания"
              value={createData.end_date}
              onChange={e => setCreateData({ ...createData, end_date: e.target.value })}
              style={{ flex: 1 }}
            />
            <Select
              value={createData.status}
              onChange={e => setCreateData({ ...createData, status: e.target.value })}
              options={[
                { value: 'PLANNED', label: 'Запланирована' },
                { value: 'ACTIVE', label: 'Активна' },
                { value: 'COMPLETED', label: 'Завершена' },
                { value: 'CANCELLED', label: 'Отменена' },
              ]}
              style={{ flex: 1 }}
            />
            <Button type="submit" disabled={submitting}><i className="fas fa-save"></i> Создать</Button>
          </div>
        </form>
      </Card>

      <Card title="Мои поездки" icon="fa-list">
        <div className="flex-row" style={{ marginBottom: 16 }}>
          <Select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
            options={[
              { value: '', label: 'Все' },
              { value: 'PLANNED', label: 'Запланирована' },
              { value: 'ACTIVE', label: 'Активна' },
              { value: 'COMPLETED', label: 'Завершена' },
              { value: 'CANCELLED', label: 'Отменена' },
            ]}
            style={{ flex: 1 }}
          />
          <Input
            type="date"
            placeholder="С"
            value={filterFrom}
            onChange={e => setFilterFrom(e.target.value)}
            style={{ flex: 1 }}
          />
          <Input
            type="date"
            placeholder="По"
            value={filterTo}
            onChange={e => setFilterTo(e.target.value)}
            style={{ flex: 1 }}
          />
          <Button variant="secondary" onClick={loadTrips}><i className="fas fa-search"></i> Поиск</Button>
        </div>

        {loading ? (
          <p>Загрузка...</p>
        ) : trips.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-suitcase"></i>
            <p>Нет поездок. Создайте первую!</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            {trips.map(t => (
              <div
                key={t.id}
                style={{
                  background: 'var(--color-card)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--border-radius)',
                  padding: '16px 20px',
                  transition: 'var(--transition)',
                  cursor: 'pointer',
                }}
                onClick={() => navigate(`/trips/${t.id}`)}
                onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-4px)'}
                onMouseLeave={e => e.currentTarget.style.transform = 'translateY(0)'}
              >
                <div className="flex-row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
                  <span style={{ fontWeight: 700, fontSize: 18 }}>
                    <i className={`fas ${statusIcons[t.status] || 'fa-circle'}`} style={{ marginRight: 8, color: statusColors[t.status] || 'var(--color-primary-dark)' }}></i>
                    {t.title}
                  </span>
                  <span className={`status-badge ${t.status.toLowerCase()}`} style={{ background: statusColors[t.status] || '#e8e0d8' }}>
                    {t.status === 'PLANNED' ? 'Запланирована' :
                     t.status === 'ACTIVE' ? 'Активна' :
                     t.status === 'COMPLETED' ? 'Завершена' : 'Отменена'}
                  </span>
                </div>
                <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>
                  <i className="fas fa-calendar-alt" style={{ marginRight: 6 }}></i>
                  {t.start_date || '—'} — {t.end_date || '—'}
                </div>
                <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
                  <Button
                    variant="primary"
                    className="btn-sm"
                    onClick={(e) => { e.stopPropagation(); navigate(`/trips/${t.id}`); }}
                  >
                    <i className="fas fa-eye"></i> Открыть
                  </Button>
                  <Button
                    variant="secondary"
                    className="btn-sm"
                    onClick={(e) => { e.stopPropagation(); editTrip(t.id); }}
                  >
                    <i className="fas fa-edit"></i>
                  </Button>
                  <Button
                    variant="danger"
                    className="btn-sm"
                    onClick={(e) => { e.stopPropagation(); deleteTrip(t.id); }}
                  >
                    <i className="fas fa-trash"></i>
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
};

export default Trips;