#!/usr/bin/python
# -*- coding: utf-8 -*-

## USAGE EXAMPLE: python check_messsage_translations.sh cs

import sys
import os
import codecs
import re

from check_message_lib import find_language_file_name

script_directory = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_directory)

language1 = sys.argv[1]
language2 = sys.argv[2] if len(sys.argv) > 2 else 'en'

dspace_script = 'dspace-l10n-check.py'

xml_file_name1 = find_language_file_name(language1, 'xml')
xml_file_name2 = find_language_file_name(language2, 'xml')
os.system('python ' + dspace_script + ' ' + xml_file_name1 + ' ' + xml_file_name2)

js_key_regexp = r'^\s*["\']([\w-]+?)["\']\s*:'
def find_js_keys(js_file_name):
    js_file = codecs.open(js_file_name, 'r', 'UTF-8')
    keys = set()
    for line in js_file:
        match = re.search(js_key_regexp, line.strip(), re.U)
        if (match):
            keys.add(match.group(1))
    return keys

js_file_name1 = find_language_file_name(language1, 'js')
js_keys1 = find_js_keys(js_file_name1)
js_file_name2 = find_language_file_name(language2, 'js')
js_keys2 = find_js_keys(js_file_name2)

print '\nPresent in ' + js_file_name2 + ' but missing in ' + js_file_name1 + ':'
for key in (js_keys2 - js_keys1):
    print key

print '\nPresent in ' + js_file_name1 + ' but missing in ' + js_file_name2 + ':'
for key in (js_keys1 - js_keys2):
    print key

