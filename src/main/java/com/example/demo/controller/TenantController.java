package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Stores;
import com.example.demo.form.TenantSearchForm;
import com.example.demo.form.TenantUpdateForm;
import com.example.demo.service.TenantService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // テナント管理画面・検索
    @GetMapping
    public String showTenants(
            @ModelAttribute("filters") TenantSearchForm filters,
            Model model) {

        // 検索条件で一覧を絞り込む
        model.addAttribute(
                "stores",
                filterStores(tenantService.findAll(), filters)
        );

        model.addAttribute("newStore", new Stores());
        model.addAttribute("updateForm", new TenantUpdateForm());
        model.addAttribute(
                "updateErrors",
                new HashMap<Integer, Map<String, String>>()
        );

        return "admin/tenants";
    }

    // 複数店舗の一括更新
    @PostMapping("/update")
    public String updateAll(
            @Valid @ModelAttribute("updateForm") TenantUpdateForm form,
            BindingResult bindingResult,
            @ModelAttribute("filters") TenantSearchForm filters,
            Model model,
            RedirectAttributes redirectAttributes) {

        // バリデーションエラーがある場合
        if (bindingResult.hasErrors()) {

            List<Stores> submittedStores = form.getStores();

            if (submittedStores == null) {
                submittedStores = new ArrayList<>();
            }

            model.addAttribute("stores", submittedStores);
            model.addAttribute("newStore", new Stores());

            Map<Integer, Map<String, String>> updateErrors =
                    new HashMap<>();

            Pattern pattern =
                    Pattern.compile("^stores\\[(\\d+)\\]\\.(.+)$");

            for (FieldError error : bindingResult.getFieldErrors()) {
                Matcher matcher = pattern.matcher(error.getField());

                if (!matcher.matches()) {
                    continue;
                }

                int index = Integer.parseInt(matcher.group(1));
                String field = matcher.group(2);

                updateErrors
                        .computeIfAbsent(index, k -> new HashMap<>())
                        .put(field, error.getDefaultMessage());
            }

            model.addAttribute("updateErrors", updateErrors);

            return "admin/tenants";
        }

        List<Stores> submittedStores = form.getStores();

        if (submittedStores == null || submittedStores.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "更新対象の店舗はありませんでした。"
            );

            addFilters(redirectAttributes, filters);

            return "redirect:/admin/tenants";
        }

        // 更新前のDBデータを取得
        Map<Integer, Stores> originalStores = tenantService.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Stores::getId,
                        store -> store
                ));

        // 実際に変更された店舗だけを抽出
        List<Stores> changedStores = submittedStores.stream()
                .filter(submitted -> {
                    Stores original =
                            originalStores.get(submitted.getId());

                    return original != null
                            && !getChanges(original, submitted).isEmpty();
                })
                .collect(Collectors.toList());

        if (!changedStores.isEmpty()) {

            // DBを更新
            tenantService.updateAll(changedStores);

            // 項目ごとの変更前・変更後をメッセージにまとめる
            String message = buildUpdateMessage(
                    changedStores,
                    originalStores
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    message
            );

        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "変更された店舗はありませんでした。"
            );
        }

        // 更新後も検索条件を保持
        addFilters(redirectAttributes, filters);

        return "redirect:/admin/tenants";
    }

    // 変更された項目と変更前・変更後の値を取得
    private Map<String, String> getChanges(
            Stores original,
            Stores submitted) {

        Map<String, String> changes = new LinkedHashMap<>();

        addChange(
                changes,
                "名前",
                original.getName(),
                submitted.getName()
        );

        addChange(
                changes,
                "ふりがな",
                original.getNameKana(),
                submitted.getNameKana()
        );

        addChange(
                changes,
                "メール",
                original.getMail(),
                submitted.getMail()
        );

        addChange(
                changes,
                "電話番号",
                original.getPhone(),
                submitted.getPhone()
        );

        addChange(
                changes,
                "代表者",
                original.getRepresentative(),
                submitted.getRepresentative()
        );

        addChange(
                changes,
                "住所",
                original.getAddress(),
                submitted.getAddress()
        );

        addChange(
                changes,
                "説明",
                original.getDescription(),
                submitted.getDescription()
        );

        addChange(
                changes,
                "画像URL",
                original.getPictureUrl(),
                submitted.getPictureUrl()
        );

        return changes;
    }

    // 値が異なる場合だけ変更項目に追加
    private void addChange(
            Map<String, String> changes,
            String label,
            String oldValue,
            String newValue) {

        if (!same(oldValue, newValue)) {
            changes.put(
                    label,
                    formatValue(oldValue)
                            + " → "
                            + formatValue(newValue)
            );
        }
    }

    // null・空欄を表示用に整形
    private String formatValue(String value) {
        if (value == null || value.isEmpty()) {
            return "（空欄）";
        }

        return value;
    }

    // 店舗ごとに変更内容をまとめる
    private String buildUpdateMessage(
            List<Stores> changedStores,
            Map<Integer, Stores> originalStores) {

        StringBuilder message = new StringBuilder();
        message.append("以下の店舗を更新しました。");

        for (Stores submitted : changedStores) {

            Stores original = originalStores.get(submitted.getId());

            Map<String, String> changes =
                    getChanges(original, submitted);

            message.append("\n\n");
            message.append("■ ")
                    .append(formatValue(original.getName()))
                    .append("（ID: ")
                    .append(submitted.getId())
                    .append("）");

            for (Map.Entry<String, String> entry
                    : changes.entrySet()) {

                message.append("\n  ")
                        .append(entry.getKey())
                        .append("：")
                        .append(entry.getValue());
            }
        }

        return message.toString();
    }

    // nullも安全に比較する
    private boolean same(String a, String b) {
        return Objects.equals(a, b);
    }

    // 論理削除
    @PostMapping("/delete")
    public String delete(
            @RequestParam(
                    value = "deleteIds",
                    required = false)
            List<Integer> deleteIds,
            @ModelAttribute("filters") TenantSearchForm filters,
            RedirectAttributes redirectAttributes) {

        tenantService.deleteByIds(deleteIds);

        // 削除後も検索条件を保持して一覧へ戻る
        addFilters(redirectAttributes, filters);

        return "redirect:/admin/tenants";
    }

    // 新規登録
    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("newStore") Stores store,
            BindingResult bindingResult,
            @ModelAttribute("filters") TenantSearchForm filters,
            Model model,
            RedirectAttributes redirectAttributes) {

        // パスワードチェック
        if (store.getPassword() == null
                || store.getPassword().isBlank()) {

            bindingResult.rejectValue(
                    "password",
                    "password.blank",
                    "パスワードを入力してください"
            );

        } else if (store.getPassword().length() < 8) {

            bindingResult.rejectValue(
                    "password",
                    "password.size",
                    "パスワードは8文字以上で入力してください"
            );
        }

        // バリデーションエラーがある場合
        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "stores",
                    filterStores(tenantService.findAll(), filters)
            );

            model.addAttribute("updateForm", new TenantUpdateForm());
            model.addAttribute(
                    "updateErrors",
                    new HashMap<Integer, Map<String, String>>()
            );

            return "admin/tenants";
        }

        // エラーがなければ登録
        tenantService.create(store);

        // 登録後も検索条件を保持
        addFilters(redirectAttributes, filters);

        return "redirect:/admin/tenants";
    }

    // 検索条件に一致する店舗だけを返す
    private List<Stores> filterStores(
            List<Stores> stores,
            TenantSearchForm filters) {

        return stores.stream()
                .filter(store -> matches(
                        String.valueOf(store.getId()),
                        filters.getId()))
                .filter(store -> matches(
                        store.getName(),
                        filters.getName()))
                .filter(store -> matches(
                        store.getNameKana(),
                        filters.getNameKana()))
                .filter(store -> matches(
                        store.getMail(),
                        filters.getMail()))
                .filter(store -> matches(
                        store.getPhone(),
                        filters.getPhone()))
                .filter(store -> matches(
                        store.getRepresentative(),
                        filters.getRepresentative()))
                .filter(store -> matches(
                        store.getAddress(),
                        filters.getAddress()))
                .filter(store -> matches(
                        store.getDescription(),
                        filters.getDescription()))
                .filter(store -> matches(
                        store.getPictureUrl(),
                        filters.getPictureUrl()))
                .collect(Collectors.toList());
    }

    // 部分一致・大文字小文字を区別しない検索
    private boolean matches(String value, String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        if (value == null) {
            return false;
        }

        return value.toLowerCase(Locale.ROOT)
                .contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    // リダイレクト先URLに検索条件を引き継ぐ
    private void addFilters(
            RedirectAttributes redirectAttributes,
            TenantSearchForm filters) {

        addFilter(redirectAttributes, "id", filters.getId());
        addFilter(redirectAttributes, "name", filters.getName());
        addFilter(
                redirectAttributes,
                "nameKana",
                filters.getNameKana());
        addFilter(redirectAttributes, "mail", filters.getMail());
        addFilter(redirectAttributes, "phone", filters.getPhone());
        addFilter(
                redirectAttributes,
                "representative",
                filters.getRepresentative());
        addFilter(redirectAttributes, "address", filters.getAddress());
        addFilter(
                redirectAttributes,
                "description",
                filters.getDescription());
        addFilter(
                redirectAttributes,
                "pictureUrl",
                filters.getPictureUrl());
    }

    private void addFilter(
            RedirectAttributes redirectAttributes,
            String name,
            String value) {

        if (value != null && !value.isBlank()) {
            redirectAttributes.addAttribute(name, value);
        }
    }
}