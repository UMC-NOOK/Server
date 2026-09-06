DROP INDEX idx_search_history_user_keyword_type;

ALTER TABLE search_history
    ADD CONSTRAINT uk_search_history_user_keyword_type
        UNIQUE (user_id, keyword, search_type);
