import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../api/api';
import Card from '../common/Card';

const Achievements = () => {
  const [all, setAll] = useState([]);
  const [mine, setMine] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [allRes, mineRes] = await Promise.all([
          apiFetch('/achievements'),
          apiFetch('/users/me/achievements'),
        ]);
        const allData = await allRes.json();
        const mineData = await mineRes.json();
        setAll(allData);
        // дедупликация mineData по achievementId
        const unique = [];
        const seen = new Set();
        (mineData || []).forEach(ua => {
          const achId = ua.achievementId || ua.achievement_id;
          if (!seen.has(achId)) {
            seen.add(achId);
            unique.push(ua);
          }
        });
        setMine(unique);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) return <div>Загрузка...</div>;

  return (
    <div>
      <h2>Достижения</h2>
      <Card title="Мои достижения">
        {mine.length === 0 ? (
          <p>Вы ещё не получили ни одного достижения</p>
        ) : (
          mine.map(ua => {
            const ach = all.find(a => a.id === (ua.achievementId || ua.achievement_id));
            return (
              <div key={ua.id || ua.achievementId || ua.achievement_id} style={{ marginBottom: 12 }}>
                <strong>{ach?.title || 'Достижение'}</strong>
                <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>
                  {ach?.description} — получено {new Date(ua.unlockedAt || ua.unlocked_at).toLocaleString()}
                </div>
              </div>
            );
          })
        )}
      </Card>
      <Card title="Доступные достижения">
        {all.length === 0 ? (
          <p>Нет доступных достижений</p>
        ) : (
          all.map(ach => (
            <div key={ach.id} style={{ marginBottom: 8 }}>
              <strong>{ach.title}</strong>
              <div style={{ fontSize: 14, color: 'var(--color-text-secondary)' }}>
                {ach.description} ({ach.conditionType}: {ach.conditionValue})
              </div>
            </div>
          ))
        )}
      </Card>
    </div>
  );
};

export default Achievements;