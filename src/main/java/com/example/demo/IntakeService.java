package com.example.demo;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.HomeController.IntakeRow;

@Service
public class IntakeService {

	private final IntakeRepository intakeRepo;

	public IntakeService(IntakeRepository intakeRepo) {
		this.intakeRepo = intakeRepo;
	}
	
	public List<Map<String, Object>> getFavoriteList(String userId){
		return intakeRepo.getFavoriteList(userId);
	}
	
	public List<IntakeRow> getDailyRecords(String userId, LocalDate date) {
		return intakeRepo.getDailyRecords(userId, date);
	}
	
	public int generateRandomIndex(List<IntakeRow> targetDateAteRecords) {
		int getCnt = targetDateAteRecords.size();
		if(getCnt == 1) {
			return 0;
		}else if(getCnt == 0) {
			return 999;	// 実質エラー
		}
		return ThreadLocalRandom.current().nextInt(0,getCnt);
	}
	
	public int calcTotalCal(String userId, LocalDate eatenDate) {
		return intakeRepo.calcTotalCal(userId, eatenDate);
	}

	public int insIntake(String userId, long nutritionId) {
		LocalDateTime now = LocalDateTime.now();
		return intakeRepo.insIntake(userId, nutritionId, now.toLocalDate(), now.toLocalTime());
	}
	
	public void updIntake(String userId, long intakeId, LocalDate eatenDate, LocalTime eatenTime) {
		intakeRepo.updIntake(userId, intakeId, eatenDate, eatenTime);
	}
	
	public int delIntake(String userId, long intakeId) {
		return intakeRepo.delIntake(intakeId, userId);
	}
	
	public int insFoodMaker(String userId, String makerName) {
		return intakeRepo.insFoodMaker(userId, makerName);
	}
	
	public boolean chkDepliFood(String userId, String foodName, long makerId) {
		//重複チェック
		if(intakeRepo.chkDepliFood(userId, foodName, makerId)) {
			return false;	// エラー
		}
		return true;
	}
	
	public boolean chkDepliFood(String userId, String foodName, long makerId, long foodId) {
		//重複チェック
		if(intakeRepo.chkDepliFood(userId, foodName, makerId, foodId)) {
			return false;	// エラー
		}
		return true;
	}
	
	public int insFood(String userId, String foodName, long makerId) {
		return intakeRepo.insFood(userId, foodName, makerId);
	}
	
	public long getRegistFoodId(String userId, String foodName, long makerId) {
		return intakeRepo.getRegistFoodId(userId, foodName, makerId);
	}
	
	public boolean chkDepliMaker(String userId, String makerName) {
		if(intakeRepo.chkDepliMaker(userId, makerName)) {
			return false;	// エラー
		}
		return true;
	}
	
	public boolean chkDepliNutrition(String userId, String className, long foodId) {
		//重複チェック
		if(intakeRepo.chkDepliNutrition(userId, className, foodId)) {
			return false;	// エラー
		}
		return true;
	}
	
	public void insNutrition(String userId, String className, long foodId, int calorie, Double protein, Double lipid, Double carbo, Double salt) {
		intakeRepo.insNutrition(userId, className, foodId, calorie, protein, lipid, carbo, salt);
		
	}
	
	public Map<String, Object> getIntakeDetail(String userId, long intakeId) {
		return intakeRepo.getIntakeDetail(userId, intakeId);
	}
	
	public List<Map<String, Object>> getMakerList(String userId){
		return intakeRepo.getMakerList(userId);
	}
	
	public String getMakerName(String userId, long makerId) {
		return intakeRepo.getMakerName(userId, makerId);
	}
	
	public List<Map<String, Object>> getFoodList(String userId, long makerId){
		return intakeRepo.getFoodList(userId, makerId);
	}
	
	public List<Map<String, Object>> getFoodListAll(String userId){
		return intakeRepo.getFoodListAll(userId);
	}
	
	public Map<String, Object> getHeaderInfo (String userId, long foodId) {
		return intakeRepo.getHeaderInfo(userId, foodId);
	}
	
	public List<Map<String, Object>> getNutritionList(String userId, long foodId){
		return intakeRepo.getNutritionList(userId, foodId);
	}
	
	public List<Map<String, Object>> getNutritionListAll(String userId){
		return intakeRepo.getNutritionListAll(userId);
	}
	
	public Map<String, Object> getNutritionInfo (String userId, long nutritionId) {
		return intakeRepo.getNutritionInfo(userId, nutritionId);
	}
	
	public String chkUserId(HttpServletRequest req, String uidCookie) {
		String userId = "";
		if (req.getCookies() != null) {
	        for (Cookie c : req.getCookies()) {
	            if (uidCookie.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
	            	userId = intakeRepo.chkUserId(c);
	            }
	            if(!userId.isEmpty()) {
	            	break;
	            }
	        }
	    }
		return userId;
	}
	
	public String registUserId() {
		String userId = UUID.randomUUID().toString();
		intakeRepo.registUserId(userId);
		return userId;
	}
	
	public Cookie setCookie(String uidCookie, String userId, HttpServletRequest req) {
		// Cookie保存
	    Cookie cookie = new Cookie(uidCookie, userId);
	    cookie.setPath("/");
	    cookie.setHttpOnly(true);
	    cookie.setMaxAge(60 * 60 * 24 * 365); // 1年

	    // Renderはリバプロなのでヘッダを見る
	    String xfProto = req.getHeader("X-Forwarded-Proto");
	    boolean https = "https".equalsIgnoreCase(xfProto) || req.isSecure();
	    cookie.setSecure(https);

	    // SameSite=Lax（Servlet APIの都合で setAttribute 方式）
	    cookie.setAttribute("SameSite", "Lax");
	    
	    return cookie;
	}
	
	public void updLastAccessDate(String userId) {
		intakeRepo.updLastAccessDate(userId);
	}
	
	public int updMaker(long makerId, String makerName) {
		return intakeRepo.updMaker(makerId, makerName);
	}
	
	public int updFood(String userId, long foodId, String foodName) {
		return intakeRepo.updFood(userId, foodId, foodName);
	}
	
	public int updNutrition(String userId, long nutritionId, String className, int calorie, BigDecimal protein, BigDecimal lipid , BigDecimal carbo , BigDecimal salt) {
		return intakeRepo.updNutrition(userId, nutritionId, className, calorie, protein, lipid, carbo, salt);
	}
	
	public String getPostMsg(LocalDate today, LocalDate targetDate, int totalKcal, List<IntakeRow> targetDateAteRecords, int r) {
		String postDate= "";
		String message = "";
		boolean futureFlg = false;
		
		if (targetDate.equals(today)) {
		    // targetDate が「今日」のとき
			postDate = "本日";
		}else {
			// 日付が未来ならフラグをオン
			if(targetDate.isAfter(today)){
				futureFlg = true;
			}
			// formatで変換
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年M月d日");
			postDate = targetDate.format(fmt);
		}
		
		if(futureFlg) {
			message = postDate + "はまだ来てないよ\n";
		}else if(totalKcal == 0) {
			message = postDate + "は何も食べてないよ\n";

		}else {
			message = postDate + "の間食では合計 " + totalKcal + " kcal摂取したよ\n"
					  + targetDateAteRecords.get(r).foodName()
					  + "(" + targetDateAteRecords.get(r).calorie() + "kcal)などを食べたよ\n";
		}
		message += "https://yuru-snack-log.onrender.com/\n"+ "#ゆるゆる間食ログ";
		
		return URLEncoder.encode(message, StandardCharsets.UTF_8);
	}
	
	public int insFavorite(String userId, long nutritionId) {
		return intakeRepo.insFavorite(userId, nutritionId);
	}
	
	public int delFavorite(String userId, long nutritionId) {
		return intakeRepo.delFavorite(userId, nutritionId);
	}
	
	public void swapFavorite(String userId, long favoriteId, String direction) {
		intakeRepo.swapFavorite(userId, favoriteId, direction);
	}
	
	public boolean chkDepliNutritionUpd(String userId, String className, long foodId, long nutritionId) {
		return intakeRepo.chkDepliNutritionUpd(userId, className, foodId, nutritionId);
	}
	
	@Transactional
	public boolean delMakerWithFavorites(String userId, long makerId) {
	    intakeRepo.delFavoriteFromMaker(userId, makerId);
	    return intakeRepo.delMaker(userId, makerId) != 0;
	}
	
	@Transactional
	public boolean delFoodWithFavorites(String userId, long foodId) {
	    intakeRepo.delFavoriteFromFood(userId, foodId);
	    return intakeRepo.delFood(userId, foodId) != 0;
	}
	
	@Transactional
	public boolean delNutritionWithFavorites(String userId, long nutritionId) {
	    intakeRepo.delFavoriteFromNutrition(userId, nutritionId);
	    return intakeRepo.delNutrition(userId, nutritionId) != 0;
	}
}
