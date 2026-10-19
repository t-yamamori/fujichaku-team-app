
package com.example.demo.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

import com.example.demo.entity.ShopAdmin;
import com.example.demo.form.ShopAdminSearchForm;
import com.example.demo.form.ShopAdminUpdateForm;
import com.example.demo.service.ShopAdminService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/shop-admins")
@RequiredArgsConstructor
public class ShopAdminController {

    private final ShopAdminService shopAdminService;

    /**
     * 店舗管理者一覧・検索
     */
    @GetMapping
    public String showShopAdmins(
            @ModelAttribute("filters") ShopAdminSearchForm filters,
            Model model) {

        List<ShopAdmin> filteredShopAdmins =
                filterShopAdmins(shopAdminService.findAll(), filters);

        model.addAttribute("shopAdmins", filteredShopAdmins);
        model.addAttribute("newShopAdmin", new ShopAdmin());
        model.addAttribute("updateForm",
                toUpdateForm(filteredShopAdmins));
        model.addAttribute("updateErrors",
                new HashMap<Integer, Map<String, String>>());
        model.addAttribute("createErrors",
                new HashMap<String, String>());

        return "admin/shop-admins";
    }

    /**
     * 一括更新
     */
    @PostMapping("/update")
    public String updateAll(
            @Valid @ModelAttribute("updateForm") ShopAdminUpdateForm form,
            BindingResult bindingResult,
            @RequestParam(value = "filterId", required = false)
            String filterId,
            @RequestParam(value = "filterStoreId", required = false)
            String filterStoreId,
            @RequestParam(value = "filterName", required = false)
            String filterName,
            @RequestParam(value = "filterMail", required = false)
            String filterMail,
            @RequestParam(value = "filterBirthDate", required = false)
            LocalDate filterBirthDate,
            Model model,
            RedirectAttributes redirectAttributes) {

        ShopAdminSearchForm filters = buildFilters(
                filterId, filterStoreId, filterName, filterMail,
                filterBirthDate);

        List<ShopAdmin> submitted =
                form.getShopAdmins() == null
                        ? new ArrayList<>()
                        : form.getShopAdmins();

        /*
         * 店舗IDの存在チェック
         *
         * Bean Validationのチェックに加え、
         * データベースに存在しない店舗IDが指定されていないか確認する。
         *
         * 1件でも不正な店舗IDがあればBindingResultにエラーを追加する。
         * その後のエラー判定によって、一括更新全体を中止する。
         */
        for (int i = 0; i < submitted.size(); i++) {
            ShopAdmin submittedAdmin = submitted.get(i);

            if (submittedAdmin.getId() <= 0) {
                continue;
            }

            Integer storeId = submittedAdmin.getStoreId();

            if (storeId != null
                    && !shopAdminService.existsStoreId(storeId)) {

                bindingResult.rejectValue(
                        "shopAdmins[" + i + "].storeId",
                        "invalid.storeId",
                        "存在する店舗IDを入力してください");
            }
        }

        // 入力エラーがあれば更新せず、入力値とエラーを表示する
        if (bindingResult.hasErrors()) {
            model.addAttribute("filters", filters);
            model.addAttribute("shopAdmins", submitted);
            model.addAttribute("newShopAdmin", new ShopAdmin());
            model.addAttribute("updateErrors",
                    collectUpdateErrors(bindingResult));
            model.addAttribute("createErrors",
                    new HashMap<String, String>());

            return "admin/shop-admins";
        }

        List<ShopAdmin> allShopAdmins = shopAdminService.findAll();

        Map<Integer, ShopAdmin> originalById = allShopAdmins.stream()
                .collect(Collectors.toMap(
                        ShopAdmin::getId,
                        admin -> admin,
                        (first, second) -> first,
                        LinkedHashMap::new));

        Set<Integer> visibleIds =
                filterShopAdmins(allShopAdmins, filters).stream()
                        .map(ShopAdmin::getId)
                        .collect(Collectors.toSet());

        List<ShopAdmin> changedShopAdmins = new ArrayList<>();
        List<String> changeMessages = new ArrayList<>();
        Set<Integer> processedIds = new HashSet<>();

        for (ShopAdmin submittedAdmin : submitted) {
            if (submittedAdmin.getId() <= 0) {
                continue;
            }

            int id = submittedAdmin.getId();

            if (!visibleIds.contains(id)
                    || !processedIds.add(id)
                    || !originalById.containsKey(id)) {
                continue;
            }

            ShopAdmin original = originalById.get(id);
            List<String> changes = getChanges(original, submittedAdmin);

            if (!changes.isEmpty()) {
                changedShopAdmins.add(submittedAdmin);
                changeMessages.add(
                        "ID " + id + "（" + original.getName() + "）："
                                + String.join("、", changes));
            }
        }

        if (changedShopAdmins.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "更新対象に変更はありませんでした。");
        } else {
            shopAdminService.updateAll(changedShopAdmins);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    changedShopAdmins.size() + "件を更新しました。\n"
                            + String.join("\n", changeMessages));
        }

        addFilters(redirectAttributes, filters);
        return "redirect:/admin/shop-admins";
    }

    /**
     * チェックされた店舗管理者を論理削除
     */
    @PostMapping("/delete")
    public String delete(
            @RequestParam(value = "deleteIds", required = false)
            List<Integer> deleteIds,
            @RequestParam(value = "filterId", required = false)
            String filterId,
            @RequestParam(value = "filterStoreId", required = false)
            String filterStoreId,
            @RequestParam(value = "filterName", required = false)
            String filterName,
            @RequestParam(value = "filterMail", required = false)
            String filterMail,
            @RequestParam(value = "filterBirthDate", required = false)
            LocalDate filterBirthDate,
            RedirectAttributes redirectAttributes) {

        ShopAdminSearchForm filters = buildFilters(
                filterId, filterStoreId, filterName, filterMail,
                filterBirthDate);

        if (deleteIds == null || deleteIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "削除対象を選択してください。");
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/shop-admins";
        }

        Set<Integer> visibleIds =
                filterShopAdmins(shopAdminService.findAll(), filters)
                        .stream()
                        .map(ShopAdmin::getId)
                        .collect(Collectors.toSet());

        List<Integer> validIds = deleteIds.stream()
                .filter(visibleIds::contains)
                .distinct()
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "削除対象が見つかりませんでした。");
        } else {
            shopAdminService.deleteByIds(validIds);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    validIds.size() + "件の店舗管理者を削除しました。");
        }

        addFilters(redirectAttributes, filters);
        return "redirect:/admin/shop-admins";
    }

    /**
     * 新規登録
     */
    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("newShopAdmin") ShopAdmin shopAdmin,
            BindingResult bindingResult,
            @RequestParam(value = "filterId", required = false)
            String filterId,
            @RequestParam(value = "filterStoreId", required = false)
            String filterStoreId,
            @RequestParam(value = "filterName", required = false)
            String filterName,
            @RequestParam(value = "filterMail", required = false)
            String filterMail,
            @RequestParam(value = "filterBirthDate", required = false)
            LocalDate filterBirthDate,
            Model model,
            RedirectAttributes redirectAttributes) {

        ShopAdminSearchForm filters = buildFilters(
                filterId, filterStoreId, filterName, filterMail,
                filterBirthDate);

        // パスワードの入力チェック
        if (shopAdmin.getPassword() == null
                || shopAdmin.getPassword().isBlank()) {
            bindingResult.rejectValue(
                    "password",
                    "password.blank",
                    "パスワードを入力してください");
        } else if (shopAdmin.getPassword().length() < 8) {
            bindingResult.rejectValue(
                    "password",
                    "password.size",
                    "パスワードは8文字以上で入力してください");
        }

        // 店舗IDの存在チェック
        if (shopAdmin.getStoreId() != null
                && !shopAdminService.existsStoreId(shopAdmin.getStoreId())) {
            bindingResult.rejectValue(
                    "storeId",
                    "invalid.storeId",
                    "存在する店舗IDを入力してください");
        }

        // 入力チェックでエラーがあれば登録しない
        if (bindingResult.hasErrors()) {
            return showCreateErrors(
                    shopAdmin, filters, bindingResult, model);
        }

        // 登録処理
        try {
            shopAdminService.create(shopAdmin);
        } catch (DuplicateKeyException e) {
            bindingResult.rejectValue(
                    "mail",
                    "duplicate.mail",
                    "このメールアドレスは既に使用されています");

            return showCreateErrors(
                    shopAdmin, filters, bindingResult, model);
        }

        redirectAttributes.addFlashAttribute(
                "successMessage", "店舗管理者を登録しました。");

        addFilters(redirectAttributes, filters);
        return "redirect:/admin/shop-admins";
    }

    /**
     * 新規登録エラー時の画面表示を共通化
     */
    private String showCreateErrors(
            ShopAdmin shopAdmin,
            ShopAdminSearchForm filters,
            BindingResult bindingResult,
            Model model) {

        List<ShopAdmin> filteredShopAdmins =
                filterShopAdmins(shopAdminService.findAll(), filters);

        model.addAttribute("filters", filters);
        model.addAttribute("shopAdmins", filteredShopAdmins);
        model.addAttribute("updateForm",
                toUpdateForm(filteredShopAdmins));
        model.addAttribute("updateErrors",
                new HashMap<Integer, Map<String, String>>());
        model.addAttribute("createErrors",
                collectCreateErrors(bindingResult));

        // パスワードは画面に再表示しない
        shopAdmin.setPassword(null);

        return "admin/shop-admins";
    }

    /**
     * 検索条件を組み立てる
     */
    private ShopAdminSearchForm buildFilters(
            String id,
            String storeId,
            String name,
            String mail,
            LocalDate birthDate) {

        ShopAdminSearchForm filters = new ShopAdminSearchForm();
        filters.setId(id);
        filters.setStoreId(storeId);
        filters.setName(name);
        filters.setMail(mail);
        filters.setBirthDate(birthDate);

        return filters;
    }

    /**
     * 検索条件で絞り込む
     */
    private List<ShopAdmin> filterShopAdmins(
            List<ShopAdmin> shopAdmins,
            ShopAdminSearchForm filters) {

        return shopAdmins.stream()
                .filter(admin -> matches(
                        String.valueOf(admin.getId()), filters.getId()))
                .filter(admin -> matches(
                        admin.getStoreId() == null
                                ? null : String.valueOf(admin.getStoreId()),
                        filters.getStoreId()))
                .filter(admin -> matches(
                        admin.getName(), filters.getName()))
                .filter(admin -> matches(
                        admin.getMail(), filters.getMail()))
                .filter(admin -> matchesDate(
                        admin.getBirthDate(), filters.getBirthDate()))
                .collect(Collectors.toList());
    }

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

    private boolean matchesDate(LocalDate value, LocalDate target) {
        return target == null || target.equals(value);
    }

    private ShopAdminUpdateForm toUpdateForm(
            List<ShopAdmin> shopAdmins) {

        ShopAdminUpdateForm form = new ShopAdminUpdateForm();
        form.setShopAdmins(new ArrayList<>(shopAdmins));
        return form;
    }

    /**
     * 一括更新のエラーを行番号・フィールド名ごとに整理する
     */
    private Map<Integer, Map<String, String>> collectUpdateErrors(
            BindingResult bindingResult) {

        Map<Integer, Map<String, String>> errors = new HashMap<>();
        Pattern pattern = Pattern.compile(
                "^shopAdmins\\[(\\d+)\\]\\.(.+)$");

        for (FieldError error : bindingResult.getFieldErrors()) {
            Matcher matcher = pattern.matcher(error.getField());

            if (!matcher.matches()) {
                continue;
            }

            int index = Integer.parseInt(matcher.group(1));
            String field = matcher.group(2);

            errors.computeIfAbsent(index, key -> new HashMap<>())
                    .put(field, error.getDefaultMessage());
        }

        return errors;
    }

    /**
     * 新規登録のエラーをフィールド名ごとに整理する
     */
    private Map<String, String> collectCreateErrors(
            BindingResult bindingResult) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return errors;
    }

    /**
     * 元データと入力値を比較する
     */
    private List<String> getChanges(
            ShopAdmin original,
            ShopAdmin updated) {

        List<String> changes = new ArrayList<>();

        addChange(changes, "店舗ID",
                original.getStoreId(), updated.getStoreId());
        addChange(changes, "管理者名",
                original.getName(), updated.getName());
        addChange(changes, "メール",
                original.getMail(), updated.getMail());
        addChange(changes, "生年月日",
                original.getBirthDate(), updated.getBirthDate());

        return changes;
    }

    private void addChange(
            List<String> changes,
            String label,
            Object oldValue,
            Object newValue) {

        if (oldValue == null
                ? newValue != null
                : !oldValue.equals(newValue)) {
            changes.add(label + "：" + formatValue(oldValue)
                    + " → " + formatValue(newValue));
        }
    }

    private String formatValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return "（空欄）";
        }
        return value.toString();
    }

    /**
     * リダイレクト時に検索条件を維持する
     */
    private void addFilters(
            RedirectAttributes redirectAttributes,
            ShopAdminSearchForm filters) {

        addFilter(redirectAttributes, "id", filters.getId());
        addFilter(redirectAttributes, "storeId", filters.getStoreId());
        addFilter(redirectAttributes, "name", filters.getName());
        addFilter(redirectAttributes, "mail", filters.getMail());

        if (filters.getBirthDate() != null) {
            redirectAttributes.addAttribute(
                    "birthDate", filters.getBirthDate());
        }
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
