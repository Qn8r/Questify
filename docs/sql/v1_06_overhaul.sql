create table if not exists public.app_plan_states (
  user_id text primary key,
  user_name text not null default 'Player',
  plans_json text not null default '{}',
  updated_at timestamptz not null default now()
);

create table if not exists public.app_history_states (
  user_id text not null,
  epoch_day bigint not null,
  user_name text not null default 'Player',
  done_count integer not null default 0,
  total_count integer not null default 0,
  all_done boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (user_id, epoch_day)
);

create index if not exists app_history_states_user_day_idx
  on public.app_history_states (user_id, epoch_day desc);

alter table public.app_plan_states enable row level security;
alter table public.app_history_states enable row level security;

drop policy if exists app_plan_states_insert_policy on public.app_plan_states;
create policy app_plan_states_insert_policy
on public.app_plan_states
for insert
to anon, authenticated
with check (true);

drop policy if exists app_plan_states_update_policy on public.app_plan_states;
create policy app_plan_states_update_policy
on public.app_plan_states
for update
to anon, authenticated
using (true)
with check (true);

drop policy if exists app_history_states_insert_policy on public.app_history_states;
create policy app_history_states_insert_policy
on public.app_history_states
for insert
to anon, authenticated
with check (true);

drop policy if exists app_history_states_update_policy on public.app_history_states;
create policy app_history_states_update_policy
on public.app_history_states
for update
to anon, authenticated
using (true)
with check (true);
