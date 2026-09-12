alter table reward_offer
    add column if not exists total_quantity integer not null default 0;

alter table reward_offer drop constraint if exists reward_offer_total_quantity_check;
alter table reward_offer add constraint reward_offer_total_quantity_check
    check (total_quantity >= 0);

update reward_offer offer
set total_quantity = inventory.total_quantity
    from (
    select reward_offer_id, count(*)::integer as total_quantity
    from reward_inventory_item
    group by reward_offer_id
) inventory
where offer.id = inventory.reward_offer_id;

create or replace function sync_reward_offer_total_quantity()
returns trigger as $$
begin
    if tg_op = 'INSERT' then
update reward_offer
set total_quantity = total_quantity + 1
where id = new.reward_offer_id;
return new;
end if;

    if tg_op = 'DELETE' then
update reward_offer
set total_quantity = greatest(total_quantity - 1, 0)
where id = old.reward_offer_id;
return old;
end if;

return null;
end;
$$ language plpgsql;

drop trigger if exists reward_inventory_total_quantity_trigger on reward_inventory_item;
create trigger reward_inventory_total_quantity_trigger
    after insert or delete on reward_inventory_item
for each row execute function sync_reward_offer_total_quantity();