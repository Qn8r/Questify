create extension if not exists pgcrypto;

create table if not exists public.app_feedback_inbox (
  id uuid primary key default gen_random_uuid(),
  user_id text not null,
  user_name text not null default 'Player',
  category text not null default 'General',
  message text not null,
  app_theme text not null default 'DEFAULT',
  level integer not null default 1,
  created_at timestamptz not null default now()
);

create index if not exists app_feedback_inbox_created_at_idx
  on public.app_feedback_inbox (created_at desc);

alter table public.app_feedback_inbox enable row level security;

drop policy if exists app_feedback_inbox_insert_policy on public.app_feedback_inbox;
create policy app_feedback_inbox_insert_policy
on public.app_feedback_inbox
for insert
to anon, authenticated
with check (true);
