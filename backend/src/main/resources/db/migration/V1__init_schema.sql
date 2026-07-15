create table workflows (
    id bigserial primary key,
    nome varchar(255) not null,
    data_creazione timestamp not null,
    stato varchar(32) not null
);

create table tasks (
    id bigserial primary key,
    workflow_id bigint not null references workflows(id) on delete cascade,
    nome varchar(255) not null,
    stato varchar(32) not null,
    tentativi integer not null,
    command_type varchar(64) not null,
    command_payload text not null
);

create index idx_tasks_workflow_id on tasks(workflow_id);

create table task_dependencies (
    task_id bigint not null references tasks(id) on delete cascade,
    dependency_task_id bigint not null references tasks(id) on delete cascade,
    primary key (task_id, dependency_task_id),
    constraint chk_task_dependency_no_self check (task_id <> dependency_task_id)
);

create index idx_task_dependencies_dependency_task_id
    on task_dependencies(dependency_task_id);

create table engine_config (
    id bigint primary key,
    max_tentativi integer not null,
    intervallo integer not null
);
