-- V7: data hygiene — capitalise hotel cities typed in lowercase in the admin
-- ("hammamet" → "Hammamet"). New entries are normalised by the API (Validation.capitalizeWords).
UPDATE hotel SET city = initcap(city), updated_at = now() WHERE city = lower(city);
