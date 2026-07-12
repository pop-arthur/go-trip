// ============================================================
//  TOAST NOTIFICATIONS
// ============================================================
function showToast(msg, type = 'success') {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();
    const toast = document.createElement('div');
    toast.className = 'toast';
    const colors = {
        success: '#2a4a5a',
        error: '#5a2a3a',
        info: '#3a4a5a'
    };
    toast.style.cssText = `
        position: fixed; bottom: 24px; right: 24px; padding: 14px 24px;
        background: ${colors[type] || colors.info};
        border: 1px solid ${type === 'error' ? '#8a5a6a' : '#4a6a7a'};
        border-radius: 12px; color: #f0eef7; font-size: 15px;
        z-index: 9999; max-width: 400px;
        box-shadow: 0 8px 24px rgba(0,0,0,0.2);
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
    localStorage.removeItem('notificationEnabled');
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
        case 'statistics': renderStatistics(container); break;
        case 'recommendations': renderRecommendations(container); break;
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

    const saved = localStorage.getItem('notificationEnabled');
    if (saved !== null) {
        state.notificationEnabled = saved === 'true';
        updateToggleFromState();
    }

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
                    localStorage.setItem('notificationEnabled', pref.is_enabled);
                    updateToggleFromState();
                }
            })
            .catch(err => {
                showToast('Не удалось загрузить настройки уведомлений: ' + err.message, 'error');
                statusEl.innerHTML = '<div class="error-state">Ошибка загрузки настроек</div>';
            });
    }

    loadPreferences();

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
                    localStorage.setItem('notificationEnabled', enabled);
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
                    <span style="font-size:12px;color:#6b5a90;">${n.is_read ? 'Прочитано' : 'Новое'}</span>
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

    Promise.all([
        apiFetch('/achievements').then(resp => resp.json()),
        apiFetch('/users/me/achievements').then(resp => resp.json())
    ]).then(([allAchievements, userAchievements]) => {
        const achievementsMap = Object.fromEntries(allAchievements.map(a => [a.id, a]));

        const uniqueUserAchievements = [];
        const seen = new Set();
        if (userAchievements) {
            const sorted = [...userAchievements].sort((a, b) => {
                const dateA = new Date(a.unlocked_at ?? a.unlockedAt);
                const dateB = new Date(b.unlocked_at ?? b.unlockedAt);
                return dateB - dateA;
            });
            for (const ua of sorted) {
                const achId = ua.achievement_id ?? ua.achievementId;
                if (!seen.has(achId)) {
                    seen.add(achId);
                    uniqueUserAchievements.push(ua);
                }
            }
        }

        const myEl = document.getElementById('my-achievements');
        if (!uniqueUserAchievements || uniqueUserAchievements.length === 0) {
            myEl.innerHTML = '<div class="empty-state">Вы ещё не получили ни одного достижения</div>';
        } else {
            myEl.innerHTML = uniqueUserAchievements.map(ua => {
                const achId = ua.achievement_id ?? ua.achievementId;
                const ach = achievementsMap[achId];
                const unlockedDate = ua.unlocked_at ?? ua.unlockedAt;
                const dateStr = unlockedDate ? new Date(unlockedDate).toLocaleString() : 'неизвестно';
                return `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${ach ? ach.title : 'Достижение'}</div>
                            <div class="sub">${ach?.description || ''} ${ach ? '— получено ' + dateStr : ''}</div>
                        </div>
                    </div>
                `;
            }).join('');
        }

        const allEl = document.getElementById('all-achievements');
        if (!allAchievements?.length) {
            allEl.innerHTML = '<div class="empty-state">Нет доступных достижений</div>';
        } else {
            allEl.innerHTML = allAchievements.map(a => `
                <div class="list-item">
                    <div class="info">
                        <div class="title">${a.title}</div>
                        <div class="sub">${a.description || ''} (${a.condition_type ?? a.conditionType}: ${a.condition_value ?? a.conditionValue})</div>
                    </div>
                </div>
            `).join('');
        }
    }).catch(() => {
        document.getElementById('my-achievements').innerHTML = '<div class="error-state">Ошибка загрузки</div>';
        document.getElementById('all-achievements').innerHTML = '<div class="error-state">Ошибка загрузки</div>';
    });
}

// ============================================================
//  REVIEWS
// ============================================================
let providersMap = {};
let locationsMap = {};
let servicesMap = {};

async function loadReviewMaps() {
    try {
        const [provResp, locResp, servResp] = await Promise.all([
            apiFetch('/providers'),
            apiFetch('/locations'),
            apiFetch('/additional-services')
        ]);
        if (provResp.ok) {
            const data = await provResp.json();
            providersMap = {};
            data.forEach(item => providersMap[item.id] = item.name);
        }
        if (locResp.ok) {
            const data = await locResp.json();
            locationsMap = {};
            data.forEach(item => locationsMap[item.id] = item.name);
        }
        if (servResp.ok) {
            const data = await servResp.json();
            servicesMap = {};
            data.forEach(item => servicesMap[item.id] = item.title);
        }
    } catch (e) {
        console.error('Ошибка загрузки справочников для отзывов', e);
    }
}

function getTargetName(targetType, targetId) {
    if (!targetType || !targetId) return 'неизвестно';
    const type = targetType.toUpperCase();
    switch (type) {
        case 'PROVIDER':
            return providersMap[targetId] || `Провайдер #${targetId.slice(0, 8)}`;
        case 'LOCATION':
            return locationsMap[targetId] || `Локация #${targetId.slice(0, 8)}`;
        case 'ADDITIONAL_SERVICE':
            return servicesMap[targetId] || `Услуга #${targetId.slice(0, 8)}`;
        case 'ORDER':
            return `Заказ #${targetId.slice(0, 8)}`;
        default:
            return targetId.slice(0, 8);
    }
}

function getUserLabel(userId, currentUserId) {
    if (!userId) return 'неизвестно';
    if (userId === currentUserId) return 'Вы';
    return `#${userId.slice(0, 8)}`;
}

function renderReviews(container) {
    loadReviewMaps();

    let viewMode = 'all';

    container.innerHTML = `
        <div class="section active">
            <h2>Отзывы</h2>
            <div class="card">
                <h3>Создать отзыв</h3>
                <div class="form-row">
                    <label>Тип цели 
                        <select id="review-target-type">
                            <option value="PROVIDER">Провайдер</option>
                            <option value="LOCATION">Локация</option>
                            <option value="ORDER">Заказ</option>
                            <option value="ADDITIONAL_SERVICE">Доп. услуга</option>
                        </select>
                    </label>
                    <label>Цель 
                        <select id="review-target-id">
                            <option value="">-- Сначала выберите тип --</option>
                        </select>
                    </label>
                    <div id="order-trip-select-wrapper" style="display:none;">
                        <label>Поездка 
                            <select id="review-trip-select">
                                <option value="">-- Выберите поездку --</option>
                            </select>
                        </label>
                    </div>
                </div>
                <div class="form-row">
                    <label>Оценка (1-5) <input type="number" id="review-rating" min="1" max="5" /></label>
                    <label>Текст <textarea id="review-text"></textarea></label>
                </div>
                <button class="btn btn-primary" id="review-create-btn">Оставить отзыв</button>
            </div>
            <div class="card">
                <h3>Список отзывов</h3>
                <div class="form-row" style="margin-bottom:12px;">
                    <button class="btn btn-sm btn-outline review-tab" data-view="all" style="background: var(--color-primary);">Все</button>
                    <button class="btn btn-sm btn-outline review-tab" data-view="mine">Мои</button>
                </div>
                <div class="form-row">
                    <label>Тип цели <select id="review-filter-type">
                        <option value="">Все</option>
                        <option value="PROVIDER">Провайдеры</option>
                        <option value="LOCATION">Локации</option>
                        <option value="ORDER">Заказы</option>
                        <option value="ADDITIONAL_SERVICE">Доп. услуги</option>
                    </select></label>
                    <label>Цель <select id="review-filter-target">
                        <option value="">-- Все --</option>
                    </select></label>
                    <button class="btn" id="review-filter-btn">Поиск</button>
                </div>
                <div id="review-list"></div>
            </div>
        </div>
    `;

    const targetTypeSelect = document.getElementById('review-target-type');
    const targetIdSelect = document.getElementById('review-target-id');
    const tripSelectWrapper = document.getElementById('order-trip-select-wrapper');
    const tripSelect = document.getElementById('review-trip-select');
    const filterTypeSelect = document.getElementById('review-filter-type');
    const filterTargetSelect = document.getElementById('review-filter-target');
    const filterBtn = document.getElementById('review-filter-btn');
    const createBtn = document.getElementById('review-create-btn');

    let filterType = '';
    let filterTargetId = '';
    let filterTargetOptions = [];

    function loadTargets(targetType) {
        const select = targetIdSelect;
        select.innerHTML = '<option value="">Загрузка...</option>';
        select.disabled = true;
        tripSelectWrapper.style.display = 'none';
        tripSelect.innerHTML = '<option value="">-- Выберите поездку --</option>';

        let url = '';
        let labelField = 'name';
        let idField = 'id';

        switch (targetType) {
            case 'PROVIDER':
                url = '/providers';
                labelField = 'name';
                break;
            case 'LOCATION':
                url = '/locations';
                labelField = 'name';
                break;
            case 'ADDITIONAL_SERVICE':
                url = '/additional-services';
                labelField = 'title';
                break;
            case 'ORDER':
                loadTripsForOrder();
                return;
            default:
                select.innerHTML = '<option value="">-- Сначала выберите тип --</option>';
                select.disabled = true;
                return;
        }

        apiFetch(url)
            .then(resp => {
                if (!resp.ok) throw new Error(`Ошибка загрузки: ${resp.status}`);
                return resp.json();
            })
            .then(list => {
                if (!list || list.length === 0) {
                    select.innerHTML = '<option value="">Нет доступных объектов</option>';
                    select.disabled = true;
                    return;
                }
                select.innerHTML = '<option value="">Выберите цель</option>';
                list.forEach(item => {
                    const option = document.createElement('option');
                    option.value = String(item[idField]);
                    option.textContent = item[labelField] || item[idField];
                    select.appendChild(option);
                });
                select.disabled = false;
            })
            .catch(err => {
                select.innerHTML = `<option value="">Ошибка: ${err.message}</option>`;
                select.disabled = true;
                showToast('Не удалось загрузить цели: ' + err.message, 'error');
            });
    }

    function loadTripsForOrder() {
        const select = targetIdSelect;
        select.innerHTML = '<option value="">Сначала выберите поездку</option>';
        select.disabled = true;
        tripSelectWrapper.style.display = 'block';
        tripSelect.innerHTML = '<option value="">Загрузка поездок...</option>';
        tripSelect.disabled = true;

        apiFetch('/trips')
            .then(resp => resp.json())
            .then(trips => {
                if (!trips || trips.length === 0) {
                    tripSelect.innerHTML = '<option value="">Нет поездок</option>';
                    tripSelect.disabled = true;
                    return;
                }
                tripSelect.innerHTML = '<option value="">Выберите поездку</option>';
                trips.forEach(t => {
                    const opt = document.createElement('option');
                    opt.value = String(t.id);
                    opt.textContent = t.title || t.id;
                    tripSelect.appendChild(opt);
                });
                tripSelect.disabled = false;
                tripSelect.onchange = function() {
                    const tripId = tripSelect.value;
                    if (!tripId) {
                        select.innerHTML = '<option value="">-- Выберите поездку сначала --</option>';
                        select.disabled = true;
                        return;
                    }
                    loadOrdersForTrip(tripId);
                };
                if (tripSelect.value) {
                    loadOrdersForTrip(tripSelect.value);
                }
            })
            .catch(err => {
                tripSelect.innerHTML = `<option value="">Ошибка: ${err.message}</option>`;
                tripSelect.disabled = true;
                showToast('Не удалось загрузить поездки', 'error');
            });
    }

    function loadOrdersForTrip(tripId) {
        const select = targetIdSelect;
        select.innerHTML = '<option value="">Загрузка заказов...</option>';
        select.disabled = true;

        apiFetch(`/trips/${tripId}/orders`)
            .then(resp => resp.json())
            .then(orders => {
                if (!orders || orders.length === 0) {
                    select.innerHTML = '<option value="">Нет заказов в этой поездке</option>';
                    select.disabled = true;
                    return;
                }
                select.innerHTML = '<option value="">Выберите заказ</option>';
                orders.forEach(o => {
                    const opt = document.createElement('option');
                    opt.value = String(o.id);
                    opt.textContent = o.title || o.id;
                    select.appendChild(opt);
                });
                select.disabled = false;
            })
            .catch(err => {
                select.innerHTML = `<option value="">Ошибка: ${err.message}</option>`;
                select.disabled = true;
                showToast('Не удалось загрузить заказы', 'error');
            });
    }

    function loadFilterTargets(targetType) {
        const select = filterTargetSelect;
        select.innerHTML = '<option value="">Загрузка...</option>';
        select.disabled = true;
        filterTargetId = '';
        filterTargetOptions = [];

        let url = '';
        let labelField = 'name';
        let idField = 'id';

        switch (targetType) {
            case 'PROVIDER':
                url = '/providers';
                labelField = 'name';
                break;
            case 'LOCATION':
                url = '/locations';
                labelField = 'name';
                break;
            case 'ADDITIONAL_SERVICE':
                url = '/additional-services';
                labelField = 'title';
                break;
            case 'ORDER':
                select.innerHTML = '<option value="">Введите ID заказа</option>';
                select.disabled = false;
                const inputId = document.createElement('input');
                inputId.type = 'text';
                inputId.placeholder = 'ID заказа';
                inputId.id = 'review-filter-order-id';
                inputId.style.marginLeft = '8px';
                select.parentNode.insertBefore(inputId, select.nextSibling);
                select.style.display = 'none';
                inputId.addEventListener('input', function() {
                    filterTargetId = this.value;
                });
                return;
            default:
                select.innerHTML = '<option value="">-- Все --</option>';
                select.disabled = false;
                return;
        }

        apiFetch(url)
            .then(resp => {
                if (!resp.ok) throw new Error(`Ошибка загрузки: ${resp.status}`);
                return resp.json();
            })
            .then(list => {
                if (!list || list.length === 0) {
                    select.innerHTML = '<option value="">Нет доступных объектов</option>';
                    select.disabled = true;
                    return;
                }
                const allOption = document.createElement('option');
                allOption.value = '';
                allOption.textContent = `Все ${targetType.toLowerCase()}`;
                select.innerHTML = '';
                select.appendChild(allOption);
                list.forEach(item => {
                    const option = document.createElement('option');
                    option.value = String(item[idField]);
                    option.textContent = item[labelField] || item[idField];
                    select.appendChild(option);
                });
                filterTargetOptions = list.map(item => ({ value: item[idField], label: item[labelField] || item[idField] }));
                select.disabled = false;
                const inputOrder = document.getElementById('review-filter-order-id');
                if (inputOrder) inputOrder.remove();
                select.style.display = '';
            })
            .catch(err => {
                select.innerHTML = `<option value="">Ошибка: ${err.message}</option>`;
                select.disabled = true;
                showToast('Не удалось загрузить цели для фильтра', 'error');
            });
    }

    filterTypeSelect.addEventListener('change', function() {
        const type = this.value;
        filterType = type;
        const oldInput = document.getElementById('review-filter-order-id');
        if (oldInput) oldInput.remove();
        const select = filterTargetSelect;
        select.style.display = '';
        if (type) {
            loadFilterTargets(type);
        } else {
            select.innerHTML = '<option value="">-- Все --</option>';
            select.disabled = false;
            filterTargetId = '';
        }
        loadReviews();
    });

    filterTargetSelect.addEventListener('change', function() {
        filterTargetId = this.value;
        loadReviews();
    });

    targetTypeSelect.addEventListener('change', () => {
        const type = targetTypeSelect.value;
        if (type) {
            loadTargets(type);
        } else {
            targetIdSelect.innerHTML = '<option value="">-- Сначала выберите тип --</option>';
            targetIdSelect.disabled = true;
            tripSelectWrapper.style.display = 'none';
        }
    });

    let currentView = 'all';
    const tabs = container.querySelectorAll('.review-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            tabs.forEach(t => t.style.background = '');
            this.style.background = 'var(--color-primary)';
            currentView = this.dataset.view;
            loadReviews();
        });
    });
    tabs[0].style.background = 'var(--color-primary)';

    async function loadReviews() {
        const listEl = document.getElementById('review-list');
        if (!listEl) return;
        listEl.innerHTML = '<p>Загрузка...</p>';

        try {
            const params = new URLSearchParams();
            if (filterType) {
                params.append('targetType', filterType);
                if (filterTargetId) {
                    params.append('targetId', filterTargetId);
                }
            }
            const resp = await apiFetch(`/reviews?${params.toString()}`);
            if (!resp.ok) {
                listEl.innerHTML = '<div class="error-state">Ошибка загрузки отзывов</div>';
                return;
            }
            let data = await resp.json();
            if (currentView === 'mine') {
                data = data.filter(r => r.userId === state.user?.id);
            }
            if (!data || data.length === 0) {
                listEl.innerHTML = '<div class="empty-state">Нет отзывов</div>';
                return;
            }
            let html = '';
            data.forEach(r => {
                const targetName = getTargetName(r.targetType, r.targetId);
                const userLabel = getUserLabel(r.userId, state.user?.id);
                const text = r.text || '(без текста)';
                html += `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">
                                <span style="color: #f5b342;">⭐</span> ${r.rating}/5 — ${targetName}
                                <button class="btn btn-sm btn-outline" style="margin-left:8px;" onclick="viewRating('${r.targetType}', '${r.targetId}')">ℹ️</button>
                            </div>
                            <div class="sub">${text}</div>
                            <div class="sub" style="font-size:12px;color:var(--color-text-secondary);">
                                От пользователя: ${userLabel}
                            </div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="editReview('${r.id}')">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteReview('${r.id}')">✕</button>
                        </div>
                    </div>
                `;
            });
            listEl.innerHTML = html;
        } catch (e) {
            listEl.innerHTML = '<div class="error-state">Ошибка загрузки отзывов</div>';
            showToast('Ошибка загрузки отзывов', 'error');
        }
    }

    window.viewRating = async (targetType, targetId) => {
        try {
            const resp = await apiFetch(`/reviews/rating-summary?targetType=${targetType.toUpperCase()}&targetId=${targetId}`);
            if (!resp.ok) {
                showToast('Не удалось загрузить рейтинг', 'error');
                return;
            }
            const data = await resp.json();
            const name = getTargetName(targetType, targetId);
            alert(`Цель: ${name}\nСредняя оценка: ${data.averageRating ? data.averageRating.toFixed(1) : 'нет оценок'}\nКоличество отзывов: ${data.reviewCount || 0}`);
        } catch (e) {
            showToast('Ошибка загрузки рейтинга', 'error');
        }
    };

    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const targetType = targetTypeSelect.value;
            const targetId = targetIdSelect.value;
            const rating = parseInt(document.getElementById('review-rating').value);
            const text = document.getElementById('review-text').value;

            if (!targetType || !targetId) {
                showToast('Выберите тип и цель', 'error');
                return;
            }
            if (!rating || rating < 1 || rating > 5) {
                showToast('Введите оценку от 1 до 5', 'error');
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
                    document.getElementById('review-rating').value = '';
                    document.getElementById('review-text').value = '';
                    targetIdSelect.value = '';
                    filterTypeSelect.value = '';
                    filterTargetSelect.innerHTML = '<option value="">-- Все --</option>';
                    filterTargetSelect.disabled = false;
                    filterType = '';
                    filterTargetId = '';
                    loadReviews();
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + (err.message || ''), 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });
    }

    if (filterBtn) {
        filterBtn.addEventListener('click', loadReviews);
    }

    loadTargets(targetTypeSelect.value);
    filterTypeSelect.value = '';
    filterTargetSelect.innerHTML = '<option value="">-- Все --</option>';
    filterTargetSelect.disabled = false;
    loadReviews();

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
                        <option value="COUNTRY">Страна</option>
                        <option value="CITY">Город</option>
                        <option value="AIRPORT">Аэропорт</option>
                        <option value="TRAIN_STATION">Вокзал</option>
                        <option value="BUS_STATION">Автовокзал</option>
                        <option value="PORT">Порт</option>
                        <option value="HOTEL">Отель</option>
                        <option value="MEETING_POINT">Место сбора</option>
                        <option value="ATTRACTION">Достопримечательность</option>
                        <option value="OTHER">Другое</option>
                    </select></label>
                </div>
                <div class="form-row">
                    <label>Страна <input id="loc-country" /></label>
                    <label>Город <input id="loc-city" /></label>
                    <label>Адрес <input id="loc-address" /></label>
                </div>
                <button class="btn btn-primary" id="loc-create-btn">Создать</button>
            </div>
            <div class="card">
                <h3>Поиск локаций</h3>
                <div class="form-row">
                    <label>Тип <select id="loc-filter-type"><option value="">Все</option>
                        <option value="COUNTRY">Страна</option><option value="CITY">Город</option>
                        <option value="AIRPORT">Аэропорт</option><option value="TRAIN_STATION">Вокзал</option>
                        <option value="BUS_STATION">Автовокзал</option><option value="PORT">Порт</option>
                        <option value="HOTEL">Отель</option><option value="MEETING_POINT">Место сбора</option>
                        <option value="ATTRACTION">Достопримечательность</option><option value="OTHER">Другое</option>
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
            if (!name) { showToast('Название обязательно', 'error'); return; }
            try {
                const resp = await apiFetch('/locations', {
                    method: 'POST',
                    body: JSON.stringify({ name, type, country, city, address })
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
                            <button class="btn btn-sm btn-outline" onclick="editLocation('${l.id}')">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteLocation('${l.id}')">✕</button>
                            <button class="btn btn-sm btn-outline" onclick="viewLocation('${l.id}')">👁</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка загрузки', 'error'); }
    }

    window.viewLocation = async (id) => {
        try {
            const resp = await apiFetch(`/locations/${id}`);
            if (!resp.ok) { showToast('Не удалось загрузить локацию', 'error'); return; }
            const loc = await resp.json();
            const ratingResp = await apiFetch(`/reviews/rating-summary?targetType=LOCATION&targetId=${id}`);
            let rating = null;
            if (ratingResp.ok) {
                rating = await ratingResp.json();
            }
            alert(
                `ID: ${loc.id}\n` +
                `Название: ${loc.name}\n` +
                `Тип: ${loc.type}\n` +
                `Страна: ${loc.country || '—'}\n` +
                `Город: ${loc.city || '—'}\n` +
                `Адрес: ${loc.address || '—'}\n` +
                `Средняя оценка: ${rating ? (rating.averageRating ? rating.averageRating.toFixed(1) : 'нет оценок') : 'нет оценок'}\n` +
                `Количество отзывов: ${rating ? rating.reviewCount || 0 : 0}`
            );
        } catch (e) {
            showToast('Ошибка загрузки', 'error');
        }
    };

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
                        <option value="AIRLINE">Авиакомпания</option>
                        <option value="HOTEL">Отель</option>
                        <option value="TOUR_COMPANY">Туроператор</option>
                        <option value="TRANSPORT_COMPANY">Транспортная компания</option>
                        <option value="BOOKING_PLATFORM">Платформа бронирования</option>
                        <option value="INSURANCE_COMPANY">Страховая компания</option>
                        <option value="OTHER">Другое</option>
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
                        <option value="AIRLINE">Авиакомпания</option>
                        <option value="HOTEL">Отель</option>
                        <option value="TOUR_COMPANY">Туроператор</option>
                        <option value="TRANSPORT_COMPANY">Транспортная компания</option>
                        <option value="BOOKING_PLATFORM">Платформа бронирования</option>
                        <option value="INSURANCE_COMPANY">Страховая компания</option>
                        <option value="OTHER">Другое</option>
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
                            <button class="btn btn-sm btn-danger" onclick="deleteProvider('${p.id}')">✕</button>
                            <button class="btn btn-sm btn-outline" onclick="viewProvider('${p.id}')">👁</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка', 'error'); }
    }

    window.viewProvider = async (id) => {
        try {
            const resp = await apiFetch(`/providers/${id}`);
            if (!resp.ok) { showToast('Не удалось загрузить провайдера', 'error'); return; }
            const prov = await resp.json();
            const ratingResp = await apiFetch(`/reviews/rating-summary?targetType=PROVIDER&targetId=${id}`);
            let rating = null;
            if (ratingResp.ok) {
                rating = await ratingResp.json();
            }
            alert(
                `ID: ${prov.id}\n` +
                `Название: ${prov.name}\n` +
                `Тип: ${prov.type}\n` +
                `Сайт: ${prov.website || '—'}\n` +
                `Контакты: ${prov.support_contact || '—'}\n` +
                `Средняя оценка: ${rating ? (rating.averageRating ? rating.averageRating.toFixed(1) : 'нет оценок') : 'нет оценок'}\n` +
                `Количество отзывов: ${rating ? rating.reviewCount || 0 : 0}`
            );
        } catch (e) {
            showToast('Ошибка загрузки', 'error');
        }
    };

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
                        <option value="FLIGHT">Авиа</option>
                        <option value="TRAIN">Поезд</option>
                        <option value="BUS">Автобус</option>
                        <option value="HOTEL">Отель</option>
                        <option value="TOUR">Тур</option>
                        <option value="CAR_RENTAL">Аренда авто</option>
                        <option value="INSURANCE">Страховка</option>
                        <option value="TAXI">Такси</option>
                        <option value="ESIM">eSIM</option>
                        <option value="LOUNGE">Лаунж</option>
                        <option value="EXTRA_BAGGAGE">Доп. багаж</option>
                        <option value="OTHER">Другое</option>
                    </select></label>
                    <label>ID локации <input id="svc-filter-loc" type="text" placeholder="ID локации" /></label>
                    <label>ID провайдера <input id="svc-filter-prov" type="text" placeholder="ID провайдера" /></label>
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
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="viewService('${s.id}')">👁</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) { showToast('Ошибка', 'error'); }
    }

    window.viewService = async (id) => {
        try {
            const resp = await apiFetch(`/additional-services/${id}`);
            if (!resp.ok) { showToast('Не удалось загрузить услугу', 'error'); return; }
            const serv = await resp.json();
            const ratingResp = await apiFetch(`/reviews/rating-summary?targetType=ADDITIONAL_SERVICE&targetId=${id}`);
            let rating = null;
            if (ratingResp.ok) {
                rating = await ratingResp.json();
            }
            alert(
                `ID: ${serv.id}\n` +
                `Название: ${serv.title}\n` +
                `Тип: ${serv.service_type}\n` +
                `Описание: ${serv.description || '—'}\n` +
                `Цена: ${serv.price_amount ? serv.price_amount + ' ' + serv.price_currency : '—'}\n` +
                `Активна: ${serv.is_active ? 'Да' : 'Нет'}\n` +
                `Средняя оценка: ${rating ? (rating.averageRating ? rating.averageRating.toFixed(1) : 'нет оценок') : 'нет оценок'}\n` +
                `Количество отзывов: ${rating ? rating.reviewCount || 0 : 0}`
            );
        } catch (e) {
            showToast('Ошибка загрузки', 'error');
        }
    };
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
                            <button class="btn btn-sm btn-outline" onclick="viewTrip('${t.id}')">📋</button>
                            <button class="btn btn-sm btn-outline" onclick="editTrip('${t.id}')">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteTrip('${t.id}')">✕</button>
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
                <h3>Маршрут</h3>
                <button class="btn btn-primary" id="add-location-btn" style="margin-bottom:16px;">+ Добавить локацию</button>
                <div id="trip-location-list"></div>
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

    async function loadAll() {
        try {
            const tripResp = await apiFetch(`/trips/${tripId}`);
            if (tripResp.ok) {
                const trip = await tripResp.json();
                document.getElementById('trip-detail-title').textContent = `Поездка: ${trip.title}`;
                document.getElementById('trip-info').innerHTML = `
                    <p><strong>ID:</strong> ${trip.id}</p>
                    <p><strong>Название:</strong> ${trip.title}</p>
                    <p><strong>Дата начала:</strong> ${trip.start_date || '—'}</p>
                    <p><strong>Дата окончания:</strong> ${trip.end_date || '—'}</p>
                    <p><strong>Статус:</strong> ${trip.status}</p>
                `;
            }
        } catch (e) { showToast('Ошибка загрузки поездки', 'error'); }

        loadTripLocations();
        loadOrders();
    }

    async function loadTripLocations() {
        const el = document.getElementById('trip-location-list');
        if (!el) return;
        try {
            const resp = await apiFetch(`/trips/${tripId}/locations`);
            if (resp.ok) {
                const locations = await resp.json();
                if (!locations || locations.length === 0) {
                    el.innerHTML = '<div class="empty-state">Нет локаций в маршруте</div>';
                    return;
                }
                const locResp = await apiFetch('/locations');
                const allLocations = locResp.ok ? await locResp.json() : [];
                const locMap = {};
                allLocations.forEach(l => locMap[l.id] = l.name);

                el.innerHTML = locations.map(l => `
                    <div class="list-item">
                        <div class="info">
                            <div class="title">${locMap[l.location_id] || l.location_id.slice(0,8)} (порядок: ${l.visit_order})</div>
                            <div class="sub">Прибытие: ${l.arrival_date || '—'} / Отбытие: ${l.departure_date || '—'}</div>
                        </div>
                        <div class="actions">
                            <button class="btn btn-sm btn-outline" onclick="editTripLocation('${l.id}')">✎</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteTripLocation('${l.id}')">✕</button>
                        </div>
                    </div>
                `).join('');
            } else {
                el.innerHTML = '<div class="error-state">Ошибка загрузки маршрута</div>';
            }
        } catch (e) {
            el.innerHTML = '<div class="error-state">Ошибка загрузки маршрута</div>';
        }
    }

    window.editTripLocation = async (locationId) => {
        try {
            const resp = await apiFetch(`/trips/${tripId}/locations/${locationId}`);
            if (!resp.ok) { showToast('Не удалось загрузить локацию', 'error'); return; }
            const loc = await resp.json();

            const visitOrderInput = prompt('Новый порядок (число > 0):', loc.visit_order);
            if (visitOrderInput === null) return;
            const visitOrder = parseInt(visitOrderInput, 10);
            if (isNaN(visitOrder) || visitOrder < 1) {
                showToast('Порядок должен быть положительным числом', 'error');
                return;
            }

            // Проверяем, занят ли новый порядок
            const locationsResp = await apiFetch(`/trips/${tripId}/locations`);
            const allLocations = locationsResp.ok ? await locationsResp.json() : [];
            const conflicting = allLocations.find(l => l.id !== locationId && l.visit_order === visitOrder);

            let arrivalDate = prompt('Дата прибытия (ISO, опционально):', loc.arrival_date || '');
            let departureDate = prompt('Дата отбытия (ISO, опционально):', loc.departure_date || '');

            if (conflicting) {
                // Меняем местами
                const maxOrder = allLocations.reduce((max, l) => Math.max(max, l.visit_order), 0);
                const tempOrder = maxOrder + 1;

                // Шаг 1: текущая на временный
                const r1 = await apiFetch(`/trips/${tripId}/locations/${locationId}`, {
                    method: 'PATCH',
                    body: JSON.stringify({ visit_order: tempOrder })
                });
                if (!r1.ok) throw new Error('Не удалось установить временный порядок');

                // Шаг 2: конфликтующая на старый
                const r2 = await apiFetch(`/trips/${tripId}/locations/${conflicting.id}`, {
                    method: 'PATCH',
                    body: JSON.stringify({ visit_order: loc.visit_order })
                });
                if (!r2.ok) throw new Error('Не удалось обновить конфликтующую локацию');

                // Шаг 3: текущая на новый
                const r3 = await apiFetch(`/trips/${tripId}/locations/${locationId}`, {
                    method: 'PATCH',
                    body: JSON.stringify({ visit_order: visitOrder })
                });
                if (!r3.ok) throw new Error('Не удалось установить новый порядок');

                showToast('Порядки успешно обменяны');
            } else {
                // Просто обновляем
                const r = await apiFetch(`/trips/${tripId}/locations/${locationId}`, {
                    method: 'PATCH',
                    body: JSON.stringify({
                        visit_order: visitOrder,
                        arrival_date: arrivalDate || null,
                        departure_date: departureDate || null
                    })
                });
                if (!r.ok) throw new Error('Ошибка обновления');
                showToast('Локация обновлена');
            }

            loadTripLocations();
        } catch (e) {
            showToast('Ошибка: ' + e.message, 'error');
            console.error(e);
        }
    };

    window.deleteTripLocation = async (locationId) => {
        if (!confirm('Удалить локацию из маршрута?')) return;
        try {
            const resp = await apiFetch(`/trips/${tripId}/locations/${locationId}`, { method: 'DELETE' });
            if (resp.ok) {
                showToast('Локация удалена');
                loadTripLocations();
            } else {
                const err = await resp.json();
                showToast('Ошибка: ' + (err.message || ''), 'error');
            }
        } catch (e) {
            showToast('Ошибка', 'error');
        }
    };

    document.getElementById('add-location-btn').addEventListener('click', async () => {
        const locationId = prompt('Введите ID локации:');
        if (!locationId) return;
        const visitOrder = prompt('Порядок (число):') || 1;
        try {
            const resp = await apiFetch(`/trips/${tripId}/locations`, {
                method: 'POST',
                body: JSON.stringify({
                    location_id: locationId,
                    visit_order: parseInt(visitOrder)
                })
            });
            if (resp.ok) {
                showToast('Локация добавлена');
                loadTripLocations();
            } else {
                const err = await resp.json();
                showToast('Ошибка: ' + (err.message || ''), 'error');
            }
        } catch (e) {
            showToast('Ошибка', 'error');
        }
    });

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
                            <button class="btn btn-sm btn-outline" onclick="viewOrderFiles('${o.id}')">📁</button>
                            <button class="btn btn-sm btn-outline" onclick="editOrder('${o.id}')">✎</button>
                            <button class="btn btn-sm btn-outline" onclick="changeOrderStatus('${o.id}')">🔄</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteOrder('${o.id}')">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) {
            el.innerHTML = '<div class="error-state">Ошибка загрузки заказов</div>';
        }
    };

    window.viewOrderFiles = (orderId) => {
        state.orderDetailId = orderId;
        renderOrderFiles(container);
    };

    window.loadOrders();
    loadAll();

    document.getElementById('order-create-btn').addEventListener('click', () => {
        showOrderCreateForm(tripId, container);
    });
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
                        <option value="IMAGE">Изображение</option>
                        <option value="EMAIL">Email</option>
                        <option value="JSON">JSON</option>
                        <option value="OTHER">Другое</option>
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
                            <button class="btn btn-sm btn-outline" onclick="viewFile('${f.file_url}')">👁</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteFile('${f.id}')">✕</button>
                        </div>
                    </div>
                `).join('');
            }
        } catch (e) {
            el.innerHTML = '<div class="error-state">Ошибка загрузки файлов</div>';
        }
    }

    window.viewFile = (fileUrl) => {
        window.open(API_BASE + '/' + fileUrl, '_blank');
    };

    window.deleteFile = async (fileId) => {
        if (!confirm('Удалить файл?')) return;
        try {
            const resp = await apiFetch(`/orders/${orderId}/files/${fileId}`, { method: 'DELETE' });
            if (resp.ok) { showToast('Удалено'); loadFiles(); }
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
                <option value="FLIGHT">Авиа</option>
                <option value="TRAIN">Поезд</option>
                <option value="BUS">Автобус</option>
                <option value="HOTEL">Отель</option>
                <option value="TOUR">Тур</option>
                <option value="CAR_RENTAL">Аренда авто</option>
                <option value="INSURANCE">Страховка</option>
                <option value="TAXI">Такси</option>
                <option value="ESIM">eSIM</option>
                <option value="LOUNGE">Лаунж</option>
                <option value="EXTRA_BAGGAGE">Доп. багаж</option>
                <option value="OTHER">Другое</option>
            </select></label>
            <label>Название <input id="order-title" /></label>
        </div>
        <div class="form-row">
            <label>Статус <select id="order-status">
                <option value="PENDING_VERIFICATION">Ожидает проверки</option>
                <option value="CONFIRMED">Подтверждён</option>
                <option value="DELAYED">Задержан</option>
                <option value="CANCELLED">Отменён</option>
                <option value="COMPLETED">Завершён</option>
                <option value="REFUND_PENDING">Возврат средств</option>
                <option value="REFUNDED">Возвращён</option>
            </select></label>
            <label>Цена <input id="order-price" type="number" step="any" /></label>
            <label>Валюта <input id="order-currency" /></label>
        </div>
        <div class="form-row">
            <label>Провайдер <select id="order-provider"><option value="">Выберите провайдера</option></select></label>
            <label>Внешний ID <input id="order-ext-id" /></label>
        </div>
        <div class="form-row">
            <label>Начало <input id="order-start" type="datetime-local" /></label>
            <label>Конец <input id="order-end" type="datetime-local" /></label>
        </div>
        <div class="form-row">
            <label>Локация отправления <select id="order-dep-loc"><option value="">Выберите локацию</option></select></label>
            <label>Локация прибытия <select id="order-arr-loc"><option value="">Выберите локацию</option></select></label>
        </div>
        <button class="btn btn-primary" id="order-save-btn">Сохранить</button>
        <button class="btn" id="order-cancel-btn">Отмена</button>
    `;
    const listContainer = document.getElementById('order-list');
    if (!listContainer) return;
    listContainer.parentNode.insertBefore(card, listContainer);

    function populateSelect(url, selectId, labelField = 'name', idField = 'id') {
        const select = document.getElementById(selectId);
        apiFetch(url)
            .then(resp => resp.json())
            .then(list => {
                select.innerHTML = '<option value="">Выберите</option>';
                list.forEach(item => {
                    const opt = document.createElement('option');
                    opt.value = String(item[idField]);
                    opt.textContent = item[labelField] || item[idField];
                    select.appendChild(opt);
                });
            })
            .catch(() => showToast('Ошибка загрузки данных', 'error'));
    }
    populateSelect('/providers', 'order-provider', 'name');
    populateSelect('/locations', 'order-dep-loc', 'name');
    populateSelect('/locations', 'order-arr-loc', 'name');

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
                provider_id: providerId || null,
                start_datetime: start ? new Date(start).toISOString() : null,
                end_datetime: end ? new Date(end).toISOString() : null,
                departure_location_id: depLoc || null,
                arrival_location_id: arrLoc || null
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
//  STATISTICS
// ============================================================
function renderStatistics(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Статистика</h2>
            <div class="card">
                <h3>Страны</h3>
                <div id="stat-countries"></div>
            </div>
            <div class="card">
                <h3>Затраты по месяцам</h3>
                <div id="stat-spending"></div>
            </div>
            <div class="card">
                <h3>Предстоящие поездки</h3>
                <div id="stat-upcoming"></div>
            </div>
            <div class="card">
                <h3>Длительность поездок</h3>
                <div id="stat-durations"></div>
            </div>
        </div>
    `;

    function getEl(id) {
        const el = document.getElementById(id);
        if (!el) console.warn(`Element ${id} not found`);
        return el;
    }

    function extractItems(data) {
        if (Array.isArray(data)) return data;
        if (data && data.items && Array.isArray(data.items)) return data.items;
        if (data && data.data && Array.isArray(data.data)) return data.data;
        return [];
    }

    apiFetch('/statistics/countries')
        .then(resp => {
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return resp.json();
        })
        .then(data => {
            const el = getEl('stat-countries');
            if (!el) return;
            const items = extractItems(data);
            if (!items || items.length === 0) {
                el.innerHTML = '<div class="empty-state">Нет данных о странах</div>';
                return;
            }
            el.innerHTML = items.map(c => `
                <div class="list-item">
                    <div class="info">${c.country || c.name || 'Неизвестно'} — ${c.count || c.value || 0} посещений</div>
                </div>
            `).join('');
        })
        .catch(err => {
            const el = getEl('stat-countries');
            if (el) el.innerHTML = `<div class="error-state">Ошибка загрузки: ${err.message}</div>`;
            console.error('Countries stats error:', err);
        });

    apiFetch('/statistics/spending')
        .then(resp => {
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return resp.json();
        })
        .then(data => {
            const el = getEl('stat-spending');
            if (!el) return;
            const items = extractItems(data);
            if (!items || items.length === 0) {
                el.innerHTML = '<div class="empty-state">Нет данных о затратах</div>';
                return;
            }
            el.innerHTML = items.map(s => `
                <div class="list-item">
                    <div class="info">${s.month || s.period || 'Неизвестно'} — ${s.amount || s.total || 0} ${s.currency || ''}</div>
                </div>
            `).join('');
        })
        .catch(err => {
            const el = getEl('stat-spending');
            if (el) el.innerHTML = `<div class="error-state">Ошибка загрузки: ${err.message}</div>`;
            console.error('Spending stats error:', err);
        });

    apiFetch('/statistics/upcoming-trips')
        .then(resp => {
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return resp.json();
        })
        .then(data => {
            const el = getEl('stat-upcoming');
            if (!el) return;
            const items = extractItems(data);
            if (!items || items.length === 0) {
                el.innerHTML = '<div class="empty-state">Нет предстоящих поездок</div>';
                return;
            }
            el.innerHTML = items.map(t => `
                <div class="list-item">
                    <div class="info">${t.title || t.name || 'Без названия'} — ${t.start_date || t.start || 'дата неизвестна'}</div>
                </div>
            `).join('');
        })
        .catch(err => {
            const el = getEl('stat-upcoming');
            if (el) el.innerHTML = `<div class="error-state">Ошибка загрузки: ${err.message}</div>`;
            console.error('Upcoming trips error:', err);
        });

    apiFetch('/statistics/trip-durations')
        .then(resp => {
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return resp.json();
        })
        .then(data => {
            const el = getEl('stat-durations');
            if (!el) return;
            const items = extractItems(data);
            if (!items || items.length === 0) {
                el.innerHTML = '<div class="empty-state">Нет данных о длительности</div>';
                return;
            }
            el.innerHTML = items.map(d => `
                <div class="list-item">
                    <div class="info">${d.trip_title || d.title || 'Без названия'} — ${d.duration_days || d.duration || 0} дней</div>
                </div>
            `).join('');
        })
        .catch(err => {
            const el = getEl('stat-durations');
            if (el) el.innerHTML = `<div class="error-state">Ошибка загрузки: ${err.message}</div>`;
            console.error('Trip durations error:', err);
        });
}

// ============================================================
//  RECOMMENDATIONS
// ============================================================
function renderRecommendations(container) {
    container.innerHTML = `
        <div class="section active">
            <h2>Рекомендации</h2>
            <div class="card">
                <h3>Рекомендации для поездки</h3>
                <div class="form-row">
                    <label>Выберите поездку <select id="rec-trip-select"><option value="">Загрузка поездок...</option></select></label>
                    <button class="btn" id="rec-trip-btn">Показать рекомендации</button>
                </div>
                <div id="rec-trip-result"></div>
            </div>
            <div class="card">
                <h3>Рекомендации для заказа</h3>
                <div class="form-row">
                    <label>Поездка <select id="rec-order-trip-select"><option value="">Загрузка поездок...</option></select></label>
                </div>
                <div class="form-row">
                    <label>Заказ <select id="rec-order-select"><option value="">Сначала выберите поездку</option></select></label>
                    <button class="btn" id="rec-order-btn">Показать рекомендации</button>
                </div>
                <div id="rec-order-result"></div>
            </div>
        </div>
    `;

    function getEl(id) {
        const el = document.getElementById(id);
        if (!el) console.warn(`Element ${id} not found`);
        return el;
    }

    apiFetch('/trips')
        .then(resp => {
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            return resp.json();
        })
        .then(trips => {
            const select = getEl('rec-trip-select');
            if (!select) return;
            select.innerHTML = '<option value="">Выберите поездку</option>';
            trips.forEach(t => {
                const opt = document.createElement('option');
                opt.value = String(t.id);
                opt.textContent = t.title || t.id;
                select.appendChild(opt);
            });
        })
        .catch(err => {
            const select = getEl('rec-trip-select');
            if (select) select.innerHTML = `<option value="">Ошибка: ${err.message}</option>`;
            console.error('Load trips error:', err);
        });

    const orderTripSelect = getEl('rec-order-trip-select');
    if (orderTripSelect) {
        apiFetch('/trips')
            .then(resp => {
                if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                return resp.json();
            })
            .then(trips => {
                orderTripSelect.innerHTML = '<option value="">Выберите поездку</option>';
                trips.forEach(t => {
                    const opt = document.createElement('option');
                    opt.value = String(t.id);
                    opt.textContent = t.title || t.id;
                    orderTripSelect.appendChild(opt);
                });
            })
            .catch(err => {
                orderTripSelect.innerHTML = `<option value="">Ошибка: ${err.message}</option>`;
                console.error('Load trips for orders error:', err);
            });
    }

    if (orderTripSelect) {
        orderTripSelect.addEventListener('change', function() {
            const tripId = this.value;
            const orderSelect = getEl('rec-order-select');
            if (!orderSelect) return;
            if (!tripId) {
                orderSelect.innerHTML = '<option value="">Сначала выберите поездку</option>';
                return;
            }
            orderSelect.innerHTML = '<option value="">Загрузка заказов...</option>';
            apiFetch(`/trips/${tripId}/orders`)
                .then(resp => {
                    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                    return resp.json();
                })
                .then(orders => {
                    orderSelect.innerHTML = '<option value="">Выберите заказ</option>';
                    orders.forEach(o => {
                        const opt = document.createElement('option');
                        opt.value = String(o.id);
                        opt.textContent = o.title || o.id;
                        orderSelect.appendChild(opt);
                    });
                })
                .catch(err => {
                    orderSelect.innerHTML = `<option value="">Ошибка: ${err.message}</option>`;
                    console.error('Load orders error:', err);
                });
        });
    }

    const tripBtn = getEl('rec-trip-btn');
    if (tripBtn) {
        tripBtn.addEventListener('click', function() {
            const tripId = getEl('rec-trip-select')?.value;
            if (!tripId) { showToast('Выберите поездку', 'error'); return; }
            const resultEl = getEl('rec-trip-result');
            if (!resultEl) return;
            resultEl.innerHTML = '<div class="loading">Загрузка...</div>';
            apiFetch(`/trips/${tripId}/recommendations`)
                .then(resp => {
                    if (resp.status === 404) {
                        throw new Error('Эндпоинт не найден (404). Возможно, рекомендации ещё не реализованы на бэкенде.');
                    }
                    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                    return resp.json();
                })
                .then(data => {
                    if (!data || data.length === 0) {
                        resultEl.innerHTML = '<div class="empty-state">Нет рекомендаций</div>';
                        return;
                    }
                    resultEl.innerHTML = data.map(r => `
                        <div class="list-item">
                            <div class="info">
                                <div class="title">${r.service?.title || 'Без названия'}</div>
                                <div class="sub">${r.reason || ''} ${r.score ? '(оценка: ' + r.score + ')' : ''}</div>
                            </div>
                        </div>
                    `).join('');
                })
                .catch(err => {
                    resultEl.innerHTML = `<div class="error-state">Ошибка: ${err.message}</div>`;
                    console.error('Recommendations error:', err);
                });
        });
    }

    const orderBtn = getEl('rec-order-btn');
    if (orderBtn) {
        orderBtn.addEventListener('click', function() {
            const orderId = getEl('rec-order-select')?.value;
            if (!orderId) { showToast('Выберите заказ', 'error'); return; }
            const resultEl = getEl('rec-order-result');
            if (!resultEl) return;
            resultEl.innerHTML = '<div class="loading">Загрузка...</div>';
            apiFetch(`/orders/${orderId}/recommendations`)
                .then(resp => {
                    if (resp.status === 404) {
                        throw new Error('Эндпоинт не найден (404). Возможно, рекомендации ещё не реализованы на бэкенде.');
                    }
                    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                    return resp.json();
                })
                .then(data => {
                    if (!data || data.length === 0) {
                        resultEl.innerHTML = '<div class="empty-state">Нет рекомендаций</div>';
                        return;
                    }
                    resultEl.innerHTML = data.map(r => `
                        <div class="list-item">
                            <div class="info">
                                <div class="title">${r.service?.title || 'Без названия'}</div>
                                <div class="sub">${r.reason || ''} ${r.score ? '(оценка: ' + r.score + ')' : ''}</div>
                            </div>
                        </div>
                    `).join('');
                })
                .catch(err => {
                    resultEl.innerHTML = `<div class="error-state">Ошибка: ${err.message}</div>`;
                    console.error('Order recommendations error:', err);
                });
        });
    }
}

// ============================================================
//  ADMIN PANEL
// ============================================================
function renderAdmin(container) {
    if (!state.roles.includes('ADMIN')) {
        container.innerHTML = `<div class="section active"><h2>Доступ запрещён</h2><p>У вас нет прав администратора.</p></div>`;
        return;
    }

    let activeTab = 'achievements';

    container.innerHTML = `
        <div class="section active">
            <h2>Административная панель</h2>
            <div class="form-row" style="margin-bottom:16px;">
                <button class="btn btn-sm btn-outline admin-tab" data-tab="achievements" style="background:var(--color-primary);">Достижения</button>
                <button class="btn btn-sm btn-outline admin-tab" data-tab="providers">Провайдеры</button>
                <button class="btn btn-sm btn-outline admin-tab" data-tab="services">Доп. услуги</button>
                <button class="btn btn-sm btn-outline admin-tab" data-tab="orders">Симуляция заказов</button>
            </div>
            <div id="admin-content"></div>
        </div>
    `;

    const tabs = container.querySelectorAll('.admin-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            tabs.forEach(t => t.style.background = '');
            this.style.background = 'var(--color-primary)';
            activeTab = this.dataset.tab;
            renderAdminTab(activeTab);
        });
    });

    function renderAdminTab(tab) {
        const content = document.getElementById('admin-content');
        switch (tab) {
            case 'achievements':
                renderAchievementsAdmin(content);
                break;
            case 'providers':
                renderProvidersAdmin(content);
                break;
            case 'services':
                renderServicesAdmin(content);
                break;
            case 'orders':
                renderOrdersAdmin(content);
                break;
            default:
                content.innerHTML = '<p>Неизвестная вкладка</p>';
        }
    }

    function renderAchievementsAdmin(el) {
        let achievements = [];
        let loading = true;
        let createData = { code: '', title: '', description: '', conditionType: 'TRIPS_COUNT', conditionValue: '', iconUrl: '' };

        el.innerHTML = `
            <div class="card">
                <h3>Управление достижениями</h3>
                <div class="form-row">
                    <label>Код <input id="adm-ach-code" /></label>
                    <label>Название <input id="adm-ach-title" /></label>
                    <label>Описание <input id="adm-ach-desc" /></label>
                    <label>Тип условия <select id="adm-ach-cond">
                        <option value="TRIPS_COUNT">Количество поездок</option>
                        <option value="COUNTRIES_COUNT">Количество стран</option>
                        <option value="ORDERS_COUNT">Количество заказов</option>
                        <option value="REVIEWS_COUNT">Количество отзывов</option>
                        <option value="SPENDING_AMOUNT">Сумма трат</option>
                    </select></label>
                    <label>Значение <input id="adm-ach-value" type="number" /></label>
                    <label>URL иконки <input id="adm-ach-icon" /></label>
                </div>
                <button class="btn btn-primary" id="adm-ach-create">Создать</button>
                <hr style="margin:20px 0;border-color:#2a2440;" />
                <div id="adm-ach-list"><p>Загрузка...</p></div>
            </div>
        `;

        async function loadAchievements() {
            try {
                const resp = await apiFetch('/achievements');
                if (resp.ok) {
                    achievements = await resp.json();
                    renderList();
                }
            } catch (e) {
                document.getElementById('adm-ach-list').innerHTML = '<div class="error-state">Ошибка загрузки</div>';
            }
        }

        function renderList() {
            const listEl = document.getElementById('adm-ach-list');
            if (!listEl) return;
            if (!achievements || achievements.length === 0) {
                listEl.innerHTML = '<div class="empty-state">Нет достижений</div>';
                return;
            }
            listEl.innerHTML = achievements.map(a => `
                <div class="list-item">
                    <div class="info">
                        <div class="title">${a.title} (${a.code})</div>
                        <div class="sub">${a.description || ''}</div>
                    </div>
                    <div class="actions">
                        <button class="btn btn-sm btn-outline" onclick="adminEditAchievement('${a.id}')">✎</button>
                        <button class="btn btn-sm btn-danger" onclick="adminDeleteAchievement('${a.id}')">✕</button>
                    </div>
                </div>
            `).join('');
        }

        document.getElementById('adm-ach-create').addEventListener('click', async () => {
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
                if (resp.ok) {
                    showToast('Достижение создано');
                    loadAchievements();
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + err.message, 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });

        window.adminEditAchievement = (id) => {
            const ach = achievements.find(a => a.id === id);
            if (!ach) return;
            const code = prompt('Код:', ach.code);
            if (code === null) return;
            const title = prompt('Название:', ach.title);
            if (title === null) return;
            const desc = prompt('Описание:', ach.description || '');
            const condType = prompt('Тип условия (TRIPS_COUNT/COUNTRIES_COUNT/ORDERS_COUNT/REVIEWS_COUNT/SPENDING_AMOUNT):', ach.conditionType);
            if (condType === null) return;
            const condValue = prompt('Значение:', ach.conditionValue);
            if (condValue === null) return;
            const normalizedCondType = condType.toUpperCase().replace(/\s+/g, '_');
            apiFetch(`/admin/achievements/${id}`, {
                method: 'PATCH',
                body: JSON.stringify({
                    code, title, description: desc || null,
                    conditionType: normalizedCondType,
                    conditionValue: parseInt(condValue),
                    iconUrl: ach.iconUrl
                })
            }).then(resp => {
                if (resp.ok) { showToast('Обновлено'); loadAchievements(); }
                else { resp.json().then(err => showToast('Ошибка: ' + err.message, 'error')); }
            }).catch(() => showToast('Ошибка', 'error'));
        };

        window.adminDeleteAchievement = async (id) => {
            if (!confirm('Удалить достижение?')) return;
            try {
                const resp = await apiFetch(`/admin/achievements/${id}`, { method: 'DELETE' });
                if (resp.ok) { showToast('Удалено'); loadAchievements(); }
                else { showToast('Ошибка', 'error'); }
            } catch (e) {
                showToast('Ошибка', 'error');
            }
        };

        loadAchievements();
    }

    function renderProvidersAdmin(el) {
        let providers = [];
        let createData = { name: '', type: 'OTHER', website: '', support_contact: '' };

        el.innerHTML = `
            <div class="card">
                <h3>Управление провайдерами</h3>
                <div class="form-row">
                    <label>Название <input id="adm-prov-name" /></label>
                    <label>Тип <select id="adm-prov-type">
                        <option value="AIRLINE">Авиакомпания</option>
                        <option value="HOTEL">Отель</option>
                        <option value="TOUR_COMPANY">Туроператор</option>
                        <option value="TRANSPORT_COMPANY">Транспортная компания</option>
                        <option value="BOOKING_PLATFORM">Платформа бронирования</option>
                        <option value="INSURANCE_COMPANY">Страховая компания</option>
                        <option value="OTHER">Другое</option>
                    </select></label>
                    <label>Сайт <input id="adm-prov-website" /></label>
                    <label>Контакты <input id="adm-prov-contact" /></label>
                </div>
                <button class="btn btn-primary" id="adm-prov-create">Создать</button>
                <hr style="margin:20px 0;border-color:#2a2440;" />
                <div id="adm-prov-list"><p>Загрузка...</p></div>
            </div>
        `;

        async function loadProviders() {
            try {
                const resp = await apiFetch('/providers');
                if (resp.ok) {
                    providers = await resp.json();
                    renderList();
                }
            } catch (e) {
                document.getElementById('adm-prov-list').innerHTML = '<div class="error-state">Ошибка загрузки</div>';
            }
        }

        function renderList() {
            const listEl = document.getElementById('adm-prov-list');
            if (!listEl) return;
            if (!providers || providers.length === 0) {
                listEl.innerHTML = '<div class="empty-state">Нет провайдеров</div>';
                return;
            }
            listEl.innerHTML = providers.map(p => `
                <div class="list-item">
                    <div class="info">
                        <div class="title">${p.name} (${p.type})</div>
                        <div class="sub">${p.website || ''} ${p.support_contact || ''}</div>
                    </div>
                    <div class="actions">
                        <button class="btn btn-sm btn-outline" onclick="adminEditProvider('${p.id}')">✎</button>
                        <button class="btn btn-sm btn-danger" onclick="adminDeleteProvider('${p.id}')">✕</button>
                    </div>
                </div>
            `).join('');
        }

        document.getElementById('adm-prov-create').addEventListener('click', async () => {
            const name = document.getElementById('adm-prov-name').value;
            const type = document.getElementById('adm-prov-type').value;
            const website = document.getElementById('adm-prov-website').value;
            const contact = document.getElementById('adm-prov-contact').value;
            if (!name) { showToast('Название обязательно', 'error'); return; }
            try {
                const resp = await apiFetch('/admin/providers', {
                    method: 'POST',
                    body: JSON.stringify({ name, type, website: website || null, support_contact: contact || null })
                });
                if (resp.ok) {
                    showToast('Провайдер создан');
                    loadProviders();
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + err.message, 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });

        window.adminEditProvider = (id) => {
            const p = providers.find(x => x.id === id);
            if (!p) return;
            const name = prompt('Название:', p.name);
            if (name === null) return;
            const type = prompt('Тип:', p.type);
            if (type === null) return;
            const website = prompt('Сайт:', p.website || '');
            const contact = prompt('Контакты:', p.support_contact || '');
            apiFetch(`/admin/providers/${id}`, {
                method: 'PATCH',
                body: JSON.stringify({ name, type, website: website || null, support_contact: contact || null })
            }).then(resp => {
                if (resp.ok) { showToast('Обновлено'); loadProviders(); }
                else { resp.json().then(err => showToast('Ошибка: ' + err.message, 'error')); }
            }).catch(() => showToast('Ошибка', 'error'));
        };

        window.adminDeleteProvider = async (id) => {
            if (!confirm('Удалить провайдера?')) return;
            try {
                const resp = await apiFetch(`/admin/providers/${id}`, { method: 'DELETE' });
                if (resp.ok) { showToast('Удалено'); loadProviders(); }
                else { showToast('Ошибка', 'error'); }
            } catch (e) {
                showToast('Ошибка', 'error');
            }
        };

        loadProviders();
    }

    function renderServicesAdmin(el) {
        let services = [];
        let createData = { title: '', description: '', service_type: 'OTHER', provider_id: '', location_id: '', price_amount: '', price_currency: '', is_active: true };

        el.innerHTML = `
            <div class="card">
                <h3>Управление доп. услугами</h3>
                <div class="form-row">
                    <label>Название <input id="adm-serv-title" /></label>
                    <label>Описание <input id="adm-serv-desc" /></label>
                    <label>Тип <select id="adm-serv-type">
                        <option value="FLIGHT">Авиа</option>
                        <option value="TRAIN">Поезд</option>
                        <option value="BUS">Автобус</option>
                        <option value="HOTEL">Отель</option>
                        <option value="TOUR">Тур</option>
                        <option value="CAR_RENTAL">Аренда авто</option>
                        <option value="INSURANCE">Страховка</option>
                        <option value="TAXI">Такси</option>
                        <option value="ESIM">eSIM</option>
                        <option value="LOUNGE">Лаунж</option>
                        <option value="EXTRA_BAGGAGE">Доп. багаж</option>
                        <option value="OTHER">Другое</option>
                    </select></label>
                    <label>Цена <input id="adm-serv-price" type="number" step="any" /></label>
                    <label>Валюта <input id="adm-serv-currency" /></label>
                    <label>ID провайдера <input id="adm-serv-provider" /></label>
                    <label>ID локации <input id="adm-serv-location" /></label>
                    <label><input type="checkbox" id="adm-serv-active" checked /> Активна</label>
                </div>
                <button class="btn btn-primary" id="adm-serv-create">Создать</button>
                <hr style="margin:20px 0;border-color:#2a2440;" />
                <div id="adm-serv-list"><p>Загрузка...</p></div>
            </div>
        `;

        async function loadServices() {
            try {
                const resp = await apiFetch('/additional-services');
                if (resp.ok) {
                    services = await resp.json();
                    renderList();
                }
            } catch (e) {
                document.getElementById('adm-serv-list').innerHTML = '<div class="error-state">Ошибка загрузки</div>';
            }
        }

        function renderList() {
            const listEl = document.getElementById('adm-serv-list');
            if (!listEl) return;
            if (!services || services.length === 0) {
                listEl.innerHTML = '<div class="empty-state">Нет услуг</div>';
                return;
            }
            listEl.innerHTML = services.map(s => `
                <div class="list-item">
                    <div class="info">
                        <div class="title">${s.title} (${s.service_type})</div>
                        <div class="sub">${s.description || ''} ${s.price_amount ? s.price_amount + ' ' + s.price_currency : ''}</div>
                    </div>
                    <div class="actions">
                        <button class="btn btn-sm btn-outline" onclick="adminEditService('${s.id}')">✎</button>
                        <button class="btn btn-sm btn-danger" onclick="adminDeleteService('${s.id}')">✕</button>
                    </div>
                </div>
            `).join('');
        }

        document.getElementById('adm-serv-create').addEventListener('click', async () => {
            const title = document.getElementById('adm-serv-title').value;
            const description = document.getElementById('adm-serv-desc').value;
            const service_type = document.getElementById('adm-serv-type').value;
            const price_amount = parseFloat(document.getElementById('adm-serv-price').value);
            const price_currency = document.getElementById('adm-serv-currency').value;
            const provider_id = document.getElementById('adm-serv-provider').value;
            const location_id = document.getElementById('adm-serv-location').value;
            const is_active = document.getElementById('adm-serv-active').checked;
            if (!title) { showToast('Название обязательно', 'error'); return; }
            try {
                const resp = await apiFetch('/admin/additional-services', {
                    method: 'POST',
                    body: JSON.stringify({
                        title, description: description || null, service_type,
                        price_amount: isNaN(price_amount) ? null : price_amount,
                        price_currency: price_currency || null,
                        provider_id: provider_id || null,
                        location_id: location_id || null,
                        is_active
                    })
                });
                if (resp.ok) {
                    showToast('Услуга создана');
                    loadServices();
                } else {
                    const err = await resp.json();
                    showToast('Ошибка: ' + err.message, 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });

        window.adminEditService = (id) => {
            const s = services.find(x => x.id === id);
            if (!s) return;
            const title = prompt('Название:', s.title);
            if (title === null) return;
            const description = prompt('Описание:', s.description || '');
            const service_type = prompt('Тип:', s.service_type);
            if (service_type === null) return;
            const price = prompt('Цена:', s.price_amount || '');
            const currency = prompt('Валюта:', s.price_currency || '');
            const is_active = confirm('Активна?');
            apiFetch(`/admin/additional-services/${id}`, {
                method: 'PATCH',
                body: JSON.stringify({
                    title, description: description || null, service_type,
                    price_amount: price ? parseFloat(price) : null,
                    price_currency: currency || null,
                    is_active
                })
            }).then(resp => {
                if (resp.ok) { showToast('Обновлено'); loadServices(); }
                else { resp.json().then(err => showToast('Ошибка: ' + err.message, 'error')); }
            }).catch(() => showToast('Ошибка', 'error'));
        };

        window.adminDeleteService = async (id) => {
            if (!confirm('Удалить услугу?')) return;
            try {
                const resp = await apiFetch(`/admin/additional-services/${id}`, { method: 'DELETE' });
                if (resp.ok) { showToast('Удалено'); loadServices(); }
                else { showToast('Ошибка', 'error'); }
            } catch (e) {
                showToast('Ошибка', 'error');
            }
        };

        loadServices();
    }

    function renderOrdersAdmin(el) {
        el.innerHTML = `
            <div class="card">
                <h3>Симуляция статуса заказа</h3>
                <div class="form-row">
                    <label>ID заказа <input id="adm-order-id" placeholder="UUID заказа" /></label>
                    <label>Статус <select id="adm-order-status">
                        <option value="PENDING_VERIFICATION">Ожидает проверки</option>
                        <option value="CONFIRMED">Подтверждён</option>
                        <option value="DELAYED">Задержан</option>
                        <option value="CANCELLED">Отменён</option>
                        <option value="COMPLETED">Завершён</option>
                        <option value="REFUND_PENDING">Возврат средств</option>
                        <option value="REFUNDED">Возвращён</option>
                    </select></label>
                    <label>Причина <input id="adm-order-reason" placeholder="Опционально" /></label>
                </div>
                <button class="btn btn-primary" id="adm-order-simulate">Симулировать</button>
                <p style="font-size:14px;color:var(--color-text-secondary);">Этот эндпоинт создаёт событие статуса с источником admin_simulation.</p>
                <div id="adm-order-result"></div>
            </div>
        `;

        document.getElementById('adm-order-simulate').addEventListener('click', async () => {
            const orderId = document.getElementById('adm-order-id').value.trim();
            const status = document.getElementById('adm-order-status').value;
            const reason = document.getElementById('adm-order-reason').value;
            if (!orderId) { showToast('Введите ID заказа', 'error'); return; }
            try {
                const resp = await apiFetch(`/admin/orders/${orderId}/simulate-status-change`, {
                    method: 'POST',
                    body: JSON.stringify({ status, reason: reason || null })
                });
                if (resp.ok) {
                    const data = await resp.json();
                    document.getElementById('adm-order-result').innerHTML = `<div class="success">Статус обновлён: ${data.status}</div>`;
                    showToast('Симуляция выполнена');
                } else {
                    const err = await resp.json();
                    document.getElementById('adm-order-result').innerHTML = `<div class="error-state">${err.message || 'Ошибка'}</div>`;
                    showToast('Ошибка: ' + err.message, 'error');
                }
            } catch (e) {
                showToast('Ошибка: ' + e.message, 'error');
            }
        });
    }

    renderAdminTab('achievements');
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