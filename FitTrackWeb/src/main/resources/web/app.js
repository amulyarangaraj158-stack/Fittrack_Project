(function () {
    'use strict';

    const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

    const state = {
        token: null, // Kept strictly in memory (never persisted in Web Storage)
        user: null,
        settings: {
            themeMode: 'light',
            workoutReminders: true,
            waterReminders: true,
            goalAlerts: true,
            achievementAlerts: true,
            weeklyDigest: true
        },
        currentPage: 'dashboard',
        dashRange: '7d',
        dashMetric: 'calories',
        dashData: null,
        workouts: [],
        plans: [],
        maxPlansAllowed: 7,
        reportsData: null,
        activeWorkoutDetail: null,
        timer: {
            mode: 'stopwatch',
            running: false,
            elapsedSec: 0,
            remainingSec: 20 * 60,
            intervalId: null
        },
        confirmAction: null,
        searchTimer: null
    };

    const $ = (id) => document.getElementById(id);

    function todayISO() {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatNum(n) {
        return Number(n || 0).toLocaleString();
    }

    let toastTimeout = null;
    function showToast(msg, isError) {
        const el = $('toast');
        el.textContent = msg;
        el.className = isError ? 'toast error' : 'toast';
        clearTimeout(toastTimeout);
        toastTimeout = setTimeout(() => el.classList.add('hidden'), 3600);
    }

    async function api(path, options = {}) {
        const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
        if (state.token) {
            headers['Authorization'] = `Bearer ${state.token}`;
        }
        const res = await fetch(path, { ...options, headers });
        const text = await res.text();
        let data = {};
        try {
            data = text ? JSON.parse(text) : {};
        } catch (_) {
            data = {};
        }
        if (res.status === 401 && state.token && !path.startsWith('/api/auth/')) {
            handleSessionExpired();
            throw new Error(data.error || 'Session expired. Please sign in again.');
        }
        if (!res.ok) {
            throw new Error(data.error || `Request failed (${res.status})`);
        }
        return data;
    }

    function handleSessionExpired() {
        state.token = null;
        state.user = null;
        stopTimer();
        applyTheme('light');
        $('app-shell').classList.add('hidden');
        $('auth-shell').classList.remove('hidden');
        showToast('Session ended. Please sign in.', true);
    }

    function applyTheme(mode) {
        const dark = String(mode || '').toLowerCase() === 'dark';
        document.documentElement.classList.toggle('dark', dark);
        if ($('set-theme')) {
            $('set-theme').value = dark ? 'dark' : 'light';
        }
        redrawVisibleCharts();
    }

    function redrawVisibleCharts() {
        if (state.currentPage === 'dashboard' && state.dashData) {
            renderDashboardCharts(state.dashData);
        } else if (state.currentPage === 'reports' && state.reportsData) {
            renderReportCharts(state.reportsData);
        }
    }

    function updateUserChrome() {
        const u = state.user;
        if (!u) return;
        const badge = u.membershipBadge || (u.hasPremiumAnalytics ? 'Premium Member' : 'Regular Member');
        $('sidebar-user-name').textContent = u.fullName;
        $('sidebar-user-tier').textContent = badge;
        $('topbar-welcome').textContent = `Welcome back, ${u.fullName}`;
        $('topbar-date').textContent = new Date().toLocaleDateString(undefined, {
            weekday: 'short', year: 'numeric', month: 'short', day: 'numeric'
        });
        $('topbar-profile-name').textContent = u.fullName.split(' ')[0];
        $('topbar-avatar').textContent = (u.fullName || 'U').trim().charAt(0).toUpperCase();

        // Annotate premium range buttons when user is Regular
        document.querySelectorAll('#dash-range-bar .range-btn').forEach(btn => {
            const r = btn.dataset.range;
            const locked = (r === '3m' || r === '6m') && !u.hasPremiumAnalytics;
            btn.classList.toggle('locked-tab', locked);
            btn.title = locked ? 'Premium membership required for 3-month and 6-month analytics' : '';
        });
        $('dash-tier-hint').textContent = u.hasPremiumAnalytics
            ? 'Premium Analytics Active (up to 6 months & 50 plans)'
            : 'Regular Tier (up to 30 days analytics & 7 plans)';
    }

    function setPageDefaultDates() {
        const iso = todayISO();
        ['w-date', 'qw-date', 'act-date-input', 'water-date', 's-date', 'wt-date'].forEach(id => {
            const el = $(id);
            if (el && !el.value) el.value = iso;
        });
    }

    function closeAllPopovers() {
        $('search-popover').classList.add('hidden');
        $('notif-popover').classList.add('hidden');
        $('profile-popover').classList.add('hidden');
    }

    function closeMobileSidebar() {
        $('sidebar').classList.remove('open');
        $('sidebar-backdrop').classList.add('hidden');
    }

    async function navigateTo(page) {
        state.currentPage = page;
        closeAllPopovers();
        closeMobileSidebar();

        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.page === page);
        });
        document.querySelectorAll('.page-view').forEach(sec => {
            sec.classList.toggle('hidden', sec.id !== `page-${page}`);
        });

        try {
            if (page === 'dashboard') await loadDashboard();
            else if (page === 'workouts') await loadWorkouts();
            else if (page === 'planner') await loadPlanner();
            else if (page === 'goals') await loadGoals();
            else if (page === 'activities') await loadActivities();
            else if (page === 'water') await loadWater();
            else if (page === 'sleep') await loadSleep();
            else if (page === 'weight') await loadWeight();
            else if (page === 'achievements') await loadAchievements();
            else if (page === 'reports') await loadReports();
            else if (page === 'profile') await loadProfile();
            else if (page === 'settings') await loadSettings();
        } catch (err) {
            showToast(err.message, true);
        }
    }

    /* ---------------- CONFIRMATION MODAL ---------------- */
    function openConfirmModal({ title, message, confirmText = 'Delete', requirePassword = false, onConfirm }) {
        $('cm-title').textContent = title;
        $('cm-message').textContent = message;
        $('cm-confirm-btn').textContent = confirmText;
        $('cm-password-input').value = '';
        $('cm-password-wrap').classList.toggle('hidden', !requirePassword);
        state.confirmAction = onConfirm;
        $('confirm-modal').classList.remove('hidden');
    }

    function closeConfirmModal() {
        $('confirm-modal').classList.add('hidden');
        state.confirmAction = null;
    }

    /* ---------------- AUTH ---------------- */
    async function enterWorkspace(authResp) {
        state.token = authResp.token;
        state.user = authResp.user;
        $('auth-shell').classList.add('hidden');
        $('app-shell').classList.remove('hidden');
        setPageDefaultDates();
        updateUserChrome();
        await navigateTo('dashboard');
    }

    /* ---------------- 1. DASHBOARD ---------------- */
    function deltaHtml(cur, prev, unit = '') {
        const c = Number(cur) || 0;
        const p = Number(prev) || 0;
        if (p === 0 && c === 0) return '<span class="delta-flat">No change vs prev</span>';
        if (p === 0) return `<span class="delta-up">+${formatNum(Math.round(c * 10) / 10)}${unit} vs prev</span>`;
        const diffPct = Math.round(((c - p) / p) * 100);
        if (diffPct > 0) return `<span class="delta-up">+${diffPct}% vs prev</span>`;
        if (diffPct < 0) return `<span class="delta-down">${diffPct}% vs prev</span>`;
        return '<span class="delta-flat">0% vs prev</span>';
    }

    function renderGoalRings(goals) {
        const items = [
            { label: 'Steps', pct: goals.stepPercent || 0, sub: `${formatNum(goals.todaySteps)} / ${formatNum(goals.dailyStepTarget)}`, cls: '' },
            { label: 'Water', pct: goals.waterPercent || 0, sub: `${formatNum(goals.todayWaterMl)} / ${formatNum(goals.dailyWaterTargetMl)} ml`, cls: 'blue' },
            { label: 'Workouts', pct: goals.weeklyPercent || 0, sub: `${goals.weeklyWorkouts} / ${goals.weeklyWorkoutTarget} wk`, cls: '' },
            { label: 'Calories', pct: goals.caloriePercent || 0, sub: `${Math.round(goals.todayCalories)} / ${Math.round(goals.dailyCalorieTarget)}`, cls: 'amber' },
            { label: 'Active Min', pct: goals.minutesPercent || 0, sub: `${goals.weeklyMinutes} / ${goals.activeMinutesTarget} m`, cls: '' },
            { label: 'Distance', pct: goals.distancePercent || 0, sub: `${goals.weeklyDistanceKm} / ${goals.distanceKmTarget} km`, cls: 'blue' }
        ];
        const r = 29;
        const c = 2 * Math.PI * r;
        $('dash-goal-rings').innerHTML = items.map(it => {
            const clamped = Math.max(0, Math.min(100, Number(it.pct) || 0));
            const offset = c - (clamped / 100) * c;
            return `
                <div class="ring-card">
                    <div class="ring-wrap">
                        <svg class="ring-svg" viewBox="0 0 72 72">
                            <circle class="ring-track" cx="36" cy="36" r="${r}"></circle>
                            <circle class="ring-progress ${it.cls}" cx="36" cy="36" r="${r}"
                                stroke-dasharray="${c.toFixed(1)}" stroke-dashoffset="${offset.toFixed(1)}"></circle>
                        </svg>
                        <div class="ring-center-val">${Math.round(clamped)}%</div>
                    </div>
                    <div>
                        <div style="font-weight:600; font-size:13px;">${escapeHtml(it.label)}</div>
                        <div class="muted-meta tabular">${escapeHtml(it.sub)}</div>
                    </div>
                </div>
            `;
        }).join('');
    }

    function renderWeekBoard(containerEl, plans) {
        const grouped = {};
        DAYS.forEach(d => { grouped[d] = []; });
        (plans || []).forEach(p => {
            if (grouped[p.dayOfWeek]) grouped[p.dayOfWeek].push(p);
        });
        containerEl.innerHTML = DAYS.map(day => {
            const list = grouped[day];
            const itemsHtml = list.length === 0
                ? '<div class="muted-meta">Rest day</div>'
                : list.map(p => `
                    <div class="day-plan-item ${p.completedDate ? 'done' : ''}">
                        <div class="day-plan-title">${escapeHtml(p.workoutName || p.workoutType)}</div>
                        <div class="muted-meta tabular">${escapeHtml(p.workoutType)} &middot; ${p.plannedMinutes} min</div>
                        <div>
                            <span class="status-label ${p.completedDate ? 'success' : 'neutral'}">
                                ${p.completedDate ? 'Done' : escapeHtml(p.intensity)}
                            </span>
                        </div>
                    </div>
                `).join('');
            return `
                <div class="day-col">
                    <div class="day-col-title">${day.slice(0, 3)}</div>
                    ${itemsHtml}
                </div>
            `;
        }).join('');
    }

    function renderDashboardCharts(d) {
        const series = d.activitySeries || [];
        const metricMap = {
            calories: { key: 'calories', color: '#D97706' },
            duration: { key: 'duration', color: '#059669' },
            sessions: { key: 'sessions', color: '#0284C7' },
            steps: { key: 'steps', color: '#7C3AED' }
        };
        const cfg = metricMap[state.dashMetric] || metricMap.calories;
        window.FitTrackCharts.drawBarSeries($('dash-overview-chart'), series, cfg.key, cfg.color);
        window.FitTrackCharts.drawWorkoutBreakdownChart($('dash-breakdown-chart'), d.typeBreakdown || []);

        window.FitTrackCharts.drawSparkline($('spark-steps'), series.map(s => s.steps), '#059669');
        window.FitTrackCharts.drawSparkline($('spark-calories'), series.map(s => s.calories), '#D97706');
        window.FitTrackCharts.drawSparkline($('spark-duration'), series.map(s => s.duration), '#059669');
        window.FitTrackCharts.drawSparkline($('spark-sessions'), series.map(s => s.sessions), '#0284C7');
    }

    async function loadDashboard() {
        const qp = new URLSearchParams({ range: state.dashRange });
        if (state.dashRange === 'custom') {
            qp.set('from', $('dash-from').value || todayISO());
            qp.set('to', $('dash-to').value || todayISO());
        }
        const d = await api(`/api/dashboard?${qp.toString()}`);
        state.dashData = d;
        state.user = d.user;
        if (d.settings) {
            state.settings = d.settings;
            applyTheme(d.settings.themeMode);
        }
        updateUserChrome();

        $('dash-greeting').textContent = `Welcome back, ${d.user.fullName}`;
        $('dash-subtitle').textContent = `Overview for ${d.fromDate} to ${d.toDate} (${d.user.activityPreference})`;

        const g = d.goals || {};
        const ps = d.periodStats || {};

        $('kpi-steps').textContent = formatNum(d.todaySteps);
        $('kpi-steps-delta').innerHTML = deltaHtml(ps.steps, ps.prevSteps);
        $('kpi-steps-sub').textContent = `Target: ${formatNum(g.dailyStepTarget)}`;
        $('kpi-steps-bar').style.width = `${Math.min(100, g.stepPercent || 0)}%`;

        $('kpi-calories').textContent = `${Math.round(d.todayCalories || 0)} kcal`;
        $('kpi-cal-delta').innerHTML = deltaHtml(ps.calories, ps.prevCalories, ' kcal');
        $('kpi-calories-sub').textContent = `Target: ${Math.round(g.dailyCalorieTarget || 500)} kcal`;
        $('kpi-cal-bar').style.width = `${Math.min(100, g.caloriePercent || 0)}%`;

        $('kpi-duration').textContent = `${d.todayDuration || 0} min`;
        $('kpi-dur-delta').innerHTML = deltaHtml(ps.minutes, ps.prevMinutes, ' min');
        $('kpi-duration-sub').textContent = `7d: ${d.weeklyMinutes || 0} / ${g.activeMinutesTarget || 150} min`;
        $('kpi-dur-bar').style.width = `${Math.min(100, g.minutesPercent || 0)}%`;

        $('kpi-water').textContent = `${formatNum(d.todayWaterMl)} ml`;
        $('kpi-water-pct-label').textContent = `${Math.round(g.waterPercent || 0)}% of goal`;
        $('kpi-water-sub').textContent = `Target: ${formatNum(g.dailyWaterTargetMl)} ml`;
        $('kpi-water-bar').style.width = `${Math.min(100, g.waterPercent || 0)}%`;

        $('kpi-weekly-workouts').textContent = String(d.weeklyWorkouts || 0);
        $('kpi-sess-delta').innerHTML = deltaHtml(ps.sessions, ps.prevSessions);
        $('kpi-weekly-sub').textContent = `Target: ${g.weeklyWorkoutTarget || 4} sessions`;
        $('kpi-weekly-bar').style.width = `${Math.min(100, g.weeklyPercent || 0)}%`;

        const avgSleep = Number(d.weeklyAvgSleep || 0);
        $('kpi-sleep').textContent = `${avgSleep.toFixed(1)} hrs`;
        $('kpi-streak-label').textContent = `Streak: ${(d.streaks || {}).currentStreak || 0}d`;
        $('kpi-dist-sub').textContent = `7d dist: ${d.weeklyDistanceKm || 0} km`;
        $('kpi-sleep-bar').style.width = `${Math.min(100, Math.round((avgSleep / 8) * 100))}%`;

        updateUnreadBadge(d.unreadNotifications || 0);
        renderDashboardCharts(d);
        renderGoalRings(g);

        $('dash-pref-label').textContent = d.user.activityPreference || 'Balanced';
        const recs = d.recommendations || [];
        $('dash-recommendations').innerHTML = recs.length === 0
            ? '<div class="muted-text">No recommendations available.</div>'
            : recs.map(r => `
                <div class="rec-item">
                    <span>${escapeHtml(r)}</span>
                    <button type="button" class="btn btn-secondary btn-sm quick-rec-btn">Log</button>
                </div>
            `).join('');

        renderWeekBoard($('dash-week-board'), d.plans || []);

        const recent = d.recentWorkouts || [];
        $('dash-recent-tbody').innerHTML = recent.length === 0
            ? '<tr><td colspan="5" class="empty-cell">No workouts recorded yet.</td></tr>'
            : recent.map(w => `
                <tr class="clickable-row" data-workout-id="${w.workoutId}">
                    <td class="tabular">${escapeHtml(w.workoutDate)}</td>
                    <td><strong>${escapeHtml(w.workoutName || w.workoutType)}</strong></td>
                    <td>${escapeHtml(w.workoutType)}</td>
                    <td class="num">${w.durationMinutes} min</td>
                    <td class="num">${Math.round(w.caloriesBurned)} kcal</td>
                </tr>
            `).join('');

        const streaks = d.streaks || {};
        $('dash-current-streak').textContent = `${streaks.currentStreak || 0}d`;
        $('dash-longest-streak').textContent = `${streaks.longestStreak || 0}d`;
        const achs = d.achievements || [];
        const unlocked = achs.filter(a => a.unlocked);
        $('dash-achievement-count').textContent = `${unlocked.length}/${achs.length}`;

        const nextLocked = achs.find(a => !a.unlocked);
        const previewList = unlocked.slice(0, 2);
        if (nextLocked) previewList.push(nextLocked);
        $('dash-achievements').innerHTML = previewList.length === 0
            ? '<div class="muted-text">Log a workout to start unlocking milestones.</div>'
            : previewList.map(a => `
                <div class="rec-item">
                    <div>
                        <strong>${escapeHtml(a.title)}</strong>
                        <div class="muted-meta">${escapeHtml(a.description)}</div>
                    </div>
                    <span class="status-label ${a.unlocked ? 'success' : 'neutral'} tabular">
                        ${a.unlocked ? 'Unlocked' : `${Math.round(a.progressPercent || 0)}%`}
                    </span>
                </div>
            `).join('');
    }

    /* ---------------- TOPBAR SEARCH & NOTIFICATIONS ---------------- */
    function updateUnreadBadge(count) {
        const el = $('notif-badge');
        if (count > 0) {
            el.textContent = count > 99 ? '99+' : String(count);
            el.classList.remove('hidden');
        } else {
            el.classList.add('hidden');
        }
    }

    async function loadNotificationsPopover() {
        const res = await api('/api/notifications');
        updateUnreadBadge(res.unreadCount || 0);
        const list = res.notifications || [];
        $('notif-list').innerHTML = list.length === 0
            ? '<div style="padding:18px; text-align:center;" class="muted-text">No notifications right now.</div>'
            : list.map(n => `
                <div class="notif-item ${n.read ? '' : 'unread'}">
                    <div class="notif-item-head">
                        <span>${escapeHtml(n.title)}</span>
                        <span class="status-label ${n.read ? 'neutral' : 'info'}">${escapeHtml(n.category)}</span>
                    </div>
                    <div class="notif-item-msg">${escapeHtml(n.message)}</div>
                    <div class="notif-item-actions">
                        <span class="muted-meta tabular">${escapeHtml((n.createdAt || '').slice(0, 16))}</span>
                        <div class="inline-actions">
                            ${!n.read ? `<button type="button" class="link-btn notif-read-one" data-id="${n.notificationId}">Mark read</button>` : ''}
                            <button type="button" class="link-btn notif-del-one" style="color:var(--danger)" data-id="${n.notificationId}">Dismiss</button>
                        </div>
                    </div>
                </div>
            `).join('');
    }

    async function runGlobalSearch(q) {
        const pop = $('search-popover');
        if (!q || q.trim().length < 2) {
            pop.classList.add('hidden');
            return;
        }
        const res = await api(`/api/search?q=${encodeURIComponent(q.trim())}`);
        const wList = res.workouts || [];
        const pList = res.plans || [];
        if (wList.length === 0 && pList.length === 0) {
            pop.innerHTML = '<div style="padding:12px;" class="muted-text">No matching workouts or plans found.</div>';
            pop.classList.remove('hidden');
            return;
        }
        let html = '';
        if (wList.length > 0) {
            html += '<div class="search-group-title">Workouts</div>';
            html += wList.map(w => `
                <button type="button" class="search-result-item search-open-workout" data-id="${w.workoutId}">
                    <span><strong>${escapeHtml(w.workoutName || w.workoutType)}</strong> &middot; ${escapeHtml(w.workoutType)}</span>
                    <span class="muted-meta tabular">${escapeHtml(w.workoutDate)}</span>
                </button>
            `).join('');
        }
        if (pList.length > 0) {
            html += '<div class="search-group-title">Planned Sessions</div>';
            html += pList.map(p => `
                <button type="button" class="search-result-item search-open-planner">
                    <span><strong>${escapeHtml(p.workoutName || p.workoutType)}</strong> (${escapeHtml(p.dayOfWeek)})</span>
                    <span class="muted-meta tabular">${p.plannedMinutes} min</span>
                </button>
            `).join('');
        }
        pop.innerHTML = html;
        pop.classList.remove('hidden');
    }

    /* ---------------- 2. WORKOUTS & TIMER & MODALS ---------------- */
    function formatMMSS(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    function updateTimerUI() {
        const val = state.timer.mode === 'stopwatch' ? state.timer.elapsedSec : state.timer.remainingSec;
        $('timer-display').textContent = formatMMSS(Math.max(0, val));
    }

    function stopTimer() {
        if (state.timer.intervalId) {
            clearInterval(state.timer.intervalId);
            state.timer.intervalId = null;
        }
        state.timer.running = false;
    }

    async function previewWorkoutCalories(prefix) {
        const duration = Number($(`${prefix}-duration`).value) || 0;
        if (duration <= 0) {
            showToast('Enter a positive duration to preview calories.', true);
            return;
        }
        const res = await api('/api/workouts/preview', {
            method: 'POST',
            body: JSON.stringify({
                workoutType: $(`${prefix}-type`).value,
                durationMinutes: duration,
                intensity: $(`${prefix}-intensity`).value,
                steps: Number($(`${prefix}-steps`).value) || 0,
                distanceKm: Number($(`${prefix}-distance`).value) || 0
            })
        });
        const label = prefix === 'w' ? $('workout-calorie-preview') : $('qw-preview-label');
        label.textContent = `Estimated: ${Math.round(res.caloriesBurned)} kcal`;
    }

    async function openWorkoutDetail(workoutId) {
        const w = await api(`/api/workouts/${workoutId}`);
        state.activeWorkoutDetail = w;
        $('wd-body').innerHTML = `
            <div class="detail-grid">
                <div class="detail-item"><div class="detail-label">Workout Name</div><div class="detail-value">${escapeHtml(w.workoutName || w.workoutType)}</div></div>
                <div class="detail-item"><div class="detail-label">Type</div><div class="detail-value">${escapeHtml(w.workoutType)}</div></div>
                <div class="detail-item"><div class="detail-label">Date</div><div class="detail-value tabular">${escapeHtml(w.workoutDate)}</div></div>
                <div class="detail-item"><div class="detail-label">Intensity</div><div class="detail-value">${escapeHtml(w.intensity)}</div></div>
                <div class="detail-item"><div class="detail-label">Duration</div><div class="detail-value tabular">${w.durationMinutes} min</div></div>
                <div class="detail-item"><div class="detail-label">Calories Burned</div><div class="detail-value tabular">${Math.round(w.caloriesBurned)} kcal</div></div>
                <div class="detail-item"><div class="detail-label">Steps</div><div class="detail-value tabular">${formatNum(w.steps)}</div></div>
                <div class="detail-item"><div class="detail-label">Distance</div><div class="detail-value tabular">${Number(w.distanceKm || 0).toFixed(1)} km</div></div>
            </div>
            <div class="detail-item" style="margin-bottom: 14px;">
                <div class="detail-label">Notes &amp; Activity Summary</div>
                <div style="margin-top:4px; font-size:13px;">${escapeHtml(w.notes || 'No notes recorded.')}</div>
                <div class="muted-meta" style="margin-top:6px;">${escapeHtml(w.summary || '')}</div>
            </div>
        `;
        $('workout-detail-modal').classList.remove('hidden');
    }

    async function loadWorkouts() {
        const sortRaw = ($('wf-sort').value || 'date:desc').split(':');
        const qp = new URLSearchParams({
            q: $('wf-q').value.trim(),
            type: $('wf-type').value,
            intensity: $('wf-intensity').value,
            from: $('wf-from').value,
            to: $('wf-to').value,
            sortBy: sortRaw[0] || 'date',
            sortDir: sortRaw[1] || 'desc'
        });
        const res = await api(`/api/workouts?${qp.toString()}`);
        state.workouts = res.workouts || [];
        $('workouts-tbody').innerHTML = state.workouts.length === 0
            ? '<tr><td colspan="9" class="empty-cell">No workouts match the active filters.</td></tr>'
            : state.workouts.map(w => `
                <tr>
                    <td class="tabular">${escapeHtml(w.workoutDate)}</td>
                    <td><strong>${escapeHtml(w.workoutName || w.workoutType)}</strong></td>
                    <td>${escapeHtml(w.workoutType)}</td>
                    <td class="num">${w.durationMinutes} min</td>
                    <td><span class="status-label ${w.intensity === 'High' ? 'warn' : w.intensity === 'Low' ? 'neutral' : 'info'}">${escapeHtml(w.intensity)}</span></td>
                    <td class="num">${formatNum(w.steps)}</td>
                    <td class="num">${Number(w.distanceKm || 0).toFixed(1)} km</td>
                    <td class="num">${Math.round(w.caloriesBurned)} kcal</td>
                    <td class="table-actions">
                        <button type="button" class="btn btn-secondary btn-sm w-view-btn" data-id="${w.workoutId}">Details</button>
                        <button type="button" class="btn btn-secondary btn-sm w-edit-btn" data-id="${w.workoutId}">Edit</button>
                        <button type="button" class="btn btn-ghost btn-sm w-del-btn" style="color:var(--danger)" data-id="${w.workoutId}">Delete</button>
                    </td>
                </tr>
            `).join('');
    }

    function beginEditWorkout(w) {
        $('workout-edit-id').value = String(w.workoutId);
        $('workout-form-title').textContent = 'Edit workout';
        $('w-name').value = w.workoutName || w.workoutType;
        $('w-type').value = w.workoutType;
        $('w-duration').value = w.durationMinutes;
        $('w-date').value = w.workoutDate;
        $('w-intensity').value = w.intensity;
        $('w-steps').value = w.steps || 0;
        $('w-distance').value = w.distanceKm || 0;
        $('w-notes').value = w.notes || '';
        $('w-submit-btn').textContent = 'Update workout';
        $('w-cancel-edit-btn').classList.remove('hidden');
        $('workout-calorie-preview').textContent = `Current: ${Math.round(w.caloriesBurned)} kcal`;
    }

    function resetWorkoutForm() {
        $('workout-edit-id').value = '';
        $('workout-form-title').textContent = 'Log a workout';
        $('workout-form').reset();
        $('w-date').value = todayISO();
        $('w-duration').value = 30;
        $('w-intensity').value = 'Moderate';
        $('w-submit-btn').textContent = 'Save workout';
        $('w-cancel-edit-btn').classList.add('hidden');
        $('workout-calorie-preview').textContent = 'Estimated: -- kcal';
    }

    /* ---------------- 3. PLANNER ---------------- */
    async function loadPlanner() {
        const res = await api('/api/plans');
        state.plans = res.plans || [];
        state.maxPlansAllowed = res.maxAllowed || 7;
        $('planner-limit-label').textContent = `${state.plans.length} / ${state.maxPlansAllowed} plans`;

        renderWeekBoard($('planner-week-board'), state.plans);

        $('planner-tbody').innerHTML = state.plans.length === 0
            ? '<tr><td colspan="8" class="empty-cell">No scheduled workouts yet. Add one above.</td></tr>'
            : state.plans.map(p => `
                <tr>
                    <td><strong>${escapeHtml(p.dayOfWeek)}</strong></td>
                    <td>${escapeHtml(p.workoutName || p.workoutType)}</td>
                    <td>${escapeHtml(p.workoutType)}</td>
                    <td class="num">${p.plannedMinutes} min</td>
                    <td>${escapeHtml(p.intensity)}</td>
                    <td>${escapeHtml(p.notes || '-')}</td>
                    <td>
                        <span class="status-label ${p.completedDate ? 'success' : 'neutral'}">
                            ${p.completedDate ? `Completed (${escapeHtml(p.completedDate)})` : 'Planned'}
                        </span>
                    </td>
                    <td class="table-actions">
                        ${!p.completedDate ? `<button type="button" class="btn btn-primary btn-sm pl-complete-btn" data-id="${p.planId}">Complete</button>` : ''}
                        <button type="button" class="btn btn-secondary btn-sm pl-edit-btn" data-id="${p.planId}">Edit</button>
                        <button type="button" class="btn btn-ghost btn-sm pl-del-btn" style="color:var(--danger)" data-id="${p.planId}">Delete</button>
                    </td>
                </tr>
            `).join('');
    }

    function resetPlannerForm() {
        $('planner-edit-id').value = '';
        $('planner-form-title').textContent = 'Create planned workout';
        $('planner-form').reset();
        $('pl-minutes').value = 30;
        $('pl-intensity').value = 'Moderate';
        $('pl-submit-btn').textContent = 'Add to schedule';
        $('pl-cancel-edit-btn').classList.add('hidden');
    }

    /* ---------------- 4. GOALS ---------------- */
    async function loadGoals() {
        const g = await api('/api/goals');
        $('g-steps').value = g.dailyStepTarget;
        $('g-water').value = g.dailyWaterTargetMl;
        $('g-weekly').value = g.weeklyWorkoutTarget;
        $('g-calories').value = g.dailyCalorieTarget;
        $('g-minutes').value = g.activeMinutesTarget;
        $('g-distance').value = g.distanceKmTarget;

        const bars = [
            { label: 'Daily steps', pct: g.stepPercent, val: `${formatNum(g.todaySteps)} / ${formatNum(g.dailyStepTarget)}`, cls: 'green' },
            { label: 'Daily hydration', pct: g.waterPercent, val: `${formatNum(g.todayWaterMl)} / ${formatNum(g.dailyWaterTargetMl)} ml`, cls: 'blue' },
            { label: 'Weekly workouts', pct: g.weeklyPercent, val: `${g.weeklyWorkouts} / ${g.weeklyWorkoutTarget}`, cls: 'amber' },
            { label: 'Daily calories', pct: g.caloriePercent, val: `${Math.round(g.todayCalories)} / ${Math.round(g.dailyCalorieTarget)} kcal`, cls: 'green' },
            { label: 'Weekly active minutes', pct: g.minutesPercent, val: `${g.weeklyMinutes} / ${g.activeMinutesTarget} min`, cls: 'green' },
            { label: 'Weekly distance', pct: g.distancePercent, val: `${g.weeklyDistanceKm} / ${g.distanceKmTarget} km`, cls: 'blue' }
        ];
        $('goals-progress-bars').innerHTML = bars.map(b => `
            <div class="progress-item">
                <div class="progress-head">
                    <span><strong>${escapeHtml(b.label)}</strong></span>
                    <span class="tabular">${escapeHtml(b.val)} (${Math.round(b.pct || 0)}%)</span>
                </div>
                <div class="progress-track"><div class="progress-fill ${b.cls}" style="width:${Math.min(100, b.pct || 0)}%"></div></div>
            </div>
        `).join('');

        const hist = g.history || [];
        $('goals-history-tbody').innerHTML = hist.length === 0
            ? '<tr><td colspan="7" class="empty-cell">No goal snapshots saved yet.</td></tr>'
            : hist.map(h => `
                <tr>
                    <td class="tabular">${escapeHtml((h.recordedAt || '').slice(0, 16))}</td>
                    <td class="num">${formatNum(h.dailyStepTarget)}</td>
                    <td class="num">${formatNum(h.dailyWaterTargetMl)}</td>
                    <td class="num">${h.weeklyWorkoutTarget}</td>
                    <td class="num">${Math.round(h.dailyCalorieTarget)}</td>
                    <td class="num">${h.activeMinutesTarget}</td>
                    <td class="num">${Number(h.distanceKmTarget || 0).toFixed(1)}</td>
                </tr>
            `).join('');
    }

    /* ---------------- 5. ACTIVITIES ---------------- */
    async function loadActivities() {
        const res = await api('/api/activities');
        $('act-today-steps').textContent = formatNum(res.todaySteps);
        $('act-step-target').textContent = `Target: ${formatNum(res.dailyStepTarget)} steps`;
        $('act-weekly-steps').textContent = formatNum(res.weeklySteps);
        window.FitTrackCharts.drawBarSeries($('steps-chart'), res.stepSeries || [], 'steps', '#059669');

        const rows = res.records || [];
        $('activities-tbody').innerHTML = rows.length === 0
            ? '<tr><td colspan="6" class="empty-cell">No activity records logged yet.</td></tr>'
            : rows.map(r => `
                <tr>
                    <td class="tabular">${escapeHtml(r.activityDate)}</td>
                    <td>${escapeHtml(r.activitySource || 'Activity')}</td>
                    <td class="num">${formatNum(r.stepsCount)}</td>
                    <td class="num">${r.activeMinutes} min</td>
                    <td class="num">${Math.round(r.caloriesBurned)} kcal</td>
                    <td>
                        ${r.workoutId === 0
                            ? `<button type="button" class="btn btn-ghost btn-sm act-del-btn" style="color:var(--danger)" data-id="${r.recordId}">Delete</button>`
                            : '<span class="muted-meta">Linked to workout</span>'}
                    </td>
                </tr>
            `).join('');
    }

    /* ---------------- 6. WATER ---------------- */
    async function loadWater() {
        const res = await api('/api/water');
        $('water-today-total').textContent = `${formatNum(res.todayMl)} ml`;
        $('water-today-target').textContent = `${formatNum(res.targetMl)} ml`;
        $('water-remaining-text').textContent = `Remaining: ${formatNum(res.remainingMl)} ml`;
        $('water-progress-bar').style.width = `${Math.min(100, res.percent || 0)}%`;
        window.FitTrackCharts.drawBarSeries($('water-chart'), res.weeklySeries || [], 'ml', '#0284C7');

        const entries = res.entries || [];
        $('water-tbody').innerHTML = entries.length === 0
            ? '<tr><td colspan="5" class="empty-cell">No water entries logged yet.</td></tr>'
            : entries.map(e => `
                <tr>
                    <td class="tabular">${escapeHtml(e.intakeDate)}</td>
                    <td class="tabular">${escapeHtml(e.intakeTime || '')}</td>
                    <td class="num">${formatNum(e.amountMl)} ml</td>
                    <td>${escapeHtml(e.note || '-')}</td>
                    <td><button type="button" class="btn btn-ghost btn-sm water-del-btn" style="color:var(--danger)" data-id="${e.waterId}">Delete</button></td>
                </tr>
            `).join('');
    }

    /* ---------------- 7. SLEEP ---------------- */
    async function loadSleep() {
        const res = await api('/api/sleep');
        const ins = res.insights || {};
        $('sleep-avg-label').textContent = `${Number(res.weeklyAvgHours || 0).toFixed(1)} hrs`;
        $('sleep-quality-label').textContent = `${Number(res.weeklyAvgQuality || 0).toFixed(1)} / 5`;
        $('sleep-nights-label').textContent = `${ins.loggedNights || 0} nights`;
        $('sleep-insight-box').textContent = ins.consistencySummary || 'Log sleep entries to review weekly schedule patterns.';

        window.FitTrackCharts.drawBarSeries($('sleep-chart'), res.weeklySeries || [], 'hours', '#7C3AED');

        const logs = res.logs || [];
        $('sleep-tbody').innerHTML = logs.length === 0
            ? '<tr><td colspan="5" class="empty-cell">No sleep logs recorded yet.</td></tr>'
            : logs.map(s => `
                <tr>
                    <td class="tabular">${escapeHtml(s.sleepDate)}</td>
                    <td class="num">${Number(s.hoursSlept).toFixed(1)} hrs</td>
                    <td class="num">${s.qualityRating} / 5</td>
                    <td>${escapeHtml(s.notes || '-')}</td>
                    <td><button type="button" class="btn btn-ghost btn-sm sleep-del-btn" style="color:var(--danger)" data-id="${s.sleepId}">Delete</button></td>
                </tr>
            `).join('');
    }

    /* ---------------- 8. WEIGHT ---------------- */
    async function loadWeight() {
        const qp = new URLSearchParams();
        if ($('wt-from').value) qp.set('from', $('wt-from').value);
        if ($('wt-to').value) qp.set('to', $('wt-to').value);
        const queryStr = qp.toString();
        const res = await api(`/api/weight${queryStr ? `?${queryStr}` : ''}`);
        const list = res.weightHistory || [];

        $('wt-current-val').textContent = `${Number(res.currentWeightKg || 0).toFixed(1)} kg`;
        if (state.user) {
            $('wt-bmi-val').textContent = `${Number(state.user.bmi || 0).toFixed(1)} (${state.user.bmiCategory || ''})`;
        }

        if (list.length >= 2) {
            const ordered = [...list].sort((a, b) => String(a.recordedDate).localeCompare(String(b.recordedDate)));
            const delta = ordered[ordered.length - 1].weightKg - ordered[0].weightKg;
            const sign = delta > 0 ? '+' : '';
            $('wt-change-val').textContent = `${sign}${delta.toFixed(1)} kg`;
        } else {
            $('wt-change-val').textContent = '0.0 kg';
        }

        window.FitTrackCharts.drawWeightTrendChart($('weight-page-chart'), list);

        $('weight-tbody').innerHTML = list.length === 0
            ? '<tr><td colspan="3" class="empty-cell">No weight entries logged in this range.</td></tr>'
            : list.map(w => `
                <tr>
                    <td class="tabular">${escapeHtml(w.recordedDate)}</td>
                    <td class="num">${Number(w.weightKg).toFixed(1)} kg</td>
                    <td class="table-actions">
                        <button type="button" class="btn btn-secondary btn-sm wt-edit-btn" data-id="${w.weightId}" data-kg="${w.weightKg}" data-date="${escapeHtml(w.recordedDate)}">Edit</button>
                        <button type="button" class="btn btn-ghost btn-sm wt-del-btn" style="color:var(--danger)" data-id="${w.weightId}">Delete</button>
                    </td>
                </tr>
            `).join('');
    }

    function resetWeightForm() {
        $('wt-edit-id').value = '';
        $('wt-form-title').textContent = 'Log weight entry';
        $('wt-submit-btn').textContent = 'Save weight';
        $('wt-cancel-edit').classList.add('hidden');
        $('wt-date').value = todayISO();
    }

    /* ---------------- 9. ACHIEVEMENTS ---------------- */
    async function loadAchievements() {
        const res = await api('/api/achievements');
        const streaks = res.streaks || {};
        $('ach-current-streak').textContent = `${streaks.currentStreak || 0} days`;
        $('ach-longest-streak').textContent = `${streaks.longestStreak || 0} days`;
        $('ach-unlocked-count').textContent = `${res.unlockedCount || 0} / ${res.totalCount || 0}`;

        const list = res.achievements || [];
        $('achievements-grid').innerHTML = list.map(a => `
            <article class="badge-card ${a.unlocked ? '' : 'locked'}">
                <div class="badge-card-head">
                    <strong>${escapeHtml(a.title)}</strong>
                    <span class="status-label ${a.unlocked ? 'success' : 'neutral'}">
                        ${a.unlocked ? 'Unlocked' : 'Locked'}
                    </span>
                </div>
                <p class="muted-text">${escapeHtml(a.description)}</p>
                <div>
                    <div class="progress-head">
                        <span class="muted-meta">Progress</span>
                        <span class="muted-meta tabular">${a.currentValue} / ${a.targetValue} (${Math.round(a.progressPercent || 0)}%)</span>
                    </div>
                    <div class="progress-track">
                        <div class="progress-fill ${a.unlocked ? 'green' : 'blue'}" style="width:${Math.min(100, a.progressPercent || 0)}%"></div>
                    </div>
                </div>
                <div class="muted-meta tabular">
                    ${a.unlocked && a.unlockedAt ? `Earned: ${escapeHtml(a.unlockedAt.slice(0, 10))}` : 'In progress'}
                </div>
            </article>
        `).join('');
    }

    /* ---------------- 10. REPORTS ---------------- */
    function renderReportCharts(res) {
        window.FitTrackCharts.drawBarSeries($('weekly-calories-chart'), res.dailySeries || [], 'calories', '#D97706');
        window.FitTrackCharts.drawWorkoutBreakdownChart($('workout-type-chart'), res.typeBreakdown || []);
        window.FitTrackCharts.drawBarSeries($('rep-water-chart'), res.waterSeries || [], 'ml', '#0284C7');
        window.FitTrackCharts.drawBarSeries($('rep-sleep-chart'), res.sleepSeries || [], 'hours', '#7C3AED');
        window.FitTrackCharts.drawWeightTrendChart($('weight-trend-chart'), res.weightHistory || []);
    }

    async function loadReports() {
        const preset = $('rep-preset').value || '7d';
        const qp = new URLSearchParams();
        if (preset !== 'all' && preset !== 'custom') qp.set('range', preset);
        if ($('rep-from').value) qp.set('from', $('rep-from').value);
        if ($('rep-to').value) qp.set('to', $('rep-to').value);
        if ($('rep-search').value.trim()) qp.set('q', $('rep-search').value.trim());

        const res = await api(`/api/reports?${qp.toString()}`);
        state.reportsData = res;

        $('rep-total-sessions').textContent = formatNum(res.totalSessions);
        $('rep-total-minutes').textContent = `${formatNum(res.totalMinutes)} min`;
        $('rep-total-calories').textContent = `${formatNum(Math.round(res.totalCalories || 0))} kcal`;
        $('rep-total-distance').textContent = `${Number(res.totalDistanceKm || 0).toFixed(1)} km`;
        $('rep-count-meta').textContent = `${res.totalSessions || 0} workouts`;

        renderReportCharts(res);

        const rows = res.history || [];
        $('reports-tbody').innerHTML = rows.length === 0
            ? '<tr><td colspan="9" class="empty-cell">No workouts match the selected criteria.</td></tr>'
            : rows.map(w => `
                <tr>
                    <td class="tabular">${escapeHtml(w.workoutDate)}</td>
                    <td><strong>${escapeHtml(w.workoutName || w.workoutType)}</strong></td>
                    <td>${escapeHtml(w.workoutType)}</td>
                    <td class="num">${w.durationMinutes} min</td>
                    <td>${escapeHtml(w.intensity)}</td>
                    <td class="num">${formatNum(w.steps)}</td>
                    <td class="num">${Number(w.distanceKm || 0).toFixed(1)} km</td>
                    <td class="num">${Math.round(w.caloriesBurned)} kcal</td>
                    <td>${escapeHtml(w.notes || '-')}</td>
                </tr>
            `).join('');
    }

    function exportReportsCsv() {
        const rows = (state.reportsData && state.reportsData.history) || [];
        if (rows.length === 0) {
            showToast('No workout rows available to export.', true);
            return;
        }
        const headers = ['Date', 'Workout Name', 'Type', 'Duration (min)', 'Intensity', 'Steps', 'Distance (km)', 'Calories (kcal)', 'Notes'];
        const csvLines = [headers.join(',')];
        rows.forEach(w => {
            const cols = [
                w.workoutDate,
                w.workoutName || w.workoutType,
                w.workoutType,
                w.durationMinutes,
                w.intensity,
                w.steps,
                w.distanceKm,
                w.caloriesBurned,
                w.notes || ''
            ].map(v => `"${String(v ?? '').replace(/"/g, '""')}"`);
            csvLines.push(cols.join(','));
        });
        const blob = new Blob([csvLines.join('\n')], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `fittrack-report-${todayISO()}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showToast('CSV report exported.');
    }

    /* ---------------- 11. PROFILE & 12. SETTINGS ---------------- */
    async function loadProfile() {
        const u = await api('/api/profile');
        state.user = u;
        updateUserChrome();

        $('prof-badge-tag').textContent = u.membershipBadge || 'Regular Member';
        $('prof-badge-tag').className = `status-label ${u.hasPremiumAnalytics ? 'success' : 'info'}`;
        $('prof-bmi-score').textContent = Number(u.bmi || 0).toFixed(1);
        $('prof-bmi-category').textContent = u.bmiCategory || 'Normal';
        $('prof-tier-benefits').textContent = u.membershipBenefits || '';

        $('p-name').value = u.fullName || '';
        $('p-email').value = u.email || '';
        $('p-tier').value = u.accountType || 'REGULAR';
        $('p-preference').value = u.activityPreference || 'Balanced';
        $('p-age').value = u.age || 25;
        $('p-gender').value = u.gender || 'Not Specified';
        $('p-height').value = u.heightCm || 170;
        $('p-weight').value = u.weightKg || 70;
    }

    async function loadSettings() {
        const s = await api('/api/settings');
        state.settings = s;
        applyTheme(s.themeMode);
        $('set-theme').value = s.themeMode || 'light';
        $('set-workout-rem').checked = Boolean(s.workoutReminders);
        $('set-water-rem').checked = Boolean(s.waterReminders);
        $('set-goal-alerts').checked = Boolean(s.goalAlerts);
        $('set-ach-alerts').checked = Boolean(s.achievementAlerts);
        $('set-weekly-digest').checked = Boolean(s.weeklyDigest);
    }

    /* ---------------- EVENT BINDINGS ---------------- */
    function bindEvents() {
        // Auth toggles
        $('go-register-btn').addEventListener('click', () => {
            $('view-login').classList.add('hidden');
            $('view-register').classList.remove('hidden');
        });
        $('go-login-btn').addEventListener('click', () => {
            $('view-register').classList.add('hidden');
            $('view-login').classList.remove('hidden');
        });
        $('fill-demo-btn').addEventListener('click', () => {
            $('login-email').value = 'alex@fittrack.com';
            $('login-password').value = 'FitTrack@123';
        });

        $('login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const resp = await api('/api/auth/login', {
                    method: 'POST',
                    body: JSON.stringify({
                        email: $('login-email').value.trim(),
                        password: $('login-password').value
                    })
                });
                await enterWorkspace(resp);
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('register-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            if ($('reg-password').value !== $('reg-confirm').value) {
                showToast('Passwords do not match.', true);
                return;
            }
            try {
                const resp = await api('/api/auth/register', {
                    method: 'POST',
                    body: JSON.stringify({
                        fullName: $('reg-name').value.trim(),
                        email: $('reg-email').value.trim(),
                        password: $('reg-password').value,
                        accountType: $('reg-tier').value,
                        activityPreference: $('reg-preference').value,
                        age: Number($('reg-age').value),
                        gender: $('reg-gender').value,
                        heightCm: Number($('reg-height').value),
                        weightKg: Number($('reg-weight').value)
                    })
                });
                await enterWorkspace(resp);
            } catch (err) {
                showToast(err.message, true);
            }
        });

        async function doLogout() {
            try {
                await api('/api/auth/logout', { method: 'POST' });
            } catch (_) {
                // ignore
            }
            state.token = null;
            state.user = null;
            stopTimer();
            applyTheme('light');
            $('app-shell').classList.add('hidden');
            $('auth-shell').classList.remove('hidden');
        }
        $('logout-btn').addEventListener('click', doLogout);
        $('popover-logout-btn').addEventListener('click', doLogout);

        // Sidebar & nav links
        document.querySelectorAll('.nav-item[data-page]').forEach(btn => {
            btn.addEventListener('click', () => navigateTo(btn.dataset.page));
        });
        document.addEventListener('click', (e) => {
            const navTrigger = e.target.closest('[data-nav]');
            if (navTrigger) {
                navigateTo(navTrigger.dataset.nav);
            }
        });

        // Mobile drawer
        $('mobile-menu-btn').addEventListener('click', () => {
            $('sidebar').classList.toggle('open');
            $('sidebar-backdrop').classList.toggle('hidden');
        });
        $('sidebar-backdrop').addEventListener('click', closeMobileSidebar);

        // Topbar Search
        $('topbar-search-input').addEventListener('input', (e) => {
            clearTimeout(state.searchTimer);
            const val = e.target.value;
            state.searchTimer = setTimeout(() => {
                runGlobalSearch(val).catch(() => {});
            }, 220);
        });

        $('search-popover').addEventListener('click', async (e) => {
            const wBtn = e.target.closest('.search-open-workout');
            if (wBtn) {
                $('search-popover').classList.add('hidden');
                try {
                    await openWorkoutDetail(Number(wBtn.dataset.id));
                } catch (err) {
                    showToast(err.message, true);
                }
                return;
            }
            if (e.target.closest('.search-open-planner')) {
                $('search-popover').classList.add('hidden');
                await navigateTo('planner');
            }
        });

        // Notification Bell Popover
        $('topbar-notif-btn').addEventListener('click', async (e) => {
            e.stopPropagation();
            const isOpen = !$('notif-popover').classList.contains('hidden');
            closeAllPopovers();
            if (!isOpen) {
                $('notif-popover').classList.remove('hidden');
                try {
                    await loadNotificationsPopover();
                } catch (err) {
                    showToast(err.message, true);
                }
            }
        });

        $('notif-read-all-btn').addEventListener('click', async () => {
            try {
                await api('/api/notifications/read-all', { method: 'POST' });
                await loadNotificationsPopover();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('notif-list').addEventListener('click', async (e) => {
            const readBtn = e.target.closest('.notif-read-one');
            const delBtn = e.target.closest('.notif-del-one');
            try {
                if (readBtn) {
                    await api(`/api/notifications/${readBtn.dataset.id}/read`, { method: 'POST' });
                    await loadNotificationsPopover();
                } else if (delBtn) {
                    await api(`/api/notifications/${delBtn.dataset.id}`, { method: 'DELETE' });
                    await loadNotificationsPopover();
                }
            } catch (err) {
                showToast(err.message, true);
            }
        });

        // Profile Dropdown
        $('topbar-profile-btn').addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = !$('profile-popover').classList.contains('hidden');
            closeAllPopovers();
            if (!isOpen) $('profile-popover').classList.remove('hidden');
        });

        document.addEventListener('click', (e) => {
            if (!e.target.closest('.topbar-actions') && !e.target.closest('.topbar-center')) {
                closeAllPopovers();
            }
        });

        // Quick Add Workout Modal
        function openQuickWorkoutModal() {
            $('qw-date').value = todayISO();
            $('qw-preview-label').textContent = 'Estimated: -- kcal';
            $('quick-workout-modal').classList.remove('hidden');
        }
        $('topbar-quick-workout').addEventListener('click', openQuickWorkoutModal);
        $('dash-quick-workout-btn').addEventListener('click', openQuickWorkoutModal);
        $('qw-close-btn').addEventListener('click', () => $('quick-workout-modal').classList.add('hidden'));
        $('qw-preview-btn').addEventListener('click', () => previewWorkoutCalories('qw').catch(err => showToast(err.message, true)));

        $('quick-workout-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const type = $('qw-type').value;
                await api('/api/workouts', {
                    method: 'POST',
                    body: JSON.stringify({
                        workoutName: $('qw-name').value.trim() || type,
                        workoutType: type,
                        durationMinutes: Number($('qw-duration').value),
                        workoutDate: $('qw-date').value || todayISO(),
                        intensity: $('qw-intensity').value,
                        steps: Number($('qw-steps').value) || 0,
                        distanceKm: Number($('qw-distance').value) || 0,
                        notes: $('qw-notes').value.trim()
                    })
                });
                $('quick-workout-modal').classList.add('hidden');
                $('quick-workout-form').reset();
                showToast('Workout saved.');
                await navigateTo(state.currentPage);
            } catch (err) {
                showToast(err.message, true);
            }
        });

        // Quick Dashboard Buttons
        $('dash-quick-water-btn').addEventListener('click', async () => {
            try {
                await api('/api/water', {
                    method: 'POST',
                    body: JSON.stringify({ amountMl: 250, intakeDate: todayISO(), note: 'Quick hydration' })
                });
                showToast('+250 ml water logged.');
                await loadDashboard();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('dash-quick-steps-btn').addEventListener('click', async () => {
            try {
                await api('/api/steps', {
                    method: 'POST',
                    body: JSON.stringify({ steps: 1500, date: todayISO(), activitySource: 'Quick Step Log' })
                });
                showToast('+1,500 steps logged.');
                await loadDashboard();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('dash-recommendations').addEventListener('click', (e) => {
            if (e.target.closest('.quick-rec-btn')) {
                openQuickWorkoutModal();
            }
        });

        // Dashboard Range & Metric Controls
        $('dash-range-bar').addEventListener('click', async (e) => {
            const btn = e.target.closest('.range-btn');
            if (!btn) return;
            const range = btn.dataset.range;
            if (range === 'custom') {
                $('dash-custom-range').classList.remove('hidden');
                document.querySelectorAll('#dash-range-bar .range-btn').forEach(b => b.classList.toggle('active', b === btn));
                state.dashRange = 'custom';
                return;
            }
            $('dash-custom-range').classList.add('hidden');
            const prevRange = state.dashRange;
            state.dashRange = range;
            try {
                await loadDashboard();
                document.querySelectorAll('#dash-range-bar .range-btn').forEach(b => b.classList.toggle('active', b === btn));
            } catch (err) {
                state.dashRange = prevRange;
                showToast(err.message, true);
            }
        });

        $('dash-custom-apply').addEventListener('click', () => {
            loadDashboard().catch(err => showToast(err.message, true));
        });

        $('dash-metric-tabs').addEventListener('click', (e) => {
            const btn = e.target.closest('.metric-tab-btn');
            if (!btn) return;
            state.dashMetric = btn.dataset.metric;
            document.querySelectorAll('#dash-metric-tabs .metric-tab-btn').forEach(b => b.classList.toggle('active', b === btn));
            if (state.dashData) renderDashboardCharts(state.dashData);
        });

        $('dash-recent-tbody').addEventListener('click', (e) => {
            const row = e.target.closest('tr[data-workout-id]');
            if (row) {
                openWorkoutDetail(Number(row.dataset.workoutId)).catch(err => showToast(err.message, true));
            }
        });

        // Workout Detail Modal
        $('wd-close-btn').addEventListener('click', () => $('workout-detail-modal').classList.add('hidden'));
        $('wd-edit-btn').addEventListener('click', async () => {
            const w = state.activeWorkoutDetail;
            if (!w) return;
            $('workout-detail-modal').classList.add('hidden');
            await navigateTo('workouts');
            beginEditWorkout(w);
        });
        $('wd-delete-btn').addEventListener('click', () => {
            const w = state.activeWorkoutDetail;
            if (!w) return;
            openConfirmModal({
                title: 'Delete Workout',
                message: `Remove "${w.workoutName || w.workoutType}" (${w.workoutDate})?`,
                onConfirm: async () => {
                    await api(`/api/workouts/${w.workoutId}`, { method: 'DELETE' });
                    $('workout-detail-modal').classList.add('hidden');
                    showToast('Workout deleted.');
                    await navigateTo(state.currentPage);
                }
            });
        });

        // Timer
        document.querySelectorAll('.timer-mode-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                stopTimer();
                state.timer.mode = btn.dataset.mode;
                document.querySelectorAll('.timer-mode-btn').forEach(b => b.classList.toggle('active', b === btn));
                $('timer-countdown-config').classList.toggle('hidden', state.timer.mode !== 'countdown');
                const mins = Math.max(1, Number($('timer-minutes-input').value) || 20);
                state.timer.remainingSec = mins * 60;
                state.timer.elapsedSec = 0;
                updateTimerUI();
            });
        });
        $('timer-minutes-input').addEventListener('change', () => {
            if (state.timer.mode === 'countdown' && !state.timer.running) {
                state.timer.remainingSec = Math.max(1, Number($('timer-minutes-input').value) || 20) * 60;
                updateTimerUI();
            }
        });
        $('timer-start-btn').addEventListener('click', () => {
            if (state.timer.running) return;
            state.timer.running = true;
            state.timer.intervalId = setInterval(() => {
                if (state.timer.mode === 'stopwatch') {
                    state.timer.elapsedSec += 1;
                } else {
                    state.timer.remainingSec = Math.max(0, state.timer.remainingSec - 1);
                    if (state.timer.remainingSec === 0) {
                        stopTimer();
                        showToast('Countdown completed!');
                    }
                }
                updateTimerUI();
            }, 1000);
        });
        $('timer-pause-btn').addEventListener('click', stopTimer);
        $('timer-reset-btn').addEventListener('click', () => {
            stopTimer();
            state.timer.elapsedSec = 0;
            const mins = Math.max(1, Number($('timer-minutes-input').value) || 20);
            state.timer.remainingSec = mins * 60;
            updateTimerUI();
        });
        $('timer-apply-btn').addEventListener('click', () => {
            let mins;
            if (state.timer.mode === 'stopwatch') {
                mins = Math.max(1, Math.round(state.timer.elapsedSec / 60));
            } else {
                const total = Math.max(1, Number($('timer-minutes-input').value) || 20) * 60;
                mins = Math.max(1, Math.round((total - state.timer.remainingSec) / 60) || Number($('timer-minutes-input').value));
            }
            $('w-duration').value = mins;
            showToast(`Applied ${mins} min to workout duration.`);
        });

        // Workouts Form & Filters
        $('w-preview-btn').addEventListener('click', () => previewWorkoutCalories('w').catch(err => showToast(err.message, true)));
        $('w-cancel-edit-btn').addEventListener('click', resetWorkoutForm);
        $('workout-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const editId = $('workout-edit-id').value;
            const type = $('w-type').value;
            const payload = {
                workoutName: $('w-name').value.trim() || type,
                workoutType: type,
                durationMinutes: Number($('w-duration').value),
                workoutDate: $('w-date').value || todayISO(),
                intensity: $('w-intensity').value,
                steps: Number($('w-steps').value) || 0,
                distanceKm: Number($('w-distance').value) || 0,
                notes: $('w-notes').value.trim()
            };
            try {
                if (editId) {
                    await api(`/api/workouts/${editId}`, { method: 'PUT', body: JSON.stringify(payload) });
                    showToast('Workout updated.');
                } else {
                    await api('/api/workouts', { method: 'POST', body: JSON.stringify(payload) });
                    showToast('Workout saved.');
                }
                resetWorkoutForm();
                await loadWorkouts();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('workouts-filter-form').addEventListener('submit', (e) => {
            e.preventDefault();
            loadWorkouts().catch(err => showToast(err.message, true));
        });
        $('wf-reset-btn').addEventListener('click', () => {
            $('workouts-filter-form').reset();
            $('wf-sort').value = 'date:desc';
            loadWorkouts().catch(err => showToast(err.message, true));
        });

        $('workouts-tbody').addEventListener('click', async (e) => {
            const viewBtn = e.target.closest('.w-view-btn');
            const editBtn = e.target.closest('.w-edit-btn');
            const delBtn = e.target.closest('.w-del-btn');
            if (viewBtn) {
                openWorkoutDetail(Number(viewBtn.dataset.id)).catch(err => showToast(err.message, true));
            } else if (editBtn) {
                const w = state.workouts.find(x => x.workoutId === Number(editBtn.dataset.id));
                if (w) beginEditWorkout(w);
            } else if (delBtn) {
                const id = Number(delBtn.dataset.id);
                openConfirmModal({
                    title: 'Delete Workout',
                    message: 'Are you sure you want to delete this workout record?',
                    onConfirm: async () => {
                        await api(`/api/workouts/${id}`, { method: 'DELETE' });
                        showToast('Workout deleted.');
                        await loadWorkouts();
                    }
                });
            }
        });

        // Planner
        $('pl-cancel-edit-btn').addEventListener('click', resetPlannerForm);
        $('planner-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const editId = $('planner-edit-id').value;
            const type = $('pl-type').value;
            const payload = {
                dayOfWeek: $('pl-day').value,
                workoutName: $('pl-name').value.trim() || type,
                workoutType: type,
                plannedMinutes: Number($('pl-minutes').value),
                intensity: $('pl-intensity').value,
                notes: $('pl-notes').value.trim()
            };
            try {
                if (editId) {
                    await api(`/api/plans/${editId}`, { method: 'PUT', body: JSON.stringify(payload) });
                    showToast('Planned session updated.');
                } else {
                    await api('/api/plans', { method: 'POST', body: JSON.stringify(payload) });
                    showToast('Session added to weekly schedule.');
                }
                resetPlannerForm();
                await loadPlanner();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('planner-tbody').addEventListener('click', async (e) => {
            const completeBtn = e.target.closest('.pl-complete-btn');
            const editBtn = e.target.closest('.pl-edit-btn');
            const delBtn = e.target.closest('.pl-del-btn');
            try {
                if (completeBtn) {
                    await api(`/api/plans/${completeBtn.dataset.id}/complete`, { method: 'POST' });
                    showToast('Session completed and logged to My Workouts.');
                    await loadPlanner();
                } else if (editBtn) {
                    const p = state.plans.find(x => x.planId === Number(editBtn.dataset.id));
                    if (p) {
                        $('planner-edit-id').value = String(p.planId);
                        $('planner-form-title').textContent = 'Edit planned workout';
                        $('pl-day').value = p.dayOfWeek;
                        $('pl-name').value = p.workoutName || p.workoutType;
                        $('pl-type').value = p.workoutType;
                        $('pl-minutes').value = p.plannedMinutes;
                        $('pl-intensity').value = p.intensity;
                        $('pl-notes').value = p.notes || '';
                        $('pl-submit-btn').textContent = 'Update schedule';
                        $('pl-cancel-edit-btn').classList.remove('hidden');
                    }
                } else if (delBtn) {
                    const id = Number(delBtn.dataset.id);
                    openConfirmModal({
                        title: 'Remove Planned Session',
                        message: 'Delete this scheduled session from your weekly planner?',
                        onConfirm: async () => {
                            await api(`/api/plans/${id}`, { method: 'DELETE' });
                            showToast('Planned session removed.');
                            await loadPlanner();
                        }
                    });
                }
            } catch (err) {
                showToast(err.message, true);
            }
        });

        // Goals
        $('suggest-goals-btn').addEventListener('click', async () => {
            try {
                const s = await api('/api/goals/suggest');
                $('g-steps').value = s.dailyStepTarget;
                $('g-water').value = s.dailyWaterTargetMl;
                $('g-weekly').value = s.weeklyWorkoutTarget;
                $('g-calories').value = s.dailyCalorieTarget;
                $('g-minutes').value = s.activeMinutesTarget;
                $('g-distance').value = s.distanceKmTarget;
                showToast('Suggested targets loaded. Click Save goals to apply.');
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('goals-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                await api('/api/goals', {
                    method: 'PUT',
                    body: JSON.stringify({
                        dailyStepTarget: Number($('g-steps').value),
                        dailyWaterTargetMl: Number($('g-water').value),
                        weeklyWorkoutTarget: Number($('g-weekly').value),
                        dailyCalorieTarget: Number($('g-calories').value),
                        activeMinutesTarget: Number($('g-minutes').value),
                        distanceKmTarget: Number($('g-distance').value)
                    })
                });
                showToast('Fitness goals updated.');
                await loadGoals();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        // Activities
        document.querySelectorAll('.step-preset-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                try {
                    const steps = Number(btn.dataset.steps);
                    await api('/api/steps', {
                        method: 'POST',
                        body: JSON.stringify({ steps, date: todayISO(), activitySource: 'Quick Preset' })
                    });
                    showToast(`+${formatNum(steps)} steps added.`);
                    await loadActivities();
                } catch (err) {
                    showToast(err.message, true);
                }
            });
        });

        $('activity-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                await api('/api/steps', {
                    method: 'POST',
                    body: JSON.stringify({
                        steps: Number($('act-steps-input').value),
                        date: $('act-date-input').value || todayISO(),
                        activitySource: $('act-source-input').value.trim() || 'Daily Walk'
                    })
                });
                showToast('Activity logged.');
                await loadActivities();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('activities-tbody').addEventListener('click', (e) => {
            const delBtn = e.target.closest('.act-del-btn');
            if (!delBtn) return;
            const id = Number(delBtn.dataset.id);
            openConfirmModal({
                title: 'Delete Activity Record',
                message: 'Remove this manual step entry?',
                onConfirm: async () => {
                    await api(`/api/activities/${id}`, { method: 'DELETE' });
                    showToast('Activity record deleted.');
                    await loadActivities();
                }
            });
        });

        // Water
        document.querySelectorAll('.water-preset-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                try {
                    const ml = Number(btn.dataset.ml);
                    await api('/api/water', {
                        method: 'POST',
                        body: JSON.stringify({ amountMl: ml, intakeDate: todayISO(), note: 'Quick hydration' })
                    });
                    showToast(`+${ml} ml logged.`);
                    await loadWater();
                } catch (err) {
                    showToast(err.message, true);
                }
            });
        });

        $('water-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                await api('/api/water', {
                    method: 'POST',
                    body: JSON.stringify({
                        amountMl: Number($('water-amount').value),
                        intakeDate: $('water-date').value || todayISO(),
                        note: $('water-note').value.trim() || 'Hydration log'
                    })
                });
                $('water-note').value = '';
                showToast('Water entry saved.');
                await loadWater();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('water-tbody').addEventListener('click', (e) => {
            const delBtn = e.target.closest('.water-del-btn');
            if (!delBtn) return;
            const id = Number(delBtn.dataset.id);
            openConfirmModal({
                title: 'Delete Hydration Entry',
                message: 'Remove this water intake entry?',
                onConfirm: async () => {
                    await api(`/api/water/${id}`, { method: 'DELETE' });
                    showToast('Water entry deleted.');
                    await loadWater();
                }
            });
        });

        // Sleep
        $('sleep-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                await api('/api/sleep', {
                    method: 'POST',
                    body: JSON.stringify({
                        sleepDate: $('s-date').value || todayISO(),
                        hoursSlept: Number($('s-hours').value),
                        qualityRating: Number($('s-quality').value),
                        notes: $('s-notes').value.trim()
                    })
                });
                $('s-notes').value = '';
                showToast('Sleep log saved.');
                await loadSleep();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('sleep-tbody').addEventListener('click', (e) => {
            const delBtn = e.target.closest('.sleep-del-btn');
            if (!delBtn) return;
            const id = Number(delBtn.dataset.id);
            openConfirmModal({
                title: 'Delete Sleep Log',
                message: 'Remove this sleep entry?',
                onConfirm: async () => {
                    await api(`/api/sleep/${id}`, { method: 'DELETE' });
                    showToast('Sleep log deleted.');
                    await loadSleep();
                }
            });
        });

        // Weight
        $('wt-cancel-edit').addEventListener('click', resetWeightForm);
        $('weight-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const editId = $('wt-edit-id').value;
            const payload = {
                weightKg: Number($('wt-kg').value),
                recordedDate: $('wt-date').value || todayISO()
            };
            try {
                if (editId) {
                    await api(`/api/weight/${editId}`, { method: 'PUT', body: JSON.stringify(payload) });
                    showToast('Weight entry updated.');
                } else {
                    const res = await api('/api/weight', { method: 'POST', body: JSON.stringify(payload) });
                    if (res.user) state.user = res.user;
                    showToast('Weight entry saved.');
                }
                resetWeightForm();
                await loadWeight();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('weight-filter-form').addEventListener('submit', (e) => {
            e.preventDefault();
            loadWeight().catch(err => showToast(err.message, true));
        });
        $('wt-filter-reset').addEventListener('click', () => {
            $('wt-from').value = '';
            $('wt-to').value = '';
            loadWeight().catch(err => showToast(err.message, true));
        });

        $('weight-tbody').addEventListener('click', (e) => {
            const editBtn = e.target.closest('.wt-edit-btn');
            const delBtn = e.target.closest('.wt-del-btn');
            if (editBtn) {
                $('wt-edit-id').value = editBtn.dataset.id;
                $('wt-kg').value = editBtn.dataset.kg;
                $('wt-date').value = editBtn.dataset.date;
                $('wt-form-title').textContent = 'Edit weight entry';
                $('wt-submit-btn').textContent = 'Update weight';
                $('wt-cancel-edit').classList.remove('hidden');
            } else if (delBtn) {
                const id = Number(delBtn.dataset.id);
                openConfirmModal({
                    title: 'Delete Weight Entry',
                    message: 'Remove this body weight record?',
                    onConfirm: async () => {
                        await api(`/api/weight/${id}`, { method: 'DELETE' });
                        showToast('Weight entry deleted.');
                        await loadWeight();
                    }
                });
            }
        });

        // Reports
        $('report-filter-form').addEventListener('submit', (e) => {
            e.preventDefault();
            loadReports().catch(err => showToast(err.message, true));
        });
        $('rep-preset').addEventListener('change', () => {
            const val = $('rep-preset').value;
            if (val !== 'custom') {
                $('rep-from').value = '';
                $('rep-to').value = '';
                loadReports().catch(err => showToast(err.message, true));
            }
        });
        $('rep-reset-btn').addEventListener('click', () => {
            $('report-filter-form').reset();
            $('rep-preset').value = '7d';
            loadReports().catch(err => showToast(err.message, true));
        });
        $('export-csv-btn').addEventListener('click', exportReportsCsv);
        $('print-report-btn').addEventListener('click', () => window.print());

        // Profile
        $('profile-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const updated = await api('/api/profile', {
                    method: 'PUT',
                    body: JSON.stringify({
                        fullName: $('p-name').value.trim(),
                        accountType: $('p-tier').value,
                        activityPreference: $('p-preference').value,
                        age: Number($('p-age').value),
                        gender: $('p-gender').value,
                        heightCm: Number($('p-height').value),
                        weightKg: Number($('p-weight').value)
                    })
                });
                state.user = updated;
                updateUserChrome();
                await loadProfile();
                showToast('Profile saved.');
            } catch (err) {
                showToast(err.message, true);
            }
        });

        // Settings
        $('set-theme').addEventListener('change', () => applyTheme($('set-theme').value));
        $('settings-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const saved = await api('/api/settings', {
                    method: 'PUT',
                    body: JSON.stringify({
                        themeMode: $('set-theme').value,
                        workoutReminders: $('set-workout-rem').checked,
                        waterReminders: $('set-water-rem').checked,
                        goalAlerts: $('set-goal-alerts').checked,
                        achievementAlerts: $('set-ach-alerts').checked,
                        weeklyDigest: $('set-weekly-digest').checked
                    })
                });
                state.settings = saved;
                applyTheme(saved.themeMode);
                showToast('Preferences saved.');
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('password-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            if ($('pw-new').value !== $('pw-confirm').value) {
                showToast('New passwords do not match.', true);
                return;
            }
            try {
                await api('/api/profile/password', {
                    method: 'PUT',
                    body: JSON.stringify({
                        currentPassword: $('pw-current').value,
                        newPassword: $('pw-new').value
                    })
                });
                $('password-form').reset();
                showToast('Password updated.');
            } catch (err) {
                showToast(err.message, true);
            }
        });

        $('delete-account-btn').addEventListener('click', () => {
            openConfirmModal({
                title: 'Delete Account Permanently',
                message: 'This action cannot be undone. Enter your current password to delete your account and all fitness history.',
                confirmText: 'Delete Account',
                requirePassword: true,
                onConfirm: async (password) => {
                    await api('/api/profile', {
                        method: 'DELETE',
                        body: JSON.stringify({ password })
                    });
                    state.token = null;
                    state.user = null;
                    applyTheme('light');
                    $('app-shell').classList.add('hidden');
                    $('auth-shell').classList.remove('hidden');
                    showToast('Account deleted.');
                }
            });
        });

        // Confirm modal buttons
        $('cm-cancel-top').addEventListener('click', closeConfirmModal);
        $('cm-cancel-btn').addEventListener('click', closeConfirmModal);
        $('cm-confirm-btn').addEventListener('click', async () => {
            const fn = state.confirmAction;
            if (!fn) {
                closeConfirmModal();
                return;
            }
            const pw = $('cm-password-input').value;
            try {
                await fn(pw);
                closeConfirmModal();
            } catch (err) {
                showToast(err.message, true);
            }
        });

        window.addEventListener('resize', () => redrawVisibleCharts());
    }

    bindEvents();
})();
