
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

import org.springframework.dao.DuplicateKeyException;
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

        model.addAttribute(
                "stores",
                filterStores(tenantService.findAll(), filters));

        model.addAttribute("newStore", new Stores());
        model.addAttribute("updateForm", new TenantUpdateForm());
        model.addAttribute(
                "updateErrors",
                new HashMap<Integer, Map<String, String>>());

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

        List<Stores> submittedStores = form.getStores();

        if (submittedStores == null) {
            submittedStores = new ArrayList<>();
        }

        if (bindingResult.hasErrors()) {
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

            prepareUpdateErrorModel(
                    model, filters, form, submittedStores, updateErrors);

            return "admin/tenants";
        }

        if (submittedStores.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "更新対象の店舗はありませんでした。");

            addFilters(redirectAttributes, filters);
            return "redirect:/admin/tenants";
        }

        Map<Integer, Stores> originalStores = tenantService.findAll()
                .stream()
                .collect(Collectors.toMap(
                        Stores::getId,
                        store -> store));

        List<Stores> changedStores = new ArrayList<>();
        List<Integer> changedIndexes = new ArrayList<>();

        for (int i = 0; i < submittedStores.size(); i++) {
            Stores submitted = submittedStores.get(i);
            Stores original = originalStores.get(submitted.getId());

            if (original != null
                    && !getChanges(original, submitted).isEmpty()) {
                changedStores.add(submitted);
                changedIndexes.add(i);
            }
        }

        if (!changedStores.isEmpty()) {
            Map<Integer, Map<String, String>> updateErrors =
                    new HashMap<>();

            List<Integer> changedIds = changedStores.stream()
                    .map(Stores::getId)
                    .distinct()
                    .collect(Collectors.toList());

            for (int i = 0; i < changedStores.size(); i++) {
                Stores store = changedStores.get(i);
                int rowIndex = changedIndexes.get(i);

                if (hasText(store.getName())
                        && hasText(store.getNameKana())
                        && hasText(store.getMail())) {

                    if (tenantService.existsSameStoreExcludingIds(
                            store, changedIds)) {
                        addUpdateError(
                                updateErrors, rowIndex, "name",
                                "同じ店舗が存在します");

                    } else if (tenantService.existsMailExcludingIds(
                            store.getMail(), changedIds)) {
                        addUpdateError(
                                updateErrors, rowIndex, "mail",
                                "このメールアドレスは既に使用されています");
                    }

                } else if (hasText(store.getMail())
                        && tenantService.existsMailExcludingIds(
                                store.getMail(), changedIds)) {
                    addUpdateError(
                            updateErrors, rowIndex, "mail",
                            "このメールアドレスは既に使用されています");
                }
            }

            for (int i = 0; i < changedStores.size(); i++) {
                Stores first = changedStores.get(i);
                int firstIndex = changedIndexes.get(i);

                for (int j = i + 1; j < changedStores.size(); j++) {
                    Stores second = changedStores.get(j);
                    int secondIndex = changedIndexes.get(j);

                    if (Objects.equals(first.getMail(), second.getMail())
                            && hasText(first.getMail())) {

                        boolean sameStore =
                                Objects.equals(
                                        first.getName(), second.getName())
                                && Objects.equals(
                                        first.getNameKana(),
                                        second.getNameKana());

                        if (sameStore) {
                            addUpdateError(
                                    updateErrors, secondIndex, "name",
                                    "同じ店舗が存在します");
                        } else {
                            addUpdateError(
                                    updateErrors, secondIndex, "mail",
                                    "このメールアドレスは既に使用されています");
                        }
                    }
                }
            }

            if (!updateErrors.isEmpty()) {
                prepareUpdateErrorModel(
                        model, filters, form, submittedStores, updateErrors);
                return "admin/tenants";
            }

            try {
                tenantService.updateAll(changedStores);

            } catch (DuplicateKeyException e) {
                int rowIndex = changedIndexes.get(0);

                addUpdateError(
                        updateErrors, rowIndex, "mail",
                        "このメールアドレスは既に使用されています");

                prepareUpdateErrorModel(
                        model, filters, form, submittedStores, updateErrors);
                return "admin/tenants";
            }

            String message = buildUpdateMessage(
                    changedStores, originalStores);

            redirectAttributes.addFlashAttribute(
                    "successMessage", message);

        } else {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "変更された店舗はありませんでした。");
        }

        addFilters(redirectAttributes, filters);
        return "redirect:/admin/tenants";
    }


    // 一括更新エラー時に画面表示に必要なデータを設定
    private void prepareUpdateErrorModel(
            Model model,
            TenantSearchForm filters,
            TenantUpdateForm form,
            List<Stores> submittedStores,
            Map<Integer, Map<String, String>> updateErrors) {

        model.addAttribute("filters", filters);
        model.addAttribute("stores", submittedStores);
        model.addAttribute("newStore", new Stores());
        model.addAttribute("updateForm", form);
        model.addAttribute("updateErrors", updateErrors);
    }

    // 一括更新エラーを行・項目単位で設定
    private void addUpdateError(
            Map<Integer, Map<String, String>> updateErrors,
            int index,
            String field,
            String message) {

        updateErrors
                .computeIfAbsent(index, k -> new HashMap<>())
                .putIfAbsent(field, message);
    }

    // 変更された項目と変更前・変更後の値を取得
    private Map<String, String> getChanges(
            Stores original,
            Stores submitted) {

        Map<String, String> changes = new LinkedHashMap<>();

        addChange(changes, "名前",
                original.getName(), submitted.getName());

        addChange(changes, "ふりがな",
                original.getNameKana(), submitted.getNameKana());

        addChange(changes, "メール",
                original.getMail(), submitted.getMail());

        addChange(changes, "電話番号",
                original.getPhone(), submitted.getPhone());

        addChange(changes, "代表者",
                original.getRepresentative(),
                submitted.getRepresentative());

        addChange(changes, "住所",
                original.getAddress(), submitted.getAddress());

        addChange(changes, "説明",
                original.getDescription(), submitted.getDescription());

        addChange(changes, "画像URL",
                original.getPictureUrl(), submitted.getPictureUrl());

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
                            + formatValue(newValue));
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

            @RequestParam(value = "filterId", required = false)
            String filterId,

            @RequestParam(value = "filterName", required = false)
            String filterName,

            @RequestParam(value = "filterNameKana", required = false)
            String filterNameKana,

            @RequestParam(value = "filterMail", required = false)
            String filterMail,

            @RequestParam(value = "filterPhone", required = false)
            String filterPhone,

            @RequestParam(value = "filterRepresentative", required = false)
            String filterRepresentative,

            @RequestParam(value = "filterAddress", required = false)
            String filterAddress,

            @RequestParam(value = "filterDescription", required = false)
            String filterDescription,

            @RequestParam(value = "filterPictureUrl", required = false)
            String filterPictureUrl,

            Model model,
            RedirectAttributes redirectAttributes) {

        // 検索条件を専用パラメータから組み立てる
        TenantSearchForm filters = buildFilters(
                filterId,
                filterName,
                filterNameKana,
                filterMail,
                filterPhone,
                filterRepresentative,
                filterAddress,
                filterDescription,
                filterPictureUrl);

        // パスワードチェック
        if (store.getPassword() == null
                || store.getPassword().isBlank()) {

            bindingResult.rejectValue(
                    "password",
                    "password.blank",
                    "パスワードを入力してください");

        } else if (store.getPassword().length() < 8) {

            bindingResult.rejectValue(
                    "password",
                    "password.size",
                    "パスワードは8文字以上で入力してください");
        }

        // 重複確認
        if (hasText(store.getName())
                && hasText(store.getNameKana())
                && hasText(store.getMail())) {

            if (tenantService.existsSameStore(store)) {

                bindingResult.rejectValue(
                        "name",
                        "tenant.duplicateStore",
                        "同じ店舗が存在します");

            } else if (tenantService.existsMail(store.getMail())) {

                bindingResult.rejectValue(
                        "mail",
                        "tenant.duplicateMail",
                        "このメールアドレスは既に使用されています");
            }
        }

        // バリデーションエラーがある場合
        if (bindingResult.hasErrors()) {
            prepareCreateErrorModel(model, filters, store);
            return "admin/tenants";
        }

        // 登録
        try {
            tenantService.create(store);

        } catch (DuplicateKeyException e) {

            bindingResult.rejectValue(
                    "mail",
                    "tenant.duplicateMail",
                    "このメールアドレスは既に使用されています");

            prepareCreateErrorModel(model, filters, store);
            return "admin/tenants";
        }

        // 登録後も検索条件を保持
        addFilters(redirectAttributes, filters);

        return "redirect:/admin/tenants";
    }

    // 空欄でないことを確認
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // 新規登録エラー時に画面表示に必要なデータを再設定
    private void prepareCreateErrorModel(
            Model model,
            TenantSearchForm filters,
            Stores store) {

        model.addAttribute("filters", filters);
        model.addAttribute("newStore", store);

        model.addAttribute(
                "stores",
                filterStores(tenantService.findAll(), filters));

        model.addAttribute("updateForm", new TenantUpdateForm());

        model.addAttribute(
                "updateErrors",
                new HashMap<Integer, Map<String, String>>());
    }

    // 新規登録フォームから送信された検索条件を組み立てる
    private TenantSearchForm buildFilters(
            String id,
            String name,
            String nameKana,
            String mail,
            String phone,
            String representative,
            String address,
            String description,
            String pictureUrl) {

        TenantSearchForm filters = new TenantSearchForm();

        filters.setId(id);
        filters.setName(name);
        filters.setNameKana(nameKana);
        filters.setMail(mail);
        filters.setPhone(phone);
        filters.setRepresentative(representative);
        filters.setAddress(address);
        filters.setDescription(description);
        filters.setPictureUrl(pictureUrl);

        return filters;
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