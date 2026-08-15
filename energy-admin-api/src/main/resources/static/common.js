/**
 * 公共 JS 工具 —— 通知提示 + 工具中文名运行时映射。
 * 所有 admin 页面通过 <script src="common.js"></script> 引入。
 */

/**
 * 工具名 → 中文名 运行时映射（从 /api/tool 接口动态加载，不硬编码）。
 */
let TOOL_CN_NAMES = {};
let _toolMapLoaded = false;

/**
 * 异步加载工具映射（页面初始化时调用一次）。需要传入 apiBaseUrl。
 */
async function loadToolCnNames(apiBaseUrl, authHeader) {
    if (_toolMapLoaded || !apiBaseUrl) return;
    try {
        const headers = {};
        if (authHeader) headers['Authorization'] = authHeader;
        const resp = await fetch(apiBaseUrl.replace(/\/$/, '') + '/api/tool', {headers});
        const tools = await resp.json();
        if (Array.isArray(tools)) {
            tools.forEach(t => {
                if (t.toolName) TOOL_CN_NAMES[t.toolName] = t.cnName || t.toolName;
            });
        }
        _toolMapLoaded = true;
    } catch (e) {
        console.warn('loadToolCnNames failed:', e.message);
    }
}

/**
 * 给工具名加中文标注，例：getCurrentDateTime → getCurrentDateTime（获取当前时间）
 */
function toolDisplayName(name) {
    const cn = TOOL_CN_NAMES[name];
    return cn && cn !== name ? `${name}（${cn}）` : name;
}

/**
 * 同步加载版本（用于已确保映射加载完成的场景）
 */
function toolDisplayNameSync(name) {
    return toolDisplayName(name);
}

/**
 * 轻量 Toast 通知（替代 alert）
 * @param {string} message 消息
 * @param {string} type success | error | warning | info
 * @param {number} duration 自动消失毫秒，默认 2500
 */
function showToast(message, type = 'info', duration = 2500) {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const icons = {success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️'};
    const toast = document.createElement('div');
    toast.className = `notification ${type}`;
    toast.innerHTML = `<span>${icons[type] || ''}</span><span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('hide');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

/**
 * 优雅的确认对话框（替代 confirm）—— 返回 Promise<boolean>
 * 用法：if (await confirmDialog('确定保存？')) { ... }
 */
function confirmDialog(message, title = '确认操作') {
    return new Promise(resolve => {
        // 复用 theme.css 的 .modal 样式
        const overlay = document.createElement('div');
        overlay.className = 'modal';
        overlay.style.display = 'flex';
        overlay.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h2>${title}</h2>
                    <span class="close">&times;</span>
                </div>
                <div class="modal-body">
                    <p style="font-size:0.95rem;line-height:1.6;">${message}</p>
                </div>
                <div class="modal-footer">
                    <button class="btn" data-act="cancel">取消</button>
                    <button class="btn btn-primary" data-act="ok">确定</button>
                </div>
            </div>`;
        document.body.appendChild(overlay);
        const close = (result) => {
            overlay.remove();
            resolve(result);
        };
        overlay.querySelector('.close').onclick = () => close(false);
        overlay.querySelector('[data-act="cancel"]').onclick = () => close(false);
        overlay.querySelector('[data-act="ok"]').onclick = () => close(true);
        overlay.addEventListener('click', e => {
            if (e.target === overlay) close(false);
        });
    });
}

/**
 * 优雅的输入对话框（替代 prompt）—— 支持多字段，返回 Promise<obj|null>
 * 用法：const r = await promptDialog('编辑', [
 *          { key:'cnName', label:'中文名', value: oldVal },
 *          { key:'desc', label:'描述', value: oldDesc }
 *       ]);
 *       if (r) { ... r.cnName ... }
 */
function promptDialog(title, fields) {
    return new Promise(resolve => {
        const overlay = document.createElement('div');
        overlay.className = 'modal';
        overlay.style.display = 'flex';
        let fieldHtml = '';
        for (const f of fields) {
            const val = (f.value || '').replace(/"/g, '&quot;');
            fieldHtml += `<div class="form-group" style="margin-bottom:14px;">
                <label>${f.label}</label>
                <input type="text" id="pd_${f.key}" value="${val}" placeholder="${f.placeholder || ''}" style="width:100%;">
            </div>`;
        }
        overlay.innerHTML = `
            <div class="modal-content">
                <div class="modal-header">
                    <h2>${title}</h2>
                    <span class="close">&times;</span>
                </div>
                <div class="modal-body" style="min-width:420px;">
                    ${fieldHtml}
                </div>
                <div class="modal-footer">
                    <button class="btn" data-act="cancel">取消</button>
                    <button class="btn btn-primary" data-act="ok">确定</button>
                </div>
            </div>`;
        document.body.appendChild(overlay);
        const firstInput = overlay.querySelector('input');
        if (firstInput) {
            firstInput.focus();
            firstInput.select();
        }
        const close = (result) => {
            overlay.remove();
            resolve(result);
        };
        const submit = () => {
            const result = {};
            for (const f of fields) {
                result[f.key] = overlay.querySelector('#pd_' + f.key).value.trim();
            }
            close(result);
        };
        overlay.querySelector('.close').onclick = () => close(null);
        overlay.querySelector('[data-act="cancel"]').onclick = () => close(null);
        overlay.querySelector('[data-act="ok"]').onclick = submit;
        overlay.addEventListener('click', e => {
            if (e.target === overlay) close(null);
        });
        overlay.querySelectorAll('input').forEach(input => {
            input.addEventListener('keydown', e => {
                if (e.key === 'Enter') submit();
            });
        });
    });
}
