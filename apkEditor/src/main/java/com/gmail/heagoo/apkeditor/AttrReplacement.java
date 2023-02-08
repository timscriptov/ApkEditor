package com.gmail.heagoo.apkeditor;

/*
interface IGetAttrName {
	public String getString(int id);
}

class AttrReplacement_Common implements IGetAttrName {

	private boolean isMaskMode;
	private SparseArray<String> id2str = new SparseArray<String>();

	public AttrReplacement_Common() {
		this(false);
	}

	public AttrReplacement_Common(boolean maskMode) {
		this.isMaskMode = maskMode;
	}

	public void add(int id, String name) {
		this.id2str.put(id, name);
	}

	// Get String by id
	public String getString(int id) {
		if (isMaskMode) {
			// direct return
			String s = id2str.get(id);
			if (s != null) {
				return s;
			}
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < id2str.size(); i++) {
				int curId = id2str.keyAt(i);
				if (curId == 0) continue;
				if ((id & curId) == curId) {
					sb.append(id2str.valueAt(i)).append("|");
				}
			}
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
				return sb.toString();
			}
			return null;
		}

		return id2str.get(id);
	}
}

public class AttrReplacement {
	public static String getReplacement(String attrName, int id) {
		IGetAttrName r = replaces.get(attrName);
		if (r != null) {
			String str = r.getString(id);
			if (str != null) {
				return str;
			}
		}
		return null;
	}

	private static Map<String, IGetAttrName> replaces = new HashMap<String, IGetAttrName>();
	static {
		AttrReplacement_Common replace = new AttrReplacement_Common();
		replace.add(-1, "fill_parent");
		replace.add(-2, "wrap_content");
		replaces.put("layout_width", replace);
		replaces.put("layout_height", replace);
		replaces.put("dropDownWidth", replace);
		replaces.put("dropDownHeight", replace);

		// android:orientation
		replace = new AttrReplacement_Common();
		replace.add(0, "horizontal");
		replace.add(1, "vertical");
		replaces.put("orientation", replace);
		
		// android:clipOrientation
		replace = new AttrReplacement_Common(true);
		replace.add(1, "horizontal");
		replace.add(2, "vertical");
		replaces.put("clipOrientation", replace);

		// android:visibility
		replace = new AttrReplacement_Common();
		replace.add(0, "visible");
		replace.add(1, "invisible");
		replace.add(2, "gone");
		replaces.put("visibility", replace);

		// android:gravity
		AttrReplacement_Gravity gravityReplace = new AttrReplacement_Gravity();
		replaces.put("gravity", gravityReplace);
		replaces.put("layout_gravity", gravityReplace);
		replaces.put("foregroundGravity", gravityReplace);
		replaces.put("scaleGravity", gravityReplace);

		// android:ellipsize
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "start");
		replace.add(2, "middle");
		replace.add(3, "end");
		replace.add(4, "marquee");
		replaces.put("ellipsize", replace);

		replace = new AttrReplacement_Common(true);
		replace.add(0, "normal");
		replace.add(1, "bold");
		replace.add(2, "italic");
		replaces.put("textStyle", replace);

		// android:shape
		replace = new AttrReplacement_Common();
		replace.add(0, "rectangle");
		replace.add(1, "oval");
		replace.add(2, "line");
		replace.add(3, "rectangle");
		replaces.put("shape", replace);

		// android:type
		replace = new AttrReplacement_Common();
		replace.add(0, "linear");
		replace.add(1, "radial");
		replace.add(2, "sweep");
		replaces.put("type", replace);

		// android:installLocation
		replace = new AttrReplacement_Common();
		replace.add(0, "auto");
		replace.add(1, "internalOnly");
		replace.add(2, "preferExternal");
		replaces.put("installLocation", replace);
		
		// android:reqKeyboardType
		replace = new AttrReplacement_Common();
		replace.add(0, "undefined");
		replace.add(1, "nokeys");
		replace.add(2, "qwerty");
		replace.add(3, "twelvekey");
		replaces.put("reqKeyboardType", replace);
		
		// android:reqNavigation=["undefined" | "nonav" | "dpad" | "trackball" | "wheel"]
		replace = new AttrReplacement_Common();
		replace.add(0, "undefined");
		replace.add(1, "nonav");
		replace.add(2, "dpad");
		replace.add(3, "trackball");
		replace.add(4, "wheel");
		replaces.put("reqNavigation", replace);
		
		// android:reqTouchScreen=["undefined" | "notouch" | "stylus" | "finger"] />
		replace = new AttrReplacement_Common();
		replace.add(0, "undefined");
		replace.add(1, "notouch");
		replace.add(2, "stylus");
		replace.add(3, "finger");
		replaces.put("reqTouchScreen", replace);

		// android:configChanges
		replace = new AttrReplacement_Common(true);
		replace.add(0x0001, "mcc");
		replace.add(0x0002, "mnc");
		replace.add(0x0004, "locale");
		replace.add(0x0008, "touchscreen");
		replace.add(0x0010, "keyboard");
		replace.add(0x0020, "keyboardHidden");
		replace.add(0x0040, "navigation");
		replace.add(0x0080, "orientation");
		replace.add(0x0100, "screenLayout");
		replace.add(0x0200, "uiMode");
		replace.add(0x0400, "screenSize");
		replace.add(0x0800, "smallestScreenSize");
		replace.add(0x2000, "layoutDirection");
		replace.add(0x40000000, "fontScale");
		replaces.put("configChanges", replace);

		// android:launchMode
		replace = new AttrReplacement_Common();
		replace.add(0, "standard");
		replace.add(1, "singleTop");
		replace.add(2, "singleTask");
		replace.add(3, "singleInstance");
		replaces.put("launchMode", replace);

		// android:uiOptions
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "splitActionBarWhenNarrow");
		replaces.put("uiOptions", replace);

		// android:windowSoftInputMode
		AttrReplacement_WindowSoftInputMode wsim = new AttrReplacement_WindowSoftInputMode();
		replaces.put("windowSoftInputMode", wsim);
		
		// android:scrollbarStyle
		replace = new AttrReplacement_Common();
		replace.add(200, "small");
		replace.add(300, "normal");
		replace.add(400, "large");
		replace.add(500, "xlarge");
		replaces.put("screenSize", replace);
		
		// android:screenDensity
		replace = new AttrReplacement_Common();
		replace.add(120, "ldpi");
		replace.add(160, "mdpi");
		replace.add(240, "hdpi");
		replace.add(320, "xhdpi");
		replaces.put("screenDensity", replace);

		// android:inputType
		replace = new AttrReplacement_Common(true);
		replace.add(0x000000, "none");
		replace.add(0x000001, "text");
		replace.add(0x001001, "textCapCharacters");
		replace.add(0x002001, "textCapWords");
		replace.add(0x004001, "textCapSentences");
		replace.add(0x008001, "textAutoCorrect");
		replace.add(0x010001, "textAutoComplete");
		replace.add(0x020001, "textMultiLine");
		replace.add(0x040001, "textImeMultiLine");
		replace.add(0x080001, "textNoSuggestions");
		replace.add(0x000011, "textUri");
		replace.add(0x000021, "textEmailAddress");
		replace.add(0x000031, "textEmailSubject");
		replace.add(0x000041, "textShortMessage");
		replace.add(0x000051, "textLongMessage");
		replace.add(0x000061, "textPersonName");
		replace.add(0x000071, "textPostalAddress");
		replace.add(0x000081, "textPassword");
		replace.add(0x000091, "textVisiblePassword");
		replace.add(0x0000A1, "textWebEditText");
		replace.add(0x0000B1, "textFilter");
		replace.add(0x0000C1, "textPhonetic");
		replace.add(0x000002, "number");
		replace.add(0x001002, "numberSigned");
		replace.add(0x002002, "numberDecimal");
		replace.add(0x000003, "phone");
		replace.add(0x000004, "datetime");
		replace.add(0x000014, "date");
		replace.add(0x000024, "time");
		replaces.put("inputType", replace);
		
		// android:displayOptions
		replace = new AttrReplacement_Common(true);
		replace.add(0, "none");
		replace.add(0x01, "useLogo");
		replace.add(0x02, "showHome");
		replace.add(0x04, "homeAsUp");
		replace.add(0x08, "showTitle");
		replace.add(0x10, "showCustom");
		replace.add(0x20, "disableHome");
		replaces.put("displayOptions", replace);
		
		// anddroid:showDividers
		replace = new AttrReplacement_Common(true);
		replace.add(0, "none");
		replace.add(1, "beginning");
		replace.add(2, "middle");
		replace.add(4, "end");
		replaces.put("showDividers", replace);

		// android:scaleType
		replace = new AttrReplacement_Common();
		replace.add(5, "center");
		replace.add(0, "matrix");
		replace.add(1, "fitXY");
		replace.add(2, "fitStart");
		replace.add(4, "fitEnd");
		replace.add(3, "fitCenter");
		replace.add(7, "centerInside");
		replace.add(6, "centerCrop");
		replaces.put("scaleType", replace);

		// android:showAsAction
		replace = new AttrReplacement_Common(true);
		replace.add(0x000001, "ifRoom");
		replace.add(0x000000, "never");
		replace.add(0x000004, "withText");
		replace.add(0x000002, "always");
		replace.add(0x000008, "collapseActionView");
		replaces.put("showAsAction", replace);

		// android:menuCategory
		replace = new AttrReplacement_Common();
		replace.add(0x010000, "container");
		replace.add(0x020000, "system");
		replace.add(0x030000, "secondary");
		replace.add(0x040000, "alternative");
		replaces.put("menuCategory", replace);

		// android:scrollbars
		replace = new AttrReplacement_Common(true);
		replace.add(0x00000000, "none");
		replace.add(0x00000100, "horizontal");
		replace.add(0x00000200, "vertical");
		replaces.put("scrollbars", replace);
		
		// android:scrollbarStyle
		replace = new AttrReplacement_Common();
		replace.add(0x0, "insideOverlay");
		replace.add(0x01000000, "insideInset");
		replace.add(0x02000000, "outsideOverlay");
		replace.add(0x03000000, "outsideInset");
		replaces.put("scrollbarStyle", replace);
		
		// android:accessibilityLiveRegion
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "polite");
		replace.add(2, "assertive");
		replaces.put("accessibilityLiveRegion", replace);
		
		// android:drawingCacheQuality
		replace = new AttrReplacement_Common();
		replace.add(0, "auto");
		replace.add(1, "low");
		replace.add(2, "high");
		replaces.put("drawingCacheQuality", replace);
		
		// android:importantForAccessibility
		replace = new AttrReplacement_Common();
		replace.add(0, "auto");
		replace.add(1, "yes");
		replace.add(2, "no");
		replace.add(4, "noHideDescendants");
		replaces.put("importantForAccessibility", replace);
		
		// android:layerType
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "software");
		replace.add(2, "hardware");
		replaces.put("layerType", replace);
		
		// android:layoutDirection
		replace = new AttrReplacement_Common();
		replace.add(0, "ltr");
		replace.add(1, "rtl");
		replace.add(2, "inherit");
		replace.add(3, "locale");
		replaces.put("layoutDirection", replace);
		
		// android:layoutMode
		replace = new AttrReplacement_Common();
		replace.add(0, "clipBounds");
		replace.add(1, "opticalBounds");
		replaces.put("layoutMode", replace);
		
		// android:requiresFadingEdge
		replace = new AttrReplacement_Common(true);
		replace.add(0, "none");
		replace.add(0x00001000, "horizontal");
		replace.add(0x00002000, "vertical");
		replaces.put("requiresFadingEdge", replace);
		
		// android:textAlignment
		replace = new AttrReplacement_Common();
		replace.add(0, "inherit");
		replace.add(1, "gravity");
		replace.add(2, "textStart");
		replace.add(3, "textEnd");
		replace.add(4, "center");
		replace.add(5, "viewStart");
		replace.add(6, "viewEnd");
		replaces.put("textAlignment", replace);
		
		// android:textDirection
		replace = new AttrReplacement_Common();
		replace.add(0, "inherit");
		replace.add(1, "firstStrong");
		replace.add(2, "anyRtl");
		replace.add(3, "ltr");
		replace.add(4, "rtl");
		replace.add(5, "locale");
		replaces.put("textDirection", replace);
		

		// android:typeface
		replace = new AttrReplacement_Common();
		replace.add(0, "normal");
		replace.add(1, "sans");
		replace.add(2, "serif");
		replace.add(3, "monospace");
		replaces.put("typeface", replace);

		replace = new AttrReplacement_Common();
		replace.add(-1, "auto_fit");
		replaces.put("numColumns", replace);

		// android:stretchMode
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "spacingWidth");
		replace.add(2, "columnWidth");
		replace.add(3, "spacingWidthUniform");
		replaces.put("stretchMode", replace);

		// android:choiceMode
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "singleChoice");
		replace.add(2, "multipleChoice");
		replace.add(3, "multipleChoiceModal");
		replaces.put("choiceMode", replace);

		// android:transcriptMode
		replace = new AttrReplacement_Common();
		replace.add(0, "disabled");
		replace.add(1, "normal");
		replace.add(2, "alwaysScroll");
		replaces.put("transcriptMode", replace);
		
		// android:transitionOrdering
		replace = new AttrReplacement_Common();
		replace.add(0, "together");
		replace.add(1, "sequential");
		replaces.put("transitionOrdering", replace);

		// android:imeOptions
		AttrReplacement_ImeOptions imeOptionsReplace = new AttrReplacement_ImeOptions();
		replaces.put("imeOptions", imeOptionsReplace);

		// android:descendantFocusability
		replace = new AttrReplacement_Common();
		replace.add(0, "beforeDescendants");
		replace.add(1, "afterDescendants");
		replace.add(2, "blocksDescendants");
		replaces.put("descendantFocusability", replace);

		// android:fadingEdge
		replace = new AttrReplacement_Common(true);
		replace.add(0, "none");
		replace.add(0x00001000, "horizontal");
		replace.add(0x00002000, "vertical");
		replaces.put("fadingEdge", replace);
		
		// android:fadingMode
		replace = new AttrReplacement_Common(true);
		replace.add(1, "fade_in");
		replace.add(2, "fade_out");
		replace.add(3, "fade_in_out");
		replaces.put("fadingMode", replace);

		// android:marqueeRepeatLimit
		replace = new AttrReplacement_Common();
		replace.add(-1, "marquee_forever");
		replaces.put("marqueeRepeatLimit", replace);

		// android:autoLink
		replace = new AttrReplacement_Common(true);
		replace.add(0, "none");
		replace.add(0x01, "web");
		replace.add(0x02, "email");
		replace.add(0x04, "phone");
		replace.add(0x08, "map");
		replaces.put("autoLink", replace);

		// android:bufferType
		replace = new AttrReplacement_Common();
		replace.add(0, "normal");
		replace.add(1, "spannable");
		replace.add(2, "editable");
		replaces.put("bufferType", replace);

		// android:capitalize
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "sentences");
		replace.add(2, "words");
		replace.add(3, "characters");
		replaces.put("capitalize", replace);

		// android:numeric
		replace = new AttrReplacement_Common(true);
		replace.add(0x01, "integer");
		replace.add(0x03, "signed");
		replace.add(0x05, "decimal");
		replaces.put("numeric", replace);

		// android:alignmentMode
		replace = new AttrReplacement_Common();
		replace.add(0, "alignBounds");
		replace.add(1, "alignMargins");
		replaces.put("alignmentMode", replace);

		// android:indeterminateBehavior
		replace = new AttrReplacement_Common();
		replace.add(1, "repeat");
		replace.add(2, "cycle");
		replaces.put("indeterminateBehavior", replace);

		// android:spinnerMode
		replace = new AttrReplacement_Common();
		replace.add(0, "dialog");
		replace.add(1, "dropdown");
		replaces.put("spinnerMode", replace);

		// android:screenOrientation
		replace = new AttrReplacement_Common();
		replace.add(3, "behind");
		replace.add(10, "fullSensor");
		replace.add(13, "fullUser");
		replace.add(0, "landscape");
		replace.add(14, "locked");
		replace.add(5, "nosensor");
		replace.add(1, "portrait");
		replace.add(8, "reverseLandscape");
		replace.add(9, "reversePortrait");
		replace.add(4, "sensor");
		replace.add(6, "sensorLandscape");
		replace.add(7, "sensorPortrait");
		replace.add(2, "user");
		replace.add(11, "userLandscape");
		replace.add(12, "userPortrait");
		replace.add(-1, "unspecified");
		replaces.put("screenOrientation", replace);
		
		// android:protectionLevel
		replace = new AttrReplacement_Common(true);
		replace.add(0, "normal");
		replace.add(1, "dangerous");
		replace.add(2, "signature");
		replace.add(3, "signatureOrSystem");
		replace.add(0x10, "system");
		replace.add(0x20, "development");
		replaces.put("protectionLevel", replace);
		
		// android:tileMode
		replace = new AttrReplacement_Common();
		replace.add(-1, "disabled");
		replace.add(0, "clamp");
		replace.add(1, "repeat");
		replace.add(2, "mirror");
		replaces.put("tileMode", replace);
		
		// android:overScrollMode
		replace = new AttrReplacement_Common();
		replace.add(0, "always");
		replace.add(1, "if_content_scrolls");
		replace.add(2, "never");
		replaces.put("overScrollMode", replace);
		
		// android:repeatCount
		replace = new AttrReplacement_Common();
		replace.add(-1, "infinite");
		replaces.put("repeatCount", replace);
		
		// android:repeatMode
		replace = new AttrReplacement_Common();
		replace.add(1, "restart");
		replace.add(2, "reverse");
		replaces.put("repeatMode", replace);
		
		// android:zAdjustment
		replace = new AttrReplacement_Common();
		replace.add(0, "normal");
		replace.add(1, "top");
		replace.add(-1, "bottom");
		replaces.put("zAdjustment", replace);
		
		// Added at 20140521
		// android:accessibilityEventTypes
		replace = new AttrReplacement_Common(true);
		replace.add(0x00000001, "typeViewClicked");
		replace.add(0x00000002, "typeViewLongClicked");
		replace.add(0x00000004, "typeViewSelected");
		replace.add(0x00000008, "typeViewFocused");
		replace.add(0x00000010, "typeViewTextChanged");
		replace.add(0x00000020, "typeWindowStateChanged");
		replace.add(0x00000040, "typeNotificationStateChanged");
		replace.add(0x00000080, "typeViewHoverEnter");
		replace.add(0x00000100, "typeViewHoverExit");
		replace.add(0x00000200, "typeTouchExplorationGestureStart");
		replace.add(0x00000400, "typeTouchExplorationGestureEnd");
		replace.add(0x00000800, "typeWindowContentChanged");
		replace.add(0x00001000, "typeViewScrolled");
		replace.add(0x00002000, "typeViewTextSelectionChanged");
		replace.add(0xffffffff, "typeAllMask");
		replaces.put("accessibilityEventTypes", replace);
		
		// android:accessibilityFeedbackType
		replace = new AttrReplacement_Common(true);
		replace.add(0x00000001, "feedbackSpoken");
		replace.add(0x00000002, "feedbackHaptic");
		replace.add(0x00000004, "feedbackAudible");
		replace.add(0x00000008, "feedbackVisual");
		replace.add(0x00000010, "feedbackGeneric");
		replace.add(0xffffffff, "feedbackAllMask");
		replaces.put("accessibilityFeedbackType", replace);
				
		// android:accessibilityFlags
		replace = new AttrReplacement_Common(true);
		replace.add(0x00000001, "flagDefault");
		replace.add(0x00000002, "flagIncludeNotImportantViews");
		replace.add(0x00000004, "flagRequestTouchExplorationMode");
		replace.add(0x00000008, "flagRequestEnhancedWebAccessibility");
		replace.add(0x00000010, "flagReportViewIds");
		replace.add(0x00000020, "flagRequestFilterKeyEvents");
		replaces.put("accessibilityFlags", replace);

		// android:actionBarSize
		replace = new AttrReplacement_Common();
		replace.add(0, "wrap_content");
		replaces.put("actionBarSize", replace);
		
		// android:animationOrder
		replace = new AttrReplacement_Common();
		replace.add(0, "normal");
		replace.add(1, "reverse");
		replace.add(2, "random");
		replaces.put("animationOrder", replace);
		
		// android:checkableBehavior
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "all");
		replace.add(2, "single");
		replaces.put("checkableBehavior", replace);
		
		// android:directionPriority
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "column");
		replace.add(2, "row");
		replaces.put("directionPriority", replace);
		
		// android:fastScrollOverlayPosition
		replace = new AttrReplacement_Common();
		replace.add(0, "floating");
		replace.add(1, "atThumb");
		replaces.put("fastScrollOverlayPosition", replace);
		
		// android:gestureStrokeType
		replace = new AttrReplacement_Common();
		replace.add(0, "single");
		replace.add(1, "multiple");
		replaces.put("gestureStrokeType", replace);
	
		// android:keyEdgeFlags
		replace = new AttrReplacement_Common(true);
		replace.add(1, "left");
		replace.add(2, "right");
		replaces.put("keyEdgeFlags", replace);
	
		// android:mediaRouteTypes
		replace = new AttrReplacement_Common();
		replace.add(0x01, "liveAudio");
		replace.add(0x800000, "user");
		replaces.put("mediaRouteTypes", replace);
	
		// android:mode
		replace = new AttrReplacement_Common();
		replace.add(1, "oneLine");
		replace.add(2, "collapsing");
		replace.add(3, "twoLine");
		replaces.put("mode", replace);
	
		// android:navigationMode
		replace = new AttrReplacement_Common();
		replace.add(0, "normal");
		replace.add(1, "listMode");
		replace.add(2, "tabMode");
		replaces.put("navigationMode", replace);
		
		// android:opacity
		replace = new AttrReplacement_Common();
		replace.add(-1, "opaque");
		replace.add(-2, "transparent");
		replace.add(-3, "translucent");
		replaces.put("opacity", replace);
		
		// android:ordering
		replace = new AttrReplacement_Common();
		replace.add(0, "together");
		replace.add(1, "sequentially");
		replaces.put("ordering", replace);
		
		// android:permissionFlags
		replace = new AttrReplacement_Common(true);
		replace.add(0x0001, "costsMoney");
		replaces.put("permissionFlags", replace);
		
		// android:permissionGroupFlags
		replace = new AttrReplacement_Common(true);
		replace.add(0x0001, "personalInfo");
		replaces.put("permissionGroupFlags", replace);
		
		// android:persistentDrawingCache
		replace = new AttrReplacement_Common();
		replace.add(0, "none");
		replace.add(1, "animation");
		replace.add(2, "scrolling");
		replace.add(3, "all");
		replaces.put("persistentDrawingCache", replace);
		
		// android:resizeMode
		replace = new AttrReplacement_Common(true);
		replace.add(0, "none");
		replace.add(1, "horizontal");
		replace.add(2, "vertical");
		replaces.put("resizeMode", replace);
		
		// android:ringtoneType
		replace = new AttrReplacement_Common(true);
		replace.add(1, "ringtone");
		replace.add(2, "notification");
		replace.add(4, "alarm");
		replace.add(7, "all");
		replaces.put("ringtoneType", replace);
		
		// android:rowEdgeFlags
		replace = new AttrReplacement_Common(true);
		replace.add(4, "top");
		replace.add(8, "bottom");
		replaces.put("rowEdgeFlags", replace);
		
		// android:searchMode
		replace = new AttrReplacement_Common(true);
		replace.add(0x04, "showSearchLabelAsBadge");
		replace.add(0x08, "showSearchIconAsBadge");
		replace.add(0x10, "queryRewriteFromData");
		replace.add(0x20, "queryRewriteFromText");
		replaces.put("searchMode", replace);
		
		// android:streamType
		replace = new AttrReplacement_Common();
		replace.add(0, "voice");
		replace.add(1, "system");
		replace.add(2, "ring");
		replace.add(3, "music");
		replace.add(4, "alarm");
		replaces.put("streamType", replace);
		
		// android:valueType
		replace = new AttrReplacement_Common();
		replace.add(0, "floatType");
		replace.add(1, "intType");
		replaces.put("valueType", replace);
		
		// android:verticalScrollbarPosition
		replace = new AttrReplacement_Common();
		replace.add(0, "defaultPosition");
		replace.add(1, "left");
		replace.add(2, "right");
		replaces.put("verticalScrollbarPosition", replace);
		
		// android:voiceSearchMode
		replace = new AttrReplacement_Common(true);
		replace.add(1, "showVoiceSearchButton");
		replace.add(2, "launchWebSearch");
		replace.add(4, "launchRecognizer");
		replaces.put("voiceSearchMode", replace);
		
		// android:widgetCategory
		replace = new AttrReplacement_Common(true);
		replace.add(1, "home_screen");
		replace.add(2, "keyguard");
		replaces.put("widgetCategory", replace);
		
	}
}

class AttrReplacement_Gravity implements IGetAttrName {

	private SparseArray<String> id2str = new SparseArray<String>();

	public AttrReplacement_Gravity() {
		id2str.put(0, "no_gravity");
		id2str.put(1, "center_horizontal");
		id2str.put(3, "left");
		id2str.put(5, "right");
		id2str.put(7, "fill_horizontal");
		id2str.put(8, "clip_horizontal");
		id2str.put(16, "center_vertical");
		id2str.put(17, "center");
		id2str.put(48, "top");
		id2str.put(80, "bottom");
		id2str.put(112, "fill_vertical");
		id2str.put(119, "fill"); // //B1110111
		id2str.put(128, "clip_vertical");
		id2str.put(0x00800003, "start");
		id2str.put(0x00800005, "end");
	}

	// Special case: "gravity", "layout_gravity"
	public String getString(int id) {
		String value = id2str.get(id);
		if (value != null) {
			return value;
		}

		int low = (id & 0x0f);
		int high = (id & 0xf0);
		String hStr = null;
		String vStr = null;
		if (low != 0) {
			hStr = id2str.get(low);
		}
		if (high != 0) {
			vStr = id2str.get(high);
		}
		if (hStr != null && vStr != null) {
			return hStr + "|" + vStr;
		} else if (hStr != null) {
			return hStr;
		} else {
			return vStr;
		}
	}
}

class AttrReplacement_WindowSoftInputMode implements IGetAttrName {
	private SparseArray<String> id2str = new SparseArray<String>();

	public AttrReplacement_WindowSoftInputMode() {
		id2str.put(0, "stateUnspecified");
		id2str.put(1, "stateUnchanged");
		id2str.put(2, "stateHidden");
		id2str.put(3, "stateAlwaysHidden");
		id2str.put(4, "stateVisible");
		id2str.put(5, "stateAlwaysVisible");
	}

	@Override
	public String getString(int id) {
		String lowValue = "stateUnspecified";
		int low = id & 0x0f;
		if (low != 0) {
			lowValue = id2str.get(low);
		}

		String highValue = "adjustUnspecified";
		int high = id & 0xf0;
		if (high == 0x10) {
			highValue = "adjustResize";
		} else if (high == 0x20) {
			highValue = "adjustPan";
		} else if (high == 0x30) {
			highValue = "adjustNothing";
		}

		return lowValue + "|" + highValue;
	}
}

class AttrReplacement_ImeOptions implements IGetAttrName {

	private SparseArray<String> id2str = new SparseArray<String>();

	public AttrReplacement_ImeOptions() {
		id2str.put(0x00000000, "normal");
		id2str.put(0x00000001, "actionNone");
		id2str.put(0x00000002, "actionGo");
		id2str.put(0x00000003, "actionSearch");
		id2str.put(0x00000004, "actionSend");
		id2str.put(0x00000005, "actionNext");
		id2str.put(0x00000006, "actionDone");
		id2str.put(0x00000007, "actionPrevious");
		id2str.put(0x2000000, "flagNoFullscreen");
		id2str.put(0x4000000, "flagNavigatePrevious");
		id2str.put(0x8000000, "flagNavigateNext");
		id2str.put(0x10000000, "flagNoExtractUi");
		id2str.put(0x20000000, "flagNoAccessoryAction");
		id2str.put(0x40000000, "flagNoEnterAction");
		id2str.put(0x80000000, "flagForceAscii");
	}

	public String getString(int id) {
		String value = id2str.get(id);
		if (value != null) {
			return value;
		}

		String lowValue = null;
		int low = id & 0x0f;
		if (low != 0) {
			lowValue = id2str.get(low);
		}

		String highValue = null;
		int high = id & 0xff000000;
		if (high != 0) {
			highValue = getStringValue(high);
		}

		if (lowValue != null && highValue != null) {
			return lowValue + "|" + highValue;
		} else if (lowValue != null) {
			return lowValue;
		} else {
			return highValue;
		}
	}

	private String getStringValue(int id) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < id2str.size(); i++) {
			int curId = id2str.keyAt(i);
			if ((id & curId) != 0) {
				sb.append(id2str.valueAt(i)).append("|");
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
		return null;
	}
}
*/