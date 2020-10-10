# --- !Ups

create table "entities" (
  "name" varchar not null PRIMARY KEY,
  "stateName" varchar not null
);

# --- !Downs

drop table "entities" if exists;
