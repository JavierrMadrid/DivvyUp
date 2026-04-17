-- =============================================================================
-- DivvyUp — V010
-- Permite que múltiples usuarios puedan seleccionar el mismo participante
-- como "Soy yo" en un grupo, eliminando la restricción unique(participant_id).
--
-- CONTEXTO:
--   V004 creó participant_user_links con:
--     unique (group_id, user_id)   → un usuario tiene un participante por grupo ✅ (se mantiene)
--     unique (participant_id)      → un participante tiene como máximo un usuario ❌ (se elimina)
--
--   El requisito del usuario es "da igual que se repita" → varios usuarios pueden
--   vincularse al mismo participante. La restricción unique(group_id, user_id)
--   garantiza que un usuario sigue teniendo solo UN participante por grupo.
-- =============================================================================

-- Eliminar la restricción unique(participant_id)
alter table participant_user_links
  drop constraint if exists participant_user_links_participant_id_key;

