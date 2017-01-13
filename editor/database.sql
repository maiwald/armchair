CREATE TABLE `characters` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `name` TEXT NOT NULL UNIQUE
);

CREATE TABLE "scenes" (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `label` TEXT NOT NULL,
  `initial_line_id` INTEGER,
  FOREIGN KEY(`initial_line_id`) REFERENCES lines(id)
);

CREATE TABLE "lines" (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `character_id` INTEGER NOT NULL,
  `text` TEXT NOT NULL,
  `next_item_id` INTEGER,
  `next_item_type` TEXT CHECK(next_item_type IN ("line", "option")),
  `is_initial_line` NUMERIC NOT NULL DEFAULT 0 CHECK(is_initial_line IN (0, 1)),
  FOREIGN KEY(`character_id`) REFERENCES `characters`(`id`),
  FOREIGN KEY(`next_item_id`) REFERENCES `lines`(`id`)
);

CREATE TABLE `options` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `option_0_text` TEXT NOT NULL,
  `option_0_next_line_id` INTEGER,
  `option_1_text` TEXT NOT NULL,
  `option_1_next_line_id` INTEGER,
  `option_2_text` TEXT,
  `option_2_next_line_id` INTEGER,
  `option_3_text` TEXT,
  `option_3_next_line_id` INTEGER,
  FOREIGN KEY(`option_0_next_line_id`) REFERENCES lines(id),
  FOREIGN KEY(`option_1_next_line_id`) REFERENCES lines(id),
  FOREIGN KEY(`option_2_next_line_id`) REFERENCES lines(id),
  FOREIGN KEY(`option_3_next_line_id`) REFERENCES lines(id)
);
