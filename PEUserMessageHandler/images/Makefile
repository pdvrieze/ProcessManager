
RESOLUTION=90

all: ../war/images/startNode.png ../war/images/endNode.png ../war/images/arrow.png ../src/nl/adaptivity/process/userMessageHandler/client/blackSplitPanel.png ../war/images/diagramBackground.png

../war/images/%.png: %.svg
	@inkscape -d ${RESOLUTION} -e $@ -f $<

../src/nl/adaptivity/process/userMessageHandler/client/%.png: %.svg
	@inkscape -d ${RESOLUTION} -e $@ -f $<
