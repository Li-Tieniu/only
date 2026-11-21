"""Local HTTP acceptance only: fixed loopback ports and isolated only_acceptance_test schema.
Requires ONLY_ACCEPTANCE=1. Never point these services at a business database.
Reports contain case names/statuses, never passwords or tokens. No third-party dependencies.
"""
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import HTTPError

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'target/phase6'
MYSQL = os.environ.get('ONLY_MYSQL_BIN', 'mysql')
MODE = sys.argv[1] if len(sys.argv) > 1 else 'normal'
assert os.environ.get('ONLY_ACCEPTANCE') == '1', 'Explicit isolated-test opt-in required'
assert MODE in ('normal', 'redis-down', 'redis-restored')
cases = []

def sql(statement):
    cmd = [MYSQL, '--no-defaults', '--protocol=TCP', '-h127.0.0.1', '-P13317', '-uroot',
           '--default-character-set=utf8mb4', '--batch', '--raw', '--skip-column-names', 'only_acceptance_test']
    return subprocess.check_output(cmd, input=statement.encode('utf8')).decode('utf8').strip()

assert 'target/phase6/mysql' in sql('SELECT @@datadir').replace('\\', '/').lower(), 'Wrong MySQL instance'

def check(name, condition):
    cases.append({'case': name, 'passed': bool(condition)})
    if not condition:
        save()
        raise AssertionError(name)

def save():
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f'http-{MODE}.json').write_text(json.dumps(cases, ensure_ascii=False, indent=2), encoding='utf8')

def call(name, path, data=None, token=None, expected=200, admin=False):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    req = Request(('http://127.0.0.1:8081' if admin else 'http://127.0.0.1:8080') + path,
                  data=None if data is None else json.dumps(data).encode(), headers=headers)
    try:
        with urlopen(req, timeout=12) as response: status, raw = response.status, response.read()
    except HTTPError as error: status, raw = error.code, error.read()
    body = json.loads(raw)
    check(name, status == expected and body['code'] == expected)
    return body['data']

def redis(*parts):
    with socket.create_connection(('127.0.0.1', 16389), timeout=2) as conn:
        wire = b'*%d\r\n' % len(parts)
        for part in parts:
            item = str(part).encode()
            wire += b'$%d\r\n' % len(item) + item + b'\r\n'
        conn.sendall(wire)
        stream = conn.makefile('rb')
        line = stream.readline()
        if line[:1] == b'$':
            count = int(line[1:]); return None if count < 0 else stream.read(count).decode()
        if line[:1] == b':': return int(line[1:])
        return line[1:].decode().strip()

def add(admin, name='AcceptanceGame', price='12.34'):
    return call('admin adds product', '/gametrading/addgame', {
        'gameName': name, 'price': price, 'coverImage': '/favicon.ico',
        'wallImages': '/favicon.ico', 'details': 'Isolated acceptance fixture'}, admin, admin=True)

def state(item):
    return sql(f'SELECT sale_status FROM game_trading WHERE item_id={int(item)}')

def create(token, item, expected=200):
    return call('create order', '/order/create', {'itemId': item, 'userId': 999999}, token, expected)

def transition(token, order, action, expected=200):
    return call(action, '/order/' + order['orderNo'] + '/' + action, {}, token, expected)

if MODE == 'normal':
    stamp = str(int(time.time()))
    credentials = {'username': 'accept_admin_' + stamp, 'password': 'Phase6-local-test!'}
    admin_id = call('register admin candidate', '/user/register', credentials)
    sql(f"UPDATE sys_user SET role='ADMIN' WHERE id={int(admin_id)}")
    admin = call('admin login', '/user/login', credentials)['token']
    user_data = {'username': 'accept_user_' + stamp, 'password': ' spaced-local-password '}
    user_id = call('register whitespace password', '/user/register', user_data)
    call('duplicate registration rejected', '/user/register', user_data, expected=400)
    user = call('unchanged whitespace password login', '/user/login', user_data)['token']
    call('incorrect password', '/user/login', dict(user_data, password='wrong'), expected=401)
    call('bcrypt byte limit', '/user/register', {'username': 'bytes_' + stamp, 'password': '密' * 25}, expected=400)
    info = call('current user', '/user/info', token=user)
    check('no password in user response', info['id'] == user_id and 'password' not in info)
    for route in ('getinfoproduct', 'getinfodetails'):
        call('anonymous admin read denied', '/gametrading/' + route, expected=401, admin=True)
        call('user admin read denied', '/gametrading/' + route, token=user, expected=403, admin=True)
        call('admin read allowed', '/gametrading/' + route, token=admin, admin=True)
    call('user admin write denied', '/gametrading/addgame', {}, user, 403, True)
    call('anonymous order denied', '/order/my', expected=401)
    call('malformed JWT denied', '/order/my', token='not-a-token', expected=401)
    secret = json.loads((OUT / 'runtime.json').read_text())['JWT_SECRET']
    encode = lambda obj: base64.urlsafe_b64encode(json.dumps(obj).encode()).rstrip(b'=')
    unsigned = encode({'alg': 'HS256'}) + b'.' + encode({'sub': str(user_id), 'exp': int(time.time()) - 60})
    expired = (unsigned + b'.' + base64.urlsafe_b64encode(hmac.new(secret.encode(), unsigned, hashlib.sha256).digest()).rstrip(b'=')).decode()
    call('expired signed JWT denied', '/order/my', token=expired, expected=401)
    sql(f'UPDATE sys_user SET status=0 WHERE id={int(user_id)}')
    try:
        call('disabled user old JWT denied', '/order/my', token=user, expected=401)
        call('disabled user login denied', '/user/login', user_data, expected=403)
    finally: sql(f'UPDATE sys_user SET status=1 WHERE id={int(user_id)}')
    item = add(admin)
    call('partial edit', '/gametrading/updategame', {'itemId': item, 'details': 'edited'}, admin, admin=True)
    detail = call('detail after omitted price edit', f'/gametrading/details/{item}')
    check('omitted price unchanged', detail['price'] == 12.34)
    for price in ('-1', '1.001', '100000000', 'invalid'):
        call('invalid amount ' + price, '/gametrading/updategame', {'itemId': item, 'price': price}, admin, 400, True)
    call('invalid page', '/gametrading/details?page=0', expected=400)
    call('invalid sort', '/gametrading/product?sort=price;drop', expected=400)
    call('SQL metacharacters safely bound', '/gametrading/product?keyword=%27%20OR%201%3D1--')
    page = call('search price filter paging', '/gametrading/details?keyword=AcceptanceGame&min_price=12&max_price=13&sort=price_asc&page_size=1')
    check('filtered page contents', page['total'] == 1 and page['items'][0]['itemId'] == item)
    first = create(user, item)
    check('request cannot impersonate user', sql(f"SELECT user_id FROM game_order WHERE id={int(first['id'])}") == str(user_id) and state(item) == 'LOCKED')
    create(admin, item, 409)
    call('locked product hidden', f'/gametrading/details/{item}', expected=409)
    rows = call('locked product cannot edit', '/gametrading/updategame', {'itemId': item, 'price': '1'}, admin, admin=True)
    check('locked edit affects zero rows', rows == 0)
    call('other user cannot inspect order', '/order/' + first['orderNo'], token=admin, expected=403)
    transition(admin, first, 'cancel', 403)
    transition(user, first, 'cancel')
    check('cancel releases product', state(item) == 'ON_SALE')
    transition(user, first, 'cancel', 409)
    transition(user, first, 'pay', 400)
    call('resale price edit', '/gametrading/updategame', {'itemId': item, 'price': '20.00'}, admin, admin=True)
    historic = call('historic order snapshot', '/order/' + first['orderNo'], token=user)
    check('historic amount unchanged', historic['price'] == 12.34)
    second = create(admin, item)
    check('new price snapshot', second['price'] == 20)
    transition(admin, second, 'pay')
    check('payment sold product', state(item) == 'SOLD')
    transition(admin, second, 'cancel', 409)
    create(user, item, 409)
    my = call('order list user isolation', '/order/my', token=user)
    check('only owned orders returned', [o['orderNo'] for o in my['items']] == [first['orderNo']])
    disposable = add(admin, 'AcceptanceDelete')
    call('soft delete', '/gametrading/softdeletegame', {'itemId': disposable}, admin, admin=True)
    call('deleted product hidden', f'/gametrading/details/{disposable}', expected=404)
    check('soft deleted record retained', sql(f'SELECT is_deleted FROM game_trading WHERE item_id={disposable}') == '1')
    call('name first query', '/gametrading/names?keyword=AcceptanceGame')
    check('Redis name key exists with TTL', 0 < redis('TTL', 'only:catalog:names:v1::AcceptanceGame') <= 600)
    before = redis('INFO', 'stats')
    call('name repeat query', '/gametrading/names?keyword=AcceptanceGame')
    after = redis('INFO', 'stats')
    hits = lambda info: int(next(x.split(':')[1] for x in info.splitlines() if x.startswith('keyspace_hits:')))
    check('repeat query increases Redis hits', hits(after) > hits(before))
    other_names = call('name keyword isolation', '/gametrading/names?keyword=NoSuchName')
    check('different name result', other_names['items'] == [])
    add(admin, 'AcceptanceCacheNew')
    check('committed product change evicts name key', redis('EXISTS', 'only:catalog:names:v1::AcceptanceGame') == 0)
    (OUT / 'http-session.json').write_text(json.dumps({'admin': admin, 'user': user}), encoding='utf8')
else:
    session = json.loads((OUT / 'http-session.json').read_text())
    admin, user = session['admin'], session['user']
    call('names during ' + MODE, '/gametrading/names?keyword=AcceptanceGame')
    if MODE == 'redis-down':
        item = add(admin, 'AcceptanceOffline')
        first = create(user, item)
        create(admin, item, 409)
        transition(user, first, 'cancel')
        second = create(admin, item)
        transition(admin, second, 'pay')
        check('Redis outage preserves final SOLD', state(item) == 'SOLD')
    else:
        check('Redis writes restored', redis('EXISTS', 'only:catalog:names:v1::AcceptanceGame') == 1)
        before = redis('INFO', 'stats')
        call('Redis repeat after recovery', '/gametrading/names?keyword=AcceptanceGame')
        after = redis('INFO', 'stats')
        hits = lambda info: int(next(x.split(':')[1] for x in info.splitlines() if x.startswith('keyspace_hits:')))
        check('Redis cache hits restored', hits(after) > hits(before))

save()
print(f'{MODE}: {len(cases)} checks passed, 0 failed')
