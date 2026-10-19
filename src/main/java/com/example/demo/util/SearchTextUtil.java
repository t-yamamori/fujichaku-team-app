package com.example.demo.util;

import java.text.Normalizer;

/**
 * 【新規】店舗検索用の文字列ユーティリティ
 *
 * 検索キーワードと stores.name_kana の表記ゆれをなくすために、
 * どちらも同じルールで「揃えて」から比べる。
 */
public class SearchTextUtil {

	// インスタンス化しない（static メソッドだけのクラス）
	private SearchTextUtil() {
	}

	/**
	 * 検索用に文字を揃える
	 *   ① NFKC 正規化 : 半角ｶﾌｪ → 全角カフェ、全角ＣＡＦＥ → 半角CAFE
	 *   ② 小文字化    : CAFE → cafe
	 *   ③ カタカナ → ひらがな : カフェ → かふぇ
	 *
	 * 例：「ｶﾌｪ」「カフェ」「かふぇ」→ すべて「かふぇ」
	 */
	public static String normalize(String s) {
		if (s == null) {
			return null;
		}
		String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFKC).toLowerCase();

		StringBuilder sb = new StringBuilder(n.length());
		for (char c : n.toCharArray()) {
			// ァ(U+30A1)～ヶ(U+30F6) は 0x60 引くと対応するひらがなになる
			if (c >= 'ァ' && c <= 'ヶ') {
				sb.append((char) (c - 0x60));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 【追加】全角・半角だけを揃える（stores.name との比較用）
	 *   NFKC 正規化 : 全角ｓ → 半角s、全角１２３ → 半角123、半角ｶﾌｪ → 全角カフェ
	 *
	 * normalize() と違い、カタカナ → ひらがな変換はしない。
	 * （stores.name は「カフェ」などカタカナのまま登録されているため）
	 * 大文字小文字は SQL 側の ilike で吸収する。
	 */
	public static String normalizeWidth(String s) {
		if (s == null) {
			return null;
		}
		return Normalizer.normalize(s.trim(), Normalizer.Form.NFKC);
	}

	/**
	 * LIKE の特殊文字（\ % _）をエスケープする
	 * （「100%」などで検索したときに全件ヒットしてしまうのを防ぐ）
	 * SQL側では escape '\' を指定して使う
	 */
	public static String escapeLike(String s) {
		if (s == null) {
			return null;
		}
		return s.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}