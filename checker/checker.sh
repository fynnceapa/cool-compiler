#!/bin/bash

SRC_PATH="../src"

setup()
{
    cp -r --no-clobber $SRC_PATH/* .
    make build
    make compiler
}

cleanup()
{
    make clean
}

run_tests()
{
    CLASSPATH=.:$(realpath ../antlr-4.13.0-complete.jar)

    passed=0
    for source_file in ./tests/*.cl; do
        echo -e "\n`basename $source_file`"
        java -cp $CLASSPATH cool.compiler.Compiler $source_file > ./tests/`basename $source_file .cl`.s
        if [ "$source_file" = "./tests/32-big.cl" ]; then
            echo 5 | spim -exception_file trap.handler.nogc -file tests/`basename $source_file .cl`.s > tests/`basename $source_file .cl`.out
        else
            spim -exception_file trap.handler.nogc -file tests/`basename $source_file .cl`.s > tests/`basename $source_file .cl`.out
        fi

        diff tests/`basename $source_file .cl`.ref tests/`basename $source_file .cl`.out

        if [ $? = 0 ]; then
            echo -e "Test passed!\n"
            passed=$(($passed + 1))
        else
            echo -e "Test failed!\n"
        fi
    done

    echo -e "\nTotal: $(( $(($passed * 100)) / 32))"
}

setup
if [ -z "$1" ] ; then
    run_tests
fi

cleanup
