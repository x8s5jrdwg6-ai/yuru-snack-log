package com.example.demo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
		// modelに格納
		model.addAttribute("targetDate", targetDate.toString());
		// 合計カロリー取得
		model.addAttribute("totalKcal", intakeSvc.calcTotalCal(userId, targetDate));
		// getDailyRecordsで対象日の履歴を取得
		model.addAttribute("targetDateAteRecords", intakeSvc.getDailyRecords(userId, targetDate));
		model.addAttribute("prevDate", targetDate.minusDays(1).toString());
		model.addAttribute("todayDate", LocalDate.now().toString());
		model.addAttribute("nextDate", targetDate.plusDays(1).toString());
		
		model.addAttribute("forDetailDate", targetDate.toString());

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
	
	// ナビゲーション：メーカー登録押下時
	@GetMapping("/makers/new")
	public String makerNew(HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		return "maker_new";
	}
	
	
	// ナビゲーション：食品情報登録押下時
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
	
	// ナビゲーション：栄養情報登録押下時
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
				"SELECT food_id, food_name FROM food WHERE regist_user_id=? ORDER BY food_name",
				userId
				);

		model.addAttribute("foods", foods);
		model.addAttribute("selectedFoodId", foodId); // 初期選択用
		model.addAttribute("error", error);
		return "nutrition_new";
	}
	
	/*--------------------------------------
		詳細画面
	--------------------------------------*/
	// 削除押下時
	@PostMapping("/intake/delete")
	public String delete(@RequestParam("intakeId") long intakeId,
			@RequestParam("hiddenDate") String hiddenDate,
            HttpServletRequest req,
            HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		
		intakeSvc.delIntake(userId, intakeId);
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
	
	// メニュー押下時
	@GetMapping("/menu")
	public String menu(HttpServletRequest req, HttpServletResponse res) {
		// user_id取得処理（仮）
		String userId = resolveUserId(req, res);
		return "menu";
	}

}
