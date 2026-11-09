#!/usr/bin/env python3
"""Build a reproducible Persian-language catalogue from iptv-org's public data."""
import csv, io, json, re, urllib.request, zipfile
from collections import defaultdict
from pathlib import Path

def archive(repo):
    url = f'https://codeload.github.com/iptv-org/{repo}/zip/refs/heads/master'
    with urllib.request.urlopen(url, timeout=90) as response:
        return zipfile.ZipFile(io.BytesIO(response.read()))

def rows(z, name):
    return list(csv.DictReader(io.TextIOWrapper(z.open('database-master/data/' + name + '.csv'), encoding='utf-8')))

def build(db, playlists):
    channels = {r['id']: r for r in rows(db, 'channels') if r['is_nsfw'] != 'TRUE' and not r['closed']}
    persian = {r['channel'] for r in rows(db, 'feeds') if 'fas' in r['languages'].split(';')}
    eligible = {k for k, v in channels.items() if v['country'] == 'IR' or k in persian}
    logos = {}
    for r in rows(db, 'logos'):
        if r['channel'] in eligible and r['in_use'] == 'TRUE' and r['url'].startswith('https://'):
            logos.setdefault(r['channel'], r['url'])
    streams = defaultdict(list)
    for filename in playlists.namelist():
        if not filename.startswith('iptv-master/streams/') or not filename.endswith('.m3u'):
            continue
        channel = None
        for line in playlists.read(filename).decode('utf-8-sig').splitlines():
            line = line.strip()
            if line.startswith('#EXTINF:'):
                match = re.search(r'tvg-id="([^"@]+)', line)
                channel = match.group(1) if match else None
            elif line and not line.startswith('#'):
                if channel in eligible and line.startswith(('http://', 'https://')) and line not in streams[channel]:
                    streams[channel].append(line)
                channel = None
    result = []
    for key in sorted(streams):
        c = channels[key]
        result.append({'id': key, 'name': c['name'], 'logo': logos.get(key, ''),
                       'categories': [v for v in c.get('categories', '').split(';') if v],
                       'urls': streams[key]})
    if not result:
        raise ValueError('No channels found; refusing to replace the catalogue')
    return result

if __name__ == '__main__':
    result = build(archive('database'), archive('iptv'))
    path = Path('app/src/main/assets/channels.json')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'Wrote {len(result)} channels to {path}')
