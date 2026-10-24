-- Repairs only the demo rows whose text was stored as CP1251 interpreted as UTF-8.
-- User-created rows are not included in these ID ranges.
BEGIN;

UPDATE clients
SET full_name = CASE
        WHEN id = 1 THEN U&'\0418\0432\0430\043D\043E\0432 \0418\0432\0430\043D \0418\0432\0430\043D\043E\0432\0438\0447'
        WHEN left(full_name, 1) = U&'\0420' THEN convert_from(convert_to(full_name, 'WIN1251'), 'UTF8')
        ELSE full_name
    END,
    address = CASE
        WHEN left(address, 1) = U&'\0420' THEN convert_from(convert_to(address, 'WIN1251'), 'UTF8')
        ELSE address
    END
WHERE id BETWEEN 1 AND 5
  AND (left(full_name, 1) = U&'\0420' OR left(address, 1) = U&'\0420');

UPDATE service_requests
SET description = convert_from(convert_to(description, 'WIN1251'), 'UTF8')
WHERE id BETWEEN 1 AND 10
  AND left(description, 1) = U&'\0420';

COMMIT;
