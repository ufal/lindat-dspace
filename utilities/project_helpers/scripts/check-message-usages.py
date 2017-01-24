#!/usr/bin/python

import sys
import subprocess
import os
import re
import xml.etree.ElementTree as xml

from check_message_lib import find_language_file_name, root_directory

language = sys.argv[1]

grep_command = 'grep -R "[>\'\\"]xmlui\\." --include=*.java --include=*.xsl --include=*.xmap --include=*.xslt'

os.chdir(root_directory)
output = subprocess.check_output(grep_command, shell=True)
output_lines = output.strip().split('\n')

message_prefixes = set()
for line in output_lines:
    match = re.search("^([^:]+):.*[>'\"](xmlui\.[^<'\"]+)(?:[<'\"]|$)", line, re.U)
    if (match):
        message_prefixes.add((match.group(2), match.group(1)))

message_prefixes = sorted(message_prefixes, key=lambda x: -len(x[0]))

file_name = find_language_file_name(language)
results = {'yes':[], 'no':[], 'partial':[]}
root = xml.parse(file_name)
messages = root.findall('*')
print 'Checking message usage for the ' + str(len(messages)) + ' messages in ' + file_name + ' ...'
for message in root.findall('*'):
    key = message.get('key')
    found_file_name = None
    for (prefix, file_name) in message_prefixes:
        if (key == prefix):
            results['yes'].append((key, file_name))
            found_file_name = file_name
            break
        elif (key.startswith(prefix)):
            results['partial'].append((key, prefix, file_name))
            found_file_name = file_name
            break
    if (found_file_name is None):
        results['no'].append(key)
    ## hmm, what about messages not starting with "xmlui", like "input-forms", etc ...

print ''
print 'Message keys with no match (' + str(len(results['no'])) + '):'
for key in sorted(results['no']):
    print '  ' + key

print ''
print 'Message keys with partial match (' + str(len(results['partial'])) + '):'
for (key, prefix, file_name) in sorted(results['partial'], key=lambda x: x[0]):
    print '  ' + key + '  (' + prefix + ')  [' + file_name + ']'

print ''
print 'Message keys with full match (' + str(len(results['yes'])) + '):'
for (key, file_name) in sorted(results['yes'], key=lambda x: x[0]):
    print '  ' + key + '  [' + file_name + ']'
