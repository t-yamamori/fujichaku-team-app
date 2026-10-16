document.addEventListener("DOMContentLoaded", () => {
    const lookupUrl = "/admin/reviews/lookup";

    async function updateName(input) {
        const id = input.value.trim();
        const isStore = input.classList.contains("store-id-input")
            || input.name === "storeId";
        const type = isStore ? "store" : "member";

        const targetId = input.dataset.target;
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

        // 初期表示時も名前を照合
        if (input.value.trim()) {
            updateName(input);
        }
    });
});