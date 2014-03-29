
.PHONY: res

QUALS:= mdpi hdpi xhdpi xxhdpi
NODPITARGETS:=ic_launcher-web.png # promo.png feature.png
COLOURS:=red blue green yellow white purple black orange gray question
SCORES:=black white none
#DRAWABLES:=$(foreach peg,peg btn,$(foreach colour,$(COLOURS),$(peg)_$(colour).xml)) \
#           $(foreach score,$(SCORES),score_$(score).png) \
#	   ic_launcher.png ic_menu_scores.png ic_action_scores.png popupframe.9.png popuparrowdown.png popuparrowup.png
ICONS=ic_cont_arrow.png ic_cont_edit.png ic_cont_delete.png
DRAWABLES:= ic_launcher.png $(ICONS) btn_default.9.png btn_pressed.9.png btn_active.9.png
TARGETS:= $(foreach qual,$(QUALS),$(foreach drawable,$(DRAWABLES),res/drawable-$(qual)/$(drawable))) $(NODPITARGETS) 

res: $(TARGETS)

.PRECIOUS:: %.png

dp2px=$(shell scripts/dp2px.sh $(1) $(2))

ressrc/iconsource.png: ressrc/iconsource.svg
	@eval inkscape -h 512 -e $@ -j -i icon $<
	@optipng $@

$(foreach qual,$(QUALS),res/drawable-$(qual)/ic_launcher.png): ressrc/iconsource.svg
	@eval scripts/iconize.sh 48 48 $< $@
#	@QUAL=$(subst res/drawable-,,$(subst /ic_launcher.png,,$@)); \
#	PX=$$(scripts/dp2px.sh $$QUAL 48); \
#	echo "QUAL: $$QUAL PX: $$PX"; \
#	convert $< -filter Lanczos2 -resize $${PX}x$${PX} $@
#	@optipng $@


ic_launcher-web.png: ressrc/iconsource.svg
	@TMP="$$(mktemp --suffix=".png" ic_launcher.XXXXX)" ;\
	eval inkscape -w 448 -e $$TMP -j -i icon $< ;\
#	convert $${TMP} -virtual-pixel transparent -distort Perspective '0,448 0,448 448,448 448,448 0,0 64,0 448,0 384,0' \
#	    -gravity North -crop 448x448+0x0 \
#	    \( +clone -background black -shadow '80x16+0+0' \) +swap -background none \
#	    -layers merge \
#	    +repage \
#	    -crop 512x512+0x0 \
#	    +repage $@ ; \
	cp $${TMP} $@;\
	RESULT=$$? ;\
	rm $${TMP} ;\
	exit $${RESULT}
	@optipng $@

#$(foreach qual,$(QUALS),res/drawable-$(qual)/ic_action_scores.png): ressrc/menu_highscore_ics.svg
#	@eval scripts/iconize.sh 32 32 $< $@ icon

$(foreach qual,$(QUALS),res/drawable-$(qual)/ic_cont_%.png): ressrc/ic_cont_%.svg
	@eval scripts/iconize.sh 32 32 $< $@

feature.png: ressrc/iconsource.svg
	@eval inkscape -w 1024 -h 500 -e $@ -j -i feature icon $<
	@optipng $@

$(foreach qual,$(QUALS),res/drawable-$(qual)/btn_default.9.png): ressrc/btn_src.svg Makefile
	@scripts/9patch.php 38 38 $< default $@ 10 18 10 18 9 20 9 20
	@optipng $@

$(foreach qual,$(QUALS),res/drawable-$(qual)/btn_active.9.png): ressrc/btn_src.svg Makefile
	@scripts/9patch.php 38 38 $< active $@ 13 18 13 18 12 22 12 20
	@optipng $@

$(foreach qual,$(QUALS),res/drawable-$(qual)/btn_pressed.9.png): ressrc/btn_src.svg Makefile
	@scripts/9patch.php 38 38 $< pressed $@ 13 18 13 18 12 22 12 20
	@optipng $@

#$(foreach qual,$(QUALS),res/drawable-$(qual)/popuparrowdown.png): ressrc/popup_bg.svg
#	@eval scripts/iconize.sh 17 12 $<  $@ arrowdown
#
#$(foreach qual,$(QUALS),res/drawable-$(qual)/popuparrowup.png): ressrc/popup_bg.svg
#	@eval scripts/iconize.sh 17 12 $<  $@ arrowup

promo.png: ressrc/iconsource.svg
	@eval inkscape -w 180 -h 120 -e $@ -j -i promo icon $<
	@optipng $@

