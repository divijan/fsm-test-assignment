# --- !Ups

create table "transitions" (
  "entity_name" varchar not null,
  "from" varchar,
  "to" varchar not null,
  "timestamp" timestamp not null
);

# --- !Downs

drop table "transitions" if exists;
