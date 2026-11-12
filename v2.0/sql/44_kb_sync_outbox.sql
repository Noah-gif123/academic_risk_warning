-- 课程知识库 → Dify 向量副本同步 outbox（P0）
create table if not exists ai_kb_sync_outbox (
    id bigint primary key auto_increment,
    course_id bigint not null,
    doc_id bigint null,
    action varchar(20) not null,
    status varchar(20) not null default 'PENDING',
    attempts int not null default 0,
    error varchar(500) null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    key idx_status (status, id),
    key idx_course (course_id)
) engine=InnoDB default charset=utf8mb4;

alter table ai_kb_doc add column dify_document_id varchar(64) null comment 'Dify 向量库文档 ID';
