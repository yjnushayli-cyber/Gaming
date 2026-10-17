package com.yrc.gaming;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single bounded root command gateway. No untrusted inputs or PUBG access.
 * A separate stream reader prevents deadlock if the shell emits too much data.
 */
final class RootRunner {
    private RootRunner(){}
    static String run(String command) throws Exception {
        if(command==null||command.length()>500||command.indexOf('\n')>=0)
            throw new IllegalArgumentException("Unsafe command");
        Process process=new ProcessBuilder("su","-c",command)
            .redirectErrorStream(true).start();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        AtomicReference<IOException> readError=new AtomicReference<>();
        Thread reader=new Thread(()->{
            try(InputStream in=process.getInputStream()){
                byte[] buffer=new byte[1024];int n;
                while((n=in.read(buffer))!=-1){
                    synchronized(bytes){
                        if(bytes.size()<12000)
                            bytes.write(buffer,0,Math.min(12000-bytes.size(),n));
                    }
                }
            }catch(IOException ex){readError.set(ex);}
        },"YRC-root-output");
        reader.setDaemon(true);
        reader.start();
        boolean done=process.waitFor(7,TimeUnit.SECONDS);
        if(!done){
            process.destroyForcibly();
            reader.join(400);
            throw new IOException("Root shell timed out. Check KernelSU Next permission.");
        }
        reader.join(1000);
        if(reader.isAlive())throw new IOException("Root stream did not finish");
        if(readError.get()!=null)throw readError.get();
        String value;
        synchronized(bytes){value=bytes.toString("UTF-8").trim();}
        if(process.exitValue()!=0)throw new IOException(
            "su exit "+process.exitValue()+": "+(value.length()>240?value.substring(0,240):value));
        return value;
    }
    static void requireRoot() throws Exception{
        if(!"0".equals(run("id -u")))
            throw new IOException("KernelSU Next root permission not granted");
    }
    static String get(String table,String key)throws Exception{
        checkSetting(table,key);
        return run("settings get "+table+" "+key);
    }
    static String put(String table,String key,String value)throws Exception{
        checkSetting(table,key);
        if(value==null||!value.matches("[0-9.]{1,20}"))
            throw new IllegalArgumentException("Unsafe setting value");
        run("settings put "+table+" "+key+" "+value);
        return get(table,key);
    }
    static String restore(String table,String key,String old)throws Exception{
        checkSetting(table,key);
        if(old==null||"null".equals(old)||old.isEmpty())
            run("settings delete "+table+" "+key);
        else{
            if(!old.matches("[A-Za-z0-9_.-]{1,40}"))
                throw new IllegalArgumentException("Invalid backed-up setting");
            run("settings put "+table+" "+key+" "+old);
        }
        return get(table,key);
    }
    private static void checkSetting(String table,String key){
        if(!("system".equals(table)||"global".equals(table))||
            !key.matches("[a-z_]{2,40}"))
            throw new IllegalArgumentException("Unapproved setting");
    }
    static boolean equalSetting(String actual,String target){
        try{
            return Math.abs(Double.parseDouble(actual)-Double.parseDouble(target))<0.05;
        }catch(Exception e){return target.equals(actual);}
    }
}
