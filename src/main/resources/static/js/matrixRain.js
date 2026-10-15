
(() => {
    "use strict";

    function startMatrixRain() {
        const canvas = document.getElementById("matrixRain");

        if (!canvas) {
            console.error("[MatrixRain] canvas が見つかりません。");
            return;
        }

        const ctx = canvas.getContext("2d");

        if (!ctx) {
            console.error("[MatrixRain] Canvas 2D を利用できません。");
            return;
        }

        const chars =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "アイウエオカキクケコサシスセソタチツテト" +
            "ナニヌネノハヒフヘホマミムメモヤユヨラリルレロ";

        const fontSize = 16;
        let columns = 0;
        let drops = [];
        let animationId = null;
        let lastFrame = 0;

        function resize() {
            const dpr = Math.min(window.devicePixelRatio || 1, 2);

            canvas.width = Math.floor(window.innerWidth * dpr);
            canvas.height = Math.floor(window.innerHeight * dpr);
            canvas.style.width = "100%";
            canvas.style.height = "100%";

            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

            columns = Math.ceil(window.innerWidth / fontSize);
            drops = Array.from(
                { length: columns },
                () => Math.random() * -40
            );

            ctx.clearRect(
                0,
                0,
                window.innerWidth,
                window.innerHeight
            );
        }

        function draw(timestamp) {
            if (timestamp - lastFrame < 45) {
                animationId = requestAnimationFrame(draw);
                return;
            }

            lastFrame = timestamp;

            const width = window.innerWidth;
            const height = window.innerHeight;

            ctx.fillStyle = "rgba(3, 7, 5, 0.12)";
            ctx.fillRect(0, 0, width, height);

            ctx.font = fontSize + "px monospace";
            ctx.textBaseline = "top";

            for (let i = 0; i < columns; i++) {
                const char = chars.charAt(
                    Math.floor(Math.random() * chars.length)
                );

                const x = i * fontSize;
                const y = drops[i] * fontSize;

                ctx.fillStyle =
                    Math.random() > 0.97
                        ? "#c4ffd0"
                        : "#00ff41";

                ctx.fillText(char, x, y);

                if (y > height && Math.random() > 0.975) {
                    drops[i] = Math.random() * -20;
                } else {
                    drops[i]++;
                }
            }

            animationId = requestAnimationFrame(draw);
        }

        resize();

        window.addEventListener("resize", resize);

        if (animationId !== null) {
            cancelAnimationFrame(animationId);
        }

        animationId = requestAnimationFrame(draw);

        console.info("[MatrixRain] 描画を開始しました。");
    }

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            startMatrixRain,
            { once: true }
        );
    } else {
        startMatrixRain();
    }
})();
