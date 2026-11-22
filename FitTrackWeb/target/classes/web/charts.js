function getChartPalette() {
    const isDark = document.documentElement.classList.contains('dark');
    return {
        text: isDark ? '#94A3B8' : '#64748B',
        textStrong: isDark ? '#F1F5F9' : '#0F172A',
        grid: isDark ? '#263244' : '#E2E8F0',
        surface: isDark ? '#161F30' : '#FFFFFF',
        green: isDark ? '#10B981' : '#059669',
        blue: isDark ? '#38BDF8' : '#0284C7',
        amber: isDark ? '#FBBF24' : '#D97706',
        violet: isDark ? '#A78BFA' : '#7C3AED',
        slate: isDark ? '#64748B' : '#475569'
    };
}

function setupCanvas(canvas, defaultW, defaultH) {
    if (!canvas) return null;
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const w = Math.max(defaultW || 300, Math.floor(rect.width || defaultW || 300));
    const h = defaultH || parseInt(canvas.getAttribute('height') || '220', 10);
    canvas.width = w * dpr;
    canvas.height = h * dpr;
    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);
    return { ctx, w, h };
}

function drawEmptyState(ctx, w, h, text) {
    const p = getChartPalette();
    ctx.fillStyle = p.text;
    ctx.font = '13px "DM Sans", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, w / 2, h / 2);
}

window.FitTrackCharts = {
    drawSparkline(canvas, values, colorHex) {
        if (!canvas) return;
        const dpr = window.devicePixelRatio || 1;
        const rect = canvas.getBoundingClientRect();
        const w = Math.max(120, Math.floor(rect.width || 180));
        const h = 36;
        canvas.width = w * dpr;
        canvas.height = h * dpr;
        const ctx = canvas.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, w, h);

        const nums = (values || []).map(v => Number(v) || 0);
        if (nums.length < 2) {
            return;
        }
        const min = Math.min(...nums);
        const max = Math.max(...nums);
        const span = max - min || 1;
        const padX = 3;
        const padY = 5;
        const chartW = w - padX * 2;
        const chartH = h - padY * 2;

        ctx.beginPath();
        nums.forEach((v, idx) => {
            const x = padX + (idx / (nums.length - 1)) * chartW;
            const y = padY + chartH - ((v - min) / span) * chartH;
            if (idx === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });
        ctx.strokeStyle = colorHex || getChartPalette().green;
        ctx.lineWidth = 2;
        ctx.lineJoin = 'round';
        ctx.stroke();

        // Soft area fill under sparkline
        ctx.lineTo(padX + chartW, h - 2);
        ctx.lineTo(padX, h - 2);
        ctx.closePath();
        ctx.globalAlpha = 0.12;
        ctx.fillStyle = colorHex || getChartPalette().green;
        ctx.fill();
        ctx.globalAlpha = 1.0;
    },

    drawBarSeries(canvas, series, valueKey, colorHex, unitLabel) {
        const setup = setupCanvas(canvas, 500, 230);
        if (!setup) return;
        const { ctx, w, h } = setup;
        const p = getChartPalette();

        if (!series || series.length === 0) {
            drawEmptyState(ctx, w, h, 'No activity logged in this date range');
            return;
        }

        const values = series.map(item => Number(item[valueKey]) || 0);
        const maxVal = Math.max(1, ...values);
        const pad = { top: 22, right: 14, bottom: 32, left: 48 };
        const chartW = w - pad.left - pad.right;
        const chartH = h - pad.top - pad.bottom;

        // Horizontal gridlines
        ctx.strokeStyle = p.grid;
        ctx.lineWidth = 1;
        for (let i = 0; i <= 4; i++) {
            const y = pad.top + (chartH / 4) * i;
            ctx.beginPath();
            ctx.moveTo(pad.left, y);
            ctx.lineTo(w - pad.right, y);
            ctx.stroke();

            const tickVal = Math.round(maxVal * (1 - i / 4));
            ctx.fillStyle = p.text;
            ctx.font = '11px "JetBrains Mono", monospace';
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            ctx.fillText(String(tickVal), pad.left - 8, y);
        }

        const step = chartW / series.length;
        const barW = Math.min(34, Math.max(6, step * 0.58));
        const labelEvery = series.length > 14 ? Math.ceil(series.length / 7) : 1;

        series.forEach((item, idx) => {
            const val = Number(item[valueKey]) || 0;
            const barH = (val / maxVal) * chartH;
            const x = pad.left + idx * step + (step - barW) / 2;
            const y = pad.top + chartH - barH;

            ctx.fillStyle = colorHex || p.green;
            ctx.beginPath();
            const radius = Math.min(4, barW / 2, barH);
            if (barH > 0) {
                ctx.roundRect(x, y, barW, barH, [radius, radius, 0, 0]);
                ctx.fill();
            }

            if (series.length <= 10 && val > 0) {
                ctx.fillStyle = p.textStrong;
                ctx.font = '10px "JetBrains Mono", monospace';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'bottom';
                ctx.fillText(`${Math.round(val * 10) / 10}${unitLabel ? '' : ''}`, x + barW / 2, y - 3);
            }

            if (idx % labelEvery === 0 || idx === series.length - 1) {
                const label = (item.date || '').slice(5);
                ctx.fillStyle = p.text;
                ctx.font = '10px "JetBrains Mono", monospace';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'top';
                ctx.fillText(label, x + barW / 2, pad.top + chartH + 8);
            }
        });
    },

    drawWorkoutBreakdownChart(canvas, breakdown) {
        const setup = setupCanvas(canvas, 420, 230);
        if (!setup) return;
        const { ctx, w, h } = setup;
        const p = getChartPalette();

        if (!breakdown || breakdown.length === 0) {
            drawEmptyState(ctx, w, h, 'Log workouts to view type distribution');
            return;
        }

        const colors = [p.green, p.blue, p.amber, p.violet, '#EC4899', '#14B8A6', '#F97316', '#6366F1'];
        const maxCal = Math.max(1, ...breakdown.map(b => Number(b.calories) || 0));
        const visible = breakdown.slice(0, 7);
        const rowH = Math.min(30, Math.floor((h - 20) / visible.length));
        const padLeft = 118;
        const padRight = 84;
        const barMaxW = Math.max(60, w - padLeft - padRight);

        visible.forEach((item, idx) => {
            const y = 14 + idx * rowH;
            const cal = Number(item.calories) || 0;
            const barW = Math.max(4, (cal / maxCal) * barMaxW);

            ctx.fillStyle = p.textStrong;
            ctx.font = '12px "DM Sans", sans-serif';
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            const label = String(item.workoutType || '').slice(0, 16);
            ctx.fillText(label, padLeft - 10, y + 10);

            ctx.fillStyle = p.grid;
            ctx.beginPath();
            ctx.roundRect(padLeft, y + 3, barMaxW, 14, 4);
            ctx.fill();

            ctx.fillStyle = colors[idx % colors.length];
            ctx.beginPath();
            ctx.roundRect(padLeft, y + 3, barW, 14, 4);
            ctx.fill();

            ctx.fillStyle = p.text;
            ctx.font = '11px "JetBrains Mono", monospace';
            ctx.textAlign = 'left';
            ctx.fillText(`${Math.round(cal)} kcal`, padLeft + barMaxW + 8, y + 10);
        });
    },

    drawWeightTrendChart(canvas, entries) {
        const setup = setupCanvas(canvas, 760, 220);
        if (!setup) return;
        const { ctx, w, h } = setup;
        const p = getChartPalette();

        if (!entries || entries.length === 0) {
            drawEmptyState(ctx, w, h, 'Log body weight entries to visualize trend');
            return;
        }

        const ordered = [...entries].sort((a, b) => String(a.recordedDate).localeCompare(String(b.recordedDate)));
        const weights = ordered.map(e => Number(e.weightKg) || 0);
        const minW = Math.floor(Math.min(...weights) - 1);
        const maxW = Math.ceil(Math.max(...weights) + 1);
        const span = Math.max(1, maxW - minW);

        const pad = { top: 20, right: 24, bottom: 32, left: 48 };
        const chartW = w - pad.left - pad.right;
        const chartH = h - pad.top - pad.bottom;

        ctx.strokeStyle = p.grid;
        ctx.lineWidth = 1;
        for (let i = 0; i <= 4; i++) {
            const y = pad.top + (chartH / 4) * i;
            ctx.beginPath();
            ctx.moveTo(pad.left, y);
            ctx.lineTo(w - pad.right, y);
            ctx.stroke();

            const tick = (maxW - (span / 4) * i).toFixed(1);
            ctx.fillStyle = p.text;
            ctx.font = '11px "JetBrains Mono", monospace';
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            ctx.fillText(tick, pad.left - 8, y);
        }

        const points = ordered.map((e, idx) => {
            const x = ordered.length === 1
                ? pad.left + chartW / 2
                : pad.left + (idx / (ordered.length - 1)) * chartW;
            const y = pad.top + chartH - (((Number(e.weightKg) - minW) / span) * chartH);
            return { x, y, label: String(e.recordedDate).slice(5), weight: Number(e.weightKg) };
        });

        if (points.length > 1) {
            ctx.beginPath();
            points.forEach((pt, idx) => {
                if (idx === 0) ctx.moveTo(pt.x, pt.y);
                else ctx.lineTo(pt.x, pt.y);
            });
            ctx.strokeStyle = p.blue;
            ctx.lineWidth = 2.5;
            ctx.lineJoin = 'round';
            ctx.stroke();
        }

        const stepLabel = points.length > 10 ? Math.ceil(points.length / 7) : 1;
        points.forEach((pt, idx) => {
            ctx.beginPath();
            ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2);
            ctx.fillStyle = p.surface;
            ctx.fill();
            ctx.lineWidth = 2.2;
            ctx.strokeStyle = p.blue;
            ctx.stroke();

            if (idx % stepLabel === 0 || idx === points.length - 1) {
                ctx.fillStyle = p.text;
                ctx.font = '10px "JetBrains Mono", monospace';
                ctx.textAlign = 'center';
                ctx.textBaseline = 'top';
                ctx.fillText(pt.label, pt.x, pad.top + chartH + 8);
            }
        });
    }
};
