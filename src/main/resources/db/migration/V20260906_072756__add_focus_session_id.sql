ALTER TABLE focuses ADD COLUMN session_id UUID NOT NULL;
CREATE INDEX idx_focus_library_session ON focuses (library_id, session_id);
