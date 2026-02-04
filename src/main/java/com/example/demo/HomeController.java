package com.example.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class HomeController {
	/*--------------------------------------
		定数
	--------------------------------------*/
	private final JdbcTemplate jdbc;
	private final IntakeService intakeSvc;

	private static final String UID_COOKIE = "cc_uid";

	/*--------------------------------------
		record
	--------------------------------------*/
	public record IntakeRow(long intakeId, String eatenDate, String eatenTime, double qty, String foodName, String className, int calorie) {
		public int kcalTotal() {
			return (int) Math.round(calorie * qty);
		}
	}
	
	/*--------------------------------------
		共通
	--------------------------------------*/
	@ControllerAdvice
	public class CommonModelAdvice {

	    @Value("${app.version:dev}")
	    private String appVersion;

	    @ModelAttribute("appVersion")
	    public String appVersion() {
	        return appVersion;
	    }
	}
	
	// ユーザーIDの取得・登録処理を行う
	private String resolveUserId(HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理
		String userId = intakeSvc.chkUserId(req, UID_COOKIE);

		if(!userId.isEmpty()) {
			// 最終アクセス日時を更新
			intakeSvc.updLastAccessDate(userId);
			return userId;
		}
		
		// 無ければ新規UUIDをuser_idとしてusersテーブルに登録
		userId = intakeSvc.registUserId();
		
	    res.addCookie(intakeSvc.setCookie(UID_COOKIE, userId, req));
	    return userId;
	}
	

	public HomeController(JdbcTemplate jdbc, IntakeRepository intakeRepo, IntakeService intakeSvc) {
		this.jdbc = jdbc;
		this.intakeSvc = intakeSvc;
	}
	
	/*--------------------------------------
		画面遷移
	--------------------------------------*/
	// 初期表示想定
	// 日付選択時
	// 「Homeへ戻る」押下時
	@GetMapping("/")
	public String home(@RequestParam(name = "date", required = false) String date, Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		// 三項演算子（条件 ? 真のときの値 : 偽のときの値）
		LocalDate targetDate = (date == null || date.isBlank()) ? LocalDate.now(): LocalDate.parse(date);
		
		// 合計カロリー取得
		int totalKcal = intakeSvc.calcTotalCal(userId, targetDate);
		// getDailyRecordsで対象日の履歴を取得
		List<IntakeRow> targetDateAteRecords = intakeSvc.getDailyRecords(userId, targetDate);
		
		LocalDate today = LocalDate.now();
		
		// modelに格納
		model.addAttribute("targetDate", targetDate.toString());
		model.addAttribute("totalKcal", totalKcal);
		model.addAttribute("targetDateAteRecords", targetDateAteRecords);
		model.addAttribute("prevDate", targetDate.minusDays(1).toString());
		model.addAttribute("todayDate", today.toString());
		model.addAttribute("nextDate", targetDate.plusDays(1).toString());
		model.addAttribute("forDetailDate", targetDate.toString());
		
		// 取得した履歴の範囲で乱数を作成
		int r = intakeSvc.generateRandomIndex(targetDateAteRecords);
		
		// ポストのメッセージを設定
		String postMsg = intakeSvc.getPostMsg(today, targetDate, totalKcal, targetDateAteRecords, r);
		
		model.addAttribute("postMessage", postMsg);
		
		return "home";
	}
	
	
	// お知らせ押下時
	@GetMapping("/info")
	public String info(HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		return "information";
	}
	
	// 使い方押下時
	@GetMapping("/howto")
	public String howToUse(HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		return "how_to_use";
	}
	
	// TOP画面：詳細押下時
	@GetMapping("/intake/detail")
	public String intakeDetail(@RequestParam("intakeId") long intakeId,
			@RequestParam("date") String date,
			Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// IDから詳細情報を取得
		Map<String, Object> map = intakeSvc.getIntakeDetail(userId, intakeId);
		if(map == null) {
			model.addAttribute("errorMsg", "該当の履歴が見つかりません");
			return "home";
		}
		
		model.addAttribute("targetDate", date);
		model.addAttribute("targetId", map.get("intakeId"));
		model.addAttribute("intakeId", map.get("intakeId"));
		model.addAttribute("eatenDatetime", (map.get("eatenDate") + " " + map.get("eatenTime")));
		model.addAttribute("makerName", map.get("makerName"));
		model.addAttribute("foodName", map.get("foodName"));
		model.addAttribute("className", map.get("className"));
		model.addAttribute("calorie", map.get("calorie"));
		model.addAttribute("protein", map.get("protein"));
		model.addAttribute("lipid", map.get("lipid"));
		model.addAttribute("carbo", map.get("carbo"));
		model.addAttribute("salt", map.get("salt"));
		
		return "intake_detail";
	}
	
	// 詳細画面：編集押下時
	@GetMapping("/intake/edit")
	public String intakeEdit(@RequestParam("intakeId") long intakeId,
			@RequestParam("date") String date,
			Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// IDから詳細情報を取得
		Map<String, Object> map = intakeSvc.getIntakeDetail(userId, intakeId);
		if(map == null) {
			model.addAttribute("errorMsg", "該当の履歴が見つかりません");
			return "home";
		}
				
		model.addAttribute("targetDate", date);
		model.addAttribute("intakeId", map.get("intakeId"));
		model.addAttribute("eatenDate", (map.get("eatenDate")));
		model.addAttribute("eatenTime", (map.get("eatenTime")));
		model.addAttribute("foodName", map.get("foodName"));
		model.addAttribute("className", map.get("className"));
		model.addAttribute("calorie", map.get("calorie"));

		return "intake_edit";
	}
	
	// ナビゲーション：食べた押下時
	// 食べた登録画面：食品選択画面：「←メーカー選択へ戻る」押下時
	@GetMapping("/eat")
	public String eatMaker(Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDに紐づくメーカー一覧を取得
		List<Map<String, Object>> makers = intakeSvc.getMakerList(userId);
		
		model.addAttribute("makers", makers);
		return "eat_maker";
	}
	
	// 食べた登録画面：メーカ選択画面：メーカー選択時
	@GetMapping("/eat/foods")
	public String eatFoods(@RequestParam("makerId") long makerId, Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 表示するメーカー名を取得
		String makerName = intakeSvc.getMakerName(userId, makerId);

		// ユーザーIDと選択したメーカーに紐づく食品一覧を取得
		List<Map<String, Object>> foods = intakeSvc.getFoodList(userId, makerId);
		
		model.addAttribute("makerId", makerId);
		model.addAttribute("makerName", makerName);
		model.addAttribute("foods", foods);
		return "eat_food";
	}
	
	// 食べた登録画面：食品選択画面：食品選択時
	@GetMapping("/eat/nutritions")
	public String eatNutritions(@RequestParam("foodId") long foodId,
			@RequestParam(name="error", required=false) String error,
			Model model,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDと食品IDに紐づく食品情報とメーカ情報を取得
		Map<String, Object> headerInfo = intakeSvc.getHeaderInfo(userId, foodId);

		// ユーザーIDと食品IDに紐づく分類一覧を取得
		List<Map<String, Object>> nutritionList = intakeSvc.getNutritionList(userId, foodId);
		
		model.addAttribute("header", headerInfo);
		model.addAttribute("nutritions", nutritionList);
		model.addAttribute("error", error);
		return "eat_nutrition";
	}
	
	// メニュー：メーカー登録押下時
	@GetMapping("/makers/new")
	public String makerNew(HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		return "maker_new";
	}
	
	
	// メニュー：食品情報登録押下時
	@GetMapping("/foods/new")
	public String foodNew(Model model,
			@RequestParam(name="makerId", required=false) Long makerId,
			@RequestParam(name="error", required=false) String error,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// メーカー一覧を取得
		List<Map<String, Object>> makers = jdbc.queryForList(
				"SELECT maker_id, maker_name FROM maker WHERE regist_user_id=? ORDER BY maker_name",
				userId
				);

		model.addAttribute("makers", makers);
		model.addAttribute("selectedMakerId", makerId); // 初期選択用
		model.addAttribute("error", error);
		return "food_new";
	}
	
	// メニュー：栄養情報登録押下時
	// 食品情報登録画面：「食品を選択して栄養情報登録へ進む」押下時
	@GetMapping("/nutritions/new")
	public String nutritionNew(Model model,
			@RequestParam(name="foodId", required=false) Long foodId,
			@RequestParam(name="error", required=false) String error,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 食品一覧を取得
		List<Map<String, Object>> foods = jdbc.queryForList(
				"SELECT food_id, food_name FROM food f INNER JOIN maker m ON m.maker_id = f.maker_id WHERE regist_user_id=? ORDER BY food_name",
				userId
				);

		model.addAttribute("foods", foods);
		model.addAttribute("selectedFoodId", foodId); // 初期選択用
		model.addAttribute("error", error);
		return "nutrition_new";
	}
	
	
	// メニュー押下時
	@GetMapping("/menu")
	public String menu(HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		return "menu";
	}
	
	// メニュー：メーカー一覧押下時
	@GetMapping("/list/maker")
	public String listMaker(@RequestParam(name="makerId", required=false) Long makerId,
			@RequestParam(name="makerName", required=false) String makerName,
			Model model,HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		// ユーザーIDに紐づくメーカー一覧を取得
		List<Map<String, Object>> makers = intakeSvc.getMakerList(userId);
		
		model.addAttribute("makers", makers);
		return "list_maker";
	}
	
	// メーカー一覧：選択時
	@GetMapping("/edit/maker")
	public String editMaker(@RequestParam(name="makerId", required=false) Long makerId,
			@RequestParam(name="makerName", required=false) String makerName,
			Model model,
			HttpServletRequest req,
			HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		model.addAttribute("makerId", makerId);
		model.addAttribute("makerName", makerName);
		
		return "edit_maker";
	}
	
	// メニュー：食品情報一覧押下
	@GetMapping("/list/food")
	public String listFood(
//			@RequestParam(name="foodId", required=false) Long foodId,
//			@RequestParam(name="foodName", required=false) String foodName,
			Model model,
			HttpServletRequest req,
			HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDと選択したメーカーに紐づく食品一覧を取得
		List<Map<String, Object>> foods = intakeSvc.getFoodListAll(userId);
		
		model.addAttribute("foods", foods);
		
//		model.addAttribute("foodId", foodId);
//		model.addAttribute("foodName", foodName);
		
		return "list_food";
	}
	
	// 食品情報一覧：選択時
	@GetMapping("/edit/food")
	public String editFood(@RequestParam(name="makerId", required=false) Long makerId,
			@RequestParam(name="foodId", required=false) Long foodId,
			@RequestParam(name="foodName", required=false) String foodName,
			Model model,
			HttpServletRequest req,
			HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		model.addAttribute("makerId", makerId);
		model.addAttribute("foodId", foodId);
		model.addAttribute("foodName", foodName);
		
		return "edit_food";
	}
	
	// メニュー：食品情報一覧押下
	@GetMapping("/list/nutrition")
	public String listNutrition(
			Model model,
			HttpServletRequest req,
			HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDと選択したメーカーに紐づく食品一覧を取得
		List<Map<String, Object>> nutritions = intakeSvc.getNutritionListAll(userId);
		
		model.addAttribute("nutritions", nutritions);
		
		
		return "list_nutrition";
	}
	
	// 食品情報一覧：選択時
	@GetMapping("/nutrition/detail")
	public String nutritionDetail(@RequestParam(name="nutritionId", required=false) Long nutritionId,
			Model model,
			HttpServletRequest req,
			HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDと食品IDに紐づく食品情報とメーカ情報を取得
		Map<String, Object> nutritionInfo = intakeSvc.getNutritionInfo(userId, nutritionId);

		model.addAttribute("makerId", nutritionInfo.get("makerId").toString());
		model.addAttribute("foodId", nutritionInfo.get("foodId").toString());
		model.addAttribute("nutritionId", nutritionId);
		model.addAttribute("makerName", nutritionInfo.get("makerName").toString());
		model.addAttribute("foodName", nutritionInfo.get("foodName").toString());
		model.addAttribute("className", nutritionInfo.get("className").toString());
		model.addAttribute("calorie", nutritionInfo.get("calorie").toString());
		model.addAttribute("protein", nutritionInfo.get("protein").toString());
		model.addAttribute("lipid", nutritionInfo.get("lipid").toString());
		model.addAttribute("carbo", nutritionInfo.get("carbo").toString());
		model.addAttribute("salt", nutritionInfo.get("salt").toString());
		
		return "nutrition_detail";
	}
	
	// 食品情報詳細：編集押下時
	@GetMapping("/edit/nutrition")
	public String editNutrition(@RequestParam(name="nutritionId", required=false) Long nutritionId,
			Model model,
			HttpServletRequest req,
			HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// ユーザーIDと食品IDに紐づく食品情報とメーカ情報を取得
		Map<String, Object> nutritionInfo = intakeSvc.getNutritionInfo(userId, nutritionId);

		model.addAttribute("makerId", nutritionInfo.get("makerId").toString());
		model.addAttribute("foodId", nutritionInfo.get("foodId").toString());
		model.addAttribute("nutritionId", nutritionId);
		model.addAttribute("makerName", nutritionInfo.get("makerName").toString());
		model.addAttribute("foodName", nutritionInfo.get("foodName").toString());
		model.addAttribute("className", nutritionInfo.get("className").toString());
		model.addAttribute("calorie", nutritionInfo.get("calorie").toString());
		model.addAttribute("protein", nutritionInfo.get("protein").toString());
		model.addAttribute("lipid", nutritionInfo.get("lipid").toString());
		model.addAttribute("carbo", nutritionInfo.get("carbo").toString());
		model.addAttribute("salt", nutritionInfo.get("salt").toString());
		
		return "edit_nutrition";
	}
	
	/*--------------------------------------
		詳細画面
	--------------------------------------*/
	// 削除押下時
	@PostMapping("/intake/delete")
	public String delete(@RequestParam("intakeId") long intakeId,
			@RequestParam("hiddenDate") String hiddenDate,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 削除実行
		if(intakeSvc.delIntake(userId, intakeId) != 0) {
			ra.addFlashAttribute("msg", "食べた！履歴を削除しました");
			return "redirect:/?date=" + hiddenDate;
		}
		
		ra.addFlashAttribute("errorMsg", "削除に失敗しました");
		return "redirect:/?date=" + hiddenDate;
	}
	
	@PostMapping("/intake/update")
	public String intakeUpdate(
			@RequestParam("intakeId") long intakeId,
			@RequestParam("eatenDate") LocalDate eatenDate,
			@RequestParam("eatenTime") LocalTime eatenTime,
			@RequestParam("hiddenDate") String hiddenDate,
            HttpServletRequest req,
            HttpServletResponse res
			){
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		intakeSvc.updIntake(userId, intakeId, eatenDate, eatenTime);

		return "redirect:/intake/detail?intakeId=" + intakeId + "&date=" + hiddenDate;
	}


	/*--------------------------------------
		メーカー登録画面
	--------------------------------------*/
	// メーカー登録時
	@PostMapping("/makers/create-and-next")
	public String makerCreateAndNext(@RequestParam("makerName") String makerName,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// メーカー重複チェック true：重複なし false：重複あり
		if(!intakeSvc.chkDepliMaker(userId, makerName)) {
			ra.addFlashAttribute("errorMsg", "同じメーカーが既に登録されています");
			return "redirect:/makers/new";
		}

		// メーカー登録
		int foodMakerId = intakeSvc.insFoodMaker(userId, makerName);
		
		ra.addFlashAttribute("msg", "メーカーを登録しました。続けて食品情報を登録してください");
		return "redirect:/foods/new?makerId=" + Integer.toString(foodMakerId);
	}


	/*--------------------------------------
		食品登録画面
	--------------------------------------*/
	// 食品登録時
	@PostMapping("/foods/create-and-next")
	public String foodCreateAndNext(@RequestParam("makerId") int makerId,
			@RequestParam("foodName") String foodName,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 食品重複チェック true：重複なし false：重複あり
		if(!intakeSvc.chkDepliFood(userId, foodName, makerId)) {
			ra.addFlashAttribute("errorMsg", "同じメーカー内に同名の食品が既に登録されています");
			return "redirect:/foods/new";
		}
		// 食品情報登録
		int foodId = intakeSvc.insFood(userId, foodName, makerId);

		ra.addFlashAttribute("msg", "食品を登録しました。続けて栄養情報を登録してください");
		return "redirect:/nutritions/new?foodId=" + Integer.toString(foodId);
	}

	/*--------------------------------------
		栄養情報登録画面
	--------------------------------------*/
	@PostMapping("/nutritions/create")
	public String nutritionCreate(
			@RequestParam("foodId") long foodId,
			@RequestParam("className") String className,
			@RequestParam("calorie") int calorie,
			@RequestParam(name="protein", required=false) Double protein,
			@RequestParam(name="lipid", required=false) Double lipid,
			@RequestParam(name="carbo", required=false) Double carbo,
			@RequestParam(name="salt", required=false) Double salt,
//			@RequestParam(name="plainFlg", required=false) String plainFlg,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res
			) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 栄養情報重複チェック true：重複なし false：重複あり
		if(!intakeSvc.chkDepliNutrition(userId, className, foodId)) {
			ra.addFlashAttribute("errorMsg", "同じ分類の栄養情報が既に登録されています");
			return "redirect:/nutritions/new";
		}

		// 栄養情報登録
		intakeSvc.insNutrition(userId, className, foodId, calorie, protein, lipid, carbo, salt);

		ra.addFlashAttribute("msg", "栄養情報を登録しました");
		return "redirect:/nutritions/new";
	}

	/*--------------------------------------
		食べた登録画面
	--------------------------------------*/
	// 食べた！押下時
	@PostMapping("/eat/record")
	public String eatRecord(@RequestParam("nutritionId") long nutritionId,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		intakeSvc.insIntake(userId, nutritionId);
		ra.addFlashAttribute("msg", "食べた！を記録しました。");
		return "redirect:/";
	}
	
	/*--------------------------------------
		メーカー編集画面
	--------------------------------------*/
	@PostMapping("/edit/maker")
	public String editMaker(@RequestParam("makerId") long makerId,
			@RequestParam("makerName") String makerName,
			@RequestParam(value="deleteFlg", required=false) String  deleteFlg,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 削除押下なら削除実行
		if(deleteFlg.equals("true")) {
			if(intakeSvc.delMaker(userId, makerId) != 0) {
				ra.addFlashAttribute("msg", "メーカーを削除しました");
				return "redirect:/list/maker";
			}
			ra.addFlashAttribute("errorMsg", "削除に失敗しました");
			return "redirect:/list/maker";
		}
		
		// メーカー名必須チェック
		if(makerName.isEmpty()) {
			ra.addFlashAttribute("errorMsg", "メーカー名を入力してください");
			return "redirect:/edit/maker?makerId=" + makerId;
		}
		
		// メーカー重複チェック true：重複なし false：重複あり
		if(!intakeSvc.chkDepliMaker(userId, makerName)) {
			ra.addFlashAttribute("errorMsg", "同じメーカーが既に登録されています");
			return "redirect:/edit/maker?makerId=" + makerId;
		}
		
		// 更新実行
		if(intakeSvc.updMaker(makerId, makerName) != 0) {
			ra.addFlashAttribute("msg", "メーカー名を更新しました");
		}else {
			ra.addFlashAttribute("errorMsg", "更新に失敗しました");
		}
		
		return "redirect:/list/maker";
	}
	
	/*--------------------------------------
		食品情報編集画面
	--------------------------------------*/
	@PostMapping("/edit/food")
	public String editFood(@RequestParam("makerId") long makerId,
			@RequestParam("foodId") long foodId,
			@RequestParam("foodName") String foodName,
			@RequestParam(value="deleteFlg", required=false) String  deleteFlg,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 削除押下なら削除実行
		if(deleteFlg.equals("true")) {
			if(intakeSvc.delFood(userId, foodId) != 0) {
				ra.addFlashAttribute("msg", "食品情報を削除しました");
				return "redirect:/list/food";
			}
			ra.addFlashAttribute("errorMsg", "削除に失敗しました");
			return "redirect:/list/food";
		}
		
		// メーカー名必須チェック
		if(foodName.isEmpty()) {
			ra.addFlashAttribute("errorMsg", "食品名を入力してください");
			return "redirect:/edit/food?makerId=" + makerId + "&foodId=" + foodId;
		}
		
		// メーカー重複チェック true：重複なし false：重複あり
		if(!intakeSvc.chkDepliFood(userId, foodName, makerId)) {
			ra.addFlashAttribute("errorMsg", "同一メーカーで重複する食品名が既に登録されています");
			return "redirect:/edit/food?makerId=" + makerId + "&foodId=" + foodId;
		}
		
		// 更新実行
		if(intakeSvc.updFood(userId, foodId, foodName) != 0) {
			ra.addFlashAttribute("msg", "食品情報を更新しました");
		}else {
			ra.addFlashAttribute("errorMsg", "更新に失敗しました");
		}
		
		return "redirect:/list/food";
	}
	
	/*--------------------------------------
		栄養情報編集画面
	--------------------------------------*/
	@PostMapping("/edit/nutrition")
	public String editNutrition(@RequestParam("nutritionId") long nutritionId,
			@RequestParam("className") String className,
			@RequestParam("calorie") int calorie,
			@RequestParam(value="protein", required=false) BigDecimal protein,
			@RequestParam(value="lipid",   required=false) BigDecimal lipid ,
			@RequestParam(value="carbo",   required=false) BigDecimal carbo ,
			@RequestParam(value="salt",    required=false) BigDecimal salt,
			@RequestParam(value="deleteFlg", required=false) String deleteFlg,
			RedirectAttributes ra,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		// 削除押下なら削除実行
		if(deleteFlg.equals("true")) {
			if(intakeSvc.delNutrition(userId, nutritionId) != 0) {
				ra.addFlashAttribute("msg", "栄養情報を削除しました");
				return "redirect:/list/nutrition";
			}
			ra.addFlashAttribute("errorMsg", "削除に失敗しました");
			return "redirect:/list/nutrition";
		}
		
		// 分類名必須チェック
		if(className.isEmpty()) {
			ra.addFlashAttribute("errorMsg", "分類名を入力してください");
			return "redirect:/edit/nutrition?nutritionId=" + nutritionId + "&=calorie" + calorie + "&=protein" +protein + "&=lipid" + lipid + "&=carbo" + carbo + "&=salt" + salt;

		}
		
		// 更新実行
		if(intakeSvc.updNutrition(userId, nutritionId, className, calorie, protein, lipid, carbo, salt) != 0) {
			ra.addFlashAttribute("msg", "栄養情報を更新しました");
		}else {
			ra.addFlashAttribute("errorMsg", "更新に失敗しました");
		}
		
		return "redirect:/list/nutrition";
	}
}
