delete from reward_offer where partner_name in ('Кофейня «Маршрут»','Киноцентр «Нева»','Самокат','Буквоед','Спортцентр «Баланс»');
insert into reward_offer(partner_name, title, description, price, active) values
                                                                              ('SmartJam', 'Набор цифрового джема', 'Промокод на набор стикеров и оформление профиля в стиле музыкального джема.', 180, true),
                                                                              ('Volta', 'Заряд энергии Volta', 'Виртуальный купон на бонусные баллы за активность и устойчивые привычки.', 220, true),
                                                                              ('Baza', 'База полезных шаблонов', 'Доступ к подборке чек-листов для учебных и командных проектов.', 160, true),
                                                                              ('gtest_research', 'Набор тестировщика', 'Промокод на тематический мерч про надежные тесты и чистые проверки.', 210, true),
                                                                              ('MoveOn', 'Маршрут без паузы', 'Бонус за активное передвижение: цифровой бейдж и купон партнера.', 190, true),
                                                                              ('GoodRoad Project', 'Значок безопасного маршрута', 'Награда за вклад в доступную городскую среду и помощь на маршрутах.', 150, true),
                                                                              ('SkillTree', 'Ускоритель навыка', 'Цифровой бустер прогресса для дерева навыков.', 240, true),
                                                                              ('Cheqmate', 'Ход конем', 'Промокод на шахматный набор задач и тематический бейдж.', 200, true),
                                                                              ('Codzilla HSE', 'Кодзилла-стикерпак', 'Набор цифровых наклеек для разработчиков и командных проектов.', 170, true),
                                                                              ('PlotMap', 'Карта сюжета', 'Промокод на оформление личной карты идей и маршрутов.', 210, true),
                                                                              ('PySATL', 'Python-ядро', 'Бонус для тех, кто любит вычисления, библиотеки и аккуратный код.', 230, true),
                                                                              ('AlignLib', 'Идеальное выравнивание', 'Цифровой бейдж за точность, порядок и аккуратную работу с данными.', 180, true),
                                                                              ('PR2026', 'Публичный релиз', 'Купон на оформление карточки проекта и подготовку презентации.', 260, true),
                                                                              ('Pawspective', 'Лапка заботы', 'Тематический бейдж и купон для любителей питомцев и заботливых прогулок.', 190, true),
                                                                              ('Messenger ALYOsha', 'Связь без помех', 'Награда за коммуникацию: цифровой набор реакций и оформление чата.', 170, true),
                                                                              ('Yet Another Survey', 'Голос учтен', 'Промокод на расширенный шаблон опроса и аналитику ответов.', 160, true),
                                                                              ('DICE', 'Удачный бросок', 'Виртуальный кубик, бейдж и бонус за выполненные задания.', 180, true),
                                                                              ('Truten', 'Пчелиный бонус', 'Тематический купон и цифровой значок за регулярную активность.', 150, true),
                                                                              ('Family Budget', 'Финансовый планер', 'Шаблон бюджета и бейдж за разумное распределение баллов.', 220, true),
                                                                              ('PRIYOMysh', 'Теплый прием', 'Награда за помощь и заботливое сопровождение новых участников.', 170, true),
                                                                              ('Chronos', 'Плюс ко времени', 'Купон на планировщик задач и бейдж пунктуальности.', 240, true),
                                                                              ('Seagull Messenger', 'Чайка на связи', 'Цифровой набор для быстрых сообщений и командной координации.', 180, true),
                                                                              ('KeySpaceBreaker', 'Ключ к пространству', 'Бейдж для тех, кто открывает новые маршруты и решает сложные задачи.', 250, true),
                                                                              ('RoomSched', 'Свободная аудитория', 'Промокод на планировщик встреч и комнат.', 200, true),
                                                                              ('PearToPear', 'Груша к груше', 'Награда за взаимопомощь и обмен опытом между пользователями.', 170, true),
                                                                              ('OnlineDesk', 'Рабочее место онлайн', 'Купон на цифровой рабочий стол и набор продуктивности.', 210, true)
    on conflict do nothing;

update task set status = 'ARCHIVED' where activity_type = 'VOLUNTEER' and target_count in (5, 10) and status = 'ACTIVE';

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


-- Если в базе уже есть реальные препятствия, этот блок добавит цели к review-заданиям.
with review_tasks as (
    select id, title, target_count, row_number() over (order by id) as rn
    from task
    where activity_type = 'REVIEW' and status = 'ACTIVE'
), numbered_features as (
    select id, coalesce(nullif(concat_ws(', ', street, house), ''), place_name, 'Точка #' || id) as title,
           latitude, longitude, row_number() over (order by id) as rn
    from obstacle_feature
)
insert into task_target(task_id, target_type, target_id, title, latitude, longitude, sort_order)
select t.id, 'OBSTACLE_FEATURE', f.id, f.title, f.latitude, f.longitude, (f.rn - 1) % t.target_count
from review_tasks t
    join numbered_features f on f.rn > (t.rn - 1) * t.target_count and f.rn <= t.rn * t.target_count
    on conflict do nothing;

-- Если в базе уже есть открытые заявки о помощи с координатами старта, этот блок добавит цели к volunteer-заданиям.
with volunteer_tasks as (
    select id, title, target_count, row_number() over (order by id) as rn
    from task
    where activity_type = 'VOLUNTEER' and status = 'ACTIVE' and target_count in (1, 3)
), numbered_requests as (
    select id, concat(from_address, ' → ', to_address, ', ', walk_date, ' ', walk_time) as title,
           start_latitude, start_longitude, row_number() over (order by walk_date, walk_time, id) as rn
    from help_request
    where status = 'OPEN' and volunteer_id is null and start_latitude is not null and start_longitude is not null
)
insert into task_target(task_id, target_type, target_id, title, latitude, longitude, sort_order)
select t.id, 'HELP_REQUEST', r.id, r.title, r.start_latitude, r.start_longitude, (r.rn - 1) % t.target_count
from volunteer_tasks t
    join numbered_requests r on r.rn > (t.rn - 1) * t.target_count and r.rn <= t.rn * t.target_count
    on conflict do nothing;
