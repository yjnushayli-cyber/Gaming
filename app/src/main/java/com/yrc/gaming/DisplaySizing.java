package com.yrc.gaming;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic screen math. Never changes game memory or game files. */
final class DisplaySizing {
    private static final Pattern PHYSICAL=Pattern.compile("Physical size:\\s*(\\d{3,5})x(\\d{3,5})");
    private static final Pattern OVERRIDE=Pattern.compile("Override size:\\s*(\\d{3,5})x(\\d{3,5})");
    private DisplaySizing(){}

    static final class Size {
        final int width;
        final int height;
        Size(int width,int height){
            if(width<480||height<480||width>10000||height>10000)
                throw new IllegalArgumentException("Invalid display size");
            this.width=width;this.height=height;
        }
        @Override public String toString(){return width+"x"+height;}
        @Override public boolean equals(Object o){
            if(!(o instanceof Size))return false;
            Size other=(Size)o;
            return width==other.width&&height==other.height;
        }
        @Override public int hashCode(){return width*31+height;}
    }

    static Size parsePhysical(String output){return extract(output,PHYSICAL);}
    static Size parseOverride(String output){
        Matcher m=OVERRIDE.matcher(output==null?"":output);
        return m.find()?new Size(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2))):null;
    }
    static Size effective(String output){
        Size value=parseOverride(output);
        return value!=null?value:parsePhysical(output);
    }
    private static Size extract(String s,Pattern p){
        Matcher m=p.matcher(s==null?"":s);
        if(!m.find())throw new IllegalArgumentException("Screen dimensions unavailable");
        return new Size(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)));
    }

    /** Scales the CURRENT display, preserves aspect, never compounds from a prior boost. */
    static Size scaled(Size baseline,int percent){
        if(percent!=90&&percent!=100)
            throw new IllegalArgumentException("Unsupported render profile");
        if(percent==100)return baseline;
        int w=2*Math.round(baseline.width*percent/200f);
        int h=2*Math.round(baseline.height*percent/200f);
        if(w<480||h<800)
            throw new IllegalArgumentException("Render resolution too small");
        return new Size(w,h);
    }

    /** Phone in portrait. 16:10 aspect keeps narrow dimension unchanged. */
    static Size pc16by10(Size physical){
        int shortSide=Math.min(physical.width,physical.height);
        int longSide=2*((int)Math.round(shortSide*1.6)/2);
        return physical.width<physical.height?
            new Size(shortSide,longSide):new Size(longSide,shortSide);
    }
}
