-- KEYS:
-- 1 family, 2 grace, 3 base index, 4 elevated index
-- ARGV:
-- 1 op, 2 sid, 3 userId, 4 instanceId, 5 effectiveNowMs,
-- 6 exact expected family JSON, 7 successor family JSON, 8 successor expiry ms,
-- 9 grace JSON, 10 grace expiry ms, 11 presented hash, 12 client binding hash,
-- 13 successor elevated (0/1), 14 authVersion, 15 adminSessionVersion,
-- 16 family key prefix, 17 grace key prefix, 18 index expiry ms,
-- 19 current idle expiry ms, 20 current absolute expiry ms,
-- 21 current counter, 22 successor counter, 23 retained sid,
-- 24 user auth-version key, 25 admin-session-version key.

local BASE_CAPACITY = 10
local ELEVATED_CAPACITY = 3

local function redis_type(key)
    return redis.call('TYPE', key).ok
end

local function is_type(key, expected)
    local actual = redis_type(key)
    return actual == 'none' or actual == expected
end

local function decode_json(value)
    if not value or value == '' then
        return nil
    end
    local ok, decoded = pcall(cjson.decode, value)
    if not ok or type(decoded) ~= 'table' then
        return nil
    end
    return decoded
end

local function decimal_field(json, field)
    if not json then
        return nil
    end
    return string.match(json, '"' .. field .. '"%s*:%s*(%d+)%s*[,}]')
end

local function has_admin_role(roles)
    if type(roles) ~= 'table' then
        return false
    end
    for _, role in ipairs(roles) do
        if role == 'INSTANCE_ADMIN' then
            return true
        end
    end
    return false
end

local function is_sha256(value)
    return type(value) == 'string' and string.len(value) == 64 and
        string.match(value, '^[0-9a-f]+$') ~= nil
end

local function canonical_roles(roles)
    if type(roles) ~= 'table' or #roles == 0 then
        return false
    end
    local previous = nil
    for _, role in ipairs(roles) do
        if type(role) ~= 'string' or not string.match(role, '^[A-Z][A-Z0-9_]*$') then
            return false
        end
        if previous and previous >= role then
            return false
        end
        previous = role
    end
    return true
end

local function family_status(json, required_user, required_instance)
    local family = decode_json(json)
    if not family then
        return nil, 'MALFORMED_STATE'
    end
    if type(family.recordVersion) ~= 'number' then
        return nil, 'MALFORMED_STATE'
    end
    local raw_record_version = decimal_field(json, 'recordVersion')
    if not raw_record_version then
        return nil, 'MALFORMED_STATE'
    end
    if raw_record_version ~= '2' then
        return nil, 'UNSUPPORTED_VERSION'
    end
    local allowed = {
        recordVersion = true,
        userId = true,
        instanceId = true,
        roles = true,
        authVersion = true,
        adminSessionVersion = true,
        currentRefreshTokenHash = true,
        rotationCounter = true,
        createdAt = true,
        lastUsedAt = true,
        idleExpiresAt = true,
        absoluteExpiresAt = true,
        deviceLabel = true,
        clientBindingHash = true
    }
    for field, _ in pairs(family) do
        if not allowed[field] then
            return nil, 'MALFORMED_STATE'
        end
    end
    if type(family.userId) ~= 'string' then
        return nil, 'MALFORMED_STATE'
    end
    if required_user ~= '' and family.userId ~= required_user then
        return nil, 'OWNER_MISMATCH'
    end
    if required_instance ~= '' and
        (type(family.instanceId) ~= 'string' or family.instanceId ~= required_instance) then
        return nil, 'MALFORMED_STATE'
    end
    if type(family.instanceId) ~= 'string' or not canonical_roles(family.roles) then
        return nil, 'MALFORMED_STATE'
    end
    if type(family.authVersion) ~= 'number' or not decimal_field(json, 'authVersion') then
        return nil, 'MALFORMED_STATE'
    end
    if family.adminSessionVersion ~= nil and family.adminSessionVersion ~= cjson.null and
        (type(family.adminSessionVersion) ~= 'number' or
            not decimal_field(json, 'adminSessionVersion')) then
        return nil, 'MALFORMED_STATE'
    end
    if not is_sha256(family.currentRefreshTokenHash) or
        not is_sha256(family.clientBindingHash) or
        not decimal_field(json, 'rotationCounter') then
        return nil, 'MALFORMED_STATE'
    end
    if type(family.createdAt) ~= 'string' or type(family.lastUsedAt) ~= 'string' or
        type(family.idleExpiresAt) ~= 'string' or type(family.absoluteExpiresAt) ~= 'string' then
        return nil, 'MALFORMED_STATE'
    end
    if family.deviceLabel ~= nil and family.deviceLabel ~= cjson.null and
        type(family.deviceLabel) ~= 'string' then
        return nil, 'MALFORMED_STATE'
    end

    local admin_role = has_admin_role(family.roles)
    local admin_version = family.adminSessionVersion ~= nil and family.adminSessionVersion ~= cjson.null
    if admin_role ~= admin_version then
        return nil, 'MALFORMED_STATE'
    end

    return family, nil
end

local function grace_status(json)
    local grace = decode_json(json)
    if not grace then
        return nil, 'MALFORMED_STATE'
    end
    local allowed = {
        graceVersion = true,
        previousRefreshTokenHash = true,
        successorRefreshTokenHash = true,
        successorRotationCounter = true,
        successorEnvelope = true,
        clientBindingHash = true,
        expiresAt = true,
        expiresAtEpochMilli = true,
        consumed = true
    }
    for field, _ in pairs(grace) do
        if not allowed[field] then
            return nil, 'MALFORMED_STATE'
        end
    end
    if type(grace.graceVersion) ~= 'number' or grace.graceVersion ~= 1 or
        decimal_field(json, 'graceVersion') ~= '1' or
        not is_sha256(grace.previousRefreshTokenHash) or
        not is_sha256(grace.successorRefreshTokenHash) or
        grace.previousRefreshTokenHash == grace.successorRefreshTokenHash or
        not decimal_field(json, 'successorRotationCounter') or
        type(grace.successorEnvelope) ~= 'string' or grace.successorEnvelope == '' or
        not is_sha256(grace.clientBindingHash) or
        type(grace.expiresAt) ~= 'string' or
        type(grace.expiresAtEpochMilli) ~= 'number' or
        not decimal_field(json, 'expiresAtEpochMilli') or
        type(grace.consumed) ~= 'boolean' then
        return nil, 'MALFORMED_STATE'
    end
    return grace, nil
end

local function family_is_elevated(family)
    return has_admin_role(family.roles) and
        family.adminSessionVersion ~= nil and family.adminSessionVersion ~= cjson.null
end

local function preflight_fixed_types(include_target)
    if include_target then
        if not is_type(KEYS[1], 'string') or not is_type(KEYS[2], 'string') then
            return false
        end
    end
    if not is_type(KEYS[3], 'zset') or not is_type(KEYS[4], 'zset') then
        return false
    end
    if ARGV[24] ~= '' and not is_type(ARGV[24], 'string') then
        return false
    end
    if ARGV[25] ~= '' and not is_type(ARGV[25], 'string') then
        return false
    end
    return true
end

local function add_unique(values, seen, value)
    if not seen[value] then
        seen[value] = true
        table.insert(values, value)
    end
end

local function inspect_indexes()
    local base_members = redis.call('ZRANGE', KEYS[3], 0, -1)
    local elevated_members = redis.call('ZRANGE', KEYS[4], 0, -1)
    local all_members = {}
    local seen = {}
    for _, sid in ipairs(base_members) do
        add_unique(all_members, seen, sid)
    end
    for _, sid in ipairs(elevated_members) do
        add_unique(all_members, seen, sid)
    end

    local orphan_members = {}
    local malformed_members = {}
    local stale_elevated = {}
    local foreign_members = {}
    local valid = {}
    local base_member = {}
    local elevated_member = {}
    local scores = {}
    for _, sid in ipairs(base_members) do
        base_member[sid] = true
        scores[sid] = tonumber(redis.call('ZSCORE', KEYS[3], sid))
    end
    for _, sid in ipairs(elevated_members) do
        elevated_member[sid] = true
        if not scores[sid] then
            scores[sid] = tonumber(redis.call('ZSCORE', KEYS[4], sid))
        end
    end

    for _, sid in ipairs(all_members) do
        local family_key = ARGV[16] .. sid
        local grace_key = ARGV[17] .. sid
        if not is_type(family_key, 'string') or not is_type(grace_key, 'string') then
            return nil, 'WRONG_TYPE'
        end

        local json = redis.call('GET', family_key)
        if not json then
            orphan_members[sid] = true
        else
            local family, status = family_status(json, ARGV[3], '')
            if status == 'OWNER_MISMATCH' then
                foreign_members[sid] = true
            elseif status then
                malformed_members[sid] = true
            else
                valid[sid] = family
                if elevated_member[sid] and not family_is_elevated(family) then
                    stale_elevated[sid] = true
                end
            end
        end
    end

    local ordered_base = {}
    local ordered_elevated = {}
    local missing_base = {}
    local missing_elevated = {}
    for sid, family in pairs(valid) do
        if not orphan_members[sid] and not malformed_members[sid] and not foreign_members[sid] then
            table.insert(ordered_base, { sid = sid, score = scores[sid] })
            if not base_member[sid] then
                missing_base[sid] = scores[sid]
            end
            if family_is_elevated(family) then
                table.insert(ordered_elevated, { sid = sid, score = scores[sid] })
                if not elevated_member[sid] then
                    missing_elevated[sid] = scores[sid]
                end
            end
        end
    end
    local function lru_order(first, second)
        if first.score == second.score then
            return first.sid < second.sid
        end
        return first.score < second.score
    end
    table.sort(ordered_base, lru_order)
    table.sort(ordered_elevated, lru_order)

    return {
        base = base_members,
        elevated = elevated_members,
        valid = valid,
        orphanMembers = orphan_members,
        malformedMembers = malformed_members,
        staleElevated = stale_elevated,
        foreignMembers = foreign_members,
        baseMember = base_member,
        elevatedMember = elevated_member,
        orderedBase = ordered_base,
        orderedElevated = ordered_elevated,
        missingBase = missing_base,
        missingElevated = missing_elevated
    }, nil
end

local function select_evictions(indexes, target_elevated)
    local evicted = {}
    local elevated_count = 0
    for _, entry in ipairs(indexes.orderedElevated) do
        local sid = entry.sid
        if not indexes.orphanMembers[sid] and not indexes.malformedMembers[sid] and
            not indexes.foreignMembers[sid] and not indexes.staleElevated[sid] and sid ~= ARGV[2] then
            elevated_count = elevated_count + 1
        end
    end
    if target_elevated then
        elevated_count = elevated_count + 1
    end

    if elevated_count > ELEVATED_CAPACITY then
        for _, entry in ipairs(indexes.orderedElevated) do
            local sid = entry.sid
            if elevated_count <= ELEVATED_CAPACITY then
                break
            end
            if sid ~= ARGV[2] and not indexes.orphanMembers[sid] and
                not indexes.malformedMembers[sid] and not indexes.foreignMembers[sid] and
                not indexes.staleElevated[sid] and not evicted[sid] then
                evicted[sid] = true
                elevated_count = elevated_count - 1
            end
        end
    end

    local base_count = 0
    for _, entry in ipairs(indexes.orderedBase) do
        local sid = entry.sid
        if not indexes.orphanMembers[sid] and not indexes.malformedMembers[sid] and
            not indexes.foreignMembers[sid] and not evicted[sid] and sid ~= ARGV[2] then
            base_count = base_count + 1
        end
    end
    base_count = base_count + 1

    if base_count > BASE_CAPACITY then
        for _, entry in ipairs(indexes.orderedBase) do
            local sid = entry.sid
            if base_count <= BASE_CAPACITY then
                break
            end
            if sid ~= ARGV[2] and not indexes.orphanMembers[sid] and
                not indexes.malformedMembers[sid] and not indexes.foreignMembers[sid] and
                not evicted[sid] then
                evicted[sid] = true
                base_count = base_count - 1
            end
        end
    end

    if base_count > BASE_CAPACITY or elevated_count > ELEVATED_CAPACITY then
        return nil
    end
    return evicted
end

local function delete_family(sid)
    redis.call('DEL', ARGV[16] .. sid, ARGV[17] .. sid)
    redis.call('ZREM', KEYS[3], sid)
    redis.call('ZREM', KEYS[4], sid)
end

local function apply_cleanup(indexes, evicted)
    for sid, _ in pairs(indexes.orphanMembers) do
        delete_family(sid)
    end
    for sid, _ in pairs(indexes.malformedMembers) do
        redis.call('ZREM', KEYS[3], sid)
        redis.call('ZREM', KEYS[4], sid)
    end
    for sid, _ in pairs(indexes.staleElevated) do
        redis.call('ZREM', KEYS[4], sid)
    end
    for sid, _ in pairs(indexes.foreignMembers) do
        redis.call('ZREM', KEYS[3], sid)
        redis.call('ZREM', KEYS[4], sid)
    end
    for sid, score in pairs(indexes.missingBase) do
        if not evicted[sid] and not indexes.orphanMembers[sid] and
            not indexes.malformedMembers[sid] and not indexes.foreignMembers[sid] then
            redis.call('ZADD', KEYS[3], score, sid)
        end
    end
    for sid, score in pairs(indexes.missingElevated) do
        if not evicted[sid] and not indexes.orphanMembers[sid] and
            not indexes.malformedMembers[sid] and not indexes.foreignMembers[sid] then
            redis.call('ZADD', KEYS[4], score, sid)
        end
    end
    for sid, _ in pairs(evicted) do
        delete_family(sid)
    end
end

local function expire_or_delete_indexes(index_expiry)
    if redis.call('ZCARD', KEYS[3]) == 0 then
        redis.call('DEL', KEYS[3])
    elseif index_expiry then
        redis.call('PEXPIREAT', KEYS[3], index_expiry)
    end
    if redis.call('ZCARD', KEYS[4]) == 0 then
        redis.call('DEL', KEYS[4])
    elseif index_expiry then
        redis.call('PEXPIREAT', KEYS[4], index_expiry)
    end
end

local function write_versions()
    redis.call('SET', ARGV[24], ARGV[14])
    if ARGV[13] == '1' then
        redis.call('SET', ARGV[25], ARGV[15])
    end
end

local function preflight_successor(current, next_json, grace_required)
    local successor, status = family_status(next_json, ARGV[3], ARGV[4])
    if status then
        return nil, nil, status
    end
    if decimal_field(next_json, 'rotationCounter') ~= ARGV[22] then
        return nil, nil, 'MALFORMED_STATE'
    end
    if current then
        if successor.createdAt ~= current.createdAt or
            successor.absoluteExpiresAt ~= current.absoluteExpiresAt then
            return nil, nil, 'MALFORMED_STATE'
        end
    end

    local grace = nil
    if grace_required then
        local grace_failure = nil
        grace, grace_failure = grace_status(ARGV[9])
        if grace_failure or grace.consumed ~= false then
            return nil, nil, grace_failure or 'MALFORMED_STATE'
        end
        if grace.previousRefreshTokenHash ~= current.currentRefreshTokenHash or
            grace.successorRefreshTokenHash ~= successor.currentRefreshTokenHash or
            grace.clientBindingHash ~= ARGV[12] or
            decimal_field(ARGV[9], 'expiresAtEpochMilli') ~= ARGV[10] or
            decimal_field(ARGV[9], 'successorRotationCounter') ~= ARGV[22] then
            return nil, nil, 'MALFORMED_STATE'
        end
    end
    return successor, grace, nil
end

local function create_family()
    if not preflight_fixed_types(true) then
        return { 'WRONG_TYPE' }
    end
    if redis.call('EXISTS', KEYS[1]) == 1 then
        return { 'MALFORMED_STATE' }
    end

    local successor, _, status = preflight_successor(nil, ARGV[7], false)
    if status then
        return { status }
    end
    if ARGV[21] ~= '0' or ARGV[22] ~= '0' or
        tonumber(ARGV[8]) <= tonumber(ARGV[5]) then
        return { 'MALFORMED_STATE' }
    end

    local indexes, index_status = inspect_indexes()
    if index_status then
        return { index_status }
    end
    local evicted = select_evictions(indexes, ARGV[13] == '1')
    if not evicted then
        return { 'MALFORMED_STATE' }
    end

    apply_cleanup(indexes, evicted)
    redis.call('SET', KEYS[1], ARGV[7], 'PXAT', ARGV[8])
    redis.call('DEL', KEYS[2])
    redis.call('ZADD', KEYS[3], ARGV[5], ARGV[2])
    if ARGV[13] == '1' then
        redis.call('ZADD', KEYS[4], ARGV[5], ARGV[2])
    else
        redis.call('ZREM', KEYS[4], ARGV[2])
    end
    write_versions()
    expire_or_delete_indexes(ARGV[18])
    return { 'CREATED', ARGV[7] }
end

local function revoke_replay()
    delete_family(ARGV[2])
    expire_or_delete_indexes(nil)
    return { 'REPLAY_REVOKED' }
end

local function recover_grace(current_json, current)
    local grace_json = redis.call('GET', KEYS[2])
    if not grace_json then
        return revoke_replay()
    end
    local grace, grace_failure = grace_status(grace_json)
    if grace_failure then
        return { grace_failure }
    end

    local grace_counter = decimal_field(grace_json, 'successorRotationCounter')
    local grace_expiry = decimal_field(grace_json, 'expiresAtEpochMilli')
    local current_counter = decimal_field(current_json, 'rotationCounter')
    local eligible = grace.previousRefreshTokenHash == ARGV[11] and
        grace.successorRefreshTokenHash == current.currentRefreshTokenHash and
        grace.clientBindingHash == ARGV[12] and
        grace.consumed == false and
        grace_counter == current_counter and
        grace_expiry ~= nil and tonumber(ARGV[5]) < tonumber(grace_expiry)
    if not eligible then
        return revoke_replay()
    end

    local consumed_json, replacements = string.gsub(
        grace_json,
        '"consumed"%s*:%s*false',
        '"consumed":true',
        1)
    if replacements ~= 1 then
        return { 'MALFORMED_STATE' }
    end

    redis.call('SET', KEYS[2], consumed_json, 'KEEPTTL')
    return {
        'GRACE_RECOVERED',
        grace.successorEnvelope,
        current_json,
        grace_counter,
        grace_expiry
    }
end

local function rotate_family()
    if not preflight_fixed_types(true) then
        return { 'WRONG_TYPE' }
    end
    local current_json = redis.call('GET', KEYS[1])
    if not current_json then
        return { 'MISSING' }
    end
    local current, status = family_status(current_json, ARGV[3], ARGV[4])
    if status then
        if status == 'OWNER_MISMATCH' then
            return { 'MALFORMED_STATE' }
        end
        return { status }
    end

    if ARGV[11] ~= current.currentRefreshTokenHash then
        return recover_grace(current_json, current)
    end
    if current_json ~= ARGV[6] or decimal_field(current_json, 'rotationCounter') ~= ARGV[21] then
        return { 'MALFORMED_STATE' }
    end
    if tonumber(ARGV[5]) >= tonumber(ARGV[19]) or tonumber(ARGV[5]) >= tonumber(ARGV[20]) then
        delete_family(ARGV[2])
        expire_or_delete_indexes(nil)
        return { 'EXPIRED' }
    end

    local successor, _, successor_status = preflight_successor(current, ARGV[7], true)
    if successor_status then
        return { successor_status }
    end
    if tonumber(ARGV[8]) <= tonumber(ARGV[5]) or tonumber(ARGV[10]) <= tonumber(ARGV[5]) then
        return { 'MALFORMED_STATE' }
    end

    local indexes, index_status = inspect_indexes()
    if index_status then
        return { index_status }
    end
    local evicted = select_evictions(indexes, ARGV[13] == '1')
    if not evicted then
        return { 'MALFORMED_STATE' }
    end

    apply_cleanup(indexes, evicted)
    redis.call('SET', KEYS[1], ARGV[7], 'PXAT', ARGV[8])
    redis.call('SET', KEYS[2], ARGV[9], 'PXAT', ARGV[10])
    redis.call('ZADD', KEYS[3], ARGV[5], ARGV[2])
    if ARGV[13] == '1' then
        redis.call('ZADD', KEYS[4], ARGV[5], ARGV[2])
    else
        redis.call('ZREM', KEYS[4], ARGV[2])
    end
    write_versions()
    expire_or_delete_indexes(ARGV[18])
    return { 'ROTATED', ARGV[7] }
end

local function revoke_many(operation)
    if not preflight_fixed_types(false) then
        return { 'WRONG_TYPE' }
    end
    local indexes, status = inspect_indexes()
    if status then
        return { status }
    end
    if operation == 'REVOKE_OTHERS' and not indexes.valid[ARGV[23]] then
        return { 'MALFORMED_STATE' }
    end

    local targets = {}
    local seen = {}
    for _, sid in ipairs(indexes.base) do
        add_unique(targets, seen, sid)
    end
    for _, sid in ipairs(indexes.elevated) do
        add_unique(targets, seen, sid)
    end

    for _, sid in ipairs(targets) do
        if operation == 'REVOKE_ALL' or sid ~= ARGV[23] then
            if indexes.foreignMembers[sid] or indexes.malformedMembers[sid] then
                redis.call('ZREM', KEYS[3], sid)
                redis.call('ZREM', KEYS[4], sid)
            else
                delete_family(sid)
            end
        end
    end
    if operation == 'REVOKE_ALL' then
        redis.call('DEL', KEYS[3], KEYS[4])
    else
        expire_or_delete_indexes(nil)
    end
    return { 'REVOKED' }
end

local function revoke_one()
    if not preflight_fixed_types(true) then
        return { 'WRONG_TYPE' }
    end
    local json = redis.call('GET', KEYS[1])
    if json then
        local family, status = family_status(json, ARGV[3], '')
        if status == 'OWNER_MISMATCH' then
            return { 'MALFORMED_STATE' }
        end
        if status then
            return { status }
        end
        if family and family.userId ~= ARGV[3] then
            return { 'MALFORMED_STATE' }
        end
    end
    delete_family(ARGV[2])
    expire_or_delete_indexes(nil)
    return { 'REVOKED' }
end

local function revoke_presented()
    if not preflight_fixed_types(true) then
        return { 'WRONG_TYPE' }
    end

    local current_json = redis.call('GET', KEYS[1])
    if not current_json then
        return { 'NOT_REVOKED' }
    end

    local current, status = family_status(current_json, ARGV[3], ARGV[4])
    if status then
        if status == 'OWNER_MISMATCH' then
            return { 'MALFORMED_STATE' }
        end
        return { status }
    end

    if current.currentRefreshTokenHash == ARGV[11] then
        delete_family(ARGV[2])
        expire_or_delete_indexes(nil)
        return { 'REVOKED' }
    end

    local grace_json = redis.call('GET', KEYS[2])
    if not grace_json then
        return { 'NOT_REVOKED' }
    end

    local grace, grace_failure = grace_status(grace_json)
    if grace_failure then
        return { grace_failure }
    end

    local grace_counter = decimal_field(grace_json, 'successorRotationCounter')
    local current_counter = decimal_field(current_json, 'rotationCounter')
    local was_immediately_previous = grace.previousRefreshTokenHash == ARGV[11] and
        grace.successorRefreshTokenHash == current.currentRefreshTokenHash and
        grace_counter == current_counter
    if not was_immediately_previous then
        return { 'NOT_REVOKED' }
    end

    delete_family(ARGV[2])
    expire_or_delete_indexes(nil)
    return { 'REVOKED' }
end

if ARGV[1] == 'CREATE' then
    return create_family()
elseif ARGV[1] == 'ROTATE' then
    return rotate_family()
elseif ARGV[1] == 'REVOKE_PRESENTED' then
    return revoke_presented()
elseif ARGV[1] == 'REVOKE_ONE' then
    return revoke_one()
elseif ARGV[1] == 'REVOKE_OTHERS' or ARGV[1] == 'REVOKE_ALL' then
    return revoke_many(ARGV[1])
end

return { 'MALFORMED_STATE' }
