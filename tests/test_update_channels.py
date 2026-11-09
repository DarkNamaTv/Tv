import io
import unittest
import zipfile
from scripts.update_channels import build


def zipped(files):
    data = io.BytesIO()
    with zipfile.ZipFile(data, 'w') as z:
        for name, content in files.items():
            z.writestr(name, content)
    data.seek(0)
    return zipfile.ZipFile(data)


class CatalogueTest(unittest.TestCase):
    def test_merges_streams_and_filters_channels(self):
        db = zipped({
            'database-master/data/channels.csv': 'id,name,country,is_nsfw,closed,categories\nOne.ir,One,IR,FALSE,,music;movies\nTwo.us,Two,US,FALSE,,news\nAdult.ir,Adult,IR,TRUE,,\n',
            'database-master/data/feeds.csv': 'channel,languages\nTwo.us,eng;fas\n',
            'database-master/data/logos.csv': 'channel,in_use,url\nOne.ir,TRUE,https://example.com/logo.png\n',
        })
        streams = zipped({'iptv-master/streams/ir.m3u': '''#EXTM3U
#EXTINF:-1 tvg-id="One.ir@HD",One
https://example.com/one.m3u8
#EXTINF:-1 tvg-id="One.ir@SD",One
https://example.com/two.m3u8
#EXTINF:-1 tvg-id="One.ir@HD",One
https://example.com/one.m3u8
#EXTINF:-1 tvg-id="Two.us",Two
https://example.com/other.m3u8
#EXTINF:-1 tvg-id="Adult.ir",Adult
https://example.com/adult.m3u8
'''})
        result = build(db, streams)
        self.assertEqual([r['name'] for r in result], ['One', 'Two'])
        self.assertEqual(len(result[0]['urls']), 2)
        self.assertEqual(result[0]['logo'], 'https://example.com/logo.png')
        self.assertEqual(result[0]['categories'], ['music', 'movies'])

    def test_empty_does_not_replace_catalogue(self):
        db = zipped({
            'database-master/data/channels.csv': 'id,name,country,is_nsfw,closed\n',
            'database-master/data/feeds.csv': 'channel,languages\n',
            'database-master/data/logos.csv': 'channel,in_use,url\n',
        })
        with self.assertRaises(ValueError):
            build(db, zipped({}))

if __name__ == '__main__': unittest.main()
