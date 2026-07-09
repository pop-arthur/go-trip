import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import { useToast } from '../../contexts/ToastContext';
import Card from '../common/Card';

const Statistics = () => {
  const { showToast } = useToast();
  const [countries, setCountries] = useState({ countries: [], count: 0 });
  const [spending, setSpending] = useState({ total: 0, currency: 'USD', items: [] });
  const [upcoming, setUpcoming] = useState([]);
  const [durations, setDurations] = useState([]);
  const [loading, setLoading] = useState({ countries: true, spending: true, upcoming: true, durations: true });

  const fetchCountries = async () => {
    try {
      const resp = await apiFetch('/statistics/countries');
      if (resp.ok) {
        const data = await resp.json();
        setCountries({ countries: data.countries || [], count: data.countries_count || 0 });
      }
    } catch (e) {
      showToast('Ошибка загрузки статистики стран', 'error');
    } finally {
      setLoading(prev => ({ ...prev, countries: false }));
    }
  };

  const fetchSpending = async () => {
    try {
      const resp = await apiFetch('/statistics/spending');
      if (resp.ok) {
        const data = await resp.json();
        setSpending(data);
      }
    } catch (e) {
      showToast('Ошибка загрузки статистики затрат', 'error');
    } finally {
      setLoading(prev => ({ ...prev, spending: false }));
    }
  };

  const fetchUpcoming = async () => {
    try {
      const resp = await apiFetch('/statistics/upcoming-trips');
      if (resp.ok) {
        const data = await resp.json();
        setUpcoming(data.trips || []);
      }
    } catch (e) {
      showToast('Ошибка загрузки предстоящих поездок', 'error');
    } finally {
      setLoading(prev => ({ ...prev, upcoming: false }));
    }
  };

  const fetchDurations = async () => {
    try {
      const resp = await apiFetch('/statistics/trip-durations');
      if (resp.ok) {
        const data = await resp.json();
        setDurations(data.items || []);
      }
    } catch (e) {
      showToast('Ошибка загрузки длительности поездок', 'error');
    } finally {
      setLoading(prev => ({ ...prev, durations: false }));
    }
  };

  useEffect(() => {
    fetchCountries();
    fetchSpending();
    fetchUpcoming();
    fetchDurations();
    // eslint-disable-next-line
  }, []);

  const renderCurrency = (amount, currency) => {
    // Если валюта USD – показываем как рубли (меняем символ)
    if (currency === 'USD') {
      return `${amount} ₽`;
    }
    return `${amount} ${currency}`;
  };

  return (
    <div>
      <h2><i className="fas fa-chart-simple" style={{ marginRight: 12, color: 'var(--color-primary-dark)' }}></i>Статистика</h2>

      <Card title="Страны" icon="fa-globe">
        {loading.countries ? (
          <p>Загрузка...</p>
        ) : countries.countries.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-globe"></i>
            <p>Нет данных о странах</p>
          </div>
        ) : (
          <div>
            <p>Всего стран: <strong>{countries.count}</strong></p>
            <ul style={{ listStyle: 'none', padding: 0 }}>
              {countries.countries.map(c => <li key={c} style={{ padding: '4px 0' }}><i className="fas fa-flag" style={{ marginRight: 8, color: 'var(--color-primary-dark)' }}></i>{c}</li>)}
            </ul>
          </div>
        )}
      </Card>

      <Card title="Затраты" icon="fa-ruble-sign">
        {loading.spending ? (
          <p>Загрузка...</p>
        ) : spending.items.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-coins"></i>
            <p>Нет данных о затратах</p>
          </div>
        ) : (
          <div>
            <p><strong>Общая сумма:</strong> {renderCurrency(spending.total_amount, spending.currency)}</p>
            {spending.items.map(item => (
              <div key={item.trip_id} className="list-item" style={{ padding: '8px 0' }}>
                <div className="info">
                  <div className="title">{item.trip_title}</div>
                  <div className="sub">{renderCurrency(item.amount, spending.currency)}</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card title="Предстоящие поездки" icon="fa-calendar-check">
        {loading.upcoming ? (
          <p>Загрузка...</p>
        ) : upcoming.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-calendar"></i>
            <p>Нет предстоящих поездок</p>
          </div>
        ) : (
          upcoming.map((t, idx) => (
            <div key={idx} className="list-item" style={{ padding: '8px 0' }}>
              <div className="info">
                <div className="title">{t.trip?.title || 'Без названия'}</div>
                <div className="sub">{t.trip?.start_date || 'дата неизвестна'}</div>
              </div>
            </div>
          ))
        )}
      </Card>

      <Card title="Длительность поездок" icon="fa-clock">
        {loading.durations ? (
          <p>Загрузка...</p>
        ) : durations.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-hourglass"></i>
            <p>Нет данных о длительности</p>
          </div>
        ) : (
          durations.map(d => (
            <div key={d.trip_id} className="list-item" style={{ padding: '8px 0' }}>
              <div className="info">
                <div className="title">{d.trip_title}</div>
                <div className="sub">{d.duration_days} дней ({d.duration_hours} часов)</div>
              </div>
            </div>
          ))
        )}
      </Card>
    </div>
  );
};

export default Statistics;