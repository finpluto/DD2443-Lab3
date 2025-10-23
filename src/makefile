SRC = ${wildcard *.java}
CLASSES = ${patsubst %.java,%.class,${SRC}}

.PHONY: all
all: ${CLASSES}

%.class: %.java
	javac $<

.PHONY: clean
clean:
	@rm *.class
