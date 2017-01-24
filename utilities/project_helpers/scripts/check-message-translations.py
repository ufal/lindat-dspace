#!/usr/bin/python
# -*- coding: utf-8 -*-

## USAGE EXAMPLE: python check_messsage_translations.sh cs

import sys
import os
import codecs

from check_message_lib import find_language_file_name

script_directory = os.path.dirname(os.path.realpath(__file__))
os.chdir(script_directory)

language1 = sys.argv[1]
language2 = sys.argv[2] if len(sys.argv) > 2 else 'en'

dspace_script = 'dspace-l10n-check.py'

file_name1 = find_language_file_name(language1)
file_name2 = find_language_file_name(language2)
os.system('python ' + dspace_script + ' ' + file_name1 + ' ' + file_name2)
