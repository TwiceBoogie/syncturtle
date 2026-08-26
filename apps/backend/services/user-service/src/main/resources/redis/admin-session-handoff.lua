-- KEYS: 1 handoff receipt
-- ARGV claim:   1 CLAIM, 2 code hash, 3 pre-auth binding hash,
--               4 client binding hash, 5 application now ms, 6 claim id
-- ARGV release: 1 RELEASE, 2 claim id
-- ARGV consume: 1 CONSUME, 2 claim id

local function redis_type(key)
    return redis.call('TYPE', key).ok
end

local function decimal_field(json, field)
    return string.match(json, '"' .. field .. '"%s*:%s*(%d+)%s*[,}]')
end

local function is_sha256(value)
    return type(value) == 'string' and string.len(value) == 64 and
        string.match(value, '^[0-9a-f]+$') ~= nil
end

local function is_canonical_uuid(value)
    return type(value) == 'string' and
        string.match(value,
            '^[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]%-' ..
            '[0-9a-f][0-9a-f][0-9a-f][0-9a-f]%-' ..
            '[0-9a-f][0-9a-f][0-9a-f][0-9a-f]%-' ..
            '[0-9a-f][0-9a-f][0-9a-f][0-9a-f]%-' ..
            '[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]' ..
            '[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]$') ~= nil
end

local function decode_record(raw)
    local ok, record = pcall(cjson.decode, raw)
    if not ok or type(record) ~= 'table' then
        return nil, 'MALFORMED'
    end

    if type(record.recordVersion) ~= 'number' then
        return nil, 'MALFORMED'
    end
    local raw_version = decimal_field(raw, 'recordVersion')
    if not raw_version then
        return nil, 'MALFORMED'
    end
    if raw_version ~= '1' then
        return nil, 'UNSUPPORTED_VERSION'
    end

    local allowed = {
        recordVersion = true,
        codeHash = true,
        userId = true,
        instanceId = true,
        userAuthVersion = true,
        adminSessionVersion = true,
        preAuthBindingHash = true,
        clientBindingHash = true,
        issuedAtEpochMilli = true,
        expiresAtEpochMilli = true,
        state = true,
        claimId = true
    }
    for field, _ in pairs(record) do
        if not allowed[field] then
            return nil, 'MALFORMED'
        end
    end

    if not is_sha256(record.codeHash) or
        not is_canonical_uuid(record.userId) or
        not is_canonical_uuid(record.instanceId) or
        not is_sha256(record.preAuthBindingHash) or
        not is_sha256(record.clientBindingHash) then
        return nil, 'MALFORMED'
    end

    local user_version = decimal_field(raw, 'userAuthVersion')
    local admin_version = decimal_field(raw, 'adminSessionVersion')
    local issued_at = decimal_field(raw, 'issuedAtEpochMilli')
    local expires_at = decimal_field(raw, 'expiresAtEpochMilli')
    if not user_version or not admin_version or not issued_at or not expires_at then
        return nil, 'MALFORMED'
    end
    if type(record.userAuthVersion) ~= 'number' or
        type(record.adminSessionVersion) ~= 'number' or
        type(record.issuedAtEpochMilli) ~= 'number' or
        type(record.expiresAtEpochMilli) ~= 'number' then
        return nil, 'MALFORMED'
    end

    local issued_number = tonumber(issued_at)
    local expires_number = tonumber(expires_at)
    if issued_number < 0 or expires_number <= issued_number or
        expires_number - issued_number > 30000 then
        return nil, 'MALFORMED'
    end

    if record.state == 'PENDING' then
        if record.claimId ~= cjson.null then
            return nil, 'MALFORMED'
        end
    elseif record.state == 'CLAIMED' then
        if not is_canonical_uuid(record.claimId) then
            return nil, 'MALFORMED'
        end
    else
        return nil, 'MALFORMED'
    end

    return record, nil
end

if redis_type(KEYS[1]) == 'none' then
    return { 'MISSING' }
end
if redis_type(KEYS[1]) ~= 'string' then
    return { 'WRONG_TYPE' }
end

local raw = redis.call('GET', KEYS[1])
if not raw then
    return { 'MISSING' }
end

local record, failure = decode_record(raw)
if failure then
    return { failure }
end

local operation = ARGV[1]
if operation == 'CLAIM' then
    local now = tonumber(ARGV[5])
    if not now or not is_canonical_uuid(ARGV[6]) then
        return { 'MALFORMED' }
    end
    if now >= record.expiresAtEpochMilli then
        redis.call('DEL', KEYS[1])
        return { 'EXPIRED' }
    end
    if record.state ~= 'PENDING' or record.claimId ~= cjson.null then
        return { 'ALREADY_CLAIMED' }
    end
    if record.codeHash ~= ARGV[2] then
        return { 'INVALID' }
    end
    if record.preAuthBindingHash ~= ARGV[3] or record.clientBindingHash ~= ARGV[4] then
        return { 'BINDING_MISMATCH' }
    end

    local ttl = redis.call('PTTL', KEYS[1])
    if ttl <= 0 then
        redis.call('DEL', KEYS[1])
        return { 'EXPIRED' }
    end
    record.state = 'CLAIMED'
    record.claimId = ARGV[6]
    local updated = cjson.encode(record)
    redis.call('SET', KEYS[1], updated, 'PX', ttl)
    return { 'CLAIMED', updated }
end

if operation == 'RELEASE' then
    if record.state ~= 'CLAIMED' or record.claimId ~= ARGV[2] then
        return { 'CLAIM_MISMATCH' }
    end

    local ttl = redis.call('PTTL', KEYS[1])
    if ttl <= 0 then
        redis.call('DEL', KEYS[1])
        return { 'EXPIRED' }
    end
    record.state = 'PENDING'
    record.claimId = cjson.null
    redis.call('SET', KEYS[1], cjson.encode(record), 'PX', ttl)
    return { 'RELEASED' }
end

if operation == 'CONSUME' then
    if record.state ~= 'CLAIMED' or record.claimId ~= ARGV[2] then
        return { 'CLAIM_MISMATCH' }
    end
    redis.call('DEL', KEYS[1])
    return { 'CONSUMED' }
end

return { 'MALFORMED' }
