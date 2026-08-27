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
-- INVENTORY reconciliation only:
-- 26 candidate count, then repeating groups of five values from ARGV 27:
-- sid, exact family JSON, last-used score ms, elevated (0/1), expired (0/1).

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

local function canonical_sid(value)
    if type(value) ~= 'string' or string.len(value) ~= 36 or string.lower(value) ~= value then
        return false
    end
    if string.sub(value, 9, 9) ~= '-' or string.sub(value, 14, 14) ~= '-' or
        string.sub(value, 19, 19) ~= '-' or string.sub(value, 24, 24) ~= '-' then
        return false
    end
    local compact = string.gsub(value, '-', '')
    return string.len(compact) == 32 and string.match(compact, '^[0-9a-f]+$') ~= nil
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

local function inspect_indexes(explicit_sid)
    if redis.call('ZCARD', KEYS[3]) > BASE_CAPACITY or
        redis.call('ZCARD', KEYS[4]) > ELEVATED_CAPACITY then
        return nil, 'MALFORMED_STATE'
    end

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
    if explicit_sid and explicit_sid ~= '' then
        add_unique(all_members, seen, explicit_sid)
    end

    local orphan_members = {}
    local malformed_members = {}
    local stale_elevated = {}
    local foreign_members = {}
    local valid = {}
    local valid_json = {}
    local base_member = {}
    local elevated_member = {}
    local scores = {}
    local corrupt_status = nil
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
        if not canonical_sid(sid) then
            return nil, 'MALFORMED_STATE'
        end

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
                corrupt_status = corrupt_status or 'MALFORMED_STATE'
            elseif status then
                malformed_members[sid] = true
                corrupt_status = corrupt_status or status
            else
                valid[sid] = family
                valid_json[sid] = json
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
            if scores[sid] then
                table.insert(ordered_base, { sid = sid, score = scores[sid] })
            end
            if not base_member[sid] and scores[sid] then
                missing_base[sid] = scores[sid]
            end
            if family_is_elevated(family) then
                if scores[sid] then
                    table.insert(ordered_elevated, { sid = sid, score = scores[sid] })
                end
                if not elevated_member[sid] and scores[sid] then
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

    local valid_count = 0
    for _, _ in pairs(valid) do
        valid_count = valid_count + 1
    end
    if valid_count > BASE_CAPACITY then
        return nil, 'MALFORMED_STATE'
    end

    return {
        all = all_members,
        base = base_members,
        elevated = elevated_members,
        valid = valid,
        validJson = valid_json,
        validCount = valid_count,
        corruptStatus = corrupt_status,
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

local function remove_memberships(sid)
    redis.call('ZREM', KEYS[3], sid)
    redis.call('ZREM', KEYS[4], sid)
end

local function has_entries(values)
    return next(values) ~= nil
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
    local has_explicit_current = ARGV[23] ~= ''
    if not preflight_fixed_types(has_explicit_current) then
        return { 'WRONG_TYPE' }
    end
    local explicit_sid = has_explicit_current and ARGV[23] or nil
    local indexes, status = inspect_indexes(explicit_sid)
    if status then
        return { status }
    end
    if indexes.corruptStatus then
        return { indexes.corruptStatus }
    end

    if operation == 'REVOKE_OTHERS' then
        local current_json = redis.call('GET', KEYS[1])
        if not current_json then
            return { 'MISSING' }
        end
        if current_json ~= ARGV[6] then
            return { 'CHANGED' }
        end
        local current, current_status = family_status(current_json, ARGV[3], '')
        if current_status then
            return { current_status == 'OWNER_MISMATCH' and 'MALFORMED_STATE' or current_status }
        end
        if not indexes.valid[ARGV[23]] or family_is_elevated(current) ~= (ARGV[13] == '1') then
            return { 'MALFORMED_STATE' }
        end
    end

    for _, sid in ipairs(indexes.all) do
        if operation == 'REVOKE_ALL' or sid ~= ARGV[23] then
            delete_family(sid)
        end
    end
    if operation == 'REVOKE_ALL' then
        redis.call('DEL', KEYS[3], KEYS[4])
    else
        redis.call('ZADD', KEYS[3], ARGV[5], ARGV[23])
        if ARGV[13] == '1' then
            redis.call('ZADD', KEYS[4], ARGV[5], ARGV[23])
        else
            redis.call('ZREM', KEYS[4], ARGV[23])
        end
        expire_or_delete_indexes(ARGV[18])
    end
    return { 'REVOKED' }
end

local function revoke_one()
    if not preflight_fixed_types(false) then
        return { 'WRONG_TYPE' }
    end
    local family_type = redis_type(KEYS[1])
    if family_type ~= 'none' and family_type ~= 'string' then
        return { 'WRONG_TYPE' }
    end
    local json = redis.call('GET', KEYS[1])
    if not json then
        remove_memberships(ARGV[2])
        expire_or_delete_indexes(nil)
        return { 'NOT_REVOKED' }
    end
    local family, status = family_status(json, ARGV[3], '')
    if status == 'OWNER_MISMATCH' then
        return { 'NOT_REVOKED' }
    end
    if status then
        return { status }
    end
    if not is_type(KEYS[2], 'string') then
        return { 'WRONG_TYPE' }
    end
    delete_family(ARGV[2])
    expire_or_delete_indexes(nil)
    return { 'REVOKED' }
end

local function inventory_snapshot()
    if not preflight_fixed_types(true) then
        return { 'WRONG_TYPE' }
    end
    local indexes, status = inspect_indexes(ARGV[2])
    if status then
        return { status }
    end
    if indexes.corruptStatus then
        return { indexes.corruptStatus }
    end

    for sid, _ in pairs(indexes.orphanMembers) do
        delete_family(sid)
    end
    expire_or_delete_indexes(nil)

    if not indexes.valid[ARGV[2]] then
        return { 'MISSING' }
    end

    local session_ids = {}
    for sid, _ in pairs(indexes.valid) do
        table.insert(session_ids, sid)
    end
    table.sort(session_ids)

    local result = { 'INVENTORY' }
    for _, sid in ipairs(session_ids) do
        table.insert(result, sid)
        table.insert(result, indexes.validJson[sid])
    end
    return result
end

local function reconcile_inventory()
    if not preflight_fixed_types(true) then
        return { 'WRONG_TYPE' }
    end
    local candidate_count = tonumber(ARGV[26])
    if not candidate_count or candidate_count < 1 or candidate_count > BASE_CAPACITY or
        candidate_count ~= math.floor(candidate_count) then
        return { 'MALFORMED_STATE' }
    end

    local expected = {}
    local ordered = {}
    local argument = 27
    for _ = 1, candidate_count do
        local sid = ARGV[argument]
        local json = ARGV[argument + 1]
        local score = tonumber(ARGV[argument + 2])
        local elevated = ARGV[argument + 3]
        local expired = ARGV[argument + 4]
        if not canonical_sid(sid) or expected[sid] or not json or json == '' or not score or
            (elevated ~= '0' and elevated ~= '1') or (expired ~= '0' and expired ~= '1') then
            return { 'MALFORMED_STATE' }
        end
        expected[sid] = {
            json = json,
            score = score,
            elevated = elevated == '1',
            expired = expired == '1'
        }
        table.insert(ordered, sid)
        argument = argument + 5
    end
    if not expected[ARGV[2]] then
        return { 'MALFORMED_STATE' }
    end

    local indexes, status = inspect_indexes(ARGV[2])
    if status then
        return { status }
    end
    if indexes.corruptStatus then
        return { indexes.corruptStatus }
    end
    if has_entries(indexes.orphanMembers) or indexes.validCount ~= candidate_count then
        return { 'CHANGED' }
    end

    for _, sid in ipairs(ordered) do
        local candidate = expected[sid]
        if indexes.validJson[sid] ~= candidate.json then
            return { 'CHANGED' }
        end
        local family = indexes.valid[sid]
        if not family or family_is_elevated(family) ~= candidate.elevated then
            return { 'MALFORMED_STATE' }
        end
    end
    for sid, _ in pairs(indexes.valid) do
        if not expected[sid] then
            return { 'CHANGED' }
        end
    end

    local current_expired = false
    for _, sid in ipairs(ordered) do
        local candidate = expected[sid]
        if candidate.expired then
            delete_family(sid)
            if sid == ARGV[2] then
                current_expired = true
            end
        else
            redis.call('ZADD', KEYS[3], candidate.score, sid)
            if candidate.elevated then
                redis.call('ZADD', KEYS[4], candidate.score, sid)
            else
                redis.call('ZREM', KEYS[4], sid)
            end
        end
    end
    expire_or_delete_indexes(ARGV[18])
    if current_expired then
        return { 'CURRENT_EXPIRED' }
    end
    return { 'RECONCILED' }
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
elseif ARGV[1] == 'INVENTORY' then
    return inventory_snapshot()
elseif ARGV[1] == 'RECONCILE_INVENTORY' then
    return reconcile_inventory()
end

return { 'MALFORMED_STATE' }
