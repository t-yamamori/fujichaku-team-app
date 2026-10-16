(() => {
    "use strict";

    const CANVAS_ID = "shopAdminWrigglingCanvas";

    function startWrigglingEffect() {
        // 二重起動を防止
        if (document.getElementById(CANVAS_ID)) {
            return;
        }

        const canvas = document.createElement("canvas");
        canvas.id = CANVAS_ID;
        canvas.setAttribute("aria-hidden", "true");

        Object.assign(canvas.style, {
            position: "fixed",
            inset: "0",
            display: "block",
            width: "100%",
            height: "100%",
            zIndex: "-1",
            pointerEvents: "none",
            opacity: "1"
        });

        document.body.appendChild(canvas);

        const ctx = canvas.getContext("2d");

        if (!ctx) {
            canvas.remove();
            console.error(
                "[ShopAdminWriggling] Canvas 2D を利用できません。"
            );
            return;
        }

        // 文字列の塊。各グループが独立して動く
		const textGroups = [
		    "SYSTEM::ACCESS_GRANTED",
		    "USER_DATA_010101",
		    "ADMIN_NODE://ACTIVE",
		    "SELECT * FROM USERS",
		    "AUTHENTICATION_PENDING",
		    "MEMORY_BLOCK_0x7F",
		    "DATABASE::CONNECTED",
		    "ROOT_ACCESS_DENIED",
		    "NULL_POINTER_EXCEPTION",
		    "ENCRYPTED_DATA_0101",
		    "SESSION_ID::UNKNOWN",
		    "EXECUTE_PROTOCOL_07",
		    "TRACE://UNRESOLVED",
		    "SYSTEM_WARNING...",
		    "0x00FF::SIGNAL_LOST",
		    "RECORD_NOT_FOUND",
		    "CONNECTION_ESTABLISHED",
		    "PROCESSING_REQUEST...",

		    "KERNEL::INITIALIZED",
		    "ACCESS_LEVEL::ROOT",
		    "NETWORK_NODE_0x3A",
		    "QUERY_EXECUTION_PENDING",
		    "SECURITY_PROTOCOL_ACTIVE",
		    "CACHE_MEMORY::LOADED",
		    "SYSTEM_CLOCK_010101",
		    "FIREWALL::ENABLED",
		    "PACKET_TRACE://RUNNING",
		    "PROCESS_ID::0x0042",
		    "DATA_STREAM::ENCRYPTED",
		    "AUTH_TOKEN::VERIFIED",
		    "REMOTE_HOST_UNKNOWN",
		    "BUFFER_OVERFLOW_WARNING",
		    "HEAP_MEMORY::ALLOCATED",
		    "SIGNAL_RECEIVED_0xFF",
		    "ACCESS_LOG::UPDATED",
		    "THREAD_EXECUTION_ACTIVE",
		    "PROTOCOL::HANDSHAKE",
		    "SYSTEM_SCAN_IN_PROGRESS",
		    "DATABASE_QUERY::OK",
		    "USER_SESSION::ACTIVE",
		    "ENCRYPTION_KEY_0xA7",
		    "MEMORY_DUMP::READY",
		    "PACKET_LOSS_DETECTED",
		    "PROCESS_TERMINATED_07",
		    "SYSTEM_STATUS::NOMINAL",
		    "AWAITING_RESPONSE..."
		];
        const groups = [];

        const FONT_SIZE_MIN = 10;
        const FONT_SIZE_MAX = 17;
        const GROUP_COUNT = 18;

        let width = 0;
        let height = 0;
        let dpr = 1;
        let animationId = null;
        let lastFrame = 0;

        const FRAME_INTERVAL = 40;

        function random(min, max) {
            return min + Math.random() * (max - min);
        }

        function createGroup(index) {
            const text = textGroups[index % textGroups.length];

            return {
                text,

                // 画面内の初期位置
                x: random(0, Math.max(width, 1)),
                y: random(0, Math.max(height, 1)),

                // グループごとの移動速度
                speedX: random(-0.12, 0.12),
                speedY: random(-0.10, 0.10),

                // サイズと傾き
                fontSize: random(FONT_SIZE_MIN, FONT_SIZE_MAX),
                rotation: random(-0.12, 0.12),
                rotationSpeed: random(-0.0005, 0.0005),

                // うねりの特性
                phase: random(0, Math.PI * 2),
                waveSpeed: random(0.0003, 0.0008),
                waveAmount: random(2, 8),

                // 点滅の特性
                blinkPhase: random(0, Math.PI * 2),
                blinkSpeed: random(0.0004, 0.0010),
                minAlpha: random(0.00, 0.22),
                maxAlpha: random(0.35, 0.75),

                // グループごとの文字間隔
                letterSpacing: random(0.5, 2.5)
            };
        }

        function resize() {
            dpr = Math.min(window.devicePixelRatio || 1, 2);

            width = window.innerWidth;
            height = window.innerHeight;

            canvas.width = Math.floor(width * dpr);
            canvas.height = Math.floor(height * dpr);

            canvas.style.width = width + "px";
            canvas.style.height = height + "px";

            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

            // リサイズ後も既存グループを維持し、
            // 画面外に出たグループだけ位置を戻す
            for (const group of groups) {
                group.x = Math.min(Math.max(group.x, 0), width);
                group.y = Math.min(Math.max(group.y, 0), height);
            }
        }

        function drawGroup(group, timestamp) {
            // ゆっくりと明滅
            const blink =
                (Math.sin(
                    timestamp * group.blinkSpeed +
                    group.blinkPhase
                ) + 1) / 2;

            const alpha =
                group.minAlpha +
                (group.maxAlpha - group.minAlpha) * blink;

            // グループ全体をわずかにうねらせる
            const waveX =
                Math.sin(
                    timestamp * group.waveSpeed + group.phase
                ) * group.waveAmount;

            const waveY =
                Math.cos(
                    timestamp * group.waveSpeed * 0.8 +
                    group.phase
                ) * group.waveAmount;

            ctx.save();

            ctx.translate(
                group.x + waveX,
                group.y + waveY
            );

            ctx.rotate(
                group.rotation +
                Math.sin(
                    timestamp * group.waveSpeed +
                    group.phase
                ) * 0.025
            );

            ctx.font =
                group.fontSize + 'px Consolas, "Courier New", monospace';

            ctx.textBaseline = "middle";
            ctx.globalAlpha = alpha;

            // 発光を弱めに設定し、背景の文字を読みやすくする
            ctx.shadowColor = "#00dfff";
            ctx.shadowBlur = 5 + blink * 7;
            ctx.fillStyle = "#00dfff";

            // 文字列を一塊として描画
            ctx.fillText(group.text, 0, 0);

            // ごく薄い残像風の二重描画
            ctx.globalAlpha = alpha * 0.18;
            ctx.shadowBlur = 1;
            ctx.fillStyle = "#a5f7ff";
            ctx.fillText(
                group.text,
                1,
                1
            );

            ctx.restore();
        }

        function updateGroup(group) {
            // 一定方向に流し続けず、少しずつ進行方向を変える
            group.speedX += random(-0.002, 0.002);
            group.speedY += random(-0.002, 0.002);

            // 速度が大きくなりすぎないように制限
            group.speedX = Math.max(
                -0.16,
                Math.min(0.16, group.speedX)
            );

            group.speedY = Math.max(
                -0.13,
                Math.min(0.13, group.speedY)
            );

            group.x += group.speedX;
            group.y += group.speedY;

            // ゆっくり回転
            group.rotation += group.rotationSpeed;

            // 画面端では反対方向へ緩やかに戻す
            const margin = 220;

            if (group.x < -margin) {
                group.x = width + margin;
            } else if (group.x > width + margin) {
                group.x = -margin;
            }

            if (group.y < -margin) {
                group.y = height + margin;
            } else if (group.y > height + margin) {
                group.y = -margin;
            }
        }

        function draw(timestamp) {
            if (timestamp - lastFrame < FRAME_INTERVAL) {
                animationId = requestAnimationFrame(draw);
                return;
            }

            lastFrame = timestamp;

            // 毎フレーム消去し、残像を引きずらない
            ctx.clearRect(0, 0, width, height);

            for (const group of groups) {
                updateGroup(group);
                drawGroup(group, timestamp);
            }

            animationId = requestAnimationFrame(draw);
        }

        // 画面全体に文字列を分散配置
        resize();

        for (let i = 0; i < GROUP_COUNT; i++) {
            groups.push(createGroup(i));
        }

        window.addEventListener("resize", resize);

        // タブが非表示なら描画を止め、復帰時に再開する
        function handleVisibilityChange() {
            if (document.hidden) {
                if (animationId !== null) {
                    cancelAnimationFrame(animationId);
                    animationId = null;
                }
                return;
            }

            if (animationId === null) {
                lastFrame = 0;
                animationId = requestAnimationFrame(draw);
            }
        }

        document.addEventListener(
            "visibilitychange",
            handleVisibilityChange
        );

        animationId = requestAnimationFrame(draw);

        console.info(
            "[ShopAdminWriggling] 文字列エフェクトを開始しました。"
        );
    }

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            startWrigglingEffect,
            { once: true }
        );
    } else {
        startWrigglingEffect();
    }
})();