#!/bin/bash

DIR="data/news/inter"
T_DIR="data/news/topic"

bin/mallet import-file --input "$DIR/malletInput.txt" --output "$T_DIR/topic-input.mallet" --keep-sequence --stoplist-file "$DIR/stoplist.txt"
bin/mallet train-topics --input "$T_DIR/topic-input.mallet" --num-topics 200 --output-model "$T_DIR/model" --output-doc-topics "$T_DIR/doc_topics" --doc-topics-max 5 --output-topic-keys "$T_DIR/topic_keys" --num-top-words 10
echo "Job finished!"

