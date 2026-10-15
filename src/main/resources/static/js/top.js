/* =========================================================
   top.js
   配置場所：src/main/resources/static/js/top.js
   対象画面：shops.html（トップ画面）

   できること
     1. 検索欄の「×」ボタン（入力を消す）
        ※ 検索は「検索」ボタンを押したときだけ、検索結果画面（/shops/search）で行う。
          入力している間に一覧が変わることはない。
     2. 並び替え（標準／名前順）
     3. お知らせの「×」で閉じる
     4. 下までスクロールしたら「先頭へ戻る」ボタンを出す
   ========================================================= */
document.addEventListener('DOMContentLoaded', () => {

    /* ----- 1. 検索欄の「×」ボタン ----- */
    const input    = document.getElementById('store-search');
    const clearBtn = document.querySelector('.search-clear');

    if (input && clearBtn) {
        // 文字が入っているときだけ「×」を表示する（一覧は変えない）
        const toggleClear = () => { clearBtn.hidden = input.value === ''; };
        input.addEventListener('input', toggleClear);
        toggleClear();

        clearBtn.addEventListener('click', () => {
            input.value = '';
            toggleClear();
            input.focus();
        });
    }

    /* ----- 2. 並び替え ----- */
    const grid       = document.querySelector('.shop-grid');
    const sortSelect = document.getElementById('store-sort');

    if (grid && sortSelect) {
        const cards = Array.from(grid.querySelectorAll('.shop'));
        sortSelect.addEventListener('change', () => {
            const sorted = cards.slice().sort((a, b) => {
                if (sortSelect.value === 'name') {
                    return a.dataset.name.localeCompare(b.dataset.name, 'ja');
                }
                return Number(a.dataset.index) - Number(b.dataset.index);   // 標準＝元の並び
            });
            sorted.forEach((card) => grid.appendChild(card));
        });
    }
	
	/* ----- 2-2. フロアで絞り込み ----- */
	const floorSelect = document.getElementById('floor-filter');
	const countEl     = document.getElementById('visible-count');

	if (grid && floorSelect) {
	    floorSelect.addEventListener('change', () => {
	        const floor = floorSelect.value;
	        let count = 0;

	        grid.querySelectorAll('.shop').forEach((card) => {
	            // 「すべて」か、住所（＝階数）が選んだ階と同じなら表示
	            const show = floor === 'all' || card.dataset.address === floor;
	            card.hidden = !show;
	            if (show) count++;
	        });

	        if (countEl) countEl.textContent = count;   // 「全N件」の数字も更新
	    });
	}

    /* ----- 3. お知らせを閉じる ----- */
    document.querySelectorAll('.flash-close').forEach((btn) => {
        btn.addEventListener('click', () => btn.closest('.flash').remove());
    });

    /* ----- 4. 先頭へ戻るボタン ----- */
    const toTop = document.getElementById('to-top');
    if (toTop) {
        const toggle = () => { toTop.hidden = window.scrollY < 600; };
        window.addEventListener('scroll', toggle, { passive: true });
        toggle();
        toTop.addEventListener('click', () => window.scrollTo({ top: 0 }));
    }
});
