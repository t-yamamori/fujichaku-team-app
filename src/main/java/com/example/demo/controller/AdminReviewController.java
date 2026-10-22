package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.AdminReview;
import com.example.demo.form.AdminReviewSearchForm;
import com.example.demo.form.AdminReviewUpdateForm;
import com.example.demo.service.AdminReviewService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private static final int MAX_CHANGED_REVIEWS = 50;

    private final AdminReviewService adminReviewService;

    @GetMapping
    public String showReviews(
            @ModelAttribute("filters") AdminReviewSearchForm filters,
            Model model) {

        List<AdminReview> visibleReviews =
                filterReviews(adminReviewService.findAllActive(), filters);

        model.addAttribute("reviews", visibleReviews);
        model.addAttribute("updateForm", toUpdateForm(visibleReviews));
        model.addAttribute("newReview", new AdminReview());
        model.addAttribute("updateErrors",
                new HashMap<Integer, Map<String, String>>());
        model.addAttribute("createErrors", new HashMap<String, String>());

        return "admin/reviews";
    }

    /*
     * ID入力時に店舗名・会員名を取得する。
     */
    @GetMapping("/lookup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> lookup(
            @RequestParam("type") String type,
            @RequestParam("id") Integer id) {

        Map<String, Object> response = new HashMap<>();

        if (id == null || id <= 0) {
            response.put("found", false);
            response.put("name", "");
            response.put("message", "正しいIDを入力してください。");
            return ResponseEntity.ok(response);
        }

        String name;

        if ("store".equals(type)) {
            name = adminReviewService.findActiveStoreName(id);
        } else if ("member".equals(type)) {
            name = adminReviewService.findActiveMemberName(id);
        } else {
            response.put("found", false);
            response.put("name", "");
            response.put("message", "検索対象が正しくありません。");
            return ResponseEntity.badRequest().body(response);
        }

        boolean found = name != null && !name.isBlank();

        response.put("found", found);
        response.put("name", found ? name : "");
        response.put("message",
                found ? "" : "該当するデータが存在しないか、削除済みです。");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public String updateAll(
            @ModelAttribute("updateForm") AdminReviewUpdateForm form,
            BindingResult bindingResult,
            @RequestParam(value = "filterId", required = false) String filterId,
            @RequestParam(value = "filterStoreId", required = false) String filterStoreId,
            @RequestParam(value = "filterStoreName", required = false) String filterStoreName,
            @RequestParam(value = "filterMemberId", required = false) String filterMemberId,
            @RequestParam(value = "filterMemberName", required = false) String filterMemberName,
            @RequestParam(value = "filterComment", required = false) String filterComment,
            @RequestParam(value = "filterGrade", required = false) String filterGrade,
            @RequestParam(value = "filterCreatedAt", required = false) String filterCreatedAt,
            RedirectAttributes redirectAttributes) {

        AdminReviewSearchForm filters = buildFilters(filterId, filterStoreId,
                filterStoreName, filterMemberId, filterMemberName,
                filterComment, filterGrade, filterCreatedAt);

        /*
         * 店舗ID・会員IDがIntegerの範囲を超えるなど、
         * フォームのデータ変換に失敗した場合は更新を中止する。
         *
         * BindingResultは対象のModelAttributeの直後に置く。
         */
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .filter(error ->
                            error.getField().endsWith(".storeId")
                            || error.getField().endsWith(".memberId"))
                    .map(error -> {
                        if (error.getField().endsWith(".storeId")) {
                            return "店舗IDは1～2147483647の整数で入力してください。";
                        }
                        return "会員IDは1～2147483647の整数で入力してください。";
                    })
                    .distinct()
                    .collect(Collectors.joining(" "));

            if (message.isBlank()) {
                message = "入力値が正しくありません。各項目を確認してください。";
            }

            redirectAttributes.addFlashAttribute("successMessage", message);
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        List<AdminReview> allActive = adminReviewService.findAllActive();

        Set<Integer> visibleIds = filterReviews(allActive, filters).stream()
                .map(AdminReview::getId)
                .collect(Collectors.toSet());

        Map<Integer, AdminReview> originalById = allActive.stream()
                .collect(Collectors.toMap(
                        AdminReview::getId,
                        review -> review,
                        (first, second) -> first));

        List<AdminReview> submitted = form.getReviews() == null
                ? new ArrayList<>() : form.getReviews();

        List<AdminReview> changed = new ArrayList<>();
        List<String> changeDetails = new ArrayList<>();
        Set<Integer> processedIds = new LinkedHashSet<>();

        for (AdminReview submittedReview : submitted) {
            if (submittedReview == null || submittedReview.getId() == null) {
                continue;
            }

            Integer id = submittedReview.getId();

            if (!visibleIds.contains(id)
                    || !processedIds.add(id)
                    || !originalById.containsKey(id)) {
                continue;
            }

            AdminReview original = originalById.get(id);

            if (submittedReview.getStoreId() == null
                    || !adminReviewService.existsActiveStore(
                            submittedReview.getStoreId())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "レビューID " + id
                        + " の店舗IDが存在しないか、削除済みです。更新を中止しました。");
                addFilters(redirectAttributes, filters);
                return "redirect:/admin/reviews";
            }

            if (submittedReview.getMemberId() == null
                    || !adminReviewService.existsActiveMember(
                            submittedReview.getMemberId())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "レビューID " + id
                        + " の会員IDが存在しないか、削除済みです。更新を中止しました。");
                addFilters(redirectAttributes, filters);
                return "redirect:/admin/reviews";
            }

            if (submittedReview.getComment() == null
                    || submittedReview.getComment().isBlank()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "レビューID " + id
                        + " のコメントが空欄です。更新を中止しました。");
                addFilters(redirectAttributes, filters);
                return "redirect:/admin/reviews";
            }

            if (submittedReview.getComment().length() > 2000) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "レビューID " + id
                        + " のコメントは2000文字以内で入力してください。更新を中止しました。");
                addFilters(redirectAttributes, filters);
                return "redirect:/admin/reviews";
            }

            if (submittedReview.getGrade() == null
                    || submittedReview.getGrade() < 1
                    || submittedReview.getGrade() > 5) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "レビューID " + id
                        + " の評価は1～5で入力してください。更新を中止しました。");
                addFilters(redirectAttributes, filters);
                return "redirect:/admin/reviews";
            }

            String newComment = submittedReview.getComment().trim();

            boolean storeChanged =
                    !same(original.getStoreId(), submittedReview.getStoreId());
            boolean memberChanged =
                    !same(original.getMemberId(), submittedReview.getMemberId());
            boolean commentChanged =
                    !same(original.getComment(), newComment);
            boolean gradeChanged =
                    !same(original.getGrade(), submittedReview.getGrade());

            if (!storeChanged && !memberChanged
                    && !commentChanged && !gradeChanged) {
                continue;
            }

            StringBuilder detail = new StringBuilder();
            detail.append("レビューID ").append(id);

            if (original.getStoreName() != null) {
                detail.append("（店舗：")
                        .append(original.getStoreName()).append("）");
            }

            detail.append("：");

            List<String> fields = new ArrayList<>();

            if (storeChanged) {
                String newStoreName = adminReviewService.findActiveStoreName(
                        submittedReview.getStoreId());

                fields.add("店舗ID "
                        + original.getStoreId() + "（"
                        + display(original.getStoreName()) + "）"
                        + " → "
                        + submittedReview.getStoreId() + "（"
                        + display(newStoreName) + "）");
            }

            if (memberChanged) {
                String newMemberName = adminReviewService.findActiveMemberName(
                        submittedReview.getMemberId());

                fields.add("会員ID "
                        + original.getMemberId() + "（"
                        + display(original.getMemberName()) + "）"
                        + " → "
                        + submittedReview.getMemberId() + "（"
                        + display(newMemberName) + "）");
            }

            if (commentChanged) {
                fields.add("コメント「" + display(original.getComment())
                        + "」→「" + newComment + "」");
            }

            if (gradeChanged) {
                fields.add("評価 " + original.getGrade()
                        + " → " + submittedReview.getGrade());
            }

            detail.append(String.join("、", fields));
            changeDetails.add(detail.toString());

            AdminReview update = new AdminReview();
            update.setId(id);
            update.setStoreId(submittedReview.getStoreId());
            update.setMemberId(submittedReview.getMemberId());
            update.setComment(newComment);
            update.setGrade(submittedReview.getGrade());

            changed.add(update);
        }

        if (changed.size() > MAX_CHANGED_REVIEWS) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "一度に更新できるレビューは50件までです。入力内容は保持されています。"
                    + "変更件数：" + changed.size() + "件");
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        try {
            adminReviewService.updateAll(changed);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", ex.getMessage());
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        redirectAttributes.addFlashAttribute("reviewUpdateSucceeded", true);

        if (changed.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "変更されたレビューはありませんでした。");
        } else {
            String message = changed.size() + "件のレビューを更新しました。\n"
                    + String.join("\n", changeDetails);
            redirectAttributes.addFlashAttribute("successMessage", message);
        }

        addFilters(redirectAttributes, filters);
        return "redirect:/admin/reviews";
    }

    @PostMapping("/delete")
    public String delete(
            @RequestParam(value = "deleteIds", required = false)
            List<Integer> deleteIds,
            @RequestParam(value = "filterId", required = false) String filterId,
            @RequestParam(value = "filterStoreId", required = false) String filterStoreId,
            @RequestParam(value = "filterStoreName", required = false) String filterStoreName,
            @RequestParam(value = "filterMemberId", required = false) String filterMemberId,
            @RequestParam(value = "filterMemberName", required = false) String filterMemberName,
            @RequestParam(value = "filterComment", required = false) String filterComment,
            @RequestParam(value = "filterGrade", required = false) String filterGrade,
            @RequestParam(value = "filterCreatedAt", required = false) String filterCreatedAt,
            RedirectAttributes redirectAttributes) {

        AdminReviewSearchForm filters = buildFilters(filterId, filterStoreId,
                filterStoreName, filterMemberId, filterMemberName,
                filterComment, filterGrade, filterCreatedAt);

        if (deleteIds == null || deleteIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "削除対象を選択してください。");
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        Set<Integer> visibleIds = filterReviews(
                adminReviewService.findAllActive(), filters).stream()
                .map(AdminReview::getId)
                .collect(Collectors.toSet());

        List<Integer> validIds = deleteIds.stream()
                .filter(visibleIds::contains)
                .distinct()
                .collect(Collectors.toList());

        int deletedCount = adminReviewService.deleteByIds(validIds);

        redirectAttributes.addFlashAttribute(
                "successMessage", deletedCount + "件のレビューを論理削除しました。");

        addFilters(redirectAttributes, filters);
        return "redirect:/admin/reviews";
    }

    @PostMapping("/create")
    public String create(
            @ModelAttribute("newReview") AdminReview review,
            BindingResult bindingResult,
            @RequestParam(value = "filterId", required = false) String filterId,
            @RequestParam(value = "filterStoreId", required = false) String filterStoreId,
            @RequestParam(value = "filterStoreName", required = false) String filterStoreName,
            @RequestParam(value = "filterMemberId", required = false) String filterMemberId,
            @RequestParam(value = "filterMemberName", required = false) String filterMemberName,
            @RequestParam(value = "filterComment", required = false) String filterComment,
            @RequestParam(value = "filterGrade", required = false) String filterGrade,
            @RequestParam(value = "filterCreatedAt", required = false) String filterCreatedAt,
            RedirectAttributes redirectAttributes) {

        AdminReviewSearchForm filters = buildFilters(filterId, filterStoreId,
                filterStoreName, filterMemberId, filterMemberName,
                filterComment, filterGrade, filterCreatedAt);

        if (bindingResult.hasFieldErrors("storeId")) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "店舗IDは1～2147483647の整数で入力してください。");
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        if (bindingResult.hasFieldErrors("memberId")) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "会員IDは1～2147483647の整数で入力してください。");
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        if (bindingResult.hasFieldErrors("grade")) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "評価は1～5の整数で選択してください。");
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "入力値が正しくありません。各項目を確認してください。");
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        String error = validateNewReview(review);

        if (error != null) {
            redirectAttributes.addFlashAttribute("successMessage", error);
            addFilters(redirectAttributes, filters);
            return "redirect:/admin/reviews";
        }

        try {
            review.setComment(review.getComment().trim());
            adminReviewService.create(review);

            String storeName =
                    adminReviewService.findActiveStoreName(review.getStoreId());
            String memberName =
                    adminReviewService.findActiveMemberName(review.getMemberId());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "レビューを登録しました。店舗：" + display(storeName)
                    + "、会員：" + display(memberName)
                    + "、評価：" + review.getGrade());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }

        addFilters(redirectAttributes, filters);
        return "redirect:/admin/reviews";
    }

    private String validateNewReview(AdminReview review) {
        if (review.getStoreId() == null || review.getStoreId() <= 0) {
            return "店舗IDは1～2147483647の整数で入力してください。";
        }

        if (review.getMemberId() == null || review.getMemberId() <= 0) {
            return "会員IDは1～2147483647の整数で入力してください。";
        }

        if (review.getComment() == null || review.getComment().isBlank()) {
            return "コメントを入力してください。";
        }

        if (review.getComment().length() > 2000) {
            return "コメントは2000文字以内で入力してください。";
        }

        if (review.getGrade() == null
                || review.getGrade() < 1 || review.getGrade() > 5) {
            return "評価は1～5で入力してください。";
        }

        if (!adminReviewService.existsActiveStore(review.getStoreId())) {
            return "指定された店舗IDが存在しないか、削除済みです。";
        }

        if (!adminReviewService.existsActiveMember(review.getMemberId())) {
            return "指定された会員IDが存在しないか、削除済みです。";
        }

        return null;
    }

    private AdminReviewSearchForm buildFilters(
            String id, String storeId, String storeName,
            String memberId, String memberName, String comment,
            String grade, String createdAt) {

        AdminReviewSearchForm filters = new AdminReviewSearchForm();
        filters.setId(id);
        filters.setStoreId(storeId);
        filters.setStoreName(storeName);
        filters.setMemberId(memberId);
        filters.setMemberName(memberName);
        filters.setComment(comment);
        filters.setGrade(grade);
        filters.setCreatedAt(createdAt);

        return filters;
    }

    private List<AdminReview> filterReviews(
            List<AdminReview> reviews, AdminReviewSearchForm filters) {

        return reviews.stream()
                .filter(r -> matches(number(r.getId()), filters.getId()))
                .filter(r -> matches(number(r.getStoreId()), filters.getStoreId()))
                .filter(r -> matches(r.getStoreName(), filters.getStoreName()))
                .filter(r -> matches(number(r.getMemberId()), filters.getMemberId()))
                .filter(r -> matches(r.getMemberName(), filters.getMemberName()))
                .filter(r -> matches(r.getComment(), filters.getComment()))
                .filter(r -> matches(number(r.getGrade()), filters.getGrade()))
                .filter(r -> matches(
                        r.getCreatedAt() == null ? null : r.getCreatedAt().toString(),
                        filters.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private boolean matches(String value, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        return value != null && value.toLowerCase(Locale.ROOT)
                .contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private String number(Integer value) {
        return value == null ? null : value.toString();
    }

    private boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "(なし)" : value;
    }

    private AdminReviewUpdateForm toUpdateForm(List<AdminReview> reviews) {
        AdminReviewUpdateForm form = new AdminReviewUpdateForm();
        form.setReviews(new ArrayList<>(reviews));
        return form;
    }

    private void addFilters(
            RedirectAttributes attributes, AdminReviewSearchForm filters) {

        attributes.addAttribute("id", filters.getId());
        attributes.addAttribute("storeId", filters.getStoreId());
        attributes.addAttribute("storeName", filters.getStoreName());
        attributes.addAttribute("memberId", filters.getMemberId());
        attributes.addAttribute("memberName", filters.getMemberName());
        attributes.addAttribute("comment", filters.getComment());
        attributes.addAttribute("grade", filters.getGrade());
        attributes.addAttribute("createdAt", filters.getCreatedAt());
    }
}