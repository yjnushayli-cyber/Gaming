package com.yrc.gaming;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression tests: preserve the user's screen and PC aspect without compounding. */
public class DisplaySizingTest {
    @Test public void defaultPhysicalDimensions(){
        DisplaySizing.Size size=DisplaySizing.parsePhysical("Physical size: 1224x2720");
        assertEquals("1224x2720",size.toString());
        assertEquals(size,DisplaySizing.effective("Physical size: 1224x2720"));
    }
    @Test public void respectsExistingOverride(){
        DisplaySizing.Size current=DisplaySizing.effective(
            "Physical size: 1224x2720\nOverride size: 1224x1958");
        assertEquals(new DisplaySizing.Size(1224,1958),current);
    }
    @Test public void scale90PercentOfActivePcRatio(){
        DisplaySizing.Size view=new DisplaySizing.Size(1224,1958);
        DisplaySizing.Size scaled=DisplaySizing.scaled(view,90);
        assertEquals(new DisplaySizing.Size(1102,1762),scaled);
        assertTrue(Math.abs((double)scaled.height/scaled.width-1.6)<0.01);
    }
    @Test public void pc16by10FromPhysical(){
        DisplaySizing.Size pc=DisplaySizing.pc16by10(new DisplaySizing.Size(1224,2720));
        assertEquals(new DisplaySizing.Size(1224,1958),pc);
    }
    @Test public void noChangeFor100Percent(){
        DisplaySizing.Size original=new DisplaySizing.Size(1224,1958);
        assertSame(original,DisplaySizing.scaled(original,100));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnsupportedProfile(){DisplaySizing.scaled(new DisplaySizing.Size(1224,1958),84);}
    @Test(expected=IllegalArgumentException.class)
    public void rejectsMissingPhysical(){DisplaySizing.parsePhysical("Error: permission denied");}
    @Test public void previousScreenOverrideCanBeParsed(){
        String read="Physical size: 1224x2720\nOverride size: 1162x2584";
        assertEquals(new DisplaySizing.Size(1162,2584),DisplaySizing.parseOverride(read));
    }
}
