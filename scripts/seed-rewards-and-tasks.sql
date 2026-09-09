delete from reward_offer
where partner_name in (
    'SmartJam', 'Volta', 'Baza', 'gtest_research', 'MoveOn', 'GoodRoad Project',
    'SkillTree', 'Cheqmate', 'Codzilla HSE', 'PlotMap', 'PySATL', 'AlignLib',
    'PR2026', 'Pawspective', 'Messenger ALYOsha', 'Yet Another Survey', 'DICE',
    'Truten', 'Family Budget', 'PRIYOMysh', 'Chronos', 'Seagull Messenger',
    'KeySpaceBreaker', 'RoomSched', 'PearToPear', 'OnlineDesk',
    'Кофейня «Маршрут»', 'Киноцентр «Нева»', 'Самокат', 'Буквоед', 'Спортцентр «Баланс»'
);

insert into reward_offer(
    partner_name, title, description, price, active, reward_type, validity_days
) values
    ('Кофейня «Теплый поворот»', 'Капучино в подарок', 'Купон на один капучино стандартного размера.', 180, true, 'COUPON', 30),
    ('Пекарня «Северный крендель»', 'Скидка 25% на выпечку', 'Купон на скидку 25% на выпечку собственного производства.', 150, true, 'COUPON', 30),
    ('Книжный магазин «Лист и линия»', 'Скидка 20% на книгу', 'Купон на скидку 20% на одну книгу из ассортимента магазина.', 220, true, 'COUPON', 45),
    ('Магазин «Городская полка»', 'Скидка 300 рублей', 'Купон на скидку 300 рублей при покупке от 1200 рублей.', 260, true, 'COUPON', 30),
    ('Кафе «Маяк во дворе»', 'Десерт в подарок', 'Купон на один десерт при заказе любого горячего напитка.', 200, true, 'COUPON', 30),
    ('Магазин «Шаг рядом»', 'Скидка 15% на покупку', 'Купон на скидку 15% на одну покупку.', 190, true, 'COUPON', 30),
    ('Кофейня «Точка сбора»', 'Любой чай в подарок', 'Купон на один чай стандартного размера.', 160, true, 'COUPON', 30),
    ('Магазин «Лавка у моста»', 'Скидка 20% на товары для дома', 'Купон на скидку 20% на товары для дома.', 230, true, 'COUPON', 45),
    ('Кафе «Медный чайник»', 'Скидка 30% на завтрак', 'Купон на скидку 30% на одно блюдо из меню завтраков.', 210, true, 'COUPON', 30),
    ('Магазин «Соседний дом»', 'Скидка 250 рублей', 'Купон на скидку 250 рублей при покупке от 1000 рублей.', 200, true, 'COUPON', 30)
on conflict (partner_name, title) do update
set description = excluded.description,
    price = excluded.price,
    active = excluded.active,
    reward_type = excluded.reward_type,
    validity_days = excluded.validity_days;

insert into reward_inventory_item(reward_offer_id, code, status)
select offer.id, codes.code, 'AVAILABLE'
from (
    values
        ('Кофейня «Теплый поворот»', 'Капучино в подарок', 'TURN-COFFEE-001'),
        ('Кофейня «Теплый поворот»', 'Капучино в подарок', 'TURN-COFFEE-002'),
        ('Кофейня «Теплый поворот»', 'Капучино в подарок', 'TURN-COFFEE-003'),
        ('Кофейня «Теплый поворот»', 'Капучино в подарок', 'TURN-COFFEE-004'),
        ('Кофейня «Теплый поворот»', 'Капучино в подарок', 'TURN-COFFEE-005'),

        ('Пекарня «Северный крендель»', 'Скидка 25% на выпечку', 'NORTH-BREAD-001'),
        ('Пекарня «Северный крендель»', 'Скидка 25% на выпечку', 'NORTH-BREAD-002'),
        ('Пекарня «Северный крендель»', 'Скидка 25% на выпечку', 'NORTH-BREAD-003'),
        ('Пекарня «Северный крендель»', 'Скидка 25% на выпечку', 'NORTH-BREAD-004'),
        ('Пекарня «Северный крендель»', 'Скидка 25% на выпечку', 'NORTH-BREAD-005'),

        ('Книжный магазин «Лист и линия»', 'Скидка 20% на книгу', 'LEAF-LINE-001'),
        ('Книжный магазин «Лист и линия»', 'Скидка 20% на книгу', 'LEAF-LINE-002'),
        ('Книжный магазин «Лист и линия»', 'Скидка 20% на книгу', 'LEAF-LINE-003'),
        ('Книжный магазин «Лист и линия»', 'Скидка 20% на книгу', 'LEAF-LINE-004'),
        ('Книжный магазин «Лист и линия»', 'Скидка 20% на книгу', 'LEAF-LINE-005'),

        ('Магазин «Городская полка»', 'Скидка 300 рублей', 'CITY-SHELF-001'),
        ('Магазин «Городская полка»', 'Скидка 300 рублей', 'CITY-SHELF-002'),
        ('Магазин «Городская полка»', 'Скидка 300 рублей', 'CITY-SHELF-003'),
        ('Магазин «Городская полка»', 'Скидка 300 рублей', 'CITY-SHELF-004'),
        ('Магазин «Городская полка»', 'Скидка 300 рублей', 'CITY-SHELF-005'),

        ('Кафе «Маяк во дворе»', 'Десерт в подарок', 'YARD-LIGHT-001'),
        ('Кафе «Маяк во дворе»', 'Десерт в подарок', 'YARD-LIGHT-002'),
        ('Кафе «Маяк во дворе»', 'Десерт в подарок', 'YARD-LIGHT-003'),
        ('Кафе «Маяк во дворе»', 'Десерт в подарок', 'YARD-LIGHT-004'),
        ('Кафе «Маяк во дворе»', 'Десерт в подарок', 'YARD-LIGHT-005'),

        ('Магазин «Шаг рядом»', 'Скидка 15% на покупку', 'NEAR-STEP-001'),
        ('Магазин «Шаг рядом»', 'Скидка 15% на покупку', 'NEAR-STEP-002'),
        ('Магазин «Шаг рядом»', 'Скидка 15% на покупку', 'NEAR-STEP-003'),
        ('Магазин «Шаг рядом»', 'Скидка 15% на покупку', 'NEAR-STEP-004'),
        ('Магазин «Шаг рядом»', 'Скидка 15% на покупку', 'NEAR-STEP-005'),

        ('Кофейня «Точка сбора»', 'Любой чай в подарок', 'MEET-TEA-001'),
        ('Кофейня «Точка сбора»', 'Любой чай в подарок', 'MEET-TEA-002'),
        ('Кофейня «Точка сбора»', 'Любой чай в подарок', 'MEET-TEA-003'),
        ('Кофейня «Точка сбора»', 'Любой чай в подарок', 'MEET-TEA-004'),
        ('Кофейня «Точка сбора»', 'Любой чай в подарок', 'MEET-TEA-005'),

        ('Магазин «Лавка у моста»', 'Скидка 20% на товары для дома', 'BRIDGE-SHOP-001'),
        ('Магазин «Лавка у моста»', 'Скидка 20% на товары для дома', 'BRIDGE-SHOP-002'),
        ('Магазин «Лавка у моста»', 'Скидка 20% на товары для дома', 'BRIDGE-SHOP-003'),
        ('Магазин «Лавка у моста»', 'Скидка 20% на товары для дома', 'BRIDGE-SHOP-004'),
        ('Магазин «Лавка у моста»', 'Скидка 20% на товары для дома', 'BRIDGE-SHOP-005'),

        ('Кафе «Медный чайник»', 'Скидка 30% на завтрак', 'COPPER-TEA-001'),
        ('Кафе «Медный чайник»', 'Скидка 30% на завтрак', 'COPPER-TEA-002'),
        ('Кафе «Медный чайник»', 'Скидка 30% на завтрак', 'COPPER-TEA-003'),
        ('Кафе «Медный чайник»', 'Скидка 30% на завтрак', 'COPPER-TEA-004'),
        ('Кафе «Медный чайник»', 'Скидка 30% на завтрак', 'COPPER-TEA-005'),

        ('Магазин «Соседний дом»', 'Скидка 250 рублей', 'NEXT-HOME-001'),
        ('Магазин «Соседний дом»', 'Скидка 250 рублей', 'NEXT-HOME-002'),
        ('Магазин «Соседний дом»', 'Скидка 250 рублей', 'NEXT-HOME-003'),
        ('Магазин «Соседний дом»', 'Скидка 250 рублей', 'NEXT-HOME-004'),
        ('Магазин «Соседний дом»', 'Скидка 250 рублей', 'NEXT-HOME-005')
) as codes(partner_name, title, code)
join reward_offer offer
  on offer.partner_name = codes.partner_name
 and offer.title = codes.title
on conflict (reward_offer_id, code) do nothing;

update task
set status = 'ARCHIVED'
where activity_type = 'VOLUNTEER'
  and target_count in (5, 10)
  and status = 'ACTIVE';

insert into task(activity_type, title, points, target_count, status, auto_generated, center_latitude, center_longitude) values
    ('REVIEW', 'Проверьте три новых препятствия у учебных корпусов', 30, 3, 'ACTIVE', false, 59.9343, 30.3351),
    ('REVIEW', 'Оцените три перехода рядом с метро', 30, 3, 'ACTIVE', false, 59.9311, 30.3609),
    ('REVIEW', 'Проверьте три точки с жалобами на покрытие', 30, 3, 'ACTIVE', false, 59.9298, 30.3420),
    ('REVIEW', 'Оцените три входа в общественные здания', 30, 3, 'ACTIVE', false, 59.9380, 30.3146),
    ('REVIEW', 'Проверьте три маршрута к остановкам транспорта', 30, 3, 'ACTIVE', false, 59.9441, 30.3600),
    ('REVIEW', 'Оцените три дворовых прохода после дождя', 30, 3, 'ACTIVE', false, 59.9500, 30.3200),
    ('REVIEW', 'Проверьте три пандуса в центральном районе', 30, 3, 'ACTIVE', false, 59.9200, 30.3500),
    ('REVIEW', 'Оцените три узких участка тротуара', 30, 3, 'ACTIVE', false, 59.9600, 30.3100),
    ('REVIEW', 'Проверьте три опасных бордюра на маршруте', 30, 3, 'ACTIVE', false, 59.9700, 30.3000),
    ('REVIEW', 'Оцените три точки около социальных учреждений', 30, 3, 'ACTIVE', false, 59.9100, 30.3300),
    ('REVIEW', 'Проверьте пять мало проверенных точек у парков', 50, 5, 'ACTIVE', false, 59.9450, 30.2900),
    ('REVIEW', 'Оцените пять переходов на популярных маршрутах', 50, 5, 'ACTIVE', false, 59.9350, 30.3800),
    ('REVIEW', 'Проверьте пять препятствий около остановок', 50, 5, 'ACTIVE', false, 59.9250, 30.3000),
    ('REVIEW', 'Оцените пять точек с низким числом отзывов', 50, 5, 'ACTIVE', false, 59.9550, 30.3400),
    ('REVIEW', 'Проверьте пять участков около учебных зданий', 50, 5, 'ACTIVE', false, 59.9650, 30.3600),
    ('REVIEW', 'Оцените пять входов и съездов на тротуарах', 50, 5, 'ACTIVE', false, 59.9150, 30.3100),
    ('REVIEW', 'Проверьте пять сложных мест у больниц и аптек', 50, 5, 'ACTIVE', false, 59.9050, 30.3700),
    ('REVIEW', 'Оцените пять точек на вечернем маршруте', 50, 5, 'ACTIVE', false, 59.9750, 30.3300),
    ('REVIEW', 'Проверьте десять старых отметок на карте', 100, 10, 'ACTIVE', false, 59.9400, 30.3300),
    ('REVIEW', 'Оцените десять препятствий в соседних кварталах', 100, 10, 'ACTIVE', false, 59.9300, 30.3200),
    ('REVIEW', 'Проверьте десять точек без свежих отзывов', 100, 10, 'ACTIVE', false, 59.9200, 30.3100),
    ('REVIEW', 'Оцените десять объектов городской доступности', 100, 10, 'ACTIVE', false, 59.9100, 30.3000),
    ('REVIEW', 'Проверьте десять маршрутов вокруг кампуса', 100, 10, 'ACTIVE', false, 59.9000, 30.3400),
    ('REVIEW', 'Оцените десять точек на длинной прогулке', 100, 10, 'ACTIVE', false, 59.9800, 30.3500),
    ('REVIEW', 'Проверьте десять мест с возможными барьерами', 100, 10, 'ACTIVE', false, 59.9900, 30.3600),
    ('VOLUNTEER', 'Помогите одному человеку дойти до остановки', 120, 1, 'ACTIVE', false, 59.9343, 30.3351),
    ('VOLUNTEER', 'Помогите одному человеку на коротком маршруте', 120, 1, 'ACTIVE', false, 59.9311, 30.3609),
    ('VOLUNTEER', 'Сопроводите одного человека до аптеки', 120, 1, 'ACTIVE', false, 59.9298, 30.3420),
    ('VOLUNTEER', 'Помогите одному человеку перейти сложный участок', 120, 1, 'ACTIVE', false, 59.9380, 30.3146),
    ('VOLUNTEER', 'Сопроводите одного человека до транспорта', 120, 1, 'ACTIVE', false, 59.9441, 30.3600),
    ('VOLUNTEER', 'Помогите одному человеку с прогулкой вечером', 120, 1, 'ACTIVE', false, 59.9500, 30.3200),
    ('VOLUNTEER', 'Сопроводите одного человека по знакомому району', 120, 1, 'ACTIVE', false, 59.9600, 30.3100),
    ('VOLUNTEER', 'Помогите одному человеку с маршрутом до магазина', 120, 1, 'ACTIVE', false, 59.9700, 30.3000),
    ('VOLUNTEER', 'Сопроводите одного человека после учебы', 120, 1, 'ACTIVE', false, 59.9100, 30.3300),
    ('VOLUNTEER', 'Помогите одному человеку пройти маршрут без барьеров', 120, 1, 'ACTIVE', false, 59.9450, 30.2900),
    ('VOLUNTEER', 'Помогите трем людям с прогулками у метро', 150, 3, 'ACTIVE', false, 59.9350, 30.3800),
    ('VOLUNTEER', 'Сопроводите трех людей на непересекающихся маршрутах', 150, 3, 'ACTIVE', false, 59.9250, 30.3000),
    ('VOLUNTEER', 'Помогите трем людям в соседних кварталах', 150, 3, 'ACTIVE', false, 59.9550, 30.3400),
    ('VOLUNTEER', 'Сопроводите трех людей до остановок', 150, 3, 'ACTIVE', false, 59.9650, 30.3600),
    ('VOLUNTEER', 'Помогите трем людям с дневными прогулками', 150, 3, 'ACTIVE', false, 59.9150, 30.3100),
    ('VOLUNTEER', 'Сопроводите трех людей по коротким маршрутам', 150, 3, 'ACTIVE', false, 59.9050, 30.3700),
    ('VOLUNTEER', 'Помогите трем людям около социальных объектов', 150, 3, 'ACTIVE', false, 59.9750, 30.3300),
    ('VOLUNTEER', 'Сопроводите трех людей в разное время дня', 150, 3, 'ACTIVE', false, 59.9400, 30.3300),
    ('VOLUNTEER', 'Помогите трем людям с безопасным маршрутом', 150, 3, 'ACTIVE', false, 59.9300, 30.3200),
    ('VOLUNTEER', 'Сопроводите трех людей рядом с кампусом', 150, 3, 'ACTIVE', false, 59.9200, 30.3100),
    ('VOLUNTEER', 'Помогите трем людям на маршрутах без пересечений', 150, 3, 'ACTIVE', false, 59.9100, 30.3000),
    ('VOLUNTEER', 'Сопроводите трех людей в разных районах', 150, 3, 'ACTIVE', false, 59.9000, 30.3400),
    ('VOLUNTEER', 'Помогите трем людям пройти сложные переходы', 150, 3, 'ACTIVE', false, 59.9800, 30.3500),
    ('VOLUNTEER', 'Сопроводите трех людей по вечерним маршрутам', 150, 3, 'ACTIVE', false, 59.9900, 30.3600),
    ('VOLUNTEER', 'Помогите трем людям с прогулками рядом с вами', 150, 3, 'ACTIVE', false, 59.9343, 30.3351)
on conflict do nothing;

update task
set points = case
    when activity_type = 'REVIEW' and target_count = 3 then 30
    when activity_type = 'REVIEW' and target_count = 5 then 50
    when activity_type = 'REVIEW' and target_count = 10 then 100
    when activity_type = 'VOLUNTEER' and target_count = 1 then 120
    when activity_type = 'VOLUNTEER' and target_count = 3 then 150
    else points
end
where activity_type in ('REVIEW', 'VOLUNTEER');

with review_tasks as (
    select id, title, target_count, row_number() over (order by id) as rn
    from task
    where activity_type = 'REVIEW' and status = 'ACTIVE'
), numbered_features as (
    select id,
           coalesce(nullif(concat_ws(', ', street, house), ''), place_name, 'Точка #' || id) as title,
           latitude,
           longitude,
           row_number() over (order by id) as rn
    from obstacle_feature
)
insert into task_target(task_id, target_type, target_id, title, latitude, longitude, sort_order)
select t.id,
       'OBSTACLE_FEATURE',
       f.id,
       f.title,
       f.latitude,
       f.longitude,
       (f.rn - 1) % t.target_count
from review_tasks t
join numbered_features f
  on f.rn > (t.rn - 1) * t.target_count
 and f.rn <= t.rn * t.target_count
on conflict do nothing;

with volunteer_tasks as (
    select id, title, target_count, row_number() over (order by id) as rn
    from task
    where activity_type = 'VOLUNTEER'
      and status = 'ACTIVE'
      and target_count in (1, 3)
), numbered_requests as (
    select id,
           concat(from_address, ' → ', to_address, ', ', walk_date, ' ', walk_time) as title,
           start_latitude,
           start_longitude,
           row_number() over (order by walk_date, walk_time, id) as rn
    from help_request
    where status = 'OPEN'
      and volunteer_id is null
      and start_latitude is not null
      and start_longitude is not null
)
insert into task_target(task_id, target_type, target_id, title, latitude, longitude, sort_order)
select t.id,
       'HELP_REQUEST',
       r.id,
       r.title,
       r.start_latitude,
       r.start_longitude,
       (r.rn - 1) % t.target_count
from volunteer_tasks t
join numbered_requests r
  on r.rn > (t.rn - 1) * t.target_count
 and r.rn <= t.rn * t.target_count
on conflict do nothing;
