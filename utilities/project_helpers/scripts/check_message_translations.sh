#!/bin/bash

source_dir=$1
language=$2

messages_dir=${source_dir}/dspace-xmlui/src/main/webapp/i18n

python dspace-l10n-check.py ${messages_dir}/messages.xml ${messages_dir}/messages_${language}.xml
