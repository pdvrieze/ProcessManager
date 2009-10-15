
RESOLUTION=90

all: startNode.png endNode.png

%.png: %.svg
	inkscape -d ${RESOLUTION} -e $@ -f $<
