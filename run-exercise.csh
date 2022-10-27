#!/bin/csh -f

sbt -v "runMain exercise.${argv}"
#sbt -v "test:runMain exercise.${argv}"
