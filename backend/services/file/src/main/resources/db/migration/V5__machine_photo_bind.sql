CREATE UNIQUE INDEX files_machine_photo_entity ON files (entity_id) WHERE kind = 'MACHINE_PHOTO' AND entity_id IS NOT NULL;
