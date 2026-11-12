create table if not exists ai_qa_feedback (
    id bigint primary key auto_increment,
    run_id bigint not null,
    student_id bigint not null,
    useful tinyint(1) not null,
    reason varchar(300) null,
    create_time datetime not null default current_timestamp,
    unique key uk_run (run_id)
) engine=InnoDB default charset=utf8mb4;
