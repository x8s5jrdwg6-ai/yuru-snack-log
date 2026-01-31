package com.example.demo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

//import com.example.demo.HomeController.IntakeDetailRow;
import com.example.demo.HomeController.IntakeRow;

@Repository
public class IntakeRepository {
    private final JdbcTemplate jdbc;

	public IntakeRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
	
/*--------------------------------------
 	home画面
--------------------------------------*/
    /*
	 * 	初期表示
	 * 	合計カロリーの計算を行うメソッド
	 *	@param	userId ユーザーID  
	 *	@param	eatenDate 食べた日付
	 *	@return	合計カロリー
	 */
	public int calcTotalCal(String userId, LocalDate eatenDate) {
		String sql = """
				SELECT COALESCE(SUM(fl.calorie * i.qty), 0) AS total_kcal
				FROM intake i
				JOIN nutrition fl ON fl.nutrition_id = i.nutrition_id
				WHERE i.regist_user_id = ?
				AND i.eaten_date = ?
			""";
		// intはnullを持てないので一度Integerで受ける
		Integer v = jdbc.queryForObject(sql, Integer.class, userId, eatenDate);
		// 三項演算子（条件 ? 真のときの値 : 偽のときの値）
		return (v == null) ? 0 : v;
	}
	
	/*
	 * 	初期表示
	 * 	対象日の履歴の取得を行うメソッド
	 *	@param	userId ユーザーID  
	 *	@param	eatenDate 食べた日付
	 *	@return	登録件数（通常は1）
	 */
	// 対象日の履歴の取得
	public List<IntakeRow> getDailyRecords(String userId, LocalDate eatenDate){
		String sql = """
				SELECT
				  i.intake_id,
				  i.eaten_date,
				  i.eaten_time,
				  i.qty,
				  f.food_name,
				  fl.class_name,
				  fl.calorie
				FROM intake i
				JOIN nutrition fl ON fl.nutrition_id = i.nutrition_id
				JOIN food f ON f.food_id = fl.food_id
				WHERE i.regist_user_id = ?
				  AND i.eaten_date = ?
				ORDER BY i.eaten_time ASC, i.intake_id ASC
				""";
		
		// RowMapperで結果をList<IntakeRow>型で取得
		List<IntakeRow> records = jdbc.query(
			sql,
			(rs, rowNum) -> new IntakeRow(
			rs.getLong("intake_id"),
			rs.getString("eaten_date"),
			rs.getString("eaten_time"),
			rs.getDouble("qty"),
			rs.getString("food_name"),
			rs.getString("class_name"),
			rs.getInt("calorie")
			),
			userId, eatenDate
		);
		return records;
	}
	
	
	
	public Map<String, Object> getIntakeDetail(String userId, long intakeId) {
		String sql = """
			    SELECT
			      i.intake_id,
			      i.eaten_date,
			      i.eaten_time,
			      i.qty,
			      m.maker_name,
			      f.food_name,
			      fl.class_name,
			      fl.calorie,
			      COALESCE(fl.protein, 0) AS protein,
			      COALESCE(fl.lipid, 0)   AS lipid,
			      COALESCE(fl.carbo, 0)   AS carbo,
			      COALESCE(fl.salt, 0)    AS salt
			    FROM intake i
			    JOIN nutrition fl ON fl.nutrition_id = i.nutrition_id
			    JOIN food f ON f.food_id = fl.food_id
			    JOIN maker m ON m.maker_id = f.maker_id
			    WHERE i.intake_id = ?
			      AND i.regist_user_id = ?
			""";
		
		Map<String, Object> intake = jdbc.queryForObject(
			    sql,
			    new Object[]{ intakeId, userId },
			    (rs, rowNum) -> {
			        Map<String, Object> map = new HashMap<>();
			        map.put("intakeId", rs.getLong("intake_id"));
			        map.put("eatenDate", rs.getString("eaten_date"));
			        map.put("eatenTime", rs.getString("eaten_time"));
			        map.put("qty", rs.getInt("qty"));
			        map.put("makerName", rs.getString("maker_name"));
			        map.put("foodName", rs.getString("food_name"));
			        map.put("nutritionName", rs.getString("class_name"));
			        map.put("calorie", rs.getInt("calorie"));
			        map.put("protein", rs.getDouble("protein"));
			        map.put("lipid", rs.getDouble("lipid"));
			        map.put("carbo", rs.getDouble("carbo"));
			        map.put("salt", rs.getDouble("salt"));
			        return map;
			    }
			);
		return intake;
	}
	
/*--------------------------------------
 	詳細画面
--------------------------------------*/
	/*
	 * 	「削除」押下
	 * 	intakeテーブルの削除を行うメソッド
	 *	@param	intakeId intakeId 
	 * 	@param	userId ユーザーID 
	 *	@return	削除件数（通常は1）
	 */
	public int delIntake(long intakeId, String userId) {
		String sql = "DELETE FROM intake WHERE intake_id = ? AND regist_user_id = ?";
		return jdbc.update(sql, intakeId, userId);
	}
	
/*--------------------------------------
 	履歴編集画面
--------------------------------------*/
	/*
	 * 	「更新」押下
	 * 	intakeテーブルの更新を行うメソッド
	 * 	@param	userId ユーザーID 
	 *	@param	intakeId intakeId 
	 * 	@param	eatenDate 食べた日付
	 *	@param	eatenTime 食べた時刻
	 *	@return	更新件数（通常は1）
	 */
	public int updIntake(String userId, long intakeId, LocalDate eatenDate, LocalTime eatenTime) {
		String sql = """
			       UPDATE intake
			       SET eaten_date = ?, eaten_time = ?
			       WHERE intake_id = ? AND regist_user_id = ?
			     """;
		return jdbc.update(sql, eatenDate, eatenTime, intakeId, userId);
	}
	
/*--------------------------------------
 	食べた登録画面
--------------------------------------*/
	/*
	 * 	「食べた！」押下
	 * 	intakeテーブルに登録を行うメソッド
	 *	@param	userId ユーザーID  
	 * 	@param	nutritionId 味ID 
	 *	@param	eatenDate 食べた日付
	 *	@param	eatenTime 食べた時刻
	 *	@return	登録件数（通常は1）
	 */
	public int insIntake(String userId, long nutritionId, LocalDate eatenDate, LocalTime eatenTime) {
		String sql = """
		         INSERT INTO intake (regist_user_id, nutrition_id, eaten_date, eaten_time, qty)
		         VALUES (?, ?, ?, ? ,1)
		     """;
		// 登録件数を返す（通常 1）。失敗時は例外が投げられることが多い
		return jdbc.update(sql, userId, nutritionId, eatenDate, eatenTime);
	}
	
	public Map<String, Object> getHeaderInfo (String userId, long foodId) {
		String sql = """
				SELECT f.food_id, f.food_name, m.maker_id, m.maker_name
				FROM food f
				JOIN maker m ON m.maker_id = f.maker_id
				WHERE f.food_id = ? AND f.regist_user_id = ?
			""";
		
		Map<String, Object> headerInfo = jdbc.queryForObject(
			    sql,
			    new Object[]{ foodId, userId },
			    (rs, rowNum) -> {
			        Map<String, Object> map = new HashMap<>();
			        map.put("foodId", rs.getLong("food_id"));
			        map.put("foodName", rs.getString("food_name"));
			        map.put("foodMakerId", rs.getString("maker_id"));
			        map.put("makerName", rs.getString("maker_name"));
			        return map;
			    }
			);
		return headerInfo;
	}
	
	/*
	 * 	食べた登録画面：①：画面表示時
	 * 	メーカーの一覧を取得するメソッド
	 *	@param	userId ユーザーID 
	 *	@return	List<Map<String, Object>>	メーカー一覧
	 */
	public List<Map<String, Object>> getMakerList(String userId){
		String sql = """
				SELECT maker_id, maker_name
				FROM maker
				WHERE regist_user_id = ?
				ORDER BY maker_name
			""";
		
		return jdbc.queryForList(sql, userId);
	}
	
	/*
	 * 	食べた登録画面：②：画面表示時
	 * 	選択したメーカーのメーカー名を取得するメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	makerId	メーカーID
	 *	@return	メーカー名
	 */
	public String getMakerName(String userId, long makerId) {
		String sql = """
		        SELECT maker_name
		        FROM maker
		        WHERE maker_id = ?
		          AND regist_user_id = ?
		    """;
		
		return jdbc.queryForObject(sql, String.class, makerId, userId);
	}
	
	/*
	 * 	食べた登録画面：③：画面表示時
	 * 	ユーザーIDと食品IDに紐づく味一覧を取得するメソッド
	 *	@param	userId ユーザーID 
	 *	@param	foodId	食品ID
	 *	@return	List<Map<String, Object>>	味一覧
	 */
	public List<Map<String, Object>> getNutritionList(String userId, long foodId){
		String sql = """
				SELECT nutrition_id, class_name, calorie, protein, lipid, carbo, salt
				FROM nutrition
				WHERE regist_user_id = ? AND food_id = ?
				ORDER BY class_name
			""";
		
		return jdbc.queryForList(sql, userId, foodId);
	}
	
	
	/* 	
	 * 	食べた登録画面：③：画面表示時
	 * 	ユーザーIDと選択したメーカーIDに紐づく味の一覧を取得するメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	makerId	メーカーID(@RequestParamから)
	 *	@return	List<Map<String, Object>> 味一覧
	 */
	public List<Map<String, Object>> getFoodList(String userId, long makerId){
		String sql = """
				SELECT food_id, food_name
				FROM food
				WHERE regist_user_id = ? AND maker_id = ?
				ORDER BY food_name
			""";
		
		return jdbc.queryForList(sql, userId, makerId);
	}
	
/*--------------------------------------
 	メーカー登録画面
--------------------------------------*/
	/*
	 * 	「登録」押下
	 * 	ユーザーごとにメーカー名の重複チェックを行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	makerName	登録するメーカー名
	 *	@return	重複がなければ0。重複がある場合は重複件数
	 */
	public boolean chkDepliMaker(String userId, String makerName) {
		String sql = """
				SELECT EXISTS(
					SELECT 1
					FROM maker
				WHERE regist_user_id = ?
					AND maker_name = ?
				)
			""";
		// 重複があればtrue、なければfalseを返す
		return jdbc.queryForObject(sql, Boolean.class, userId, makerName);
	}
	
	
	/*
	 * 	「登録」押下
	 * 	makerテーブルに登録を行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	makerName	登録するメーカー名
	 *	@return	登録件数（通常は1）
	 */
	public int insFoodMaker(String userId, String makerName) {
		String sql = "INSERT INTO maker (maker_name, regist_user_id) VALUES (?, ?) RETURNING maker_id";
		return jdbc.queryForObject(sql, int.class,  makerName, userId);
	}
	
/*--------------------------------------
 	食品登録画面
--------------------------------------*/
	/*
	 * 	「食品を登録して栄養情報登録へ進む」押下
	 * 	ユーザーごとに食品名の重複チェックを行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	foodName	登録する食品名
	 * 	@param	makerId	メーカーID
	 *	@return	重複がなければ0。重複がある場合は重複件数
	 */
	public boolean chkDepliFood(String userId, String foodName, long makerId) {
		String sql = """
				SELECT EXISTS(
					SELECT 1
					FROM food
					WHERE regist_user_id = ?
						AND food_name = ?
						AND maker_id = ?
				)
			""";
		// 重複があればtrue、なければfalseを返す
		return jdbc.queryForObject(sql, Boolean.class, userId, foodName, makerId);
	}
	
	/*
	 * 	「食品を登録して栄養情報登録へ進む」押下
	 * 	foodテーブルに登録を行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	foodName	登録する食品名
	 * 	@param	makerId	メーカーID
	 *	@return	登録件数（通常は1）
	 */
	public int insFood(String userId, String foodName,long makerId) {
		String sql = "INSERT INTO food (maker_id, food_name, regist_user_id) VALUES (?, ?, ?) RETURNING food_id";
		// 登録件数を返す（通常 1）。失敗時は例外が投げられることが多い
		return jdbc.queryForObject(sql, int.class, makerId, foodName, userId);
	}
	
	/*
	 * 	「食品を登録して栄養情報登録へ進む」押下
	 * 	登録した食品の食品IDを返すメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	foodName	登録する食品名
	 * 	@param	makerId	メーカーID
	 *	@return	登録した食品の食品ID
	 */
	public long getRegistFoodId(String userId, String foodName,long makerId) {
		String sql = "SELECT food_id FROM food WHERE regist_user_id=? AND maker_id=? AND food_name=?";
		return jdbc.queryForObject(sql, Long.class, userId, makerId, foodName);
	}
	
/*--------------------------------------
 	栄養情報登録画面
--------------------------------------*/
	/*
	 * 	「登録」押下
	 * 	ユーザーごとに味名の重複チェックを行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	nutritionName	登録する味名
	 * 	@param	foodId	食品ID
	 *	@return	重複がなければ0。重複がある場合は重複件数
	 */
	public boolean chkDepliNutrition(String userId, String nutritionName, long foodId) {
		String sql = """
				SELECT EXISTS(
					SELECT 1
				FROM nutrition
				WHERE regist_user_id = ?
					AND class_name = ?
					AND food_id = ?
				)
			""";
		// 重複があればtrue、なければfalseを返す
		return jdbc.queryForObject(sql, Boolean.class, userId, nutritionName, foodId);
	}
	
	/*
	 * 	「登録」押下
	 * 	makerテーブルに登録を行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	nutritionName	登録する味名
	 *	@param	foodId	食品ID
	 *	@param	calorie	カロリー
	 *	@param	protein	たんぱく質
	 *	@param	carbo	炭水化物
	 *	@param	salt	塩分
	 *	@return	登録件数（通常は1）
	 */
	public int insNutrition(String userId, String nutritionName, long foodId, int calorie, Double protein, Double lipid, Double carbo, Double salt) {
		String sql = "INSERT INTO nutrition (class_name, food_id, calorie, protein, lipid, carbo, salt, regist_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		return jdbc.update(sql, nutritionName, foodId, calorie, protein, lipid, carbo, salt, userId);
	}
	
	/*--------------------------------------
 		user_idの取得・登録・更新処理
	--------------------------------------*/
	/*
	 *	@param	c Cookie 
	 *	@return	userId DBと照合したユーザーID
	 */
	public String chkUserId(Cookie c) {
		String userId = "";
		String sql = """
				SELECT EXISTS(
					SELECT 1
					FROM users
					WHERE user_id = ?
				)
			""";
		if(jdbc.queryForObject(sql, Boolean.class, c.getValue())) {
			userId = c.getValue();
		}
		return userId;
	}
	
	public void registUserId(String userId) {
		String sql = """
	    		INSERT INTO users(user_id)
	    		VALUES (?)
	    		ON CONFLICT (user_id) DO NOTHING;
			""";
		// usersに登録
	    jdbc.update(sql, userId);
	}
	
	public void updLastAccessDate(String userId) {
		String sql = """
				UPDATE users
				SET last_access_date = CURRENT_TIMESTAMP
				WHERE user_id = ?
			""";
		jdbc.update(sql, userId);
	}
}
