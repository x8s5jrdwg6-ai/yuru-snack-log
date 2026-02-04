CREATE TABLE IF NOT EXISTS users (
  user_id     varchar(36)  PRIMARY KEY,
  user_name   varchar(36),
  password    varchar(255) ,
  regist_date timestamp DEFAULT now(),
  last_access_date timestamp DEFAULT now()
);

CREATE TABLE IF NOT EXISTS maker (
  maker_id  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  maker_name     varchar(50) NOT NULL,
  regist_user_id varchar(36) NOT NULL,
  UNIQUE (regist_user_id, maker_name)
);

CREATE INDEX IF NOT EXISTS idx_maker_user ON maker(regist_user_id);

CREATE TABLE IF NOT EXISTS food (
  food_id        integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  maker_id  integer NOT NULL,
  food_name      varchar(50) NOT NULL,
  regist_user_id varchar(36) NOT NULL,
  UNIQUE (regist_user_id, maker_id, food_name)
);

CREATE INDEX IF NOT EXISTS idx_food_user_maker ON food(regist_user_id, maker_id);

CREATE TABLE IF NOT EXISTS nutrition (
  nutrition_id      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  class_name    varchar(50) NOT NULL,
  food_id        integer NOT NULL,
  calorie        integer NOT NULL,
  protein        numeric(10,1) NOT NULL DEFAULT 0,
  lipid          numeric(10,1) NOT NULL DEFAULT 0,
  carbo          numeric(10,1) NOT NULL DEFAULT 0,
  salt           numeric(10,2) NOT NULL DEFAULT 0,
  regist_user_id varchar(36) NOT NULL,
  UNIQUE (regist_user_id, food_id, class_name)
);

CREATE INDEX IF NOT EXISTS idx_nutrition_food ON nutrition(food_id);
CREATE INDEX IF NOT EXISTS idx_nutrition_user ON nutrition(regist_user_id);

CREATE TABLE IF NOT EXISTS intake (
  intake_id      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  regist_user_id  varchar(36) NOT NULL,
  nutrition_id       integer NOT NULL,
  eaten_date      date NOT NULL,
  eaten_time      time NOT NULL,
  qty             integer NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_intake_user_time ON intake(regist_user_id, eaten_date, eaten_time);
CREATE INDEX IF NOT EXISTS idx_intake_nutrition ON intake(nutrition_id);
