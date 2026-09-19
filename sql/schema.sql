-- РЎРєСЂРёРїС‚ СЂР°СЃСЃС‡РёС‚Р°РЅ РЅР° Р·Р°РїСѓСЃРє С‡РµСЂРµР· psql.
-- РЎРѕР·РґР°РЅРёРµ Р‘Р” РїСЂРё РЅРµРѕР±С…РѕРґРёРјРѕСЃС‚Рё:
SELECT 'CREATE DATABASE aquarium_service'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'aquarium_service')\gexec

\connect aquarium_service

DROP TABLE IF EXISTS service_requests;
DROP TABLE IF EXISTS clients;

CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL
);

CREATE TABLE service_requests (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    aquarium_type VARCHAR(30) NOT NULL,
    service_type VARCHAR(40) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_request_client
        FOREIGN KEY (client_id) REFERENCES clients(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE ,
    CONSTRAINT chk_priority CHECK (priority BETWEEN 1 AND 5),
    CONSTRAINT chk_price CHECK (price >= 0),
    CONSTRAINT chk_aquarium_type CHECK (aquarium_type IN ('FRESHWATER','MARINE','REPTILE','PLANT')),
    CONSTRAINT chk_service_type CHECK (service_type IN ('CLEANING','WATER_CHANGE','EQUIPMENT_REPAIR','FISH_HEALTH','AQUASCAPING')),
    CONSTRAINT chk_status CHECK (status IN ('NEW','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED'))
);

INSERT INTO clients(full_name, phone, email, address) VALUES
('РРІР°РЅРѕРІ РРІР°РЅ РРІР°РЅРѕРІРёС‡', '+79990000001', 'ivanov@example.com', 'РњРѕСЃРєРІР°, СѓР». Р›РµРЅРёРЅР°, 10'),
('РџРµС‚СЂРѕРІР° РђРЅРЅР° РЎРµСЂРіРµРµРІРЅР°', '+79990000002', 'petrova@example.com', 'РњРѕСЃРєРІР°, СѓР». РњРёСЂР°, 21'),
('РЎРёРґРѕСЂРѕРІ РњР°РєСЃРёРј РћР»РµРіРѕРІРёС‡', '+79990000003', 'sidorov@example.com', 'РњРѕСЃРєРІР°, СѓР». РџСѓС€РєРёРЅР°, 7'),
('РљСѓР·РЅРµС†РѕРІР° Р•Р»РµРЅР° Р’РёРєС‚РѕСЂРѕРІРЅР°', '+79990000004', 'kuznetsova@example.com', 'РњРѕСЃРєРІР°, СѓР». Р“Р°РіР°СЂРёРЅР°, 14'),
('РћСЂР»РѕРІ Р”РјРёС‚СЂРёР№ РђРЅРґСЂРµРµРІРёС‡', '+79990000005', 'orlov@example.com', 'РњРѕСЃРєРІР°, СѓР». РЎРѕРІРµС‚СЃРєР°СЏ, 3');

INSERT INTO service_requests
(client_id, aquarium_type, service_type, description, status, priority, created_at, scheduled_at, price)
VALUES
(1,'FRESHWATER','CLEANING','РџРѕР»РЅР°СЏ С‡РёСЃС‚РєР° Р°РєРІР°СЂРёСѓРјР° 200 Р»РёС‚СЂРѕРІ','NEW',3,'2026-09-01 10:00:00','2026-09-20 12:00:00',3500.00),
(2,'MARINE','WATER_CHANGE','Р—Р°РјРµРЅР° РІРѕРґС‹ Рё РїСЂРѕРІРµСЂРєР° СЃРѕР»С‘РЅРѕСЃС‚Рё','CONFIRMED',4,'2026-09-02 11:00:00','2026-09-21 14:00:00',5000.00),
(3,'FRESHWATER','EQUIPMENT_REPAIR','Р РµРјРѕРЅС‚ РІРЅРµС€РЅРµРіРѕ С„РёР»СЊС‚СЂР°','IN_PROGRESS',5,'2026-09-03 09:30:00','2026-09-19 15:00:00',4200.00),
(4,'PLANT','AQUASCAPING','РљРѕСЂСЂРµРєС†РёСЏ РєРѕРјРїРѕР·РёС†РёРё Рё СЂР°СЃС‚РµРЅРёР№','NEW',2,'2026-09-04 13:00:00','2026-09-22 10:00:00',6000.00),
(5,'REPTILE','CLEANING','РћС‡РёСЃС‚РєР° С‚РµСЂСЂР°СЂРёСѓРјР° СЃ РІРѕРґРЅРѕР№ Р·РѕРЅРѕР№','COMPLETED',2,'2026-09-05 15:00:00','2026-09-10 11:00:00',2800.00),
(1,'FRESHWATER','FISH_HEALTH','РћСЃРјРѕС‚СЂ СЂС‹Р± Рё РґРёР°РіРЅРѕСЃС‚РёРєР°','COMPLETED',5,'2026-09-06 10:00:00','2026-09-12 16:00:00',3000.00),
(2,'MARINE','EQUIPMENT_REPAIR','Р—Р°РјРµРЅР° РїРѕРјРїС‹','CANCELLED',4,'2026-09-07 12:00:00','2026-09-13 13:00:00',4500.00),
(3,'PLANT','CLEANING','РЈРґР°Р»РµРЅРёРµ РІРѕРґРѕСЂРѕСЃР»РµР№','CONFIRMED',1,'2026-09-08 09:00:00','2026-09-23 12:00:00',2500.00),
(4,'FRESHWATER','WATER_CHANGE','РџР»Р°РЅРѕРІР°СЏ РїРѕРґРјРµРЅР° РІРѕРґС‹','NEW',3,'2026-09-09 14:00:00','2026-09-24 11:00:00',2200.00),
(5,'MARINE','AQUASCAPING','РќР°СЃС‚СЂРѕР№РєР° РјРѕСЂСЃРєРѕРіРѕ СЂРёС„Р°','IN_PROGRESS',5,'2026-09-10 16:00:00','2026-09-19 17:00:00',7500.00);
