package com.example.demo.util;

import org.springframework.stereotype.Component;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

/**
 * 【新規】漢字まじりの文字列を読みがな（ひらがな）に変換する
 *
 * 形態素解析ライブラリ Kuromoji を使用（pom.xml に依存関係の追加が必要）
 * Tokenizer は生成に時間がかかるので、@Component にしてアプリ全体で1つだけ使い回す。
 *
 * 例：珈琲 → こーひー、渋谷カフェ → しぶやかふぇ
 */
@Component
public class ReadingConverter {

	private final Tokenizer tokenizer = new Tokenizer();

	public String toReading(String text) {
		if (text == null || text.isBlank()) {
			return text;
		}

		StringBuilder sb = new StringBuilder();
		for (Token token : tokenizer.tokenize(text)) {
			String reading = token.getReading();

			// 辞書にない語（英字・造語など）は読みが "*" か null になるので、表記をそのまま使う
			if (reading == null || "*".equals(reading)) {
				sb.append(token.getSurface());
			} else {
				// 読みはカタカナで返ってくる（後の normalize でひらがなにする）
				sb.append(reading);
			}
		}

		// 検索側と同じルール（ひらがな・小文字など）に揃える
		return SearchTextUtil.normalize(sb.toString());
	}
}