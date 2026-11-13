-- 异步 embedding 任务队列
create table if not exists ai_kb_embedding_job (
    id bigint primary key auto_increment,
    chunk_id bigint not null,
    course_id bigint not null,
    action varchar(10) not null default 'UPSERT',
    status varchar(20) not null default 'PENDING',
    attempts int not null default 0,
    error varchar(500) null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    unique key uk_chunk_action (chunk_id, action),
    key idx_status (status, id)
) engine=InnoDB default charset=utf8mb4;
