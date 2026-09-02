ALTER TABLE focuses DROP CONSTRAINT fk_focuses_theme;
ALTER TABLE focuses DROP COLUMN theme_id;
DROP TABLE themes;
