#!/bin/sh

# Voces disponibles en http://forja.guadalinex.org/frs/?group_id=21&release_id=120
#echo $1 | iconv -f utf-8 -t iso-8859-1 |text2wave -o output.wav  -eval "(JuntaDeAndalucia_es_sf_diphone)"
echo $1 | iconv -f utf-8 -t iso-8859-1 |text2wave -o /tmp/$2.wav  -eval "(voice_JuntaDeAndalucia_es_pa_diphone)"
ffmpeg -i /tmp/$2.wav $2
rm /tmp/$2.wav
