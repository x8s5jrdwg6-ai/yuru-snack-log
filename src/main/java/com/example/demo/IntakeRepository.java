package com.example.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
				SELECT COALESCE(SUM(n.calorie * i.qty), 0) AS total_kcal
				FROM intake i
				INNER JOIN nutrition n
					ON n.nutrition_id = i.nutrition_id
				LEFT JOIN food f 
					ON f.food_id = n.food_id
				LEFT JOIN maker m
					ON m.maker_id = f.maker_id
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
	 * 	お気に入りの取得を行うメソッド
	 *	@param	userId ユーザーID  
	 *	@return	List<Map<String, Object>>
	 */
	public List<Map<String, Object>> getFavoriteList(String userId){
		String sql = """
				SELECT f.food_name, n.class_name, fav.favorite_id, n.nutrition_id
				FROM favorite fav
				INNER JOIN nutrition n 
					ON fav.nutrition_id = n.nutrition_id
				INNER JOIN food f
					ON n.food_id = f.food_id
				INNER JOIN maker m
					ON f.maker_id = m.maker_id
				WHERE fav.regist_user_id = ?
				ORDER BY fav.sort_order ASC, fav.favorite_id ASC
			""";
		
		return jdbc.queryForList(sql, userId);
	}
	
	/*
	 * 	初期表示
	 * 	お気に入りのソート順を更新するメソッド
	 *	@param	userId ユーザーID  
	 *	@param	favoriteId	お気に入りID
	 *	@param	direction	UP or DOWN
	 *	@return	List<Map<String, Object>>
	 */
	public void swapFavorite(String userId, long favoriteId, String direction) {
		String sql = """
					SELECT sort_order
				FROM favorite
				WHERE favorite_id = ?
					AND regist_user_id = ?;
			""";
		int currentOrder = jdbc.queryForObject(sql, Integer.class, favoriteId, userId);
		
		int targetOrder;

		if ("UP".equals(direction)) {
		    targetOrder = currentOrder - 1;
		} else { // DOWN
		    targetOrder = currentOrder + 1;
		}
		
		String sql_2 = """
					UPDATE favorite
					SET sort_order = CASE
						WHEN sort_order = ? THEN ?
						WHEN sort_order = ? THEN ?
					END
					WHERE regist_user_id = ?
						AND sort_order IN (?, ?)
				""";
			
		jdbc.update(sql_2, currentOrder, targetOrder, targetOrder, currentOrder, userId, currentOrder, targetOrder);
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
				  COALESCE(f.food_name, '削除された食品') AS food_name,
				  n.class_name,
				  n.calorie
				FROM intake i
				INNER JOIN nutrition n
					ON n.nutrition_id = i.nutrition_id
				LEFT JOIN food f 
					ON f.food_id = n.food_id
				LEFT JOIN maker m
					ON m.maker_id = f.maker_id
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
	
	
	/*
	 * 	詳細選択時
	 * 	履歴の詳細取得を行うメソッド
	 *	@param	userId ユーザーID  
	 *	@param	intakeId 食べたID
	 *	@return	Map<String, Object>
	 */
	public Map<String, Object> getIntakeDetail(String userId, long intakeId) {
		String sql = """
			    SELECT
			      i.intake_id,
			      i.eaten_date,
			      i.eaten_time,
			      i.qty,
			      COALESCE(m.maker_name, '削除されたメーカー') AS maker_name,
			      COALESCE(f.food_name, '削除された食品') AS food_name,
			      n.class_name,
			      n.calorie,
			      COALESCE(n.protein, 0) AS protein,
			      COALESCE(n.lipid, 0)   AS lipid,
			      COALESCE(n.carbo, 0)   AS carbo,
			      COALESCE(n.salt, 0)    AS salt
			    FROM intake i
			    INNER JOIN nutrition n 
				    ON n.nutrition_id = i.nutrition_id
			    LEFT JOIN food f 
				    ON f.food_id = n.food_id
			    LEFT JOIN maker m 
				    ON m.maker_id = f.maker_id
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
			        map.put("qty", rs.getDouble("qty"));
			        map.put("makerName", rs.getString("maker_name"));
			        map.put("foodName", rs.getString("food_name"));
			        map.put("className", rs.getString("class_name"));
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
	 * 	@param	nutritionId 栄養ID 
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
	 * 	食べた登録画面：②：画面表示時
	 * 	ユーザーIDと食品IDに紐づく食品情報とメーカ情報を取得するメソッド
	 *	@param	userId ユーザーID  
	 * 	@param	foodId 食品ID 
	 *	@return	Map<String, Object>
	 */
	public Map<String, Object> getHeaderInfo (String userId, long foodId) {
		String sql = """
				SELECT f.food_id, f.food_name, m.maker_id, m.maker_name
				FROM food f
				INNER JOIN maker m
					ON m.maker_id = f.maker_id
				WHERE f.food_id = ? AND f.regist_user_id = ?
			""";
		
		Map<String, Object> headerInfo = jdbc.queryForObject(
			    sql,
			    new Object[]{ foodId, userId },
			    (rs, rowNum) -> {
			        Map<String, Object> map = new HashMap<>();
			        map.put("foodId", rs.getLong("food_id"));
			        map.put("foodName", rs.getString("food_name"));
			        map.put("makerId", rs.getString("maker_id"));
			        map.put("makerName", rs.getString("maker_name"));
			        return map;
			    }
			);
		return headerInfo;
	}
	
	/*
	 * 	食べた登録画面：③：画面表示時
	 * 	ユーザーIDと食品IDに紐づく分類一覧を取得するメソッド
	 *	@param	userId ユーザーID 
	 *	@param	foodId	食品ID
	 *	@return	List<Map<String, Object>>	栄養情報一覧
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
	 * 	ユーザーIDと選択したメーカーIDに紐づく食品情報の一覧を取得するメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	makerId	メーカーID(@RequestParamから)
	 *	@return	List<Map<String, Object>> 分類一覧
	 */
	public List<Map<String, Object>> getFoodList(String userId, long makerId){
		String sql = """
				SELECT food_id, food_name
				FROM food f
				INNER JOIN maker m
					ON m.maker_id = f.maker_id
				WHERE f.regist_user_id = ?
					AND f.maker_id = ?
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
	 *	@return	重複がなければfalse。重複がある場合はtrue
	 */
	public boolean chkDepliFood(String userId, String foodName, long makerId) {
		String sql = """
				SELECT EXISTS(
					SELECT 1
					FROM food f
					INNER JOIN maker m
						ON m.maker_id = f.maker_id
					WHERE f.regist_user_id = ?
						AND food_name = ?
						AND f.maker_id = ?
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
	 * 	ユーザーごとに分類名の重複チェックを行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	className	登録する分類名
	 * 	@param	foodId	食品ID
	 *	@return	重複がなければ0。重複がある場合は重複件数
	 */
	public boolean chkDepliNutrition(String userId, String className, long foodId) {
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
		return jdbc.queryForObject(sql, Boolean.class, userId, className, foodId);
	}
	
	/*
	 * 	「登録」押下
	 * 	makerテーブルに登録を行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	className	登録する分類名
	 *	@param	foodId	食品ID
	 *	@param	calorie	カロリー
	 *	@param	protein	たんぱく質
	 *	@param	carbo	炭水化物
	 *	@param	salt	塩分
	 *	@return	登録件数（通常は1）
	 */
	public int insNutrition(String userId, String className, long foodId, int calorie, Double protein, Double lipid, Double carbo, Double salt) {
		String sql = """
				INSERT INTO nutrition (class_name, food_id, calorie, protein, lipid, carbo, salt, regist_user_id)
				VALUES (?, ?, ?, COALESCE(?, 0), COALESCE(?, 0), COALESCE(?, 0), COALESCE(?, 0), ?);
			""";
		return jdbc.update(sql, className, foodId, calorie, protein, lipid, carbo, salt, userId);
	}
	
	/*--------------------------------------
 		user_idの取得・登録・更新処理
	--------------------------------------*/
	/*
	 * 	user_idの存在チェックを行うメソッド
	 *	@param	c Cookie 
	 *	@return	userId DBと照合したユーザーID、存在しなければ空文字
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
	
	/*
	 * 	user_idの登録を行うメソッド
	 *	@param	userId user_id用に作成したUUID 
	 */
	public void registUserId(String userId) {
		String sql = """
	    		INSERT INTO users(user_id)
	    		VALUES (?)
	    		ON CONFLICT (user_id) DO NOTHING;
			""";
		// usersに登録
	    jdbc.update(sql, userId);
	}
	
	/*
	 * 	最終アクセス日時の更新を行うメソッド
	 *	@param	userId user_id
	 */
	public void updLastAccessDate(String userId) {
		String sql = """
				UPDATE users
				SET last_access_date = CURRENT_TIMESTAMP
				WHERE user_id = ?
			""";
		jdbc.update(sql, userId);
	}
	
	/*--------------------------------------
		メーカ編集
	--------------------------------------*/
	/*
	 * 	メーカー名の更新を行うメソッド
	 *	@param	userId user_id
	 *	@param	makerId	メーカーID
	 *	@param	makerName	メーカー名
	 *	@return	更新件数（通常は1）
	 */
	public int updMaker(long makerId, String makerName) {
		String sql = """
				UPDATE maker
				SET maker_name = ?
				WHERE maker_id = ?
			""";
		return jdbc.update(sql, makerName, makerId);
	}
	
	/*
	 * 	「削除」押下
	 * 	メーカーの削除を行うメソッド
	 *	@param	userId user_id
	 *	@param	makerId	メーカーID
	 *	@return	削除件数（通常は1）
	 */
	public int delMaker(String userId, long makerId) {
		String sql = "DELETE FROM maker WHERE maker_id = ? AND regist_user_id = ?";
		return jdbc.update(sql, makerId, userId);
	}
	
	/*
	 * 	「削除」押下
	 * 	お気に入りを削除後、もとのsort_orderの順番で1からsort_orderを振り直すメソッド
	 *	@param	userId user_id
	 *	@param	makerId	メーカーID
	 */
	@Transactional
	public void delFavoriteFromMaker(String userId, long makerId) {

		// 1) メーカー配下のfavoriteを全削除
		String del = """
				DELETE FROM favorite
				WHERE regist_user_id = ?
				AND nutrition_id IN (
					SELECT n.nutrition_id
					FROM nutrition n
					JOIN food f ON f.food_id = n.food_id
					WHERE f.maker_id = ?
				)
			""";
		jdbc.update(del, userId, makerId);

		// 2) 残ったfavoriteの並びを詰め直し（0始まり）
		String resequence = """
				WITH ranked AS (
					SELECT favorite_id,
						ROW_NUMBER() OVER (ORDER BY sort_order, favorite_id) - 1 AS new_order
					FROM favorite
					WHERE regist_user_id = ?
				)
				UPDATE favorite f
				SET sort_order = ranked.new_order
				FROM ranked
				WHERE f.favorite_id = ranked.favorite_id
			""";
		jdbc.update(resequence, userId);
	}
	
	/*--------------------------------------
		食品情報一覧
	--------------------------------------*/
	/* 	
	 * 	画面表示時
	 * 	ユーザーIDと選択したメーカーIDに紐づく分類の一覧を取得するメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	makerId	メーカーID(@RequestParamから)
	 *	@return	List<Map<String, Object>> 分類一覧
	 */
	public List<Map<String, Object>> getFoodListAll(String userId){
		String sql = """
				SELECT food_id, food_name, f.maker_id, maker_name
				FROM food f
				INNER JOIN maker m
					ON f.maker_id = m.maker_id
				WHERE f.regist_user_id = ?
				ORDER BY food_name, maker_name
			""";
		
		return jdbc.queryForList(sql, userId);
	}
	
	/*
	 * 	「食品を登録して栄養情報登録へ進む」押下
	 * 	ユーザーごとに食品名の重複チェックを行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	foodName	登録する食品名
	 * 	@param	makerId	メーカーID
	 * 	@param	foodId	食品ID
	 *	@return	重複がなければfalse。重複がある場合はtrue
	 */
	public boolean chkDepliFood(String userId, String foodName, long makerId, long foodId) {
		String sql = """
				SELECT EXISTS(
					SELECT 1
					FROM food f
					INNER JOIN maker m
						ON m.maker_id = f.maker_id
					WHERE f.regist_user_id = ?
						AND food_name = ?
						AND f.maker_id = ?
						AND f.food_id <> ?
				)
			""";
		// 重複があればtrue、なければfalseを返す
		return jdbc.queryForObject(sql, Boolean.class, userId, foodName, makerId, foodId);
	}
	
	/*
	 * 	食品情報の更新を行うメソッド
	 *	@param	userId user_id
	 *	@param	makerId	メーカーID
	 *	@param	makerName	メーカー名
	 *	@return	更新件数（通常は1）
	 */
	public int updFood(String userId, long foodId, String foodName) {
		String sql = """
				UPDATE food
				SET food_name = ?
				WHERE food_id = ?
					AND regist_user_id = ?
			""";
		return jdbc.update(sql, foodName, foodId, userId);
	}
	
	/*
	 * 	「削除」押下
	 * 	食品情報の削除を行うメソッド
	 *	@param	userId user_id
	 *	@param	makerId	メーカーID
	 *	@return	削除件数（通常は1）
	 */
	public int delFood (String userId, long foodId) {
		String sql = "DELETE FROM food WHERE food_id = ? AND regist_user_id = ?";
		return jdbc.update(sql, foodId, userId);
	}
	
	/*
	 * 	「削除」押下
	 * 	お気に入りを削除後、もとのsort_orderの順番で1からsort_orderを振り直すメソッド
	 *	@param	userId user_id
	 *	@param	foodId	食品ID
	 */
	@Transactional
	public void delFavoriteFromFood(String userId, long foodId) {

		// 1) food配下のfavoriteを全削除
		String del = """
				DELETE FROM favorite
				WHERE regist_user_id = ?
					AND nutrition_id IN (
						SELECT nutrition_id
						FROM nutrition
						WHERE regist_user_id = ?
							AND food_id = ?
					)
			""";
		jdbc.update(del, userId, userId, foodId);

		// 2) 残ったfavoriteの並びを詰め直し（0始まり）
		String resequence = """
					WITH ranked AS (
						SELECT favorite_id,
							ROW_NUMBER() OVER (ORDER BY sort_order, favorite_id) - 1 AS new_order
						FROM favorite
						WHERE regist_user_id = ?
					)
					UPDATE favorite f
					SET sort_order = ranked.new_order
					FROM ranked
					WHERE f.favorite_id = ranked.favorite_id
				""";
		jdbc.update(resequence, userId);
	}
	
	/*--------------------------------------
		栄養情報一覧
	--------------------------------------*/
	/*
	 * 	栄養情報一覧画面表示時
	 * 	ユーザーIDと食品IDに紐づく分類一覧を取得するメソッド
	 *	@param	userId ユーザーID 
	 *	@return	List<Map<String, Object>>	栄養情報一覧
	 */
	public List<Map<String, Object>> getNutritionListAll(String userId){
		String sql = """
				SELECT nutrition_id, class_name, calorie, protein, lipid, carbo, salt, f.food_id, food_name, m.maker_id, maker_name
				FROM nutrition n
				INNER JOIN food f
					ON f.food_id = n.food_id
				INNER JOIN maker m
					ON m.maker_id = f.maker_id
				WHERE n.regist_user_id = ?
				ORDER BY maker_name, food_name, class_name
			""";
		
		return jdbc.queryForList(sql, userId);
	}
	
	/*
	 * 	栄養情報一覧：選択時
	 * 	ユーザーIDと食品IDに紐づく食品情報とメーカ情報を取得するメソッド
	 *	@param	userId ユーザーID  
	 * 	@param	nutritionId 栄養ID 
	 *	@return	Map<String, Object>
	 */
	public Map<String, Object> getNutritionInfo (String userId, long nutritionId) {
		String sql = """
				SELECT n.nutrition_id, class_name, calorie, protein, lipid, carbo, salt, m.maker_id, maker_name, f.food_id, food_name, COALESCE(fav.favorite_id, 0) AS favorite_id
				FROM nutrition n
				INNER JOIN food f
					ON f.food_id = n.food_id
				INNER JOIN maker m
					ON m.maker_id = f.maker_id
				LEFT JOIN favorite fav
					ON	fav.nutrition_id = n.nutrition_id
				WHERE n.nutrition_id = ?
					AND f.regist_user_id = ?
			""";
		
		Map<String, Object> headerInfo = jdbc.queryForObject(
			    sql,
			    new Object[]{ nutritionId, userId },
			    (rs, rowNum) -> {
			        Map<String, Object> map = new HashMap<>();
			        map.put("nutritionId", rs.getString("nutrition_id"));
			        map.put("className", rs.getString("class_name"));
			        map.put("calorie", rs.getString("calorie"));
			        map.put("protein", rs.getString("protein"));
			        map.put("lipid", rs.getString("lipid"));
			        map.put("carbo", rs.getString("carbo"));
			        map.put("salt", rs.getString("salt"));
			        map.put("foodId", rs.getString("food_id"));
			        map.put("foodName", rs.getString("food_name"));
			        map.put("makerId", rs.getString("maker_id"));
			        map.put("makerName", rs.getString("maker_name"));
			        map.put("favoriteId", rs.getString("favorite_id"));
			        return map;
			    }
			);
		return headerInfo;
	}
	
	
	/*--------------------------------------
		栄養情報詳細画面
	--------------------------------------*/
	/*
	 * 	栄養情報のお気に入り登録を行うメソッド
	 *	@param	userId user_id
	 *	@param	nutritionId	栄養ID
	 *	@return	登録件数（通常は1）
	 */
	public int insFavorite(String userId, long nutritionId) {
		String sql = """
				INSERT INTO favorite (regist_user_id, nutrition_id, sort_order)
				VALUES (?, ?, COALESCE((SELECT MAX(sort_order) + 1 FROM favorite WHERE regist_user_id = ?),0))
				ON CONFLICT (regist_user_id, nutrition_id) DO NOTHING
		     """;
		// 登録件数を返す（通常 1）。失敗時は例外が投げられることが多い
		return jdbc.update(sql, userId, nutritionId, userId);
	}
	
	
	/*
	 * 	栄養情報のお気に入りを削除し、削除後もとのsort_orderの順番で1からsort_orderを振り直すメソッド
	 *	@param	userId user_id
	 *	@param	nutritionId	栄養ID
	 *	@return	削除件数（通常は1）
	 */
	@Transactional
	public int delFavorite(String userId, long nutritionId) {
		// 1) 単体削除
		String del = """
				DELETE FROM favorite
				WHERE regist_user_id = ?
				AND nutrition_id = ?
			""";
		int delCnt = jdbc.update(del, userId, nutritionId);
		if (delCnt == 0) return 0;

		// 2) 並びを詰め直し（0始まり）
		String resequence = """
				WITH ranked AS (
					SELECT favorite_id,
						ROW_NUMBER() OVER (ORDER BY sort_order, favorite_id) - 1 AS new_order
					FROM favorite
					WHERE regist_user_id = ?
				)
				UPDATE favorite f
				SET sort_order = ranked.new_order
				FROM ranked
				WHERE f.favorite_id = ranked.favorite_id
			""";
		jdbc.update(resequence, userId);

		return delCnt;
	}

	
	/*
	 * 	「削除」押下
	 * 	食品情報の削除を行うメソッド
	 *	@param	userId user_id
	 *	@param	makerId	メーカーID
	 *	@return	削除件数（通常は1）
	 */
	public int delNutrition (String userId, long nutritionId) {
		String sql = "DELETE FROM nutrition WHERE nutrition_id = ? AND regist_user_id = ?";
		return jdbc.update(sql, nutritionId, userId);
	}
	
	/*
	 * 	「削除」押下
	 * 	お気に入りを削除後、もとのsort_orderの順番で1からsort_orderを振り直すメソッド
	 *	@param	userId user_id
	 *	@param	nutritionId	栄養ID
	 */
	@Transactional
	public void delFavoriteFromNutrition(String userId, long nutritionId) {

		// 1) food配下のfavoriteを全削除
		String del = """
				DELETE FROM favorite
				WHERE regist_user_id = ?
					AND nutrition_id = ?
			""";
		jdbc.update(del, userId, nutritionId);

		// 2) 残ったfavoriteの並びを詰め直し（0始まり）
		String resequence = """
					WITH ranked AS (
						SELECT favorite_id,
							ROW_NUMBER() OVER (ORDER BY sort_order, favorite_id) - 1 AS new_order
						FROM favorite
						WHERE regist_user_id = ?
					)
					UPDATE favorite f
					SET sort_order = ranked.new_order
					FROM ranked
					WHERE f.favorite_id = ranked.favorite_id
				""";
		jdbc.update(resequence, userId);
	}

	
	
	
	/*--------------------------------------
		栄養情報編集画面
	--------------------------------------*/
	/*
	 * 	「更新」押下
	 * 	ユーザーごとに分類名の重複チェックを行うメソッド
	 *	@param	userId ユーザーID 
	 * 	@param	className	登録する分類名
	 * 	@param	foodId	食品ID
	 *  @param	nutritionId	栄養ID
	 *	@return	重複がなければ0。重複がある場合は重複件数
	 */
	public boolean chkDepliNutritionUpd(String userId, String className, long foodId, long nutritionId) {
		String sql = """
				SELECT EXISTS(
					SELECT 1
				FROM nutrition
				WHERE regist_user_id = ?
					AND class_name = ?
					AND food_id = ?
					AND nutrition_id <> ?
				)
			""";
		// 重複があればtrue、なければfalseを返す
		return jdbc.queryForObject(sql, Boolean.class, userId, className, foodId, nutritionId);
	}
	/*
	 * 	栄養情報の更新を行うメソッド
	 *	@param	userId user_id
	 *	@param	nutritionId	栄養ID
	 *	@param	className	分類名
	 *	@param	calorie
	 *	@param	protein
	 *	@param	lipid
	 *	@param	carbo
	 *	@param	salt
	 *	@return	更新件数（通常は1）
	 */
	public int updNutrition(String userId, long nutritionId, String className, int calorie, BigDecimal protein, BigDecimal lipid , BigDecimal carbo , BigDecimal salt) {
		String sql = """
				UPDATE nutrition
				SET class_name = ?, calorie = ?, protein = COALESCE(?, 0.0), lipid = COALESCE(?, 0.0), carbo = COALESCE(?, 0.0), salt = COALESCE(?, 0.00)
				WHERE regist_user_id = ?
					AND nutrition_id = ?
			""";
		return jdbc.update(sql, className, calorie, protein, lipid, carbo, salt, userId, nutritionId);
	}
}
