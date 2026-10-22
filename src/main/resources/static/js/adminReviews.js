document.addEventListener("DOMContentLoaded", () => {
    const lookupUrl = "/admin/reviews/lookup";
    const updateForm = document.getElementById("bulkUpdateForm");
    const updateMessage = document.getElementById("bulkUpdateClientMessage");
    const MAX_CHANGED_REVIEWS = 50;
    const DRAFT_KEY = "adminReviews.bulkUpdateDraft.v1";

    // --------------------------------------------------
    // 店舗名・会員名の照合
    // --------------------------------------------------
    async function updateName(input) {
        const id = input.value.trim();
        const isStore = input.classList.contains("store-id-input")
            || input.name === "storeId";
        const type = isStore ? "store" : "member";

        const targetId = input.dataset.target;
        if (!targetId) {
            return;
        }

        const messageId = input.dataset.message
            || (isStore
                ? targetId.replace("store-name-", "store-message-")
                : targetId.replace("member-name-", "member-message-"));

        const nameElement = document.getElementById(targetId);
        const messageElement = document.getElementById(messageId);

        if (!nameElement) {
            return;
        }

        nameElement.textContent = "";
        if (messageElement) {
            messageElement.textContent = "";
        }

        if (!id) {
            nameElement.textContent = "未入力";
            return;
        }

        if (!/^[1-9]\d*$/.test(id)) {
            nameElement.textContent = "未入力";
            if (messageElement) {
                messageElement.textContent = "正しいIDを入力してください。";
            }
            return;
        }

        nameElement.textContent = "確認中…";

        try {
            const response = await fetch(
                `${lookupUrl}?type=${encodeURIComponent(type)}&id=${encodeURIComponent(id)}`,
                {
                    method: "GET",
                    headers: { "Accept": "application/json" }
                }
            );

            if (!response.ok) {
                throw new Error("名前の取得に失敗しました。");
            }

            const data = await response.json();

            // 応答待ちの間に入力値が変わった場合は古い応答を表示しない
            if (input.value.trim() !== id) {
                return;
            }

            if (data.found) {
                nameElement.textContent = data.name;
                if (messageElement) {
                    messageElement.textContent = "";
                }
            } else {
                nameElement.textContent = "該当なし";
                if (messageElement) {
                    messageElement.textContent =
                        data.message || "該当するデータがありません。";
                }
            }
        } catch (error) {
            nameElement.textContent = "取得失敗";
            if (messageElement) {
                messageElement.textContent =
                    "名前を取得できませんでした。再入力してください。";
            }
            console.error(error);
        }
    }

    document.querySelectorAll(".lookup-id").forEach((input) => {
        input.addEventListener("input", () => updateName(input));
        input.addEventListener("change", () => updateName(input));
        if (input.value.trim()) {
            updateName(input);
        }
    });

    if (!updateForm) {
        return;
    }

    const rows = Array.from(updateForm.querySelectorAll("tbody tr"))
        .filter((row) => row.querySelector('input[name$=".id"]'));

    // 行IDをキーに、編集対象の入力欄をまとめる。
    function getRowData(row) {
        const idInput = row.querySelector('input[name$=".id"]');
        if (!idInput) {
            return null;
        }

        const idMatch = idInput.name.match(/^reviews\[\d+\]\.id$/);
        if (!idMatch) {
            return null;
        }

        const inputs = Array.from(row.querySelectorAll("input, textarea, select"))
            .filter((element) => /^reviews\[\d+\]\.(storeId|memberId|comment|grade)$/.test(element.name));

        return {
            id: idInput.value,
            idInput,
            fields: inputs,
            row
        };
    }

    const rowData = rows.map(getRowData).filter(Boolean);

    function readFieldValues(fields) {
        return fields.map((field) => field.value);
    }

    // サーバーから返された初期値を基準値として保存する。
    const baselineById = new Map();
    rowData.forEach((item) => {
        baselineById.set(item.id, readFieldValues(item.fields));
    });

    function isChanged(item) {
        const baseline = baselineById.get(item.id) || [];
        const current = readFieldValues(item.fields);
        return current.some((value, index) => value !== baseline[index]);
    }

    function saveDraft() {
        const draft = {
            savedAt: Date.now(),
            rows: {}
        };

        rowData.forEach((item) => {
            draft.rows[item.id] = {};
            item.fields.forEach((field) => {
                const property = field.name.match(/\.(storeId|memberId|comment|grade)$/);
                if (property) {
                    draft.rows[item.id][property[1]] = field.value;
                }
            });
        });

        try {
            sessionStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
        } catch (error) {
            // ストレージが利用できない環境でも、通常のフォーム送信は継続する。
            console.warn("入力内容を一時保存できませんでした。", error);
        }
    }

    function restoreDraft() {
        // 更新成功時のみドラフトを破棄する。エラー時は入力内容を復元する。
        if (updateForm.dataset.updateSucceeded === "true") {
            sessionStorage.removeItem(DRAFT_KEY);
            return;
        }

        let draft;
        try {
            draft = JSON.parse(sessionStorage.getItem(DRAFT_KEY) || "null");
        } catch (error) {
            sessionStorage.removeItem(DRAFT_KEY);
            return;
        }

        if (!draft || !draft.rows) {
            return;
        }

        rowData.forEach((item) => {
            const saved = draft.rows[item.id];
            if (!saved) {
                return;
            }

            item.fields.forEach((field) => {
                const property = field.name.match(/\.(storeId|memberId|comment|grade)$/);
                if (property && Object.prototype.hasOwnProperty.call(saved, property[1])) {
                    field.value = saved[property[1]];
                }
            });
        });

        // 復元したIDについて、店舗名・会員名も再照合する。
        updateForm.querySelectorAll(".lookup-id").forEach((input) => {
            if (input.value.trim()) {
                updateName(input);
            }
        });
    }

    restoreDraft();

    // 編集中の値を定期的な操作イベントごとに保存する。
    updateForm.addEventListener("input", saveDraft);
    updateForm.addEventListener("change", saveDraft);

    function showMessage(message, isError = false) {
        if (!updateMessage) {
            window.alert(message);
            return;
        }
        updateMessage.textContent = message;
        updateMessage.style.color = isError ? "#ff7777" : "";
    }

    updateForm.addEventListener("submit", (event) => {
        // 送信対象は変更された行だけに絞る。
        const changedRows = rowData.filter(isChanged);

        if (changedRows.length === 0) {
            event.preventDefault();
            showMessage("変更されたレビューはありません。", false);
            return;
        }

        if (changedRows.length > MAX_CHANGED_REVIEWS) {
            event.preventDefault();
            saveDraft();
            showMessage(
                `一度に更新できるレビューは${MAX_CHANGED_REVIEWS}件までです。現在${changedRows.length}件が変更されています。入力内容は保持されています。`,
                true
            );
            return;
        }

        // 変更行を連番で組み立て、SpringのListフォームバインドに対応させる。
        // 元のname属性は退避し、ブラウザ内で送信失敗した場合にも戻せるようにする。
        const originalNames = [];
        rowData.forEach((item) => {
            [item.idInput, ...item.fields].forEach((field) => {
                originalNames.push({ field, name: field.name });
                field.removeAttribute("name");
            });
        });

        changedRows.forEach((item, index) => {
            item.idInput.name = `reviews[${index}].id`;
            item.fields.forEach((field) => {
                const property = field.dataset.reviewProperty
                    || field.name.match(/\.(storeId|memberId|comment|grade)$/)?.[1]
                    || originalNames.find((entry) => entry.field === field)?.name.match(/\.(storeId|memberId|comment|grade)$/)?.[1];
                if (property) {
                    field.name = `reviews[${index}].${property}`;
                }
            });
        });

        saveDraft();

        // Native POSTを続行する。サーバー応答がエラーならsessionStorageに草稿が残る。
    });
});
