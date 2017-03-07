#!/usr/bin/python

import sys
import subprocess
import codecs
import os
import re
import xml.etree.ElementTree as xml

from check_message_lib import find_language_file_name, root_directory

language = sys.argv[1]

line_regexp = r'^(.+?):(.*)$'

def find_xml_prefixes_and_files():

    prefixes = ['xmlui', 'homepage', 'input_forms', 'org.dspace', 'PiwikStatisticsTransformer', 'UFAL.firstpage']
    grep_command = 'grep -R -P "[>\'\\"](' + '|'.join(prefixes) + ')\\." --include=*.java --include=*.xsl --include=*.xmap --include=*.xslt --include=input-forms.xml --exclude-dir=*/target/* *'
    prefix_regexp = "[>'\"]((?:" + "|".join(prefixes) + ")\..+?)[<'\"]"

    os.chdir(root_directory)
    with open(os.devnull, 'w') as devnull:
        output = subprocess.check_output(grep_command, shell=True, stderr=devnull)
    output_lines = output.strip().split('\n')
    message_prefixes = set()
    for grep_line in output_lines:
        line_match = re.search(line_regexp, grep_line, re.U)
        (file_name, line) = line_match.groups()
        match_tuples = re.findall(prefix_regexp, line, re.U)
        for match_tuple in match_tuples:
            prefix = match_tuple
            message_prefixes.add((prefix, file_name))

    return sorted(message_prefixes, key=lambda x: -len(x[0]))

def add_xml_results(language, results):
    message_prefixes = find_xml_prefixes_and_files()
    file_name = find_language_file_name(language, 'xml')
    root = xml.parse(file_name)
    messages = root.findall('*')
    print 'Checking message usage for the ' + str(len(messages)) + ' messages in ' + file_name + ' ...'
    for message in messages:
        key = message.get('key')
        result = {'type':'xml', 'match':'no', 'key':key, 'file_name':None, 'prefix':None}
        for (prefix, file_name) in message_prefixes:
            if (key == prefix):
                result['match'] = 'full'
                result['file_name'] = file_name
                break
            elif (key.startswith(prefix)):
                result['match'] = 'partial'
                result['file_name'] = file_name
                result['prefix'] = prefix
                break
        results.append(result)

def add_js_results(language, results):
    message_file_name = find_language_file_name(language, 'js')
    message_file = codecs.open(message_file_name, 'r', 'UTF-8')
    message_regexp = '^"(.*?)": ".*?",?$'
    in_messages_flag = False
    for line in message_file:
        message_match = re.search(message_regexp, line.strip(), re.U)
        if (message_match):
            key = message_match.group(1)
            result = {'type':'js', 'match':'no', 'key':key, 'file_name':None, 'prefix':None}
            grep_command = 'grep -R -P "(\\\\$|jQuery)\\.i18n\._\\([\'\\"]' + key + '[\'\\"][),]" --include=*.js --include=*.html --exclude-dir=*/target/* *'
            os.chdir(root_directory)
            try:
                with open(os.devnull, 'w') as devnull:
                    output = subprocess.check_output(grep_command, shell=True, stderr=devnull)
                output_lines = output.strip().split('\n')
                line_match = re.search(line_regexp, output_lines[0], re.U)
                (file_name, line) = line_match.groups()
                result['match'] = 'full'
                result['file_name'] = file_name
            except subprocess.CalledProcessError as e:
                if e.returncode > 1:
                    raise
            results.append(result)

results = []
add_xml_results(language, results)
add_js_results(language, results)

results_xml_no = [result for result in results if result['type'] == 'xml' and result['match'] == 'no']
results_xml_partial = [result for result in results if result['type'] == 'xml' and result['match'] == 'partial']
results_xml_full = [result for result in results if result['type'] == 'xml' and result['match'] == 'full']

results_js_no = [result for result in results if result['type'] == 'js' and result['match'] == 'no']
results_js_full = [result for result in results if result['type'] == 'js' and result['match'] == 'full']

print ''
print 'XML message keys with no match (' + str(len(results_xml_no)) + '):'
for result in sorted(results_xml_no, key=lambda x: x['key']):
    print '  ' + result['key']
    
print ''
print 'XML message keys with partial match (' + str(len(results_xml_partial)) + '):'
for result in sorted(results_xml_partial, key=lambda x: x['key']):
    print '  ' + result['key'] + '  (' + result['prefix'] + ')  [' + result['file_name'] + ']'

print ''
print 'XML message keys with full match (' + str(len(results_xml_full)) + '):'
for result in sorted(results_xml_full, key=lambda x: x['key']):
    print '  ' + result['key'] + '  [' + result['file_name'] + ']'

print ''
print 'JS message keys with no match (' + str(len(results_js_no)) + '):'
for result in sorted(results_js_no, key=lambda x: x['key']):
    print '  ' + result['key']
    
print ''
print 'JS message keys with match (' + str(len(results_js_full)) + '):'
for result in sorted(results_js_full, key=lambda x: x['key']):
    print '  ' + result['key'] + '  [' + result['file_name'] + ']'
