-- Authorization System Migration
-- Adds user authentication, role-based access control, and user-specific data isolation

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- User knowledge bases junction table
CREATE TABLE IF NOT EXISTS user_knowledge_bases (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, knowledge_base_id)
);

CREATE INDEX IF NOT EXISTS idx_user_knowledge_bases_user_id ON user_knowledge_bases(user_id);
CREATE INDEX IF NOT EXISTS idx_user_knowledge_bases_kb_id ON user_knowledge_bases(knowledge_base_id);

-- User conversations junction table
CREATE TABLE IF NOT EXISTS user_conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, conversation_id)
);

CREATE INDEX IF NOT EXISTS idx_user_conversations_user_id ON user_conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_user_conversations_conv_id ON user_conversations(conversation_id);

-- User activity log table
CREATE TABLE IF NOT EXISTS user_activity_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id UUID,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_activity_log_user_id ON user_activity_log(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_log_action ON user_activity_log(action);
CREATE INDEX IF NOT EXISTS idx_user_activity_log_created ON user_activity_log(created_at DESC);

-- Add owner_id to knowledge_bases
ALTER TABLE knowledge_bases ADD COLUMN IF NOT EXISTS owner_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_owner_id ON knowledge_bases(owner_id);

-- Add owner_id to conversations
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS owner_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_conversations_owner_id ON conversations(owner_id);

-- Add owner_id to collections
ALTER TABLE collections ADD COLUMN IF NOT EXISTS owner_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_collections_owner_id ON collections(owner_id);

-- Create trigger to update updated_at on users
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default admin user
-- Password: Admin@123 (BCrypt hashed)
INSERT INTO users (id, email, password_hash, username, role, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@enterprise.ai',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- Assign existing knowledge bases to default admin user
UPDATE knowledge_bases SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;

-- Assign existing conversations to default admin user
UPDATE conversations SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;

-- Assign existing collections to default admin user
UPDATE collections SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;

-- Create junction table entries for existing knowledge bases
INSERT INTO user_knowledge_bases (id, user_id, knowledge_base_id, created_at)
SELECT 
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    id,
    CURRENT_TIMESTAMP
FROM knowledge_bases
WHERE owner_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (user_id, knowledge_base_id) DO NOTHING;

-- Create junction table entries for existing conversations
INSERT INTO user_conversations (id, user_id, conversation_id, created_at)
SELECT 
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    id,
    CURRENT_TIMESTAMP
FROM conversations
WHERE owner_id = '00000000-0000-0000-0000-000000000001'
ON CONFLICT (user_id, conversation_id) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE users IS 'User accounts for authentication and authorization';
COMMENT ON TABLE user_knowledge_bases IS 'Junction table for user-knowledge base ownership';
COMMENT ON TABLE user_conversations IS 'Junction table for user-conversation ownership';
COMMENT ON TABLE user_activity_log IS 'Audit log for user activities';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role: ADMIN or USER';
COMMENT ON COLUMN knowledge_bases.owner_id IS 'Owner user ID for access control';
COMMENT ON COLUMN conversations.owner_id IS 'Owner user ID for access control';
COMMENT ON COLUMN collections.owner_id IS 'Owner user ID for access control';
