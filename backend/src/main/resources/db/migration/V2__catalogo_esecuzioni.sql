delete from task_dependencies;
delete from tasks;
delete from workflows;

alter table workflows drop column stato;
alter table workflows add column definizione_json text not null;
alter table workflows add column numero_task integer not null default 0;

create table esecuzioni (
    id bigserial primary key,
    workflow_id bigint not null references workflows(id) on delete cascade,
    nome varchar(255) not null,
    data_inizio timestamp not null,
    data_fine timestamp,
    stato varchar(32) not null
);

create index idx_esecuzioni_workflow_id on esecuzioni(workflow_id);

alter table tasks drop constraint tasks_workflow_id_fkey;
alter table tasks rename column workflow_id to esecuzione_id;
alter table tasks add constraint tasks_esecuzione_id_fkey
    foreign key (esecuzione_id) references esecuzioni(id) on delete cascade;
alter index idx_tasks_workflow_id rename to idx_tasks_esecuzione_id;
