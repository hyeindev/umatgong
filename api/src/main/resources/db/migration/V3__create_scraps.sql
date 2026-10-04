-- 스크랩 (기획서 E-08 「가고 싶은 곳 저장」). 나만 보는 목록이다. 클럽과 나누지 않는다.
-- 장소가 지워지면(클럽 전용 장소의 클럽이 지워질 때) 스크랩도 함께 지운다.
CREATE TABLE scraps (
    user_id     bigint       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    place_id    bigint       NOT NULL REFERENCES places (id) ON DELETE CASCADE,
    created_at  timestamptz  NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, place_id)
);

-- 내 스크랩 목록 (최근순)
CREATE INDEX ix_scraps_user_created_at ON scraps (user_id, created_at DESC);
