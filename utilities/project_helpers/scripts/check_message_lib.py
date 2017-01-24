#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import codecs

script_directory = os.path.dirname(os.path.realpath(__file__))
root_directory = script_directory + '/../../..'
i18n_directory = root_directory + '/dspace-xmlui/src/main/webapp/i18n'
en_joint_file_name = '/tmp/messages-en.xml'

def find_language_file_name(language):
    if (language != 'en'):
        file_name = i18n_directory + '/messages_' + language + '.xml'
    else:
        file_name = en_joint_file_name
        _create_en_joint_file()
    return file_name

## Merge together all messages.xml into one temporary messages-en.xml.
## Avoids xml parsing to prevent namespace complications.
def _create_en_joint_file():
    en_file_names = set()
    for (dpath, dnames, fnames) in os.walk(root_directory):
        for fname in [os.path.join(dpath, fname) for fname in fnames]:
            if (fname.endswith('/messages.xml')):
                en_file_names.add(fname)
    print 'Constructing temporary /tmp/messages_en.xml from all messages.xml:\n  ' + '\n  '.join(en_file_names) + '\n'
    en_joint_file = codecs.open(en_joint_file_name, 'w', 'UTF-8')
    for (index, en_file_name) in enumerate(en_file_names):
        en_file = codecs.open(en_file_name, 'r', 'UTF-8')
        if (index == 0):
            for line in en_file:
                if ('</catalogue>' not in line):
                    en_joint_file.write(line)
        else:
            inside_catalogue_flag = False
            for line in en_file:
                if (inside_catalogue_flag):
                    if ('</catalogue>' in line):
                        inside_catalogue_flag = False
                    else:
                        en_joint_file.write(line)
                else:
                    if ('<catalogue' in line):
                        inside_catalogue_flag = True
        en_file.close()
    en_joint_file.write('</catalogue>\n')
    en_joint_file.close()
