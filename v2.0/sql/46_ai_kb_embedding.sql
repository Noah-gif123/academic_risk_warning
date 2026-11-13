-- 方案 A：本地 embedding 向量（qwen3.7-text-embedding, 1024 维）
create table if not exists ai_kb_embedding (
    chunk_id bigint primary key,
    course_id bigint not null,
    model varchar(64) not null,
    dim int not null,
    vector longtext not null,
    content_hash varchar(64) not null,
    update_time datetime not null default current_timestamp on update current_timestamp,
    key idx_course (course_id)
) engine=InnoDB default charset=utf8mb4;
