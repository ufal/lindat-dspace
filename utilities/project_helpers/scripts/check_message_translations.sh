#!/bin/bash

language=$1

script_dir=`dirname ${BASH_SOURCE[0]}`
cd $script_dir
xmlui_dir=../../../dspace-xmlui
messages_dir=$xmlui_dir/src/main/webapp/i18n
echo $messages_dir
python dspace-l10n-check.py ${messages_dir}/messages.xml ${messages_dir}/messages_${language}.xml
