-- Add index for optimizing message retrieval by created_at and id
CREATE INDEX chat_messages_created_at_id_idx ON chat_messages (created_at DESC, id DESC);
