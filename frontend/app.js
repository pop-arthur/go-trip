// ============================================================
//  TOAST NOTIFICATIONS
// ============================================================
function showToast(msg, type = 'success') {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.style.cssText = `
        position: fixed; bottom: 24px; right: 24px; padding: 14px 24px;
        background: ${type === 'error' ? '#3d1f2a' : '#1f2a3d'};
        border: 1px solid ${type === 'error' ? '#6a3f4f' : '#2f4a6a'};
        border-radius: 12px; color: #f0eef7; font-size: 15px;
        z-index: 9999; max-width: 400px;
        box-shadow: 0 8px 24px rgba(0,0,0,0.6);
        animation: fadeIn 0.2s ease;
    `;
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
}

// ============================================================
//  STATE
// ============================================================
const state = {
    token: localStorage.getItem('access_token'),
    refreshToken: localStorage.getItem('refresh_token'),
    user: null,
    roles: [],
    currentSection: 'profile',
    editing: null,
    tripDetailId: null,
    orderDetailId: null,
    notificationEnabled: null,
};

// ============================================================
//  API HELPERS
// ============================================================
const API_BASE = 'http://localhost:8080';

async function apiFetch(path, opts = {}) {
    const url = API_BASE + path;
    const headers = {
        'Content-Type': 'application/json',
        ...(opts.headers || {})
    };
    if (state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }
    const resp = await fetch(url, {
        ...opts,
        headers,
    });
    if (resp.status === 401) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            headers['Authorization'] = `Bearer ${state.token}`;
            const retry = await fetch(url, {
                ...opts,
                headers,
            });
            return retry;
        } else {
            logout();
            throw new Error('Сессия истекла, войдите заново.');
        }
    }
    return resp;
}

async function refreshAccessToken() {
    if (!state.refreshToken) return false;
    try {
        const resp = await fetch(API_BASE + '/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refresh_token: state.refreshToken })
        });
        if (resp.ok) {
            const data = await resp.json();
            state.token = data.access_token;
            state.refreshToken = data.refresh_token;
            localStorage.setItem('access_token', state.token);
            localStorage.setItem('refresh_token', state.refreshToken);
            return true;
        }
    } catch (e) { /* ignore */ }
    return false;
}

function logout() {
    state.token = null;
    state.refreshToken = null;
    state.user = null;
    state.roles = [];
    state.notificationEnabled = null;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    document.getElementById('app').classList.add('hidden');
    document.getElementById('auth-page').style.display = 'flex';
    document.getElementById('auth-error').textContent = '';
    showAuthForm('login');
}

// ============================================================
//  AUTH
// ============================================================
let authMode = 'login';

function showAuthForm(mode) {
    authMode = mode;
    const submit = document.getElementById('auth-submit');
    const switchText = document.getElementById('auth-switch-text');
    const switchLink = document.getElementById('auth-switch-link');
    const fullnameGroup = document.getElementById('register-fullname-group');
    const emailGroup = document.getElementById('register-email-group');
    const passwordGroup = document.getElementById('register-password-group');

    if (mode === 'register') {
        submit.textContent = 'Зарегистрироваться';
        switchText.textContent = 'Уже есть аккаунт?';
        switchLink.textContent = 'Войти';
        fullnameGroup.classList.remove('hidden');
        emailGroup.querySelector('label').textContent = 'Email';
        passwordGroup.querySelector('label').textContent = 'Пароль (мин. 8)';
    } else {
        submit.textContent = 'Войти';
        switchText.textContent = 'Нет аккаунта?';
        switchLink.textContent = 'Зарегистрироваться';
        fullnameGroup.classList.add('hidden');
        emailGroup.querySelector('label').textContent = 'Email';
        passwordGroup.querySelector('label').textContent = 'Пароль';
    }
}

document.getElementById('auth-switch-link').addEventListener('click', (e) => {
    e.preventDefault();
    const newMode = authMode === 'login' ? 'register' : 'login';
    showAuthForm(newMode);
    document.getElementById('auth-error').textContent = '';
});

document.getElementById('auth-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('auth-email').value;
    const password = document.getElementById('auth-password').value;
    const fullName = document.getElementById('auth-fullname').value;
    const errorEl = document.getElementById('auth-error');
    errorEl.textContent = '';

    try {
        let resp;
        if (authMode === 'register') {
            resp = await apiFetch('/auth/register', {
                method: 'POST',
                body: JSON.stringify({ email, password, full_name: fullName || null })
            });
        } else {
            resp = await apiFetch('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });
        }
        if (!resp.ok) {
            const err = await resp.json();
            throw new Error(err.message || 'Ошибка авторизации');
        }
        const data = await resp.json();
        state.token = data.access_token;
        state.refreshToken = data.refresh_token;
        localStorage.setItem('access_token', state.token);
        localStorage.setItem('refresh_token', state.refreshToken);
        await loadUserProfile();
        document.getElementById('auth-page').style.display = 'none';
        document.getElementById('app').classList.remove('hidden');
        renderApp();
    } catch (err) {
        errorEl.textContent = err.message || 'Произошла ошибка';
    }
});

// ============================================================
//  USER PROFILE
// ============================================================
async function loadUserProfile() {
    const resp = await apiFetch('/users/me');
    if (resp.ok) {
        const user = await resp.json();
        state.user = user;
        state.roles = user.roles || [];
        document.getElementById('sidebar-email').textContent = user.email;
        document.getElementById('sidebar-roles').textContent = state.roles.join(', ');
        if (state.roles.includes('ADMIN')) {
            document.getElementById('nav-admin').classList.remove('hidden');
        } else {
            document.getElementById('nav-admin').classList.add('hidden');
        }
        return user;
    }
    throw new Error('Не удалось загрузить профиль');
}

// ============================================================
//  RENDER ENGINE
// ============================================================
function renderApp() {
    renderSection(state.currentSection);
    document.querySelectorAll('#main-nav a[data-section]').forEach(link => {
        link.addEventListener('click', () => {
            const section = link.dataset.section;
            if (section !== 'trips') {
                state.tripDetailId = null;
                state.orderDetailId = null;
            }
            state.currentSection = section;
            renderSection(section);
            document.querySelectorAll('#main-nav a[data-section]').forEach(l => l.classList.remove('active'));
            link.classList.add('active');
        });
    });
    document.getElementById('logout-btn').addEventListener('click', logout);
}

function renderSection(section) {
    const container = document.getElementById('section-container');
    switch (section) {
        case 'profile': renderProfile(container); break;
        case 'notifications': renderNotifications(container); break;
        case 'achievements': renderAchievements(container); break;
        case 'reviews': renderReviews(container); break;
        case 'locations': renderLocations(container); break;
        case 'providers': renderProviders(container); break;
        case 'services': renderServices(container); break;
        case 'trips': renderTrips(container); break;
        case 'admin': renderAdmin(container); break;
        default: container.innerHTML = '<h2>Страница не найдена</h2>';
    }
}

// ============================================================
//  PROFILE
// ============================================================
function renderProfile(container) {
    if (!state.user) {
        container.innerHTML = '<p>Загрузка...</p>';
        return;
    }
    const u = state.user;
    container.innerHTML = `
        <div class="section active">
            <h2>Профиль</h2>
            <div class="card">
                <h3>Личные данные</h3>
                <div class="form-row">
                    <label>Email <input type="email" id="profile-email" value="${u.email}" /></label>
                    <label>Полное имя <input type="text" id="profile-fullname" value="${u.full_name || ''}" /></label>
                </div>
                <button class="btn btn-primary" id="profile-update-btn">Обновить</button>
            </div>
            <div class="card">
                <h3>Информация</h3>
                <p><strong>ID:</strong> ${u.id}</p>
                <p><strong>Роли:</strong> ${(u.roles || []).join(', ')}</p>
                <p><strong>Дата регистрации:</strong> ${new Date(u.created_at).toLocaleString()}</p>
            </div>
        </div>
    `;
    const updateBtn = document.getElementById('profile-update-btn');
    if (updateBtn) {
        updateBtn.addEventListener('click', async () => {
            const email = document.getElementById('profile-email').value;
            const fullName = document.getElementById('profile-fullname').value;
            try {
                const resp = await apiFetch('/users/me', {
                    method: 'PATCH',
                    body: JSON.stringify({ email, full_name: fullName || null })
                });
                if (resp.ok) {
                    const updated = await resp.json();
                    state.user = updated;
                    state.roles = updated.roles || [];
                    document.getElementById('sidebar-email').textContent = updated.email;
                    document.getElementById('sidebar-roles').textContent = state.roles.join(', ');
                    renderProfile(container);
                    showToast('Профиль обновлён');
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + (err.message || 'Неизвестная ошибка'), 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });
    }
}

// ============================================================
//  NOTIFICATIONS
// ============================================================
function renderNotifications(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Уведомления</h2>
            <div class="card">
                <h3>Настройки уведомлений</h3>
                <div class="form-row">
                    <label>
                        <input type="checkbox" id="notif-toggle" /> Получать уведомления
                    </label>
                    <button class="btn btn-primary" id="notif-save-btn">Сохранить</button>
                </div>
                <div id="notif-status"></div>
            </div>
            <div class="card">
                <h3>История уведомлений</h3>
                <div id="notif-list"></div>
            </div>
        </div>
    `;

    const toggle = document.getElementById('notif-toggle');
    const statusEl = document.getElementById('notif-status');

    function updateToggleFromState() {
        if (state.notificationEnabled !== null) {
            toggle.checked = state.notificationEnabled;
        }
    }

    updateToggleFromState();

    function loadPreferences() {
        apiFetch('/notification-preferences')
            .then(resp => {
                if (!resp.ok) {
                    if (resp.status === 404) {
                        return apiFetch('/notification-preferences', {
                            method: 'PUT',
                            body: JSON.stringify({ isEnabled: true })
                        }).then(resp => {
                            if (!resp.ok) throw new Error(`Не удалось создать настройки: ${resp.status}`);
                            return resp.json();
                        });
                    }
                    throw new Error(`Ошибка загрузки настроек: ${resp.status}`);
                }
                return resp.json();
            })
            .then(pref => {
                if (pref && pref.is_enabled !== undefined) {
                    state.notificationEnabled = pref.is_enabled;
                    updateToggleFromState();
                }
            })
            .catch(err => {
                showToast('Не удалось загрузить настройки уведомлений: ' + err.message, 'error');
                statusEl.innerHTML = '<div class="error-state">Ошибка загрузки настроек</div>';
            });
    }

    if (state.notificationEnabled === null) {
        loadPreferences();
    }

    const saveBtn = document.getElementById('notif-save-btn');
    if (saveBtn) {
        saveBtn.addEventListener('click', async () => {
            const enabled = toggle.checked;
            try {
                const resp = await apiFetch('/notification-preferences', {
                    method: 'PUT',
                    body: JSON.stringify({ isEnabled: enabled })
                });
                if (resp.ok) {
                    state.notificationEnabled = enabled;
                    showToast('Настройки сохранены');
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + (err.message || ''), 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });
    }

    apiFetch('/notifications')
        .then(resp => {
            if (!resp.ok) throw new Error(`Ошибка загрузки уведомлений: ${resp.status}`);
            return resp.json();
        })
        .then(list => {
            const el = document.getElementById('notif-list');
            if (!list || list.length === 0) {
                el.innerHTML = '<div class="empty-state">Нет уведомлений</div>';
                return;
            }
            el.innerHTML = list.map(n => `
                <div class="list-item">
                    <div class="info">
                        <div class="title">${n.title}</div>
                        <div class="sub">${n.body} — ${new Date(n.sent_at).toLocaleString()}</div>
                    </div>
                    <span style="font-size:12px;color:#6b5a90;">${n.is_read ? '✔ Прочитано' : '🆕 Новое'}</span>
                </div>
            `).join('');
        })
        .catch(err => {
            showToast('Не удалось загрузить историю уведомлений: ' + err.message, 'error');
            document.getElementById('notif-list').innerHTML = '<div class="error-state">Ошибка загрузки истории</div>';
        });
}

// ============================================================
//  ACHIEVEMENTS
// ============================================================
function renderAchievements(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Достижения</h2>
            <div class="card">
                <h3>Мои достижения</h3>
                <div id="my-achievements"></div>
            </div>
            <div class="card">
                <h3>Доступные достижения</h3>
                <div id="all-achievements"></div>
            </div>
        </div>
    `;
    apiFetch('/users/me/achievements').then(resp => resp.json()).then(list => {
        const el = document.getElementById('my-achievements');
        if (!list || list.length === 0) {
            el.innerHTML = '<div class="empty-state">Вы ещё не получили ни одного достижения</div>';
            return;
        }
        el.innerHTML = list.map(a => `
            <div class="list-item">
                <div class="info">
                    <div class="title">🏆 ${a.achievement_id || 'Достижение'}</div>
                    <div class="sub">Получено: ${new Date(a.unlocked_at).toLocaleString()}</div>
                </div>
            </div>
        `).join('');
    }).catch(() => {});

    apiFetch('/achievements').then(resp => resp.json()).then(list => {
        const el = document.getElementById('all-achievements');
        if (!list || list.length === 0) {
            el.innerHTML = '<div class="empty-state">Нет доступных достижений</div>';
            return;
        }
        el.innerHTML = list.map(a => `
            <div class="list-item">
                <div class="info">
                    <div class="title">${a.title}</div>
                    <div class="sub">${a.description || ''} (${a.condition_type}: ${a.condition_value})</div>
                </div>
            </div>
        `).join('');
    }).catch(() => {});
}

// ============================================================
//  REVIEWS
// ============================================================
function renderReviews(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Отзывы</h2>
            <div class="card">
                <h3>Создать отзыв</h3>
                <div class="form-row">
                    <label>Тип цели <select id="review-target-type">
                        <option value="PROVIDER">Провайдер</option>
                        <option value="LOCATION">Локация</option>
                        <option value="ORDER">Заказ</option>
                        <option value="ADDITIONAL_SERVICE">Доп. услуга</option>
                    </select></label>
                    <label>ID цели <input type="number" id="review-target-id" /></label>
                </div>
                <div class="form-row">
                    <label>Оценка (1-5) <input type="number" id="review-rating" min="1" max="5" /></label>
                    <label>Текст <textarea id="review-text"></textarea></label>
                </div>
                <button class="btn btn-primary" id="review-create-btn">Оставить отзыв</button>
            </div>
            <div class="card">
                <h3>Список отзывов</h3>
                <div class="form-row">
                    <label>Тип цели <select id="review-filter-type">
                        <option value="">Все</option>
                        <option value="PROVIDER">Провайдер</option>
                        <option value="LOCATION">Локация</option>
                        <option value="ORDER">Заказ</option>
                        <option value="ADDITIONAL_SERVICE">Доп. услуга</option>
                    </select></label>
                    <label>ID цели <input type="number" id="review-filter-id" /></label>
                    <button class="btn" id="review-filter-btn">Поиск</button>
                </div>
                <div id="review-list"></div>
            </div>
        </div>
    `;

    const createBtn = document.getElementById('review-create-btn');
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const targetType = document.getElementById('review-target-type').value;
            const targetId = parseInt(document.getElementById('review-target-id').value);
            const rating = parseInt(document.getElementById('review-rating').value);
            const text = document.getElementById('review-text').value;
            if (!targetId || !rating) {
                showToast('Заполните все обязательные поля', 'error');
                return;
            }
            try {
                const resp = await apiFetch('/reviews', {
                    method: 'POST',
                    body: JSON.stringify({
                        targetType: targetType,
                        targetId: targetId,
                        rating: rating,
                        text: text || null
                    })
                });
                if (resp.ok) {
                    showToast('Отзыв создан');
                    renderReviews(container);
                } else {
                    const textBody = await resp.text();
                    let errMsg = 'Ошибка создания отзыва';
                    try {
                        const json = JSON.parse(textBody);
                        errMsg = json.message || errMsg;
                    } catch (_) {
                        errMsg = textBody || errMsg;
                    }
                    showToast('Ошибка: ' + errMsg, 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });
    }

    const filterBtn = document.getElementById('review-filter-btn');
    if (filterBtn) {
        filterBtn.addEventListener('click', () => {
            loadReviews();
        });
    }
    loadReviews();

    async function loadReviews() {
        const targetType = document.getElementById('review-filter-type').value;
        const targetId = document.getElementById('review-filter-id').value;
        let url = '/reviews';
        const params = new URLSearchParams();
        if (targetType) params.append('targetType', targetType);
        if (targetId) params.append('targetId', targetId);
        if (params.toString()) url += '?' + params.toString();
        try {
            const resp = await apiFetch(url);
            if (resp.ok) {
                const list = await resp.json();
                const el = document.getElementById('review-list');
                if (!list || list.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет отзывов</div>';
                    return;
                }
                el.innerHTML = list.map(r => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">⭐ ${r.rating}/5 — ${r.target_type} #${r.target_id}</div>
                            <div class="sub">${r.text || '(без текста)'} — от ${r.user_id}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="editReview(${r.id})">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteReview(${r.id})">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) {
            showToast('Ошибка загрузки отзывов', 'error');
        }
    }

    window.editReview = (id) => {
        apiFetch(`/reviews/${id}`).then(resp => resp.json()).then(review => {
            const newRating = prompt('Новая оценка (1-5):', review.rating);
            if (newRating === null) return;
            const newText = prompt('Новый текст:', review.text || '');
            if (newText === null) return;
            apiFetch(`/reviews/${id}`, {
                method: 'PATCH',
                body: JSON.stringify({ rating: parseInt(newRating), text: newText })
            }).then(resp => {
                if (resp.ok) { showToast('Отзыв обновлён'); loadReviews(); }
                else showToast('Ошибка обновления', 'error');
            }).catch(() => showToast('Ошибка', 'error'));
        }).catch(() => showToast('Не удалось загрузить отзыв', 'error'));
    };

    window.deleteReview = async (id) => {
        if (!confirm('Удалить отзыв?')) return;
        try {
            const resp = await apiFetch(`/reviews/${id}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); loadReviews(); }
            else showToast('Ошибка удаления', 'error');
        } catch (e) {
            showToast('Ошибка', 'error');
        }
    };
}

// ============================================================
//  LOCATIONS
// ============================================================
function renderLocations(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Локации</h2>
            <div class="card">
                <h3>Создать локацию</h3>
                <div class="form-row">
                    <label>Название <input id="loc-name" /></label>
                    <label>Тип <select id="loc-type">
                        <option value="COUNTRY">COUNTRY</option>
                        <option value="CITY">CITY</option>
                        <option value="AIRPORT">AIRPORT</option>
                        <option value="TRAIN_STATION">TRAIN_STATION</option>
                        <option value="BUS_STATION">BUS_STATION</option>
                        <option value="PORT">PORT</option>
                        <option value="HOTEL">HOTEL</option>
                        <option value="MEETING_POINT">MEETING_POINT</option>
                        <option value="ATTRACTION">ATTRACTION</option>
                        <option value="OTHER">OTHER</option>
                    </select></label>
                </div>
                <div class="form-row">
                    <label>Страна <input id="loc-country" /></label>
                    <label>Город <input id="loc-city" /></label>
                    <label>Адрес <input id="loc-address" /></label>
                </div>
                <div class="form-row">
                    <label>Широта <input id="loc-lat" type="number" step="any" /></label>
                    <label>Долгота <input id="loc-lon" type="number" step="any" /></label>
                </div>
                <button class="btn btn-primary" id="loc-create-btn">Создать</button>
            </div>
            <div class="card">
                <h3>Поиск локаций</h3>
                <div class="form-row">
                    <label>Тип <select id="loc-filter-type"><option value="">Все</option>
                        <option value="COUNTRY">COUNTRY</option><option value="CITY">CITY</option>
                        <option value="AIRPORT">AIRPORT</option><option value="TRAIN_STATION">TRAIN_STATION</option>
                        <option value="BUS_STATION">BUS_STATION</option><option value="PORT">PORT</option>
                        <option value="HOTEL">HOTEL</option><option value="MEETING_POINT">MEETING_POINT</option>
                        <option value="ATTRACTION">ATTRACTION</option><option value="OTHER">OTHER</option>
                    </select></label>
                    <label>Страна <input id="loc-filter-country" /></label>
                    <label>Город <input id="loc-filter-city" /></label>
                    <label>Запрос <input id="loc-filter-query" /></label>
                    <button class="btn" id="loc-search-btn">Поиск</button>
                </div>
                <div id="loc-list"></div>
            </div>
        </div>
    `;
    const createBtn = document.getElementById('loc-create-btn');
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const name = document.getElementById('loc-name').value;
            const type = document.getElementById('loc-type').value;
            const country = document.getElementById('loc-country').value;
            const city = document.getElementById('loc-city').value;
            const address = document.getElementById('loc-address').value;
            const lat = parseFloat(document.getElementById('loc-lat').value) || null;
            const lon = parseFloat(document.getElementById('loc-lon').value) || null;
            if (!name) { showToast('Название обязательно', 'error'); return; }
            try {
                const resp = await apiFetch('/locations', {
                    method: 'POST',
                    body: JSON.stringify({ name, type, country, city, address, latitude: lat, longitude: lon })
                });
                if (resp.ok) { showToast('Создано'); renderLocations(container); }
                else { const err = await resp.json(); showToast('Ошибка: ' + err.message, 'error'); }
            } catch (e) { showToast('Ошибка: ' + e.message, 'error'); }
        });
    }

    const searchBtn = document.getElementById('loc-search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', loadLocations);
    }
    loadLocations();

    async function loadLocations() {
        const type = document.getElementById('loc-filter-type').value;
        const country = document.getElementById('loc-filter-country').value;
        const city = document.getElementById('loc-filter-city').value;
        const query = document.getElementById('loc-filter-query').value;
        const params = new URLSearchParams();
        if (type) params.append('type', type);
        if (country) params.append('country', country);
        if (city) params.append('city', city);
        if (query) params.append('query', query);
        const url = '/locations' + (params.toString() ? '?' + params.toString() : '');
        try {
            const resp = await apiFetch(url);
            if (resp.ok) {
                const list = await resp.json();
                const el = document.getElementById('loc-list');
                if (!list || list.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет локаций</div>';
                    return;
                }
                el.innerHTML = list.map(l => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${l.name} (${l.type})</div>
                            <div class="sub">${l.country || ''} ${l.city || ''} ${l.address || ''}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="editLocation(${l.id})">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteLocation(${l.id})">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка загрузки', 'error'); }
    }

    window.editLocation = (id) => {
        apiFetch(`/locations/${id}`).then(resp => resp.json()).then(loc => {
            const name = prompt('Название:', loc.name);
            if (name === null) return;
            const type = prompt('Тип:', loc.type);
            if (type === null) return;
            const country = prompt('Страна:', loc.country || '');
            const city = prompt('Город:', loc.city || '');
            apiFetch(`/locations/${id}`, {
                method: 'PATCH',
                body: JSON.stringify({ name, type, country: country || null, city: city || null })
            }).then(resp => {
                if (resp.ok) { showToast('Обновлено'); loadLocations(); }
                else showToast('Ошибка', 'error');
            });
        }).catch(() => showToast('Ошибка', 'error'));
    };
    window.deleteLocation = async (id) => {
        if (!confirm('Удалить локацию?')) return;
        try {
            const resp = await apiFetch(`/locations/${id}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); loadLocations(); }
            else showToast('Ошибка', 'error');
        } catch (e) { showToast('Ошибка', 'error'); }
    };
}

// ============================================================
//  PROVIDERS
// ============================================================
function renderProviders(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Провайдеры</h2>
            <div class="card">
                <h3>Создать провайдера</h3>
                <div class="form-row">
                    <label>Название <input id="prov-name" /></label>
                    <label>Тип <select id="prov-type">
                        <option value="AIRLINE">AIRLINE</option><option value="HOTEL">HOTEL</option>
                        <option value="TOUR_COMPANY">TOUR_COMPANY</option><option value="TRANSPORT_COMPANY">TRANSPORT_COMPANY</option>
                        <option value="BOOKING_PLATFORM">BOOKING_PLATFORM</option><option value="INSURANCE_COMPANY">INSURANCE_COMPANY</option>
                        <option value="OTHER">OTHER</option>
                    </select></label>
                </div>
                <div class="form-row">
                    <label>Сайт <input id="prov-website" /></label>
                    <label>Контакты <input id="prov-contact" /></label>
                </div>
                <button class="btn btn-primary" id="prov-create-btn">Создать</button>
            </div>
            <div class="card">
                <h3>Список провайдеров</h3>
                <div class="form-row">
                    <label>Тип <select id="prov-filter-type"><option value="">Все</option>
                        <option value="AIRLINE">AIRLINE</option><option value="HOTEL">HOTEL</option>
                        <option value="TOUR_COMPANY">TOUR_COMPANY</option><option value="TRANSPORT_COMPANY">TRANSPORT_COMPANY</option>
                        <option value="BOOKING_PLATFORM">BOOKING_PLATFORM</option><option value="INSURANCE_COMPANY">INSURANCE_COMPANY</option>
                        <option value="OTHER">OTHER</option>
                    </select></label>
                    <label>Запрос <input id="prov-filter-query" /></label>
                    <button class="btn" id="prov-search-btn">Поиск</button>
                </div>
                <div id="prov-list"></div>
            </div>
        </div>
    `;
    const createBtn = document.getElementById('prov-create-btn');
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const name = document.getElementById('prov-name').value;
            const type = document.getElementById('prov-type').value;
            const website = document.getElementById('prov-website').value;
            const contact = document.getElementById('prov-contact').value;
            if (!name) { showToast('Название обязательно', 'error'); return; }
            try {
                const resp = await apiFetch('/providers', {
                    method: 'POST',
                    body: JSON.stringify({ name, type, website: website || null, support_contact: contact || null })
                });
                if (resp.ok) { showToast('Создано'); renderProviders(container); }
                else { const err = await resp.json(); showToast('Ошибка: ' + err.message, 'error'); }
            } catch (e) { showToast('Ошибка: ' + e.message, 'error'); }
        });
    }

    const searchBtn = document.getElementById('prov-search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', loadProviders);
    }
    loadProviders();

    async function loadProviders() {
        const type = document.getElementById('prov-filter-type').value;
        const query = document.getElementById('prov-filter-query').value;
        const params = new URLSearchParams();
        if (type) params.append('type', type);
        if (query) params.append('query', query);
        const url = '/providers' + (params.toString() ? '?' + params.toString() : '');
        try {
            const resp = await apiFetch(url);
            if (resp.ok) {
                const list = await resp.json();
                const el = document.getElementById('prov-list');
                if (!list || list.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет провайдеров</div>';
                    return;
                }
                el.innerHTML = list.map(p => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${p.name} (${p.type})</div>
                            <div class="sub">${p.website || ''} ${p.support_contact || ''}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-danger" onclick="deleteProvider(${p.id})">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка', 'error'); }
    }

    window.deleteProvider = async (id) => {
        if (!confirm('Удалить провайдера?')) return;
        try {
            const resp = await apiFetch(`/providers/${id}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); loadProviders(); }
            else showToast('Ошибка', 'error');
        } catch (e) { showToast('Ошибка', 'error'); }
    };
}

// ============================================================
//  ADDITIONAL SERVICES
// ============================================================
function renderServices(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Дополнительные услуги</h2>
            <div class="card">
                <h3>Список услуг</h3>
                <div class="form-row">
                    <label>Тип услуги <select id="svc-filter-type">
                        <option value="">Все</option>
                        <option value="FLIGHT">FLIGHT</option><option value="TRAIN">TRAIN</option>
                        <option value="BUS">BUS</option><option value="HOTEL">HOTEL</option>
                        <option value="TOUR">TOUR</option><option value="CAR_RENTAL">CAR_RENTAL</option>
                        <option value="INSURANCE">INSURANCE</option><option value="TAXI">TAXI</option>
                        <option value="ESIM">ESIM</option><option value="LOUNGE">LOUNGE</option>
                        <option value="EXTRA_BAGGAGE">EXTRA_BAGGAGE</option><option value="OTHER">OTHER</option>
                    </select></label>
                    <label>ID локации <input id="svc-filter-loc" type="number" /></label>
                    <label>ID провайдера <input id="svc-filter-prov" type="number" /></label>
                    <button class="btn" id="svc-search-btn">Поиск</button>
                </div>
                <div id="svc-list"></div>
            </div>
        </div>
    `;
    const searchBtn = document.getElementById('svc-search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', loadServices);
    }
    loadServices();

    async function loadServices() {
        const serviceType = document.getElementById('svc-filter-type').value;
        const locId = document.getElementById('svc-filter-loc').value;
        const provId = document.getElementById('svc-filter-prov').value;
        const params = new URLSearchParams();
        if (serviceType) params.append('serviceType', serviceType);
        if (locId) params.append('locationId', locId);
        if (provId) params.append('providerId', provId);
        const url = '/additional-services' + (params.toString() ? '?' + params.toString() : '');
        try {
            const resp = await apiFetch(url);
            if (resp.ok) {
                const list = await resp.json();
                const el = document.getElementById('svc-list');
                if (!list || list.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет услуг</div>';
                    return;
                }
                el.innerHTML = list.map(s => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${s.title} (${s.service_type})</div>
                            <div class="sub">${s.description || ''} ${s.price_amount ? s.price_amount + ' ' + s.price_currency : ''}</div>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка', 'error'); }
    }
}

// ============================================================
//  TRIPS
// ============================================================
function renderTrips(container) {
    if (state.tripDetailId) {
        renderTripDetail(container);
        return;
    }
    container.innerHTML = `
        <div class="section active">
            <h2>Поездки</h2>
            <div class="card">
                <h3>Создать поездку</h3>
                <div class="form-row">
                    <label>Название <input id="trip-title" /></label>
                    <label>Дата начала <input id="trip-start" type="date" /></label>
                    <label>Дата окончания <input id="trip-end" type="date" /></label>
                </div>
                <div class="form-row">
                    <label>Статус <select id="trip-status">
                        <option value="PLANNED">Запланирована</option>
                        <option value="ACTIVE">Активна</option>
                        <option value="COMPLETED">Завершена</option>
                        <option value="CANCELLED">Отменена</option>
                    </select></label>
                </div>
                <button class="btn btn-primary" id="trip-create-btn">Создать</button>
            </div>
            <div class="card">
                <h3>Мои поездки</h3>
                <div class="form-row">
                    <label>Статус <select id="trip-filter-status">
                        <option value="">Все</option>
                        <option value="PLANNED">Запланирована</option>
                        <option value="ACTIVE">Активна</option>
                        <option value="COMPLETED">Завершена</option>
                        <option value="CANCELLED">Отменена</option>
                    </select></label>
                    <label>С <input id="trip-filter-from" type="date" /></label>
                    <label>По <input id="trip-filter-to" type="date" /></label>
                    <button class="btn" id="trip-filter-btn">Поиск</button>
                </div>
                <div id="trip-list"></div>
            </div>
        </div>
    `;

    const createBtn = document.getElementById('trip-create-btn');
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const title = document.getElementById('trip-title').value;
            const startDate = document.getElementById('trip-start').value;
            const endDate = document.getElementById('trip-end').value;
            const status = document.getElementById('trip-status').value;
            if (!title) { showToast('Название обязательно', 'error'); return; }
            try {
                const resp = await apiFetch('/trips', {
                    method: 'POST',
                    body: JSON.stringify({
                        title,
                        start_date: startDate || null,
                        end_date: endDate || null,
                        status
                    })
                });
                if (resp.ok) {
                    showToast('Поездка создана');
                    renderTrips(container);
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + (err.message || ''), 'error');
                }
            } catch (e) { showToast('Ошибка: ' + e.message, 'error'); }
        });
    }

    const filterBtn = document.getElementById('trip-filter-btn');
    if (filterBtn) {
        filterBtn.addEventListener('click', loadTrips);
    }
    loadTrips();

    async function loadTrips() {
        const status = document.getElementById('trip-filter-status').value;
        const fromDate = document.getElementById('trip-filter-from').value;
        const toDate = document.getElementById('trip-filter-to').value;
        const params = new URLSearchParams();
        if (status) params.append('status', status);
        if (fromDate) params.append('fromDate', fromDate);
        if (toDate) params.append('toDate', toDate);
        const url = '/trips' + (params.toString() ? '?' + params.toString() : '');
        try {
            const resp = await apiFetch(url);
            if (resp.ok) {
                const list = await resp.json();
                const el = document.getElementById('trip-list');
                if (!list || list.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет поездок</div>';
                    return;
                }
                el.innerHTML = list.map(t => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${t.title}</div>
                            <div class="sub">${t.status} ${t.start_date ? 'с ' + t.start_date : ''} ${t.end_date ? 'по ' + t.end_date : ''}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="viewTrip(${t.id})">📋</button>
                            <button class="btn btn-sm btn-outline" onclick="editTrip(${t.id})">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteTrip(${t.id})">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка загрузки поездок', 'error'); }
    }

    window.viewTrip = (id) => {
        state.tripDetailId = id;
        renderTrips(container);
    };

    window.editTrip = (id) => {
        apiFetch(`/trips/${id}`).then(resp => resp.json()).then(trip => {
            const title = prompt('Название:', trip.title);
            if (title === null) return;
            const startDate = prompt('Дата начала (YYYY-MM-DD):', trip.start_date || '');
            const endDate = prompt('Дата окончания (YYYY-MM-DD):', trip.end_date || '');
            const status = prompt('Статус (PLANNED/ACTIVE/COMPLETED/CANCELLED):', trip.status);
            if (!status) return;
            apiFetch(`/trips/${id}`, {
                method: 'PATCH',
                body: JSON.stringify({ title, start_date: startDate || null, end_date: endDate || null, status })
            }).then(resp => {
                if (resp.ok) { showToast('Поездка обновлена'); loadTrips(); }
                else showToast('Ошибка', 'error');
            });
        }).catch(() => showToast('Ошибка', 'error'));
    };

    window.deleteTrip = async (id) => {
        if (!confirm('Удалить поездку?')) return;
        try {
            const resp = await apiFetch(`/trips/${id}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); loadTrips(); }
            else showToast('Ошибка', 'error');
        } catch (e) { showToast('Ошибка', 'error'); }
    };
}

// ============================================================
//  TRIP DETAIL
// ============================================================
function renderTripDetail(container) {
    const tripId = state.tripDetailId;
    container.innerHTML = `
        <div class="section active">
            <div class="back-link" id="trip-back-btn">← Назад к списку</div>
            <h2 id="trip-detail-title">Поездка #${tripId}</h2>
            <div class="card">
                <h3>Информация о поездке</h3>
                <div id="trip-info"></div>
            </div>
            <div class="card">
                <h3>Заказы</h3>
                <button class="btn btn-primary" id="order-create-btn" style="margin-bottom:16px;">+ Создать заказ</button>
                <div id="order-list"></div>
            </div>
        </div>
    `;

    const backBtn = document.getElementById('trip-back-btn');
    if (backBtn) {
        backBtn.addEventListener('click', () => {
            state.tripDetailId = null;
            renderTrips(container);
        });
    }

    apiFetch(`/trips/${tripId}`).then(resp => resp.json()).then(trip => {
        document.getElementById('trip-detail-title').textContent = `Поездка: ${trip.title}`;
        document.getElementById('trip-info').innerHTML = `
            <p><strong>ID:</strong> ${trip.id}</p>
            <p><strong>Название:</strong> ${trip.title}</p>
            <p><strong>Дата начала:</strong> ${trip.start_date || '—'}</p>
            <p><strong>Дата окончания:</strong> ${trip.end_date || '—'}</p>
            <p><strong>Статус:</strong> ${trip.status}</p>
        `;
    }).catch(() => showToast('Ошибка загрузки поездки', 'error'));

    window.loadOrders = async function() {
        const el = document.getElementById('order-list');
        if (!el) return;
        try {
            const resp = await apiFetch(`/trips/${tripId}/orders`);
            if (resp.ok) {
                const orders = await resp.json();
                if (!orders || orders.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет заказов</div>';
                    return;
                }
                el.innerHTML = orders.map(o => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${o.title} (${o.service_type})</div>
                            <div class="sub">${o.status} — ${o.price_amount ? o.price_amount + ' ' + o.price_currency : ''}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="viewOrderFiles(${o.id})">📁</button>
                            <button class="btn btn-sm btn-outline" onclick="editOrder(${o.id})">✎</button>
                            <button class="btn btn-sm btn-outline" onclick="changeOrderStatus(${o.id})">🔄</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteOrder(${o.id})">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) {
            el.innerHTML = '<div class="error-state">Ошибка загрузки заказов</div>';
        }
    };

    window.loadOrders();

    const orderCreateBtn = document.getElementById('order-create-btn');
    if (orderCreateBtn) {
        orderCreateBtn.addEventListener('click', () => {
            showOrderCreateForm(tripId, container);
        });
    }

    window.viewOrderFiles = (orderId) => {
        state.orderDetailId = orderId;
        renderOrderFiles(container);
    };

    window.editOrder = (orderId) => {
        apiFetch(`/orders/${orderId}`).then(resp => resp.json()).then(order => {
            const title = prompt('Название:', order.title);
            if (title === null) return;
            const status = prompt('Статус (PENDING_VERIFICATION/CONFIRMED/DELAYED/CANCELLED/COMPLETED/REFUND_PENDING/REFUNDED):', order.status);
            if (!status) return;
            const price = prompt('Цена:', order.price_amount || '');
            const currency = prompt('Валюта:', order.price_currency || '');
            apiFetch(`/orders/${orderId}`, {
                method: 'PATCH',
                body: JSON.stringify({ title, status, price_amount: price ? parseFloat(price) : null, price_currency: currency || null })
            }).then(resp => {
                if (resp.ok) { showToast('Заказ обновлён'); window.loadOrders(); }
                else showToast('Ошибка', 'error');
            });
        }).catch(() => showToast('Ошибка', 'error'));
    };

    window.changeOrderStatus = (orderId) => {
        const newStatus = prompt('Новый статус (PENDING_VERIFICATION/CONFIRMED/DELAYED/CANCELLED/COMPLETED/REFUND_PENDING/REFUNDED):');
        if (!newStatus) return;
        const reason = prompt('Причина (опционально):');
        apiFetch(`/orders/${orderId}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status: newStatus, reason: reason || null })
        }).then(resp => {
            if (resp.ok) { showToast('Статус обновлён'); window.loadOrders(); }
            else showToast('Ошибка', 'error');
        }).catch(() => showToast('Ошибка', 'error'));
    };

    window.deleteOrder = async (orderId) => {
        if (!confirm('Удалить заказ?')) return;
        try {
            const resp = await apiFetch(`/orders/${orderId}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); window.loadOrders(); }
            else showToast('Ошибка', 'error');
        } catch (e) { showToast('Ошибка', 'error'); }
    };
}

// ============================================================
//  ORDER CREATE FORM
// ============================================================
function showOrderCreateForm(tripId, container) {
    const card = document.createElement('div');
    card.className = 'card';
    card.id = 'order-create-card';
    card.innerHTML = `
        <h3>Создать заказ</h3>
        <div class="form-row">
            <label>Тип услуги <select id="order-service-type">
                <option value="FLIGHT">FLIGHT</option><option value="TRAIN">TRAIN</option>
                <option value="BUS">BUS</option><option value="HOTEL">HOTEL</option>
                <option value="TOUR">TOUR</option><option value="CAR_RENTAL">CAR_RENTAL</option>
                <option value="INSURANCE">INSURANCE</option><option value="TAXI">TAXI</option>
                <option value="ESIM">ESIM</option><option value="LOUNGE">LOUNGE</option>
                <option value="EXTRA_BAGGAGE">EXTRA_BAGGAGE</option><option value="OTHER">OTHER</option>
            </select></label>
            <label>Название <input id="order-title" /></label>
        </div>
        <div class="form-row">
            <label>Статус <select id="order-status">
                <option value="PENDING_VERIFICATION">PENDING_VERIFICATION</option>
                <option value="CONFIRMED">CONFIRMED</option>
                <option value="DELAYED">DELAYED</option>
                <option value="CANCELLED">CANCELLED</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="REFUND_PENDING">REFUND_PENDING</option>
                <option value="REFUNDED">REFUNDED</option>
            </select></label>
            <label>Цена <input id="order-price" type="number" step="any" /></label>
            <label>Валюта <input id="order-currency" /></label>
        </div>
        <div class="form-row">
            <label>ID провайдера <input id="order-provider" type="number" /></label>
            <label>Внешний ID <input id="order-ext-id" /></label>
        </div>
        <div class="form-row">
            <label>Начало <input id="order-start" type="datetime-local" /></label>
            <label>Конец <input id="order-end" type="datetime-local" /></label>
        </div>
        <div class="form-row">
            <label>ID локации отправления <input id="order-dep-loc" type="number" /></label>
            <label>ID локации прибытия <input id="order-arr-loc" type="number" /></label>
        </div>
        <button class="btn btn-primary" id="order-save-btn">Сохранить</button>
        <button class="btn" id="order-cancel-btn">Отмена</button>
    `;
    const listContainer = document.getElementById('order-list');
    if (!listContainer) return;
    listContainer.parentNode.insertBefore(card, listContainer);

    const cancelBtn = document.getElementById('order-cancel-btn');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
            card.remove();
        });
    }

    const saveBtn = document.getElementById('order-save-btn');
    if (saveBtn) {
        saveBtn.addEventListener('click', async () => {
            const serviceType = document.getElementById('order-service-type').value;
            const title = document.getElementById('order-title').value;
            const status = document.getElementById('order-status').value;
            const price = parseFloat(document.getElementById('order-price').value);
            const currency = document.getElementById('order-currency').value;
            const providerId = document.getElementById('order-provider').value;
            const extId = document.getElementById('order-ext-id').value;
            const start = document.getElementById('order-start').value;
            const end = document.getElementById('order-end').value;
            const depLoc = document.getElementById('order-dep-loc').value;
            const arrLoc = document.getElementById('order-arr-loc').value;
            if (!title) { showToast('Название обязательно', 'error'); return; }
            const body = {
                service_type: serviceType,
                title,
                status,
                price_amount: isNaN(price) ? null : price,
                price_currency: currency || null,
                external_order_id: extId || null,
                provider_id: providerId ? parseInt(providerId) : null,
                start_datetime: start ? new Date(start).toISOString() : null,
                end_datetime: end ? new Date(end).toISOString() : null,
                departure_location_id: depLoc ? parseInt(depLoc) : null,
                arrival_location_id: arrLoc ? parseInt(arrLoc) : null
            };
            try {
                const resp = await apiFetch(`/trips/${tripId}/orders`, {
                    method: 'POST',
                    body: JSON.stringify(body)
                });
                if (resp.ok) {
                    showToast('Заказ создан');
                    card.remove();
                    if (typeof window.loadOrders === 'function') {
                        window.loadOrders();
                    } else {
                        renderTripDetail(container);
                    }
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + (err.message || ''), 'error');
                }
            } catch (e) { showToast('Ошибка: ' + e.message, 'error'); }
        });
    }
}

// ============================================================
//  ORDER FILES
// ============================================================
function renderOrderFiles(container) {
    const orderId = state.orderDetailId;
    container.innerHTML = `
        <div class="section active">
            <div class="back-link" id="order-files-back-btn">← Назад к заказам</div>
            <h2>Файлы заказа #${orderId}</h2>
            <div class="card">
                <h3>Добавить файл</h3>
                <div class="form-row">
                    <label>URL файла <input id="file-url" /></label>
                    <label>Тип файла <select id="file-type">
                        <option value="PDF">PDF</option>
                        <option value="IMAGE">IMAGE</option>
                        <option value="EMAIL">EMAIL</option>
                        <option value="JSON">JSON</option>
                        <option value="OTHER">OTHER</option>
                    </select></label>
                </div>
                <button class="btn btn-primary" id="file-create-btn">Добавить</button>
            </div>
            <div class="card">
                <h3>Список файлов</h3>
                <div id="file-list"></div>
            </div>
        </div>
    `;

    const backBtn = document.getElementById('order-files-back-btn');
    if (backBtn) {
        backBtn.addEventListener('click', () => {
            state.orderDetailId = null;
            renderTripDetail(container);
        });
    }

    const createBtn = document.getElementById('file-create-btn');
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const fileUrl = document.getElementById('file-url').value;
            const fileType = document.getElementById('file-type').value;
            if (!fileUrl) { showToast('URL обязателен', 'error'); return; }
            try {
                const resp = await apiFetch(`/orders/${orderId}/files`, {
                    method: 'POST',
                    body: JSON.stringify({ file_url: fileUrl, file_type: fileType })
                });
                if (resp.ok) {
                    showToast('Файл добавлен');
                    loadFiles();
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + (err.message || ''), 'error');
                }
            } catch (e) { showToast('Ошибка: ' + e.message, 'error'); }
        });
    }

    loadFiles();

    async function loadFiles() {
        const el = document.getElementById('file-list');
        if (!el) return;
        try {
            const resp = await apiFetch(`/orders/${orderId}/files`);
            if (resp.ok) {
                const files = await resp.json();
                if (!files || files.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет файлов</div>';
                    return;
                }
                el.innerHTML = files.map(f => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${f.file_url}</div>
                            <div class="sub">${f.file_type} — ${new Date(f.uploaded_at).toLocaleString()}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-danger" onclick="deleteFile(${f.id})">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) {
            el.innerHTML = '<div class="error-state">Ошибка загрузки файлов</div>';
        }
    }

    window.deleteFile = async (fileId) => {
        if (!confirm('Удалить файл?')) return;
        try {
            const resp = await apiFetch(`/orders/${orderId}/files/${fileId}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); loadFiles(); }
            else showToast('Ошибка', 'error');
        } catch (e) { showToast('Ошибка', 'error');
    };
}

// ============================================================
//  ADMIN PANEL
// ============================================================
function renderAdmin(container) {
    if (!state.roles.includes('ADMIN')) {
        container.innerHTML = `<div class="section active"><h2>Доступ запрещён</h2><p>У вас нет прав администратора.</p></div>`;
        return;
    }
    container.innerHTML = `
        <div class="section active">
            <h2>Административная панель</h2>
            <div class="card">
                <h3>Управление достижениями</h3>
                <div class="form-row">
                    <label>Код <input id="adm-ach-code" /></label>
                    <label>Название <input id="adm-ach-title" /></label>
                </div>
                <div class="form-row">
                    <label>Описание <input id="adm-ach-desc" /></label>
                    <label>Тип условия <select id="adm-ach-cond">
                        <option value="TRIPS_COUNT">TRIPS_COUNT</option>
                        <option value="COUNTRIES_COUNT">COUNTRIES_COUNT</option>
                        <option value="ORDERS_COUNT">ORDERS_COUNT</option>
                        <option value="REVIEWS_COUNT">REVIEWS_COUNT</option>
                        <option value="SPENDING_AMOUNT">SPENDING_AMOUNT</option>
                    </select></label>
                </div>
                <div class="form-row">
                    <label>Значение <input id="adm-ach-value" type="number" /></label>
                    <label>URL иконки <input id="adm-ach-icon" /></label>
                </div>
                <button class="btn btn-primary" id="adm-ach-create">Создать достижение</button>
                <hr style="margin:20px 0;border-color:#2a2440;" />
                <div id="adm-ach-list"></div>
            </div>
        </div>
    `;
    loadAdminAchievements();

    const createBtn = document.getElementById('adm-ach-create');
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const code = document.getElementById('adm-ach-code').value;
            const title = document.getElementById('adm-ach-title').value;
            const description = document.getElementById('adm-ach-desc').value;
            const conditionType = document.getElementById('adm-ach-cond').value;
            const conditionValue = parseInt(document.getElementById('adm-ach-value').value);
            const iconUrl = document.getElementById('adm-ach-icon').value;
            if (!code || !title || !conditionValue) {
                showToast('Заполните обязательные поля', 'error');
                return;
            }
            try {
                const resp = await apiFetch('/admin/achievements', {
                    method: 'POST',
                    body: JSON.stringify({ code, title, description: description || null, conditionType, conditionValue, iconUrl: iconUrl || null })
                });
                if (resp.ok) { showToast('Достижение создано'); loadAdminAchievements(); }
                else { const err = await resp.json(); showToast('Ошибка: ' + err.message, 'error'); }
            } catch (e) { showToast('Ошибка: ' + e.message, 'error'); }
        });
    }

    async function loadAdminAchievements() {
        try {
            const resp = await apiFetch('/achievements');
            if (resp.ok) {
                const list = await resp.json();
                const el = document.getElementById('adm-ach-list');
                if (!el) return;
                if (!list || list.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет достижений</div>';
                    return;
                }
                el.innerHTML = list.map(a => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${a.title} (${a.code})</div>
                            <div class="sub">${a.description || ''}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="adminEditAchievement(${a.id})">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="adminDeleteAchievement(${a.id})">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка загрузки', 'error'); }
    }

    window.adminEditAchievement = (id) => {
        apiFetch(`/achievements`).then(resp => resp.json()).then(list => {
            const a = list.find(x => x.id === id);
            if (!a) return;
            const code = prompt('Код:', a.code);
            if (code === null) return;
            const title = prompt('Название:', a.title);
            if (title === null) return;
            const desc = prompt('Описание:', a.description || '');
            const condType = prompt('Тип условия:', a.condition_type);
            const condValue = prompt('Значение:', a.condition_value);
            if (condValue === null) return;
            apiFetch(`/admin/achievements/${id}`, {
                method: 'PATCH',
                body: JSON.stringify({ code, title, description: desc || null, conditionType: condType, conditionValue: parseInt(condValue) })
            }).then(resp => {
                if (resp.ok) { showToast('Достижение обновлено'); loadAdminAchievements(); }
                else showToast('Ошибка', 'error');
            });
        }).catch(() => showToast('Ошибка', 'error'));
    };
    window.adminDeleteAchievement = async (id) => {
        if (!confirm('Удалить достижение?')) return;
        try {
            const resp = await apiFetch(`/admin/achievements/${id}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); loadAdminAchievements(); }
            else showToast('Ошибка', 'error');
        } catch (e) { showToast('Ошибка', 'error');
        }
    };
}

// ============================================================
//  INIT
// ============================================================
document.addEventListener('DOMContentLoaded', async () => {
    if (state.token) {
        try {
            await loadUserProfile();
            document.getElementById('auth-page').style.display = 'none';
            document.getElementById('app').classList.remove('hidden');
            renderApp();
        } catch (e) {
            logout();
        }
    } else {
        document.getElementById('auth-page').style.display = 'flex';
    }
});
}