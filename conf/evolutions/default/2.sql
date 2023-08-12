# --- !Ups

create table "entities" (
  "name" varchar not null PRIMARY KEY,
  "state" varchar not null
);

# --- !Downs

drop table "entities" if exists;
