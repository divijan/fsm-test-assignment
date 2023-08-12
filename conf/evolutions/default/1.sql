# --- !Ups

create table "state_transitions" (
  "from" varchar not null,
  "to" varchar not null,
  PRIMARY KEY("from", "to")
);

create table "init_states" (
  "name" varchar not null PRIMARY KEY
);

# --- !Downs

drop table "state_transitions" if exists;
drop table "init_states" if exists;