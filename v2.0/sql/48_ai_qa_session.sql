create table if not exists ai_qa_session (
    id bigint primary key auto_increment,
    student_id bigint not null,
    course_id bigint not null,
    teaching_class_id bigint null,
    title varchar(120) not null default '课程答疑',
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    key idx_student_course (student_id, course_id, update_time)
) engine=InnoDB default charset=utf8mb4;

create table if not exists ai_qa_message (
    id bigint primary key auto_increment,
    session_id bigint not null,
    role varchar(16) not null,
    content longtext not null,
    citations_json longtext null,
    run_id bigint null,
    create_time datetime not null default current_timestamp,
    key idx_session (session_id, id)
) engine=InnoDB default charset=utf8mb4;
