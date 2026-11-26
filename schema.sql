-- Real-Time Sync Schema for Admin Web, TV Display APK, and Counter PC APK
-- Run this in Supabase SQL Editor to enable live sync across separate TV, PC, and Mobile devices.

create table if not exists game_state (
  id text primary key default 'main_room',
  round_number integer not null default 108,
  phase text not null default 'OPEN', -- 'OPEN' or 'CLOSE'
  seconds_left integer not null default 300,
  is_paused boolean not null default false,
  open_panna text,
  open_ank text,
  close_panna text,
  close_ank text,
  jodi text,
  smart_enabled boolean not null default true,
  balance_preset text not null default 'balanced', -- 'strict_profit', 'balanced', 'high_action'
  announcement text not null default '5 MIN OPEN | 5 MIN CLOSE | FUN COUNTER GAME',
  updated_at timestamptz not null default now()
);

create table if not exists round_history (
  id bigserial primary key,
  round_number integer not null,
  open_panna text not null,
  open_ank text not null,
  close_panna text not null,
  close_ank text not null,
  jodi text not null,
  created_at timestamptz not null default now()
);

create table if not exists counter_slips (
  slip_id text primary key,
  round_number integer not null,
  slot text not null, -- 'OPEN' or 'CLOSE'
  items_json jsonb not null,
  total_points integer not null,
  signature text not null,
  status text not null default 'ACTIVE', -- 'ACTIVE', 'CLAIMED', 'VOID'
  created_at timestamptz not null default now()
);

insert into game_state (id) values ('main_room') on conflict (id) do nothing;
