insert into reward_offer(partner_name, title, description, price, active) values
('Кофейня «Маршрут»', 'Скидка 20% на кофе', 'Промокод на один напиток в партнерской кофейне.', 100, true),
('Киноцентр «Нева»', 'Билет со скидкой', 'Скидка 300 рублей на билет.', 250, true),
('Самокат', 'Купон на доставку', 'Промокод на бесплатную доставку.', 350, true),
('Буквоед', 'Скидка на книгу', 'Промокод 15% на одну покупку.', 500, true),
('Спортцентр «Баланс»', 'Разовое посещение', 'Пробное посещение тренажерного зала.', 900, true)
on conflict do nothing;

insert into task(activity_type, title, points, target_count, status, auto_generated, center_latitude, center_longitude)
values ('REVIEW', 'Оцените качество трех точек на карте в центре города', 30, 3, 'ACTIVE', false, 59.9343, 30.3351),
       ('VOLUNTEER', 'Помогите трем людям с прогулкой в ближайшем районе', 130, 3, 'ACTIVE', false, 59.9343, 30.3351)
on conflict do nothing;

-- Цели заданий лучше добавлять под реальные id из вашей базы. Примеры:
-- insert into task_target(task_id, target_type, target_id, title, latitude, longitude, sort_order)
-- select t.id, 'OBSTACLE_FEATURE', f.id, concat(f.street, ', ', f.house), f.latitude, f.longitude, row_number() over ()
-- from task t join obstacle_feature f on f.id in (1,2,3)
-- where t.title = 'Оцените качество трех точек на карте в центре города';
