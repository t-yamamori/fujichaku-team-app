package com.example.demo.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Reservations;
import com.example.demo.entity.ShopAdmin;
import com.example.demo.entity.Stores;
import com.example.demo.entity.Users;
import com.example.demo.mapper.ReservationMapper;
import com.example.demo.mapper.ShopAdminMapper;
import com.example.demo.mapper.StoresMapper;
import com.example.demo.mapper.UsersMapper;

@Controller
public class ReservationManagementController {

    private final ReservationMapper reservationMapper;
    private final StoresMapper storesMapper;
    private final ShopAdminMapper shopAdminMapper;
    private final UsersMapper usersMapper;

    private static final String STATUS_RESERVED = "予約済み";
    private static final String STATUS_VISITED = "来店済み";
    private static final String STATUS_CANCELLED = "キャンセル";

    private static final List<String> ALLOWED_STATUSES = List.of(
            STATUS_RESERVED, STATUS_VISITED, STATUS_CANCELLED);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public ReservationManagementController(
            ReservationMapper reservationMapper,
            StoresMapper storesMapper,
            ShopAdminMapper shopAdminMapper,
            UsersMapper usersMapper) {
        this.reservationMapper = reservationMapper;
        this.storesMapper = storesMapper;
        this.shopAdminMapper = shopAdminMapper;
        this.usersMapper = usersMapper;
    }

    // モール管理者：予約一覧・検索
    @GetMapping("/admin/reservations")
    public String mallAdminReservations(
            Authentication authentication,
            @RequestParam(required = false) Integer reservationId,
            @RequestParam(required = false) String reservationDate,
            Model model) {

        requireRole(authentication, "ROLE_ADMIN");

        LocalDate searchDate = parseSearchDate(reservationDate);

        List<Reservations> reservations =
                reservationMapper.searchAllForAdmin(reservationId, searchDate);

        prepareModel(model, reservations, "/admin/reservations", true, null);

        // 予約追加フォームには、予約可能な店舗だけを表示する
        model.addAttribute("stores", storesMapper.selectReservableList());

        model.addAttribute("searchReservationId", reservationId);
        model.addAttribute("searchReservationDate", reservationDate);

        return "admin/reservations";
    }

    // 店舗管理者：担当店舗の予約一覧・検索
    @GetMapping("/shop-admin/{storeId}/reservations")
    public String shopAdminReservations(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam(required = false) Integer reservationId,
            @RequestParam(required = false) String reservationDate,
            Model model) {

        int assignedStoreId = requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外にはアクセスできません。");
        }

        Stores store = storesMapper.selectById(assignedStoreId);

        if (store == null) {
            throw new AccessDeniedException("担当店舗が見つかりません。");
        }

        LocalDate searchDate = parseSearchDate(reservationDate);

        List<Reservations> reservations =
                reservationMapper.searchForStoreAdmin(
                        assignedStoreId, reservationId, searchDate);

        prepareModel(
                model,
                reservations,
                "/shop-admin/" + assignedStoreId + "/reservations",
                false,
                store.getName());

        model.addAttribute("stores", List.of(store));
        model.addAttribute("searchReservationId", reservationId);
        model.addAttribute("searchReservationDate", reservationDate);

        return "admin/reservations";
    }

    // モール管理者：予約追加
    // ゲスト氏名・メールアドレスが入力された場合は、
    // usersテーブルの既存ユーザーを検索し、なければ新規登録する。
    @PostMapping("/admin/reservations/add")
    @Transactional
    public String addReservationByMallAdmin(
            Authentication authentication,
            @RequestParam int storeId,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String guestName,
            @RequestParam(required = false) String guestMail,
            RedirectAttributes redirectAttributes) {

        requireRole(authentication, "ROLE_ADMIN");

        try {
            createReservation(
                    storeId,
                    reservationDateTime,
                    number,
                    memberId,
                    userId,
                    guestName,
                    guestMail,
                    true);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約を追加しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/admin/reservations";
    }

    // 店舗管理者：担当店舗の予約追加
    @PostMapping("/shop-admin/{storeId}/reservations/add")
    @Transactional
    public String addReservationByShopAdmin(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer userId,
            RedirectAttributes redirectAttributes) {

        int assignedStoreId = requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外には予約を追加できません。");
        }

        try {
            createReservation(
                    assignedStoreId,
                    reservationDateTime,
                    number,
                    memberId,
                    userId,
                    null,
                    null,
                    false);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約を追加しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/shop-admin/" + assignedStoreId + "/reservations";
    }

    // モール管理者：予約日時・人数の編集
    @PostMapping("/admin/reservations/edit")
    public String editReservationByMallAdmin(
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            RedirectAttributes redirectAttributes) {

        requireRole(authentication, "ROLE_ADMIN");

        try {
            LocalDateTime dateTime =
                    parseReservationDateTime(reservationDateTime);

            // 編集時は過去の日時も許可する
            validateReservation(dateTime, number);

            int updated = reservationMapper.updateForAdmin(
                    reservationId, dateTime, number);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "予約が存在しないか、キャンセル済みです。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約内容を更新しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/admin/reservations";
    }

    // 店舗管理者：担当店舗の予約日時・人数の編集
    @PostMapping("/shop-admin/{storeId}/reservations/edit")
    public String editReservationByShopAdmin(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String reservationDateTime,
            @RequestParam int number,
            RedirectAttributes redirectAttributes) {

        int assignedStoreId = requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外の予約は編集できません。");
        }

        try {
            LocalDateTime dateTime =
                    parseReservationDateTime(reservationDateTime);

            // 編集時は過去の日時も許可する
            validateReservation(dateTime, number);

            int updated = reservationMapper.updateForStore(
                    reservationId, assignedStoreId, dateTime, number);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "予約が存在しない、担当店舗外、またはキャンセル済みです。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage", "予約内容を更新しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/shop-admin/" + assignedStoreId + "/reservations";
    }

    // モール管理者：ステータス変更
    @PostMapping("/admin/reservations/status")
    public String changeStatusByMallAdmin(
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        requireRole(authentication, "ROLE_ADMIN");

        try {
            validateStatus(status);

            int updated = reservationMapper.updateStatusForAdmin(
                    reservationId, status);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "対象の予約が見つかりません。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "予約ID：" + reservationId
                            + " のステータスを「" + status
                            + "」に変更しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/admin/reservations";
    }

    // 店舗管理者：担当店舗のステータス変更
    @PostMapping("/shop-admin/{storeId}/reservations/status")
    public String changeStatusByShopAdmin(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam int reservationId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {

        int assignedStoreId = requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外の予約は変更できません。");
        }

        try {
            validateStatus(status);

            int updated = reservationMapper.updateStatusForStore(
                    reservationId, assignedStoreId, status);

            if (updated == 0) {
                throw new IllegalArgumentException(
                        "担当店舗の対象予約が見つかりません。");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "予約ID：" + reservationId
                            + " のステータスを「" + status
                            + "」に変更しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/shop-admin/" + assignedStoreId + "/reservations";
    }

    // モール管理者：一括更新（AJAX）
    @PostMapping("/admin/reservations/bulk-update")
    @ResponseBody
    @Transactional
    public Map<String, Object> bulkUpdateByMallAdmin(
            Authentication authentication,
            @RequestParam("reservationIds") List<Integer> reservationIds,
            @RequestParam("reservationDateTimes")
            List<String> reservationDateTimes,
            @RequestParam("numbers") List<Integer> numbers,
            @RequestParam("statuses") List<String> statuses) {

        requireRole(authentication, "ROLE_ADMIN");

        return bulkUpdateReservations(
                reservationIds, reservationDateTimes,
                numbers, statuses, null, true);
    }

    // 店舗管理者：担当店舗の予約を一括更新（AJAX）
    @PostMapping("/shop-admin/{storeId}/reservations/bulk-update")
    @ResponseBody
    @Transactional
    public Map<String, Object> bulkUpdateByShopAdmin(
            @PathVariable int storeId,
            Authentication authentication,
            @RequestParam("reservationIds") List<Integer> reservationIds,
            @RequestParam("reservationDateTimes")
            List<String> reservationDateTimes,
            @RequestParam("numbers") List<Integer> numbers,
            @RequestParam("statuses") List<String> statuses) {

        int assignedStoreId = requireShopAdminStoreId(authentication);

        if (assignedStoreId != storeId) {
            throw new AccessDeniedException(
                    "担当店舗以外の予約は変更できません。");
        }

        return bulkUpdateReservations(
                reservationIds, reservationDateTimes,
                numbers, statuses, assignedStoreId, false);
    }

    // 共通：一括更新
    // 全行を検証してから更新する
    private Map<String, Object> bulkUpdateReservations(
            List<Integer> reservationIds,
            List<String> reservationDateTimes,
            List<Integer> numbers,
            List<String> statuses,
            Integer storeId,
            boolean mallAdmin) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (reservationIds == null || reservationDateTimes == null
                || numbers == null || statuses == null
                || reservationIds.size() != reservationDateTimes.size()
                || reservationIds.size() != numbers.size()
                || reservationIds.size() != statuses.size()) {

            result.put("success", false);
            result.put("message",
                    "送信データが正しくありません。画面を更新してやり直してください。");
            result.put("updatedCount", 0);
            return result;
        }

        List<ReservationBulkChange> changes = new ArrayList<>();

        try {
            for (int i = 0; i < reservationIds.size(); i++) {
                Integer id = reservationIds.get(i);

                if (id == null || id <= 0) {
                    throw new IllegalArgumentException(
                            "予約IDが正しくありません。");
                }

                Reservations current;

                if (mallAdmin) {
                    List<Reservations> found =
                            reservationMapper.searchAllForAdmin(id, null);
                    current = found.isEmpty() ? null : found.get(0);
                } else {
                    List<Reservations> found =
                            reservationMapper.searchForStoreAdmin(
                                    storeId, id, null);
                    current = found.isEmpty() ? null : found.get(0);
                }

                if (current == null) {
                    throw new IllegalArgumentException(
                            "予約ID：" + id
                                    + " が見つからないか、変更権限がありません。");
                }

                LocalDateTime newDateTime =
                        parseReservationDateTime(reservationDateTimes.get(i));

                Integer newNumber = numbers.get(i);
                String newStatus = statuses.get(i);

                if (newNumber == null || newNumber < 1 || newNumber > 10) {
                    throw new IllegalArgumentException(
                            "予約ID：" + id
                                    + " の人数は1～10人で入力してください。");
                }

                validateStatus(newStatus);

                LocalDateTime currentDateTime = current.getReservationDate();

                if (currentDateTime != null) {
                    currentDateTime =
                            currentDateTime.withSecond(0).withNano(0);
                }

                boolean dateOrNumberChanged =
                        !newDateTime.equals(currentDateTime)
                        || newNumber.intValue() != current.getNumber();

                boolean statusChanged =
                        !newStatus.equals(current.getStatus());

                if (dateOrNumberChanged) {
                    if (STATUS_CANCELLED.equals(current.getStatus())) {
                        throw new IllegalArgumentException(
                                "予約ID：" + id
                                        + " はキャンセル済みのため、"
                                        + "日時・人数を変更できません。");
                    }

                    // 編集時は過去の日時も許可する
                    validateReservation(newDateTime, newNumber);
                }

                if (dateOrNumberChanged || statusChanged) {
                    changes.add(new ReservationBulkChange(
                            id, newDateTime, newNumber, newStatus,
                            dateOrNumberChanged, statusChanged));
                }
            }
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("updatedCount", 0);
            return result;
        }

        int updatedRows = 0;

        for (ReservationBulkChange change : changes) {
            int updated;

            if (change.dateOrNumberChanged()) {
                if (mallAdmin) {
                    updated = reservationMapper.updateForAdmin(
                            change.reservationId(),
                            change.reservationDateTime(),
                            change.number());
                } else {
                    updated = reservationMapper.updateForStore(
                            change.reservationId(),
                            storeId,
                            change.reservationDateTime(),
                            change.number());
                }

                if (updated == 0) {
                    throw new IllegalStateException(
                            "予約ID：" + change.reservationId()
                                    + " の日時・人数を更新できませんでした。"
                                    + "画面を更新してやり直してください。");
                }
            }

            if (change.statusChanged()) {
                if (mallAdmin) {
                    updated = reservationMapper.updateStatusForAdmin(
                            change.reservationId(), change.status());
                } else {
                    updated = reservationMapper.updateStatusForStore(
                            change.reservationId(), storeId, change.status());
                }

                if (updated == 0) {
                    throw new IllegalStateException(
                            "予約ID：" + change.reservationId()
                                    + " のステータスを更新できませんでした。"
                                    + "画面を更新してやり直してください。");
                }
            }

            updatedRows++;
        }

        result.put("success", true);
        result.put("message",
                updatedRows > 0
                        ? "変更しました"
                        : "変更する項目はありません");
        result.put("updatedCount", updatedRows);

        return result;
    }

    private record ReservationBulkChange(
            int reservationId,
            LocalDateTime reservationDateTime,
            int number,
            String status,
            boolean dateOrNumberChanged,
            boolean statusChanged) {
    }

    // 共通：予約追加
    private void createReservation(
            int storeId,
            String reservationDateTime,
            int number,
            Integer memberId,
            Integer userId,
            String guestName,
            String guestMail,
            boolean mallAdmin) {

        Stores store = storesMapper.selectById(storeId);

        if (store == null) {
            throw new IllegalArgumentException(
                    "指定された店舗が存在しないか、利用できません。");
        }

        // モール管理者の新規予約では、予約可能な店舗だけを許可する
        if (mallAdmin && !Boolean.TRUE.equals(store.isReservable())) {
            throw new IllegalArgumentException(
                    "この店舗は予約を受け付けていません。別の店舗を選択してください。");
        }

        // 会員ID・ゲストユーザーIDが両方入力されている場合
        if (memberId != null && userId != null) {
            throw new IllegalArgumentException(
                    "会員IDとゲストユーザーIDは同時に指定できません。");
        }

        // 会員IDの存在確認・退会確認
        if (memberId != null) {
            if (reservationMapper.countMemberId(memberId) == 0) {
                throw new IllegalArgumentException(
                        "指定された会員IDが存在しません。");
            }

            if (reservationMapper.countDeletedMemberId(memberId) > 0) {
                throw new IllegalArgumentException(
                        "退会済みです。予約できません。");
            }
        }

        // ゲストユーザーIDが直接指定された場合の存在確認
        if (userId != null
                && reservationMapper.countUserId(userId) == 0) {
            throw new IllegalArgumentException(
                    "指定されたゲストユーザーIDが存在しません。");
        }

        LocalDateTime dateTime =
                parseReservationDateTime(reservationDateTime);

        // 新規予約では過去の日時を禁止する
        validateNewReservation(dateTime, number);

        // 会員ID・ゲストユーザーIDが未入力の場合
        if (memberId == null && userId == null) {

            // ゲスト氏名・メールアドレスから登録できるのはモール管理者のみ
            if (!mallAdmin) {
                throw new IllegalArgumentException(
                        "会員IDかゲストユーザーIDのどちらかにIDを入力してください。");
            }

            String normalizedGuestName =
                    guestName == null ? "" : guestName.trim();

            String normalizedGuestMail =
                    guestMail == null ? "" : guestMail.trim();

            if (normalizedGuestName.isEmpty()) {
                throw new IllegalArgumentException(
                        "ゲストの氏名を入力してください。");
            }

            if (normalizedGuestMail.isEmpty()) {
                throw new IllegalArgumentException(
                        "ゲストのメールアドレスを入力してください。");
            }

            if (normalizedGuestMail.length() > 255) {
                throw new IllegalArgumentException(
                        "メールアドレスは255文字以内で入力してください。");
            }

            if (!EMAIL_PATTERN.matcher(normalizedGuestMail).matches()) {
                throw new IllegalArgumentException(
                        "メールアドレスの形式が正しくありません。");
            }

            // 同じメールアドレスのユーザーが存在する場合は、そのIDを再利用する
            Users existingUser =
                    usersMapper.findByMail(normalizedGuestMail);

            if (existingUser != null) {
                userId = existingUser.getId();
            } else {
                // 未登録のメールアドレスならゲストユーザーを新規作成する
                Users newUser = new Users();
                newUser.setName(normalizedGuestName);
                newUser.setMail(normalizedGuestMail);

                usersMapper.insert(newUser);

                // MyBatisのuseGeneratedKeysで採番されたIDを取得する
                userId = newUser.getId();

                if (userId == null || userId <= 0) {
                    throw new IllegalStateException(
                            "ゲストユーザーIDを取得できませんでした。");
                }
            }
        }

        Reservations reservation = new Reservations();
        reservation.setMemberId(memberId);
        reservation.setUserId(userId);
        reservation.setStoreId(storeId);
        reservation.setReservationDate(dateTime);
        reservation.setNumber(number);
        reservation.setStatus(STATUS_RESERVED);
        reservation.setCreatedAt(LocalDateTime.now());

        reservationMapper.insertReservation(reservation);
    }

    // 共通：画面表示用データ
    private void prepareModel(
            Model model,
            List<Reservations> reservations,
            String basePath,
            boolean mallAdmin,
            String currentStoreName) {

        // 既存予約の店舗名表示には、予約可否を問わず全店舗を使う
        List<Stores> stores = storesMapper.selectList();
        Map<Integer, String> storeNames = new LinkedHashMap<>();

        for (Stores store : stores) {
            storeNames.put(store.getId(), store.getName());
        }

        // トップ画面のURLを管理者の種類に応じて設定
        String topPath;

        if (mallAdmin) {
            // モール管理者のトップ画面
            topPath = "/admin/index";
        } else {
            // 店舗管理者の担当店舗トップ画面
            topPath = basePath.substring(
                    0, basePath.lastIndexOf("/reservations"));
        }

        model.addAttribute("reservations", reservations);
        model.addAttribute("storeNames", storeNames);
        model.addAttribute("basePath", basePath);
        model.addAttribute("mallAdmin", mallAdmin);
        model.addAttribute("currentStoreName", currentStoreName);
        model.addAttribute("reservationStatuses", ALLOWED_STATUSES);

        // トップ画面へのリンク
        model.addAttribute("topPath", topPath);
    }

    // 共通：権限チェック
    private void requireRole(
            Authentication authentication,
            String requiredRole) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                        .noneMatch(a ->
                                requiredRole.equals(a.getAuthority()))) {
            throw new AccessDeniedException(
                    "この操作を行う権限がありません。");
        }
    }

    // 店舗管理者のログインIDから担当店舗IDを取得
    // Authentication.getName() が shop_admins.id である前提
    private int requireShopAdminStoreId(
            Authentication authentication) {

        requireRole(authentication, "ROLE_SHOP_ADMIN");

        final int shopAdminId;

        try {
            shopAdminId = Integer.parseInt(authentication.getName());
        } catch (NumberFormatException | NullPointerException e) {
            throw new AccessDeniedException(
                    "店舗管理者のログインIDを確認できません。");
        }

        ShopAdmin shopAdmin = shopAdminMapper.findById(shopAdminId);

        if (shopAdmin == null) {
            throw new AccessDeniedException(
                    "店舗管理者が見つかりません。");
        }

        Integer assignedStoreId = shopAdmin.getStoreId();

        if (assignedStoreId == null || assignedStoreId <= 0) {
            throw new AccessDeniedException(
                    "担当店舗が設定されていません。");
        }

        return assignedStoreId;
    }

    // 共通：検索日チェック
    private LocalDate parseSearchDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "検索日はYYYY-MM-DD形式で指定してください。");
        }
    }

    // 共通：予約日時チェック
    private LocalDateTime parseReservationDateTime(String value) {

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "予約日時を正しく入力してください。");
        }
    }

    // 新規予約用：過去の日時は登録できない
    private void validateNewReservation(
            LocalDateTime reservationDate,
            int number) {

        validateReservation(reservationDate, number);

        if (reservationDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "新規予約に過去の日時は指定できません。");
        }
    }

    // 予約編集用：過去の予約日時も変更できる
    private void validateReservation(
            LocalDateTime reservationDate,
            int number) {

        if (reservationDate == null) {
            throw new IllegalArgumentException(
                    "予約日と予約時刻を入力してください。");
        }

        if (number < 1 || number > 10) {
            throw new IllegalArgumentException(
                    "人数は1～10人で入力してください。");
        }
    }

    // 共通：ステータスチェック
    private void validateStatus(String status) {

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "指定されたステータスは使用できません。");
        }
    }
}